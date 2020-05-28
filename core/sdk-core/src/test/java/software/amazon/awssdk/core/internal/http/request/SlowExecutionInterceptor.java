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

package software.amazon.awssdk.core.internal.http.request;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Implementation of {@link ExecutionInterceptor} with configurable wait times
 */
public class SlowExecutionInterceptor implements ExecutionInterceptor {

    private int beforeTransmissionWait;
    private int afterTransmissionWait;
    private int onExecutionFailureWait;

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        wait(beforeTransmissionWait);
    }

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
        wait(afterTransmissionWait);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        wait(onExecutionFailureWait);
    }

    private void wait(int secondsToWait) {
        if (secondsToWait > 0) {
            try {
                Thread.sleep(secondsToWait * 1000);
            } catch (InterruptedException e) {
                // Be a good citizen an re-interrupt the current thread
                Thread.currentThread().interrupt();
            }
        }
    }

    public SlowExecutionInterceptor beforeTransmissionWaitInSeconds(int beforeTransmissionWait) {
        this.beforeTransmissionWait = beforeTransmissionWait;
        return this;
    }

    public SlowExecutionInterceptor afterTransmissionWaitInSeconds(int afterTransmissionWait) {
        this.afterTransmissionWait = afterTransmissionWait;
        return this;
    }

    public SlowExecutionInterceptor onExecutionFailureWaitInSeconds(int onExecutionFailureWait) {
        this.onExecutionFailureWait = onExecutionFailureWait;
        return this;
    }

}
