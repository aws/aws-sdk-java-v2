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

package software.amazon.awssdk.core.crac;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only {@link SdkWarmUpProvider} registered via test-scoped {@code META-INF/services} so the real static
 * {@code SdkWarmUp.prime()} discovers and invokes it through {@link java.util.ServiceLoader}. It counts how many
 * times {@code warmUp()} is invoked across the JVM. ServiceLoader requires a public no-arg constructor.
 */
public final class CountingWarmUpProvider implements SdkWarmUpProvider {

    public static final AtomicInteger INVOCATIONS = new AtomicInteger();

    @Override
    public void warmUp() {
        INVOCATIONS.incrementAndGet();
    }
}
