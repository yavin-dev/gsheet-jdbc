/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.utils;

import java.time.LocalDateTime;
import java.time.Month;

/**
 * Utility functions for Data parsing.
 */
public class DateUtils {

    /**
     * Google serial number format epoch starts on 12/30/1899.
     */
    final static public LocalDateTime GOOGLE_EPOCH_REFERENCE = LocalDateTime.of( 1899 , Month.DECEMBER , 30 , 0, 0);

    /**
     * Converts a Google serial data into a LocalDateTime.
     * @param serialDate Serial date format (as a double).
     * @return The equivalent LocalDateTime.
     */
    public static LocalDateTime convert(Double serialDate) {
        return GOOGLE_EPOCH_REFERENCE.plusSeconds((long) (86400 * serialDate));
    }

    /**
     * Converts a Google serial data into a LocalDateTime.
     * @param serialDate Serial date format (as an object).
     * @return The equivalent LocalDateTime.
     */
    public static LocalDateTime convert(Object serialDate) {
        if (Double.class.isAssignableFrom(serialDate.getClass())) {
            return convert((Double) serialDate);
        }

        return convert(Double.valueOf(String.valueOf(serialDate)));
    }
}
