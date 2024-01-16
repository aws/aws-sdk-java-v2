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
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Response handler adaptor for {@link AwsCrtHttpClient}.
 */
@SdkInternalApi
public final class InputStreamAdaptingHttpStreamResponseHandler implements HttpStreamResponseHandler {

    private volatile InputStreamSubscriber inputStreamSubscriber;
    private final SimplePublisher<ByteBuffer> simplePublisher = new SimplePublisher<>();

    private final CompletableFuture<SdkHttpFullResponse> requestCompletionFuture;
    private final HttpClientConnection crtConn;

    private final SdkHttpFullResponse.Builder responseBuilder;
    private final ResponseHandlerHelper responseHandlerHelper;

    public InputStreamAdaptingHttpStreamResponseHandler(HttpClientConnection crtConn,
                                                        CompletableFuture<SdkHttpFullResponse> requestCompletionFuture) {
        this.crtConn = crtConn;
        this.requestCompletionFuture = requestCompletionFuture;
        this.responseBuilder = SdkHttpResponse.builder();
        this.responseHandlerHelper = new ResponseHandlerHelper(responseBuilder, crtConn);
    }

    @Override
    public void onResponseHeaders(HttpStream stream, int responseStatusCode, int blockType,
                                  HttpHeader[] nextHeaders) {
        if (blockType == HttpHeaderBlock.MAIN.getValue()) {
            for (HttpHeader h : nextHeaders) {
                responseBuilder.appendHeader(h.getName(), h.getValue());
            }
            responseBuilder.statusCode(responseStatusCode);
        }
    }

    @Override
    public int onResponseBody(HttpStream stream, byte[] bodyBytesIn) {
        if (inputStreamSubscriber == null) {
            inputStreamSubscriber = new InputStreamSubscriber();
            simplePublisher.subscribe(inputStreamSubscriber);
            // For response with a payload, we need to complete the future here to allow downstream to retrieve the data from
            // the stream directly.
            responseBuilder.content(AbortableInputStream.create(inputStreamSubscriber));
            requestCompletionFuture.complete(responseBuilder.build());
        }

        CompletableFuture<Void> writeFuture = simplePublisher.send(ByteBuffer.wrap(bodyBytesIn));

        if (writeFuture.isDone() && !writeFuture.isCompletedExceptionally()) {
            // Optimization: If write succeeded immediately, return non-zero to avoid the extra call back into the CRT.
            return bodyBytesIn.length;
        }

        writeFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                failFutureAndReleaseConnection(stream, failure);
                return;
            }

            if (!responseHandlerHelper.connectionClosed().get()) {
                // increment the window upon buffer consumption.
                stream.incrementWindow(bodyBytesIn.length);
            }
        });

        // Window will be incremented after the subscriber consumes the data, returning 0 here to disable it.
        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        if (errorCode == CRT.AWS_CRT_SUCCESS) {
            onSuccessfulResponseComplete(stream);
        } else {
            onFailedResponseComplete(stream, errorCode);
        }
    }

    private void failFutureAndReleaseConnection(HttpStream stream, Throwable failure) {
        requestCompletionFuture.completeExceptionally(failure);
        responseHandlerHelper.releaseConnection(stream);
    }

    private void failFutureAndCloseConnection(HttpStream stream, Throwable failure) {
        requestCompletionFuture.completeExceptionally(failure);
        responseHandlerHelper.closeConnection(stream);
    }

    private void onFailedResponseComplete(HttpStream stream, int errorCode) {
        Throwable toThrow =
            wrapWithIoExceptionIfRetryable(new HttpException(errorCode));

        simplePublisher.error(toThrow);
        failFutureAndCloseConnection(stream, toThrow);
    }

    private void onSuccessfulResponseComplete(HttpStream stream) {
        // For response without a payload, for example, S3 PutObjectResponse, we need to complete the future
        // in onResponseComplete callback since onResponseBody will never be invoked.
        requestCompletionFuture.complete(responseBuilder.build());

        // requestCompletionFuture has been completed at this point, no need to notify the future
        simplePublisher.complete();
        responseHandlerHelper.cleanUpConnectionBasedOnStatusCode(stream);
    }
}
