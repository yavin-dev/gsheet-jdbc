/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.driver;

import com.yahoo.gsheetjdbc.data.CredentialFetcher;
import com.yahoo.gsheetjdbc.data.DataFetcher;
import com.yahoo.gsheetjdbc.data.GoogleServiceAccountCredentialFetcher;
import com.yahoo.gsheetjdbc.data.GoogleSheetsDataFetcher;
import com.yahoo.gsheetjdbc.loader.DatabaseLoader;

import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC Driver for Google Sheets.
 */
@Slf4j
public class Driver implements java.sql.Driver {

    private static final String URL_PREFIX = "jdbc:gsheet:";

    //https://developers.google.com/docs/api/how-tos/overview#document_id
    private static final String DOC_ID_REGEX = "([a-zA-Z0-9-_]+)";
    private static final String SCHEMA_NAME_REGEX = "([a-zA-Z][a-zA-Z0-9_]*)";

    //doc=(id=abcdefg,range=MySheet!A1:G11)
    private static final String DOC_REGEX = "doc=\\(id=([a-zA-Z0-9-_]+),range=([^/!]+![a-zA-Z]+[0-9]+:[a-zA-Z]+[0-9]+)\\)";

    // Multi Sheet
    // jdbc:gsheet://doc=(id=abcdefg,range=Sheet1!A1:G11),doc=(id=xyz123,range=Sheet2!A1:G11)/MySchema
    // OR Single Sheet
    // jdbc:gsheet://doc=(id=abcdefg,range=Sheet1!A1:G11)/MySchema
    private static final String URL_REGEX =
            "^" + URL_PREFIX + "//" + DOC_REGEX + "((," + DOC_REGEX + ")*)/" + SCHEMA_NAME_REGEX + "$";

    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final Pattern DOC_PATTERN = Pattern.compile("," + DOC_REGEX);

    static {
        Driver driver = new Driver();
        try {
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            log.error("Unable to register driver: " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    Map<DocConfig, String> documents = new ConcurrentHashMap<>();
    private final DatabaseLoader loader;

    public Driver() {
        loader = new DatabaseLoader("gsheets");
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        GoogleServiceAccountCredentialFetcher credentialFetcher = new GoogleServiceAccountCredentialFetcher();
        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        Set<DocConfig> configs = parseUrl(url);

        synchronized (this) {
            for (DocConfig config : configs) {
                String lastModified = fetcher.fetchLastUpdateTime(config.getId(), credentialFetcher);

                if (!documents.containsKey(config)) {
                    fetchAndLoad(config, credentialFetcher, loader);
                    documents.put(config, lastModified);
                } else {
                    String previouslyModified = documents.get(config);

                    if (previouslyModified == null || !previouslyModified.equals(lastModified)) {
                        fetchAndLoad(config, credentialFetcher, loader);
                        documents.put(config, lastModified);
                    }

                }
            }
            return loader.getConnection();
        }
    }

    private void fetchAndLoad(DocConfig document, CredentialFetcher credentialFetcher, DatabaseLoader loader) {
        GoogleSheetsDataFetcher fetcher = new GoogleSheetsDataFetcher();

        DataFetcher.Result result = fetcher.fetchDocument(credentialFetcher, document.getSchema(), document.getId(),
                document.getRange());

        try {
            loader.refreshTempTable(result.getSchema(), result.getData());
            loader.swapTables(result.getSchema());
        } catch (SQLException e) {
            log.error("Unable to reload table: {} {}", document, e.getMessage());
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (url == null) {
            return false;
        }

        return url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    public static Set<DocConfig> parseUrl(String url) throws SQLException {
        Matcher urlMatcher = URL_PATTERN.matcher(url);

        if (! urlMatcher.find()) {
            throw new SQLException("Invalid JDBC URL : " + url);
        }

        int numberOfGroups = urlMatcher.groupCount();

        if (numberOfGroups != 7) {
            throw new SQLException("Invalid JDBC URL : " + url);
        }

        Set<DocConfig> results = new LinkedHashSet<>();
        String schema = urlMatcher.group(numberOfGroups);
        String range = URLDecoder.decode(urlMatcher.group(2), Charset.defaultCharset());

        results.add(DocConfig.builder()
                .schema(schema)
                .range(range)
                .id(urlMatcher.group(1))
                .build());

        Matcher idMatcher = DOC_PATTERN.matcher(urlMatcher.group(3));

        while (idMatcher.find()) {
            range = URLDecoder.decode(idMatcher.group(2), Charset.defaultCharset());

            results.add(DocConfig.builder()
                    .schema(schema)
                    .range(range)
                    .id(idMatcher.group(1))
                    .build());
        }

        return results;
    }
}
