/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    private Abortable abortable;

    SyncTimeoutTask(Thread threadToInterrupt) {
        this.threadToInterrupt = Validate.paramNotNull(threadToInterrupt, "threadToInterrupt");
    }

    @Override
    public void abortable(Abortable abortable) {
        this.abortable = abortable;
    }

    @Override
    public void run() {
        hasExecuted = true;
        threadToInterrupt.interrupt();

        if (abortable != null) {
            abortable.abort();
        }
    }

    @Override
    public boolean hasExecuted() {
        return hasExecuted;
    }
}
