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

package software.amazon.awssdk.core.internal.http.async;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 *
 * Response handler for asynchronous non-streaming operations.
 *
 * <p>
 * Adapts an {@link HttpResponseHandler} to the asynchronous {@link TransformingAsyncResponseHandler}. Buffers
 * all content into a {@link ByteArrayInputStream} then invokes the {@link HttpResponseHandler#handle}
 * method.
 *
 * @param <T> Type that the response handler produces.
 */
@SdkInternalApi
public final class AsyncResponseHandler<T> implements TransformingAsyncResponseHandler<T> {
    private volatile CompletableFuture<ByteArrayOutputStream> streamFuture;
    private final HttpResponseHandler<T> responseHandler;
    private final ExecutionAttributes executionAttributes;
    private final Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator;
    private SdkHttpFullResponse.Builder httpResponse;

    public AsyncResponseHandler(HttpResponseHandler<T> responseHandler,
                                Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator,
                                ExecutionAttributes executionAttributes) {
        this.responseHandler = responseHandler;
        this.executionAttributes = executionAttributes;
        this.crc32Validator = crc32Validator;
    }

    @Override
    public void onHeaders(SdkHttpResponse response) {
        this.httpResponse = ((SdkHttpFullResponse) response).toBuilder();
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        publisher.subscribe(new BaosSubscriber(streamFuture));
    }

    @Override
    public void onError(Throwable err) {
        streamFuture.completeExceptionally(err);
    }

    @Override
    public CompletableFuture<T> prepare() {
        streamFuture = new CompletableFuture<>();
        return streamFuture.thenCompose(baos -> {
            ByteArrayInputStream content = new ByteArrayInputStream(baos.toByteArray());
            // Ignore aborts - we already have all of the content.
            AbortableInputStream abortableContent = AbortableInputStream.create(content);
            httpResponse.content(abortableContent);
            try {
                return CompletableFuture.completedFuture(responseHandler.handle(crc32Validator.apply(httpResponse.build()),
                                                                                executionAttributes));
            } catch (Exception e) {
                return CompletableFutureUtils.failedFuture(e);
            }
        });
    }

    private static class BaosSubscriber implements Subscriber<ByteBuffer> {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private final CompletableFuture<ByteArrayOutputStream> streamFuture;
        private Subscription subscription;

        private BaosSubscriber(CompletableFuture<ByteArrayOutputStream> streamFuture) {
            this.streamFuture = streamFuture;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            try {
                baos.write(BinaryUtils.copyBytesFrom(byteBuffer));
                this.subscription.request(1);
            } catch (IOException e) {
                // Should never happen
                streamFuture.completeExceptionally(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            streamFuture.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            streamFuture.complete(baos);
        }
    }
}
