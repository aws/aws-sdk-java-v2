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

package software.amazon.awssdk.core.spi.identity;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * Callback interface for updating identity providers based on request-level overrides.
 * <p>
 * This allows aws-core to provide AWS-specific logic for reading credential overrides
 * from {@code AwsRequestOverrideConfiguration} without sdk-core depending on aws-core.
 */
@FunctionalInterface
@SdkProtectedApi
public interface IdentityProviderUpdater {
    /**
     * Updates identity providers based on request-level overrides.
     *
     * @param request The request (after interceptors have modified it)
     * @param base The base identity providers from client configuration
     * @return Updated identity providers, or base if no overrides
     */
    IdentityProviders update(SdkRequest request, IdentityProviders base);
}
