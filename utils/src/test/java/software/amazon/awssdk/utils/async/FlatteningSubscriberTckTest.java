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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class FlatteningSubscriberTckTest extends SubscriberWhiteboxVerification<Iterable<Integer>> {
    protected FlatteningSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Iterable<Integer>> createSubscriber(WhiteboxSubscriberProbe<Iterable<Integer>> probe) {
        Subscriber<Integer> foo = new SequentialSubscriber<>(s -> {}, new CompletableFuture<>());
        return new FlatteningSubscriber<Integer>(foo) {
            @Override
            public void onError(Throwable throwable) {
                super.onError(throwable);
                probe.registerOnError(throwable);
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                super.onSubscribe(subscription);
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        subscription.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onNext(Iterable<Integer> nextItems) {
                super.onNext(nextItems);
                probe.registerOnNext(nextItems);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public Iterable<Integer> createElement(int element) {
        return Arrays.asList(element, element);
    }
}