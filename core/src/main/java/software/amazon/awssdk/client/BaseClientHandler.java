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

import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkResponse;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.config.AdvancedClientOption;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.interceptor.InterceptorContext;
import software.amazon.awssdk.utils.Validate;

abstract class BaseClientHandler {
    private final ClientConfiguration clientConfiguration;
    private final ServiceAdvancedConfiguration serviceAdvancedConfiguration;

    BaseClientHandler(ClientConfiguration clientConfiguration,
                      ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        this.clientConfiguration = clientConfiguration;
        this.serviceAdvancedConfiguration = serviceAdvancedConfiguration;
    }

    ExecutionContext createExecutionContext(RequestConfig requestConfig) {
        AwsCredentialsProvider credentialsProvider = requestConfig.getCredentialsProvider() != null
                ? requestConfig.getCredentialsProvider()
                : clientConfiguration.credentialsProvider();

        ClientOverrideConfiguration overrideConfiguration = clientConfiguration.overrideConfiguration();

        AwsCredentials credentials = credentialsProvider.getCredentials();

        Validate.validState(credentials != null, "Credential providers must never return null.");

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
                .putAttribute(AwsExecutionAttributes.SERVICE_ADVANCED_CONFIG, serviceAdvancedConfiguration)
                .putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials)
                .putAttribute(AwsExecutionAttributes.REQUEST_CONFIG, requestConfig)
                .putAttribute(AwsExecutionAttributes.AWS_REGION,
                              overrideConfiguration.advancedOption(AdvancedClientOption.AWS_REGION));

        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(overrideConfiguration.lastExecutionInterceptors()))
                               .interceptorContext(InterceptorContext.builder()
                                                                     .request(requestConfig.getOriginalRequest())
                                                                     .build())
                               .executionAttributes(executionAttributes)
                               .signerProvider(overrideConfiguration.advancedOption(AdvancedClientOption.SIGNER_PROVIDER))
                               .build();
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

    private static <OutputT extends SdkResponse> OutputT runAfterUnmarshallingInterceptors(OutputT response,
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

    protected static <OutputT extends SdkResponse>
            HttpResponseHandler<OutputT> interceptorCalling(HttpResponseHandler<OutputT> delegate, ExecutionContext context) {
        return (response, executionAttributes) ->
                runAfterUnmarshallingInterceptors(delegate.handle(response, executionAttributes), context);
    }
}
