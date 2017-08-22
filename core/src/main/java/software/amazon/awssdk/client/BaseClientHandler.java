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

package software.amazon.awssdk.client;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkResponse;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.config.AdvancedClientOption;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.interceptor.InterceptorContext;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.util.AwsRequestMetricsFullSupport;

abstract class BaseClientHandler {
    private final ClientConfiguration clientConfiguration;
    private final ServiceAdvancedConfiguration serviceAdvancedConfiguration;

    BaseClientHandler(ClientConfiguration clientConfiguration,
                      ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        this.clientConfiguration = clientConfiguration;
        this.serviceAdvancedConfiguration = serviceAdvancedConfiguration;
    }

    ExecutionContext createExecutionContext(RequestConfig requestConfig) {
        AwsRequestMetrics requestMetrics = isRequestMetricsEnabled(requestConfig) ? new AwsRequestMetricsFullSupport()
                : new AwsRequestMetrics();

        AwsCredentialsProvider credentialsProvider = requestConfig.getCredentialsProvider() != null
                ? requestConfig.getCredentialsProvider()
                : clientConfiguration.credentialsProvider();

        ClientOverrideConfiguration overrideConfiguration = clientConfiguration.overrideConfiguration();
        ExecutionAttributes executionAttributes =
                new ExecutionAttributes().putAttribute(AwsExecutionAttributes.SERVICE_ADVANCED_CONFIG,
                                                       serviceAdvancedConfiguration)
                                         .putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS,
                                                       credentialsProvider.getCredentials())
                                         .putAttribute(AwsExecutionAttributes.REQUEST_CONFIG, requestConfig);

        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(overrideConfiguration.executionInterceptors()))
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(requestConfig.getOriginalRequest())
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .awsRequestMetrics(requestMetrics)
                               .signerProvider(overrideConfiguration.advancedOption(AdvancedClientOption.SIGNER_PROVIDER))
                               .build();
    }

    /**
     * Returns true if request metric collection is applicable to the given request; false
     * otherwise.
     */
    private boolean isRequestMetricsEnabled(RequestConfig requestConfig) {
        return hasRequestMetricsCollector(requestConfig) || isRmcEnabledAtClientOrSdkLevel();
    }

    private boolean hasRequestMetricsCollector(RequestConfig requestConfig) {
        return requestConfig.getRequestMetricsCollector() != null &&
               requestConfig.getRequestMetricsCollector().isEnabled();
    }

    /**
     * Returns true if request metric collection is enabled at the service client or AWS SDK level
     * request; false otherwise.
     */
    private boolean isRmcEnabledAtClientOrSdkLevel() {
        RequestMetricCollector collector = clientRequestMetricCollector();
        return collector != null && collector.isEnabled();
    }

    /**
     * Returns the client specific request metric collector if there is one; or the one at the AWS
     * SDK level otherwise.
     */
    private RequestMetricCollector clientRequestMetricCollector() {
        RequestMetricCollector clientLevelMetricCollector = clientConfiguration.overrideConfiguration().requestMetricCollector();
        return clientLevelMetricCollector != null ? clientLevelMetricCollector :
                AwsSdkMetrics.getRequestMetricCollector();
    }


    /**
     * Convenient method to end the client execution without logging the awsRequestMetrics.
     */
    void endClientExecution(AwsRequestMetrics awsRequestMetrics,
                            RequestConfig requestConfig,
                            Request<?> request,
                            Object response) {
        if (request != null) {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.ClientExecuteTime);
            awsRequestMetrics.getTimingInfo().endTiming();
            RequestMetricCollector metricCollector = findRequestMetricCollector(requestConfig);
            metricCollector.collectMetrics(request, response);
            awsRequestMetrics.log();
        }
    }

    /**
     * Returns the most specific request metric collector, starting from the request level, then
     * client level, then finally the AWS SDK level.
     */
    private RequestMetricCollector findRequestMetricCollector(RequestConfig requestConfig) {
        RequestMetricCollector reqLevelMetricsCollector = requestConfig.getRequestMetricsCollector();
        return reqLevelMetricsCollector != null ? reqLevelMetricsCollector : clientRequestMetricCollector();
    }

    protected void runBeforeExecutionInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeExecution(executionContext.interceptorContext(),
                                                            executionContext.executionAttributes());
    }

    protected <T> T runModifyRequestInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
                executionContext.interceptorChain().modifyRequest(executionContext.interceptorContext(),
                                                                  executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return (T) interceptorContext.request();
    }

    protected void runBeforeMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeMarshalling(executionContext.interceptorContext(),
                                                              executionContext.executionAttributes());
    }

    protected void addHttpRequest(ExecutionContext executionContext, SdkHttpFullRequest request) {
        InterceptorContext interceptorContext = executionContext.interceptorContext().copy(b -> b.httpRequest(request));
        executionContext.interceptorContext(interceptorContext);
    }

    protected void runAfterMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().afterMarshalling(executionContext.interceptorContext(),
                                                             executionContext.executionAttributes());
    }

    protected SdkHttpFullRequest runModifyHttpRequestInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
                executionContext.interceptorChain().modifyHttpRequest(executionContext.interceptorContext(),
                                                                      executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return interceptorContext.httpRequest();
    }

    private <OutputT extends SdkResponse> OutputT runAfterUnmarshallingInterceptors(OutputT response,
                                                                                    ExecutionContext context) {
        // Update interceptor context to include response
        InterceptorContext interceptorContext =
                context.interceptorContext().copy(b -> b.response(response));

        context.interceptorChain().afterUnmarshalling(interceptorContext, context.executionAttributes());

        interceptorContext = context.interceptorChain().modifyResponse(interceptorContext, context.executionAttributes());

        // Store updated context
        context.interceptorContext(interceptorContext);

        return (OutputT) interceptorContext.response();
    }

    public <OutputT extends SdkResponse> HttpResponseHandler<OutputT> interceptorCalling(HttpResponseHandler<OutputT> delegate,
                                                                                         ExecutionContext context) {
        return (response, executionAttributes) ->
                runAfterUnmarshallingInterceptors(delegate.handle(response, executionAttributes), context);
    }
}
