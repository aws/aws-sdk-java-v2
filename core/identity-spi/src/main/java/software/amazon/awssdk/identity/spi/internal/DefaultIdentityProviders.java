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

package software.amazon.awssdk.identity.spi.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * A default implementation of {@link IdentityProviders}. This implementation holds a map of {@link IdentityProvider}s and
 * retrieves from the collection based on identity type.
 */
@Immutable
@SdkInternalApi
public final class DefaultIdentityProviders implements IdentityProviders {
    // /**
    //  * TODO(sra-identity-auth): Currently, some customers assume we won't interact with the identity providers when we create
    //  * the client. This isn't true - we need to call identityType. To TEMPORARILY work around those customer's tests failing,
    //  * this is marked lazy. Once we fully migrate over to the SRA as the default code path, we should remove this lazy and
    //  * ticket everyone in live who is making those bad assumptions.
    //  */
    private final Map<Class<?>, IdentityProvider<?>> identityProviders;
    private final List<IdentityProvider<?>> identityProvidersList;

    private DefaultIdentityProviders(BuilderImpl builder) {
        this.identityProvidersList = new ArrayList<>(builder.identityProviders);
        this.identityProviders = identityProvidersList.stream().collect(
            Collectors.toMap(identityProvider -> identityProvider.identityType(),
                             Function.identity()));
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
        return ToString.builder("IdentityProviders")
                       .add("identityProviders", identityProvidersList)
                       .build();
    }

    private static final class BuilderImpl implements Builder {
        private final List<IdentityProvider<?>> identityProviders = new ArrayList<>();

        private BuilderImpl() {
        }

        private BuilderImpl(DefaultIdentityProviders identityProviders) {
            this.identityProviders.addAll(identityProviders.identityProvidersList);
        }

        @Override
        public <T extends Identity> Builder putIdentityProvider(IdentityProvider<T> identityProvider) {
            Validate.paramNotNull(identityProvider, "identityProvider");
            identityProviders.add(identityProvider);
            return this;
        }

        public IdentityProviders build() {
            return new DefaultIdentityProviders(this);
        }
    }
}
