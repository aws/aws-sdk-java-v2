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

package software.amazon.awssdk.http;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class EmptyPublisher implements SdkHttpContentPublisher {
    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new EmptySubscription(subscriber));
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of(0L);
    }

    private static class EmptySubscription implements Subscription {
        private final Subscriber subscriber;
        private volatile boolean done;

        EmptySubscription(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long l) {
            if (!done) {
                done = true;
                if (l <= 0) {
                    this.subscriber.onError(new IllegalArgumentException("Demand must be positive"));
                } else {
                    this.subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            done = true;
        }
    }
}
