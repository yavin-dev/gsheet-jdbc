/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateUtilsTest {

    @Test
    public void testStartDate() {
        assertEquals(LocalDateTime.of(LocalDate.of(1899, 12, 30), LocalTime.of(0, 0, 0, 0)),
                DateUtils.convert(0));
    }

    @Test
    public void testRandomDate() {
        assertEquals(LocalDateTime.of(LocalDate.of(2008,  9,  29), LocalTime.of(5, 45, 0, 0)),
                DateUtils.convert(39720.239583333336));
    }
}
