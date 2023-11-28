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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.utils.BinaryUtils;

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
        assertThatThrownBy(() -> new SplittingPublisher(body, AsyncRequestBodySplitConfiguration.builder()
                                                                                                .chunkSizeInBytes(10L)
                                                                                                .bufferSizeInBytes(5L)
                                                                                                .build()))
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
    }

    @ParameterizedTest
    @ValueSource(ints = {CHUNK_SIZE, CHUNK_SIZE * 2 - 1, CHUNK_SIZE * 2})
    void differentChunkSize_byteArrayShouldSplitAsyncRequestBodyCorrectly(int chunkSize) throws Exception {
        verifySplitContent(AsyncRequestBody.fromBytes(CONTENT), chunkSize);
    }

    @Test
    void contentLengthNotPresent_shouldHandle() throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        TestAsyncRequestBody asyncRequestBody = new TestAsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }
        };
        SplittingPublisher splittingPublisher = new SplittingPublisher(asyncRequestBody, AsyncRequestBodySplitConfiguration.builder()
                                                                  .chunkSizeInBytes((long) CHUNK_SIZE)
                                                                  .bufferSizeInBytes(10L)
                                                                  .build());


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


    private static void verifySplitContent(AsyncRequestBody asyncRequestBody, int chunkSize) throws Exception {
        SplittingPublisher splittingPublisher = new SplittingPublisher(asyncRequestBody,
                                                                       AsyncRequestBodySplitConfiguration.builder()
                                                                                                         .chunkSizeInBytes((long) chunkSize)
                                                                                                         .bufferSizeInBytes((long) chunkSize * 4)
                                                                                                         .build());

        verifyIndividualAsyncRequestBody(splittingPublisher, testFile.toPath(), chunkSize);
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

    private static final class OnlyRequestOnceSubscriber implements Subscriber<AsyncRequestBody> {
        private List<AsyncRequestBody> asyncRequestBodies = new ArrayList<>();

        @Override
        public void onSubscribe(Subscription s) {
            s.request(1);
        }

        @Override
        public void onNext(AsyncRequestBody requestBody) {
            asyncRequestBodies.add(requestBody);
        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete() {

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
