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

package software.amazon.awssdk.auth.signer.internal;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkInternalApi
public final class DigestComputingSubscriber implements Subscriber<ByteBuffer> {
    private final CompletableFuture<byte[]> digestBytes = new CompletableFuture<>();
    private final MessageDigest messageDigest;
    private volatile boolean canceled = false;
    private volatile Subscription subscription;

    public DigestComputingSubscriber(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;

        digestBytes.whenComplete((r, t) -> {
            if (t instanceof CancellationException) {
                synchronized (DigestComputingSubscriber.this) {
                    canceled = true;
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }
            }
        });
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized (this) {
            if (!canceled) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        if (!canceled) {
            messageDigest.update(byteBuffer);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        digestBytes.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        digestBytes.complete(messageDigest.digest());
    }

    public CompletableFuture<byte[]> digestBytes() {
        return digestBytes;
    }

    public static DigestComputingSubscriber forSha256() {
        try {
            return new DigestComputingSubscriber(MessageDigest.getInstance("SHA-256"));
        } catch (NoSuchAlgorithmException e) {
            throw SdkClientException.create("Unable to create SHA-256 computing subscriber", e);
        }
    }
}
