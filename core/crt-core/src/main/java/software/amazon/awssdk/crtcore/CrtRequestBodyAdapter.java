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

package software.amazon.awssdk.crtcore;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult;

/**
 * This class adapts a {@link Publisher} of {@link ByteBuffer} to the CRT {@link HttpRequestBodyStream}.
 */
@SdkProtectedApi
public final class CrtRequestBodyAdapter implements HttpRequestBodyStream {
    private static final int BUFFER_SIZE = 4 * 1024 * 1024; // 4 MB
    private final Publisher<ByteBuffer> requestPublisher;
    private final long contentLength;
    private ByteBufferStoringSubscriber requestBodySubscriber;

    public CrtRequestBodyAdapter(Publisher<ByteBuffer> requestPublisher, long contentLength) {
        this.requestPublisher = requestPublisher;
        this.contentLength = contentLength;
        this.requestBodySubscriber = new ByteBufferStoringSubscriber(BUFFER_SIZE);
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        return requestBodySubscriber.transferTo(bodyBytesOut) == TransferResult.END_OF_STREAM;
    }

    @Override
    public boolean resetPosition() {
        requestBodySubscriber = new ByteBufferStoringSubscriber(BUFFER_SIZE);
        requestPublisher.subscribe(requestBodySubscriber);
        return true;
    }

    @Override
    public long getLength() {
        return contentLength;
    }
}
