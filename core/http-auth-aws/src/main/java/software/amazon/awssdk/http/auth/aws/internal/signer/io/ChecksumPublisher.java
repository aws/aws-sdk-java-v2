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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Checksum;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A subscriber that takes a collection of checksums, and updates each checksum when it receives data.
 */
@SdkInternalApi
public final class ChecksumPublisher implements Publisher<ByteBuffer> {
    private final CompletableFuture<Void> signal = new CompletableFuture<>();
    private final Publisher<ByteBuffer> publisher;
    private final Collection<? extends Checksum> consumers;
    private boolean isSubscribed = false;

    public ChecksumPublisher(Publisher<ByteBuffer> publisher, Collection<? extends Checksum> consumers) {
        this.publisher = publisher;
        this.consumers = Collections.unmodifiableCollection(consumers);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        if (!isSubscribed) {
            publisher.subscribe(new ChecksumSubscriber(subscriber, consumers, signal));
            isSubscribed = true;
        } else {
            subscriber.onError(new IllegalStateException("Only one subscription may be active at a time."));
        }
    }

    public CompletableFuture<Void> checksum() {
        if (isSubscribed) {
            return signal;
        }
        throw new IllegalStateException("Checksum will never complete because nothing is subscribed.");
    }
}
