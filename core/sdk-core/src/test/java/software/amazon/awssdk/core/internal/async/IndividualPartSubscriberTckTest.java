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
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class IndividualPartSubscriberTckTest extends SubscriberWhiteboxVerification<ByteBuffer> {

    private static final byte[] DATA = {0, 1, 2, 3, 4, 5, 6, 7};

    protected IndividualPartSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber(WhiteboxSubscriberProbe<ByteBuffer> probe) {
        CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        SplittingTransformer<Object, ResponseBytes<Object>> transformer =
            SplittingTransformer.<Object, ResponseBytes<Object>>builder()
                                .upstreamResponseTransformer(AsyncResponseTransformer.toBytes())
                                .maximumBufferSizeInBytes(32L)
                                .resultFuture(new CompletableFuture<>())
                                .build();
        return transformer.new IndividualPartSubscriber<ByteBuffer>(future, ByteBuffer.wrap(new byte[0])) {
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
}
