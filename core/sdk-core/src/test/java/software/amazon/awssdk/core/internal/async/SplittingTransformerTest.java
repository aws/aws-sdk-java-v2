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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

class SplittingTransformerTest {

    @Test
    void whenSubscriberCancelSubscription_AllDataSentToTransformer() {
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            new SplittingTransformer<>(upstreamTestTransformer, 1024*1024*32, future);
        split.subscribe(new Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>>() {
            private Subscription subscription;
            private int total = 0;
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
                // simulate what is done during a service call
                if (total >= 4) {
                    subscription.cancel();
                    return;
                }
                CompletableFuture<TestResultObject> future = transformer.prepare();
                future.whenComplete((r, e) -> {
                    if (e != null) {
                        fail(e);
                    }
                });
                transformer.onResponse(new TestResultObject("container msg: " + total));
                transformer.onStream(AsyncRequestBody.fromString(String.format("This is the body of %d.", total)));
                total++;
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected onError", t);
            }

            @Override
            public void onComplete() {
                // do nothing, test only
            }
        });
        future.join();
        String expected = "This is the body of 0.This is the body of 1.This is the body of 2.This is the body of 3.";
        assertThat(upstreamTestTransformer.contentAsString()).isEqualTo(expected);
    }

    @Test
    void whenSubscriberFailsAttempt_UpstreamTransformerCompletesExceptionally() {
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            new SplittingTransformer<>(upstreamTestTransformer, 1024*1024*32, future);
        split.subscribe(new Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>>() {
            private Subscription subscription;
            private int total = 0;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
                if (total > 2) {
                    fail("Did not expect more than 2 request to be made");
                }

                if (total == 2) {
                    transformer.exceptionOccurred(new RuntimeException("TEST ERROR " + total));
                    return;
                }

                transformer.prepare();
                transformer.onResponse(new TestResultObject("container msg: " + total));
                transformer.onStream(AsyncRequestBody.fromString(String.format("This is the body of %d.", total)));
                total++;
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                // do nothing, test only
            }

            @Override
            public void onComplete() {
                // do nothing, test only
            }
        });
        assertThatThrownBy(future::join).hasMessageContaining("TEST ERROR 2");
    }

    @Test
    void whenDataExceedsBufferSize_UpstreamShouldReceiveAllData() {
        int bodyLength = 7*1024;
        int amount = 9;
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            new SplittingTransformer<>(upstreamTestTransformer, 16*1024, future);
        split.subscribe(new Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>>() {
            private Subscription subscription;
            private int total = 0;
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
                if (total >= amount) {
                    subscription.cancel();
                    return;
                }
                transformer.prepare();
                String content =
                    IntStream.range(0, bodyLength).mapToObj(i -> String.valueOf(total)).collect(Collectors.joining());
                transformer.onResponse(new TestResultObject("response :" + total));
                transformer.onStream(AsyncRequestBody.fromString(content));
                total++;
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected onError", t);
            }

            @Override
            public void onComplete() {
                // do nothing, test only
            }
        });
        future.join();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < amount; i++) {
            int value = i;
            expected.append(IntStream.range(0, bodyLength).mapToObj(j -> String.valueOf(value)).collect(Collectors.joining()));
        }
        assertThat(upstreamTestTransformer.contentAsString()).hasSize(bodyLength*amount);
        assertThat(upstreamTestTransformer.contentAsString()).isEqualTo(expected.toString());
    }

    private static class UpstreamTestTransformer implements AsyncResponseTransformer<TestResultObject, Object> {
        private final CompletableFuture<Object> future;
        private final StringBuilder content = new StringBuilder();

        public UpstreamTestTransformer() {
            this.future = new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<Object> prepare() {
            System.out.println("[UpstreamTestTransformer] prepare");
            return this.future;
        }

        @Override
        public void onResponse(TestResultObject response) {
            System.out.printf("[UpstreamTestTransformer] onResponse: %s%n", response.toString());
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            System.out.println("[UpstreamTestTransformer] onStream");
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                private Subscription subscription;
                @Override
                public void onSubscribe(Subscription s) {
                    this.subscription = s;
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    ByteBuffer dup = byteBuffer.duplicate();
                    byte[] dest = new byte[dup.capacity()];
                    dup.position(0);
                    dup.get(dest);
                    String str = new String(dest);
                    content.append(str);
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    future.complete(new Object());
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            future.completeExceptionally(error);
        }

        public String contentAsString() {
            return content.toString();
        }
    }

    private static class TestResultObject {
        String msg;
        public TestResultObject(String msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return "TestResultObject{'" + msg + "'}";
        }
    }
}