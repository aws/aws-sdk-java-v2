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

package software.amazon.awssdk.core.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.AwsRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestProvider;
import software.amazon.awssdk.core.async.AsyncResponseHandler;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.config.AsyncClientConfiguration;
import software.amazon.awssdk.core.config.InternalAdvancedClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.core.http.SdkHttpResponseAdapter;
import software.amazon.awssdk.core.http.async.SyncResponseHandlerAdapter;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.util.Throwables;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

/**
 * Default implementation of {@link ClientHandler}.
 */
@Immutable
@ThreadSafe
@SdkProtectedApi
class AsyncClientHandlerImpl extends AsyncClientHandler {
    private final AsyncClientConfiguration asyncClientConfiguration;
    private final AmazonAsyncHttpClient client;

    @ReviewBeforeRelease("Should this be migrated to use a params object, particularly because it crosses module boundaries?" +
                         "We might also need to think about how it will work after AWS/SDK are split.")
    AsyncClientHandlerImpl(AsyncClientConfiguration asyncClientConfiguration,
                           ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        super(asyncClientConfiguration, serviceAdvancedConfiguration);
        this.asyncClientConfiguration = asyncClientConfiguration;
        this.client = new AmazonAsyncHttpClient(asyncClientConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams) {
        ExecutionContext executionContext = createExecutionContext(executionParams.getInput());
        return execute(executionParams, executionContext,
            responseAdapter -> new SyncResponseHandlerAdapter<>(
                               interceptorCalling(executionParams.getResponseHandler(), executionContext),
                               responseAdapter,
                               executionContext.executionAttributes()));
    }

    /**
     * Adapter interface from {@link SdkHttpFullResponse} to {@link HttpResponse}
     */
    private interface HttpResponseAdapter extends Function<SdkHttpFullResponse, HttpResponse> {
    }

    /**
     * Factory interface for obtaining an {@link SdkHttpResponseHandler} given an {@link HttpResponseAdapter}.
     *
     * @param <ReturnT> Type param for {@link SdkHttpResponseHandler}
     */
    private interface ResponseHandlerFactory<ReturnT> extends
                                                      Function<HttpResponseAdapter, SdkHttpResponseHandler<ReturnT>> {
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            AsyncResponseHandler<OutputT, ReturnT> asyncResponseHandler) {

        ExecutionContext context = createExecutionContext(executionParams.getInput());
        ResponseHandlerFactory<ReturnT> sdkHttpResponseHandler = responseAdapter ->
                new UnmarshallingSdkHttpResponseHandler<>(asyncResponseHandler, context, executionParams.getResponseHandler());

        return execute(executionParams, context, sdkHttpResponseHandler);
    }

    private <InputT extends SdkRequest, OutputT, ReturnT> CompletableFuture<ReturnT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            ExecutionContext executionContext,
            ResponseHandlerFactory<ReturnT> sdkHttpResponseHandlerFactory) {
        runBeforeExecutionInterceptors(executionContext);
        InputT inputT = runModifyRequestInterceptors(executionContext);

        runBeforeMarshallingInterceptors(executionContext);
        Request<InputT> request = executionParams.getMarshaller().marshall(inputT);
        request.setEndpoint(asyncClientConfiguration.endpoint());

        // TODO: Can any of this be merged into the parent class? There's a lot of duplication here.
        executionContext.executionAttributes().putAttribute(AwsExecutionAttributes.SERVICE_NAME, request.getServiceName());

        addHttpRequest(executionContext, SdkHttpFullRequestAdapter.toHttpFullRequest(request));
        runAfterMarshallingInterceptors(executionContext);
        SdkHttpFullRequest marshalled = runModifyHttpRequestInterceptors(executionContext);

        SdkHttpRequestProvider requestProvider = executionParams.getAsyncRequestProvider() == null
                                                 ? null
                                                 : new SdkHttpRequestProviderAdapter(executionParams.getAsyncRequestProvider());

        HttpResponseAdapter responseAdapter
                = r -> SdkHttpResponseAdapter.adapt(isCalculateCrc32FromCompressedData(), marshalled, r);

        SdkHttpResponseHandler<ReturnT> successResponseHandler = new InterceptorCallingHttpResponseHandler<>(
                sdkHttpResponseHandlerFactory.apply(responseAdapter), executionContext);

        SdkHttpResponseHandler<? extends SdkException> errorHandler =
                resolveErrorResponseHandler(executionParams, responseAdapter, executionContext);

        return invoke(marshalled, requestProvider, inputT,
                      executionContext, successResponseHandler, errorHandler)
                .handle((resp, err) -> {
                    if (err != null) {
                        throw Throwables.failure(err);
                    }
                    return resp;
                });
    }

