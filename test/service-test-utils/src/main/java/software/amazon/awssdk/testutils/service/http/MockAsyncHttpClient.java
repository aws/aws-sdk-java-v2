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

package software.amazon.awssdk.testutils.service.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Mock implementation of {@link SdkAsyncHttpClient}.
 */
public final class MockAsyncHttpClient implements SdkAsyncHttpClient, MockHttpClient {

    private static final Duration DEFAULT_DURATION = Duration.ofMillis(50);
    private final List<SdkHttpRequest> capturedRequests = new ArrayList<>();
    private final List<Pair<HttpExecuteResponse, Duration>> responses = new LinkedList<>();
    private final AtomicInteger responseIndex = new AtomicInteger(0);
    private final ExecutorService executor;
    private Integer asyncRequestBodyLength;
    private byte[] streamingPayload;

    public MockAsyncHttpClient() {
        this.executor = Executors.newFixedThreadPool(3);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        capturedRequests.add(request.request());

        int index = responseIndex.getAndIncrement() % responses.size();
        HttpExecuteResponse nextResponse = responses.get(index).left();
        byte[] content = nextResponse.responseBody().map(p -> invokeSafely(() -> IoUtils.toByteArray(p)))
                                     .orElseGet(() -> new byte[0]);

        request.responseHandler().onHeaders(nextResponse.httpResponse());
        CompletableFuture.runAsync(() -> request.responseHandler().onStream(new ResponsePublisher(content, index)), executor);

        if (asyncRequestBodyLength != null && asyncRequestBodyLength > 0) {
            captureStreamingPayload(request.requestContentPublisher());
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public void reset() {
        this.capturedRequests.clear();
        this.responses.clear();
        this.responseIndex.set(0);
    }

    @Override
    public List<SdkHttpRequest> getRequests() {
        return Collections.unmodifiableList(capturedRequests);
    }

    @Override
    public SdkHttpRequest getLastRequest() {
        if (capturedRequests.isEmpty()) {
            throw new IllegalStateException("No requests were captured by the mock");
        }
        return capturedRequests.get(capturedRequests.size() - 1);
    }

    @Override
    public void stubNextResponse(HttpExecuteResponse nextResponse) {
        this.responses.clear();
        this.responses.add(Pair.of(nextResponse, DEFAULT_DURATION));
        this.responseIndex.set(0);
    }

    @Override
    public void stubNextResponse(HttpExecuteResponse nextResponse, Duration delay) {
        this.responses.clear();
        this.responses.add(Pair.of(nextResponse, delay));
        this.responseIndex.set(0);
    }

    @Override
    public void stubResponses(Pair<HttpExecuteResponse, Duration>... responses) {
        this.responses.clear();
        this.responses.addAll(Arrays.asList(responses));
        this.responseIndex.set(0);
    }

    @Override
    public void stubResponses(HttpExecuteResponse... responses) {
        this.responses.clear();
        this.responses.addAll(Arrays.stream(responses).map(r -> Pair.of(r, DEFAULT_DURATION)).collect(Collectors.toList()));
        this.responseIndex.set(0);
    }

    /**
     * Enable capturing the streaming payload by setting the length of the AsyncRequestBody.
     */
    public void setAsyncRequestBodyLength(int asyncRequestBodyLength) {
        this.asyncRequestBodyLength = asyncRequestBodyLength;
    }

    private void captureStreamingPayload(SdkHttpContentPublisher publisher) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(asyncRequestBodyLength);
        Subscriber<ByteBuffer> subscriber = new CapturingSubscriber(byteBuffer);
        publisher.subscribe(subscriber);
        streamingPayload = byteBuffer.array();
    }

    /**
     * Returns the streaming payload byte array, if the asyncRequestBodyLength was set correctly. Otherwise, returns empty
     * Optional.
     */
    public Optional<byte[]> getStreamingPayload() {
        return streamingPayload != null ? Optional.of(streamingPayload.clone()) : Optional.empty();
    }

    private final class ResponsePublisher implements SdkHttpContentPublisher {
        private final byte[] content;
        private final int index;

        private ResponsePublisher(byte[] content, int index) {
            this.content = content;
            this.index = index;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) content.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                private boolean running = true;

                @Override
                public void request(long n) {
                    if (n <= 0) {
                        running = false;
                        s.onError(new IllegalArgumentException("Demand must be positive"));
                    } else if (running) {
                        running = false;
                        s.onNext(ByteBuffer.wrap(content));
                        try {
                            Thread.sleep(responses.get(index).right().toMillis());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    running = false;
                }
            });
        }
    }

    private static class CapturingSubscriber implements Subscriber<ByteBuffer> {
        private ByteBuffer byteBuffer;
        private CountDownLatch done = new CountDownLatch(1);

        CapturingSubscriber(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            byteBuffer.put(bytes);
        }

        @Override
        public void onError(Throwable t) {
            done.countDown();
        }

        @Override
        public void onComplete() {
            done.countDown();
        }
    }
}
