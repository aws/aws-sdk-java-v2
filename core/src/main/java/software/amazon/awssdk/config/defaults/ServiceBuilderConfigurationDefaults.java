/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.config.defaults;

import static software.amazon.awssdk.config.AdvancedClientOption.SIGNER_PROVIDER;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.handlers.HandlerChainFactory;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * An implementation of {@link ClientConfigurationDefaults} that can be used by client builders to define their service defaults.
 */
@SdkProtectedApi
public class ServiceBuilderConfigurationDefaults extends ClientConfigurationDefaults {

    private final Supplier<SignerProvider> defaultSignerProvider;
    private final Supplier<URI> defaultEndpoint;
    private final List<String> requestHandlerPaths;

    private ServiceBuilderConfigurationDefaults(Builder builder) {
        this.defaultSignerProvider = builder.defaultSignerProvider;
        this.defaultEndpoint = builder.defaultEndpoint;
        this.requestHandlerPaths = new ArrayList<>(builder.requestHandlerPaths);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
        ClientOverrideConfiguration config = builder.build();
        builder.advancedOption(SIGNER_PROVIDER,
                               applyDefault(config.advancedOption(SIGNER_PROVIDER), defaultSignerProvider));
        HandlerChainFactory chainFactory = new HandlerChainFactory();
        requestHandlerPaths.forEach(path -> chainFactory.newRequestHandlerChain(path).forEach(builder::addRequestListener));
    }

    @Override
    protected URI getEndpointDefault() {
        return defaultEndpoint.get();
    }

    public static final class Builder {

        private Supplier<SignerProvider> defaultSignerProvider;
        private Supplier<URI> defaultEndpoint;
        private List<String> requestHandlerPaths = new ArrayList<>();

        private Builder() {}

        public Builder defaultSignerProvider(Supplier<SignerProvider> defaultSignerProvider) {
            this.defaultSignerProvider = defaultSignerProvider;
            return this;
        }

        public Builder defaultEndpoint(Supplier<URI> endpoint) {
            this.defaultEndpoint = endpoint;
            return this;
        }

        public Builder addRequestHandlerPath(String handlerPath) {
            requestHandlerPaths.add(handlerPath);
            return this;
        }

        public ServiceBuilderConfigurationDefaults build() {
            return new ServiceBuilderConfigurationDefaults(this);
        }
    }
}