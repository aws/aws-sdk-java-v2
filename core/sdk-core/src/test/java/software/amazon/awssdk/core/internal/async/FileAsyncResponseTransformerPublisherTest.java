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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

class FileAsyncResponseTransformerPublisherTest {

    private FileSystem fileSystem;
    private Path testFile;

    @BeforeEach
    void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        testFile = fileSystem.getPath("/test-file.txt");
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void happyPath_singleOnNext() throws Exception {
        // Given
        AsyncResponseTransformer<Object, Object> initialTransformer = AsyncResponseTransformer.toFile(testFile);

        FileAsyncResponseTransformerPublisher<SdkResponse> publisher =
            new FileAsyncResponseTransformerPublisher<>((FileAsyncResponseTransformer<?>) initialTransformer);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AsyncResponseTransformer<SdkResponse, SdkResponse>> receivedTransformer = new AtomicReference<>();
        CompletableFuture<SdkResponse> future = new CompletableFuture<>();

        // When
        publisher.subscribe(new Subscriber<AsyncResponseTransformer<SdkResponse, SdkResponse>>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<SdkResponse, SdkResponse> transformer) {
                receivedTransformer.set(transformer);

                // Simulate response with content-range header
                SdkResponse mockResponse = createMockResponse();
                CompletableFuture<SdkResponse> prepareFuture = transformer.prepare();
                CompletableFutureUtils.forwardResultTo(prepareFuture, future);
                transformer.onResponse(mockResponse);

                // Simulate stream data
                SdkPublisher<ByteBuffer> mockPublisher = createMockPublisher();
                transformer.onStream(mockPublisher);

                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        // Then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedTransformer.get()).isNotNull();
        assertThat(Files.exists(testFile)).isTrue();
        assertThat(future).isCompleted();
    }

    private SdkResponse createMockResponse() {
        SdkResponse mockResponse = mock(SdkResponse.class);
        SdkHttpResponse mockHttpResponse = mock(SdkHttpResponse.class);

        when(mockResponse.sdkHttpResponse()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.headers()).thenReturn(
            Collections.singletonMap("x-amz-content-range", Collections.singletonList("bytes 0-9/10"))
        );

        return mockResponse;
    }

    private SdkPublisher<ByteBuffer> createMockPublisher() {
        return s -> s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                s.onNext(ByteBuffer.wrap("test data".getBytes()));
                s.onComplete();
            }

            @Override
            public void cancel() {
            }
        });
    }

    @Test
    void multipleOnNext_differentContentRanges() throws Exception {
        // Given
        AsyncResponseTransformer<Object, Object> initialTransformer = AsyncResponseTransformer.toFile(testFile);
        FileAsyncResponseTransformerPublisher<SdkResponse> publisher =
            new FileAsyncResponseTransformerPublisher<>((FileAsyncResponseTransformer<?>) initialTransformer);

        int numTransformers = 8;
        CountDownLatch latch = new CountDownLatch(numTransformers);
        AtomicInteger transformerCount = new AtomicInteger(0);
        List<CompletableFuture<SdkResponse>> futures = new ArrayList<>();

        // When
        publisher.subscribe(new Subscriber<AsyncResponseTransformer<SdkResponse, SdkResponse>>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(numTransformers);
            }

            @Override
            public void onNext(AsyncResponseTransformer<SdkResponse, SdkResponse> transformer) {
                int index = transformerCount.getAndIncrement();

                // Each transformer gets a different 10-byte range
                long startByte = index * 10L;
                long endByte = startByte + 9;
                String contentRange = String.format("bytes %d-%d/80", startByte, endByte);
                byte[] data = new byte[10];
                for (int i = 0; i < 10; i++) {
                    data[i] = (byte) ((byte) startByte + i);
                }

                SdkResponse mockResponse = createMockResponseWithRange(contentRange);
                CompletableFuture<SdkResponse> future = transformer.prepare();
                futures.add(future);

                transformer.onResponse(mockResponse);
                transformer.onStream(createMockPublisherWithData(data));

                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                for (int i = 0; i < numTransformers; i++) {
                    latch.countDown();
                }
            }

            @Override
            public void onComplete() {
            }
        });

        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(transformerCount.get()).isEqualTo(numTransformers);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        assertThat(Files.exists(testFile)).isTrue();
        byte[] fileContent = Files.readAllBytes(testFile);

        assertThat(fileContent.length).isEqualTo(80);

        for (int i = 0; i < numTransformers; i++) {
            int startPos = i * 10;
            byte[] expectedData = new byte[10];
            for (int j = 0; j < 10; j++) {
                expectedData[j] = (byte) ((byte) startPos + j);
            }
            byte[] actualData = Arrays.copyOfRange(fileContent, startPos, startPos + 10);
            assertThat(actualData).isEqualTo(expectedData);
        }
    }

    private SdkResponse createMockResponseWithRange(String contentRange) {
        SdkResponse mockResponse = mock(SdkResponse.class);
        SdkHttpResponse mockHttpResponse = mock(SdkHttpResponse.class);

        when(mockResponse.sdkHttpResponse()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.headers()).thenReturn(
            Collections.singletonMap("x-amz-content-range", Collections.singletonList(contentRange))
        );

        return mockResponse;
    }

    private SdkPublisher<ByteBuffer> createMockPublisherWithData(byte[] data) {
        return s -> s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                s.onNext(ByteBuffer.wrap(data));
                s.onComplete();
            }

            @Override
            public void cancel() {
            }
        });
    }

}