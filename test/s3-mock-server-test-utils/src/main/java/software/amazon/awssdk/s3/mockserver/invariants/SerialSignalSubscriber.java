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

package software.amazon.awssdk.s3.mockserver.invariants;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public final class SerialSignalSubscriber<T> implements Subscriber<T> {

    private final Subscriber<T> delegate;

    private final String description;

    private final ObservationSink sink;

    private final AtomicReference<Inside> occupant = new AtomicReference<>();

    private final AtomicBoolean reported = new AtomicBoolean();

    private final AtomicBoolean signalled = new AtomicBoolean();

    private SerialSignalSubscriber(Subscriber<T> delegate, String description, ObservationSink sink) {
        this.delegate = delegate;
        this.description = description;
        this.sink = sink;
    }

    public static <T> Subscriber<T> wrap(Subscriber<T> delegate, String description) {
        return new SerialSignalSubscriber<>(delegate, description, ObservationSink.current());
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        boolean entered = enter("onSubscribe");
        try {
            delegate.onSubscribe(subscription);
        } finally {
            leave(entered);
        }
    }

    @Override
    public void onNext(T item) {
        boolean entered = enter("onNext");
        try {
            delegate.onNext(item);
        } finally {
            leave(entered);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        boolean entered = enter("onError");
        try {
            delegate.onError(throwable);
        } finally {
            leave(entered);
        }
    }

    @Override
    public void onComplete() {
        boolean entered = enter("onComplete");
        try {
            delegate.onComplete();
        } finally {
            leave(entered);
        }
    }

    private boolean enter(String signal) {
        if (signalled.compareAndSet(false, true)) {
            sink.checked(Invariant.SUBSCRIBER_SIGNALS_ARE_SERIAL);
        }
        Inside me = new Inside(Thread.currentThread(), signal);
        while (true) {
            if (occupant.compareAndSet(null, me)) {
                return true;
            }
            Inside other = occupant.get();
            if (other != null) {
                if (other.thread() != me.thread() && reported.compareAndSet(false, true)) {
                    sink.record(new Observation(Invariant.SUBSCRIBER_SIGNALS_ARE_SERIAL,
                            description + ": " + signal + " on thread " + me.thread().getName() + " while thread "
                                    + other.thread().getName() + " was inside " + other.signal()));
                }
                return false;
            }
        }
    }

    private void leave(boolean claimed) {
        if (claimed) {
            occupant.set(null);
        }
    }

    private static final class Inside {
        private final Thread thread;
        private final String signal;

        private Inside(Thread thread, String signal) {
            this.thread = thread;
            this.signal = signal;
        }

        public Thread thread() {
            return thread;
        }

        public String signal() {
            return signal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Inside that = (Inside) o;
            return Objects.equals(thread, that.thread)
                   && Objects.equals(signal, that.signal);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(thread);
            result = 31 * result + Objects.hashCode(signal);
            return result;
        }

        @Override
        public String toString() {
            return "Inside[thread=" + thread + ", signal=" + signal + "]";
        }
    }
}
