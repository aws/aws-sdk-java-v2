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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkInternalApi
public class ChecksumCalculatingAsyncRequestBody implements AsyncRequestBody {

    private final AsyncRequestBody wrapped;
    private final SdkChecksum sdkChecksum;

    public ChecksumCalculatingAsyncRequestBody(AsyncRequestBody wrapped, SdkChecksum sdkChecksum) {
        this.wrapped = wrapped;
        this.sdkChecksum = sdkChecksum;
    }

    @Override
    public Optional<Long> contentLength() {
        return wrapped.contentLength();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        sdkChecksum.reset();
        wrapped.subscribe(new ChecksumCalculatingSubscriber(s, sdkChecksum));
    }

    private static final class ChecksumCalculatingSubscriber implements Subscriber<ByteBuffer> {

        private final Subscriber<? super ByteBuffer> wrapped;
        private final SdkChecksum checksum;

        ChecksumCalculatingSubscriber(Subscriber<? super ByteBuffer> wrapped,
                                      SdkChecksum sdkChecksum) {
            this.wrapped = wrapped;
            this.checksum = sdkChecksum;
        }

        @Override
        public void onSubscribe(Subscription s) {
            wrapped.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            byte[] buf = BinaryUtils.copyBytesFrom(byteBuffer);
            checksum.update(buf, 0, buf.length);
            wrapped.onNext(byteBuffer);
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
