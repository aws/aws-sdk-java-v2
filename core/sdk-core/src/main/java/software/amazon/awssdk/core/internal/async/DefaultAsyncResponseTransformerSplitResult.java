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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultAsyncResponseTransformerSplitResult<ResponseT, ResultT>
    implements AsyncResponseTransformer.SplitResult<ResponseT, ResultT> {

    private final SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher;
    private final CompletableFuture<ResultT> future;

    private DefaultAsyncResponseTransformerSplitResult(Builder<ResponseT, ResultT> builder) {
        this.publisher = Validate.paramNotNull(
            builder.publisher(), "asyncResponseTransformerPublisher");
        this.future = Validate.paramNotNull(
            builder.resultFuture(), "future");
    }

    /**
     * The individual {@link AsyncResponseTransformer} will be available through the publisher returned by this method.
     * @return the publisher which publishes the individual {@link AsyncResponseTransformer}
     */
    public SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher() {
        return this.publisher;
    }

    /**
     * The future returned by this method will be completed when the future returned by calling the
     * {@link AsyncResponseTransformer#prepare()} method on the AsyncResponseTransformer which was split completes.
     * @return The future
     */
    public CompletableFuture<ResultT> resultFuture() {
        return this.future;
    }

    @Override
    public AsyncResponseTransformer.SplitResult.Builder<ResponseT, ResultT> toBuilder() {
        return new DefaultBuilder<>(this);
    }

    public static <ResponseT, ResultT> DefaultBuilder<ResponseT, ResultT> builder() {
        return new DefaultBuilder<>();
    }

    public static class DefaultBuilder<ResponseT, ResultT>
        implements AsyncResponseTransformer.SplitResult.Builder<ResponseT, ResultT> {
        private SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher;
        private CompletableFuture<ResultT> future;

        DefaultBuilder() {
        }

        DefaultBuilder(DefaultAsyncResponseTransformerSplitResult<ResponseT, ResultT> split) {
            this.publisher = split.publisher;
            this.future = split.future;
        }

        @Override
        public SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher() {
            return this.publisher;
        }

        @Override
        public AsyncResponseTransformer.SplitResult.Builder<ResponseT, ResultT> publisher(
            SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher) {
            this.publisher = publisher;
            return this;
        }

        @Override
        public CompletableFuture<ResultT> resultFuture() {
            return this.future;
        }

        @Override
        public AsyncResponseTransformer.SplitResult.Builder<ResponseT, ResultT> resultFuture(CompletableFuture<ResultT> future) {
            this.future = future;
            return this;
        }

        @Override
        public AsyncResponseTransformer.SplitResult<ResponseT, ResultT> build() {
            return new DefaultAsyncResponseTransformerSplitResult<>(this);
        }
    }
}
