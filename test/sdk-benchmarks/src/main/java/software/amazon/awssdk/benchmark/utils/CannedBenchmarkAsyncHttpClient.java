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

package software.amazon.awssdk.benchmark.utils;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * Benchmark-only async HTTP mock that returns fixed response bytes with no request capture list.
 *
 * <p>Completion runs on a dedicated executor so Tier C async benches continue to include mock
 * scheduling in single-operation {@code join()} latency, matching the documented measurement
 * boundary. Request body publishers are drained and discarded (not retained across calls).
 */
public final class CannedBenchmarkAsyncHttpClient implements SdkAsyncHttpClient {

    private static final SdkHttpResponse OK = SdkHttpResponse.builder().statusCode(200).build();

    private final byte[] body;
    private final ExecutorService executor;

    public CannedBenchmarkAsyncHttpClient(byte[] body) {
        this.body = body.clone();
        this.executor = Executors.newFixedThreadPool(3, new DaemonThreadFactory());
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                drainRequestBody(request.requestContentPublisher());
                request.responseHandler().onHeaders(OK);
                request.responseHandler().onStream(new FixedBodyPublisher(body));
                future.complete(null);
            } catch (Throwable t) {
                try {
                    request.responseHandler().onError(t);
                } catch (Throwable ignored) {
                    // Handler already notified; complete the execute future exceptionally below.
                }
                future.completeExceptionally(t);
            }
        }, executor);
        return future;
    }

    @Override
    public String clientName() {
        return "CannedBenchmarkAsync";
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private static void drainRequestBody(SdkHttpContentPublisher publisher) {
        if (publisher == null) {
            return;
        }
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                // Discard; do not retain request payloads across benchmark invocations.
            }

            @Override
            public void onError(Throwable throwable) {
                // Ignore drain errors; execute path surfaces failures via response handler.
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private static final class FixedBodyPublisher implements SdkHttpContentPublisher {
        private final byte[] content;

        private FixedBodyPublisher(byte[] content) {
            this.content = content;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) content.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean done;

                @Override
                public void request(long n) {
                    if (n <= 0) {
                        done = true;
                        subscriber.onError(new IllegalArgumentException("Demand must be positive"));
                        return;
                    }
                    if (!done) {
                        done = true;
                        subscriber.onNext(ByteBuffer.wrap(content));
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ddb-bench-async-mock-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
