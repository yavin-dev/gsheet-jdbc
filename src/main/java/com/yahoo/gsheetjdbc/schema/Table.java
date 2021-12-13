/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.schema;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Represents a table in the H2 database / sheet in the Google spreadsheet.
 */
@Value
@Builder
public class Table {

    @NonNull
    private String schema;

    @NonNull
    private String tableName;

    @NonNull
    @Singular
    private List<Column> columns;

    public Column getColumn(int idx) {
        return columns.get(idx);
    }
}
