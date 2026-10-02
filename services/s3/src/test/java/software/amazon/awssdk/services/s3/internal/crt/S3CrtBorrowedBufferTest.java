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
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class S3CrtBorrowedBufferTest {
    @Test
    void consumed_releasesBeforeCreditingOriginalByteCount() {
        List<String> actions = new ArrayList<>();
        AtomicLong credited = new AtomicLong();
        ByteBuffer view = ByteBuffer.allocateDirect(3);
        S3CrtBorrowedBuffer buffer = new S3CrtBorrowedBuffer(
            view, 3, () -> actions.add("release"), bytes -> {
                actions.add("credit");
                credited.addAndGet(bytes);
            });

        view.get();
        buffer.consumed();
        buffer.consumed();
        buffer.discard();

        assertThat(actions).containsExactly("release", "credit");
        assertThat(credited).hasValue(3);
        // The credited count is the byte count the lease was created with, not whatever remains in the view now.
        assertThat(buffer.byteCount()).isEqualTo(3);
    }

    @Test
    void buffer_afterRelease_throwsRatherThanExposingReleasedMemory() {
        S3CrtBorrowedBuffer consumedBuffer = buffer(2, () -> {
        }, bytes -> {
        });
        consumedBuffer.buffer().get(new byte[2]);
        consumedBuffer.consumed();

        // Once the lease is released the pooled memory can belong to another request, so handing the view back out
        // would silently serve someone else's bytes as this object's content.
        assertThatThrownBy(consumedBuffer::buffer)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("after its lease was released");

        S3CrtBorrowedBuffer discardedBuffer = buffer(2, () -> {
        }, bytes -> {
        });
        discardedBuffer.discard();
        assertThatThrownBy(discardedBuffer::buffer).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buffer_doesNotShareItsCursorWithTheViewCrtHandedOut() {
        ByteBuffer crtView = ByteBuffer.allocateDirect(4);
        S3CrtBorrowedBuffer buffer = new S3CrtBorrowedBuffer(crtView, 4, () -> {
        }, bytes -> {
        });

        // CRT returns the lease's own ByteBuffer instance and may hand it out again after the lease is released, so
        // reading here must not move its position.
        buffer.buffer().get(new byte[4]);
        assertThat(buffer.buffer().hasRemaining()).isFalse();
        assertThat(crtView.position()).isZero();
        assertThat(crtView.remaining()).isEqualTo(4);

        // The reverse direction too: CRT moving its own cursor must not shorten what we deliver.
        crtView.get();
        assertThat(buffer.byteCount()).isEqualTo(4);
    }

    @Test
    void discard_releasesWithoutCredit() {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBuffer buffer = buffer(2, releases::incrementAndGet, credited::addAndGet);

        buffer.discard();
        buffer.consumed();
        buffer.discard();

        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(0);
    }

    @Test
    void racingTerminalOperations_chooseOnePath() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        S3CrtBorrowedBuffer buffer = buffer(4, releases::incrementAndGet, credited::addAndGet);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        try {
            for (int i = 0; i < 8; i++) {
                boolean consume = i % 2 == 0;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (consume) {
                            buffer.consumed();
                        } else {
                            buffer.discard();
                        }
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
        assertThat(credited.get()).isIn(0L, 4L);
    }

    @Test
    void releaseFailure_preventsCreditAndIsNotRetried() {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credited = new AtomicLong();
        RuntimeException failure = new RuntimeException("release failed");
        S3CrtBorrowedBuffer buffer = buffer(2, () -> {
            releases.incrementAndGet();
            throw failure;
        }, credited::addAndGet);

        assertThatThrownBy(buffer::consumed).isSameAs(failure);
        buffer.consumed();
        buffer.discard();

        assertThat(releases).hasValue(1);
        assertThat(credited).hasValue(0);
    }

    @Test
    void creditFailure_occursAfterReleaseAndIsNotRetried() {
        List<String> actions = new ArrayList<>();
        RuntimeException failure = new RuntimeException("credit failed");
        S3CrtBorrowedBuffer buffer = buffer(2, () -> actions.add("release"), bytes -> {
            actions.add("credit");
            throw failure;
        });

        assertThatThrownBy(buffer::consumed).isSameAs(failure);
        buffer.consumed();
        buffer.discard();

        assertThat(actions).containsExactly("release", "credit");
    }

    private static S3CrtBorrowedBuffer buffer(int size, Runnable release, java.util.function.LongConsumer credit) {
        return new S3CrtBorrowedBuffer(ByteBuffer.allocateDirect(size), size, release, credit);
    }
}
