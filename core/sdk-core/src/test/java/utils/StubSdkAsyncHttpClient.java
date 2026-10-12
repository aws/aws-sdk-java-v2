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

package utils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * A minimal in-memory {@link SdkAsyncHttpClient} for sdk-core tests that do not need a real transport: it immediately delivers
 * a response with a configurable status code and an empty body to the response handler, without touching the network. Tests
 * that induce a timeout via a slow response handler or interceptor only need the transport to respond promptly with the right
 * status (200 vs 5xx) so the success or error path is selected.
 */
public final class StubSdkAsyncHttpClient implements SdkAsyncHttpClient {

    /**
     * A small deliberate transport latency. The real async HTTP client talking to a WireMock server (which this stub
     * replaced) always took some milliseconds to deliver a response. Tests that assert an API call timeout fires while a
     * response handler sleeps for exactly the timeout duration depend on that latency to push the total elapsed time past
     * the timeout, so the stub reproduces it rather than responding instantly.
     */
    private static final long RESPONSE_LATENCY_MILLIS = 50;

    private final int statusCode;

    private StubSdkAsyncHttpClient(int statusCode) {
        this.statusCode = statusCode;
    }

    public static StubSdkAsyncHttpClient create() {
        return new StubSdkAsyncHttpClient(200);
    }

    public static StubSdkAsyncHttpClient create(int statusCode) {
        return new StubSdkAsyncHttpClient(statusCode);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        SdkAsyncHttpResponseHandler responseHandler = request.responseHandler();
        simulateTransportLatency();
        responseHandler.onHeaders(SdkHttpResponse.builder().statusCode(statusCode).build());
        SimplePublisher<ByteBuffer> bodyPublisher = new SimplePublisher<>();
        responseHandler.onStream(bodyPublisher);
        bodyPublisher.complete();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
    }

    @Override
    public String clientName() {
        return "StubAsync";
    }

    private static void simulateTransportLatency() {
        try {
            Thread.sleep(RESPONSE_LATENCY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
