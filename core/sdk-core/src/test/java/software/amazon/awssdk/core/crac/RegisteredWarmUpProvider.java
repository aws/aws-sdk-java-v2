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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.core.ClientType;

/**
 * Test-only {@link SdkWarmUpProvider} registered in test-scoped {@code META-INF/services} so a real {@link
 * java.util.ServiceLoader} discovers and instantiates it by name. Both static fields are static because the loader
 * builds its own instance. Must be public with a no-arg constructor for ServiceLoader.
 *
 * <p>{@code INVOCATIONS} counts full {@link #warmUp()} calls; {@code WARMED_CLIENTS} records the client types passed to
 * {@link #warmUpClient(ClientType)}, so a test can tell the targeted path from the full one.
 */
public final class RegisteredWarmUpProvider implements SdkWarmUpProvider {

    public static final AtomicInteger INVOCATIONS = new AtomicInteger();

    public static final Set<ClientType> WARMED_CLIENTS =
        Collections.synchronizedSet(EnumSet.noneOf(ClientType.class));

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
        WARMED_CLIENTS.add(clientType);
    }
}
