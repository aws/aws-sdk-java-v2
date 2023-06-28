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

package software.amazon.awssdk.awscore.internal;

import static software.amazon.awssdk.auth.signer.internal.util.SignerMethodResolver.resolveSigningMethodUsed;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.authcontext.AuthorizationStrategy;
import software.amazon.awssdk.awscore.internal.authcontext.AuthorizationStrategyFactory;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.util.HttpChecksumResolver;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeProvider;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class AwsExecutionContextBuilder {

    private AwsExecutionContextBuilder() {

    }

    /**
     * Used by both sync and async clients to create the execution context, and run initial interceptors.
     */
    public static <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext
        invokeInterceptorsAndCreateExecutionContext(ClientExecutionParams<InputT, OutputT> executionParams,
                                                    SdkClientConfiguration clientConfig) {
        // Note: This is currently copied to DefaultS3Presigner and other presigners.
        // Don't edit this without considering those

        SdkRequest originalRequest = executionParams.getInput();
        MetricCollector metricCollector = resolveMetricCollector(executionParams);

        ExecutionAttributes executionAttributes = mergeExecutionAttributeOverrides(
            executionParams.executionAttributes(),
            clientConfig.option(SdkClientOption.EXECUTION_ATTRIBUTES),
            originalRequest.overrideConfiguration().map(c -> c.executionAttributes()).orElse(null));

        executionAttributes
            .putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 1)
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG,
                          clientConfig.option(SdkClientOption.SERVICE_CONFIGURATION))
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME,
                          clientConfig.option(AwsClientOption.SERVICE_SIGNING_NAME))
            .putAttribute(AwsExecutionAttribute.AWS_REGION, clientConfig.option(AwsClientOption.AWS_REGION))
            .putAttribute(AwsExecutionAttribute.ENDPOINT_PREFIX, clientConfig.option(AwsClientOption.ENDPOINT_PREFIX))
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, clientConfig.option(AwsClientOption.SIGNING_REGION))
            .putAttribute(SdkInternalExecutionAttribute.IS_FULL_DUPLEX, executionParams.isFullDuplex())
            .putAttribute(SdkInternalExecutionAttribute.HAS_INITIAL_REQUEST_EVENT, executionParams.hasInitialRequestEvent())
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, clientConfig.option(SdkClientOption.CLIENT_TYPE))
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, clientConfig.option(SdkClientOption.SERVICE_NAME))
            .putAttribute(SdkExecutionAttribute.PROFILE_FILE, clientConfig.option(SdkClientOption.PROFILE_FILE_SUPPLIER) != null ?
                                                              clientConfig.option(SdkClientOption.PROFILE_FILE_SUPPLIER).get() :
                                                              null)
            .putAttribute(SdkExecutionAttribute.PROFILE_FILE_SUPPLIER, clientConfig.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
            .putAttribute(SdkExecutionAttribute.PROFILE_NAME, clientConfig.option(SdkClientOption.PROFILE_NAME))
            .putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED,
                          clientConfig.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED))
            .putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED,
                          clientConfig.option(AwsClientOption.FIPS_ENDPOINT_ENABLED))
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, executionParams.getOperationName())
            .putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, clientConfig.option(SdkClientOption.ENDPOINT))
            .putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN))
            .putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER, clientConfig.option(SdkClientOption.ENDPOINT_PROVIDER))
            .putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS,
                          clientConfig.option(SdkClientOption.CLIENT_CONTEXT_PARAMS))
            .putAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION,
                          clientConfig.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION))
            .putAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN, clientConfig.option(SdkClientOption.SIGNER_OVERRIDDEN))
            .putAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT,
                          clientConfig.option(AwsClientOption.USE_GLOBAL_ENDPOINT))
            .putAttribute(RESOLVED_CHECKSUM_SPECS, HttpChecksumResolver.resolveChecksumSpecs(executionAttributes));


        // Auth Scheme resolution related attributes
        putAuthSchemeResolutionAttributes(executionAttributes, clientConfig, originalRequest);

        ExecutionInterceptorChain executionInterceptorChain =
                new ExecutionInterceptorChain(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS));

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                     .request(originalRequest)
                                                     .asyncRequestBody(executionParams.getAsyncRequestBody())
                                                     .requestBody(executionParams.getRequestBody())
                                                     .build();
        interceptorContext = runInitialInterceptors(interceptorContext, executionAttributes, executionInterceptorChain);

        Signer signer = null;
        if (isAuthenticatedRequest(executionAttributes)) {
            AuthorizationStrategyFactory authorizationStrategyFactory =
                new AuthorizationStrategyFactory(interceptorContext.request(), metricCollector, clientConfig);
            AuthorizationStrategy authorizationStrategy =
                authorizationStrategyFactory.strategyFor(executionParams.credentialType());
            authorizationStrategy.addCredentialsToExecutionAttributes(executionAttributes);
            signer = authorizationStrategy.resolveSigner();
        }

        executionAttributes.putAttribute(HttpChecksumConstant.SIGNING_METHOD,
                                         resolveSigningMethodUsed(
                                             signer, executionAttributes, executionAttributes.getOptionalAttribute(
                                                 AwsSignerExecutionAttribute.AWS_CREDENTIALS).orElse(null)));

        return ExecutionContext.builder()
                               .interceptorChain(executionInterceptorChain)
                               .interceptorContext(interceptorContext)
                               .executionAttributes(executionAttributes)
                               .signer(signer)
                               .metricCollector(metricCollector)
                               .build();
    }

    private static void putAuthSchemeResolutionAttributes(ExecutionAttributes executionAttributes,
                                                          SdkClientConfiguration clientConfig,
                                                          SdkRequest originalRequest) {

        // TODO: When request-level auth scheme resovler is added, use the request-level auth scheme resolver if the customer
        //  specified an override, otherwise fall back to the one on the client.
        AuthSchemeProvider authSchemeProvider = clientConfig.option(SdkClientOption.AUTH_SCHEME_PROVIDER);

        // Use auth schemes that the user specified at the request level with
        // preference over those on the client.
        // TODO: The request level schemes should be "merged" with client level, with request preferred over client

        // TODO: this may have to be setup at operation level, as some operations may use different signer for same schemeId,
        // e.g., EventStreamV4AuthScheme

        // TODO: If request level override is specified, should each operation check that overridden scheme is the
        //  appropriate type (uses the appropriate Signer) for streaming, etc.
        Map<String, AuthScheme<?>> authSchemes = clientConfig.option(SdkClientOption.AUTH_SCHEMES);

        IdentityProviderConfiguration identityProviders = resolveIdentityProviderConfiguration(originalRequest, clientConfig);

        executionAttributes
            .putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER, authSchemeProvider)
            .putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes)
            .putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDER_CONFIGURATION, identityProviders);
    }

    // TODO: This is hard coding the logic for the credentialsIdentityProvider from AwsRequestOverrideConfiguration.
    //       Currently, AwsRequestOverrideConfiguration does not support overriding the tokenIdentityProvider. When adding that
    //       support this method will need to be updated.
    private static IdentityProviderConfiguration resolveIdentityProviderConfiguration(SdkRequest originalRequest,
                                                                                      SdkClientConfiguration clientConfig) {
        IdentityProviderConfiguration identityProviderConfiguration =
            clientConfig.option(SdkClientOption.IDENTITY_PROVIDER_CONFIGURATION);

        // identityProviderConfiguration can be null, for new core with old client. In this case, even if
        // AwsRequestOverrideConfiguration has credentialsIdentityProvider set (because it is in new core), it is ok to not setup
        // IDENTITY_PROVIDER_CONFIGURATION, as old client won't have AUTH_SCHEME_PROVIDER/AUTH_SCHEMES set either, which are also
        // needed for SRA logic.
        if (identityProviderConfiguration == null) {
            return null;
        }

        return originalRequest.overrideConfiguration()
                              .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                              .map(c -> (AwsRequestOverrideConfiguration) c)
                              .flatMap(AwsRequestOverrideConfiguration::credentialsIdentityProvider)
                              .map(identityProvider ->
                                       identityProviderConfiguration.copy(b -> b.putIdentityProvider(identityProvider)))
                              .orElse(identityProviderConfiguration);
    }

    /**
     * Finalize {@link SdkRequest} by running beforeExecution and modifyRequest interceptors.
     *
     * @param interceptorContext containing the immutable SdkRequest information the interceptor can act on
     * @param executionAttributes mutable container of attributes concerning the execution and request
     * @return the {@link InterceptorContext} returns a context with a new SdkRequest
     */
    public static InterceptorContext runInitialInterceptors(InterceptorContext interceptorContext,
                                                            ExecutionAttributes executionAttributes,
                                                            ExecutionInterceptorChain executionInterceptorChain) {
        executionInterceptorChain.beforeExecution(interceptorContext, executionAttributes);
        return executionInterceptorChain.modifyRequest(interceptorContext, executionAttributes);
    }


    private static <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionAttributes mergeExecutionAttributeOverrides(
        ExecutionAttributes executionAttributes,
        ExecutionAttributes clientOverrideExecutionAttributes,
        ExecutionAttributes requestOverrideExecutionAttributes) {


        executionAttributes.putAbsentAttributes(requestOverrideExecutionAttributes);
        executionAttributes.putAbsentAttributes(clientOverrideExecutionAttributes);

        return executionAttributes;
    }

    private static MetricCollector resolveMetricCollector(ClientExecutionParams<?, ?> params) {
        MetricCollector metricCollector = params.getMetricCollector();
        if (metricCollector == null) {
            metricCollector = MetricCollector.create("ApiCall");
        }
        return metricCollector;
    }

    private static boolean isAuthenticatedRequest(ExecutionAttributes executionAttributes) {
        return executionAttributes.getOptionalAttribute(SdkInternalExecutionAttribute.IS_NONE_AUTH_TYPE_REQUEST).orElse(true);
    }

}
