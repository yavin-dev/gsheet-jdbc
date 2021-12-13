/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.loader;

import com.yahoo.gsheetjdbc.schema.Column;
import com.yahoo.gsheetjdbc.schema.Table;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads data into an H2 database.
 */
@Slf4j
public class DatabaseLoader implements Closeable {

    private static final String JDBC_DRIVER = "org.h2.Driver";

    private final String dbName;
    private Connection connection;
    private final String jdbcUrl;

    /**
     * Constructor.
     * @param dbName The name of the database to create for this loader.
     */
    public DatabaseLoader(String dbName) {
        this.dbName = dbName;

        jdbcUrl = getH2URL();
    }

    /**
     * Fetches a JDBC connection to the underlying H2 database.
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "", "");
    }

    String getH2URL() {
        return String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", dbName);
    }

    void createAndLoadTable(Table table, List<List<Object>> records, String tableSuffix) throws SQLException {
        executeStatement(generateTableCreationStatement(table, tableSuffix));

        for (List<Object> record : records) {
            executeStatement(generateTableInsertionStatement(table, tableSuffix), record);
        }
    }

    /**
     * Loads a temporary table with a set of newly fetched records.
     * @param table The table to reload.
     * @param records The records to load in.
     * @throws SQLException If an error occurs.
     */
    public void refreshTempTable(Table table, List<List<Object>> records) throws SQLException {
        //Make sure schema exists first.
        executeStatement(generateSchemaGenerationStatement(table));
        //Drop temp table.
        executeStatement(generateTableDropStatement(table, "Temp"));
        createAndLoadTable(table, records, "Temp");
    }

    /**
     * Swaps a newly loaded temporary table with the existing primary table.
     * @param table The table to swap.
     * @throws SQLException If an error occurs.
     */
    public synchronized void swapTables(Table table) throws SQLException {
        executeStatement(generateTableRenameStatement(table, "", "Old"));
        executeStatement(generateTableRenameStatement(table, "Temp", ""));
        executeStatement(generateTableDropStatement(table, "Old"));
    }

    String generateTableName(Table table, String suffix) {
        StringBuilder statement = new StringBuilder();
        statement.append("\"");
        statement.append(table.getSchema());
        statement.append("\".\"");
        statement.append(table.getTableName());
        if (suffix != null) {
            statement.append(suffix);
        }
        statement.append("\"");
        return statement.toString();
    }

    String generateTableRenameStatement(Table table, String fromSuffix, String toSuffix) {
        StringBuilder statement = new StringBuilder();
        statement.append("ALTER TABLE IF EXISTS ");
        statement.append(generateTableName(table, fromSuffix));
        statement.append(" RENAME TO ");
        statement.append(generateTableName(table, toSuffix));
        return statement.toString();
    }

    String generateTableDropStatement(Table table, String tableSuffix) {
        StringBuilder statement = new StringBuilder();
        statement.append("DROP TABLE IF EXISTS ");
        statement.append(generateTableName(table, tableSuffix));
        return statement.toString();
    }

    String generateSchemaGenerationStatement(Table table) {
        StringBuilder statement = new StringBuilder();
        statement.append("CREATE SCHEMA IF NOT EXISTS \"");
        statement.append(table.getSchema());
        statement.append("\";");

        return statement.toString();
    }

    String generateTableCreationStatement(Table table, String tableSuffix) {
        StringBuilder statement = new StringBuilder();
        statement.append("CREATE TABLE IF NOT EXISTS ");
        statement.append(generateTableName(table, tableSuffix));
        statement.append(" (");

        statement.append(table.getColumns().stream().map(
                column -> {
                    return "\"" + column.getName() + "\" " + getH2Type(column.getType());
                }
        ).collect(Collectors.joining(",")));

        statement.append(");");

        return statement.toString();
    }

    String generateTableInsertionStatement(Table table, String tableSuffix) {
        StringBuilder statement = new StringBuilder();
        statement.append("INSERT INTO ");

        statement.append(generateTableName(table, tableSuffix == null ? "" : tableSuffix));
        statement.append(" (");
        statement.append(table.getColumns().stream().map(
                column -> {
                    return "\"" + column.getName() + "\"";
                }
        ).collect(Collectors.joining(",")));

        statement.append(") VALUES (");

        statement.append(table.getColumns().stream().map((c) -> "?").collect(Collectors.joining(",")));

        statement.append(");");

        return statement.toString();
    }

    String getH2Type(Column.ColumnType columnType) {
        switch (columnType) {
            case DATE:
                return "DATE";
            case NUMBER:
                return "DOUBLE";
            case DATETIME:
                return "TIMESTAMP";
            case STRING:
                return "VARCHAR";
            case BOOLEAN:
                return "BOOLEAN";
            default:
                return "VARCHAR";
        }
    }

    private void executeStatement(String sql) throws SQLException {
        this.executeStatement(sql, List.of());
    }

    private void executeStatement(String sql, List<Object> arguments) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = getConnection();
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int idx = 1;
            for (Object obj : arguments) {
                statement.setObject(idx, obj);
                idx++;
            }
            long start = System.currentTimeMillis();
            statement.execute();
            long end = System.currentTimeMillis();

            log.debug("Executed SQL: {} Runtime: {}ms", sql, end - start);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            executeStatement("SHUTDOWN");
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
}
