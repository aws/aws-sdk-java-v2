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

package software.amazon.awssdk.http.auth.spi.internal;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultIdentityProviderConfiguration implements IdentityProviderConfiguration {

    private final Map<Class<?>, IdentityProvider<?>> identityProviders;

    private DefaultIdentityProviderConfiguration(BuilderImpl builder) {
        this.identityProviders = new HashMap<>(builder.identityProviders);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public <T extends Identity> IdentityProvider<T> identityProvider(Class<T> identityType) {
        return (IdentityProvider<T>) identityProviders.get(identityType);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public String toString() {
        return ToString.builder("IdentityProviderConfiguration")
                       .add("identityProviders", identityProviders)
                       .build();
    }

    private static final class BuilderImpl implements Builder {
        private final Map<Class<?>, IdentityProvider<?>> identityProviders = new HashMap<>();

        private BuilderImpl() {
        }

        private BuilderImpl(DefaultIdentityProviderConfiguration identityProviders) {
            this.identityProviders.putAll(identityProviders.identityProviders);
        }

        @Override
        public <T extends Identity> Builder putIdentityProvider(IdentityProvider<T> identityProvider) {
            Validate.paramNotNull(identityProvider, "identityProvider");
            identityProviders.put(identityProvider.identityType(), identityProvider);
            return this;
        }

        public IdentityProviderConfiguration build() {
            return new DefaultIdentityProviderConfiguration(this);
        }
    }
}
