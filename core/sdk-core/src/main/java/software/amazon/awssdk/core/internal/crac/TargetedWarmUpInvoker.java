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

package software.amazon.awssdk.core.internal.crac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * Warms only the service clients named by {@code SdkWarmUp.prime(Class...)}. Matches each requested client class name
 * against the discovered {@link SdkWarmUpProvider}s' sync and async class names, then warms the matched transport only.
 */
@SdkInternalApi
public final class TargetedWarmUpInvoker {

    private static final Logger log = Logger.loggerFor(TargetedWarmUpInvoker.class);

    private final WarmUpServiceLoader serviceLoader;

    @SdkTestInternalApi
    TargetedWarmUpInvoker(WarmUpServiceLoader serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    public static TargetedWarmUpInvoker create() {
        return new TargetedWarmUpInvoker(WarmUpServiceLoader.INSTANCE);
    }

    /**
     * Warms the provider transport matching each requested client class name. Best-effort: an unmatched name is logged
     * at warn and skipped, and a provider that fails does not stop the others.
     *
     * @param requestedClassNames the fully qualified sync or async client class names to warm.
     * @return the transports that were matched, for narrowing the follow-on HTTP-client warm-up.
     */
    public Set<ClientType> invoke(Collection<String> requestedClassNames) {
        Set<ClientType> matchedTransports = EnumSet.noneOf(ClientType.class);
        if (requestedClassNames.isEmpty()) {
            return matchedTransports;
        }

        List<SdkWarmUpProvider> providers = loadProviders();
        for (String requested : requestedClassNames) {
            Set<ClientType> matched = warmMatching(requested, providers);
            if (matched.isEmpty()) {
                log.warn(() -> "No warm-up provider matched client class " + requested + "; skipping.");
            }
            matchedTransports.addAll(matched);
        }
        return matchedTransports;
    }

    private Set<ClientType> warmMatching(String requested, List<SdkWarmUpProvider> providers) {
        Set<ClientType> matched = EnumSet.noneOf(ClientType.class);
        for (SdkWarmUpProvider provider : providers) {
            ClientType transport = transportFor(requested, provider);
            if (transport == null) {
                continue;
            }
            matched.add(transport);
            try {
                provider.warmUpClient(transport);
            } catch (RuntimeException | LinkageError e) {
                log.warn(() -> "Warm-up failed for " + provider.getClass().getName() + " and was skipped.", e);
            }
        }
        return matched;
    }

    private ClientType transportFor(String requested, SdkWarmUpProvider provider) {
        if (requested.equals(provider.syncClientClassName())) {
            return ClientType.SYNC;
        }
        if (requested.equals(provider.asyncClientClassName())) {
            return ClientType.ASYNC;
        }
        return null;
    }

    private List<SdkWarmUpProvider> loadProviders() {
        List<SdkWarmUpProvider> providers = new ArrayList<>();
        WarmUpDiscovery.forEachDiscovered(serviceLoader.loadProviders(), providers::add);
        return providers;
    }
}
