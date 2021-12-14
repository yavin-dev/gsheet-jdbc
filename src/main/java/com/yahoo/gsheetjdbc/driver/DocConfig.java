/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.driver;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a parsed JDBC URL component for a single Google document.
 */
@Data
@Builder
public class DocConfig {
    private final String id;
    private final String range;
    private final String schema;
}
