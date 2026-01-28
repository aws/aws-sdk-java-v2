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

import java.util.Iterator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class IterablePublisher<T> implements Publisher<T> {
    private final Iterable<T> iterable;

    public IterablePublisher(Iterable<T> iterable) {
        this.iterable = Validate.paramNotNull(iterable, "iterable");
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Iterator<T> iterator = iterable.iterator();
        SimplePublisher<T> publisher = new SimplePublisher<>();

        // Prime the simple publisher with 1 event. More will be sent as these complete.
        sendEvent(iterator, publisher);

        publisher.subscribe(subscriber);
    }

    private void sendEvent(Iterator<T> iterator, SimplePublisher<T> publisher) {
        try {
            if (!iterator.hasNext()) {
                publisher.complete();
                return;
            }

            T next = iterator.next();
            if (next == null) {
                publisher.error(new IllegalArgumentException("Iterable returned null"));
                return;
            }

            publisher.send(next).whenComplete((v, t) -> {
                if (t != null) {
                    publisher.error(t);
                } else {
                    sendEvent(iterator, publisher);
                }
            });
        } catch (Throwable e) {
            publisher.error(e);
        }
    }
}