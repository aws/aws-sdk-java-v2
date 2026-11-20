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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SingleReaderInputStream extends InputStream {

    private final InputStream delegate;

    private final String description;

    private final ObservationSink sink;

    private final AtomicReference<Thread> occupant = new AtomicReference<>();

    private final AtomicBoolean reported = new AtomicBoolean();

    private final AtomicBoolean entered = new AtomicBoolean();

    private SingleReaderInputStream(InputStream delegate, String description, ObservationSink sink) {
        this.delegate = delegate;
        this.description = description;
        this.sink = sink;
    }

    public static InputStream wrap(InputStream delegate, String description) {
        return new SingleReaderInputStream(delegate, description, ObservationSink.current());
    }

    @Override
    public int read() throws IOException {
        boolean entered = enter("read()");
        try {
            return delegate.read();
        } finally {
            leave(entered);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        boolean entered = enter("read(byte[], int, int)");
        try {
            return delegate.read(b, off, len);
        } finally {
            leave(entered);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        boolean entered = enter("skip(long)");
        try {
            return delegate.skip(n);
        } finally {
            leave(entered);
        }
    }

    @Override
    public int available() throws IOException {
        boolean entered = enter("available()");
        try {
            return delegate.available();
        } finally {
            leave(entered);
        }
    }

    @Override
    public void mark(int readLimit) {
        boolean entered = enter("mark(int)");
        try {
            delegate.mark(readLimit);
        } finally {
            leave(entered);
        }
    }

    @Override
    public void reset() throws IOException {
        boolean entered = enter("reset()");
        try {
            delegate.reset();
        } finally {
            leave(entered);
        }
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private boolean enter(String method) {
        if (entered.compareAndSet(false, true)) {
            sink.checked(Invariant.ONE_READER_PER_STREAM);
        }
        Thread me = Thread.currentThread();
        while (true) {
            if (occupant.compareAndSet(null, me)) {
                return true;
            }
            Thread other = occupant.get();
            if (other == me) {
                return false;
            }
            if (other != null) {
                if (reported.compareAndSet(false, true)) {
                    sink.record(new Observation(Invariant.ONE_READER_PER_STREAM,
                            description + ": thread " + me.getName() + " entered " + method + " while thread "
                                    + other.getName() + " was still inside the stream"));
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
}
