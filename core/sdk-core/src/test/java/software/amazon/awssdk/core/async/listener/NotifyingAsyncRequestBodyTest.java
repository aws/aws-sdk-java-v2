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

package software.amazon.awssdk.core.async.listener;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;

public class NotifyingAsyncRequestBodyTest {
    private static AsyncRequestBody mockRequestBody;
    private static AsyncRequestBodyListener mockListener;

    @BeforeEach
    public void setup() {
        mockRequestBody = mock(AsyncRequestBody.class);
        mockListener = mock(AsyncRequestBodyListener.class);
    }

    @Test
    public void contentLength_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.contentLength();
        verify(mockRequestBody).contentLength();
    }

    @Test
    public void contentType_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.contentType();
        verify(mockRequestBody).contentType();
    }

    @Test
    public void subscribe_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.subscribe(mock(Subscriber.class));
        verify(mockRequestBody).subscribe(any(Subscriber.class));
    }

    @Test
    public void split_configObject_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        AsyncRequestBodySplitConfiguration config = AsyncRequestBodySplitConfiguration.builder().build();
        notifying.split(config);
        verify(mockRequestBody).split(eq(config));
    }

    @Test
    public void split_consumer_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        Consumer<AsyncRequestBodySplitConfiguration.Builder> consumer = c -> {};
        notifying.split(consumer);
        verify(mockRequestBody).split(eq(consumer));

    }
}
