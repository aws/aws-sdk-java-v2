/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of {@link AsyncRequestBody} for providing data from memory. This is created using static
 * methods on {@link AsyncRequestBody}
 *
 * @see AsyncRequestBody#fromBytes(byte[])
 * @see AsyncRequestBody#fromByteBuffer(ByteBuffer)
 * @see AsyncRequestBody#fromString(String)
 */
@SdkInternalApi
final class ByteArrayAsyncRequestBody implements AsyncRequestBody {

    private final byte[] bytes;

    ByteArrayAsyncRequestBody(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    @Override
    public long contentLength() {
        return bytes.length;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(
                new Subscription() {
                    @Override
                    public void request(long n) {
                        if (n > 0) {
                            subscriber.onNext(ByteBuffer.wrap(bytes));
                            subscriber.onComplete();
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                });
    }
}
