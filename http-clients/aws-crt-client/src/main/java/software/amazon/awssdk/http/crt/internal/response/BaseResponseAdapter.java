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

import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;

/**
 * This is the base class that contains common logic shared between {@link CrtResponseAdapter} and
 * {@link InputStreamAdaptingHttpStreamResponseHandler}.
 *
 * CRT connection will only be closed, i.e., not reused, in one of the following conditions:
 * 1. 5xx server error OR
 * 2. It fails to read the response.
 */
@SdkInternalApi
public abstract class BaseResponseAdapter implements HttpStreamResponseHandler {
    private AtomicBoolean connectionClosed = new AtomicBoolean(false);

    /**
     * Release the connection back to the pool so that it can be reused.
     */
    public void releaseCrtConnection(HttpClientConnection connection, HttpStream stream) {
        if (connectionClosed.compareAndSet(false, true)) {
            connection.close();
            stream.close();
        }
    }

    /**
     * Close the connection completely
     */
    public void closeCrtConnection(HttpClientConnection connection, HttpStream stream) {
        if (connectionClosed.compareAndSet(false, true)) {
            connection.shutdown();
            connection.close();
            stream.close();
        }
    }
}
