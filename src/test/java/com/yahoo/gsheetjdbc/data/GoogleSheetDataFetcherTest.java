/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */

package com.yahoo.gsheetjdbc.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.gsheetjdbc.schema.Column;
import com.yahoo.gsheetjdbc.schema.Table;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class GoogleSheetDataFetcherTest {

    @Test
    public void testSchemaParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        Reader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/apiResponses/employeeData.json")));

        Spreadsheet spreadsheet = parser.parseAndClose(reader, Spreadsheet.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();
        Sheet sheet1 = spreadsheet.getSheets().get(0);

        Table table = fetcher.extractTableSchema(sheet1, "TestSchema");
        assertEquals("Salary", table.getTableName());
        assertEquals(3, table.getColumns().size());
        assertEquals("Employee", table.getColumn(0).getName());
        assertEquals(Column.ColumnType.STRING, table.getColumn(0).getType());
        assertEquals("Hire Date", table.getColumn(1).getName());
        assertEquals(Column.ColumnType.DATE, table.getColumn(1).getType());
        assertEquals("Salary", table.getColumn(2).getName());
        assertEquals(Column.ColumnType.NUMBER, table.getColumn(2).getType());

        Sheet sheet2 = spreadsheet.getSheets().get(1);

        table = fetcher.extractTableSchema(sheet2, "TestSchema");
        assertEquals("PTO", table.getTableName());
        assertEquals(2, table.getColumns().size());
        assertEquals("Employee", table.getColumn(0).getName());
        assertEquals(Column.ColumnType.STRING, table.getColumn(0).getType());
        assertEquals("Days Off", table.getColumn(1).getName());
        assertEquals(Column.ColumnType.NUMBER, table.getColumn(1).getType());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            //Missing data
            "{\"sheets\":[{\"properties\":{\"title\":\"Sheet1\"}}]}",
            //Missing row data
            "{\"sheets\":[{\"data\":[],\"properties\":{\"title\":\"Sheet1\"}}]}",
            //Empty row data
            "{\"sheets\":[{\"data\":[{\"rowData\":[]}],\"properties\":{\"title\":\"Sheet1\"}}]}"
    })
    public void testSchemaParsingMissingHeader(String data) throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        Spreadsheet spreadsheet = parser.parseAndClose(new ByteArrayInputStream(data.getBytes()),
                Charset.defaultCharset(), Spreadsheet.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        Sheet sheet1 = spreadsheet.getSheets().get(0);

        assertThrows(IllegalStateException.class, () -> fetcher.extractTableSchema(sheet1, "TestSchema"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            //Empty Column Name
            "{\"sheets\":[{\"data\":[{\"rowData\":[{\"values\":[{\"effectiveValue\":{\"stringValue\":\"\"}}]},{\"values\":[{\"effectiveValue\":{\"stringValue\":\"Alexandra\"}}]}]}],\"properties\":{\"title\":\"Sheet1\"}}]}",
            //Column Name > 256 Characters
            "{\"sheets\":[{\"data\":[{\"rowData\":[{\"values\":[{\"effectiveValue\":{\"stringValue\":\"A01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\"}}]},{\"values\":[{\"effectiveValue\":{\"stringValue\":\"Alexandra\"}}]}]}],\"properties\":{\"title\":\"Sheet1\"}}]}"
    })
    public void testSchemaParsingInvalidColumnName(String data) throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        Spreadsheet spreadsheet = parser.parseAndClose(new ByteArrayInputStream(data.getBytes()),
                Charset.defaultCharset(), Spreadsheet.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        Sheet sheet1 = spreadsheet.getSheets().get(0);

        assertThrows(IllegalStateException.class, () -> fetcher.extractTableSchema(sheet1, "TestSchema"));
    }

    @Test
    public void testDataParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        Reader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/apiResponses/employeeData.json")));

        Spreadsheet spreadsheet = parser.parseAndClose(reader, Spreadsheet.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();
        Sheet sheet1 = spreadsheet.getSheets().get(0);
        Table table1 = fetcher.extractTableSchema(sheet1, "TestSchema");
        Sheet sheet2 = spreadsheet.getSheets().get(1);
        Table table2 = fetcher.extractTableSchema(sheet2, "TestSchema");

        List<List<Object>> data1 = fetcher.extractSheetData(table1, sheet1);

        assertEquals(1, data1.size());
        List row1 = data1.get(0);
        assertEquals(3, row1.size());
        assertEquals("John Doe", row1.get(0));
        assertEquals(LocalDateTime.of(
                LocalDate.of(2021, 10, 8),
                LocalTime.of(0, 0, 0, 0)), row1.get(1));
        assertEquals(400000.0, row1.get(2));

        List<List<Object>> data2 = fetcher.extractSheetData(table2, sheet2);

        assertEquals(1, data2.size());
        row1 = data2.get(0);
        assertEquals(2, row1.size());

        assertEquals("John Doe", row1.get(0));
        assertEquals(45.0, row1.get(1));
    }

    @Test
    public void testTextCellParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        String textCell = "{\"effectiveValue\":{\"stringValue\":\"Data\"}}";
        CellData cell = parser.parseAndClose(new ByteArrayInputStream(textCell.getBytes()),
                Charset.defaultCharset(), CellData.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        assertEquals(Column.ColumnType.STRING, fetcher.extractColumnType(cell));
        assertEquals("Data", fetcher.extractCellData(
                Column.builder().name("textCell").type(Column.ColumnType.STRING).build(), cell));
    }

    @Test
    public void testNumberCellParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        String numberCell = "{\"effectiveValue\":{\"numberValue\":100}}";
        CellData cell = parser.parseAndClose(new ByteArrayInputStream(numberCell.getBytes()),
                Charset.defaultCharset(), CellData.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        assertEquals(Column.ColumnType.NUMBER, fetcher.extractColumnType(cell));
        assertEquals(100.0, fetcher.extractCellData(
                Column.builder().name("numberCell").type(Column.ColumnType.NUMBER).build(), cell));
    }

    @Test
    public void testBooleanCellParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());

        String booleanCell = "{\"effectiveValue\":{\"boolValue\":true}}";
        CellData cell = parser.parseAndClose(new ByteArrayInputStream(booleanCell.getBytes()),
                Charset.defaultCharset(), CellData.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        assertEquals(Column.ColumnType.BOOLEAN, fetcher.extractColumnType(cell));
        assertEquals(true, fetcher.extractCellData(
                Column.builder().name("booleanCell").type(Column.ColumnType.BOOLEAN).build(), cell));
    }

    @Test
    public void testDateCellParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());
        String dateCell = "{\"effectiveFormat\":{\"numberFormat\":{\"pattern\":\"yyyy-mm-dd\",\"type\":\"DATE\"}},\"effectiveValue\":{\"numberValue\":44472.0}}";

        CellData cell = parser.parseAndClose(new ByteArrayInputStream(dateCell.getBytes()),
                Charset.defaultCharset(), CellData.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        assertEquals(Column.ColumnType.DATE, fetcher.extractColumnType(cell));
        assertEquals(LocalDateTime.of(
                LocalDate.of(2021, 10, 3), LocalTime.of(0,0,0,0)), fetcher.extractCellData(
                Column.builder().name("dateCell").type(Column.ColumnType.DATE).build(), cell));
    }

    @Test
    public void testDateTimeCellParsing() throws Exception {
        JsonObjectParser parser = new JsonObjectParser(new GsonFactory());
        String datetimeCell = "{\"effectiveFormat\":{\"numberFormat\":{\"pattern\":\"M/d/yyyy H:mm:ss\",\"type\":\"DATE_TIME\"}},\"effectiveValue\":{\"numberValue\":39720.239583333336}}";

        CellData cell = parser.parseAndClose(new ByteArrayInputStream(datetimeCell.getBytes()),
                Charset.defaultCharset(), CellData.class);

        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        assertEquals(Column.ColumnType.DATETIME, fetcher.extractColumnType(cell));
        assertEquals(LocalDateTime.of(
                LocalDate.of(2008, 9, 29), LocalTime.of(5,45,0,0)), fetcher.extractCellData(
                Column.builder().name("dateTimeCell").type(Column.ColumnType.DATETIME).build(), cell));
    }
}
