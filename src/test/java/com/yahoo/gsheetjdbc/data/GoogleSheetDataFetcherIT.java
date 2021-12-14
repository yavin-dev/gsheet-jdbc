/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.gsheetjdbc.schema.Column;
import com.yahoo.gsheetjdbc.schema.Table;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Tag("RequiresCredentials")
public class GoogleSheetDataFetcherIT {
    @Test
    public void testLastModificationDate() throws Exception {
        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        fetcher.fetchLastUpdateTime("1Is6tUtJxhmjN8f4nqIYq-6n7FcW17y8glK1F9EsHzr4",
                new GoogleServiceAccountCredentialFetcher());
    }

    @Test
    public void testFetcher() {
        String documentId = "1Is6tUtJxhmjN8f4nqIYq-6n7FcW17y8glK1F9EsHzr4";
        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        DataFetcher.Result result =
                fetcher.fetchDocumentSheet(new GoogleServiceAccountCredentialFetcher(), "Test", documentId, "Class Data!A1:I31");

        Table schema = result.getSchema();

        assertEquals(Table.builder()
                .schema("Test")
                .tableName("Class Data")
                .column(Column.builder().name("Student Name").type(Column.ColumnType.STRING).build())
                .column(Column.builder().name("Gender").type(Column.ColumnType.STRING).build())
                .column(Column.builder().name("Class Level").type(Column.ColumnType.STRING).build())
                .column(Column.builder().name("Home State").type(Column.ColumnType.STRING).build())
                .column(Column.builder().name("Major").type(Column.ColumnType.STRING).build())
                .column(Column.builder().name("Extracurricular Activity").type(Column.ColumnType.STRING).build())
                .column(Column.builder().name("Earnings").type(Column.ColumnType.NUMBER).build())
                .column(Column.builder().name("Date").type(Column.ColumnType.DATETIME).build())
                .column(Column.builder().name("Formula").type(Column.ColumnType.NUMBER).build())
                .build(), schema);

        List<List<Object>> data = result.getData();

        assertEquals("Alexandra", data.get(0).get(0));
        assertEquals("Female", data.get(0).get(1));
        assertEquals("4. Senior", data.get(0).get(2));
        assertEquals("CA", data.get(0).get(3));
        assertEquals("English", data.get(0).get(4));
        assertEquals("Drama Club", data.get(0).get(5));
        assertEquals(100.0, data.get(0).get(6));
        assertEquals(LocalDateTime.of(
                LocalDate.of(2021, 10, 7),
                LocalTime.of(3, 0, 3, 0)
        ), data.get(0).get(7));
        assertEquals(4.0, data.get(0).get(8));
    }
}
