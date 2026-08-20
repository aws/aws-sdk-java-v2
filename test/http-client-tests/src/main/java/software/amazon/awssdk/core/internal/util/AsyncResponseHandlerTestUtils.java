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

package software.amazon.awssdk.core.internal.util;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.async.CombinedResponseAsyncHttpResponseHandler;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;

public class AsyncResponseHandlerTestUtils {
    private AsyncResponseHandlerTestUtils() {
    }

    public static <T> TransformingAsyncResponseHandler<T> noOpResponseHandler() {
        return noOpResponseHandler(null);
    }

    public static <T> TransformingAsyncResponseHandler<T> noOpResponseHandler(T result) {
        return new NoOpResponseHandler<>(result);
    }

    public static <T> TransformingAsyncResponseHandler<T> superSlowResponseHandler(long sleepInMillis) {
        return superSlowResponseHandler(null, sleepInMillis);
    }

    public static <T> TransformingAsyncResponseHandler<T> superSlowResponseHandler(T result, long sleepInMillis) {
        return new SuperSlowResponseHandler<>(result, sleepInMillis);
    }


    public static <T> TransformingAsyncResponseHandler<Response<T>> combinedAsyncResponseHandler(
        TransformingAsyncResponseHandler<T> successResponseHandler,
        TransformingAsyncResponseHandler<? extends SdkException> failureResponseHandler) {

        return new CombinedResponseAsyncHttpResponseHandler<>(
            successResponseHandler == null ? noOpResponseHandler() : successResponseHandler,
            failureResponseHandler == null ? noOpResponseHandler() : failureResponseHandler);
    }

    private static class NoOpResponseHandler<T> implements TransformingAsyncResponseHandler<T> {
        private final CompletableFuture<T> cf = new CompletableFuture<>();
        private final T result;

        NoOpResponseHandler(T result) {
            this.result = result;
        }

        @Override
        public CompletableFuture<T> prepare() {
            return cf;
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new DrainingSubscriber<ByteBuffer>() {
                @Override
                public void onError(Throwable t) {
                    cf.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    cf.complete(result);
                }
            });
        }

        @Override
        public void onError(Throwable error) {
            cf.completeExceptionally(error);
        }
    }

    private static class SuperSlowResponseHandler<T> implements TransformingAsyncResponseHandler<T> {
        private final CompletableFuture<T> cf = new CompletableFuture<>();
        private final T result;
        private final long sleepMillis;

        SuperSlowResponseHandler(T result, long sleepMillis) {
            this.result = result;
            this.sleepMillis = sleepMillis;
        }

        @Override
        public CompletableFuture<T> prepare() {
            return cf.thenApply(r -> {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException ignored) {
                }
                return r;
            });
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new DrainingSubscriber<ByteBuffer>() {
                @Override
                public void onError(Throwable t) {
                    cf.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    cf.complete(result);
                }
            });
        }

        @Override
        public void onError(Throwable error) {
            cf.completeExceptionally(error);
        }
    }
}
