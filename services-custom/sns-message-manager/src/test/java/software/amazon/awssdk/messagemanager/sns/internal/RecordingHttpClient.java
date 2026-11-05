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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Records every URI the manager asks to fetch and serves a body per request path, falling back to the default body.
 * A path can signal a latch when it is requested and hold its response until another latch is released.
 */
final class RecordingHttpClient implements SdkHttpClient {
    private final byte[] defaultBody;
    private final Map<String, byte[]> bodiesByPath = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch> requestedByPath = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch> holdByPath = new ConcurrentHashMap<>();
    private final List<URI> fetched = Collections.synchronizedList(new ArrayList<>());

    RecordingHttpClient(byte[] defaultBody) {
        this.defaultBody = defaultBody;
    }

    void serve(String path, byte[] body) {
        bodiesByPath.put(path, body);
    }

    void countDownWhenRequested(String path, CountDownLatch latch) {
        requestedByPath.put(path, latch);
    }

    void holdResponseUntil(String path, CountDownLatch latch) {
        holdByPath.put(path, latch);
    }

    List<URI> fetched() {
        synchronized (fetched) {
            return new ArrayList<>(fetched);
        }
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        URI uri = request.httpRequest().getUri();
        fetched.add(uri);
        byte[] body = bodiesByPath.getOrDefault(uri.getRawPath(), defaultBody);
        CountDownLatch requested = requestedByPath.get(uri.getRawPath());
        CountDownLatch hold = holdByPath.get(uri.getRawPath());

        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() throws IOException {
                if (requested != null) {
                    requested.countDown();
                }
                if (hold != null) {
                    awaitRelease(hold);
                }
                return HttpExecuteResponse.builder()
                                          .response(SdkHttpFullResponse.builder().statusCode(200).build())
                                          .responseBody(AbortableInputStream.create(new ByteArrayInputStream(body)))
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
        return "RecordingHttpClient";
    }

    private static void awaitRelease(CountDownLatch hold) throws IOException {
        try {
            if (!hold.await(10, TimeUnit.SECONDS)) {
                throw new IOException("Held response was not released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
