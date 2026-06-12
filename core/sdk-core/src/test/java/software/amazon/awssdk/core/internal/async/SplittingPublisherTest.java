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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.core.internal.async.SplittingPublisherTestUtils.verifyIndividualAsyncRequestBody;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.BufferedSplittableAsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Pair;

public class SplittingPublisherTest {
    private static final int CHUNK_SIZE = 5;

    private static final int CONTENT_SIZE = 101;
    private static final byte[] CONTENT =
        RandomStringUtils.randomAscii(CONTENT_SIZE).getBytes(Charset.defaultCharset());

    private static final int NUM_OF_CHUNK = (int) Math.ceil(CONTENT_SIZE / (double) CHUNK_SIZE);

    private static File testFile;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testFile = File.createTempFile("SplittingPublisherTest", UUID.randomUUID().toString());
        Files.write(testFile.toPath(), CONTENT);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        testFile.delete();
    }

    @Test
    public void split_contentUnknownMaxMemorySmallerThanChunkSize_shouldThrowException() {
        AsyncRequestBody body = AsyncRequestBody.fromPublisher(s -> {
        });
        AsyncRequestBodySplitConfiguration configuration = AsyncRequestBodySplitConfiguration.builder()
                                                                                             .chunkSizeInBytes(10L)
                                                                                             .bufferSizeInBytes(5L)
                                                                                             .build();
        assertThatThrownBy(() -> SplittingPublisher.builder()
                .asyncRequestBody(body)
                .splitConfiguration(configuration)
                .retryableSubAsyncRequestBodyEnabled(false)
                .build())
            .hasMessageContaining("must be larger than or equal");
    }

    @ParameterizedTest
    @ValueSource(ints = {CHUNK_SIZE, CHUNK_SIZE * 2 - 1, CHUNK_SIZE * 2})
    void differentChunkSize_shouldSplitAsyncRequestBodyCorrectly(int chunkSize) throws Exception {

        FileAsyncRequestBody fileAsyncRequestBody = FileAsyncRequestBody.builder()
                                                                        .path(testFile.toPath())
                                                                        .chunkSizeInBytes(chunkSize)
                                                                        .build();
        verifySplitContent(fileAsyncRequestBody, chunkSize);
        fileAsyncRequestBody = FileAsyncRequestBody.builder()
                                                   .path(testFile.toPath())
                                                   .chunkSizeInBytes(chunkSize)
                                                   .build();
        verifyRetryableSplitContent(fileAsyncRequestBody, chunkSize);
    }

    @ParameterizedTest
    @ValueSource(ints = {CHUNK_SIZE, CHUNK_SIZE * 2 - 1, CHUNK_SIZE * 2})
    void differentChunkSize_byteArrayShouldSplitAsyncRequestBodyCorrectly(int chunkSize) throws Exception {
        verifySplitContent(AsyncRequestBody.fromBytes(CONTENT), chunkSize);
        verifyRetryableSplitContent(AsyncRequestBody.fromBytes(CONTENT), chunkSize);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void contentLengthNotPresent_shouldHandle(boolean enableRetryableSubAsyncRequestBody) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        TestAsyncRequestBody asyncRequestBody = new TestAsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }
        };
        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(asyncRequestBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes((long) CHUNK_SIZE)
                        .bufferSizeInBytes(10L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(enableRetryableSubAsyncRequestBody)
                .build();


        List<CompletableFuture<byte[]>> futures = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);

        splittingPublisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> baosFuture = new CompletableFuture<>();
            BaosSubscriber subscriber = new BaosSubscriber(baosFuture);
            futures.add(baosFuture);
            requestBody.subscribe(subscriber);
            if (index.incrementAndGet() == NUM_OF_CHUNK) {
                assertThat(requestBody.contentLength()).hasValue(1L);
            } else {
                assertThat(requestBody.contentLength()).hasValue((long) CHUNK_SIZE);
            }
        }).get(5, TimeUnit.SECONDS);
        assertThat(futures.size()).isEqualTo(NUM_OF_CHUNK);

        for (int i = 0; i < futures.size(); i++) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(CONTENT)) {
                byte[] expected;
                if (i == futures.size() - 1) {
                    expected = new byte[1];
                } else {
                    expected = new byte[CHUNK_SIZE];
                }
                inputStream.skip(i * CHUNK_SIZE);
                inputStream.read(expected);
                byte[] actualBytes = futures.get(i).join();
                assertThat(actualBytes).isEqualTo(expected);
            };
        }

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downStreamFailed_shouldPropagateCancellation(boolean enableRetryableSubAsyncRequestBody) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        TestAsyncRequestBody asyncRequestBody = new TestAsyncRequestBody();
        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(asyncRequestBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes((long) CHUNK_SIZE)
                        .bufferSizeInBytes(10L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(enableRetryableSubAsyncRequestBody)
                .build();
        assertThatThrownBy(() -> splittingPublisher.subscribe(requestBody -> {
            throw new RuntimeException("foobar");
        }).get(5, TimeUnit.SECONDS)).hasMessageContaining("foobar");
        assertThat(asyncRequestBody.cancelled).isTrue();
    }

    @Test
    void retryableSubAsyncRequestBodyEnabled_shouldBeAbleToResubscribe() throws ExecutionException, InterruptedException, TimeoutException {
        int chunkSize = 5;
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                                                   .path(testFile.toPath())
                                                   .chunkSizeInBytes(chunkSize)
                                                   .build();

        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(asyncRequestBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes((long) chunkSize)
                        .bufferSizeInBytes((long) chunkSize * 4)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(true)
                .build();



        Map<Integer, Pair<CompletableFuture<byte[]>, CompletableFuture<byte[]>>> futures = new HashMap<>();
        AtomicInteger index = new AtomicInteger();
        splittingPublisher.subscribe(requestBody -> {
            int i = index.getAndIncrement();
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            BaosSubscriber subscriber = new BaosSubscriber(future);
            requestBody.subscribe(subscriber);

            future.whenComplete((r, t) -> {
                CompletableFuture<byte[]> future2 = new CompletableFuture<>();
                BaosSubscriber anotherSubscriber = new BaosSubscriber(future2);
                requestBody.subscribe(anotherSubscriber);
                futures.put(i, Pair.of(future, future2));

                future2.whenComplete((res, throwable) -> {
                    requestBody.close();
                });
            });
        }).get(5, TimeUnit.SECONDS);

        for (int i = 0; i < futures.size(); i++) {
            assertThat(futures.get(i).left().join()).containsExactly( futures.get(i).right().join());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void upstreamError_shouldPropagateToCurrentBodySubscriber(boolean enableRetryableSubAsyncRequestBody) throws Exception {
        RuntimeException upstreamError = new RuntimeException("upstream failure");
        AsyncRequestBody errorBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(20L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                s.onSubscribe(new Subscription() {
                    private int calls = 0;

                    @Override
                    public void request(long n) {
                        if (calls++ == 0) {
                            // Send partial data, then error
                            s.onNext(ByteBuffer.wrap(new byte[3]));
                        } else {
                            s.onError(upstreamError);
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };

        SplittingPublisher splittingPublisher =
            SplittingPublisher.builder()
                              .asyncRequestBody(errorBody)
                              .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                                                                                    .chunkSizeInBytes(10L)
                                                                                    .bufferSizeInBytes(20L)
                                                                                    .build())
                              .retryableSubAsyncRequestBodyEnabled(enableRetryableSubAsyncRequestBody)
                              .build();

        CompletableFuture<Throwable> bodyError = new CompletableFuture<>();
        splittingPublisher.subscribe(requestBody -> {
            requestBody.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                }

                @Override
                public void onError(Throwable t) {
                    bodyError.complete(t);
                }

                @Override
                public void onComplete() {
                }
            });
        });

        Throwable error = bodyError.get(5, TimeUnit.SECONDS);
        assertThat(error).isEqualTo(upstreamError);
    }

    @Test
    void bufferedSplittable_createWithFullBufferingTrue_defersPartEmission() throws Exception {
        // Create a controlled async request body with known content length that delivers data in two chunks
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        AsyncRequestBody sourceBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) data.length);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                s.onSubscribe(new Subscription() {
                    private boolean done = false;

                    @Override
                    public void request(long n) {
                        if (!done) {
                            done = true;
                            s.onNext(ByteBuffer.wrap(data));
                            s.onComplete();
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };

        // Use builder to enable full buffering
        BufferedSplittableAsyncRequestBody bufferedBody = BufferedSplittableAsyncRequestBody.builder()
                .asyncRequestBody(sourceBody)
                .bufferBeforeSend(true)
                .build();

        // Verify that content length is propagated
        assertThat(bufferedBody.contentLength()).hasValue((long) data.length);

        // Split with a chunk size of 5 (two parts of 5 bytes each)
        AsyncRequestBodySplitConfiguration splitConfig = AsyncRequestBodySplitConfiguration.builder()
                .chunkSizeInBytes(5L)
                .bufferSizeInBytes(20L)
                .build();

        SdkPublisher<CloseableAsyncRequestBody> publisher = bufferedBody.splitCloseable(splitConfig);

        // Track when parts are received by the downstream subscriber
        List<CompletableFuture<byte[]>> partFutures = new ArrayList<>();

        CompletableFuture<Void> subscribeFuture = publisher.subscribe(requestBody -> {
            // Each part should arrive with data already available (fully buffered)
            CompletableFuture<byte[]> partFuture = new CompletableFuture<>();
            partFutures.add(partFuture);
            requestBody.subscribe(new BaosSubscriber(partFuture));
            partFuture.whenComplete((r, t) -> requestBody.close());
        });

        subscribeFuture.get(5, TimeUnit.SECONDS);

        // Verify we received 2 parts with correct data
        assertThat(partFutures.size()).isEqualTo(2);

        byte[] firstPart = partFutures.get(0).get(5, TimeUnit.SECONDS);
        byte[] secondPart = partFutures.get(1).get(5, TimeUnit.SECONDS);

        byte[] expectedFirst = new byte[]{0, 1, 2, 3, 4};
        byte[] expectedSecond = new byte[]{5, 6, 7, 8, 9};

        assertThat(firstPart).isEqualTo(expectedFirst);
        assertThat(secondPart).isEqualTo(expectedSecond);
    }

    @Test
    void bufferedSplittable_createDefault_sendsPartsImmediately() throws Exception {
        // Create a body with known content length
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        AsyncRequestBody sourceBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) data.length);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                s.onSubscribe(new Subscription() {
                    private boolean done = false;

                    @Override
                    public void request(long n) {
                        if (!done) {
                            done = true;
                            s.onNext(ByteBuffer.wrap(data));
                            s.onComplete();
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };

        // Use default create(body) - full buffering should be disabled
        BufferedSplittableAsyncRequestBody bufferedBody = BufferedSplittableAsyncRequestBody.create(sourceBody);

        // Verify content length is propagated
        assertThat(bufferedBody.contentLength()).hasValue((long) data.length);

        // Split with chunk size of 5 (two parts of 5 bytes each)
        AsyncRequestBodySplitConfiguration splitConfig = AsyncRequestBodySplitConfiguration.builder()
                .chunkSizeInBytes(5L)
                .bufferSizeInBytes(20L)
                .build();

        SdkPublisher<CloseableAsyncRequestBody> publisher = bufferedBody.splitCloseable(splitConfig);

        // Track parts received
        List<CompletableFuture<byte[]>> partFutures = new ArrayList<>();

        CompletableFuture<Void> subscribeFuture = publisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> partFuture = new CompletableFuture<>();
            partFutures.add(partFuture);
            requestBody.subscribe(new BaosSubscriber(partFuture));
            partFuture.whenComplete((r, t) -> requestBody.close());
        });

        subscribeFuture.get(5, TimeUnit.SECONDS);

        // Verify we received 2 parts with correct data (existing behavior preserved)
        assertThat(partFutures.size()).isEqualTo(2);

        byte[] firstPart = partFutures.get(0).get(5, TimeUnit.SECONDS);
        byte[] secondPart = partFutures.get(1).get(5, TimeUnit.SECONDS);

        byte[] expectedFirst = new byte[]{0, 1, 2, 3, 4};
        byte[] expectedSecond = new byte[]{5, 6, 7, 8, 9};

        assertThat(firstPart).isEqualTo(expectedFirst);
        assertThat(secondPart).isEqualTo(expectedSecond);
    }

    @Test
    void bufferedSplittable_createWithFullBufferingTrue_partsAreRetryable() throws Exception {
        // Verify that create(body, true) produces retryable sub-bodies (retry buffer is populated)
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        AsyncRequestBody sourceBody = AsyncRequestBody.fromBytes(data);

        BufferedSplittableAsyncRequestBody bufferedBody = BufferedSplittableAsyncRequestBody.builder()
                .asyncRequestBody(sourceBody)
                .bufferBeforeSend(true)
                .build();

        AsyncRequestBodySplitConfiguration splitConfig = AsyncRequestBodySplitConfiguration.builder()
                .chunkSizeInBytes(5L)
                .bufferSizeInBytes(20L)
                .build();

        SdkPublisher<CloseableAsyncRequestBody> publisher = bufferedBody.splitCloseable(splitConfig);

        // Subscribe, read each part, then resubscribe to verify retry buffer is available
        Map<Integer, Pair<CompletableFuture<byte[]>, CompletableFuture<byte[]>>> futures = new HashMap<>();
        AtomicInteger index = new AtomicInteger();

        publisher.subscribe(requestBody -> {
            int i = index.getAndIncrement();
            CompletableFuture<byte[]> firstRead = new CompletableFuture<>();
            requestBody.subscribe(new BaosSubscriber(firstRead));

            firstRead.whenComplete((r, t) -> {
                // Resubscribe to verify retry works
                CompletableFuture<byte[]> secondRead = new CompletableFuture<>();
                requestBody.subscribe(new BaosSubscriber(secondRead));
                futures.put(i, Pair.of(firstRead, secondRead));
                secondRead.whenComplete((res, throwable) -> requestBody.close());
            });
        }).get(5, TimeUnit.SECONDS);

        // Verify all parts can be re-read (retry buffer is populated before downstream subscription)
        for (int i = 0; i < futures.size(); i++) {
            byte[] firstReadData = futures.get(i).left().get(5, TimeUnit.SECONDS);
            byte[] secondReadData = futures.get(i).right().get(5, TimeUnit.SECONDS);
            assertThat(firstReadData).isEqualTo(secondReadData);
        }
    }

    private static void verifySplitContent(AsyncRequestBody asyncRequestBody, int chunkSize) throws Exception {
        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(asyncRequestBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes((long) chunkSize)
                        .bufferSizeInBytes((long) chunkSize * 4)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(false)
                .build();

        verifyIndividualAsyncRequestBody(splittingPublisher.map(m -> m), testFile.toPath(), chunkSize);
    }

    private static void verifyRetryableSplitContent(AsyncRequestBody asyncRequestBody, int chunkSize) throws Exception {
        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(asyncRequestBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes((long) chunkSize)
                        .bufferSizeInBytes((long) chunkSize * 4)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(false)
                .build();

        verifyIndividualAsyncRequestBody(splittingPublisher.map(m -> m), testFile.toPath(), chunkSize);
    }

    private static class TestAsyncRequestBody implements AsyncRequestBody {
        private volatile boolean cancelled;
        private volatile boolean isDone;

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) CONTENT.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (isDone) {
                        return;
                    }
                    isDone = true;
                    s.onNext(ByteBuffer.wrap(CONTENT));
                    s.onComplete();

                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });

        }
    }

    // ==================== Tests for bufferBeforeSend ====================

    @Test
    void bufferBeforeSend_knownContentLength_defersBodyUntilComplete() throws Exception {
        // When bufferBeforeSend=true and content length is known, the downstream subscriber
        // should NOT receive the body until completeCurrentBody() is invoked (i.e., after the part is fully buffered).
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        // Use a controlled upstream that sends data in pieces
        ControlledAsyncRequestBody controlledBody = new ControlledAsyncRequestBody(Optional.of((long) data.length));

        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(controlledBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes(10L)
                        .bufferSizeInBytes(20L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(true)
                .bufferBeforeSend(true)
                .build();

        List<CompletableFuture<byte[]>> receivedBodies = new ArrayList<>();
        CompletableFuture<Void> subscribeFuture = splittingPublisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> bodyFuture = new CompletableFuture<>();
            receivedBodies.add(bodyFuture);
            BaosSubscriber subscriber = new BaosSubscriber(bodyFuture);
            requestBody.subscribe(subscriber);
        });

        // Give time for subscription to be set up
        Thread.sleep(100);

        // Send partial data — the body should NOT have been emitted yet
        controlledBody.sendData(ByteBuffer.wrap(data, 0, 5));
        Thread.sleep(100);
        assertThat(receivedBodies.size()).isEqualTo(0);

        // Send remaining data and complete
        controlledBody.sendData(ByteBuffer.wrap(data, 5, 5));
        controlledBody.complete();

        subscribeFuture.get(5, TimeUnit.SECONDS);

        // Now the body should have been emitted and completed
        assertThat(receivedBodies.size()).isEqualTo(1);
        byte[] result = receivedBodies.get(0).get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void bufferBeforeSendDisabled_knownContentLength_sendsImmediately() throws Exception {
        // When bufferBeforeSend=false (default) and content length is known, the body should
        // be sent to the downstream subscriber immediately upon initialization.
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        ControlledAsyncRequestBody controlledBody = new ControlledAsyncRequestBody(Optional.of((long) data.length));

        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(controlledBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes(10L)
                        .bufferSizeInBytes(20L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(true)
                .bufferBeforeSend(false)
                .build();

        List<CompletableFuture<byte[]>> receivedBodies = new ArrayList<>();
        splittingPublisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> bodyFuture = new CompletableFuture<>();
            receivedBodies.add(bodyFuture);
            BaosSubscriber subscriber = new BaosSubscriber(bodyFuture);
            requestBody.subscribe(subscriber);
        });

        // Give time for subscription to be set up — the body should be sent immediately
        Thread.sleep(100);
        assertThat(receivedBodies.size()).isEqualTo(1);

        // Now send data and complete
        controlledBody.sendData(ByteBuffer.wrap(data));
        controlledBody.complete();

        byte[] result = receivedBodies.get(0).get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void bufferBeforeSend_unknownContentLength_behaviorUnchanged() throws Exception {
        // When content length is unknown, behavior is unchanged regardless of bufferBeforeSend.
        // The body is always deferred until complete (existing behavior).
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        ControlledAsyncRequestBody controlledBody = new ControlledAsyncRequestBody(Optional.empty());

        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(controlledBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes(10L)
                        .bufferSizeInBytes(20L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(true)
                .bufferBeforeSend(true)
                .build();

        List<CompletableFuture<byte[]>> receivedBodies = new ArrayList<>();
        splittingPublisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> bodyFuture = new CompletableFuture<>();
            receivedBodies.add(bodyFuture);
            BaosSubscriber subscriber = new BaosSubscriber(bodyFuture);
            requestBody.subscribe(subscriber);
        });

        // Give time for subscription to be set up
        Thread.sleep(100);

        // Send partial data — the body should NOT have been emitted yet (unknown-length path defers)
        controlledBody.sendData(ByteBuffer.wrap(data, 0, 5));
        Thread.sleep(100);
        assertThat(receivedBodies.size()).isEqualTo(0);

        // Send remaining data and complete
        controlledBody.sendData(ByteBuffer.wrap(data, 5, 5));
        controlledBody.complete();

        // Now body should be emitted
        Thread.sleep(200);
        assertThat(receivedBodies.size()).isEqualTo(1);
        byte[] result = receivedBodies.get(0).get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void bufferBeforeSend_multiPart_allPartsDeferred() throws Exception {
        // When splitting into multiple parts with bufferBeforeSend=true, all parts are deferred
        // until fully buffered.
        byte[] data = new byte[20];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        ControlledAsyncRequestBody controlledBody = new ControlledAsyncRequestBody(Optional.of((long) data.length));

        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(controlledBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes(10L)
                        .bufferSizeInBytes(30L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(true)
                .bufferBeforeSend(true)
                .build();

        List<CompletableFuture<byte[]>> receivedBodies = new ArrayList<>();
        CompletableFuture<Void> subscribeFuture = splittingPublisher.subscribe(requestBody -> {
            CompletableFuture<byte[]> bodyFuture = new CompletableFuture<>();
            receivedBodies.add(bodyFuture);
            BaosSubscriber subscriber = new BaosSubscriber(bodyFuture);
            requestBody.subscribe(subscriber);
        });

        // Give time for subscription
        Thread.sleep(100);

        // Send first chunk partially — no body should be emitted yet
        controlledBody.sendData(ByteBuffer.wrap(data, 0, 5));
        Thread.sleep(100);
        assertThat(receivedBodies.size()).isEqualTo(0);

        // Complete first chunk (10 bytes) — first body should now be emitted
        controlledBody.sendData(ByteBuffer.wrap(data, 5, 5));
        Thread.sleep(100);
        assertThat(receivedBodies.size()).isEqualTo(1);

        // Send second chunk partially — second body should not be emitted yet
        controlledBody.sendData(ByteBuffer.wrap(data, 10, 5));
        Thread.sleep(100);
        assertThat(receivedBodies.size()).isEqualTo(1);

        // Complete second chunk and signal upstream complete
        controlledBody.sendData(ByteBuffer.wrap(data, 15, 5));
        controlledBody.complete();

        subscribeFuture.get(5, TimeUnit.SECONDS);

        // Both bodies should now be emitted
        assertThat(receivedBodies.size()).isEqualTo(2);

        // Verify content of first part
        byte[] firstPart = receivedBodies.get(0).get(5, TimeUnit.SECONDS);
        byte[] expectedFirst = new byte[10];
        System.arraycopy(data, 0, expectedFirst, 0, 10);
        assertThat(firstPart).isEqualTo(expectedFirst);

        // Verify content of second part
        byte[] secondPart = receivedBodies.get(1).get(5, TimeUnit.SECONDS);
        byte[] expectedSecond = new byte[10];
        System.arraycopy(data, 10, expectedSecond, 0, 10);
        assertThat(secondPart).isEqualTo(expectedSecond);
    }

    @Test
    void bufferBeforeSend_upstreamError_doesNotSendIncompleteBody() throws Exception {
        // When bufferBeforeSend=true and the upstream signals onError() before a part is fully buffered,
        // the incomplete part body should NOT be sent downstream.
        ControlledAsyncRequestBody controlledBody = new ControlledAsyncRequestBody(Optional.of(20L));

        SplittingPublisher splittingPublisher = SplittingPublisher.builder()
                .asyncRequestBody(controlledBody)
                .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                        .chunkSizeInBytes(10L)
                        .bufferSizeInBytes(20L)
                        .build())
                .retryableSubAsyncRequestBodyEnabled(true)
                .bufferBeforeSend(true)
                .build();

        List<CompletableFuture<byte[]>> receivedBodies = new ArrayList<>();
        CompletableFuture<Throwable> downstreamError = new CompletableFuture<>();
        splittingPublisher.subscribe(new Subscriber<CloseableAsyncRequestBody>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(CloseableAsyncRequestBody requestBody) {
                CompletableFuture<byte[]> bodyFuture = new CompletableFuture<>();
                receivedBodies.add(bodyFuture);
                BaosSubscriber subscriber = new BaosSubscriber(bodyFuture);
                requestBody.subscribe(subscriber);
            }

            @Override
            public void onError(Throwable t) {
                downstreamError.complete(t);
            }

            @Override
            public void onComplete() {
            }
        });

        // Give time for subscription
        Thread.sleep(100);

        // Send partial data (less than chunk size of 10)
        controlledBody.sendData(ByteBuffer.wrap(new byte[5]));
        Thread.sleep(100);

        // No body should have been emitted (bufferBeforeSend defers until complete)
        assertThat(receivedBodies.size()).isEqualTo(0);

        // Signal upstream error
        RuntimeException error = new RuntimeException("upstream failure");
        controlledBody.sendError(error);

        // The error should propagate to downstream subscriber
        Throwable receivedError = downstreamError.get(5, TimeUnit.SECONDS);
        assertThat(receivedError).isEqualTo(error);

        // The incomplete body should NOT have been sent downstream
        assertThat(receivedBodies.size()).isEqualTo(0);
    }

    /**
     * A controlled AsyncRequestBody that allows tests to send data, complete, and signal errors
     * at specific times to test deferred/immediate behavior.
     */
    private static class ControlledAsyncRequestBody implements AsyncRequestBody {
        private final Optional<Long> contentLength;
        private volatile Subscriber<? super ByteBuffer> subscriber;
        private volatile Subscription subscription;

        ControlledAsyncRequestBody(Optional<Long> contentLength) {
            this.contentLength = contentLength;
        }

        @Override
        public Optional<Long> contentLength() {
            return contentLength;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            this.subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // Controlled — data is sent explicitly via sendData()
                }

                @Override
                public void cancel() {
                }
            });
        }

        void sendData(ByteBuffer data) {
            subscriber.onNext(data);
        }

        void complete() {
            subscriber.onComplete();
        }

        void sendError(Throwable t) {
            subscriber.onError(t);
        }
    }

    private static final class BaosSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<byte[]> resultFuture;

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private Subscription subscription;

        BaosSubscriber(CompletableFuture<byte[]> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            invokeSafely(() -> baos.write(BinaryUtils.copyBytesFrom(byteBuffer)));
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            baos = null;
            resultFuture.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            resultFuture.complete(baos.toByteArray());
        }
    }
}
