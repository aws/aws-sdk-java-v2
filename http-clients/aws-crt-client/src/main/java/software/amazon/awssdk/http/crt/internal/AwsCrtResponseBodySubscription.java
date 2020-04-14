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

package software.amazon.awssdk.http.crt.internal;

import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Helper Class that passes through calls from a Subscription to a AwsCrtResponseBodyPublisher
 */
@SdkInternalApi
public class AwsCrtResponseBodySubscription implements Subscription {
    private final AwsCrtResponseBodyPublisher publisher;

    public AwsCrtResponseBodySubscription(AwsCrtResponseBodyPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            // Reactive Stream Spec requires us to call onError() callback instead of throwing Exception here.
            publisher.setError(new IllegalArgumentException("Request is for <= 0 elements: " + n));
            publisher.publishToSubscribers();
            return;
        }

        publisher.request(n);
        publisher.publishToSubscribers();
    }

    @Override
    public void cancel() {
        publisher.setCancelled();
    }
}
