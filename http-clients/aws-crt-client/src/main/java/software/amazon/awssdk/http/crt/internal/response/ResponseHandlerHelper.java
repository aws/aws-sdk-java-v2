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

package software.amazon.awssdk.http.crt.internal.response;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * This is the helper class that contains common logic shared between {@link CrtResponseAdapter} and
 * {@link InputStreamAdaptingHttpStreamResponseHandler}.
 *
 * CRT connection will only be closed, i.e., not reused, in one of the following conditions:
 * 1. 5xx server error OR
 * 2. It fails to read the response OR
 * 3. the response stream is closed/aborted by the caller.
 */
@SdkInternalApi
public class ResponseHandlerHelper {

    private final SdkHttpResponse.Builder responseBuilder;
    private final HttpClientConnection connection;
    private boolean connectionClosed;
    private final Object lock = new Object();

    public ResponseHandlerHelper(SdkHttpResponse.Builder responseBuilder, HttpClientConnection connection) {
        this.responseBuilder = responseBuilder;
        this.connection = connection;
    }

    public void onResponseHeaders(int responseStatusCode, int headerType, HttpHeader[] nextHeaders) {
        if (headerType == HttpHeaderBlock.MAIN.getValue()) {
            for (HttpHeader h : nextHeaders) {
                responseBuilder.appendHeader(h.getName(), h.getValue());
            }
            responseBuilder.statusCode(responseStatusCode);
        }
    }

    /**
     * Release the connection back to the pool so that it can be reused.
     */
    public void releaseConnection(HttpStream stream) {
        synchronized (lock) {
            if (!connectionClosed) {
                connectionClosed = true;
                connection.close();
                stream.close();
            }
        }
    }

    public void incrementWindow(HttpStream stream, int windowSize) {
        synchronized (lock) {
            if (!connectionClosed) {
                stream.incrementWindow(windowSize);
            }
        }
    }

    /**
     * Close the connection completely
     */
    public void closeConnection(HttpStream stream) {
        synchronized (lock) {
            if (!connectionClosed) {
                connectionClosed = true;
                connection.shutdown();
                connection.close();
                stream.close();
            }
        }
    }

    public void cleanUpConnectionBasedOnStatusCode(HttpStream stream) {
        // always close the connection on a 5XX response code.
        if (HttpStatusFamily.of(responseBuilder.statusCode()) == HttpStatusFamily.SERVER_ERROR) {
            closeConnection(stream);
        } else {
            releaseConnection(stream);
        }
    }
}
