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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;

class SplittingTransformerTest {
    private static final Logger log = Logger.loggerFor(SplittingTransformerTest.class);

    @Test
    void whenSubscriberCancelSubscription_AllDataSentToTransformer() {
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            SplittingTransformer.<TestResultObject, Object>builder()
                                .upstreamResponseTransformer(upstreamTestTransformer)
                                .maximumBufferSizeInBytes(1024 * 1024 * 32L)
                                .resultFuture(future)
                                .build();
        split.subscribe(new CancelAfterNTestSubscriber(
            4, n -> AsyncRequestBody.fromString(String.format("This is the body of %d.", n))));
        future.join();
        String expected = "This is the body of 0.This is the body of 1.This is the body of 2.This is the body of 3.";
        assertThat(upstreamTestTransformer.contentAsString()).isEqualTo(expected);
    }

    @Test
    void whenSubscriberFailsAttempt_UpstreamTransformerCompletesExceptionally() {
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            SplittingTransformer.<TestResultObject, Object>builder()
                                .upstreamResponseTransformer(upstreamTestTransformer)
                                .maximumBufferSizeInBytes(1024 * 1024 * 32L)
                                .resultFuture(future)
                                .build();
        split.subscribe(new FailAfterNTestSubscriber(2));
        assertThatThrownBy(future::join).hasMessageContaining("TEST ERROR 2");
    }

