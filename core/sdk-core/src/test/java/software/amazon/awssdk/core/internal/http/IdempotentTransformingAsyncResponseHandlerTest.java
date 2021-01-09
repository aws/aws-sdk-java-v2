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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;

import software.amazon.awssdk.http.SdkHttpResponse;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentTransformingAsyncResponseHandlerTest {
    private final AtomicInteger scope = new AtomicInteger(1);
    private final CompletableFuture<String> fakeFuture1 = CompletableFuture.completedFuture("one");
    private final CompletableFuture<String> fakeFuture2 = CompletableFuture.completedFuture("two");

    private IdempotentAsyncResponseHandler<String, Integer> handlerUnderTest;

    @Mock
    private TransformingAsyncResponseHandler<String> mockResponseHandler;
    @Mock
    private Publisher<ByteBuffer> mockPublisher;
    @Mock
    private SdkHttpResponse mockSdkHttpResponse;

    @Before
    public void instantiateHandler() {
        handlerUnderTest = IdempotentAsyncResponseHandler.create(mockResponseHandler,
                                                                 scope::get,
                                                                 (x, y) -> x < y + 10);
    }

    @Before
    public void stubMocks() {
        when(mockResponseHandler.prepare()).thenReturn(fakeFuture1).thenReturn(fakeFuture2);
    }

    @Test
    public void prepareDelegatesFirstTime() {
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture1);

        verify(mockResponseHandler).prepare();
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void onErrorDelegates() {
        RuntimeException e = new RuntimeException("boom");
        handlerUnderTest.onError(e);

        verify(mockResponseHandler).onError(e);
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void onStreamDelegates() {
        handlerUnderTest.onStream(mockPublisher);

        verify(mockResponseHandler).onStream(mockPublisher);
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void onHeadersDelegates() {
        handlerUnderTest.onHeaders(mockSdkHttpResponse);

        verify(mockResponseHandler).onHeaders(mockSdkHttpResponse);
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void prepare_unchangedScope_onlyDelegatesOnce() {
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture1);
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture1);

        verify(mockResponseHandler).prepare();
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void prepare_scopeChangedButStillInRange_onlyDelegatesOnce() {
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture1);
        scope.set(2);
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture1);

        verify(mockResponseHandler).prepare();
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void prepare_scopeChangedOutOfRange_delegatesTwice() {
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture1);
        scope.set(11);
        assertThat(handlerUnderTest.prepare()).isSameAs(fakeFuture2);

        verify(mockResponseHandler, times(2)).prepare();
        verifyNoMoreInteractions(mockResponseHandler);
    }

    @Test
    public void prepare_appearsToBeThreadSafe() {
        handlerUnderTest =
            IdempotentAsyncResponseHandler.create(
                mockResponseHandler,
                () -> {
                    int result = scope.get();
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        fail();
                    }
                    return result;
                    },
                Integer::equals);

        IntStream.range(0, 200).parallel().forEach(i -> handlerUnderTest.prepare());
        verify(mockResponseHandler).prepare();
        verifyNoMoreInteractions(mockResponseHandler);
    }
}