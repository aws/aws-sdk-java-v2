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

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

public class DelegatingBufferingSubscriberTckTest extends SubscriberWhiteboxVerification<ByteBuffer> {

    private static final byte[] DATA = {0, 1, 2, 3, 4, 5, 6, 7};

    protected DelegatingBufferingSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber(WhiteboxSubscriberProbe<ByteBuffer> probe) {
        Subscriber<ByteBuffer> delegate = new NoOpSubscriber();
        return new DelegatingBufferingSubscriber(1024L, delegate) {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                probe.registerOnSubscribe(new SubscriberPuppet() {
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
            public void onNext(ByteBuffer bb) {
                super.onNext(bb);
                probe.registerOnNext(bb);
            }

            @Override
            public void onError(Throwable t) {
                super.onError(t);
                probe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public ByteBuffer createElement(int element) {
        return ByteBuffer.wrap(DATA);
    }

    static class NoOpSubscriber implements Subscriber<ByteBuffer> {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            // do nothing, test only
        }

        @Override
        public void onComplete() {
            // do nothing, test only
        }
    }

}