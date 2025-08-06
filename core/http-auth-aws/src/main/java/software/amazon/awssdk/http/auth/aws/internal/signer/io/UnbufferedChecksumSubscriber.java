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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * A decorating {@code Subscriber} that updates a list of {@code SdkChecksum}s with the data of each buffer given to
 * {@code onNext}.
 * <p>
 * This is "unbuffered", as opposed to {@link ChecksumSubscriber} which <i>does</i> buffer the data.
 */
@SdkInternalApi
public class UnbufferedChecksumSubscriber implements Subscriber<ByteBuffer> {
    private final List<SdkChecksum> checksums;
    private final Subscriber<? super ByteBuffer> wrapped;

    public UnbufferedChecksumSubscriber(List<SdkChecksum> checksums, Subscriber<? super ByteBuffer> wrapped) {
        this.checksums = new ArrayList<>(checksums);
        this.wrapped = wrapped;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription is null");
        }
        wrapped.onSubscribe(subscription);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        checksums.forEach(ck -> ck.update(byteBuffer.duplicate()));
        wrapped.onNext(byteBuffer);
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException("throwable is null");
        }
        wrapped.onError(throwable);
    }

    @Override
    public void onComplete() {
        wrapped.onComplete();
    }
}
