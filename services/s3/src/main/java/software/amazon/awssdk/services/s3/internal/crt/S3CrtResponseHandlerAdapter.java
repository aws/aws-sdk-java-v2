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
    public static final int CRT_SUCCESSFUL_CANCEL = 14374;
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
        initiateResponseHandling(initialHeadersResponse.build());

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
        } else if (crtCode == CRT_SUCCESSFUL_CANCEL) {
            handleSuccessfulCancel(context, crtCode);
        } else {
            handleIoError(context, crtCode);
        }
    }

    private void handleSuccessfulCancel(S3FinishedResponseContext context, int crtCode) {
        if (!responseHandlingInitiated) {
            responseHandlingInitiated = true;
            responseHandler.onHeaders(initialHeadersResponse.build());
        }

        Throwable cause = context.getCause();

        // TODO: Potentially subclass this exception
        SdkClientException sdkClientException =
            SdkClientException.create("Failed to send the request: " +
                                      CRT.awsErrorString(crtCode), cause);
        failResponseHandlerAndFuture(sdkClientException);
        notifyResponsePublisherErrorIfNeeded(sdkClientException);
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
}
