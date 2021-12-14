/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.schema;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a column in the H2 database / Google spreadsheet.
 */
@Value
@Builder
public class Column {
    public enum ColumnType {
        DATE,
        DATETIME,
        STRING,
        BOOLEAN,
        NUMBER
    }

    @NonNull
    private ColumnType type;

    @NonNull
    private String name;
}
