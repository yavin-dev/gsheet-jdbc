/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.data;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Fetches Google service account credentials.
 */
public class GoogleServiceAccountCredentialFetcher implements CredentialFetcher {

    public static final String CREDENTIAL_ENVIRONMENT_VAR = "GSHEET_JDBC_CREDENTIALS";

    private final String credentialJson;

    /**
     * Constructor.
     */
    public GoogleServiceAccountCredentialFetcher() {
        this(System.getenv(CREDENTIAL_ENVIRONMENT_VAR));
    }

    /**
     * Constructor.
     * @param credentialJson A string containing service account credential file JSON blob.
     */
    public GoogleServiceAccountCredentialFetcher(String credentialJson) {
        if (credentialJson == null || credentialJson.isEmpty()) {
            throw new IllegalArgumentException(CREDENTIAL_ENVIRONMENT_VAR
                    + " must be set as an environment "
                    + "variable with the correct credential JSON");
        }
        this.credentialJson = credentialJson;
    }

    @Override
    public GoogleCredentials getCredentials() throws IOException {
        return GoogleCredentials.fromStream(new ByteArrayInputStream(credentialJson.getBytes()))
                .createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY, DriveScopes.DRIVE_METADATA_READONLY));
    }
}
