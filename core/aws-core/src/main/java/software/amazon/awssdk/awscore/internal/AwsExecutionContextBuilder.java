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

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
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
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class AwsExecutionContextBuilder {

    private AwsExecutionContextBuilder() {

    }

    public static <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext
        invokeInterceptorsAndCreateExecutionContext(ClientExecutionParams<InputT, OutputT> executionParams,
                                                    SdkClientConfiguration clientConfig) {

        SdkRequest originalRequest = executionParams.getInput();
        MetricCollector metricCollector = resolveMetricCollector(executionParams);

        ExecutionAttributes executionAttributes = executionParams.executionAttributes();
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
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, clientConfig.option(SdkClientOption.CLIENT_TYPE))
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, clientConfig.option(SdkClientOption.SERVICE_NAME))
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, executionParams.getOperationName())
            .putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN,
                          clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN))
            .putAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN, clientConfig.option(SdkClientOption.SIGNER_OVERRIDDEN));

        ExecutionInterceptorChain executionInterceptorChain =
                new ExecutionInterceptorChain(clientConfig.option(SdkClientOption.EXECUTION_INTERCEPTORS));

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                     .request(originalRequest)
                                                     .asyncRequestBody(executionParams.getAsyncRequestBody())
                                                     .requestBody(executionParams.getRequestBody())
                                                     .build();

        executionInterceptorChain.beforeExecution(interceptorContext, executionAttributes);
        interceptorContext = executionInterceptorChain.modifyRequest(interceptorContext, executionAttributes);

        // beforeExecution and modifyRequest interceptors should avoid dependency on credentials,
        // since they should be resolved after the interceptors run
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                         resolveCredentials(clientConfig, originalRequest, metricCollector));

        return ExecutionContext.builder()
                               .interceptorChain(executionInterceptorChain)
                               .interceptorContext(interceptorContext)
                               .executionAttributes(executionAttributes)
                               .signer(computeSigner(interceptorContext.request(), clientConfig))
                               .metricCollector(metricCollector)
                               .build();
    }

    private static AwsCredentials resolveCredentials(SdkClientConfiguration clientConfig,
                                                     SdkRequest originalRequest,
                                                     MetricCollector metricCollector) {
        AwsCredentialsProvider clientCredentials = clientConfig.option(AwsClientOption.CREDENTIALS_PROVIDER);
        AwsCredentialsProvider credentialsProvider =
            originalRequest.overrideConfiguration()
                           .filter(c -> c instanceof AwsRequestOverrideConfiguration)
                           .map(c -> (AwsRequestOverrideConfiguration) c)
                           .flatMap(AwsRequestOverrideConfiguration::credentialsProvider)
                           .orElse(clientCredentials);

        long credentialsResolveStart = System.nanoTime();
        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        Duration fetchDuration = Duration.ofNanos(System.nanoTime() - credentialsResolveStart);
        metricCollector.reportMetric(CoreMetric.CREDENTIALS_FETCH_DURATION, fetchDuration);

        Validate.validState(credentials != null, "Credential providers must never return null.");
        return credentials;
    }

    private static Signer computeSigner(SdkRequest request,
                                        SdkClientConfiguration clientConfiguration) {
        return request.overrideConfiguration()
                      .flatMap(RequestOverrideConfiguration::signer)
                      .orElseGet(() -> clientConfiguration.option(AwsAdvancedClientOption.SIGNER));
    }

    private static MetricCollector resolveMetricCollector(ClientExecutionParams<?, ?> params) {
        MetricCollector metricCollector = params.getMetricCollector();
        if (metricCollector == null) {
            metricCollector = MetricCollector.create("ApiCall");
        }
        return metricCollector;
    }
}
