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

import static software.amazon.awssdk.utils.Validate.notNull;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;

/**
 * Implementation of {@link ClientExecutionAbortTask} that interrupts the caller thread and aborts
 * any HTTP request when triggered
 */
@SdkInternalApi
public class ClientExecutionAbortTaskImpl implements ClientExecutionAbortTask {

    private final Thread thread;
    private volatile boolean hasTaskExecuted;
    private volatile Abortable currentRequest;

    public ClientExecutionAbortTaskImpl(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void run() {
        hasTaskExecuted = true;
        if (!thread.isInterrupted()) {
            thread.interrupt();
        }
        if (currentRequest != null) {
            currentRequest.abort();
        }
    }

    @Override
    public void setCurrentHttpRequest(Abortable newRequest) {
        this.currentRequest = notNull(newRequest, "Abortable cannot be null");
    }

    public boolean hasClientExecutionAborted() {
        return hasTaskExecuted;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
