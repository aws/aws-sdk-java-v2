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

    public void onResponseHeaders(HttpStreamBase stream, int responseStatusCode, int headerType, HttpHeader[] nextHeaders) {
        synchronized (streamLock) {
            if (this.stream == null) {
                this.stream = stream;
            }
        }
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

    public void closeStream() {
        synchronized (streamLock) {
            if (!streamClosed && stream != null) {
                streamClosed = true;
                stream.close();
            }
        }
    }
}
