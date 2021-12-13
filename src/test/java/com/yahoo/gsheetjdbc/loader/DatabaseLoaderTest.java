/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.gsheetjdbc.schema.Column;
import com.yahoo.gsheetjdbc.schema.Table;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class DatabaseLoaderTest {

    private Table table = Table.builder()
            .tableName("MyTable")
            .schema("MySchema")
            .column(Column.builder()
                    .name("exampleText")
                    .type(Column.ColumnType.STRING)
                    .build())
            .column(Column.builder()
                    .name("exampleBoolean")
                    .type(Column.ColumnType.BOOLEAN)
                    .build())
            .column(Column.builder()
                    .name("exampleNumber")
                    .type(Column.ColumnType.NUMBER)
                    .build())
            .column(Column.builder()
                    .name("exampleDate")
                    .type(Column.ColumnType.DATE)
                    .build())
            .column(Column.builder()
                    .name("exampleDateTime")
                    .type(Column.ColumnType.DATETIME)
                    .build())
            .build();

    @Test
    public void testCreateTableSql() {
        DatabaseLoader loader = new DatabaseLoader("test");

        String expected = "CREATE TABLE IF NOT EXISTS \"MySchema\".\"MyTable\" (\"exampleText\" VARCHAR,\"exampleBoolean\" BOOLEAN,\"exampleNumber\" DOUBLE,\"exampleDate\" DATE,\"exampleDateTime\" TIMESTAMP);";
        assertEquals(expected, loader.generateTableCreationStatement(table, ""));
    }

    @Test
    public void testCreateSchemaSql() {
        DatabaseLoader loader = new DatabaseLoader("test");

        String expected = "CREATE SCHEMA IF NOT EXISTS \"MySchema\";";
        assertEquals(expected, loader.generateSchemaGenerationStatement(table));
    }

    @Test
    public void testTableRenameSql() {
        DatabaseLoader loader = new DatabaseLoader("test");

        String expected = "ALTER TABLE IF EXISTS \"MySchema\".\"MyTableFoo\" RENAME TO \"MySchema\".\"MyTableBar\"";
        assertEquals(expected, loader.generateTableRenameStatement(table, "Foo", "Bar"));
    }

    @Test
    public void testTableDropSql() {
        DatabaseLoader loader = new DatabaseLoader("test");

        String expected = "DROP TABLE IF EXISTS \"MySchema\".\"MyTableFoo\"";
        assertEquals(expected, loader.generateTableDropStatement(table, "Foo"));
    }

    @Test
    public void testInsertTableSql() {
        DatabaseLoader loader = new DatabaseLoader("test");

        String expected = "INSERT INTO \"MySchema\".\"MyTable\" (\"exampleText\",\"exampleBoolean\",\"exampleNumber\",\"exampleDate\",\"exampleDateTime\") VALUES (?,?,?,?,?);";
        assertEquals(expected, loader.generateTableInsertionStatement(table, ""));
    }

    @Test
    public void testJdbcUrl() {
        DatabaseLoader loader = new DatabaseLoader("test");
        assertEquals("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", loader.getH2URL());
    }

    @Test
    public void testLoad() throws Exception {
        List<Object> row1 = List.of("text", true, 1.0, LocalDate.of(1999, 1, 1),
                LocalDateTime.of(
                        LocalDate.of(1999, 1, 1),
                        LocalTime.of(0, 0, 0, 0)
                )
        );

        List<Object> row2 = List.of("text", false, 2.0, LocalDate.of(1999, 1, 1),
                LocalDateTime.of(
                        LocalDate.of(1999, 1, 1),
                        LocalTime.of(0, 0, 0, 0)
                )
        );

        DatabaseLoader loader = new DatabaseLoader("test");
        loader.refreshTempTable(table, List.of(row1, row2));
        loader.swapTables(table);

        PreparedStatement statement = null;
        try (Connection connection = loader.getConnection()) {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM \"MySchema\".\"MyTable\";");

            ResultSet result = statement.executeQuery();
            assertTrue(result.last());
            assertEquals(2, result.getInt(1));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            statement.close();
            loader.close();
        }
    }
}
