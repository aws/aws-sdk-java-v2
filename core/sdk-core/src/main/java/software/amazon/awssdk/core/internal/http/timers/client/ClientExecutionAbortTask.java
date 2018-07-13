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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;

/**
 * Task to be scheduled by {@link ClientExecutionTimer}
 */
@SdkInternalApi
public interface ClientExecutionAbortTask extends Runnable {

    /**
     * Client Execution timer task needs to abort the current running HTTP request when executed.
     */
    void setCurrentHttpRequest(Abortable newRequest);

    /**
     * @return True if client execution has been aborted by the timer task. False otherwise
     */
    boolean hasClientExecutionAborted();

    /**
     * @return True if the timer task has been scheduled. False if client execution timeout is
     *         disabled for this request
     */
    boolean isEnabled();
}
