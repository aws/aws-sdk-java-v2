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

package software.amazon.awssdk.crac;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

/**
 * Records invoked operation names. Registered as a global interceptor via
 * {@code software/amazon/awssdk/global/handlers/execution.interceptors} so tests can observe calls made by clients
 * they cannot configure, such as the client a generated {@code SdkWarmUpProvider} builds internally.
 */
public class OperationRecordingInterceptor implements ExecutionInterceptor {

    private static final List<String> OPERATION_NAMES = new CopyOnWriteArrayList<>();

    public static List<String> operationNames() {
        return OPERATION_NAMES;
    }

    public static void reset() {
        OPERATION_NAMES.clear();
    }

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        OPERATION_NAMES.add(executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME));
    }
}
