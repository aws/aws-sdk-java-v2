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

package utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.utils.internal.async.EmptySubscription;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EmptySubscriptionTest {

    @Mock
    private Subscriber<String> mockSubscriber;

    @Test
    public void emptySubscription_with_invalid_request() {
        EmptySubscription emptySubscription = new EmptySubscription(mockSubscriber);
        assertThatIllegalArgumentException().isThrownBy(() -> emptySubscription.request(-1));
    }

    @Test
    public void emptySubscription_with_normal_execution() {
        EmptySubscription emptySubscription = new EmptySubscription(mockSubscriber);
        emptySubscription.request(1);
        verify(mockSubscriber).onComplete();
    }

    @Test
    public void emptySubscription_when_terminated_externally() {
        EmptySubscription emptySubscription = new EmptySubscription(mockSubscriber);
        emptySubscription.cancel();
        emptySubscription.request(1);
        verify(mockSubscriber, never()).onComplete();
    }
}
