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

package software.amazon.awssdk.utils.async;

import static org.mockito.Mockito.times;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class FlatteningSubscriberTest {
    private Subscriber<String> mockDelegate;
    private Subscription mockUpstream;
    private FlatteningSubscriber<String> flatteningSubscriber;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        mockDelegate = Mockito.mock(Subscriber.class);
        mockUpstream = Mockito.mock(Subscription.class);
        flatteningSubscriber = new FlatteningSubscriber<>(mockDelegate);
    }

    @Test
    public void requestOne() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);
        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));

        Mockito.verify(mockDelegate).onNext("foo");

        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestTwo() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(2);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestThree() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(3);

        Mockito.verify(mockUpstream, times(1)).request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
        Mockito.reset(mockUpstream, mockDelegate);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verify(mockUpstream).request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
        Mockito.reset(mockUpstream, mockDelegate);

        flatteningSubscriber.onNext(Arrays.asList("baz"));

        Mockito.verify(mockDelegate).onNext("baz");
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestInfinite() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);
        downstream.request(Long.MAX_VALUE);
        downstream.request(Long.MAX_VALUE);
        downstream.request(Long.MAX_VALUE);
        downstream.request(Long.MAX_VALUE);

        Mockito.verify(mockUpstream, times(1)).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));
        flatteningSubscriber.onComplete();

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verify(mockDelegate).onComplete();
        Mockito.verifyNoMoreInteractions(mockDelegate);
    }

    @Test
    public void onCompleteDelayedUntilAllDataDelivered() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));
        flatteningSubscriber.onComplete();

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
        Mockito.reset(mockUpstream, mockDelegate);

        downstream.request(1);
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verify(mockDelegate).onComplete();
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void onErrorDropsBufferedData() {
        Throwable t = new Throwable();

        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));
        flatteningSubscriber.onError(t);

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onError(t);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestsFromDownstreamDoNothingAfterOnComplete() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onComplete();

        Mockito.verify(mockDelegate).onComplete();
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);

        downstream.request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestsFromDownstreamDoNothingAfterOnError() {
        Throwable t = new Throwable();

        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onError(t);

        Mockito.verify(mockDelegate).onError(t);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);

        downstream.request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    private Subscription getDownstreamFromDelegate() {
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        Mockito.verify(mockDelegate).onSubscribe(subscriptionCaptor.capture());
        return subscriptionCaptor.getValue();
    }

}