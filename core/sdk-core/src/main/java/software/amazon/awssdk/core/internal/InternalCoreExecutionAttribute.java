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

package software.amazon.awssdk.core.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

/**
 * Attributes that can be applied to all sdk requests. These attributes are only used internally by the core to
 * handle executions through the pipeline.
 */
@SdkInternalApi
public final class InternalCoreExecutionAttribute extends SdkExecutionAttribute {
    /**
     * The key to store the execution attempt number that is used by handlers in the async request pipeline to help
     * regulate their behavior.
     */
    public static final ExecutionAttribute<Integer> EXECUTION_ATTEMPT =
        new ExecutionAttribute<>("SdkInternalExecutionAttempt");

    private InternalCoreExecutionAttribute() {
    }
}
