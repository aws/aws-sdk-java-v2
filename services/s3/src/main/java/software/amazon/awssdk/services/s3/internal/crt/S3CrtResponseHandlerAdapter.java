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

package software.amazon.awssdk.services.s3.internal.crt;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER_ALTERNATE;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Adapts {@link SdkAsyncHttpResponseHandler} to {@link S3MetaRequestResponseHandler}.
 */
@SdkInternalApi
public final class S3CrtResponseHandlerAdapter implements S3MetaRequestResponseHandler {
    private static final Logger log = Logger.loggerFor(S3CrtResponseHandlerAdapter.class);
    private static final Duration META_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final CompletableFuture<Void> resultFuture;
    private final SdkAsyncHttpResponseHandler responseHandler;

    private final SimplePublisher<ByteBuffer> responsePublisher = new SimplePublisher<>();

    private final SdkHttpResponse.Builder initialHeadersResponse = SdkHttpResponse.builder();
    private final CompletableFuture<S3MetaRequestWrapper> metaRequestFuture;

    private final PublisherListener<S3MetaRequestProgress> progressListener;
    private final Duration s3MetaRequestTimeout;

    private volatile boolean responseHandlingInitiated;

    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture,
                                       SdkAsyncHttpResponseHandler responseHandler,
                                       PublisherListener<S3MetaRequestProgress> progressListener,
                                       CompletableFuture<S3MetaRequestWrapper> metaRequestFuture) {
        this(executeFuture, responseHandler, progressListener, metaRequestFuture, META_REQUEST_TIMEOUT);
    }

    @SdkTestInternalApi
    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture,
                                       SdkAsyncHttpResponseHandler responseHandler,
                                       PublisherListener<S3MetaRequestProgress> progressListener,
                                       CompletableFuture<S3MetaRequestWrapper> metaRequestFuture,
                                       Duration s3MetaRequestTimeout) {
        this.resultFuture = executeFuture;
        this.metaRequestFuture = metaRequestFuture;

        resultFuture.whenComplete((r, t) -> {
            S3MetaRequestWrapper s3MetaRequest = s3MetaRequest();
            if (s3MetaRequest == null) {
                return;
            }

            if (t != null) {
                s3MetaRequest.cancel();
            }
            s3MetaRequest.close();
        });

        this.responseHandler = responseHandler;
        this.progressListener = progressListener == null ? new NoOpPublisherListener() : progressListener;
        this.s3MetaRequestTimeout = s3MetaRequestTimeout;
    }

    private S3MetaRequestWrapper s3MetaRequest() {
        try {
            return metaRequestFuture.get(s3MetaRequestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            failResponseHandlerAndFuture(
                new RuntimeException("Timeout waiting for metaRequest to be ready", e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failResponseHandlerAndFuture(new RuntimeException(e));
        }
        return null;
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        log.debug(() -> "Received response header with status code " + statusCode);
        // Note, we cannot call responseHandler.onHeaders() here because the response status code and headers may not represent
        // whether the request has succeeded or not (e.g. if this is for a HeadObject call that CRT calls under the hood). We
        // need to rely on onResponseBody/onFinished being called to determine this.
        populateSdkHttpResponse(initialHeadersResponse, statusCode, headers);
    }

    @Override
    public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
        // See reasoning in onResponseHeaders for why we call this here and not there.
        initiateResponseHandling(initialHeadersResponse.build());

        if (bodyBytesIn == null) {
            failResponseHandlerAndFuture(new IllegalStateException("ByteBuffer delivered is null"));
            return 0;
        }

        int bytesReceived = bodyBytesIn.remaining();
        CompletableFuture<Void> writeFuture = responsePublisher.send(bodyBytesIn);

        writeFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                failResponseHandlerAndFuture(failure);
                return;
            }

            S3MetaRequestWrapper metaRequest = s3MetaRequest();
            if (metaRequest == null) {
                return;
            }
            metaRequest.incrementReadWindow(bytesReceived);
        });

        // Returning 0 to disable flow control because we manually increase read window above
        return 0;
    }

    @Override
    public void onFinished(S3FinishedResponseContext context) {
        int crtCode = context.getErrorCode();
        log.debug(() -> "Request finished with code: " + crtCode);
        if (crtCode != CRT.AWS_CRT_SUCCESS) {
            handleError(context);
        } else {
            // onResponseBody() is not invoked for responses with no content, so we may not have invoked
            // SdkAsyncHttpResponseHandler#onHeaders yet.
            // See also  reasoning in onResponseHeaders for why we call this here and not there.
            initiateResponseHandling(initialHeadersResponse.build());
            onSuccessfulResponseComplete();
        }
    }

    private void onSuccessfulResponseComplete() {
        responsePublisher.complete().whenComplete((result, failure) -> {
            if (failure != null) {
                failResponseHandlerAndFuture(failure);
                return;
            }
            this.progressListener.subscriberOnComplete();
            resultFuture.complete(null);
        });
    }

    private void handleError(S3FinishedResponseContext context) {
        int crtCode = context.getErrorCode();
        HttpHeader[] headers = context.getErrorHeaders();
        int responseStatus = context.getResponseStatus();
        byte[] errorPayload = context.getErrorPayload();

        if (isServiceError(responseStatus) && errorPayload != null) {
            handleServiceError(responseStatus, headers, errorPayload);
        } else {
            handleIoError(context, crtCode);
        }
    }

    private void handleIoError(S3FinishedResponseContext context, int crtCode) {
        Throwable cause = context.getCause();

        SdkClientException sdkClientException =
            SdkClientException.create("Failed to send the request: " +
                                      CRT.awsErrorString(crtCode), cause);
        failResponseHandlerAndFuture(sdkClientException);
        notifyResponsePublisherErrorIfNeeded(sdkClientException);
    }

    private void notifyResponsePublisherErrorIfNeeded(Throwable error) {
        if (responseHandlingInitiated) {
            responsePublisher.error(error).handle((ignore, throwable) -> {
                if (throwable != null) {
                    log.warn(() -> "Exception thrown in responsePublisher#error, ignoring", throwable);
                    return null;
                }
                return null;
            });
        }
    }

    private void handleServiceError(int responseStatus, HttpHeader[] headers, byte[] errorPayload) {
        SdkHttpResponse.Builder errorResponse = populateSdkHttpResponse(SdkHttpResponse.builder(),
                                                                        responseStatus, headers);
        if (requestFailedMidwayOfOtherError(responseStatus)) {
            AwsServiceException s3Exception = buildS3Exception(responseStatus, errorPayload, errorResponse);

            SdkClientException sdkClientException =
                SdkClientException.create("Request failed during the transfer due to an error returned from S3");
            s3Exception.addSuppressed(sdkClientException);
            failResponseHandlerAndFuture(s3Exception);
            notifyResponsePublisherErrorIfNeeded(s3Exception);
        } else if (responseStatus == 200) {
            // handleServiceError is only called when crtCode != SUCCESS. If the response status is 200, this is the
            // S3 "200 with error in body" case (e.g. CompleteMultipartUpload returning HTTP 200 with <Error> XML).
            // We wrap the response in ErrorFlaggedSdkHttpResponse which overrides isSuccessful() to return false.
            // This causes the downstream SDK pipeline (XmlResponseParserUtils, DecorateErrorFromResponseBodyUnmarshaller)
            // to parse the body and detect the error, producing a properly modeled S3Exception.
            SdkHttpFullResponse errorFlagged = new ErrorFlaggedSdkHttpResponse(errorResponse.build());
            initiateResponseHandling(errorFlagged);
            onErrorResponseComplete(errorPayload);
        } else {
            initiateResponseHandling(errorResponse.build());
            onErrorResponseComplete(errorPayload);
        }
    }

    private static AwsServiceException buildS3Exception(int responseStatus,
                                                        byte[] errorPayload,
                                                        SdkHttpResponse.Builder errorResponse) {
        String requestId = errorResponse.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER_ALTERNATE)
                                        .orElse(null);
        String extendedRequestId = errorResponse.firstMatchingHeader(X_AMZ_ID_2_HEADER)
                                                .orElse(null);
        return S3Exception.builder()
                          .requestId(requestId)
                          .extendedRequestId(extendedRequestId)
                          .statusCode(responseStatus)
                          .message(errorResponse.statusText())
                          .awsErrorDetails(AwsErrorDetails.builder()
                                                          .sdkHttpResponse(errorResponse.build())
                                                          .rawResponse(SdkBytes.fromByteArray(errorPayload))
                                                          .build())
                          .build();
    }

    /**
     * Whether request failed midway or it failed due to a different error than the initial response.
     * For example, this could happen if an object got deleted after download was initiated (200
     * was received).
     */
    private boolean requestFailedMidwayOfOtherError(int responseStatus) {
        return responseHandlingInitiated && initialHeadersResponse.statusCode() != responseStatus;
    }

    private void initiateResponseHandling(SdkHttpResponse response) {
        if (!responseHandlingInitiated) {
            responseHandlingInitiated = true;
            responseHandler.onHeaders(response);
            responseHandler.onStream(responsePublisher);
        }
    }

    private void onErrorResponseComplete(byte[] errorPayload) {
        responsePublisher.send(ByteBuffer.wrap(errorPayload))
                         .thenRun(responsePublisher::complete)
                         .handle((ignore, throwable) -> {
                             if (throwable != null) {
                                 failResponseHandlerAndFuture(throwable);
                                 return null;
                             }
                             resultFuture.complete(null);
                             return null;
                         });
    }

    private void failResponseHandlerAndFuture(Throwable exception) {
        runAndLogError(log.logger(), "Exception thrown in SdkAsyncHttpResponseHandler#onError, ignoring",
                       () -> responseHandler.onError(exception));
        resultFuture.completeExceptionally(exception);
    }

    private static boolean isServiceError(int responseStatus) {
        return responseStatus != 0;
    }

    @Override
    public void onProgress(S3MetaRequestProgress progress) {
        this.progressListener.subscriberOnNext(progress);
    }

    private static SdkHttpResponse.Builder populateSdkHttpResponse(SdkHttpResponse.Builder respBuilder,
                                                                   int statusCode, HttpHeader[] headers) {
        if (headers != null) {
            for (HttpHeader h : headers) {
                respBuilder.appendHeader(h.getName(), h.getValue());
            }
        }
        respBuilder.statusCode(statusCode);
        return respBuilder;
    }

    private static class NoOpPublisherListener implements PublisherListener<S3MetaRequestProgress> {
    }

    /**
     * An {@link SdkHttpFullResponse} wrapper that overrides {@link #isSuccessful()} to always return {@code false}.
     * This forces the SDK response pipeline ({@code XmlResponseParserUtils}, {@code DecorateErrorFromResponseBodyUnmarshaller})
     * to parse the response body even for HTTP 200 responses, enabling detection of S3's "200 with error in body" pattern.
     *
     * <p>The {@link #toBuilder()} method returns a builder whose {@code build()} also produces an
     * {@code ErrorFlaggedSdkHttpResponse}, ensuring the override survives the rebuild cycle in
     * {@code AsyncResponseHandler}.</p>
     */
    static final class ErrorFlaggedSdkHttpResponse implements SdkHttpFullResponse {
        private final SdkHttpFullResponse delegate;

        ErrorFlaggedSdkHttpResponse(SdkHttpResponse delegate) {
            // Ensure we have a full response to delegate to
            if (delegate instanceof SdkHttpFullResponse) {
                this.delegate = (SdkHttpFullResponse) delegate;
            } else {
                this.delegate = SdkHttpFullResponse.builder()
                                                   .statusCode(delegate.statusCode())
                                                   .headers(delegate.headers())
                                                   .build();
            }
        }

        private ErrorFlaggedSdkHttpResponse(SdkHttpFullResponse delegate, @SuppressWarnings("unused") boolean internal) {
            this.delegate = delegate;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }

        @Override
        public int statusCode() {
            return delegate.statusCode();
        }

        @Override
        public Optional<String> statusText() {
            return delegate.statusText();
        }

        @Override
        public Map<String, List<String>> headers() {
            return delegate.headers();
        }

        @Override
        public Optional<AbortableInputStream> content() {
            return delegate.content();
        }

        @Override
        public Builder toBuilder() {
            return new ErrorFlaggedBuilder(delegate.toBuilder());
        }
    }

    /**
     * A builder that wraps a standard {@link SdkHttpFullResponse.Builder} but produces an
     * {@link ErrorFlaggedSdkHttpResponse} on {@code build()}, preserving the {@code isSuccessful()=false} override.
     */
    private static final class ErrorFlaggedBuilder implements SdkHttpFullResponse.Builder {
        private final SdkHttpFullResponse.Builder delegate;

        ErrorFlaggedBuilder(SdkHttpFullResponse.Builder delegate) {
            this.delegate = delegate;
        }

        @Override
        public SdkHttpFullResponse build() {
            return new ErrorFlaggedSdkHttpResponse(delegate.build(), true);
        }

        @Override
        public String statusText() {
            return delegate.statusText();
        }

        @Override
        public SdkHttpFullResponse.Builder statusText(String statusText) {
            delegate.statusText(statusText);
            return this;
        }

        @Override
        public int statusCode() {
            return delegate.statusCode();
        }

        @Override
        public SdkHttpFullResponse.Builder statusCode(int statusCode) {
            delegate.statusCode(statusCode);
            return this;
        }

        @Override
        public Map<String, List<String>> headers() {
            return delegate.headers();
        }

        @Override
        public SdkHttpFullResponse.Builder putHeader(String headerName, List<String> headerValues) {
            delegate.putHeader(headerName, headerValues);
            return this;
        }

        @Override
        public SdkHttpFullResponse.Builder appendHeader(String headerName, String headerValue) {
            delegate.appendHeader(headerName, headerValue);
            return this;
        }

        @Override
        public SdkHttpFullResponse.Builder headers(Map<String, List<String>> headers) {
            delegate.headers(headers);
            return this;
        }

        @Override
        public SdkHttpFullResponse.Builder removeHeader(String headerName) {
            delegate.removeHeader(headerName);
            return this;
        }

        @Override
        public SdkHttpFullResponse.Builder clearHeaders() {
            delegate.clearHeaders();
            return this;
        }

        @Override
        public AbortableInputStream content() {
            return delegate.content();
        }

        @Override
        public SdkHttpFullResponse.Builder content(AbortableInputStream content) {
            delegate.content(content);
            return this;
        }
    }
}
