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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class S3CrtBorrowedBufferLeaseTest {
    @Test
    void copyTo_readsOriginalByteCountWithoutExposingBuffer() {
        ByteBuffer view = ByteBuffer.allocateDirect(3);
        view.put(new byte[] {'a', 'b', 'c'}).flip();
        S3CrtBorrowedBufferLease buffer = new S3CrtBorrowedBufferLease(view, () -> { });
        byte[] destination = new byte[3];

        assertThat(buffer.copyTo(destination, 0, destination.length)).isEqualTo(3);
        assertThat(destination).containsExactly('a', 'b', 'c');
        assertThat(buffer.hasRemaining()).isFalse();
        assertThat(buffer.byteCount()).isEqualTo(3);
    }

    @Test
    void copyMethods_afterRelease_throw() {
        S3CrtBorrowedBufferLease buffer = buffer(2, () -> { });

        buffer.release();

        assertThatThrownBy(() -> buffer.copyTo(new byte[2], 0, 2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(buffer::hasRemaining).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void release_releasesOnce() {
        AtomicInteger releases = new AtomicInteger();
        S3CrtBorrowedBufferLease buffer = buffer(2, releases::incrementAndGet);

        buffer.release();
        buffer.release();

        assertThat(releases).hasValue(1);
    }

    @Test
    void racingReleaseOperations_releaseOnce() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        S3CrtBorrowedBufferLease buffer = buffer(4, releases::incrementAndGet);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        try {
            for (int i = 0; i < 8; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        buffer.release();
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(failures).isEmpty();
        assertThat(releases).hasValue(1);
    }

    @Test
    void releaseFailure_isNotRetried() {
        AtomicInteger releases = new AtomicInteger();
        RuntimeException failure = new RuntimeException("release failed");
        S3CrtBorrowedBufferLease buffer = buffer(2, () -> {
            releases.incrementAndGet();
            throw failure;
        });

        assertThatThrownBy(buffer::release).isSameAs(failure);
        buffer.release();

        assertThat(releases).hasValue(1);
    }

    private static S3CrtBorrowedBufferLease buffer(int size, Runnable release) {
        return new S3CrtBorrowedBufferLease(ByteBuffer.allocateDirect(size), release);
    }
}
