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
import static software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult.END_OF_STREAM;
import static software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult.SUCCESS;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class InputStreamConsumingPublisherTest {
    private static final ExecutorService EXECUTOR =
        Executors.newCachedThreadPool(new ThreadFactoryBuilder().daemonThreads(true).build());
    private ByteBufferStoringSubscriber subscriber;
    private InputStreamConsumingPublisher publisher;

    @BeforeEach
    public void setup() {
        this.subscriber = new ByteBufferStoringSubscriber(Long.MAX_VALUE);
        this.publisher = new InputStreamConsumingPublisher();
    }

    @Test
    public void subscribeAfterWrite_completes() throws InterruptedException {
        EXECUTOR.submit(() -> publisher.doBlockingWrite(streamOfLength(0)));
        Thread.sleep(200);
        publisher.subscribe(subscriber);

        assertThat(subscriber.transferTo(ByteBuffer.allocate(0))).isEqualTo(END_OF_STREAM);
    }

    @Test
    public void zeroKb_completes() {
        publisher.subscribe(subscriber);

        assertThat(publisher.doBlockingWrite(streamOfLength(0))).isEqualTo(0);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(0))).isEqualTo(END_OF_STREAM);
    }

    @Test
    public void oneKb_writesAndCompletes() {
        publisher.subscribe(subscriber);

        assertThat(publisher.doBlockingWrite(streamOfLength(1024))).isEqualTo(1024);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(1023))).isEqualTo(SUCCESS);
        assertThat(subscriber.transferTo(ByteBuffer.allocate(1))).isEqualTo(END_OF_STREAM);
    }

    @Test
    public void bytesAreDeliveredInOrder() {
        publisher.subscribe(subscriber);

        assertThat(publisher.doBlockingWrite(streamWithAllBytesInOrder())).isEqualTo(256);

        ByteBuffer output = ByteBuffer.allocate(256);
        assertThat(subscriber.transferTo(output)).isEqualTo(END_OF_STREAM);
        output.flip();

        for (int i = 0; i < 256; i++) {
            assertThat(output.get()).isEqualTo((byte) i);
        }
    }

    @Test
    public void failedRead_signalsOnError() {
        publisher.subscribe(subscriber);

        assertThatThrownBy(() -> publisher.doBlockingWrite(streamWithFailedReadAfterLength(1024)))
            .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    public void cancel_signalsOnError() {
        publisher.subscribe(subscriber);
        publisher.cancel();

        assertThatThrownBy(() -> subscriber.transferTo(ByteBuffer.allocate(0))).isInstanceOf(CancellationException.class);
    }

    @Test
    public void cancel_stopsRunningWrites() {
        publisher.subscribe(subscriber);
        Future<?> write = EXECUTOR.submit(() -> publisher.doBlockingWrite(streamOfLength(Integer.MAX_VALUE)));
        publisher.cancel();

        assertThatThrownBy(write::get).hasRootCauseInstanceOf(CancellationException.class);
    }

    @Test
    public void cancel_beforeWrite_stopsWrite() {
        publisher.subscribe(subscriber);
        publisher.cancel();
        assertThatThrownBy(() -> publisher.doBlockingWrite(streamOfLength(Integer.MAX_VALUE)))
            .hasRootCauseInstanceOf(CancellationException.class);
    }

    @Test
    public void cancel_beforeSubscribe_stopsWrite() {
        publisher.cancel();
        publisher.subscribe(subscriber);
        assertThatThrownBy(() -> publisher.doBlockingWrite(streamOfLength(Integer.MAX_VALUE)))
            .hasRootCauseInstanceOf(CancellationException.class);
    }

    public InputStream streamOfLength(int length) {
        return new InputStream() {
            int i = 0;
            @Override
            public int read() throws IOException {
                if (i >= length) {
                    return -1;
                }
                ++i;
                return 1;
            }
        };
    }

    public InputStream streamWithAllBytesInOrder() {
        return new InputStream() {
            int i = 0;
            @Override
            public int read() throws IOException {
                if (i > 255) {
                    return -1;
                }
                return i++;
            }
        };
    }

    public InputStream streamWithFailedReadAfterLength(int length) {
        return new InputStream() {
            int i = 0;
            @Override
            public int read() throws IOException {
                if (i > length) {
                    throw new IOException("Failed to read!");
                }
                ++i;
                return 1;
            }
        };
    }
}