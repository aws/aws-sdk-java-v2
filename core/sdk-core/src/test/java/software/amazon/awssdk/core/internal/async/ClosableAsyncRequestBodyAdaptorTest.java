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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.observers.BiConsumerSingleObserver;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.ClosableAsyncRequestBody;
import software.amazon.awssdk.core.exception.NonRetryableException;

public class ClosableAsyncRequestBodyAdaptorTest {
    private ClosableAsyncRequestBody closableAsyncRequestBody;

    @BeforeEach
    public void setup() {
        closableAsyncRequestBody =Mockito.mock(ClosableAsyncRequestBody.class);
        Mockito.when(closableAsyncRequestBody.doAfterOnComplete(any(Runnable.class))).thenReturn(closableAsyncRequestBody);
        Mockito.when(closableAsyncRequestBody.doAfterOnCancel(any(Runnable.class))).thenReturn(closableAsyncRequestBody);
        Mockito.when(closableAsyncRequestBody.doAfterOnError(any(Consumer.class))).thenReturn(closableAsyncRequestBody);
    }

    @Test
    void resubscribe_shouldThrowException() {
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

    @Test
    void onComplete_shouldCloseAsyncRequestBody() {
        TestClosableAsyncRequestBody asyncRequestBody = new TestClosableAsyncRequestBody();
        ClosableAsyncRequestBodyAdaptor adaptor = new ClosableAsyncRequestBodyAdaptor(asyncRequestBody);
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        Subscriber<ByteBuffer> subscriber = new ByteArrayAsyncResponseTransformer.BaosSubscriber(future);
        adaptor.subscribe(subscriber);
        assertThat(asyncRequestBody.closeInvoked).isTrue();
    }

    @Test
    void cancel_shouldCloseAsyncRequestBody() {
        TestClosableAsyncRequestBody asyncRequestBody = new TestClosableAsyncRequestBody();
        ClosableAsyncRequestBodyAdaptor adaptor = new ClosableAsyncRequestBodyAdaptor(asyncRequestBody);
        Subscriber<ByteBuffer> subscriber = new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        };
        adaptor.subscribe(subscriber);
        assertThat(asyncRequestBody.closeInvoked).isTrue();
    }

    @Test
    void onError_shouldCloseAsyncRequestBody() {
        OnErrorClosableAsyncRequestBody asyncRequestBody = new OnErrorClosableAsyncRequestBody();
        ClosableAsyncRequestBodyAdaptor adaptor = new ClosableAsyncRequestBodyAdaptor(asyncRequestBody);
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        Subscriber<ByteBuffer> subscriber = new ByteArrayAsyncResponseTransformer.BaosSubscriber(future);
        adaptor.subscribe(subscriber);
        assertThat(asyncRequestBody.closeInvoked).isTrue();
    }


    private static class TestClosableAsyncRequestBody implements ClosableAsyncRequestBody {
        private boolean closeInvoked;

        @Override
        public Optional<Long> contentLength() {
            return Optional.empty();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            Flowable.just(ByteBuffer.wrap("foo bar".getBytes(StandardCharsets.UTF_8)))
                    .subscribe(s);
        }

        @Override
        public void close() {
            closeInvoked = true;
        }
    }

    private static class OnErrorClosableAsyncRequestBody implements ClosableAsyncRequestBody {
        private boolean closeInvoked;

        @Override
        public Optional<Long> contentLength() {
            return Optional.empty();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    s.onError(new IllegalStateException("foobar"));
                }

                @Override
                public void cancel() {

                }
            });
        }

        @Override
        public void close() {
            closeInvoked = true;
        }
    }
}
