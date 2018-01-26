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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Implementation of {@link AsyncResponseHandler} that dumps content into a byte array.
 *
 * @param <ResponseT> Pojo response type.
 */
@SdkInternalApi
class ByteArrayAsyncResponseHandler<ResponseT> implements AsyncResponseHandler<ResponseT, byte[]> {

    private ByteArrayOutputStream baos;

    @Override
    public void responseReceived(ResponseT response) {
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        baos = new ByteArrayOutputStream();
        publisher.subscribe(new BaosSubscriber());
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        baos = null;
    }

    @Override
    public byte[] complete() {
        try {
            return baos.toByteArray();
        } finally {
            baos = null;
        }
    }

    /**
     * Requests chunks sequentially and dumps them into a {@link ByteArrayOutputStream}.
     */
    private class BaosSubscriber implements Subscriber<ByteBuffer> {

        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            subscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            invokeSafely(() -> baos.write(BinaryUtils.copyBytesFrom(byteBuffer)));
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            // Handled by response handler
        }

        @Override
        public void onComplete() {
            // Handled by response handler
        }
    }
}
