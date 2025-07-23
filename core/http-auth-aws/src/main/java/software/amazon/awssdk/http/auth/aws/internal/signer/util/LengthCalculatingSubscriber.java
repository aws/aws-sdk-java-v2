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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class LengthCalculatingSubscriber implements Subscriber<ByteBuffer> {
    private final CompletableFuture<Long> contentLengthFuture = new CompletableFuture<>();
    private Subscription subscription;
    private long length = 0;

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.subscription == null) {
            this.subscription = subscription;
            this.subscription.request(Long.MAX_VALUE);
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        length += byteBuffer.remaining();
    }

    @Override
    public void onError(Throwable throwable) {
        contentLengthFuture.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        contentLengthFuture.complete(length);
    }

    public CompletableFuture<Long> contentLengthFuture() {
        return contentLengthFuture;
    }
}
