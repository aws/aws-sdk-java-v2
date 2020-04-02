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
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;

/**
 * An implementation of {@link AsyncRequestBody} for providing data from memory. This is created using static
 * methods on {@link AsyncRequestBody}
 *
 * @see AsyncRequestBody#fromBytes(byte[])
 * @see AsyncRequestBody#fromByteBuffer(ByteBuffer)
 * @see AsyncRequestBody#fromString(String)
 */
@SdkInternalApi
public final class ByteArrayAsyncRequestBody implements AsyncRequestBody {

    private final byte[] bytes;

    public ByteArrayAsyncRequestBody(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of((long) bytes.length);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        // As per rule 1.9 we must throw NullPointerException if the subscriber parameter is null
        if (s == null) {
            throw new NullPointerException("Subscription MUST NOT be null.");
        }

        // As per 2.13, this method must return normally (i.e. not throw).
        try {
            s.onSubscribe(
                    new Subscription() {
                        private boolean done = false;

                        @Override
                        public void request(long n) {
                            if (done) {
                                return;
                            }
                            if (n > 0) {
                                done = true;
                                s.onNext(ByteBuffer.wrap(bytes));
                                s.onComplete();
                            } else {
                                s.onError(new IllegalArgumentException("§3.9: non-positive requests are not allowed!"));
                            }
                        }

                        @Override
                        public void cancel() {
                            synchronized (this) {
                                if (!done) {
                                    done = true;
                                }
                            }
                        }
                    }
            );
        } catch (Throwable ex) {
            new IllegalStateException(s + " violated the Reactive Streams rule 2.13 " +
                    "by throwing an exception from onSubscribe.", ex)
                    // When onSubscribe fails this way, we don't know what state the
                    // s is thus calling onError may cause more crashes.
                    .printStackTrace();
        }
    }
}
