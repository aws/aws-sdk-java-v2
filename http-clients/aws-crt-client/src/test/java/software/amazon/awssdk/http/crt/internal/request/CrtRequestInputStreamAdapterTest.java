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

package software.amazon.awssdk.http.crt.internal.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;

class CrtRequestInputStreamAdapterTest {

    @Test
    void sendRequestBody_streamReadThrows_signalsOriginalErrorAndReturnsFalse() {
        IOException error = new IOException("boom");
        AtomicReference<Throwable> signaled = new AtomicReference<>();
        CrtRequestInputStreamAdapter adapter =
            new CrtRequestInputStreamAdapter(readErroringProvider(error), signaled::set);

        assertThat(adapter.sendRequestBody(ByteBuffer.allocate(16))).isFalse();

        assertThat(signaled.get()).isSameAs(error);
    }

    @Test
    void sendRequestBody_newStreamThrowsUncheckedIoException_signalsOriginalErrorAndReturnsFalse() {
        UncheckedIOException error = new UncheckedIOException(new IOException("boom"));
        AtomicReference<Throwable> signaled = new AtomicReference<>();
        CrtRequestInputStreamAdapter adapter =
            new CrtRequestInputStreamAdapter(() -> { throw error; }, signaled::set);

        assertThat(adapter.sendRequestBody(ByteBuffer.allocate(16))).isFalse();

        assertThat(signaled.get()).isSameAs(error);
    }

    @Test
    void sendRequestBody_newStreamReturnsNull_signalsNpeAndReturnsFalse() {
        AtomicReference<Throwable> signaled = new AtomicReference<>();
        CrtRequestInputStreamAdapter adapter =
            new CrtRequestInputStreamAdapter(() -> null, signaled::set);

        assertThat(adapter.sendRequestBody(ByteBuffer.allocate(16))).isFalse();

        assertThat(signaled.get()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resetPosition_closingPreviousStreamThrows_signalsOriginalErrorAndReturnsFalse() {
        IOException error = new IOException("cannot reset");
        AtomicReference<Throwable> signaled = new AtomicReference<>();
        CrtRequestInputStreamAdapter adapter =
            new CrtRequestInputStreamAdapter(closeErroringProvider(error), signaled::set);

        assertThat(adapter.resetPosition()).isTrue();
        assertThat(adapter.resetPosition()).isFalse();

        assertThat(signaled.get()).isSameAs(error);
    }

    @Test
    void sendRequestBody_calledAgainAfterError_signalsErrorOnlyOnce() {
        AtomicInteger signalCount = new AtomicInteger(0);
        CrtRequestInputStreamAdapter adapter =
            new CrtRequestInputStreamAdapter(readErroringProvider(new IOException("boom")),
                                             t -> signalCount.incrementAndGet());

        adapter.sendRequestBody(ByteBuffer.allocate(16));
        assertThatNoException().isThrownBy(() -> adapter.sendRequestBody(ByteBuffer.allocate(16)));

        assertThat(signalCount.get()).isEqualTo(1);
    }

    private static ContentStreamProvider readErroringProvider(IOException error) {
        return () -> new InputStream() {
            @Override
            public int read() throws IOException {
                throw error;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw error;
            }
        };
    }

    private static ContentStreamProvider closeErroringProvider(IOException error) {
        return () -> new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw error;
            }
        };
    }
}
