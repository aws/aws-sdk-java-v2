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
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.ServiceAdvancedConfiguration;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.config.SyncClientConfiguration;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.util.CredentialUtils;

/**
 * Default implementation of {@link ClientHandler}.
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
public class SyncClientHandlerImpl extends ClientHandler {
    private final SyncClientConfiguration syncClientConfiguration;
    private final AmazonHttpClient client;

    @ReviewBeforeRelease("Should this be migrated to use a builder, particularly because it crosses module boundaries?")
    public SyncClientHandlerImpl(SyncClientConfiguration syncClientConfiguration,
                                 ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        super(syncClientConfiguration, serviceAdvancedConfiguration);
        this.syncClientConfiguration = syncClientConfiguration;
        this.client = AmazonHttpClient.builder()
                                      .syncClientConfiguration(syncClientConfiguration)
                                      .build();
    }

    @Override
    public <InputT extends SdkRequest, OutputT> OutputT execute(
            ClientExecutionParams<InputT, OutputT> executionParams) {
        ExecutionContext executionContext = createExecutionContext(executionParams.getRequestConfig());
        runBeforeExecutionInterceptors(executionContext);
        InputT inputT = runModifyRequestInterceptors(executionContext);

        AwsRequestMetrics awsRequestMetrics = executionContext.awsRequestMetrics();
        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.ClientExecuteTime);
        Request<InputT> request = null;
        OutputT response = null;

        try {
            awsRequestMetrics.startEvent(AwsRequestMetrics.Field.RequestMarshallTime);
            try {
                runBeforeMarshallingInterceptors(executionContext);
                request = executionParams.getMarshaller().marshall(inputT);
                request.setAwsRequestMetrics(awsRequestMetrics);
                request.setEndpoint(syncClientConfiguration.endpoint());

                // TODO: Can any of this be merged into the parent class? There's a lot of duplication here.
                executionContext.executionAttributes().putAttribute(AwsExecutionAttributes.SERVICE_NAME,
                                                                    request.getServiceName());
            } finally {
                awsRequestMetrics.endEvent(AwsRequestMetrics.Field.RequestMarshallTime);
            }

            SdkHttpFullRequest marshalled = SdkHttpFullRequestAdapter.toHttpFullRequest(request);
            addHttpRequest(executionContext, marshalled);
            runAfterMarshallingInterceptors(executionContext);
            marshalled = runModifyHttpRequestInterceptors(executionContext);

            response = invoke(marshalled,
                              executionParams.getRequestConfig(),
                              executionContext,
                              executionParams.getResponseHandler(),
                              executionParams.getErrorResponseHandler());
            return response;
        } catch (RuntimeException e) {
            throw e;
        } finally {
            endClientExecution(awsRequestMetrics, executionParams.getRequestConfig(), request, response);
        }
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the
     * request level.
     **/
    private <OutputT> OutputT invoke(SdkHttpFullRequest request,
                                     RequestConfig requestConfig,
                                     ExecutionContext executionContext,
                                     HttpResponseHandler<OutputT> responseHandler,
                                     HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {

        executionContext.setCredentialsProvider(
                CredentialUtils.getCredentialsProvider(requestConfig, syncClientConfiguration.credentialsProvider()));

        return doInvoke(request, requestConfig, executionContext, responseHandler,
                        errorResponseHandler);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the OldExecutionContext beforehand.
     **/
    private <OutputT> OutputT doInvoke(SdkHttpFullRequest request,
                                       RequestConfig requestConfig,
                                       ExecutionContext executionContext,
                                       HttpResponseHandler<OutputT> responseHandler,
                                       HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
        return client.requestExecutionBuilder()
                     .request(request)
                     .requestConfig(requestConfig)
                     .executionContext(executionContext)
                     .errorResponseHandler(errorResponseHandler)
                     .execute(responseHandler);
    }
}
