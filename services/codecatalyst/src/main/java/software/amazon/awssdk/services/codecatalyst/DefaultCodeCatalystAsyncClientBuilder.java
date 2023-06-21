/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.codecatalyst.endpoints.CodeCatalystEndpointProvider;

/**
 * Internal implementation of {@link CodeCatalystAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultCodeCatalystAsyncClientBuilder extends
        DefaultCodeCatalystBaseClientBuilder<CodeCatalystAsyncClientBuilder, CodeCatalystAsyncClient> implements
        CodeCatalystAsyncClientBuilder {
    @Override
    public DefaultCodeCatalystAsyncClientBuilder endpointProvider(CodeCatalystEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public DefaultCodeCatalystAsyncClientBuilder tokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final CodeCatalystAsyncClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.asyncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        URI endpointOverride = null;
        if (clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) != null
                && Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfiguration.option(SdkClientOption.ENDPOINT);
        }
        CodeCatalystServiceClientConfiguration serviceClientConfiguration = CodeCatalystServiceClientConfiguration.builder()
                .overrideConfiguration(overrideConfiguration()).region(clientConfiguration.option(AwsClientOption.AWS_REGION))
                .endpointOverride(endpointOverride).build();
        return new DefaultCodeCatalystAsyncClient(serviceClientConfiguration, clientConfiguration);
    }
}
