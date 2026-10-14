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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.exception.NonRetryableException;

class RetryableSubAsyncRequestBodyTest {

    private SubAsyncRequestBodyConfiguration configuration;
    private Consumer<Long> onNumBytesReceived;
    private Consumer<Long> onNumBytesConsumed;
    private RetryableSubAsyncRequestBody requestBody;

    @BeforeEach
    void setUp() {
        onNumBytesReceived = mock(Consumer.class);
        onNumBytesConsumed = mock(Consumer.class);
        
        configuration = SubAsyncRequestBodyConfiguration.builder()
                .contentLengthKnown(true)
                .maxLength(1024L)
                .partNumber(1)
                .onNumBytesReceived(onNumBytesReceived)
                .onNumBytesConsumed(onNumBytesConsumed)
                .sourceBodyName("test-body")
                .build();
        
        requestBody = new RetryableSubAsyncRequestBody(configuration);
    }

    @Test
    void getters_shouldReturnConfigurationValues() {
        assertThat(requestBody.maxLength()).isEqualTo(1024L);
        assertThat(requestBody.partNumber()).isEqualTo(1);
        assertThat(requestBody.body()).isEqualTo("test-body");
        assertThat(requestBody.contentLength()).isEqualTo(Optional.of(1024L));
        assertThat(requestBody.receivedBytesLength()).isEqualTo(0L);
    }

    @Test
    void constructor_withNullConfiguration_shouldThrowException() {
        assertThatThrownBy(() -> new RetryableSubAsyncRequestBody(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void contentLength_whenContentLengthUnknown_shouldReturnBufferedLength() {
        SubAsyncRequestBodyConfiguration unknownLengthConfig = SubAsyncRequestBodyConfiguration.builder()
                .contentLengthKnown(false)
                .maxLength(1024L)
                .partNumber(1)
                .onNumBytesReceived(onNumBytesReceived)
                .onNumBytesConsumed(onNumBytesConsumed)
                .sourceBodyName("test-body")
                .build();
        
        RetryableSubAsyncRequestBody unknownLengthBody = new RetryableSubAsyncRequestBody(unknownLengthConfig);
        
        assertThat(unknownLengthBody.contentLength()).isEqualTo(Optional.of(0L));
        
        // Send some data
        ByteBuffer data = ByteBuffer.wrap("test".getBytes());
        unknownLengthBody.send(data);
        
        assertThat(unknownLengthBody.contentLength()).isEqualTo(Optional.of(4L));
    }

    @Test
    void subscribe_shouldReceiveAllData() {
        byte[] part1 = RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8);
        byte[] part2 = RandomStringUtils.randomAscii(512).getBytes(StandardCharsets.UTF_8);
        requestBody.send(ByteBuffer.wrap(part1));
        requestBody.send(ByteBuffer.wrap(part2));
        requestBody.complete();
        List<ByteBuffer> receivedBuffers = new ArrayList<>();
        Flowable.fromPublisher(requestBody).forEach(buffer -> receivedBuffers.add(buffer));

        verify(onNumBytesReceived).accept(1024L);
        verify(onNumBytesReceived).accept(512L);
        assertThat(requestBody.receivedBytesLength()).isEqualTo(1536L);
        assertThat(receivedBuffers).containsExactly(ByteBuffer.wrap(part1), ByteBuffer.wrap(part2));
    }

    @Test
    void subscribe_secondTime_shouldUseBufferedBody() {
        byte[] part1 = RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8);
        byte[] part2 = RandomStringUtils.randomAscii(512).getBytes(StandardCharsets.UTF_8);
        requestBody.send(ByteBuffer.wrap(part1));
        requestBody.send(ByteBuffer.wrap(part2));
        requestBody.complete();

        List<ByteBuffer> buffer1 = new ArrayList<>();
        Flowable.fromPublisher(requestBody).forEach(buffer -> buffer1.add(buffer));

        List<ByteBuffer> buffer2 = new ArrayList<>();
        Flowable.fromPublisher(requestBody).forEach(buffer -> buffer2.add(buffer));

        assertThat(buffer1).containsExactly(ByteBuffer.wrap(part1), ByteBuffer.wrap(part2));
        assertThat(buffer2).containsExactly(ByteBuffer.wrap(part1), ByteBuffer.wrap(part2));
    }

    @Test
    void subscribe_retryWithoutFirstSubscriberDone_shouldSendError() {
        Subscriber<ByteBuffer> subscriber1 = mock(Subscriber.class);
        Subscriber<ByteBuffer> subscriber2 = mock(Subscriber.class);

        // First subscription
        requestBody.subscribe(subscriber1);
        // Second subscription without completing first (no buffered data)
        requestBody.subscribe(subscriber2);
        
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriber2).onSubscribe(subscriptionCaptor.capture());
        
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(subscriber2).onError(errorCaptor.capture());
        
        Throwable error = errorCaptor.getValue();
        assertThat(error).isInstanceOf(NonRetryableException.class);
        assertThat(error.getMessage()).contains("data is not buffered successfully for retry");
    }

    @Test
    void subscribe_resubscribeAfterClose_shouldSendError() {
        byte[] data = RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8);
        requestBody.send(ByteBuffer.wrap(data));
        requestBody.complete();

        Flowable.fromPublisher(requestBody).forEach(buffer -> {});

        requestBody.close();
        Subscriber<ByteBuffer> secondSubscriber = mock(Subscriber.class);
        requestBody.subscribe(secondSubscriber);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(secondSubscriber).onSubscribe(subscriptionCaptor.capture());

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(secondSubscriber).onError(errorCaptor.capture());