    @Test
    void whenDataExceedsBufferSize_UpstreamShouldReceiveAllData() {
        Long evenBufferSize = 16 * 1024L;

        // We send 9 split body of 7kb with a buffer size of 16kb. This is to test when uneven body size is used compared to
        // the buffer size, this test use a body size which does not evenly divides with the buffer size.
        int unevenBodyLength = 7 * 1024;
        int splitAmount = 9;
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            SplittingTransformer.<TestResultObject, Object>builder()
                                .upstreamResponseTransformer(upstreamTestTransformer)
                                .maximumBufferSizeInBytes(evenBufferSize)
                                .resultFuture(future)
                                .build();
        split.subscribe(new CancelAfterNTestSubscriber(
            splitAmount,
            n -> {
                String content =
                    IntStream.range(0, unevenBodyLength).mapToObj(i -> String.valueOf(n)).collect(Collectors.joining());
                return AsyncRequestBody.fromString(content);
            }));
        future.join();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < splitAmount; i++) {
            int value = i;
            expected.append(IntStream.range(0, unevenBodyLength).mapToObj(j -> String.valueOf(value)).collect(Collectors.joining()));
        }
        assertThat(upstreamTestTransformer.contentAsString()).hasSize(unevenBodyLength * splitAmount);
        assertThat(upstreamTestTransformer.contentAsString()).isEqualTo(expected.toString());
    }

    @Test
    void whenRequestingMany_allDemandGetsFulfilled() {
        UpstreamTestTransformer upstreamTestTransformer = new UpstreamTestTransformer();
        CompletableFuture<Object> future = new CompletableFuture<>();
        SplittingTransformer<TestResultObject, Object> split =
            SplittingTransformer.<TestResultObject, Object>builder()
                                .upstreamResponseTransformer(upstreamTestTransformer)
                                .maximumBufferSizeInBytes(1024 * 1024 * 32L)
                                .resultFuture(future)
                                .build();
        split.subscribe(new RequestingTestSubscriber(4));

        future.join();
        String expected = "This is the body of 1.This is the body of 2.This is the body of 3.This is the body of 4.";
        assertThat(upstreamTestTransformer.contentAsString()).isEqualTo(expected);
    }

    @Test
    void negativeBufferSize_shouldThrowIllegalArgument() {
            assertThatThrownBy(() -> SplittingTransformer.<TestResultObject, Object>builder()
                                .maximumBufferSizeInBytes(-1L)
                                .upstreamResponseTransformer(new UpstreamTestTransformer())
                                .resultFuture(new CompletableFuture<>())
                                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximumBufferSizeInBytes");
    }

    @Test
    void nullBufferSize_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SplittingTransformer.<TestResultObject, Object>builder()
                                                     .maximumBufferSizeInBytes(null)
                                                     .upstreamResponseTransformer(new UpstreamTestTransformer())
                                                     .resultFuture(new CompletableFuture<>())
                                                     .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("maximumBufferSizeInBytes");
    }

    @Test
    void nullUpstreamTransformer_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SplittingTransformer.<TestResultObject, Object>builder()
                                                     .maximumBufferSizeInBytes(1024L)
                                                     .upstreamResponseTransformer(null)
                                                     .resultFuture(new CompletableFuture<>())
                                                     .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("upstreamResponseTransformer");
    }

    @Test
    void nullFuture_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> SplittingTransformer.<TestResultObject, Object>builder()
                                                     .maximumBufferSizeInBytes(1024L)
                                                     .upstreamResponseTransformer(new UpstreamTestTransformer())
                                                     .resultFuture(null)
                                                     .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("resultFuture");
    }

    @Test
    void resultFutureCancelled_shouldSignalErrorToSubscriberAndCancelTransformerFuture() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        UpstreamTestTransformer transformer = new UpstreamTestTransformer();
        SplittingTransformer<TestResultObject, Object> split =
            SplittingTransformer.<TestResultObject, Object>builder()
                                .upstreamResponseTransformer(transformer)
                                .maximumBufferSizeInBytes(1024L)
                                .resultFuture(future)
                                .build();

        ErrorCapturingSubscriber subscriber = new ErrorCapturingSubscriber();
        split.subscribe(subscriber);

        future.cancel(true);

        assertThat(subscriber.error).isNotNull();
        assertThat(subscriber.error).isInstanceOf(CancellationException.class);

        CompletableFuture<Object> transformerFuture = transformer.future;
        assertThat(transformerFuture).isCancelled();
    }

    private static class ErrorCapturingSubscriber
        implements Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>> {

        private Subscription subscription;
        private Throwable error;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
            transformer.prepare();
            transformer.onResponse(new TestResultObject("test"));
            transformer.onStream(AsyncRequestBody.fromString("test"));
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onComplete() {
            /* do nothing, test only */
        }
    }

    private static class CancelAfterNTestSubscriber
        implements Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>> {

        private final int n;
        private Subscription subscription;
        private int total = 0;
        private final Function<Integer, AsyncRequestBody> bodySupplier;

        CancelAfterNTestSubscriber(int n, Function<Integer, AsyncRequestBody> bodySupplier) {
            this.n = n;
            this.bodySupplier = bodySupplier;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
            // simulate what is done during a service call
            if (total >= n) {
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
            transformer.onStream(bodySupplier.apply(total));
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
    }

    private static class FailAfterNTestSubscriber
        implements Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>> {

        private final int n;
        private Subscription subscription;
        private int total = 0;

        FailAfterNTestSubscriber(int n) {
            this.n = n;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
            if (total > n) {
                fail("Did not expect more than 2 request to be made");
            }

            transformer.prepare();
            if (total == n) {
                transformer.exceptionOccurred(new RuntimeException("TEST ERROR " + total));
                return;
            }

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
    }

    private static class RequestingTestSubscriber
        implements Subscriber<AsyncResponseTransformer<TestResultObject, TestResultObject>> {

        private final int totalToRequest;
        private Subscription subscription;
        private int received = 0;

        RequestingTestSubscriber(int totalToRequest) {
            this.totalToRequest = totalToRequest;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(totalToRequest);
        }

        @Override
        public void onNext(AsyncResponseTransformer<TestResultObject, TestResultObject> transformer) {
            received++;
            transformer.prepare();
            transformer.onResponse(new TestResultObject("container msg: " + received));
            transformer.onStream(AsyncRequestBody.fromString(String.format("This is the body of %d.", received)));
            if (received >= totalToRequest) {
                subscription.cancel();
            }
        }

        @Override
        public void onError(Throwable t) {
            fail("unexpected onError", t);
        }

        @Override
        public void onComplete() {
            // do nothing, test only
        }
    }


    private static class UpstreamTestTransformer implements AsyncResponseTransformer<TestResultObject, Object> {

        private final CompletableFuture<Object> future;
        private final StringBuilder content = new StringBuilder();

        UpstreamTestTransformer() {
            this.future = new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<Object> prepare() {
            log.info(() -> "[UpstreamTestTransformer] prepare");
            return this.future;
        }

        @Override
        public void onResponse(TestResultObject response) {
            log.info(() -> String.format("[UpstreamTestTransformer] onResponse: %s", response.toString()));
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            log.info(() -> "[UpstreamTestTransformer] onStream");
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

        private final String msg;

        TestResultObject(String msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return "TestResultObject{'" + msg + "'}";
        }
    }
}
