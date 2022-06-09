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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Adapts {@link SdkAsyncHttpResponseHandler} to {@link S3MetaRequestResponseHandler}.
 */
@SdkInternalApi
public class S3CrtResponseHandlerAdapter implements S3MetaRequestResponseHandler {
    private final CompletableFuture<Void> resultFuture;
    private final SdkAsyncHttpResponseHandler responseHandler;
    private final S3CrtDataPublisher publisher;
    private final SdkHttpResponse.Builder respBuilder = SdkHttpResponse.builder();

    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture, SdkAsyncHttpResponseHandler responseHandler) {
        this(executeFuture, responseHandler, new S3CrtDataPublisher());
    }

    @SdkTestInternalApi
    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture,
                                       SdkAsyncHttpResponseHandler responseHandler,
                                       S3CrtDataPublisher crtDataPublisher) {
        this.resultFuture = executeFuture;
        this.responseHandler = responseHandler;
        this.publisher = crtDataPublisher;
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        for (HttpHeader h : headers) {
            respBuilder.appendHeader(h.getName(), h.getValue());
        }

        respBuilder.statusCode(statusCode);
        responseHandler.onHeaders(respBuilder.build());
        responseHandler.onStream(publisher);
    }

    @Override
    public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
        publisher.deliverData(bodyBytesIn);
        return 0;
    }

    @Override
    public void onFinished(int crtCode, int responseStatus, byte[] errorPayload) {
        if (crtCode != CRT.AWS_CRT_SUCCESS) {
            handleError(crtCode, responseStatus, errorPayload);
        } else {
            resultFuture.complete(null);
            publisher.notifyStreamingFinished();
        }
    }

    public void cancelRequest() {
        SdkCancellationException sdkClientException =
            new SdkCancellationException("request is cancelled");
        notifyError(sdkClientException);
    }

    private void handleError(int crtCode, int responseStatus, byte[] errorPayload) {
        if (isErrorResponse(responseStatus) && errorPayload != null) {
            publisher.deliverData(ByteBuffer.wrap(errorPayload));
            publisher.notifyStreamingFinished();
            resultFuture.complete(null);
        } else {
            SdkClientException sdkClientException =
                SdkClientException.create("Failed to send the request: " +
                                          CRT.awsErrorString(crtCode));
            resultFuture.completeExceptionally(sdkClientException);

            responseHandler.onError(sdkClientException);
            publisher.notifyError(sdkClientException);
        }
    }

    private void notifyError(Exception exception) {
        resultFuture.completeExceptionally(exception);
        responseHandler.onError(exception);
        publisher.notifyError(exception);
    }

    private static boolean isErrorResponse(int responseStatus) {
        return responseStatus != 0;
    }
}
