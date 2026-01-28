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

package software.amazon.awssdk.utils.async;

import java.util.concurrent.LinkedBlockingQueue;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class FlatteningSubscriber<U> extends BaseSubscriberAdapter<Iterable<U>, U> {

    /**
     * Items given to us by the upstream subscriber that we will use to fulfill demand of the downstream subscriber.
     */
    private final LinkedBlockingQueue<U> allItems = new LinkedBlockingQueue<>();

    public FlatteningSubscriber(Subscriber<? super U> subscriber) {
        super(subscriber);
    }

    @Override
    void doWithItem(Iterable<U> nextItems) {
        nextItems.forEach(item -> {
            Validate.notNull(nextItems, "Collections flattened by the flattening subscriber must not contain null.");
            allItems.add(item);
        });
    }

    @Override
    protected void fulfillDownstreamDemand() {
        downstreamDemand.decrementAndGet();
        subscriber.onNext(allItems.poll());
    }

    /**
     * Returns true if we need to call onNext downstream. If this is executed outside the handling-state-update condition, the
     * result is subject to change.
     */
    @Override
    boolean additionalOnNextNeededCheck() {
        return !allItems.isEmpty();
    }

    /**
     * Returns true if we need to increase our upstream demand.
     */
    @Override
    boolean additionalUpstreamDemandNeededCheck() {
        return allItems.isEmpty();
    }

    /**
     * Returns true if we need to call onNext downstream. If this is executed outside the handling-state-update condition, the
     * result is subject to change.
     */
    @Override
    boolean additionalOnCompleteNeededCheck() {
        return allItems.isEmpty();
    }

    @Override
    public void onNext(Iterable<U> item) {
        super.onNext(item);
    }
}
