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

package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncResponseTransformer.BaosSubscriber;

/**
 * TCK verification test for {@link BaosSubscriber}.
 */
public class BaosSubscriberTckTest extends SubscriberWhiteboxVerification<ByteBuffer> {
    private static final byte[] CONTENT = new byte[16];

    public BaosSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber(WhiteboxSubscriberProbe<ByteBuffer> whiteboxSubscriberProbe) {
        return new BaosSubscriber(new CompletableFuture<>()) {

            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                whiteboxSubscriberProbe.registerOnSubscribe(new SubscriberPuppet() {

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
                whiteboxSubscriberProbe.registerOnNext(bb);
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
    public ByteBuffer createElement(int i) {
        return ByteBuffer.wrap(CONTENT);
    }
}
