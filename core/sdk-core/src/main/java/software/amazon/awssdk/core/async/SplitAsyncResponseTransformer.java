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
import software.amazon.awssdk.core.internal.async.DefaultSplitAsyncResponseTransformer;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Helper class containing the result of {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration) splitting} an
 * AsyncResponseTransformer. This class holds both the publisher of the individual
 * {@code AsyncResponseTransformer<ResponseT, ResponseT>} and the {@code CompletableFuture <ResultT>} which will complete when the
 * {@code AsyncResponseTransformer} that was split itself would complete.
 *
 * @param <ResponseT> ResponseT of the original AsyncResponseTransformer that was split.
 * @param <ResultT>   ResultT of the original AsyncResponseTransformer that was split.
 * @see AsyncResponseTransformer#split(SplittingTransformerConfiguration)
 */
@SdkPublicApi
public interface SplitAsyncResponseTransformer<ResponseT, ResultT>
    extends ToCopyableBuilder<SplitAsyncResponseTransformer.Builder<ResponseT, ResultT>,
    SplitAsyncResponseTransformer<ResponseT, ResultT>> {

    /**
     * The individual {@link AsyncResponseTransformer} will be available through the publisher returned by this method.
     *
     * @return the publisher which publishes the individual {@link AsyncResponseTransformer}
     */
    SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher();

    /**
     * The future returned by this method will be completed when the future returned by calling the
     * {@link AsyncResponseTransformer#prepare()} method on the AsyncResponseTransformer which was split completes.
     *
     * @return The future
     */
    CompletableFuture<ResultT> preparedFuture();


    static <ResponseT, ResultT> Builder<ResponseT, ResultT> builder() {
        return DefaultSplitAsyncResponseTransformer.builder();
    }

    interface Builder<ResponseT, ResultT>
        extends CopyableBuilder<Builder<ResponseT, ResultT>, SplitAsyncResponseTransformer<ResponseT, ResultT>> {

        /**
         * @return the publisher which was configured on this Builder instance.
         */
        SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher();

        /**
         * Sets the publisher publishing the individual {@link AsyncResponseTransformer}
         * @param publisher the publisher
         * @return an instance of this Builder
         */
        Builder<ResponseT, ResultT> publisher(SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> publisher);

        /**
         * @return The future which was configured an this Builder instance.
         */
        CompletableFuture<ResultT> preparedFuture();

        /**
         * Sets the future that will be completed when the future returned by calling the
         * {@link AsyncResponseTransformer#prepare()} method on the AsyncResponseTransformer which was split completes.
         * @param future the future
         * @return an instance of this Builder
         */
        Builder<ResponseT, ResultT> preparedFuture(CompletableFuture<ResultT> future);

    }

}
