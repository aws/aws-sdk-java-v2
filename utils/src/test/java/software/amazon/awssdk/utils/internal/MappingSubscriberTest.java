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

package software.amazon.awssdk.utils.internal;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@RunWith(MockitoJUnitRunner.class)
public class MappingSubscriberTest {
    @Mock
    private Subscription mockSubscription;

    @Mock
    private Subscriber<String> mockSubscriber;

    @Test
    public void verifyNormalFlow() {
        MappingSubscriber<String, String> mappingSubscriber =
                MappingSubscriber.create(mockSubscriber, String::toUpperCase);

        mappingSubscriber.onSubscribe(mockSubscription);
        verify(mockSubscriber).onSubscribe(mockSubscription);
        verifyNoMoreInteractions(mockSubscriber);

        reset(mockSubscriber);
        mappingSubscriber.onNext("one");
        verify(mockSubscriber).onNext("ONE");
        verifyNoMoreInteractions(mockSubscriber);

        reset(mockSubscriber);
        mappingSubscriber.onNext("two");
        verify(mockSubscriber).onNext("TWO");
        verifyNoMoreInteractions(mockSubscriber);

        reset(mockSubscriber);
        mappingSubscriber.onComplete();
        verify(mockSubscriber).onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }

    @Test
    public void verifyMappingExceptionFlow() {
        RuntimeException exception = new IllegalArgumentException("Twos are not supported");

        MappingSubscriber<String, String> mappingSubscriber =
                MappingSubscriber.create(mockSubscriber, s -> {
                    if ("two".equals(s)) {
                        throw exception;
                    }

                    return s.toUpperCase();
                });

        mappingSubscriber.onSubscribe(mockSubscription);
        verify(mockSubscriber).onSubscribe(mockSubscription);
        verifyNoMoreInteractions(mockSubscriber);

        reset(mockSubscriber);
        mappingSubscriber.onNext("one");
        verify(mockSubscriber).onNext("ONE");
        verifyNoMoreInteractions(mockSubscriber);

        reset(mockSubscriber);
        mappingSubscriber.onNext("two");
        verify(mockSubscriber).onError(exception);
        verifyNoMoreInteractions(mockSubscriber);
        verify(mockSubscription).cancel();

        reset(mockSubscriber);
        mappingSubscriber.onNext("three");
        verifyNoMoreInteractions(mockSubscriber);

        reset(mockSubscriber);
        mappingSubscriber.onComplete();
        verifyNoMoreInteractions(mockSubscriber);
    }
}