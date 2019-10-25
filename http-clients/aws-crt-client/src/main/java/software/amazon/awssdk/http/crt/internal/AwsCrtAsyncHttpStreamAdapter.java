/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.crt.utils.ByteBufferUtils.deepCopy;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.CrtHttpStreamHandler;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Implements the CrtHttpStreamHandler API and converts CRT callbacks into calls to SDK AsyncExecuteRequest methods
 */
@SdkInternalApi
public class AwsCrtAsyncHttpStreamAdapter implements CrtHttpStreamHandler {
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
        Validate.notNull(connection, "HttpConnection is null");
        Validate.notNull(responseComplete, "reqComplete Future is null");
        Validate.notNull(sdkRequest, "AsyncExecuteRequest Future is null");
        Validate.isPositive(windowSize, "windowSize is <= 0");

        this.connection = connection;
        this.responseComplete = responseComplete;
        this.sdkRequest = sdkRequest;
        this.windowSize = windowSize;
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

        respBuilder.statusCode(responseStatusCode);

        for (HttpHeader h : nextHeaders) {
            respBuilder.appendHeader(h.getName(), h.getValue());
        }
    }

    @Override
    public void onResponseHeadersDone(HttpStream stream, int headerType) {
        initRespBodyPublisherIfNeeded(stream);

        respBuilder.statusCode(stream.getResponseStatusCode());
        sdkRequest.responseHandler().onHeaders(respBuilder.build());
        sdkRequest.responseHandler().onStream(respBodyPublisher);
    }

    @Override
    public int onResponseBody(HttpStream stream, ByteBuffer bodyBytesIn) {
        initRespBodyPublisherIfNeeded(stream);

        if (respBodyPublisher == null) {
            log.error(() -> "Publisher is null, onResponseHeadersDone() was never called");
            throw new IllegalStateException("Publisher is null, onResponseHeadersDone() was never called");
        }

        // Queue a Deep Copy since bodyBytesIn is only guaranteed to contain valid memory for the lifetime of this
        // function call, and it's memory can be reused once this function returns.
        respBodyPublisher.queueBuffer(deepCopy(bodyBytesIn));
        respBodyPublisher.publishToSubscribers();

        if (bodyBytesIn.remaining() != 0) {
            throw new IllegalStateException("Unprocessed bytes remain in bodyBytesIn Buffer!");
        }

        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        initRespBodyPublisherIfNeeded(stream);

        if (errorCode == CRT.AWS_CRT_SUCCESS) {
            log.debug(() -> "Response Completed Successfully");
            respBodyPublisher.setQueueComplete();
            respBodyPublisher.publishToSubscribers();
        } else {
            HttpException error = new HttpException(errorCode);
            log.error(() -> "Response Encountered an Error.", error);

            // Invoke Error Callback on SdkAsyncHttpResponseHandler
            sdkRequest.responseHandler().onError(error);

            // Invoke Error Callback on any Subscriber's of the Response Body
            respBodyPublisher.setError(error);
            respBodyPublisher.publishToSubscribers();
        }
    }

    @Override
    public boolean sendRequestBody(HttpStream stream, ByteBuffer bodyBytesOut) {
        return requestBodySubscriber.transferRequestBody(bodyBytesOut);
    }
}
