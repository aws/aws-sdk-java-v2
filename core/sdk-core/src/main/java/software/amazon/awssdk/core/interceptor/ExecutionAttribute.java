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

package software.amazon.awssdk.core.interceptor;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An attribute attached to a particular execution, stored in {@link ExecutionAttributes}.
 *
 * This is typically used as a static final field in an {@link ExecutionInterceptor}:
 * <pre>
 * {@code
 * class MyExecutionInterceptor implements ExecutionInterceptor {
 *     private static final ExecutionAttribute<String> DATA = new ExecutionAttribute<>();
 *
 *     @Override
 *     public void beforeExecution(Context.BeforeExecution execution, ExecutionAttributes executionAttributes) {
 *         executionAttributes.put(DATA, "Request: " + execution.request());
 *     }
 *
 *     @Override
 *     public void afterExecution(Context.AfterExecution execution, ExecutionAttributes executionAttributes) {
 *         String data = executionAttributes.get(DATA); // Retrieve the value saved in beforeExecution.
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T> The type of data associated with this attribute.
 */
@SdkPublicApi
public class ExecutionAttribute<T> {

    private final String name;

    /**
     * Creates a new {@link ExecutionAttribute} bound to the provided type param.
     *
     * @param name Descriptive name for the attribute, used primarily for debugging purposes.
     */
    public ExecutionAttribute(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
