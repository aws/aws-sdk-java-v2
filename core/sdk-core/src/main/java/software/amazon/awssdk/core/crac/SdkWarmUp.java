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

import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.internal.crac.ClasspathWarmUpInvoker;
import software.amazon.awssdk.core.internal.http.loader.HttpClientWarmer;
import software.amazon.awssdk.core.internal.http.loader.SyncHttpClientWarmer;

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
 *     <li><b>Idempotent:</b> runs at most once per JVM. Concurrent callers block until the in-flight call finishes; a call
 *     that throws before completing lets a later call retry.</li>
 *     <li><b>Resilient:</b> one provider that throws or fails to load does not stop the others.</li>
 *     <li><b>Safe when empty:</b> a no-op if nothing is registered.</li>
 * </ul>
 *
 * <p>{@code prime()} also fires a best-effort {@code GET} per sync HTTP client at a regional AWS endpoint to JIT-compile the
 * HTTP, DNS, TLS, and cert-chain paths. This needs network connectivity during init; if unavailable, the failure is swallowed.
 *
 * <p>Call this once during initialization, before a CRaC checkpoint is taken.
 */
@ThreadSafe
@SdkPublicApi
public final class SdkWarmUp {

    private static final Object PRIME_LOCK = new Object();

    // The HTTP-client warmers invoked by prime(), one per transport kind. The async warmer is added here when implemented.
    private static final List<HttpClientWarmer> HTTP_CLIENT_WARMERS = Arrays.asList(SyncHttpClientWarmer.create());

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
            // Set primed only after warm-up succeeds, so a failed run leaves primed false and a later call retries.
            ClasspathWarmUpInvoker.create().invokeAll();
            for (HttpClientWarmer warmer : HTTP_CLIENT_WARMERS) {
                warmer.warmAll();
            }
            primed = true;
        }
    }
}
