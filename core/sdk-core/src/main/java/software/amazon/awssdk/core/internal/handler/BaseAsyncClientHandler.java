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

package software.amazon.awssdk.core.internal.handler;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.internal.http.IdempotentAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncAfterTransmissionInterceptorCallingResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncStreamingResponseHandler;
import software.amazon.awssdk.core.internal.http.async.CombinedResponseAsyncHttpResponseHandler;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public abstract class BaseAsyncClientHandler extends BaseClientHandler implements AsyncClientHandler {
    private static final Logger log = Logger.loggerFor(BaseAsyncClientHandler.class);
    private final AmazonAsyncHttpClient client;
    private final Function<SdkHttpFullResponse, SdkHttpFullResponse> crc32Validator;

    protected BaseAsyncClientHandler(SdkClientConfiguration clientConfiguration,
                                     AmazonAsyncHttpClient client) {
        super(clientConfiguration);
        this.client = client;
        this.crc32Validator = response -> Crc32Validation.validate(isCalculateCrc32FromCompressedData(), response);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams) {

        return measureApiCallSuccess(executionParams, () -> {
            // Running beforeExecution interceptors and modifyRequest interceptors.
            SdkClientConfiguration clientConfiguration = createRequestConfiguration(executionParams);
            ExecutionContext executionContext = invokeInterceptorsAndCreateExecutionContext(executionParams, clientConfiguration);

            TransformingAsyncResponseHandler<Response<OutputT>> combinedResponseHandler =
                createCombinedResponseHandler(executionParams, executionContext);

            return doExecute(clientConfiguration, executionParams, executionContext, combinedResponseHandler);
        });
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
        ClientExecutionParams<InputT, OutputT> executionParams,
        AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {

        return measureApiCallSuccess(executionParams, () -> {
            if (executionParams.getCombinedResponseHandler() != null) {
                // There is no support for catching errors in a body for streaming responses. Our codegen must never
                // attempt to do this.
                throw new IllegalArgumentException("A streaming 'asyncResponseTransformer' may not be used when a "
                                                   + "'combinedResponseHandler' has been specified in a "
                                                   + "ClientExecutionParams object.");
            }

            ExecutionAttributes executionAttributes = executionParams.executionAttributes();
            executionAttributes.putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 1);

            AsyncStreamingResponseHandler<OutputT, ReturnT> asyncStreamingResponseHandler =
                new AsyncStreamingResponseHandler<>(asyncResponseTransformer);

            // For streaming requests, prepare() should be called as early as possible to avoid NPE in client
            // See https://github.com/aws/aws-sdk-java-v2/issues/1268. We do this with a wrapper that caches the prepare
            // result until the execution attempt number changes. This guarantees that prepare is only called once per
            // execution.
            TransformingAsyncResponseHandler<ReturnT> wrappedAsyncStreamingResponseHandler =
                IdempotentAsyncResponseHandler.create(
                    asyncStreamingResponseHandler,
                    () -> executionAttributes.getAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT),
                    Integer::equals);
            wrappedAsyncStreamingResponseHandler.prepare();

            // Running beforeExecution interceptors and modifyRequest interceptors.
            SdkClientConfiguration clientConfiguration = createRequestConfiguration(executionParams);
            ExecutionContext context = invokeInterceptorsAndCreateExecutionContext(executionParams, clientConfiguration);

            HttpResponseHandler<OutputT> decoratedResponseHandlers =
                decorateResponseHandlers(executionParams.getResponseHandler(), context);

            asyncStreamingResponseHandler.responseHandler(decoratedResponseHandlers);

            TransformingAsyncResponseHandler<? extends SdkException> errorHandler =
                resolveErrorResponseHandler(executionParams.getErrorResponseHandler(), context, crc32Validator);

            TransformingAsyncResponseHandler<Response<ReturnT>> combinedResponseHandler =
                new CombinedResponseAsyncHttpResponseHandler<>(wrappedAsyncStreamingResponseHandler, errorHandler);

            return doExecute(clientConfiguration, executionParams, context, combinedResponseHandler);
        });
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse> TransformingAsyncResponseHandler<Response<OutputT>>
        createCombinedResponseHandler(ClientExecutionParams<InputT, OutputT> executionParams,
                                      ExecutionContext executionContext) {
        /* Decorate and combine provided response handlers into a single decorated response handler */
        validateCombinedResponseHandler(executionParams);
        TransformingAsyncResponseHandler<Response<OutputT>> combinedResponseHandler;
        if (executionParams.getCombinedResponseHandler() == null) {
            combinedResponseHandler = createDecoratedHandler(executionParams.getResponseHandler(),
                                                             executionParams.getErrorResponseHandler(),
                                                             executionContext);
        } else {
            combinedResponseHandler = createDecoratedHandler(executionParams.getCombinedResponseHandler(),
                                                             executionContext);
        }
        return combinedResponseHandler;
    }

    /**
     * Combines and decorates separate success and failure response handlers into a single combined response handler
     * that handles both cases and produces a {@link Response} object that wraps the result. The handlers are
     * decorated with additional behavior (such as CRC32 validation).
     */
    private <OutputT extends SdkResponse> TransformingAsyncResponseHandler<Response<OutputT>> createDecoratedHandler(
        HttpResponseHandler<OutputT> successHandler,
        HttpResponseHandler<? extends SdkException> errorHandler,
        ExecutionContext executionContext) {

        HttpResponseHandler<OutputT> decoratedResponseHandlers =
            decorateResponseHandlers(successHandler, executionContext);

        TransformingAsyncResponseHandler<OutputT> decoratedSuccessHandler =
            new AsyncResponseHandler<>(decoratedResponseHandlers,
                                       crc32Validator,
                                       executionContext.executionAttributes());

        TransformingAsyncResponseHandler<? extends SdkException> decoratedErrorHandler =
            resolveErrorResponseHandler(errorHandler, executionContext, crc32Validator);
        return new CombinedResponseAsyncHttpResponseHandler<>(decoratedSuccessHandler, decoratedErrorHandler);
    }

    /**
     * Decorates a combined response handler with additional behavior (such as CRC32 validation).
     */
    private <OutputT extends SdkResponse> TransformingAsyncResponseHandler<Response<OutputT>> createDecoratedHandler(
        HttpResponseHandler<Response<OutputT>> combinedResponseHandler,
        ExecutionContext executionContext) {

        HttpResponseHandler<Response<OutputT>> decoratedResponseHandlers =
            decorateSuccessResponseHandlers(combinedResponseHandler, executionContext);

        return new AsyncResponseHandler<>(decoratedResponseHandlers,
                                          crc32Validator,
                                          executionContext.executionAttributes());
    }

    private <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> doExecute(
        SdkClientConfiguration clientConfiguration,
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext,
        TransformingAsyncResponseHandler<Response<ReturnT>> asyncResponseHandler) {

        try {

            InputT inputT = (InputT) executionContext.interceptorContext().request();

            // Running beforeMarshalling, afterMarshalling and modifyHttpRequest, modifyHttpContent,
            // modifyAsyncHttpContent interceptors
            InterceptorContext finalizeSdkHttpRequestContext = finalizeSdkHttpFullRequest(executionParams,
                                                                                          executionContext,
                                                                                          inputT,
                                                                                          clientConfiguration);

            SdkHttpFullRequest marshalled = (SdkHttpFullRequest) finalizeSdkHttpRequestContext.httpRequest();

            // Ensure that the signing configuration is still valid after the
            // request has been potentially transformed.
            try {
                validateSigningConfiguration(marshalled, executionContext.signer());
            } catch (Exception e) {
                return CompletableFutureUtils.failedFuture(e);
            }

            Optional<RequestBody> requestBody = finalizeSdkHttpRequestContext.requestBody();

            // For non-streaming requests, RequestBody can be modified in the interceptors. eg:
            // CreateMultipartUploadRequestInterceptor
            if (!finalizeSdkHttpRequestContext.asyncRequestBody().isPresent() && requestBody.isPresent()) {
                marshalled = marshalled.toBuilder()
                                       .contentStreamProvider(requestBody.get().contentStreamProvider())
                                       .build();
            }

            CompletableFuture<ReturnT> invokeFuture =
                invoke(clientConfiguration,
                       marshalled,
                       finalizeSdkHttpRequestContext.asyncRequestBody().orElse(null),
                       inputT,
                       executionContext,
                       new AsyncAfterTransmissionInterceptorCallingResponseHandler<>(asyncResponseHandler,
                                                                                     executionContext));

            CompletableFuture<ReturnT> exceptionTranslatedFuture = invokeFuture.handle((resp, err) -> {
                if (err != null) {
                    throw ThrowableUtils.failure(err);
                }
                return resp;
            });

            return CompletableFutureUtils.forwardExceptionTo(exceptionTranslatedFuture, invokeFuture);
        } catch (Throwable t) {
            runAndLogError(
                log.logger(),
                "Error thrown from TransformingAsyncResponseHandler#onError, ignoring.",
                () -> asyncResponseHandler.onError(t));
            return CompletableFutureUtils.failedFuture(ThrowableUtils.asSdkException(t));
        }
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * Error responses are never streaming so we always use {@link AsyncResponseHandler}.
     *
     * @return Async handler for error responses.
     */
    private TransformingAsyncResponseHandler<? extends SdkException> resolveErrorResponseHandler(
        HttpResponseHandler<? extends SdkException> errorHandler,
        ExecutionContext executionContext,
        Function<SdkHttpFullResponse, SdkHttpFullResponse> responseAdapter) {
        return new AsyncResponseHandler<>(errorHandler,
                                          responseAdapter,
                                          executionContext.executionAttributes());
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been
     * configured in the ExecutionContext beforehand.
     **/
    private <InputT extends SdkRequest, OutputT> CompletableFuture<OutputT> invoke(
        SdkClientConfiguration clientConfiguration,
        SdkHttpFullRequest request,
        AsyncRequestBody requestProvider,
        InputT originalRequest,
        ExecutionContext executionContext,
        TransformingAsyncResponseHandler<Response<OutputT>> responseHandler) {
        return client.requestExecutionBuilder()
                     .requestProvider(requestProvider)
                     .httpClientDependencies(d -> d.clientConfiguration(clientConfiguration))
                     .request(request)
                     .originalRequest(originalRequest)
                     .executionContext(executionContext)
                     .execute(responseHandler);
    }

    private <T> CompletableFuture<T> measureApiCallSuccess(ClientExecutionParams<?, ?> executionParams,
                                                           Supplier<CompletableFuture<T>> apiCall) {
        try {
            CompletableFuture<T> apiCallResult = apiCall.get();
            CompletableFuture<T> outputFuture =
                apiCallResult.whenComplete((r, t) -> reportApiCallSuccess(executionParams, t == null));

            // Preserve cancellations on the output future, by passing cancellations of the output future to the api call future.
            CompletableFutureUtils.forwardExceptionTo(outputFuture, apiCallResult);

            return outputFuture;
        } catch (Exception e) {
            reportApiCallSuccess(executionParams, false);
            return CompletableFutureUtils.failedFuture(e);
        }
    }

    private void reportApiCallSuccess(ClientExecutionParams<?, ?> executionParams, boolean value) {
        MetricCollector metricCollector = executionParams.getMetricCollector();
        if (metricCollector != null) {
            metricCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, value);
        }
    }
}
