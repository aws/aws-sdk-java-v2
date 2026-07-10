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

import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.internal.crac.ClasspathWarmUpInvoker;
import software.amazon.awssdk.core.internal.crac.PrimedClientRegistry;
import software.amazon.awssdk.core.internal.crac.TargetedWarmUpInvoker;
import software.amazon.awssdk.core.internal.crac.TargetedWarmUpResult;
import software.amazon.awssdk.core.internal.http.loader.AsyncHttpClientWarmer;
import software.amazon.awssdk.core.internal.http.loader.ClasspathHttpWarmupInvoker;
import software.amazon.awssdk.core.internal.http.loader.SyncHttpClientWarmer;
import software.amazon.awssdk.utils.Logger;

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

    private static final Logger log = Logger.loggerFor(SdkWarmUp.class);

    private static final Object PRIME_LOCK = new Object();

    private static volatile boolean primed = false;

    private static final PrimedClientRegistry PRIMED_CLIENTS = new PrimedClientRegistry();

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
            ClasspathHttpWarmupInvoker.create().invokeAll();
            primed = true;
        }
    }

    /**
     * Primes only the given service clients, warming the sync path for a sync client and the async path for an async
     * client. Best-effort and safe to call concurrently; a client already primed by a previous call is skipped.
     *
     * <p>A class matched by no provider, or whose warm-up fails, is logged at warn and retried by a later call.
     *
     * @param clients the service client classes to prime, for example {@code S3Client.class} or
     *                {@code S3AsyncClient.class}.
     */
    @SafeVarargs
    public static void prime(Class<? extends SdkClient>... clients) {
        if (clients == null || clients.length == 0) {
            log.debug(() -> "SdkWarmUp.prime(Class...) called with no clients; nothing to do.");
            return;
        }

        Set<String> requested = new LinkedHashSet<>();
        for (Class<? extends SdkClient> client : clients) {
            if (client != null) {
                requested.add(client.getName());
            }
        }
        Set<String> toPrime = PRIMED_CLIENTS.selectUnprimed(requested);
        if (toPrime.isEmpty()) {
            return;
        }

        // Racing calls may double-warm the same client; warming is idempotent, so that is harmless.
        TargetedWarmUpResult result = TargetedWarmUpInvoker.create().invoke(toPrime);
        if (result.matchedTransports().contains(ClientType.SYNC)) {
            SyncHttpClientWarmer.create().warmAll();
        }
        if (result.matchedTransports().contains(ClientType.ASYNC)) {
            AsyncHttpClientWarmer.create().warmAll();
        }

        // Only successfully warmed names are recorded; unmatched or failed ones are retried on a later call.
        PRIMED_CLIENTS.markPrimed(result.warmedClientNames());
    }
}
