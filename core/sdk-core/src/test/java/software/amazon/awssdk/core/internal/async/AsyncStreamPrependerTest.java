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

import io.reactivex.Flowable;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.Assert;

import java.util.Iterator;
import java.util.List;

import static io.reactivex.Flowable.fromPublisher;
import static io.reactivex.Flowable.just;
import static io.reactivex.Flowable.rangeLong;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class AsyncStreamPrependerTest {
    @Test
    public void empty() {
        Flowable<Long> prepender = fromPublisher(new AsyncStreamPrepender<>(Flowable.empty(), 0L));

        List<Long> actual = prepender.toList().blockingGet();

        assertEquals(singletonList(0L), actual);
    }

    @Test
    public void single() {
        Flowable<Long> prepender = fromPublisher(new AsyncStreamPrepender<>(just(1L), 0L));

        List<Long> actual = prepender.toList().blockingGet();

        assertEquals(asList(0L, 1L), actual);
    }

    @Test
    public void sequence() {
        Flowable<Long> prepender = fromPublisher(new AsyncStreamPrepender<>(rangeLong(1L, 5L), 0L));

        Iterator<Long> iterator = prepender.blockingIterable(1).iterator();

        for (long i = 0; i <= 5; i++) {
            assertEquals(i, iterator.next().longValue());
        }
    }

    @Test
    public void error() {
        Flowable<Long> error = Flowable.error(IllegalStateException::new);
        Flowable<Long> prepender = fromPublisher(new AsyncStreamPrepender<>(error, 0L));
        Iterator<Long> iterator = prepender.blockingNext().iterator();

        assertThrows(IllegalStateException.class, iterator::next);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeRequest() {
        Flowable<Long> prepender = fromPublisher(new AsyncStreamPrepender<>(rangeLong(1L, 5L), 0L));

        prepender.blockingSubscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(-1L);
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
            }

            @Override
            public void onNext(Long aLong) {}

            @Override
            public void onComplete() {}
        });
    }
}
