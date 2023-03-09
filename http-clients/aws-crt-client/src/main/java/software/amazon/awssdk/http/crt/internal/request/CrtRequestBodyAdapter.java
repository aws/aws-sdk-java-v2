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

package software.amazon.awssdk.http.crt.internal.request;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult;

@SdkInternalApi
final class CrtRequestBodyAdapter implements HttpRequestBodyStream {
    private final SdkHttpContentPublisher requestPublisher;
    private final ByteBufferStoringSubscriber requestBodySubscriber;

    CrtRequestBodyAdapter(SdkHttpContentPublisher requestPublisher, long readLimit) {
        this.requestPublisher = requestPublisher;
        this.requestBodySubscriber = new ByteBufferStoringSubscriber(readLimit);
        requestPublisher.subscribe(requestBodySubscriber);
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        return requestBodySubscriber.transferTo(bodyBytesOut) == TransferResult.END_OF_STREAM;
    }

    @Override
    public long getLength() {
        return requestPublisher.contentLength().orElse(0L);
    }
}
