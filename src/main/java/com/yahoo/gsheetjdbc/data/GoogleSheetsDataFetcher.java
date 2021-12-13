/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.data;

import com.yahoo.gsheetjdbc.schema.Column;
import com.yahoo.gsheetjdbc.schema.Table;
import com.yahoo.gsheetjdbc.utils.DateUtils;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.auth.http.HttpCredentialsAdapter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Concrete implementation of the DataFetcher.
 */
@Slf4j
public class GoogleSheetsDataFetcher implements DataFetcher {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APP_NAME = "GSheet JDBC Driver";

    @Override
    public Result fetchDocument(CredentialFetcher credentialFetcher, String schema, String document, String range) {
        try {
            Spreadsheet spreadsheet = fetchSpreadsheet(document, range, credentialFetcher);

            if (spreadsheet == null || spreadsheet.getSheets() == null || spreadsheet.getSheets().size() != 1) {
                String message = "No spreadsheets returned from server.";
                log.error(message);
                throw new IllegalStateException(message);
            }

            Sheet sheet = spreadsheet.getSheets().get(0);
            Table table = extractTableSchema(sheet, schema);
            List<List<Object>> data = extractSheetData(table, sheet);

            return Result.builder()
                    .schema(table)
                    .data(data)
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Fetches the last time a given document ID was modified using Drive API.
     * @param documentId The Google document ID.
     * @param credentialFetcher Google API credentials
     * @return A string representing the timestamp of the last document modification.
     */
    public String fetchLastUpdateTime(
            String documentId,
            CredentialFetcher credentialFetcher
    ) {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            Drive service = new Drive.Builder(httpTransport, JSON_FACTORY,
                    new HttpCredentialsAdapter(credentialFetcher.getCredentials()))
                    .setApplicationName(APP_NAME)
                    .build();

            DateTime modifiedDate = service.files().get(documentId)
                    .setFields("modifiedTime")
                    .execute().getModifiedTime();

            if (modifiedDate == null) {
                String message = "Server did not return document modification time";
                log.error(message);
                throw new IllegalStateException(message);
            }

            return modifiedDate.toString();
        } catch (IOException | GeneralSecurityException e) {
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    Spreadsheet fetchSpreadsheet(
            String documentId,
            String range,
            CredentialFetcher credentialFetcher
    ) throws IOException, GeneralSecurityException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY,
                new HttpCredentialsAdapter(credentialFetcher.getCredentials()))
                .setApplicationName(APP_NAME)
                .build();

        Spreadsheet spreadsheet = service.spreadsheets()
                .get(documentId)
                .setRanges(List.of(range))
                .setFields("sheets(data(rowData(values(effectiveValue,effectiveFormat(numberFormat))))"
                        + ",properties(title))")
                .setIncludeGridData(true)
                .execute();

        return spreadsheet;
    }

    String extractTitle(Sheet sheet) {
        if (sheet.getProperties() == null
                || sheet.getProperties().getTitle() == null
                || sheet.getProperties().getTitle().isEmpty()
                || sheet.getProperties().getTitle().length() > 256
        ) {
            String message = "Sheet title is missing or invalid title.";
            log.error(message);
            throw new IllegalStateException(message);
        }

        return sheet.getProperties().getTitle();
    }

    String extractColumn(CellData headerCell) {
        if (headerCell.getEffectiveValue().getStringValue() == null
                || headerCell.getEffectiveValue().getStringValue().isEmpty()
                || headerCell.getEffectiveValue().getStringValue().length() > 256
        ) {
            String message = "Header row must contain all string values.";
            log.error(message);
            throw new IllegalStateException(message);
        }

        return headerCell.getEffectiveValue().getStringValue();
    }

    Table extractTableSchema(Sheet sheet, String schema) {
        if (sheet == null
                || sheet.getData() == null
                || sheet.getData().size() == 0
                || sheet.getData().get(0) == null
                || sheet.getData().get(0).getRowData() == null
                || sheet.getData().get(0).getRowData().size() < 2) {
            String message = "Google sheets require at least two rows to determine the schema.";
            log.error(message);
            throw new IllegalStateException(message);
        }

        GridData gridData = sheet.getData().get(0);

        String tableName = extractTitle(sheet);

        Table.TableBuilder schemaBuilder = Table.builder();
        schemaBuilder.tableName(tableName);
        schemaBuilder.schema(schema);

        int startRow = 0;
        int startColumn = 0;

        RowData headerRow = gridData.getRowData().get(startRow);
        RowData firstDataRow = gridData.getRowData().get(startRow + 1);
        for (int column = startColumn; column < headerRow.getValues().size(); column++) {
            CellData headerCell = headerRow.getValues().get(column);
            CellData dataCell = firstDataRow.getValues().get(column);

            //Done processing columns...
            if (headerCell.getEffectiveValue() == null) {
                break;
            }

            String columnName = extractColumn(headerCell);

            schemaBuilder.column(Column.builder()
                    .name(columnName)
                    .type(extractColumnType(dataCell))
                    .build());
        }

        Table table = schemaBuilder.build();

        if (table.getColumns().size() == 0) {
            String message = "Spreadsheet is missing header row starting at row 0.";
            log.error(message);
            throw new IllegalStateException(message);
        }
        return table;
    }

    List<List<Object>> extractSheetData(Table table, Sheet sheet) {
        List<List<Object>> results = new ArrayList<>();
        GridData gridData = sheet.getData().get(0);
        int startRow = 1;

        for (int row = startRow; row < gridData.getRowData().size(); row++) {
                List<Object> rowResults = new ArrayList<>();
                RowData rowData = gridData.getRowData().get(row);

                if (rowData.getValues().size() < table.getColumns().size()) {
                    //Can't process this row.
                    break;
                }
                int columnIndex = 0;
                for (Column column: table.getColumns()) {
                    CellData cellData = rowData.getValues().get(columnIndex);

                    if (cellData.getEffectiveValue() == null) {
                        rowResults.add(null);
                    } else {
                        rowResults.add(extractCellData(column, cellData));
                    }
                    columnIndex++;
                }

                if (rowResults.stream().allMatch((obj) -> obj == null)) {
                    //first empty row.
                    break;
                }

                results.add(rowResults);
        }
        return results;
    }

    Column.ColumnType extractColumnType(CellData cellData) {
        if (cellData.getEffectiveFormat() != null) {
            if (cellData.getEffectiveFormat().getNumberFormat() != null) {
                NumberFormat numberFormat = cellData.getEffectiveFormat().getNumberFormat();

                if (numberFormat.getType().equals("DATE")) {
                    return Column.ColumnType.DATE;
                } else if (numberFormat.getType().equals("DATE_TIME")) {
                    return Column.ColumnType.DATETIME;
                } else {
                    return Column.ColumnType.NUMBER;
                }
            }
        }
        if (cellData.getEffectiveValue() != null) {
            ExtendedValue value = cellData.getEffectiveValue();
            if (value.getBoolValue() != null) {
                return Column.ColumnType.BOOLEAN;
            } else if (value.getNumberValue() != null) {
                return Column.ColumnType.NUMBER;
            }
        }

        return Column.ColumnType.STRING;
    }

    Object extractCellData(Column column, CellData cellData) {
        Collection<Object> values = cellData.getEffectiveValue().values();

        if (values == null || values.size() != 1) {
            String message = String.format("Invalid value %s for column %s", cellData.getEffectiveValue(),
                    column.getName());
            log.error(message);
            throw new IllegalStateException(message);
        }

        Object value = values.iterator().next();

        if (column.getType().equals(Column.ColumnType.DATE) || column.getType().equals(Column.ColumnType.DATETIME)) {
            return DateUtils.convert(value);
        }
        return value;
    }
}
