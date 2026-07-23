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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * Warms only the service clients named by {@code SdkWarmUp.prime(Class...)}. Matches each requested client class name
 * against the discovered {@link SdkWarmUpProvider}s' sync and async class names, then warms the matched client type only.
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
     * Warms the provider client type matching each requested client class name. An unmatched name is logged
     * at warn and skipped, and a provider that fails does not stop the others.
     *
     * @param requestedClassNames the fully qualified sync or async client class names to warm.
     * @return the matched client types and the names that warmed successfully.
     */
    public TargetedWarmUpResult invoke(Collection<String> requestedClassNames) {
        Set<ClientType> matchedClientTypes = EnumSet.noneOf(ClientType.class);
        Set<String> warmedClientNames = new LinkedHashSet<>();
        if (requestedClassNames.isEmpty()) {
            return new TargetedWarmUpResult(matchedClientTypes, warmedClientNames);
        }

        List<SdkWarmUpProvider> providers = loadProviders();
        for (String requested : requestedClassNames) {
            boolean warmFailed = false;
            Set<ClientType> matched = EnumSet.noneOf(ClientType.class);
            for (SdkWarmUpProvider provider : providers) {
                ClientType clientType = clientTypeFor(requested, provider);
                if (clientType == null) {
                    continue;
                }
                matched.add(clientType);
                if (!WarmUpDiscovery.runSafely(provider.getClass().getName(),
                                               () -> provider.warmUpClient(clientType))) {
                    warmFailed = true;
                }
            }
            if (matched.isEmpty()) {
                log.warn(() -> "No warm-up provider matched client class " + requested + "; skipping.");
            } else if (!warmFailed) {
                warmedClientNames.add(requested);
            }
            matchedClientTypes.addAll(matched);
        }
        return new TargetedWarmUpResult(matchedClientTypes, warmedClientNames);
    }

    private ClientType clientTypeFor(String requested, SdkWarmUpProvider provider) {
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
