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

package software.amazon.awssdk.core.internal.batchmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.utils.Logger;

public class ScheduledFlushTest {

    private List<String> myBuffer;
    private ScheduledExecutorService scheduledExecutor;
    private static final Logger log = Logger.loggerFor(ScheduledFlushTest.class);

    @Before
    public void setUp() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        myBuffer = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            myBuffer.add(Integer.toString(i));
        }
    }

    @After
    public void tearDown() {
        myBuffer.clear();
        scheduledExecutor.shutdownNow();
    }

    @Test
    public void executeScheduledFlush() {
        CancellableFlush flushTask = new CancellableFlush(this::flushBuffer);
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(flushTask, 0, TimeUnit.MILLISECONDS);
        ScheduledFlush scheduledFlush = new ScheduledFlush(flushTask, scheduledFuture);
        waitForTime(10);
        Assert.assertTrue(scheduledFlush.hasExecuted());
    }

    @Test
    public void cancelScheduledFlush() {
        CancellableFlush flushTask = new CancellableFlush(this::flushBuffer);
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(flushTask, 10, TimeUnit.MILLISECONDS);
        ScheduledFlush scheduledFlush = new ScheduledFlush(flushTask, scheduledFuture);
        scheduledFlush.cancel();
        Assert.assertFalse(scheduledFlush.hasExecuted());
    }

    @Test
    public void didNotCancelScheduledFlushInTime() {
        CancellableFlush flushTask = new CancellableFlush(this::flushBuffer);
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(flushTask, 10, TimeUnit.MILLISECONDS);
        ScheduledFlush scheduledFlush = new ScheduledFlush(flushTask, scheduledFuture);
        waitForTime(20);
        scheduledFlush.cancel();
        Assert.assertTrue(scheduledFlush.hasExecuted());
    }

    @Test
    public void resetCancellableFlush() {
        CancellableFlush flushTask = new CancellableFlush(this::flushBuffer);
        ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(flushTask, 0, TimeUnit.MILLISECONDS);
        ScheduledFlush scheduledFlush = new ScheduledFlush(flushTask, scheduledFuture);
        waitForTime(10);
        Assert.assertTrue(scheduledFlush.hasExecuted());

        flushTask.reset();
        Assert.assertFalse(scheduledFlush.hasExecuted());
    }

    private static boolean waitForTime(int msToWait) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            return countDownLatch.await(msToWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn(() -> String.valueOf(e));
        }
        return false;
    }

    private void flushBuffer() {
        Iterator<String> iterator = myBuffer.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }
}