    private boolean isCalculateCrc32FromCompressedData() {
        return asyncClientConfiguration.overrideConfiguration()
                                       .advancedOption(InternalAdvancedClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED);
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * Error responses are never streaming so we always use {@link SyncResponseHandlerAdapter}.
     *
     * @param responseAdapter Adapter to convert an SdkHttpFullResponse to a legacy HttpResponse.
     * @return Async handler for error responses.
     */
    private SdkHttpResponseHandler<? extends SdkException> resolveErrorResponseHandler(
            ClientExecutionParams<?, ?> executionParams,
            Function<SdkHttpFullResponse, HttpResponse> responseAdapter,
            ExecutionContext executionContext) {
        SyncResponseHandlerAdapter<? extends SdkException> result =
                new SyncResponseHandlerAdapter<>(executionParams.getErrorResponseHandler(),
                                                 responseAdapter,
                                                 executionContext.executionAttributes());
        return new InterceptorCallingHttpResponseHandler<>(result, executionContext);
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the
     * request level.
     **/
    private <InputT extends SdkRequest, OutputT> CompletableFuture<OutputT> invoke(SdkHttpFullRequest request,
                                                        SdkHttpRequestProvider requestProvider,
                                                        InputT originalRequest,
                                                        ExecutionContext executionContext,
                                                        SdkHttpResponseHandler<OutputT> responseHandler,
                                                        SdkHttpResponseHandler<? extends SdkException> errorResponseHandler) {

        if (originalRequest instanceof AwsRequest) {
            AwsCredentialsProvider provider = ((AwsRequest) originalRequest).requestOverrideConfig()
                    .flatMap(c -> c.credentialsProvider())
                    .orElseGet(asyncClientConfiguration::credentialsProvider);
            executionContext.setCredentialsProvider(provider);
        } else {
            executionContext.setCredentialsProvider(asyncClientConfiguration.credentialsProvider());
        }

        return doInvoke(request, requestProvider, originalRequest,
                        executionContext, responseHandler, errorResponseHandler);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the ExecutionContext beforehand.
     **/
    private <InputT extends SdkRequest, OutputT> CompletableFuture<OutputT> doInvoke(
            SdkHttpFullRequest request,
            SdkHttpRequestProvider requestProvider,
            InputT originalRequest,
            ExecutionContext executionContext,
            SdkHttpResponseHandler<OutputT> responseHandler,
            SdkHttpResponseHandler<? extends SdkException> errorResponseHandler) {
        return client.requestExecutionBuilder()
                     .requestProvider(requestProvider)
                     .request(request)
                     .originalRequest(originalRequest)
                     .executionContext(executionContext)
                     .errorResponseHandler(errorResponseHandler)
                     .execute(responseHandler);
    }

    private static class InterceptorCallingHttpResponseHandler<T> implements SdkHttpResponseHandler<T> {
        private final SdkHttpResponseHandler<T> delegate;
        private final ExecutionContext context;

        private InterceptorCallingHttpResponseHandler(SdkHttpResponseHandler<T> delegate, ExecutionContext context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public void headersReceived(SdkHttpResponse response) {
            delegate.headersReceived(beforeUnmarshalling((SdkHttpFullResponse) response, context)); // TODO: Ew
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            delegate.exceptionOccurred(throwable);
        }

        @Override
        public T complete() {
            return delegate.complete();
        }
    }

    private static SdkHttpFullResponse beforeUnmarshalling(SdkHttpFullResponse response, ExecutionContext context) {
        // Update interceptor context to include response
        InterceptorContext interceptorContext =
                context.interceptorContext().copy(b -> b.httpResponse(response));

        // interceptors.afterTransmission
        context.interceptorChain().afterTransmission(interceptorContext, context.executionAttributes());

        // interceptors.modifyHttpResponse
        interceptorContext = context.interceptorChain().modifyHttpResponse(interceptorContext, context.executionAttributes());

        // interceptors.beforeUnmarshalling
        context.interceptorChain().beforeUnmarshalling(interceptorContext, context.executionAttributes());

        // Store updated context
        context.interceptorContext(interceptorContext);

        return interceptorContext.httpResponse();
    }

    /**
     * Adapter to {@link AsyncResponseHandler} that performs unmarshalling and calls {@link
     * software.amazon.awssdk.core.interceptor.ExecutionInterceptor}
     * callbacks.
     *
     * @param <OutputT> Unmarshalled POJO response type.
     * @param <ReturnT> Return type of {@link AsyncResponseHandler}
     */
    private static class UnmarshallingSdkHttpResponseHandler<OutputT extends SdkResponse, ReturnT>
            implements SdkHttpResponseHandler<ReturnT> {

        private final AsyncResponseHandler<OutputT, ReturnT> asyncResponseHandler;
        private final ExecutionContext executionContext;
        private final HttpResponseHandler<OutputT> responseHandler;

        UnmarshallingSdkHttpResponseHandler(AsyncResponseHandler<OutputT, ReturnT> asyncResponseHandler,
                                            ExecutionContext executionContext,
                                            HttpResponseHandler<OutputT> responseHandler) {
            this.asyncResponseHandler = asyncResponseHandler;
            this.executionContext = executionContext;
            this.responseHandler = responseHandler;
        }


        @Override
        public void headersReceived(SdkHttpResponse response) {
            HttpResponse httpResponse = SdkHttpResponseAdapter.adapt(false, null, ((SdkHttpFullResponse) response));
            try {
                // TODO would be better to pass in AwsExecutionAttributes to the async response handler so we can
                // provide them to HttpResponseHandler
                OutputT resp = interceptorCalling(responseHandler, executionContext).handle(httpResponse, null);
                asyncResponseHandler.responseReceived(resp);
            } catch (Exception e) {
                throw Throwables.failure(e);
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            asyncResponseHandler.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            asyncResponseHandler.exceptionOccurred(throwable);
        }

        @Override
        public ReturnT complete() {
            return asyncResponseHandler.complete();
        }
    }

    /**
     * When an operation has a streaming input, the customer must supply an {@link AsyncRequestProvider} to
     * provide the request content in a non-blocking manner. This adapts that interface to the
     * {@link SdkHttpRequestProvider} which the HTTP client SPI expects.
     */
    private static class SdkHttpRequestProviderAdapter implements SdkHttpRequestProvider {

        private final AsyncRequestProvider asyncRequestProvider;

        private SdkHttpRequestProviderAdapter(AsyncRequestProvider asyncRequestProvider) {
            this.asyncRequestProvider = asyncRequestProvider;
        }

        @Override
        public long contentLength() {
            return asyncRequestProvider.contentLength();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            asyncRequestProvider.subscribe(s);
        }

    }
}
