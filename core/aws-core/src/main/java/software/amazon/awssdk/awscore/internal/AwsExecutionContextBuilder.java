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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.authcontext.AuthorizationStrategy;
import software.amazon.awssdk.awscore.internal.authcontext.AuthorizationStrategyFactory;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
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

    /**
     * Resolves the credentials provider, with the request override configuration taking precedence over the
     * provided default.
     *
     * @return The credentials provider that will be used by the SDK to resolve credentials
     */
    public static AwsCredentialsProvider resolveCredentialsProvider(SdkRequest originalRequest,
                                                                    AwsCredentialsProvider defaultProvider) {
        return originalRequest.overrideConfiguration()
                              .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                              .map(c -> (AwsRequestOverrideConfiguration) c)
                              .flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                              .orElse(defaultProvider);
    }

    /**
     * Request override signers take precedence over the default alternative, for instance what is specified in the
     * client. Request override signers can also be modified by modifyRequest interceptors.
     *
     * @return The signer that will be used by the SDK to sign the request
     */
    public static Signer resolveSigner(SdkRequest request, Signer defaultSigner) {
        return request.overrideConfiguration()
                      .flatMap(RequestOverrideConfiguration::signer)
                      .orElse(defaultSigner);
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
