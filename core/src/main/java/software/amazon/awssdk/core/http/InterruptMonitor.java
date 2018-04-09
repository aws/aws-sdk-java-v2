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

package software.amazon.awssdk.core.http;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.http.exception.SdkInterruptedException;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * A set of utilities for monitoring the status of the currently-executing SDK thread. This is useful for periodically checking
 * whether our thread has been interrupted so that we can abort execution of a request.
 */
@SdkProtectedApi
public class InterruptMonitor {
    private InterruptMonitor() {}

    /**
     * Check if the thread has been interrupted. If so throw an {@link InterruptedException}.
     * Long running tasks should be periodically checked if the current thread has been
     * interrupted and handle it appropriately
     *
     * @throws InterruptedException If thread has been interrupted
     */
    public static void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new SdkInterruptedException();
        }
    }

    /**
     * Check if the thread has been interrupted. If so throw an {@link InterruptedException}.
     * Long running tasks should be periodically checked if the current thread has been
     * interrupted and handle it appropriately
     *
     * @param response Response to be closed before returning control to the caller to avoid
     *                 leaking the connection.
     * @throws InterruptedException If thread has been interrupted
     */
    public static void checkInterrupted(SdkHttpFullResponse response) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new SdkInterruptedException(response);
        }
    }
}
