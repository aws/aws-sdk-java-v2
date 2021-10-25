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

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkInternalApi
public class ChecksumCalculatingAsyncRequestBody implements AsyncRequestBody {
    private final Long contentLength;
    private final AsyncRequestBody wrapped;
    private final SdkChecksum sdkChecksum;

    public ChecksumCalculatingAsyncRequestBody(SdkHttpRequest request, AsyncRequestBody wrapped, SdkChecksum sdkChecksum) {
        this.contentLength = request.firstMatchingHeader("Content-Length")
                                    .map(Long::parseLong)
                                    .orElse(wrapped.contentLength()
                                                   .orElse(null));
        this.wrapped = wrapped;
        this.sdkChecksum = sdkChecksum;
    }

    @Override
    public Optional<Long> contentLength() {
        return wrapped.contentLength();
    }

    @Override
    public String contentType() {
        return wrapped.contentType();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        sdkChecksum.reset();
        wrapped.subscribe(new ChecksumCalculatingSubscriber(s, sdkChecksum, contentLength));
    }

    private static final class ChecksumCalculatingSubscriber implements Subscriber<ByteBuffer> {
        private final AtomicLong contentRead = new AtomicLong(0);
        private final Subscriber<? super ByteBuffer> wrapped;
        private final SdkChecksum checksum;
        private final Long contentLength;

        ChecksumCalculatingSubscriber(Subscriber<? super ByteBuffer> wrapped,
                                      SdkChecksum sdkChecksum,
                                      Long contentLength) {
            this.wrapped = wrapped;
            this.checksum = sdkChecksum;
            this.contentLength = contentLength;
        }

        @Override
        public void onSubscribe(Subscription s) {
            wrapped.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            int amountToReadFromByteBuffer = getAmountToReadFromByteBuffer(byteBuffer);

            if (amountToReadFromByteBuffer > 0) {
                byte[] buf = BinaryUtils.copyBytesFrom(byteBuffer, amountToReadFromByteBuffer);
                checksum.update(buf, 0, amountToReadFromByteBuffer);
            }


            wrapped.onNext(byteBuffer);
        }

        private int getAmountToReadFromByteBuffer(ByteBuffer byteBuffer) {
            // If content length is null, we should include everything in the checksum because the stream is essentially
            // unbounded.
            if (contentLength == null) {
                return byteBuffer.remaining();
            }

            long amountReadSoFar = contentRead.getAndAdd(byteBuffer.remaining());
            long amountRemaining = Math.max(0, contentLength - amountReadSoFar);

            if (amountRemaining > byteBuffer.remaining()) {
                return byteBuffer.remaining();
            } else {
                return Math.toIntExact(amountRemaining);
            }
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
