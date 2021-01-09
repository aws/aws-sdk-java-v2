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

package software.amazon.awssdk.core.internal.http;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * A response handler that returns a transformed response.
 *
 * @param <ResultT> The type of the result.
 */
@SdkInternalApi
public interface TransformingAsyncResponseHandler<ResultT> extends SdkAsyncHttpResponseHandler {
    /**
     * Return the future holding the transformed response.
     * <p>
     * This method is guaranteed to be called before the request is executed, and before {@link
     * SdkAsyncHttpResponseHandler#onHeaders(software.amazon.awssdk.http.SdkHttpResponse)} is signaled.
     *
     * @return The future holding the transformed response.
     */
    CompletableFuture<ResultT> prepare();
}
