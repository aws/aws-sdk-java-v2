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
import static org.mockito.Mockito.doNothing;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.ClosableAsyncRequestBody;
import software.amazon.awssdk.core.exception.NonRetryableException;

public class ClosableAsyncRequestBodyAdaptorTest {

    @Test
    void resubscribe_shouldThrowException() {
        ClosableAsyncRequestBody closableAsyncRequestBody = Mockito.mock(ClosableAsyncRequestBody.class);
        Mockito.when(closableAsyncRequestBody.doAfterOnComplete(any(Runnable.class))).thenReturn(closableAsyncRequestBody);
        Mockito.when(closableAsyncRequestBody.doAfterOnCancel(any(Runnable.class))).thenReturn(closableAsyncRequestBody);
        Mockito.when(closableAsyncRequestBody.doAfterOnError(any(Consumer.class))).thenReturn(closableAsyncRequestBody);

        ClosableAsyncRequestBodyAdaptor adaptor = new ClosableAsyncRequestBodyAdaptor(closableAsyncRequestBody);
        Subscriber subscriber = Mockito.mock(Subscriber.class);
        adaptor.subscribe(subscriber);

        Subscriber anotherSubscriber = Mockito.mock(Subscriber.class);
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        doNothing().when(anotherSubscriber).onError(exceptionCaptor.capture());

        adaptor.subscribe(anotherSubscriber);

        assertThat(exceptionCaptor.getValue())
            .isInstanceOf(NonRetryableException.class)
            .hasMessageContaining("A retry was attempted");
    }

}
