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
 * Dummy implementation of {@link ClientExecutionAbortTrackerTask} used when the timer is disabled
 * for a request
 */
@SdkInternalApi
public final class NoOpClientExecutionAbortTrackerTask implements ClientExecutionAbortTrackerTask {

    public static final NoOpClientExecutionAbortTrackerTask INSTANCE = new NoOpClientExecutionAbortTrackerTask();

    // Singleton
    private NoOpClientExecutionAbortTrackerTask() {
    }

    @Override
    public void setCurrentHttpRequest(Abortable newRequest) {
    }

    @Override
    public boolean hasTimeoutExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void cancelTask() {
    }

}