        Throwable error = errorCaptor.getValue();
        assertThat(error).isInstanceOf(NonRetryableException.class);
        assertThat(error.getMessage()).contains("data is not buffered successfully for retry");
    }

    @Test
    void close_shouldInvokeOnNumBytesConsumed() {
        ByteBuffer data = ByteBuffer.wrap("test data".getBytes());
        requestBody.send(data);
        
        requestBody.close();
        
        verify(onNumBytesConsumed).accept(9L);
    }

    @Test
    @Timeout(30)
    void error_whenBeforeComplete_shouldFailFirstSubscriberAndRetry() throws Exception {
        requestBody.send(ByteBuffer.wrap("hello".getBytes()));
        requestBody.error(new RuntimeException("stream failed"));
        requestBody.complete();

        RecordingSubscriber first = new RecordingSubscriber();
        requestBody.subscribe(first);
        assertThat(first.awaitError()).isInstanceOf(RuntimeException.class).hasMessage("stream failed");

        RecordingSubscriber retry = new RecordingSubscriber();
        requestBody.subscribe(retry);
        assertThat(retry.awaitError()).isInstanceOf(NonRetryableException.class);

        requestBody.close();
        verify(onNumBytesConsumed, times(1)).accept(any());
        verify(onNumBytesConsumed).accept(5L);
    }

    @Test
    @Timeout(30)
    void error_whenAfterComplete_shouldNotAffectCompletedPartOrRetry() throws Exception {
        requestBody.send(ByteBuffer.wrap("hello".getBytes()));
        requestBody.complete();
        requestBody.error(new RuntimeException("stream failed"));

        RecordingSubscriber first = new RecordingSubscriber();
        requestBody.subscribe(first);
        assertThat(first.awaitError()).isNull();
        assertThat(first.bytes.get()).isEqualTo(5);

        RecordingSubscriber retry = new RecordingSubscriber();
        requestBody.subscribe(retry);
        assertThat(retry.awaitError()).isNull();
        assertThat(retry.bytes.get()).isEqualTo(5);
        requestBody.close();
    }

    @Test
    @Timeout(30)
    void complete_whenCalledTwice_shouldKeepRetryBuffer() throws Exception {
        // SplittingPublisher completes the last known-length part from onNext and again from onComplete.
        requestBody.send(ByteBuffer.wrap("hello".getBytes()));
        requestBody.complete();
        requestBody.complete();

        RecordingSubscriber first = new RecordingSubscriber();
        requestBody.subscribe(first);
        assertThat(first.awaitError()).isNull();

        RecordingSubscriber retry = new RecordingSubscriber();
        requestBody.subscribe(retry);
        assertThat(retry.awaitError()).isNull();
        assertThat(retry.bytes.get()).isEqualTo(5);
        requestBody.close();
    }

    @Test
    @Timeout(120)
    void completeAndError_whenRacing_shouldNeverErrorAndReplay() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 2000; i++) {
                RetryableSubAsyncRequestBody body = new RetryableSubAsyncRequestBody(configuration);
                body.send(ByteBuffer.wrap("hello".getBytes()));
                CountDownLatch start = new CountDownLatch(1);
                Future<?> completer = threads.submit(() -> {
                    start.await();
                    body.complete();
                    return null;
                });
                Future<?> failer = threads.submit(() -> {
                    start.await();
                    body.error(new RuntimeException("stream failed"));
                    return null;
                });
                start.countDown();
                completer.get(5, TimeUnit.SECONDS);
                failer.get(5, TimeUnit.SECONDS);

                RecordingSubscriber first = new RecordingSubscriber();
                body.subscribe(first);
                Throwable firstError = first.awaitError();
                RecordingSubscriber retry = new RecordingSubscriber();
                body.subscribe(retry);
                Throwable retryError = retry.awaitError();

                if (firstError != null) {
                    assertThat(retryError).as("iteration %d: errored part must not replay", i)
                                          .isInstanceOf(NonRetryableException.class);
                } else {
                    assertThat(first.bytes.get()).as("iteration %d", i).isEqualTo(5);
                    assertThat(retryError).as("iteration %d: completed part must replay", i).isNull();
                    assertThat(retry.bytes.get()).as("iteration %d", i).isEqualTo(5);
                }
                body.close();
            }
        } finally {
            threads.shutdownNow();
        }
    }

    @Test
    void receivedBytesLength_shouldTrackSentData() {
        assertThat(requestBody.receivedBytesLength()).isEqualTo(0L);
        
        ByteBuffer data1 = ByteBuffer.wrap("hello".getBytes());
        requestBody.send(data1);
        assertThat(requestBody.receivedBytesLength()).isEqualTo(5L);
        
        ByteBuffer data2 = ByteBuffer.wrap(" world".getBytes());
        requestBody.send(data2);
        assertThat(requestBody.receivedBytesLength()).isEqualTo(11L);
    }

    /** Requests everything, counts bytes, and records the terminal signal in a future. */
    private static final class RecordingSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<Void> result = new CompletableFuture<>();
        private final AtomicLong bytes = new AtomicLong();

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            bytes.addAndGet(byteBuffer.remaining());
        }

        @Override
        public void onError(Throwable t) {
            result.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            result.complete(null);
        }

        /** Returns the error, or {@code null} if the subscriber completed. */
        Throwable awaitError() throws Exception {
            return result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
        }
    }
}
