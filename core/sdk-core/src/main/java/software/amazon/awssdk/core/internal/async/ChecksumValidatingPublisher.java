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

package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Publisher to update the checksum as it reads the data and
 * finally compares the computed checksum with expected Checksum.
 */
@SdkInternalApi
public final class ChecksumValidatingPublisher implements SdkPublisher<ByteBuffer> {

    private final Publisher<ByteBuffer> publisher;
    private final SdkChecksum sdkChecksum;
    private final String expectedChecksum;

    public ChecksumValidatingPublisher(Publisher<ByteBuffer> publisher,
                                       SdkChecksum sdkChecksum,
                                       String expectedChecksum) {
        this.publisher = publisher;
        this.sdkChecksum = sdkChecksum;
        this.expectedChecksum = expectedChecksum;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        publisher.subscribe(new ChecksumValidatingSubscriber(s, sdkChecksum, expectedChecksum));
    }

    private static class ChecksumValidatingSubscriber implements Subscriber<ByteBuffer> {

        private final Subscriber<? super ByteBuffer> wrapped;
        private final SdkChecksum sdkChecksum;
        private final String expectedChecksum;
        private String calculatedChecksum = null;

        ChecksumValidatingSubscriber(Subscriber<? super ByteBuffer> wrapped,
                                     SdkChecksum sdkChecksum,
                                     String expectedChecksum) {
            this.wrapped = wrapped;
            this.sdkChecksum = sdkChecksum;
            this.expectedChecksum = expectedChecksum;
        }

        @Override
        public void onSubscribe(Subscription s) {
            wrapped.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            byteBuffer.mark();
            try {
                sdkChecksum.update(byteBuffer);
            } finally {
                byteBuffer.reset();
            }
            wrapped.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }

        @Override
        public void onComplete() {
            if (this.calculatedChecksum == null) {
                calculatedChecksum = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());

                if (!expectedChecksum.equals(calculatedChecksum)) {
                    onError(SdkClientException.create(
                            String.format("Data read has a different checksum than expected. Was %s, but expected %s",
                                    calculatedChecksum, expectedChecksum)));
                    return; // Return after onError and not call onComplete below
                }
            }
            wrapped.onComplete();
        }
    }

}
