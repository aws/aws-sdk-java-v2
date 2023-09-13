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

package software.amazon.awssdk.identity.spi;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.internal.DefaultIdentityProviders;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An interface to allow retrieving an IdentityProvider based on the identity type.
 */
@SdkPublicApi
public interface IdentityProviders extends ToCopyableBuilder<IdentityProviders.Builder, IdentityProviders> {

    /**
     * Retrieve an identity provider for the provided identity type.
     */
    <T extends Identity> IdentityProvider<T> identityProvider(Class<T> identityType);

    /**
     * Get a new builder for creating a {@link IdentityProviders}.
     */
    static Builder builder() {
        return DefaultIdentityProviders.builder();
    }

    /**
     * A builder for a {@link IdentityProviders}.
     */
    interface Builder extends CopyableBuilder<Builder, IdentityProviders> {

        /**
         * Add the {@link IdentityProvider} for a given type. If a provider of that type, as determined by {@link
         * IdentityProvider#identityType()} is already added, it will be replaced.
         */
        <T extends Identity> Builder putIdentityProvider(IdentityProvider<T> identityProvider);
    }
}
