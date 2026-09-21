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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

/**
 * Internal contract for SDK response transformers that support concatenated-gzip configuration.
 */
@SdkInternalApi
public interface ConfigurableAsyncResponseTransformer<ResponseT, ResultT>
    extends AsyncResponseTransformer<ResponseT, ResultT> {

    AsyncResponseTransformer<ResponseT, ResultT> withConcatenatedGzipStreamSupportEnabled(boolean enabled);

    /**
     * Configures a transformer when it supports this internal contract. Customer transformers are returned unchanged.
     */
    @SuppressWarnings("unchecked")
    static <ResponseT, ResultT> AsyncResponseTransformer<ResponseT, ResultT> configure(
        AsyncResponseTransformer<ResponseT, ResultT> transformer, boolean enabled) {

        if (transformer instanceof ConfigurableAsyncResponseTransformer) {
            return ((ConfigurableAsyncResponseTransformer<ResponseT, ResultT>) transformer)
                .withConcatenatedGzipStreamSupportEnabled(enabled);
        }
        return transformer;
    }
}
