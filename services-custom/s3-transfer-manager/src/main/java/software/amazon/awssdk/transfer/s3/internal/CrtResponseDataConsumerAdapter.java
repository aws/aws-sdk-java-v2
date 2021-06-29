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

package software.amazon.awssdk.transfer.s3.internal;

import com.amazonaws.s3.ResponseDataConsumer;
import com.amazonaws.s3.model.GetObjectOutput;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Adapt the SDK API {@link AsyncResponseTransformer} to the CRT API {@link ResponseDataConsumer}.
 */
@SdkInternalApi
public class CrtResponseDataConsumerAdapter<ReturnT> implements ResponseDataConsumer<GetObjectOutput> {

    private static final Logger log = Logger.loggerFor(CrtResponseDataConsumerAdapter.class);
    private final AsyncResponseTransformer<GetObjectResponse, ReturnT> transformer;
    private final CompletableFuture<ReturnT> future;
    private final S3CrtDataPublisher publisher;
    private final ResponseHeadersHandler headerHandler;

    public CrtResponseDataConsumerAdapter(AsyncResponseTransformer<GetObjectResponse, ReturnT> transformer) {
        this(transformer, new S3CrtDataPublisher(), new ResponseHeadersHandler());
    }

    @SdkInternalApi
    CrtResponseDataConsumerAdapter(AsyncResponseTransformer<GetObjectResponse, ReturnT> transformer,
                                   S3CrtDataPublisher s3CrtDataPublisher,
                                   ResponseHeadersHandler headersHandler) {
        this.transformer = transformer;
        this.future = transformer.prepare();
        this.publisher = s3CrtDataPublisher;
        this.headerHandler = headersHandler;
    }

    public CompletableFuture<ReturnT> transformerFuture() {
        return future;
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        headerHandler.onResponseHeaders(statusCode, headers);
    }

    @Override
    public void onResponse(GetObjectOutput output) {
        // Passing empty SdkHttpResponse if it's not available
        SdkHttpResponse sdkHttpResponse = headerHandler.sdkHttpResponseFuture()
                                                       .getNow(SdkHttpResponse.builder().build());

        GetObjectResponse response = S3CrtPojoConversion.fromCrtGetObjectOutput(output,
                                                                                sdkHttpResponse);
        transformer.onResponse(response);
        transformer.onStream(publisher);
    }

    @Override
    public void onResponseData(ByteBuffer byteBuffer) {
        log.trace(() -> "Received data of size " + byteBuffer.remaining());

        // Need to make a copy because the incoming byteBuffer might get released soon
        ByteBuffer newByteBuffer = ByteBuffer.wrap(BinaryUtils.copyAllBytesFrom(byteBuffer));
        publisher.deliverData(newByteBuffer);
    }

    @Override
    public void onException(CrtRuntimeException e) {
        log.debug(() -> "An error occurred ", e);
        transformer.exceptionOccurred(e);
        publisher.notifyError(e);
    }

    @Override
    public void onFinished() {
        log.debug(() -> "Finished streaming ");
        publisher.notifyStreamingFinished();
    }
}
