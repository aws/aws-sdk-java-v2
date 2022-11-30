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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult;

public class OutputStreamPublisherTest {
    private StoringSubscriber<ByteBuffer> storingSubscriber;
    private ByteBufferStoringSubscriber byteStoringSubscriber;
    private OutputStreamPublisher publisher;

    @BeforeEach
    public void setup() {
        storingSubscriber = new StoringSubscriber<>(Integer.MAX_VALUE);
        byteStoringSubscriber = new ByteBufferStoringSubscriber(Integer.MAX_VALUE);
        publisher = new OutputStreamPublisher();
    }

    @Test
    public void oneByteWritesAreBuffered() {
        publisher.subscribe(storingSubscriber);
        publisher.write(0);
        assertThat(storingSubscriber.poll()).isNotPresent();
    }

    @Test
    public void oneByteWritesAreFlushedEventually() {
        publisher.subscribe(storingSubscriber);
        for (int i = 0; i < 1024 * 1024; i++) {
            publisher.write(0);
        }
        assertThat(storingSubscriber.poll()).hasValueSatisfying(e -> {
            assertThat(e.value().get()).isEqualTo((byte) 0);
        });
    }

    @Test
    public void flushDrainsBufferedBytes() {
        publisher.subscribe(storingSubscriber);
        publisher.write(0);
        publisher.flush();
        assertThat(storingSubscriber.poll()).hasValueSatisfying(v -> {
            assertThat(v.value().remaining()).isEqualTo(1);
        });
    }

    @Test
    public void emptyFlushDoesNothing() {
        publisher.subscribe(storingSubscriber);
        publisher.flush();
        assertThat(storingSubscriber.poll()).isNotPresent();
    }

    @Test
    public void oneByteWritesAreSentInOrder() {
        publisher.subscribe(storingSubscriber);
        for (int i = 0; i < 256; i++) {
            publisher.write(i);
        }
        publisher.flush();

        assertThat(storingSubscriber.poll()).hasValueSatisfying(e -> {
            assertThat(e.value().get()).isEqualTo((byte) 0);
        });
    }

    @Test
    @Timeout(30)
    public void writesBeforeSubscribeBlockUntilSubscribe() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().daemonThreads(true).build());
        try {
            Future<?> writes = executor.submit(() -> {
                publisher.write(new byte[256]);
                publisher.close();
            });

            Thread.sleep(200);

            assertThat(storingSubscriber.poll()).isNotPresent();
            assertThat(writes.isDone()).isFalse();
            publisher.subscribe(storingSubscriber);
            assertThat(storingSubscriber.poll()).isPresent();
            writes.get();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void mixedWriteTypesAreSentInOrder() {
        publisher.subscribe(byteStoringSubscriber);

        AtomicInteger i = new AtomicInteger(0);
        writeByte(i);
        writeBytes(i);
        writeOffsetBytes(i);

        writeByte(i);
        writeOffsetBytes(i);
        writeBytes(i);

        writeBytes(i);
        writeByte(i);
        writeOffsetBytes(i);

        writeBytes(i);
        writeOffsetBytes(i);
        writeByte(i);

        writeOffsetBytes(i);
        writeByte(i);
        writeBytes(i);

        writeOffsetBytes(i);
        writeByte(i);
        writeBytes(i);

        publisher.close();

        ByteBuffer out = ByteBuffer.allocate(i.get() + 1);
        assertThat(byteStoringSubscriber.blockingTransferTo(out)).isEqualTo(TransferResult.END_OF_STREAM);
        out.flip();

        for (int j = 0; j < i.get(); j++) {
            assertThat(out.get()).isEqualTo((byte) j);
        }
    }

    @Test
    public void cancel_preventsSingleByteWrites() {
        publisher.subscribe(byteStoringSubscriber);
        publisher.cancel();

        assertThatThrownBy(() -> publisher.write(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void cancel_preventsMultiByteWrites() {
        publisher.subscribe(byteStoringSubscriber);
        publisher.cancel();

        assertThatThrownBy(() -> publisher.write(new byte[8])).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void cancel_preventsOffsetByteWrites() {
        publisher.subscribe(byteStoringSubscriber);
        publisher.cancel();

        assertThatThrownBy(() -> publisher.write(new byte[8], 0, 1)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void close_preventsSingleByteWrites() {
        publisher.subscribe(byteStoringSubscriber);
        publisher.close();

        assertThatThrownBy(() -> publisher.write(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void close_preventsMultiByteWrites() {
        publisher.subscribe(byteStoringSubscriber);
        publisher.close();

        assertThatThrownBy(() -> publisher.write(new byte[8])).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void close_preventsOffsetByteWrites() {
        publisher.subscribe(byteStoringSubscriber);
        publisher.close();

        assertThatThrownBy(() -> publisher.write(new byte[8], 0, 1)).hasRootCauseInstanceOf(IllegalStateException.class);
    }


    private void writeByte(AtomicInteger i) {
        publisher.write(i.getAndIncrement());
    }

    private void writeBytes(AtomicInteger i) {
        publisher.write(new byte[] { (byte) i.getAndIncrement(), (byte) i.getAndIncrement() });
    }

    private void writeOffsetBytes(AtomicInteger i) {
        publisher.write(new byte[] {
                            0,
                            0,
                            (byte) i.getAndIncrement(),
                            (byte) i.getAndIncrement(),
                            (byte) i.getAndIncrement()
                        },
                        2, 3);
    }

}