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

package software.amazon.awssdk.services.s3.checksums;

import static java.lang.Math.toIntExact;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkInternalApi
public final class ChecksumValidatingPublisher implements SdkPublisher<ByteBuffer> {

    private final Publisher<ByteBuffer> publisher;
    private final SdkChecksum sdkChecksum;
    private final long contentLength;

    public ChecksumValidatingPublisher(Publisher<ByteBuffer> publisher,
                                       SdkChecksum sdkChecksum,
                                       long contentLength) {
        this.publisher = publisher;
        this.sdkChecksum = sdkChecksum;
        this.contentLength = contentLength;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        if (contentLength > 0) {
            publisher.subscribe(new ChecksumValidatingSubscriber(s, sdkChecksum, contentLength));
        } else {
            publisher.subscribe(new ChecksumSkippingSubscriber(s));
        }
    }

    private static class ChecksumValidatingSubscriber implements Subscriber<ByteBuffer> {

        private static final int CHECKSUM_SIZE = 16;

        private final Subscriber<? super ByteBuffer> wrapped;
        private final SdkChecksum sdkChecksum;
        private final long strippedLength;

        private byte[] streamChecksum = new byte[CHECKSUM_SIZE];
        private long lengthRead = 0;

        ChecksumValidatingSubscriber(Subscriber<? super ByteBuffer> wrapped,
                                     SdkChecksum sdkChecksum,
                                     long contentLength) {
            this.wrapped = wrapped;
            this.sdkChecksum = sdkChecksum;
            this.strippedLength = contentLength - CHECKSUM_SIZE;
        }

        @Override
        public void onSubscribe(Subscription s) {
            wrapped.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            byte[] buf = BinaryUtils.copyBytesFrom(byteBuffer);

            if (lengthRead < strippedLength) {
                int toUpdate = (int) Math.min(strippedLength - lengthRead, buf.length);

                sdkChecksum.update(buf, 0, toUpdate);
            }
            lengthRead += buf.length;

            if (lengthRead >= strippedLength) {
                // Incoming buffer contains at least a bit of the checksum
                // Code below covers both cases of the incoming buffer relative to checksum border
                // a) buffer starts before checksum border and extends into checksum
                //      |<------ data ------->|<--cksum-->|   <--- original data
                //                       |<---buffer--->|     <--- incoming buffer
                //                            |<------->|     <--- checksum bytes so far
                //                       |<-->|               <--- bufChecksumOffset
                //                            |               <--- streamChecksumOffset
                // b) buffer starts at or after checksum border
                //      |<------ data ------->|<--cksum-->|   <--- original data
                //                                |<-->|      <--- incoming buffer
                //                            |<------>|      <--- checksum bytes so far
                //                                |           <--- bufChecksumOffset
                //                            |<->|           <--- streamChecksumOffset
                int cksumBytesSoFar = toIntExact(lengthRead - strippedLength);
                int bufChecksumOffset = (buf.length > cksumBytesSoFar) ? (buf.length - cksumBytesSoFar) : 0;
                int streamChecksumOffset = (buf.length > cksumBytesSoFar) ? 0 : (cksumBytesSoFar - buf.length);
                int cksumBytes = Math.min(cksumBytesSoFar, buf.length);
                System.arraycopy(buf, bufChecksumOffset, streamChecksum, streamChecksumOffset, cksumBytes);
                if (buf.length > cksumBytesSoFar) {
                    wrapped.onNext(ByteBuffer.wrap(Arrays.copyOfRange(buf, 0, buf.length - cksumBytesSoFar)));
                } else {
                    // Always be sure to satisfy the wrapped publisher's demand.
                    wrapped.onNext(ByteBuffer.allocate(0));

                    // TODO: The most efficient implementation would request more from the upstream publisher instead of relying
                    //  on the downstream publisher to do that, but that's much more complicated: it requires tracking
                    //  outstanding demand from the downstream publisher. Long-term we should migrate to an RxJava publisher
                    //  implementation to reduce how error-prone our publisher implementations are.
                }
            } else {
                // Incoming buffer totally excludes the checksum
                wrapped.onNext(byteBuffer);
            }
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }

        @Override
        public void onComplete() {
            if (strippedLength > 0) {
                int streamChecksumInt = ByteBuffer.wrap(streamChecksum).getInt();
                int computedChecksumInt = ByteBuffer.wrap(sdkChecksum.getChecksumBytes()).getInt();
                if (streamChecksumInt != computedChecksumInt) {
                    onError(SdkClientException.create(
                        String.format("Data read has a different checksum than expected. Was %d, but expected %d",
                                      computedChecksumInt, streamChecksumInt)));
                    return; // Return after onError and not call onComplete below
                }
            }
            wrapped.onComplete();
        }
    }

    private static class ChecksumSkippingSubscriber implements Subscriber<ByteBuffer> {
        private static final int CHECKSUM_SIZE = 16;

        private final Subscriber<? super ByteBuffer> wrapped;

        ChecksumSkippingSubscriber(Subscriber<? super ByteBuffer> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void onSubscribe(Subscription s) {
            wrapped.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            byte[] buf = BinaryUtils.copyBytesFrom(byteBuffer);
            wrapped.onNext(ByteBuffer.wrap(Arrays.copyOfRange(buf, 0, buf.length - CHECKSUM_SIZE)));
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }

        @Override
        public void onComplete() {
            wrapped.onComplete();
        }
    }

}
