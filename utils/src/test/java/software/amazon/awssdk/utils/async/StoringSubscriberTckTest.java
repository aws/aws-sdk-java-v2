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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class StoringSubscriberTckTest extends SubscriberWhiteboxVerification<Integer> {
    protected StoringSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
        return new StoringSubscriber<Integer>(16) {
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
            public void onNext(Integer nextItems) {
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
    public Integer createElement(int element) {
        return element;
    }
}