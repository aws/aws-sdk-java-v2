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
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * This is the helper class that contains common logic shared between {@link CrtResponseAdapter} and
 * {@link InputStreamAdaptingHttpStreamResponseHandler}.
 *
 */
@SdkInternalApi
public class ResponseHandlerHelper {

    private final SdkHttpResponse.Builder responseBuilder;
    private HttpStreamBase stream;
    private boolean streamClosed;
    private final Object streamLock = new Object();

    public ResponseHandlerHelper(SdkHttpResponse.Builder responseBuilder) {
        this.responseBuilder = responseBuilder;
    }

    /**
     * Set the stream reference and activate it as soon as it is acquired from the pool.
     *
     * <p>Activating immediately ensures that any subsequent {@code closeConnection()} call will
     * properly trigger {@code onResponseComplete} in the CRT native layer, which is required for
     * {@code Http1StreamManager} to release the connection slot back to the pool. Without prior
     * activation, {@code cancel()} + {@code close()} releases the native stream handle without
     * triggering callbacks, permanently leaking the connection slot.
     *
     * <p>{@code activate()} is idempotent per the CRT contract — safe to call even if
     * {@code Http1StreamManager} has already activated the stream.
     */
    public void onAcquireStream(HttpStreamBase stream) {
        synchronized (streamLock) {
            if (this.stream == null) {
                this.stream = stream;
                if (this.stream != null) {
                    this.stream.activate();
                }
                // closeConnection() was requested before the stream was acquired; close it now.
                if (streamClosed && this.stream != null) {
                    this.stream.cancel();
                    this.stream.close();
                }
            }
        }
    }

    public void onResponseHeaders(HttpStreamBase stream, int responseStatusCode, int headerType, HttpHeader[] nextHeaders) {
        onAcquireStream(stream);
        if (headerType == HttpHeaderBlock.MAIN.getValue()) {
            for (HttpHeader h : nextHeaders) {
                responseBuilder.appendHeader(h.getName(), h.getValue());
            }
            responseBuilder.statusCode(responseStatusCode);
        }
    }

    public void incrementWindow(int windowSize) {
        synchronized (streamLock) {
            if (!streamClosed && stream != null) {
                stream.incrementWindow(windowSize);
            }
        }
    }

    /**
     * Release the connection back to the pool so that it may be reused. This should be called when the request
     * completes successfully and the response has been fully consumed.
     */
    public void releaseConnection() {
        synchronized (streamLock) {
            if (!streamClosed && stream != null) {
                streamClosed = true;
                stream.close();
            }
        }
    }

    /**
     * Cancel and close the stream, forcing the underlying connection to shut down rather than be returned to the
     * connection pool. This should be called on error paths or when the stream is aborted before the response is
     * fully consumed.
     *
     * <p>Calls {@code activate()} before {@code cancel()} to ensure the CRT native layer will deliver
     * {@code onResponseComplete}. This is critical for {@code Http1StreamManager} to release the
     * connection slot back to the pool. {@code activate()} is idempotent — calling it on an
     * already-activated stream is safe.
     */
    public void closeConnection() {
        synchronized (streamLock) {
            if (!streamClosed) {
                streamClosed = true;
                if (stream != null) {
                    stream.activate();
                    stream.cancel();
                    stream.close();
                }
            }
        }
    }
}
