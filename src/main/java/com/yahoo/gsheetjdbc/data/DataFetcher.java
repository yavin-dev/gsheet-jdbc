/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.data;

import com.yahoo.gsheetjdbc.schema.Table;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Fetches a spreadsheet and parses it into a table schema and associated metadata.
 */
@FunctionalInterface
public interface DataFetcher {

    /**
     * Data fetcher result.
     */
    @Value
    @Builder
    public class Result {
        private Table schema;
        private List<List<Object>> data;
    }

    /**
     * Fetches a Google spreadsheet.
     * @param credentialFetcher Wraps credentials needed to access Google APIs.
     * @param documentId The Google document ID to fetch.
     * @param range The spreadsheet range: 'SheetName!A1:G11'
     * @param schema The schema name where data will be stored in the database.
     * @return A result object containg the table schema and associated data.
     */
    Result fetchDocument(CredentialFetcher credentialFetcher, String documentId, String range, String schema);
}
