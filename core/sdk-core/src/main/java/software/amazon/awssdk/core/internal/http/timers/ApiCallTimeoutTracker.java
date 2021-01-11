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

import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Validate;

/**
 * Api Call Timeout Tracker to track the {@link TimeoutTask} and the {@link ScheduledFuture}.
 */
@SdkInternalApi
public final class ApiCallTimeoutTracker implements TimeoutTracker {

    private final TimeoutTask timeoutTask;

    private final ScheduledFuture<?> future;

    public ApiCallTimeoutTracker(TimeoutTask timeout, ScheduledFuture<?> future) {
        this.timeoutTask = Validate.paramNotNull(timeout, "timeoutTask");
        this.future = Validate.paramNotNull(future, "scheduledFuture");
    }

    @Override
    public boolean hasExecuted() {
        return timeoutTask.hasExecuted();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void cancel() {
        // Best-effort attempt to ensure that if the future hasn't started running already, don't run it.
        future.cancel(false);
        // Ensure that if the future hasn't executed its timeout logic already, it won't do so.
        timeoutTask.cancel();
    }

    @Override
    public void abortable(Abortable abortable) {
        timeoutTask.abortable(abortable);
    }
}
