package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.exception.NonRetryableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.utils.BinaryUtils;

public class BufferingAsyncRequestBodyTest {

    private static final ByteBuffer TEST_DATA_SET_1 =
        ByteBuffer.wrap(RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8));
    private static final ByteBuffer TEST_DATA_SET_2 =
        ByteBuffer.wrap(RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8));

    @Test
    public void body_whenCalled_shouldReturnConstantBytes() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        assertEquals("Bytes", body.body());
    }

    @Test
    public void close_whenCalledMultipleTimes_shouldExecuteOnlyOnce() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        Subscriber<Object> mockSubscriber = mock(Subscriber.class);

        body.subscribe(mockSubscriber);

        body.close();
        assertThat(body.bufferedData()).isEmpty();
        body.close();
        body.close();

        verify(mockSubscriber, times(1)).onError(any(NonRetryableException.class));
    }

    @ParameterizedTest
    @MethodSource("contentLengthTestCases")
    public void contentLength_withVariousInputs_shouldReturnExpectedResult(Long inputLength, boolean shouldBePresent,
                                                                           Long expectedValue) {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(inputLength);
        Optional<Long> result = body.contentLength();

        assertThat(result.isPresent()).isEqualTo(shouldBePresent);
        if (shouldBePresent) {
            assertThat(result.get()).isEqualTo(expectedValue);
        }
    }

    private static Stream<Arguments> contentLengthTestCases() {
        return Stream.of(
            Arguments.of(null, false, null),
            Arguments.of(100L, true, 100L),
            Arguments.of(0L, true, 0L)
        );
    }

    @Test
    public void send_whenByteBufferIsNull_shouldThrowNullPointerException() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);

        assertThatThrownBy(() -> body.send(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("data must not be null");
    }

    @Test
    public void subscribe_whenBodyIsClosed_shouldNotifySubscriberWithError() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.complete(); // Set dataReady to true
        body.close(); // Set closed to true
        Subscriber<Object> mockSubscriber = mock(Subscriber.class);

        body.subscribe(mockSubscriber);

        verify(mockSubscriber).onSubscribe(any());
        verify(mockSubscriber).onError(argThat(e ->
                                                   e instanceof NonRetryableException &&
                                                   e.getMessage().equals("AsyncRequestBody has been closed")
        ));
    }

    @Test
    public void subscribe_whenDataNotReady_shouldNotifySubscriberWithError() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        Subscriber<Object> mockSubscriber = mock(Subscriber.class);

        body.subscribe(mockSubscriber);

        verify(mockSubscriber).onSubscribe(any());
        verify(mockSubscriber).onError(argThat(e ->
                                                   e instanceof NonRetryableException &&
                                                   e.getMessage().equals("Unexpected error occurred. Data is not ready to be "
                                                                         + "subscribed")
        ));
    }

    @Test
    public void subscribe_whenMultipleSubscribers_shouldSupportConcurrentSubscriptions() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.send(TEST_DATA_SET_1);
        body.send(TEST_DATA_SET_2);
        body.complete();

        Subscriber<ByteBuffer> firstSubscriber = mock(Subscriber.class);
        Subscriber<ByteBuffer> secondSubscriber = mock(Subscriber.class);

        ArgumentCaptor<Subscription> firstSubscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        ArgumentCaptor<Subscription> secondSubscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);

        body.subscribe(firstSubscriber);
        body.subscribe(secondSubscriber);

        verify(firstSubscriber).onSubscribe(firstSubscriptionCaptor.capture());
        verify(secondSubscriber).onSubscribe(secondSubscriptionCaptor.capture());

        Subscription firstSubscription = firstSubscriptionCaptor.getValue();
        Subscription secondSubscription = secondSubscriptionCaptor.getValue();

        firstSubscription.request(2);
        secondSubscription.request(2);
        verifyData(firstSubscriber);
        verifyData(secondSubscriber);
        verify(firstSubscriber).onComplete();
        verify(secondSubscriber).onComplete();
    }

    private static void verifyData(Subscriber<ByteBuffer> subscriber) {
        verify(subscriber).onNext(
            argThat(buffer -> Arrays.equals(BinaryUtils.copyBytesFrom(buffer), TEST_DATA_SET_1.array())));
        verify(subscriber).onNext(
            argThat(buffer -> Arrays.equals(BinaryUtils.copyBytesFrom(buffer), TEST_DATA_SET_2.array())));
        verify(subscriber).onComplete();
    }

    @Test
    public void send_afterClose_shouldThrowIllegalStateException() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.close();

        assertThatThrownBy(() -> body.send(ByteBuffer.wrap("test".getBytes())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot send data to closed body");
    }

    @Test
    public void send_afterComplete_shouldThrowIllegalStateException() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.complete();

        assertThatThrownBy(() -> body.send(ByteBuffer.wrap("test".getBytes())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Request body has already been completed");
    }

    @Test
    public void complete_afterClose_shouldThrowIllegalStateException() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.close();

        assertThatThrownBy(() -> body.complete())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("The AsyncRequestBody has been closed");
    }

    @Test
    public void complete_calledMultipleTimes_shouldNotThrow() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.complete();
        
        // Should not throw - method returns early if already completed
        body.complete();
        body.complete();
        
        // Verify it's still in completed state
        Subscriber<ByteBuffer> mockSubscriber = mock(Subscriber.class);
        body.subscribe(mockSubscriber);
        verify(mockSubscriber).onSubscribe(any());
    }

    @Test
    public void close_withActiveSubscriptions_shouldNotifyAllSubscribers() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.send(ByteBuffer.wrap("test".getBytes()));
        body.complete();

        Subscriber<ByteBuffer> subscriber1 = mock(Subscriber.class);
        Subscriber<ByteBuffer> subscriber2 = mock(Subscriber.class);
        Subscriber<ByteBuffer> subscriber3 = mock(Subscriber.class);

        body.subscribe(subscriber1);
        body.subscribe(subscriber2);
        body.subscribe(subscriber3);

        body.close();

        verify(subscriber1).onError(argThat(e ->
            e instanceof IllegalStateException &&
            e.getMessage().contains("The publisher has been closed")
        ));
        verify(subscriber2).onError(argThat(e ->
            e instanceof IllegalStateException &&
            e.getMessage().contains("The publisher has been closed")
        ));
        verify(subscriber3).onError(argThat(e ->
            e instanceof IllegalStateException &&
            e.getMessage().contains("The publisher has been closed")
        ));
    }

    @Test
    public void bufferedData_afterClose_shouldBeEmpty() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.send(ByteBuffer.wrap("test1".getBytes()));
        body.send(ByteBuffer.wrap("test2".getBytes()));
        
        assertThat(body.bufferedData()).hasSize(2);
        
        body.close();
        
        assertThat(body.bufferedData()).isEmpty();
    }

    @Test
    public void send_withEmptyByteBuffer_shouldStoreEmptyBuffer() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        
        body.send(emptyBuffer);
        body.complete();

        Subscriber<ByteBuffer> mockSubscriber = mock(Subscriber.class);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);

        body.subscribe(mockSubscriber);
        verify(mockSubscriber).onSubscribe(subscriptionCaptor.capture());

        Subscription subscription = subscriptionCaptor.getValue();
        subscription.request(1);

        verify(mockSubscriber).onNext(argThat(buffer -> buffer.remaining() == 0));
        verify(mockSubscriber).onComplete();
    }

    @Test
    public void concurrentSendAndComplete_shouldBeThreadSafe() throws InterruptedException {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger successfulSends = new AtomicInteger(0);

        // Start multiple threads sending data
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    body.send(ByteBuffer.wrap(("data" + threadId).getBytes()));
                    successfulSends.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Expected if complete() was called first
                } finally {
                    latch.countDown();
                }
            });
        }

        // Complete after a short delay
        Thread.sleep(10);
        body.complete();

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Should have some successful sends and the body should be complete
        assertThat(successfulSends.get()).isGreaterThan(0);
        assertThat(body.bufferedData().size()).isEqualTo(successfulSends.get());
    }

    @Test
    public void concurrentSubscribeAndClose_shouldBeThreadSafe() throws InterruptedException {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        body.send(ByteBuffer.wrap("test".getBytes()));
        body.complete();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger successfulSubscriptions = new AtomicInteger(0);
        AtomicInteger errorNotifications = new AtomicInteger(0);

        // Start multiple threads subscribing
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    Subscriber<ByteBuffer> subscriber = new Subscriber<ByteBuffer>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            successfulSubscriptions.incrementAndGet();
                            s.request(1);
                        }

                        @Override
                        public void onNext(ByteBuffer byteBuffer) {}

                        @Override
                        public void onError(Throwable t) {
                            errorNotifications.incrementAndGet();
                        }

                        @Override
                        public void onComplete() {}
                    };
                    body.subscribe(subscriber);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Close after a short delay
        Thread.sleep(10);
        body.close();

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // All subscribers should have been notified
        assertThat(successfulSubscriptions.get()).isEqualTo(10);
    }

    @Test
    public void subscription_readOnlyBuffers_shouldNotAffectOriginalData() {
        BufferingAsyncRequestBody body = new BufferingAsyncRequestBody(null);
        ByteBuffer originalBuffer = ByteBuffer.wrap("test".getBytes());
        int originalPosition = originalBuffer.position();
        
        body.send(originalBuffer);
        body.complete();

        Subscriber<ByteBuffer> mockSubscriber = mock(Subscriber.class);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

        body.subscribe(mockSubscriber);
        verify(mockSubscriber).onSubscribe(subscriptionCaptor.capture());

        Subscription subscription = subscriptionCaptor.getValue();
        subscription.request(1);

        verify(mockSubscriber).onNext(bufferCaptor.capture());
        ByteBuffer receivedBuffer = bufferCaptor.getValue();

        // Verify the received buffer is read-only
        assertThat(receivedBuffer.isReadOnly()).isTrue();
        
        // Verify original buffer position is unchanged
        assertThat(originalBuffer.position()).isEqualTo(originalPosition);
    }
}
