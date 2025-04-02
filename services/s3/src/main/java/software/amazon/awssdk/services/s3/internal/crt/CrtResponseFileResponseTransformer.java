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

package software.amazon.awssdk.services.s3.internal.crt;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;

/**
 * When the CRT Response File option is used in a request, the body is streamed directly to the file.
 * This transformer is effectively a no-op transformer that waits for the stream to complete and then
 * completes the future with the response.
 *
 * @param <ResponseT> Pojo response type.
 */
@SdkInternalApi
public final class CrtResponseFileResponseTransformer<ResponseT> implements AsyncResponseTransformer<ResponseT,
    ResponseT> {

    private static final Logger log = Logger.loggerFor(CrtResponseFileResponseTransformer.class);

    private volatile CompletableFuture<Void> cf;
    private volatile ResponseT response;

    @Override
    public CompletableFuture<ResponseT> prepare() {
        cf = new CompletableFuture<>();
        return cf.thenApply(ignored -> response);
    }

    @Override
    public void onResponse(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        publisher.subscribe(new OnCompleteSubscriber(cf, this::exceptionOccurred));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        if (cf != null) {
            cf.completeExceptionally(throwable);
        } else {
            log.warn(() -> "An exception occurred before the call to prepare() was able to instantiate the CompletableFuture."
                           + "The future cannot be completed exceptionally because it is null");

        }
    }

    private static final class OnCompleteSubscriber implements Subscriber<ByteBuffer> {

        private final CompletableFuture<Void> future;
        private final Consumer<Throwable> onErrorMethod;
        private Subscription subscription;

        private OnCompleteSubscriber(CompletableFuture<Void> future, Consumer<Throwable> onErrorMethod) {
            this.future = future;
            this.onErrorMethod = onErrorMethod;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            // Request the first chunk to start producing content
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
        }

        @Override
        public void onError(Throwable throwable) {
            onErrorMethod.accept(throwable);
        }

        @Override
        public void onComplete() {
            future.complete(null);
        }
    }
}
