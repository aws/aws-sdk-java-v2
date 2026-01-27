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
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.crt.http.HttpStreamBaseResponseHandler;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AbortableInputStreamSubscriber;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Response handler adaptor for {@link AwsCrtHttpClient}.
 */
@SdkInternalApi
public final class InputStreamAdaptingHttpStreamResponseHandler implements HttpStreamBaseResponseHandler {
    private static final Logger log = Logger.loggerFor(InputStreamAdaptingHttpStreamResponseHandler.class);
    private volatile AbortableInputStreamSubscriber inputStreamSubscriber;
    private final SimplePublisher<ByteBuffer> simplePublisher;
    private final CompletableFuture<SdkHttpFullResponse> requestCompletionFuture;
    private final SdkHttpFullResponse.Builder responseBuilder;
    private final ResponseHandlerHelper responseHandlerHelper;

    public InputStreamAdaptingHttpStreamResponseHandler(CompletableFuture<SdkHttpFullResponse> requestCompletionFuture) {
        this(requestCompletionFuture, new SimplePublisher<>());
    }

    @SdkTestInternalApi
    public InputStreamAdaptingHttpStreamResponseHandler(CompletableFuture<SdkHttpFullResponse> requestCompletionFuture,
                                                        SimplePublisher<ByteBuffer> simplePublisher) {
        this.requestCompletionFuture = requestCompletionFuture;
        this.responseBuilder = SdkHttpResponse.builder();
        this.simplePublisher = simplePublisher;
        this.responseHandlerHelper = new ResponseHandlerHelper(responseBuilder);
    }

    @Override
    public void onResponseHeaders(HttpStreamBase stream, int responseStatusCode, int blockType,
                                  HttpHeader[] nextHeaders) {
        responseHandlerHelper.onResponseHeaders(stream, responseStatusCode, blockType, nextHeaders);

        // Propagate cancellation
        requestCompletionFuture.exceptionally(t -> {
            responseHandlerHelper.closeStream();
            return null;
        });
    }

    @Override
    public int onResponseBody(HttpStreamBase stream, byte[] bodyBytesIn) {
        if (inputStreamSubscriber == null) {
            inputStreamSubscriber =
                AbortableInputStreamSubscriber.builder()
                                              .doAfterClose(() -> responseHandlerHelper.closeStream())
                                              .build();
            simplePublisher.subscribe(inputStreamSubscriber);
            responseBuilder.content(AbortableInputStream.create(inputStreamSubscriber));
            requestCompletionFuture.complete(responseBuilder.build());
        }

        CompletableFuture<Void> writeFuture = simplePublisher.send(ByteBuffer.wrap(bodyBytesIn));

        if (writeFuture.isDone() && !writeFuture.isCompletedExceptionally()) {
            return bodyBytesIn.length;
        }

        writeFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                log.debug(() -> "The subscriber failed to receive the data, closing the connection and failing the future",
                          failure);
                requestCompletionFuture.completeExceptionally(failure);
                responseHandlerHelper.closeStream();
                return;
            }
            responseHandlerHelper.incrementWindow(bodyBytesIn.length);
        });

        return 0;
    }

    @Override
    public void onResponseComplete(HttpStreamBase stream, int errorCode) {
        if (errorCode == CRT.AWS_CRT_SUCCESS) {
            onSuccessfulResponseComplete();
        } else {
            onFailedResponseComplete(errorCode);
        }
    }

    private void onFailedResponseComplete(int errorCode) {
        Throwable toThrow = wrapWithIoExceptionIfRetryable(new HttpException(errorCode));
        simplePublisher.error(toThrow);
        requestCompletionFuture.completeExceptionally(toThrow);
        responseHandlerHelper.closeStream();
    }

    private void onSuccessfulResponseComplete() {
        requestCompletionFuture.complete(responseBuilder.build());
        simplePublisher.complete();
        responseHandlerHelper.closeStream();
    }
}
