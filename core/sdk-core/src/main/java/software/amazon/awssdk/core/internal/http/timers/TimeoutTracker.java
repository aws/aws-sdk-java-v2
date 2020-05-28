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

/**
 * Tracker task to track the {@link TimeoutTask} and the {@link ScheduledFuture} that
 * schedules the timeout task.
 */
@SdkInternalApi
public interface TimeoutTracker {

    /**
     * @return True if timeout task has been executed. False otherwise
     */
    boolean hasExecuted();

    /**
     * @return True if the timer task has been scheduled. False if the timeout is
     *         disabled for this request
     */
    boolean isEnabled();

    /**
     * cancel the {@link ScheduledFuture}
     */
    void cancel();

    /**
     * Sets the abortable task to be aborted by {@link TimeoutTask}
     *
     * @param abortable the abortable task
     */
    void abortable(Abortable abortable);
}
