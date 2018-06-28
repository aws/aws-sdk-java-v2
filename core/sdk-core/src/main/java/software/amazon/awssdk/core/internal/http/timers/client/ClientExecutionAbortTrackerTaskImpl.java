/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal.http.timers.client;

import static software.amazon.awssdk.core.util.ValidationUtils.assertNotNull;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;

/**
 * Keeps track of the scheduled {@link ClientExecutionAbortTask} and the associated {@link Future}
 */
@SdkInternalApi
public class ClientExecutionAbortTrackerTaskImpl implements ClientExecutionAbortTrackerTask {

    private final ClientExecutionAbortTask task;
    private final ScheduledFuture<?> future;

    public ClientExecutionAbortTrackerTaskImpl(final ClientExecutionAbortTask task, final ScheduledFuture<?> future) {
        this.task = assertNotNull(task, "task");
        this.future = assertNotNull(future, "future");
    }

    @Override
    public void setCurrentHttpRequest(Abortable newRequest) {
        task.setCurrentHttpRequest(newRequest);
    }

    @Override
    public boolean hasTimeoutExpired() {
        return task.hasClientExecutionAborted();
    }

    @Override
    public boolean isEnabled() {
        return task.isEnabled();
    }

    @Override
    public void cancelTask() {
        // Ensure task is canceled even if it's running as we don't want the Thread to be
        // interrupted in the caller's code
        future.cancel(false);
    }
}
