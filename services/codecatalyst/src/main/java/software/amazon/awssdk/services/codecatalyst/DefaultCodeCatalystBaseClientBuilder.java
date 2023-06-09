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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.codecatalyst.auth.scheme.CodeCatalystAuthSchemeProvider;
import software.amazon.awssdk.services.codecatalyst.endpoints.CodeCatalystEndpointProvider;
import software.amazon.awssdk.services.codecatalyst.endpoints.internal.CodeCatalystEndpointAuthSchemeInterceptor;
import software.amazon.awssdk.services.codecatalyst.endpoints.internal.CodeCatalystRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.codecatalyst.endpoints.internal.CodeCatalystResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultCodeCatalystClientBuilder} and {@link DefaultCodeCatalystAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultCodeCatalystBaseClientBuilder<B extends CodeCatalystBaseClientBuilder<B, C>, C> extends
        AwsDefaultClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "codecatalyst";
    }

    @Override
    protected final String serviceName() {
        return "CodeCatalyst";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
                .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                .option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, defaultTokenProvider())
                .option(SdkAdvancedClientOption.TOKEN_SIGNER, defaultTokenSigner()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new CodeCatalystResolveEndpointInterceptor());
        endpointInterceptors.add(new CodeCatalystEndpointAuthSchemeInterceptor());
        endpointInterceptors.add(new CodeCatalystRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
                .getInterceptors("software/amazon/awssdk/services/codecatalyst/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        return config.toBuilder().option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors).build();
    }

    @Override
    protected final String signingName() {
        return "codecatalyst";
    }

    private CodeCatalystEndpointProvider defaultEndpointProvider() {
        return CodeCatalystEndpointProvider.defaultProvider();
    }

    public B authSchemeProvider(CodeCatalystAuthSchemeProvider authSchemeProvider) {
        clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return thisBuilder();
    }

    private CodeCatalystAuthSchemeProvider defaultAuthSchemeProvider() {
        return CodeCatalystAuthSchemeProvider.defaultProvider();
    }

    private IdentityProvider<? extends TokenIdentity> defaultTokenProvider() {
        return DefaultAwsTokenProvider.create();
    }

    private Signer defaultTokenSigner() {
        return BearerTokenSigner.create();
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(SdkAdvancedClientOption.TOKEN_SIGNER),
                "The 'overrideConfiguration.advancedOption[TOKEN_SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER),
                "The 'tokenProvider' must be configured in the client builder.");
    }
}
