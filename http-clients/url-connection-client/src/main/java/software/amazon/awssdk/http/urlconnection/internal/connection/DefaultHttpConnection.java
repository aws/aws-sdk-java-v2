/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.urlconnection.internal.connection;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of {@link HttpConnection} that delegates calls to a {@link HttpURLConnection}.
 */
@SdkInternalApi
public class DefaultHttpConnection implements HttpConnection {
    private final HttpURLConnection connection;

    public DefaultHttpConnection(HttpURLConnection connection) {
        this.connection = connection;
    }

    @Override
    public void connect() {
        invokeSafely(connection::connect);
    }

    @Override
    public OutputStream getRequestStream() {
        return invokeSafely(connection::getOutputStream);
    }

    @Override
    public int getResponseCode() {
        return invokeSafely(connection::getResponseCode);
    }

    @Override
    public String getResponseMessage() {
        return invokeSafely(connection::getResponseMessage);
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return connection.getHeaderFields();
    }

    @Override
    public String getResponseHeader(String header) {
        return connection.getHeaderField(header);
    }

    @Override
    public InputStream getResponseStream() {
        return invokeSafely(connection::getInputStream);
    }

    @Override
    public InputStream getResponseErrorStream() {
        return connection.getErrorStream();
    }

    @Override
    public void disconnect() {
        invokeSafely(connection::disconnect);
    }
}
