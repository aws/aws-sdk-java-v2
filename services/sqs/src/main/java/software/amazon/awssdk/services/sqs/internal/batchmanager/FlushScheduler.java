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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Manages the scheduled flush tasks.
 */
@SdkInternalApi
class FlushScheduler {
    private ScheduledFuture<?> scheduledFlush;
    private final ReentrantLock schedulerLock = new ReentrantLock();

    FlushScheduler(ScheduledFuture<?> initialScheduledFlush) {
        this.scheduledFlush = initialScheduledFlush;
    }

    public void updateScheduledFlush(ScheduledFuture<?> newScheduledFlush) {
        schedulerLock.lock();
        try {
            this.scheduledFlush = newScheduledFlush;
        } finally {
            schedulerLock.unlock();
        }
    }

    public void cancelScheduledFlush() {
        schedulerLock.lock();
        try {
            if (scheduledFlush != null) {
                scheduledFlush.cancel(false);
            }
        } finally {
            schedulerLock.unlock();
        }
    }
}
