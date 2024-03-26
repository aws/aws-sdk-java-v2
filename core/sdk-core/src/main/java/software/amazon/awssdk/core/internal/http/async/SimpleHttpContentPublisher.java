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

package software.amazon.awssdk.core.internal.http.async;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Implementation of {@link SdkHttpContentPublisher} that provides all it's data at once. Useful for
 * non streaming operations that are already marshalled into memory.
 */
@SdkInternalApi
public final class SimpleHttpContentPublisher implements SdkHttpContentPublisher {

    private final byte[] content;
    private final int length;

    public SimpleHttpContentPublisher(SdkHttpFullRequest request) {
        this.content = request.contentStreamProvider().map(p -> invokeSafely(() -> IoUtils.toByteArray(p.newStream())))
                                                      .orElseGet(() -> new byte[0]);
        this.length = content.length;
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of((long) length);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        s.onSubscribe(new SubscriptionImpl(s));
    }

    private class SubscriptionImpl implements Subscription {
        private boolean running = true;
        private final Subscriber<? super ByteBuffer> s;

        private SubscriptionImpl(Subscriber<? super ByteBuffer> s) {
            this.s = s;
        }

        @Override
        public void request(long n) {
            if (running) {
                running = false;
                if (n <= 0) {
                    s.onError(new IllegalArgumentException("Demand must be positive"));
                } else {
                    s.onNext(ByteBuffer.wrap(content));
                    s.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}
