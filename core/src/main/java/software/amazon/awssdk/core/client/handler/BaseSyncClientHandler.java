/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkProtectedApi
public abstract class BaseSyncClientHandler extends BaseClientHandler implements SyncClientHandler {
    private final SdkClientConfiguration clientConfiguration;
    private final AmazonSyncHttpClient client;

    protected BaseSyncClientHandler(SdkClientConfiguration clientConfiguration,
                                    AmazonSyncHttpClient client) {
        super(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.client = client;
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ResponseTransformer<OutputT, ReturnT> responseTransformer) {

        ExecutionContext executionContext = createExecutionContext(executionParams.getInput());

        HttpResponseHandler<OutputT> decoratedResponseHandlers =
            decorateResponseHandlers(executionParams.getResponseHandler(), executionContext);

        HttpResponseHandler<ReturnT> httpResponseHandler =
            new HttpResponseHandlerAdapter<>(decoratedResponseHandlers, responseTransformer);
        return execute(executionParams, executionContext, httpResponseHandler);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {

        ExecutionContext executionContext = createExecutionContext(executionParams.getInput());

        HttpResponseHandler<OutputT> decoratedResponseHandlers =
            decorateResponseHandlers(executionParams.getResponseHandler(), executionContext);

        return execute(executionParams, executionContext, decoratedResponseHandlers);
    }


    @Override
    public void close() {
        client.close();
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the OldExecutionContext beforehand.
     **/
    protected <OutputT> OutputT invoke(SdkHttpFullRequest request,
                                       SdkRequest originalRequest,
                                       ExecutionContext executionContext,
                                       HttpResponseHandler<OutputT> responseHandler,
                                       HttpResponseHandler<? extends SdkException> errorResponseHandler) {
        return client.requestExecutionBuilder()
                     .request(request)
                     .originalRequest(originalRequest)
                     .executionContext(executionContext)
                     .errorResponseHandler(errorResponseHandler)
                     .execute(responseHandler);
    }

    private <InputT extends SdkRequest, OutputT, ReturnT> ReturnT execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext,
        HttpResponseHandler<ReturnT> responseHandler) {

        InputT inputT = finalizeSdkRequest(executionContext);

        SdkHttpFullRequest marshalled = finalizeSdkHttpFullRequest(executionParams, executionContext, inputT,
                                                                   clientConfiguration);
        return invoke(marshalled,
                      inputT,
                      executionContext,
                      responseHandler,
                      executionParams.getErrorResponseHandler());
    }

    private static class HttpResponseHandlerAdapter<ReturnT, OutputT extends SdkResponse>
        implements HttpResponseHandler<ReturnT> {

        private final HttpResponseHandler<OutputT> httpResponseHandler;
        private final ResponseTransformer<OutputT, ReturnT> responseTransformer;

        private HttpResponseHandlerAdapter(HttpResponseHandler<OutputT> httpResponseHandler,
                                           ResponseTransformer<OutputT, ReturnT> responseTransformer) {
            this.httpResponseHandler = httpResponseHandler;
            this.responseTransformer = responseTransformer;
        }

        @Override
        public ReturnT handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
            OutputT resp = httpResponseHandler.handle(response, executionAttributes);
            return responseTransformer.apply(resp, new AbortableInputStream(response.getContent(), response));
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return responseTransformer.needsConnectionLeftOpen();
        }
    }
}
