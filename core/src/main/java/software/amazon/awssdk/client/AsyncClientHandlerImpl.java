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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.async.AsyncRequestProvider;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponseAdapter;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.SyncResponseHandlerAdapter;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.util.CredentialUtils;
import software.amazon.awssdk.util.Throwables;

/**
 * Default implementation of {@link ClientHandler}.
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
public class AsyncClientHandlerImpl extends AsyncClientHandler {

    private final AwsCredentialsProvider awsCredentialsProvider;
    private final SignerProvider signerProvider;
    private final URI endpoint;
    private final List<RequestHandler> requestHandlers;
    private final RequestMetricCollector clientLevelMetricCollector;
    private final AmazonAsyncHttpClient client;
    private final boolean calculateCrc32FromCompressedData;

    public AsyncClientHandlerImpl(ClientHandlerParams handlerParams) {
        this.signerProvider = handlerParams.getClientParams().getSignerProvider();
        this.endpoint = handlerParams.getClientParams().getEndpoint();
        this.awsCredentialsProvider = handlerParams.getClientParams().getCredentialsProvider();
        this.requestHandlers = handlerParams.getClientParams().getRequestHandlers();
        this.clientLevelMetricCollector = handlerParams.getClientParams().getRequestMetricCollector();
        this.calculateCrc32FromCompressedData = handlerParams.isCalculateCrc32FromCompressedDataEnabled();
        this.client = buildHttpClient(handlerParams);
    }

    private AmazonAsyncHttpClient buildHttpClient(ClientHandlerParams handlerParams) {
        final AwsSyncClientParams clientParams = handlerParams.getClientParams();
        return AmazonAsyncHttpClient.builder()
                                    .clientConfiguration(clientParams.getClientConfiguration())
                                    .retryPolicy(clientParams.getRetryPolicy())
                                    .requestMetricCollector(clientParams.getRequestMetricCollector())
                                    .calculateCrc32FromCompressedData(handlerParams.isCalculateCrc32FromCompressedDataEnabled())
                                    .asyncExecutor(handlerParams.getAsyncClientParams().getExecutor())
                                    .sdkAsyncHttpClient(handlerParams.getAsyncClientParams().getAsyncHttpClient())
                                    .build();
    }

    @Override
    public <InputT, OutputT> CompletableFuture<OutputT> execute(ClientExecutionParams<InputT, OutputT> executionParams) {
        final InputT inputT = executionParams.getInput();
        ExecutionContext executionContext = createExecutionContext(
                executionParams.getRequestConfig());
        AwsRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.ClientExecuteTime);
        Request<InputT> request;

        awsRequestMetrics.startEvent(AwsRequestMetrics.Field.RequestMarshallTime);
        try {
            request = executionParams.getMarshaller().marshall(tryBeforeMarshalling(inputT));
            request.setAwsRequestMetrics(awsRequestMetrics);
            request.setEndpoint(endpoint);
        } catch (Exception e) {
            endClientExecution(awsRequestMetrics, executionParams.getRequestConfig(), null, null);
            throw e;
        } finally {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.RequestMarshallTime);
        }

        SdkHttpFullRequest marshalled =
                SdkHttpFullRequestAdapter.toMutableHttpFullRequest(request)
                                         .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, executionParams.getRequestConfig())
                                         .build();
        SdkHttpRequestProvider requestProvider = executionParams.getAsyncRequestProvider() != null ?
                adaptAsyncRequestProvider(executionParams.getAsyncRequestProvider()) : null;

        Function<SdkHttpFullResponse, HttpResponse> responseAdapter
                = r -> SdkHttpResponseAdapter.adapt(calculateCrc32FromCompressedData, marshalled, r);

        SdkHttpResponseHandler<OutputT> responseHandler = resolveResponseHandler(executionParams, responseAdapter);

        SdkHttpResponseHandler<? extends SdkBaseException> errorHandler =
                resolveErrorResponseHandler(executionParams, responseAdapter);

        return invoke(marshalled, requestProvider, executionParams.getRequestConfig(), executionContext,
                      responseHandler, errorHandler)
                .handle((resp, err) -> {
                    try {
                        if (err != null) {
                            throw Throwables.failure(err);
                        }
                        return resp;
                    } finally {
                        endClientExecution(awsRequestMetrics, executionParams.getRequestConfig(), request, resp);
                    }
                });
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    /**
     * When an operation has a streaming input, the customer must supply an {@link AsyncRequestProvider} to
     * provide the request content in a non-blocking manner. This adapts that interface to the
     * {@link SdkHttpRequestProvider} which the HTTP client SPI expects.
     *
     * @param asyncRequestProvider Customer supplied request provider.
     * @return Request provider to send to the HTTP layer.
     */
    private SdkHttpRequestProvider adaptAsyncRequestProvider(AsyncRequestProvider asyncRequestProvider) {
        return new SdkHttpRequestProvider() {

            @Override
            public long contentLength() {
                return asyncRequestProvider.contentLength();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                asyncRequestProvider.subscribe(s);
            }

        };
    }

    /**
     * Error responses are never streaming so we always use {@link SyncResponseHandlerAdapter}.
     *
     * @param responseAdapter Adapter to convert an SdkHttpFullResponse to a legacy HttpResponse.
     * @return Async handler for error responses.
     */
    private SdkHttpResponseHandler<? extends SdkBaseException> resolveErrorResponseHandler(
            ClientExecutionParams<?, ?> executionParams,
            Function<SdkHttpFullResponse, HttpResponse> responseAdapter) {
        return new SyncResponseHandlerAdapter<>(executionParams.getErrorResponseHandler(), responseAdapter);
    }

    /**
     * Resolve the async response handler. If this operation has a streaming output then the customer
     * must provide an {@link software.amazon.awssdk.async.AsyncResponseHandler} which will be adapted
     * by the client implementation to a {@link SdkHttpResponseHandler} (unmarshalling is done in this
     * adaption layer). If this operation does not have a streaming output we use {@link SyncResponseHandlerAdapter}
     * to buffer all contents into memory then call out to the sync response handler ({@link
     * software.amazon.awssdk.http.HttpResponseHandler}).
     */
    private <OutputT> SdkHttpResponseHandler<OutputT> resolveResponseHandler(
            ClientExecutionParams<?, OutputT> executionParams,
            Function<SdkHttpFullResponse, HttpResponse> responseAdapter) {
        return executionParams.getResponseHandler() != null ?
                new SyncResponseHandlerAdapter<>(executionParams.getResponseHandler(), responseAdapter) :
                executionParams.getAsyncResponseHandler();
    }

    private ExecutionContext createExecutionContext(RequestConfig requestConfig) {
        boolean isMetricsEnabled = isRequestMetricsEnabled(requestConfig);
        return ExecutionContext.builder()
                               .withRequestHandlers(requestHandlers)
                               .withUseRequestMetrics(isMetricsEnabled)
                               .withSignerProvider(signerProvider)
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
        RequestMetricCollector collector = requestMetricCollector();
        return collector != null && collector.isEnabled();
    }

    /**
     * Returns the client specific request metric collector if there is one; or the one at the AWS
     * SDK level otherwise.
     */
    private RequestMetricCollector requestMetricCollector() {
        return clientLevelMetricCollector != null ? clientLevelMetricCollector :
                AwsSdkMetrics.getRequestMetricCollector();
    }

    /**
     * Super big hack: beforeMarshalling requires an AmazonWebServiceRequest. Here we will try to call it if we can.
     */
    @SuppressWarnings("unchecked")
    @ReviewBeforeRelease("This should be removed when we update the listener system.")
    private <T> T tryBeforeMarshalling(T input) {
        if (input instanceof AmazonWebServiceRequest) {
            return (T) beforeMarshalling((AmazonWebServiceRequest) input);
        }
        return input;
    }

    /**
     * Runs the {@code beforeMarshalling} method of any {@code RequestHandler2}s associated with
     * this client.
     *
     * @param request the request passed in from the user
     * @return The (possibly different) request to marshall
     */
    @SuppressWarnings("unchecked")
    private <T extends AmazonWebServiceRequest> T beforeMarshalling(T request) {
        T local = request;
        for (RequestHandler handler : requestHandlers) {
            local = (T) handler.beforeMarshalling(local);
        }
        return local;
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the
     * request level.
     **/
    private <OutputT> CompletableFuture<OutputT> invoke(SdkHttpFullRequest request,
                                                        SdkHttpRequestProvider requestProvider,
                                                        RequestConfig requestConfig,
                                                        ExecutionContext executionContext,
                                                        SdkHttpResponseHandler<OutputT> responseHandler,
                                                        SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {

        executionContext.setCredentialsProvider(CredentialUtils.getCredentialsProvider(
                requestConfig, awsCredentialsProvider));

        return doInvoke(request, requestProvider, requestConfig,
                        executionContext, responseHandler, errorResponseHandler);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the ExecutionContext beforehand.
     **/
    private <OutputT> CompletableFuture<OutputT> doInvoke(
            SdkHttpFullRequest request,
            SdkHttpRequestProvider requestProvider,
            RequestConfig requestConfig,
            ExecutionContext executionContext,
            SdkHttpResponseHandler<OutputT> responseHandler,
            SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
        return client.requestExecutionBuilder()
                     .requestProvider(requestProvider)
                     .request(request)
                     .requestConfig(requestConfig)
                     .executionContext(executionContext)
                     .errorResponseHandler(errorResponseHandler)
                     .execute(responseHandler);
    }

    /**
     * Convenient method to end the client execution without logging the awsRequestMetrics.
     */
    private void endClientExecution(AwsRequestMetrics awsRequestMetrics,
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
        RequestMetricCollector reqLevelMetricsCollector = requestConfig
                .getRequestMetricsCollector();
        if (reqLevelMetricsCollector != null) {
            return reqLevelMetricsCollector;
        } else if (clientLevelMetricCollector != null) {
            return clientLevelMetricCollector;
        } else {
            return AwsSdkMetrics.getRequestMetricCollector();
        }
    }
}
