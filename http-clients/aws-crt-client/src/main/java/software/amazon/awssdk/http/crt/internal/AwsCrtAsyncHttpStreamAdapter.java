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

package software.amazon.awssdk.http.crt.internal;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Implements the CrtHttpStreamHandler API and converts CRT callbacks into calls to SDK AsyncExecuteRequest methods
 */
@SdkInternalApi
public final class AwsCrtAsyncHttpStreamAdapter implements HttpStreamResponseHandler, HttpRequestBodyStream {
    private static final Logger log = Logger.loggerFor(AwsCrtAsyncHttpStreamAdapter.class);

    private final HttpClientConnection connection;
    private final CompletableFuture<Void> responseComplete;
    private final AsyncExecuteRequest sdkRequest;
    private final SdkHttpResponse.Builder respBuilder = SdkHttpResponse.builder();
    private final int windowSize;
    private final AwsCrtRequestBodySubscriber requestBodySubscriber;
    private AwsCrtResponseBodyPublisher respBodyPublisher = null;

    public AwsCrtAsyncHttpStreamAdapter(HttpClientConnection connection, CompletableFuture<Void> responseComplete,
                                        AsyncExecuteRequest sdkRequest, int windowSize) {
        this.connection = Validate.notNull(connection, "HttpConnection is null");
        this.responseComplete = Validate.notNull(responseComplete, "reqComplete Future is null");
        this.sdkRequest = Validate.notNull(sdkRequest, "AsyncExecuteRequest Future is null");
        this.windowSize = Validate.isPositive(windowSize, "windowSize is <= 0");
        this.requestBodySubscriber = new AwsCrtRequestBodySubscriber(windowSize);

        sdkRequest.requestContentPublisher().subscribe(requestBodySubscriber);
    }

    private void initRespBodyPublisherIfNeeded(HttpStream stream) {
        if (respBodyPublisher == null) {
            respBodyPublisher = new AwsCrtResponseBodyPublisher(connection, stream, responseComplete, windowSize);
        }
    }

    @Override
    public void onResponseHeaders(HttpStream stream, int responseStatusCode, int blockType, HttpHeader[] nextHeaders) {
        initRespBodyPublisherIfNeeded(stream);

        for (HttpHeader h : nextHeaders) {
            respBuilder.appendHeader(h.getName(), h.getValue());
        }
    }

    @Override
    public void onResponseHeadersDone(HttpStream stream, int headerType) {
        if (headerType == HttpHeaderBlock.MAIN.getValue()) {
            initRespBodyPublisherIfNeeded(stream);

            respBuilder.statusCode(stream.getResponseStatusCode());
            sdkRequest.responseHandler().onHeaders(respBuilder.build());
            sdkRequest.responseHandler().onStream(respBodyPublisher);
        }
    }

    @Override
    public int onResponseBody(HttpStream stream, byte[] bodyBytesIn) {
        initRespBodyPublisherIfNeeded(stream);

        respBodyPublisher.queueBuffer(bodyBytesIn);
        respBodyPublisher.publishToSubscribers();

        /*
         * Intentionally zero. We manually manage the crt stream's window within the body publisher by updating with
         * the exact amount we were able to push to the subcriber.
         *
         * See the call to stream.incrementWindow() in AwsCrtResponseBodyPublisher.
         */
        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        initRespBodyPublisherIfNeeded(stream);

        if (HttpStatusFamily.of(respBuilder.statusCode()) == HttpStatusFamily.SERVER_ERROR) {
            connection.shutdown();
        }

        if (errorCode == CRT.AWS_CRT_SUCCESS) {
            log.debug(() -> "Response Completed Successfully");
            respBodyPublisher.setQueueComplete();
            respBodyPublisher.publishToSubscribers();
        } else {
            HttpException error = new HttpException(errorCode);
            log.error(() -> "Response Encountered an Error.", error);

            // Invoke Error Callback on SdkAsyncHttpResponseHandler
            try {
                sdkRequest.responseHandler().onError(error);
            } catch (Exception e) {
                log.error(() -> String.format("SdkAsyncHttpResponseHandler %s threw an exception in onError: %s",
                        sdkRequest.responseHandler(), e));
            }

            // Invoke Error Callback on any Subscriber's of the Response Body
            respBodyPublisher.setError(error);
            respBodyPublisher.publishToSubscribers();
        }
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        return requestBodySubscriber.transferRequestBody(bodyBytesOut);
    }
}
