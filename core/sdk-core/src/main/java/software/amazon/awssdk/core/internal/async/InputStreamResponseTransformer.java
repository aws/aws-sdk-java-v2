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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.async.AbortableInputStreamSubscriber;

/**
 * A {@link AsyncResponseTransformer} that allows performing blocking reads on the response data.
 * <p>
 * Created with {@link AsyncResponseTransformer#toBlockingInputStream()}.
 */
@SdkInternalApi
public class InputStreamResponseTransformer<ResponseT extends SdkResponse>
    implements AsyncResponseTransformer<ResponseT, ResponseInputStream<ResponseT>> {

    private volatile CompletableFuture<ResponseInputStream<ResponseT>> future;
    private volatile ResponseT response;
    private volatile WaitForSubscribeOnErrorWrapper subscriber;

    @Override
    public CompletableFuture<ResponseInputStream<ResponseT>> prepare() {
        CompletableFuture<ResponseInputStream<ResponseT>> result = new CompletableFuture<>();
        this.future = result;
        return result;
    }

    @Override
    public void onResponse(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        AbortableInputStreamSubscriber inputStreamSubscriber = AbortableInputStreamSubscriber.builder().build();
        WaitForSubscribeOnErrorWrapper waitForSubscribeSubscriber = new WaitForSubscribeOnErrorWrapper(inputStreamSubscriber);

        this.subscriber = waitForSubscribeSubscriber;

        publisher.subscribe(waitForSubscribeSubscriber);
        future.complete(new ResponseInputStream<>(response, inputStreamSubscriber));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
        if (subscriber != null) {
            this.subscriber.onError(error);
        }
    }

    @Override
    public String name() {
        return TransformerType.STREAM.getName();
    }

    // Simple wrapper subscriber that ensures we don't forward the `onError` to the delegate until onSubscribe is called, to be
    // compliant with the reactive streams spec. We use onError for forwarding the exception given to exceptionOccurred.
    private static final class WaitForSubscribeOnErrorWrapper implements Subscriber<ByteBuffer> {
        private final Object lock = new Object();
        private final AbortableInputStreamSubscriber delegate;

        private boolean subscribed = false;
        private Throwable transformerException;


        private WaitForSubscribeOnErrorWrapper(AbortableInputStreamSubscriber delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSubscribe(Subscription s) {
            synchronized (lock) {
                subscribed = true;
                delegate.onSubscribe(s);

                if (transformerException != null) {
                    delegate.onError(transformerException);
                    transformerException = null;
                }
            }
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            this.delegate.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            synchronized (lock) {
                if (subscribed) {
                    delegate.onError(t);
                } else {
                    // We're not subscribed yet, save the exception for until we are.
                    transformerException = t;
                }
            }
        }

        @Override
        public void onComplete() {
            this.delegate.onComplete();
        }
    }
}
