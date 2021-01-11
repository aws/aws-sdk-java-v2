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


package software.amazon.awssdk.core.internal.util;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/** An implementation of {@link org.reactivestreams.Subscription} that does nothing.
 * <p>
 * Useful in situations where a {@link org.reactivestreams.Publisher} needs to
 * signal {@code exceptionOccurred} or {@code onComplete} immediately after
 * {@code subscribe()} but but it needs to signal{@code onSubscription} first.
 */
@SdkInternalApi
public final class NoopSubscription implements Subscription {
    private final Subscriber<?> subscriber;

    public NoopSubscription(Subscriber<?> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void request(long l) {
        if (l < 1) {
            subscriber.onError(new IllegalArgumentException("Demand must be positive!"));
        }
    }

    @Override
    public void cancel() {
    }
}
