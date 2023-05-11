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

package software.amazon.awssdk.http.auth.spi;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;

/**
 * The identity providers configured in the SDK.
 * <p>
 * Used by the {@link HttpAuthScheme} implementation to load any @{@link IdentityProvider}s it needs from the set that are
 * configured on the SDK.
 */
@SdkPublicApi
@FunctionalInterface
public interface IdentityProviderConfiguration {

    /**
     * Retrieve an identity provider for the provided identity type.
     */
    <T extends Identity> IdentityProvider<T> identityProvider(Class<T> identityType);
}
