/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Implementation of {@link AsyncResponseHandler} that dumps content into a string using the specified {@link Charset}.
 *
 * @param <ResponseT> Pojo response type.
 */
@SdkInternalApi
class StringAsyncResponseHandler<ResponseT> implements AsyncResponseHandler<ResponseT, String> {

    private final AsyncResponseHandler<ResponseT, byte[]> byteArrayResponseHandler;
    private final Charset charset;

    /**
     * @param byteArrayResponseHandler {@link AsyncResponseHandler} implementation that dumps data into a byte array.
     * @param charset                  Charset to use for String.
     */
    StringAsyncResponseHandler(AsyncResponseHandler<ResponseT, byte[]> byteArrayResponseHandler,
                               Charset charset) {
        this.byteArrayResponseHandler = byteArrayResponseHandler;
        this.charset = charset;
    }

    @Override
    public void responseReceived(ResponseT response) {
        byteArrayResponseHandler.responseReceived(response);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        byteArrayResponseHandler.onStream(publisher);
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        byteArrayResponseHandler.exceptionOccurred(throwable);
    }

    @Override
    public String complete() {
        return new String(byteArrayResponseHandler.complete(), charset);
    }
}
