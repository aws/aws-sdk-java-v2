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

package software.amazon.awssdk.global.handlers;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

public class TestGlobalExecutionInterceptor implements ExecutionInterceptor {

    private static boolean wasCalled = false;

    public static void reset() {
        wasCalled = false;
    }

    public static boolean wasCalled() {
        return wasCalled;
    }

    @Override
    public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
        wasCalled = true;
    }
}
