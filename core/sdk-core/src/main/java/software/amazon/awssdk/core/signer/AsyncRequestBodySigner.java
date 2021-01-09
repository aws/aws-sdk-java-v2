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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Interface for the signer used for signing the async requests.
 */
@SdkPublicApi
@FunctionalInterface
public interface AsyncRequestBodySigner {
    /**
     * Method that takes in an signed request and async request body provider,
     * and returns a transformed version the request body provider.
     *
     * @param request             The signed request (with Authentication header)
     * @param asyncRequestBody    Data publisher of the request body
     * @param executionAttributes Contains the attributes required for signing the request
     * @return The transformed request body provider (with singing operator)
     */
    AsyncRequestBody signAsyncRequestBody(SdkHttpFullRequest request, AsyncRequestBody asyncRequestBody,
        ExecutionAttributes executionAttributes);
}
