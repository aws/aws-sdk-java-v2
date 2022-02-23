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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An abstract HTTP connection. Implemented with a {@link HttpURLConnection} in {@link DefaultHttpConnection}.
 */
@SdkInternalApi
public interface HttpConnection {
    /**
     * Invoke {@link HttpURLConnection#connect()}
     */
    void connect();

    /**
     * Invoke {@link HttpURLConnection#getOutputStream()}
     */
    OutputStream getRequestStream();

    /**
     * Invoke {@link HttpURLConnection#getResponseCode()}
     */
    int getResponseCode();

    /**
     * Invoke {@link HttpURLConnection#getResponseMessage()}
     */
    String getResponseMessage();

    /**
     * Invoke {@link HttpURLConnection#getHeaderFields()}
     */
    Map<String, List<String>> getResponseHeaders();

    /**
     * Invoke {@link HttpURLConnection#getHeaderField(String)}
     */
    String getResponseHeader(String header);

    /**
     * Invoke {@link HttpURLConnection#getInputStream()}
     */
    InputStream getResponseStream();

    /**
     * Invoke {@link HttpURLConnection#getErrorStream()}
     */
    InputStream getResponseErrorStream();

    /**
     * Invoke {@link HttpURLConnection#disconnect()}
     */
    void disconnect();
}
