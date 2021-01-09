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

package software.amazon.awssdk.core.internal.http.timers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import org.junit.Test;

public class SyncTimeoutTaskTest {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void teardown() {
        EXEC.shutdown();
    }

    @Test
    public void taskInProgress_hasExecutedReturnsTrue() throws InterruptedException {
        Thread mockThread = mock(Thread.class);
        SyncTimeoutTask task = new SyncTimeoutTask(mockThread);

        CountDownLatch latch = new CountDownLatch(1);

        task.abortable(() -> {
            try {
                latch.countDown();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        });

        EXEC.submit(task);

        latch.await();

        assertThat(task.hasExecuted()).isTrue();
    }

    @Test
    public void taskInProgress_cancelCalled_abortableIsNotInterrupted() throws InterruptedException {
        Thread mockThread = mock(Thread.class);
        SyncTimeoutTask task = new SyncTimeoutTask(mockThread);

        AtomicBoolean interrupted = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        task.abortable(() -> {
            try {
                latch.countDown();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });

        EXEC.submit(task);

        latch.await();
        task.cancel();
        assertThat(interrupted.get()).isFalse();
    }
}
