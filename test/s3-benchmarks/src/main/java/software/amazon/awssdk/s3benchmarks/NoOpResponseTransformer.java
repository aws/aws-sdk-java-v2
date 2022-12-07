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

package software.amazon.awssdk.s3benchmarks;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * A no-op {@link AsyncResponseTransformer}
 */
public class NoOpResponseTransformer<T> implements AsyncResponseTransformer<T, Void> {
    private CompletableFuture<Void> future;

    @Override
    public CompletableFuture<Void> prepare() {
        future = new CompletableFuture<>();
        return future;
    }

    @Override
    public void onResponse(T response) {
        // do nothing
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        publisher.subscribe(new NoOpSubscriber(future));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
    }

    static class NoOpSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<Void> future;
        private Subscription subscription;

        NoOpSubscriber(CompletableFuture<Void> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            future.complete(null);
        }
    }

}
