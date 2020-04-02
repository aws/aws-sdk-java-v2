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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class BufferingSubscriberTest {

    private static final int BUFFER_SIZE = 6;

    private static final Object data = new Object();

    @Mock
    private Subscriber mockSubscriber;

    @Mock
    private Subscription mockSubscription;

    private Subscriber bufferingSubscriber;

    @Before
    public void setup() {
        doNothing().when(mockSubscriber).onSubscribe(any());
        doNothing().when(mockSubscriber).onNext(any());
        doNothing().when(mockSubscriber).onComplete();
        doNothing().when(mockSubscription).request(anyLong());

        bufferingSubscriber = new BufferingSubscriber(mockSubscriber, BUFFER_SIZE);
        bufferingSubscriber.onSubscribe(mockSubscription);
    }

    @Test
    public void onNextNotCalled_WhenCurrentSizeLessThanBufferSize() {
        int count = 3;
        callOnNext(count);

        verify(mockSubscription, times(count)).request(1);
        verify(mockSubscriber, times(0)).onNext(any());
    }

    @Test
    public void onNextIsCalled_onlyWhen_BufferSizeRequirementIsMet() {
        callOnNext(BUFFER_SIZE);

        verify(mockSubscriber, times(1)).onNext(any());
    }

    @Test
    public void onNextIsCalled_DuringOnComplete_WhenBufferNotEmpty() {
        int count = 8;
        callOnNext(count);
        bufferingSubscriber.onComplete();

        verify(mockSubscriber, times(count/BUFFER_SIZE + 1)).onNext(any());
    }

    private void callOnNext(int times) {
        IntStream.range(0, times).forEach(i ->  bufferingSubscriber.onNext(data));
    }

}
