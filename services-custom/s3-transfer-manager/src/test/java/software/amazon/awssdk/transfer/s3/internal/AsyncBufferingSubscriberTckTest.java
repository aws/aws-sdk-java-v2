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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;


public class AsyncBufferingSubscriberTckTest extends SubscriberWhiteboxVerification<String> {

    protected AsyncBufferingSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<String> createSubscriber(SubscriberWhiteboxVerification.WhiteboxSubscriberProbe<String> whiteboxSubscriberProbe) {
        return new AsyncBufferingSubscriber<String>(s -> CompletableFuture.completedFuture("test"), new CompletableFuture<>(),
                                                    1) {

            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                whiteboxSubscriberProbe.registerOnSubscribe(new SubscriberWhiteboxVerification.SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long l) {
                        s.request(l);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(String item) {
                super.onNext(item);
                whiteboxSubscriberProbe.registerOnNext(item);
            }

            @Override
            public void onError(Throwable t) {
                super.onError(t);
                whiteboxSubscriberProbe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                whiteboxSubscriberProbe.registerOnComplete();
            }
        };
    }

    @Override
    public String createElement(int i) {
        return "test";
    }
}
