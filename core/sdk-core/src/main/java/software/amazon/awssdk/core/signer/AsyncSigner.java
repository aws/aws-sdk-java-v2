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

package software.amazon.awssdk.core.signer;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * A signer capable of including the contents of the asynchronous body into the request calculation.
 */
@SdkPublicApi
public interface AsyncSigner {
    /**
     * Sign the request, including the contents of the body into the signature calculation.
     *
     * @param request The HTTP request.
     * @param requestBody The body of the request.
     * @param executionAttributes The execution attributes that contains information information used to sign the
     *                            request.
     * @return A future containing the signed request.
     */
    CompletableFuture<SdkHttpFullRequest> sign(SdkHttpFullRequest request, AsyncRequestBody requestBody,
                                               ExecutionAttributes executionAttributes);
}
