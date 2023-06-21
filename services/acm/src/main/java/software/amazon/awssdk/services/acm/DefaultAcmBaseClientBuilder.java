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

package software.amazon.awssdk.services.acm;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.acm.auth.scheme.AcmAuthSchemeProvider;
import software.amazon.awssdk.services.acm.endpoints.AcmEndpointProvider;
import software.amazon.awssdk.services.acm.endpoints.internal.AcmEndpointAuthSchemeInterceptor;
import software.amazon.awssdk.services.acm.endpoints.internal.AcmRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.acm.endpoints.internal.AcmResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultAcmClientBuilder} and {@link DefaultAcmAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultAcmBaseClientBuilder<B extends AcmBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "acm";
    }

    @Override
    protected final String serviceName() {
        return "Acm";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
                .option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new AcmResolveEndpointInterceptor());
        endpointInterceptors.add(new AcmEndpointAuthSchemeInterceptor());
        endpointInterceptors.add(new AcmRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
                .getInterceptors("software/amazon/awssdk/services/acm/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        return config.toBuilder().option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors).build();
    }

    private Signer defaultSigner() {
        return Aws4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "acm";
    }

    private AcmEndpointProvider defaultEndpointProvider() {
        return AcmEndpointProvider.defaultProvider();
    }

    public B authSchemeProvider(AcmAuthSchemeProvider authSchemeProvider) {
        clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return thisBuilder();
    }

    private AcmAuthSchemeProvider defaultAuthSchemeProvider() {
        return AcmAuthSchemeProvider.defaultProvider();
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(SdkAdvancedClientOption.SIGNER),
                "The 'overrideConfiguration.advancedOption[SIGNER]' must be configured in the client builder.");
    }
}
