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

package software.amazon.awssdk.core.async;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.utils.Validate;

/**
 * Helper class containing the result of
 * {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration) splitting} an AsyncResponseTransformer. This class
 * holds both the publisher of the individual {@code AsyncResponseTransformer<ResponseT, ResponseT>} and the
 * {@code CompletableFuture <ResultT>} which will complete when the {@code AsyncResponseTransformer} that was split itself would
 * complete.
 *
 * @see AsyncResponseTransformer#split(SplittingTransformerConfiguration)
 * @param <ResponseT> ResponseT of the original AsyncResponseTransformer that was split.
 * @param <ResultT> ResultT of the original AsyncResponseTransformer that was split.
 *
 */
@SdkPublicApi
public final class SplitAsyncResponseTransformer<ResponseT, ResultT> {
    private final SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> asyncResponseTransformerPublisher;
    private final CompletableFuture<ResultT> future;

    private SplitAsyncResponseTransformer(Builder<ResponseT, ResultT> builder) {
        this.asyncResponseTransformerPublisher = Validate.paramNotNull(
            builder.asyncResponseTransformerPublisher, "asyncResponseTransformerPublisher");
        this.future = Validate.paramNotNull(
            builder.future, "future");
    }

    /**
     * The individual {@link AsyncResponseTransformer} will be available through the publisher returned by this method.
     * @return the publisher which publishes the individual {@link AsyncResponseTransformer}
     */
    public SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher() {
        return this.asyncResponseTransformerPublisher;
    }

    /**
     * The future returned by this method will be completed when the future returned by calling the
     * {@link AsyncResponseTransformer#prepare()} method on the AsyncResponseTransformer which was split completes.
     * @return The future
     */
    public CompletableFuture<ResultT> preparedFuture() {
        return this.future;
    }

    public static <ResponseT, ResultT> Builder<ResponseT, ResultT> builder() {
        return new Builder<>();
    }

    public static class Builder<ResponseT, ResultT> {
        private SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> asyncResponseTransformerPublisher;
        private CompletableFuture<ResultT> future;

        public Builder<ResponseT, ResultT> asyncResponseTransformerPublisher(
            SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> asyncResponseTransformerPublisher) {
            this.asyncResponseTransformerPublisher = asyncResponseTransformerPublisher;
            return this;
        }

        public Builder<ResponseT, ResultT> future(CompletableFuture<ResultT> future) {
            this.future = future;
            return this;
        }

        public SplitAsyncResponseTransformer<ResponseT, ResultT> build() {
            return new SplitAsyncResponseTransformer<>(this);
        }
    }
}
