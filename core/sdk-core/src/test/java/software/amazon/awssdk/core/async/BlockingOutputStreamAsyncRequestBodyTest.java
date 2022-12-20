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

package software.amazon.awssdk.core.async;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult.END_OF_STREAM;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.utils.CancellableOutputStream;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.StoringSubscriber;

class BlockingOutputStreamAsyncRequestBodyTest {
    @Test
    public void outputStream_waitsForSubscription() throws IOException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        try {
            BlockingOutputStreamAsyncRequestBody requestBody =
                AsyncRequestBody.forBlockingOutputStream(0L);
            executor.schedule(() -> requestBody.subscribe(new StoringSubscriber<>(1)), 100, MILLISECONDS);
            try (OutputStream stream = requestBody.outputStream()) {
                stream.write('h');
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    public void outputStream_failsIfSubscriptionNeverComes()  {
        BlockingOutputStreamAsyncRequestBody requestBody =
            new BlockingOutputStreamAsyncRequestBody(0L, Duration.ofSeconds(1));
        assertThatThrownBy(requestBody::outputStream).hasMessageContaining("The service request was not made");
    }

    @Test
    public void outputStream_writesToSubscriber() throws IOException {
        BlockingOutputStreamAsyncRequestBody requestBody =
            AsyncRequestBody.forBlockingOutputStream(0L);
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(4);
        requestBody.subscribe(subscriber);

        CancellableOutputStream outputStream = requestBody.outputStream();
        outputStream.write(0);
        outputStream.write(1);
        outputStream.close();

        ByteBuffer out = ByteBuffer.allocate(4);
        assertThat(subscriber.transferTo(out)).isEqualTo(END_OF_STREAM);
        out.flip();

        assertThat(out.remaining()).isEqualTo(2);
        assertThat(out.get()).isEqualTo((byte) 0);
        assertThat(out.get()).isEqualTo((byte) 1);
    }

}