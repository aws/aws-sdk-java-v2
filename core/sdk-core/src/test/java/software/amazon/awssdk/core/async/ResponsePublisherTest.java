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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.time.Duration;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkResponse;

class ResponsePublisherTest {

    private SdkResponse response;
    private SdkPublisher<ByteBuffer> publisher;
    private ResponsePublisher<SdkResponse> responsePublisher;

    @BeforeEach
    public void setUp() throws Exception {
        response = mock(SdkResponse.class);
        publisher = mock(SdkPublisher.class);
    }

    @Test
    void defaultTimeout_noSubscriptionBeforeTimeout() throws Exception {
        responsePublisher = new ResponsePublisher<>(response, publisher);
        Thread.sleep(2000);

        verify(publisher, never()).subscribe(any(Subscriber.class));
        assertThat(responsePublisher.hasTimeoutTask()).isTrue();
        assertThat(responsePublisher.timeoutTaskDoneOrCancelled()).isFalse();
    }

    @Test
    void customTimeout_noSubscription_cancelsSubscriptionAfterTimeout() throws Exception {
        responsePublisher = responsePublisher(Duration.ofSeconds(1));
        Thread.sleep(2000);

        ArgumentCaptor<Subscriber<ByteBuffer>> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        verify(publisher, times(1)).subscribe(subscriberCaptor.capture());

        Subscriber<ByteBuffer> cancellingSubscriber = subscriberCaptor.getValue();
        Subscription mockSubscription = mock(Subscription.class);

        cancellingSubscriber.onSubscribe(mockSubscription);
        verify(mockSubscription, times(1)).cancel();

        assertThat(responsePublisher.hasTimeoutTask()).isTrue();
        assertThat(responsePublisher.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void customTimeout_subscribeBeforeTimeout_cancelsTimeoutTask() throws Exception {
        responsePublisher = responsePublisher(Duration.ofSeconds(1));
        responsePublisher.subscribe(new TestSubscriber());
        Thread.sleep(2000);
        
        // Verify only one subscription occurred (test subscriber, not timeout cancellation)
        verify(publisher, times(1)).subscribe(any(TestSubscriber.class));
        verify(publisher, times(1)).subscribe(any(Subscriber.class));
        assertThat(responsePublisher.hasTimeoutTask()).isTrue();
        assertThat(responsePublisher.timeoutTaskDoneOrCancelled()).isTrue();
    }

    @Test
    void zeroTimeout_disablesTimeout() throws Exception {
        responsePublisher = responsePublisher(Duration.ZERO);
        Thread.sleep(2000);

        verify(publisher, never()).subscribe(any(Subscriber.class));
        assertThat(responsePublisher.hasTimeoutTask()).isFalse();
    }

    @Test
    void negativeTimeout_disablesTimeout() throws Exception {
        responsePublisher = responsePublisher(Duration.ofSeconds(-1));
        Thread.sleep(2000);

        verify(publisher, never()).subscribe(any(Subscriber.class));
        assertThat(responsePublisher.hasTimeoutTask()).isFalse();
    }

    private ResponsePublisher<SdkResponse> responsePublisher(Duration timeout) {
        return new ResponsePublisher<>(response, publisher, timeout);
    }

    private static class TestSubscriber implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(ByteBuffer byteBuffer) {}
        
        @Override
        public void onError(Throwable t) {}
        
        @Override
        public void onComplete() {}
    }

    @Test
    void equalsAndHashcode() {
        EqualsVerifier.forClass(ResponsePublisher.class)
                      .withNonnullFields("response", "publisher")
                      .withIgnoredFields("timeoutTask", "subscribed")
                      .verify();
    }
}