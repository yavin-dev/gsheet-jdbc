/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

public class DriverParserTest {

    @Test
    public void testValidUrlWithSingleDoc() throws Exception {
        String url = "jdbc:gsheet://doc=(id=docId,range=MySheet!A1:G6)/schemaName";

        Set<DocConfig> configs = Driver.parseUrl(url);

        assertEquals(1, configs.size());
        DocConfig config = configs.iterator().next();

        assertEquals("schemaName", config.getSchema());
        assertEquals("MySheet!A1:G6", config.getRange());
        assertEquals("docId", config.getId());
    }

    @Test
    public void testValidUrlWithMultipleDocs() throws Exception {
        String url = "jdbc:gsheet://doc=(id=abcdefg,range=Sheet1!A1:G11),"
            + "doc=(id=xyz123,range=Sheet2!A1:G11),"
            + "doc=(id=ffff,range=Sheet3!A1:G11)"
            + "/MySchema";

        Set<DocConfig> configs = Driver.parseUrl(url);

        assertEquals(3, configs.size());
        Iterator<DocConfig> configIt = configs.iterator();

        DocConfig config1 = configIt.next();
        DocConfig config2 = configIt.next();
        DocConfig config3 = configIt.next();

        assertEquals("MySchema", config1.getSchema());
        assertEquals("Sheet1!A1:G11", config1.getRange());
        assertEquals("abcdefg", config1.getId());

        assertEquals("MySchema", config2.getSchema());
        assertEquals("Sheet2!A1:G11", config2.getRange());
        assertEquals("xyz123", config2.getId());

        assertEquals("MySchema", config3.getSchema());
        assertEquals("Sheet3!A1:G11", config3.getRange());
        assertEquals("ffff", config3.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql",
            "jdbc:gsheet://123",
            "jdbc:gsheet://123/A1:G6",
            "jdbc:gsheet://123/A1",
            "jdbc:gsheet://123/1A:G6",
            "jdbc:gsheet://123/1A:G6/Schema",
            "jdbc:gsheet://"
    })
    public void testInvalidUrls(String url) throws Exception {
        assertThrows(SQLException.class, () -> Driver.parseUrl(url));
    }
}
