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

package software.amazon.awssdk.core.spi.endpoint;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.endpoints.Endpoint;

/**
 * Callback interface for resolving endpoints from the request and execution context.
 */
@FunctionalInterface
@SdkProtectedApi
public interface EndpointResolver {
    /**
     * Resolves the endpoint for the given request.
     *
     * @param request The SDK request (after interceptors have modified it)
     * @param executionAttributes The execution attributes containing client config, region, etc.
     * @return The resolved endpoint
     */
    Endpoint resolve(SdkRequest request, ExecutionAttributes executionAttributes);
}
