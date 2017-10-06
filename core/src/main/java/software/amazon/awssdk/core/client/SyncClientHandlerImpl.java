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

package software.amazon.awssdk.core.client;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.RequestConfig;
import software.amazon.awssdk.core.SdkBaseException;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.config.SyncClientConfiguration;
import software.amazon.awssdk.core.http.AmazonHttpClient;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.StreamingResponseHandler;
import software.amazon.awssdk.core.util.CredentialUtils;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;

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
        this.client = new AmazonHttpClient(syncClientConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            StreamingResponseHandler<OutputT, ReturnT> streamingResponseHandler) {
        ExecutionContext executionContext = createExecutionContext(executionParams.getRequestConfig());
        HttpResponseHandler<OutputT> interceptorCallingResponseHandler =
                interceptorCalling(executionParams.getResponseHandler(), executionContext);
        HttpResponseHandler<ReturnT> httpResponseHandler =
                new HttpResponseHandlerAdapter<>(interceptorCallingResponseHandler, streamingResponseHandler);
        return execute(executionParams, executionContext, httpResponseHandler);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
            ClientExecutionParams<InputT, OutputT> executionParams) {
        ExecutionContext executionContext = createExecutionContext(executionParams.getRequestConfig());
        return execute(executionParams, executionContext, interceptorCalling(executionParams.getResponseHandler(),
                                                                             executionContext));
    }

    private <InputT extends SdkRequest, OutputT, ReturnT> ReturnT execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            ExecutionContext executionContext,
            HttpResponseHandler<ReturnT> responseHandler) {
        runBeforeExecutionInterceptors(executionContext);
        InputT inputT = runModifyRequestInterceptors(executionContext);

        runBeforeMarshallingInterceptors(executionContext);
        Request<InputT> request = executionParams.getMarshaller().marshall(inputT);
        request.setEndpoint(syncClientConfiguration.endpoint());

        // TODO: Can any of this be merged into the parent class? There's a lot of duplication here.
        executionContext.executionAttributes().putAttribute(AwsExecutionAttributes.SERVICE_NAME,
                                                            request.getServiceName());

        SdkHttpFullRequest marshalled = SdkHttpFullRequestAdapter.toHttpFullRequest(request);
        addHttpRequest(executionContext, marshalled);
        runAfterMarshallingInterceptors(executionContext);
        marshalled = runModifyHttpRequestInterceptors(executionContext);

        return invoke(marshalled,
                      executionParams.getRequestConfig(),
                      executionContext,
                      responseHandler,
                      executionParams.getErrorResponseHandler());
    }

    @Override
    public void close() {
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

    private static class HttpResponseHandlerAdapter<ReturnT, OutputT extends SdkResponse>
            implements HttpResponseHandler<ReturnT> {

        private final HttpResponseHandler<OutputT> httpResponseHandler;
        private final StreamingResponseHandler<OutputT, ReturnT> streamingResponseHandler;

        private HttpResponseHandlerAdapter(HttpResponseHandler<OutputT> httpResponseHandler,
                                           StreamingResponseHandler<OutputT, ReturnT> streamingResponseHandler) {
            this.httpResponseHandler = httpResponseHandler;
            this.streamingResponseHandler = streamingResponseHandler;
        }

        @Override
        public ReturnT handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
            OutputT resp = httpResponseHandler.handle(response, executionAttributes);
            return streamingResponseHandler.apply(resp, new AbortableInputStream(response.getContent(), response));
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return streamingResponseHandler.needsConnectionLeftOpen();
        }
    }
}
