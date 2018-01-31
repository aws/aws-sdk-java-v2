/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class SingleByteArrayAsyncRequestProvider implements AsyncRequestProvider {

    private final byte[] bytes;

    SingleByteArrayAsyncRequestProvider(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    @Override
    public long contentLength() {
        return bytes.length;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        // FIXME missing protection abiding to rule 1.9, proposal:
        // As per rule 1.09, we need to throw a `java.lang.NullPointerException`
        // if the `Subscriber` is `null`
        // if (subscriber == null) throw null;

        // FIXME: onSubscribe is user code, and could be ill behaved, as library we should protect from this,
        // FIXME: This is covered by spec rule 2.13; proposal:
        // As per 2.13, this method must return normally (i.e. not throw).
        // try {
        subscriber.onSubscribe(
            new Subscription() {
                @Override
                public void request(long n) {
                    if (n > 0) {
                        subscriber.onNext(ByteBuffer.wrap(bytes));
                        subscriber.onComplete();
                    }
                    // FIXME missing required validation code (rule 1.9):
                    //   "Non-positive requests should be honored with IllegalArgumentException"
                    // proposal:
                    // else {
                    //  subscriber.onError(new IllegalArgumentException("§3.9: non-positive requests are not allowed!"));
                    // }
                }

                @Override
                public void cancel() {
                }
            }
        );
        // end of implementing 2.13 spec requirement
        //  } catch (Throwable ex) {
        //  new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 " +
        //      "by throwing an exception from onSubscribe.", ex)
        //      // When onSubscribe fails this way, we don't know what state the
        //      // subscriber is thus calling onError may cause more crashes.
        //      .printStackTrace();
        //    }
    }
}
