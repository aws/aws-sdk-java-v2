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
import software.amazon.awssdk.core.ClientType;

/**
 * Test-only {@link SdkWarmUpProvider} registered in test-scoped {@code META-INF/services} so a real {@link
 * java.util.ServiceLoader} discovers and instantiates it by name. {@code INVOCATIONS} is static because the loader
 * builds its own instance. Must be public with a no-arg constructor for ServiceLoader.
 */
public final class RegisteredWarmUpProvider implements SdkWarmUpProvider {

    public static final AtomicInteger INVOCATIONS = new AtomicInteger();

    @Override
    public void warmUp() {
        INVOCATIONS.incrementAndGet();
    }

    @Override
    public String syncClientClassName() {
        return "software.amazon.awssdk.core.crac.RegisteredSyncClient";
    }

    @Override
    public String asyncClientClassName() {
        return "software.amazon.awssdk.core.crac.RegisteredAsyncClient";
    }

    @Override
    public void warmUpClient(ClientType clientType) {
    }
}
