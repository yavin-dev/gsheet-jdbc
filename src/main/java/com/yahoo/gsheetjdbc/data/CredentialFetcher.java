/**
 * Copyright 2021, Yahoo Holdings Inc.
 * Licensed under the terms of the MIT license. See accompanying LICENSE.md file for terms.
 */
package com.yahoo.gsheetjdbc.data;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;

/**
 * Fetches credentials for API access to Google documents.
 */
@FunctionalInterface
public interface CredentialFetcher {

    /**
     * Fetch credentials.
     * @return A set of credentials to access the API.
     * @throws IOException If there is an exception fetching credentials.
     */
    GoogleCredentials getCredentials() throws IOException;
}
