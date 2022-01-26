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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.listener.AsyncResponseTransformerListener;
import software.amazon.awssdk.utils.Pair;

@SdkProtectedApi
public final class AsyncResponseTransformerUtils {

    private AsyncResponseTransformerUtils() {
    }

    /**
     * Wrap a {@link AsyncResponseTransformer} and associate it with a future that is completed upon end-of-stream, regardless of
     * whether the transformer is configured to complete its future upon end-of-response or end-of-stream.
     */
    public static <ResponseT, ResultT> Pair<AsyncResponseTransformer<ResponseT, ResultT>, CompletableFuture<Void>>
        wrapWithEndOfStreamFuture(AsyncResponseTransformer<ResponseT, ResultT> responseTransformer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AsyncResponseTransformer<ResponseT, ResultT> wrapped = AsyncResponseTransformerListener.wrap(
            responseTransformer,
            new AsyncResponseTransformerListener<ResponseT>() {
                @Override
                public void transformerExceptionOccurred(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void subscriberOnError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void subscriberOnComplete() {
                    future.complete(null);
                }
            });
        return Pair.of(wrapped, future);
    }
}
