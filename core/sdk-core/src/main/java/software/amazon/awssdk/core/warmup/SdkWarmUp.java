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

package software.amazon.awssdk.core.warmup;

import java.util.LinkedHashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.internal.http.loader.AsyncHttpClientWarmer;
import software.amazon.awssdk.core.internal.http.loader.ClasspathHttpWarmupInvoker;
import software.amazon.awssdk.core.internal.http.loader.SyncHttpClientWarmer;
import software.amazon.awssdk.core.internal.warmup.ClasspathWarmUpInvoker;
import software.amazon.awssdk.core.internal.warmup.PrimedClientRegistry;
import software.amazon.awssdk.core.internal.warmup.TargetedWarmUpInvoker;
import software.amazon.awssdk.core.internal.warmup.TargetedWarmUpResult;
import software.amazon.awssdk.core.internal.warmup.WarmedHttpClientTypeRegistry;
import software.amazon.awssdk.utils.Logger;

/**
 * Entry point for warming up SDK service request paths before a Coordinated Restore at Checkpoint (CRaC)
 * checkpoint.
 *
 * <p>Warms up the service clients and the HTTP clients, as below. Neither needs AWS credentials:
 * <ul>
 *     <li><b>Service client warm-up:</b> for each service, builds a client on a stub HTTP client that returns
 *     a fixed in-memory response and invokes one operation. This makes no network call, and JIT-compiles the
 *     marshalling, signing, and unmarshalling paths.</li>
 *     <li><b>HTTP client warm-up:</b> for each HTTP client on the classpath, sends one {@code GET} to the
 *     regional STS endpoint ({@code https://sts.<region>.amazonaws.com/}) to JIT-compile the HTTP client, DNS,
 *     TLS handshake, and certificate-chain paths. The response is discarded and any failure is ignored.</li>
 * </ul>
 *
 * <p>Behavior contract:
 * <ul>
 *     <li><b>Idempotent:</b> {@code warmUp()} runs the warm-up at most once per JVM. Once a call completes
 *     successfully, later calls return immediately. If a call throws before completing, a later call retries.
 *     Concurrent callers block until the in-flight call finishes, then observe its result.</li>
 *     <li><b>Per-provider resilience:</b> a single provider that throws from {@code warmUp()}, or that fails
 *     to load, does not prevent the remaining providers from running.</li>
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

    // Tracks per-service work already done, keyed by service-client class name.
    private static final PrimedClientRegistry PRIMED_CLIENTS = new PrimedClientRegistry();

    // Tracks the service-independent HTTP-layer warm-up, keyed by client type, so it runs at most once per type.
    private static final WarmedHttpClientTypeRegistry WARMED_HTTP_CLIENT_TYPES = new WarmedHttpClientTypeRegistry();

    private SdkWarmUp() {
    }

    /**
     * Warms every SDK service client and every HTTP client discovered on the classpath. Honors the
     * idempotency and per-provider resilience described on this class.
     *
     * <p>Refer to the {@link SdkWarmUp class documentation} for the service warm-up and the HTTP-client
     * warm-up it performs.
     *
     * <p>This method and {@link #warmUp(Class[])} track primed state independently: this method warms
     * every provider and does not skip clients that were warmed by a targeted {@link #warmUp(Class[])} call.
     */
    public static void warmUp() {
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
     * Warms only the given service clients: a sync client class warms the sync path, an async client class the
     * async path. A client already warmed by this method is skipped; a class that matches no service client, or
     * whose warm-up fails, is logged at warn and retried on the next call. Best-effort and safe to call
     * concurrently.
     *
     * <p>The HTTP clients on the classpath are warmed at most once per client type across all calls to this
     * method: the sync HTTP clients on the first call that warms a sync client, and the async HTTP clients on
     * the first call that warms an async client.
     *
     * <p>Needs no AWS credentials. Refer to the {@link SdkWarmUp class documentation} for what the service
     * warm-up and the HTTP-client warm-up each do.
     *
     * <p>This method and {@link #warmUp()} track warmed state independently: this method does not skip
     * clients that {@link #warmUp()} already warmed.
     *
     * @param clients the service client classes to warm, for example {@code ServiceClient.class} or
     *                {@code ServiceAsyncClient.class}.
     */
    @SafeVarargs
    public static void warmUp(Class<? extends SdkClient>... clients) {
        if (clients == null || clients.length == 0) {
            log.debug(() -> "SdkWarmUp.warmUp(Class...) called with no clients; nothing to do.");
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
        warmHttpClientsOnce(result.matchedClientTypes());

        // Only successfully warmed names are recorded; unmatched or failed ones are retried on a later call.
        PRIMED_CLIENTS.markPrimed(result.warmedClientNames());
    }

    /**
     * Warms the HTTP clients for the given client types, skipping types already warmed by an earlier call.
     */
    private static void warmHttpClientsOnce(Set<ClientType> matchedClientTypes) {
        if (matchedClientTypes.contains(ClientType.SYNC) && !WARMED_HTTP_CLIENT_TYPES.isWarmed(ClientType.SYNC)) {
            SyncHttpClientWarmer.create().warmAll();
            WARMED_HTTP_CLIENT_TYPES.markWarmed(ClientType.SYNC);
        }
        if (matchedClientTypes.contains(ClientType.ASYNC) && !WARMED_HTTP_CLIENT_TYPES.isWarmed(ClientType.ASYNC)) {
            AsyncHttpClientWarmer.create().warmAll();
            WARMED_HTTP_CLIENT_TYPES.markWarmed(ClientType.ASYNC);
        }
    }
}