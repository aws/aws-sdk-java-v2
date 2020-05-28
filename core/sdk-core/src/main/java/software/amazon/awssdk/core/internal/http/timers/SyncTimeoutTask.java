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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link TimeoutTask} to be scheduled for synchronous operations.
 */
@SdkInternalApi
public final class SyncTimeoutTask implements TimeoutTask {
    private final Thread threadToInterrupt;
    private volatile boolean hasExecuted;
    private volatile boolean isCancelled;

    // Synchronize calls to run(), cancel(), and hasExecuted().
    private final Object lock = new Object();

    private Abortable abortable;

    SyncTimeoutTask(Thread threadToInterrupt) {
        this.threadToInterrupt = Validate.paramNotNull(threadToInterrupt, "threadToInterrupt");
    }

    @Override
    public void abortable(Abortable abortable) {
        this.abortable = abortable;
    }

    /**
     * Runs this task. If cancel() was called prior to this invocation, has no side effects. Otherwise, behaves with the
     * following post-conditions: (1) threadToInterrupt's interrupt flag is set to true (unless a concurrent process
     * clears it); (2) hasExecuted() will return true.
     *
     * Note that run(), cancel(), and hasExecuted() behave atomically - calls to these methods operate with strict
     * happens-before relationships to one another.
     */
    @Override
    public void run() {
        synchronized (this.lock) {
            if (isCancelled) {
                return;
            }
            hasExecuted = true;
            threadToInterrupt.interrupt();

            if (abortable != null) {
                abortable.abort();
            }
        }
    }

    /**
     * Cancels this task. Once this returns, it's guaranteed that hasExecuted() will not change its value, and that this
     * task won't interrupt the threadToInterrupt this task was created with.
     */
    @Override
    public void cancel() {
        synchronized (this.lock) {
            isCancelled = true;
        }
    }

    /**
     * Returns whether this task has finished executing its timeout behavior. The interrupt flag set by this task will
     * only be set if hasExecuted() returns true, and is guaranteed not to be set at the time hasExecuted() returns
     * false.
     */
    @Override
    public boolean hasExecuted() {
        synchronized (this.lock) {
            return hasExecuted;
        }
    }
}
