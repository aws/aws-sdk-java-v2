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

import java.io.ByteArrayInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * A minimal in-memory {@link SdkHttpClient} for sdk-core tests that do not need a real transport: it immediately returns a
 * response with a configurable status code and an empty body, without touching the network. Tests that induce a timeout via a
 * slow response handler or interceptor only need the transport to return promptly with the right status (200 vs 5xx) so the
 * success or error path is selected.
 */
public final class StubSdkHttpClient implements SdkHttpClient {

    /**
     * A small deliberate transport latency. The real HTTP client talking to a WireMock server (which this stub replaced)
     * always took some milliseconds to return a response. Tests that assert an API call timeout fires while a response
     * handler sleeps for exactly the timeout duration depend on that latency to push the total elapsed time past the
     * timeout, so the stub reproduces it rather than returning instantly.
     */
    private static final long RESPONSE_LATENCY_MILLIS = 50;

    private final int statusCode;

    private StubSdkHttpClient(int statusCode) {
        this.statusCode = statusCode;
    }

    public static StubSdkHttpClient create() {
        return new StubSdkHttpClient(200);
    }

    public static StubSdkHttpClient create(int statusCode) {
        return new StubSdkHttpClient(statusCode);
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                simulateTransportLatency();
                return HttpExecuteResponse.builder()
                                          .response(SdkHttpResponse.builder().statusCode(statusCode).build())
                                          .responseBody(AbortableInputStream.create(new ByteArrayInputStream(new byte[0])))
                                          .build();
            }

            @Override
            public void abort() {
            }
        };
    }

    @Override
    public void close() {
    }

    @Override
    public String clientName() {
        return "Stub";
    }

    private static void simulateTransportLatency() {
        try {
            Thread.sleep(RESPONSE_LATENCY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
