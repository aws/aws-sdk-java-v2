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

import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.internal.crac.ClasspathWarmUpInvoker;

/**
 * Entry point for warming up SDK service request paths before a Coordinated Restore at Checkpoint (CRaC)
 * checkpoint.
 *
 * <p>{@link #prime()} discovers every {@link SdkWarmUpProvider} registered on the classpath through {@link
 * ServiceLoader} (via the {@code META-INF/services/software.amazon.awssdk.core.crac.SdkWarmUpProvider}
 * resource) and invokes {@link SdkWarmUpProvider#warmUp()} on each.
 *
 * <p>Behavior contract:
 * <ul>
 *     <li><b>Idempotent:</b> {@code prime()} runs the warm-up at most once per JVM. Once a call completes
 *     successfully, later calls return immediately. If a call throws before completing, a later call retries.
 *     Concurrent callers block until the in-flight call finishes, then observe its result.</li>
 *     <li><b>Per-provider resilience:</b> a single provider that throws from {@code warmUp()}, or that fails
 *     to load, does not prevent the remaining providers from running.</li>
 *     <li><b>Safe when empty:</b> if no providers are registered, {@code prime()} is a no-op.</li>
 * </ul>
 *
 * <p>Call this once during application initialization, before a CRaC checkpoint is taken.
 */
@ThreadSafe
@SdkPublicApi
public final class SdkWarmUp {

    private static final Object PRIME_LOCK = new Object();

    private static volatile boolean primed = false;

    private SdkWarmUp() {
    }

    /**
     * Discovers every {@link SdkWarmUpProvider} on the classpath and invokes {@link SdkWarmUpProvider#warmUp()}
     * on each, honoring the idempotency, per-provider resilience, and empty-classpath behavior described on
     * this class. Safe to call concurrently.
     */
    public static void prime() {
        if (primed) {
            return;
        }
        synchronized (PRIME_LOCK) {
            if (primed) {
                return;
            }
            // Set primed only after invokeAll() succeeds, so a failed run leaves primed false and a later call retries.
            ClasspathWarmUpInvoker.create().invokeAll();
            primed = true;
        }
    }
}
