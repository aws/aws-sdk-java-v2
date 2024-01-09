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
import static org.assertj.core.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

class SplittingTransformerTest {

    @BeforeEach
    void init() {
    }

    @Test
    void manualTest() {
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
                System.out.println("[TestSubscriber] onSubscribe: " + total);
                s.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
                // simulate what is done during a service call
                System.out.println("[TestSubscriber] onNext: " + total);
                if (total >= 4) {
                    System.out.println("[TestSubscriber] max reached, cancelling subscription");
                    subscription.cancel();
                    return;
                }
                CompletableFuture<TestResultObject> future = transformer.prepare();
                future.whenComplete((r, e) -> {
                    if (e != null) {
                        System.out.println("[TestSubscriber] future completed with error " + total);
                        System.out.println(e);
                    } else {
                        System.out.println("[TestSubscriber] future completed: " + total);
                    }
                });
                transformer.onResponse(new TestResultObject("container msg: " + total));
                transformer.onStream(AsyncRequestBody.fromString(String.format("This is the body of %d.", total)));
                total++;
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("[TestSubscriber] onError");
            }

            @Override
            public void onComplete() {
                System.out.println("[TestSubscriber] on complete");
                future.complete(new Object());
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
                System.out.println("[TestSubscriber] onSubscribe: " + total);
                s.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
                if (total == 2) {
                    transformer.exceptionOccurred(new RuntimeException("TEST ERROR " + total));
                    return;
                }
                if (total > 2) {
                    fail("Did not expect more than 2 request to be made");
                }

                CompletableFuture<TestResultObject> future = transformer.prepare();
                future.whenComplete((r, e) -> {
                    if (e != null) {
                        System.out.println("[TestSubscriber] future completed with error " + total);
                        System.out.println(e);
                    } else {
                        System.out.println("[TestSubscriber] future completed: " + total);
                    }
                });
                transformer.onResponse(new TestResultObject("container msg: " + total));
                transformer.onStream(AsyncRequestBody.fromString(String.format("This is the body of %d.", total)));
                total++;
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("[TestSubscriber] onError");
            }

            @Override
            public void onComplete() {
                System.out.println("[TestSubscriber] on complete");
                future.complete(new Object());
            }
        });
        assertThatThrownBy(future::join).hasMessageContaining("TEST ERROR 2");
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
                    System.out.printf("[UpstreamTestTransformer] received in upstream: %s%n", str);
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