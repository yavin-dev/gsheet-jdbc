/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Tag("RequiresCredentials")
public class DriverIT {
    @Test
    public void testSingleSheet() throws Exception {
        Class.forName("com.yahoo.gsheetjdbc.driver.Driver");
        String url = "jdbc:gsheet://doc=(id=1Is6tUtJxhmjN8f4nqIYq-6n7FcW17y8glK1F9EsHzr4,range=Class%20Data!A1:I31)/MySchema";
        Connection connection = DriverManager.getConnection(url, "", "");

        PreparedStatement statement = connection.prepareStatement("Select * from \"MySchema\".\"Class Data\" WHERE \"Student Name\"='Alexandra' LIMIT 1;");

        ResultSet result = statement.executeQuery();
        assertEquals(true, result.last());
        assertEquals("Alexandra", result.getString(1));
        assertEquals(100.0, result.getDouble(7));
        connection.close();
    }

    @Test
    public void testMultipleSheets() throws Exception {
        Class.forName("com.yahoo.gsheetjdbc.driver.Driver");
        String url = "jdbc:gsheet://doc=(id=1Is6tUtJxhmjN8f4nqIYq-6n7FcW17y8glK1F9EsHzr4,range=Class%20Data!A1:I31),doc=(id=1Is6tUtJxhmjN8f4nqIYq-6n7FcW17y8glK1F9EsHzr4,range=Drum%20Inventory!A1:C5)/MySchema";
        Connection connection = DriverManager.getConnection(url, "", "");

        PreparedStatement statement = connection.prepareStatement("Select * from \"MySchema\".\"Class Data\" WHERE \"Student Name\"='Alexandra' LIMIT 1;");

        ResultSet result = statement.executeQuery();
        assertEquals(true, result.last());
        assertEquals("Alexandra", result.getString(1));
        assertEquals(100.0, result.getDouble(7));


        statement = connection.prepareStatement("Select SUM(\"Price\") AS total from \"MySchema\".\"Drum Inventory\";");

        result = statement.executeQuery();
        assertEquals(true, result.last());
        assertEquals(1020, result.getDouble(1));
        connection.close();
    }

    @Test
    public void testDriverReload() throws Exception {
        String url = "jdbc:gsheet://doc=(id=1Is6tUtJxhmjN8f4nqIYq-6n7FcW17y8glK1F9EsHzr4,range=Class%20Data!A1:I31)/MySchema";

        //Load the data...
        Connection connection = DriverManager.getConnection(url, "", "");
        connection.close();

        Driver driver = (Driver) DriverManager.getDriver(url);
        assertNotNull(driver);

        //Clear out the driver data...
        Set<Runnable> todo = new LinkedHashSet<>();
        driver.documents.forEach((doc, timestamp) -> {
            todo.add(() -> driver.documents.put(doc, ""));
        });
        todo.stream().forEach((fun -> fun.run()));

        //Reload the data...
        connection = DriverManager.getConnection(url, "", "");

        PreparedStatement statement = connection.prepareStatement("Select \"Earnings ¥\" from \"MySchema\".\"Class Data\" WHERE \"Student Name\"='Alexandra' LIMIT 1;");

        ResultSet result = statement.executeQuery();
        assertEquals(true, result.last());
        assertEquals(100.0, result.getDouble(1));
        connection.close();
    }
}
