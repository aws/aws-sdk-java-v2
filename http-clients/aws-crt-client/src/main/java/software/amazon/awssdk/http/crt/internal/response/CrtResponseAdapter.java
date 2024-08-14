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

package software.amazon.awssdk.http.crt.internal.response;

import static software.amazon.awssdk.http.crt.internal.CrtUtils.wrapWithIoExceptionIfRetryable;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Response handler adaptor for {@link AwsCrtAsyncHttpClient}.
 * <p>
 * Implements the CrtHttpStreamHandler API and converts CRT callbacks into calls to SDK AsyncExecuteRequest methods
 */
@SdkInternalApi
public final class CrtResponseAdapter implements HttpStreamResponseHandler {
    private static final Logger log = Logger.loggerFor(CrtResponseAdapter.class);

    private final CompletableFuture<Void> completionFuture;
    private final SdkAsyncHttpResponseHandler responseHandler;
    private final SimplePublisher<ByteBuffer> responsePublisher;

    private final SdkHttpResponse.Builder responseBuilder;
    private final ResponseHandlerHelper responseHandlerHelper;

    private CrtResponseAdapter(HttpClientConnection connection,
                               CompletableFuture<Void> completionFuture,
                               SdkAsyncHttpResponseHandler responseHandler) {
        this(connection, completionFuture, responseHandler, new SimplePublisher<>());
    }


    @SdkTestInternalApi
    public CrtResponseAdapter(HttpClientConnection connection,
                               CompletableFuture<Void> completionFuture,
                               SdkAsyncHttpResponseHandler responseHandler,
                               SimplePublisher<ByteBuffer> simplePublisher) {
        Validate.paramNotNull(connection, "connection");
        this.completionFuture = Validate.paramNotNull(completionFuture, "completionFuture");
        this.responseHandler = Validate.paramNotNull(responseHandler, "responseHandler");
        this.responseBuilder = SdkHttpResponse.builder();
        this.responseHandlerHelper = new ResponseHandlerHelper(responseBuilder, connection);
        this.responsePublisher = simplePublisher;
    }

    public static HttpStreamResponseHandler toCrtResponseHandler(HttpClientConnection crtConn,
                                                                 CompletableFuture<Void> requestFuture,
                                                                 SdkAsyncHttpResponseHandler responseHandler) {
        return new CrtResponseAdapter(crtConn, requestFuture, responseHandler);
    }

    @Override
    public void onResponseHeaders(HttpStream stream, int responseStatusCode, int blockType, HttpHeader[] nextHeaders) {
        responseHandlerHelper.onResponseHeaders(responseStatusCode, blockType, nextHeaders);
    }

    @Override
    public void onResponseHeadersDone(HttpStream stream, int headerType) {
        if (headerType == HttpHeaderBlock.MAIN.getValue()) {
            responseHandler.onHeaders(responseBuilder.build());
            responseHandler.onStream(responsePublisher);
        }
    }

    @Override
    public int onResponseBody(HttpStream stream, byte[] bodyBytesIn) {
        CompletableFuture<Void> writeFuture = responsePublisher.send(ByteBuffer.wrap(bodyBytesIn));

        if (writeFuture.isDone() && !writeFuture.isCompletedExceptionally()) {
            // Optimization: If write succeeded immediately, return non-zero to avoid the extra call back into the CRT.
            return bodyBytesIn.length;
        }

        writeFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                handlePublisherError(stream, failure);
                return;
            }

            responseHandlerHelper.incrementWindow(stream, bodyBytesIn.length);
        });

        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        if (errorCode == CRT.AWS_CRT_SUCCESS) {
            onSuccessfulResponseComplete(stream);
        } else {
            onFailedResponseComplete(stream, new HttpException(errorCode));
        }
    }

    private void onSuccessfulResponseComplete(HttpStream stream) {
        responsePublisher.complete().whenComplete((result, failure) -> {
            if (failure != null) {
                handlePublisherError(stream, failure);
                return;
            }
            completionFuture.complete(null);
        });

        responseHandlerHelper.cleanUpConnectionBasedOnStatusCode(stream);
    }

    private void handlePublisherError(HttpStream stream, Throwable failure) {
        failResponseHandlerAndFuture(failure);
        responseHandlerHelper.closeConnection(stream);
    }

    private void onFailedResponseComplete(HttpStream stream, HttpException error) {
        log.debug(() -> "HTTP response encountered an error.", error);

        Throwable toThrow = wrapWithIoExceptionIfRetryable(error);;
        responsePublisher.error(toThrow);
        failResponseHandlerAndFuture(toThrow);
        responseHandlerHelper.closeConnection(stream);
    }

    private void failResponseHandlerAndFuture(Throwable error) {
        callResponseHandlerOnError(error);
        completionFuture.completeExceptionally(error);
    }

    private void callResponseHandlerOnError(Throwable error) {
        try {
            responseHandler.onError(error);
        } catch (RuntimeException e) {
            log.warn(() -> "Exception raised from SdkAsyncHttpResponseHandler#onError.", e);
        }
    }
}
