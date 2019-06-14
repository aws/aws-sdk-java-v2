/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.MetricCategory;

/**
 * Composite {@link MetricConfigurationProvider} that lets you specify a list of providers to get metric configuration.
 *
 * This chain decides the {@link MetricConfigurationProvider} to use at object construction time and cannot be changed
 * later. The resolution policy is as follows:
 *
 * 1) Use the first provider in the chain that has returns true in {@link MetricConfigurationProvider#enabled()} method.
 *
 * 2) If none of the providers has metrics enabled as mentioned in above step, then use the first provider in the chain.
 */
@SdkProtectedApi
public class MetricConfigurationProviderChain implements MetricConfigurationProvider {

    private static final Logger log = LoggerFactory.getLogger(MetricConfigurationProviderChain.class);

    private final List<MetricConfigurationProvider> providers;

    private final MetricConfigurationProvider resolvedProvider;

    public MetricConfigurationProviderChain(MetricConfigurationProvider... providers) {
        this.providers = new ArrayList<>(providers.length);
        Collections.addAll(this.providers, providers);

        this.resolvedProvider = resolve();
    }

    /**
     * Determine the provider to use from the chain. This is determined at object construction time and cannot
     * be changed later.
     */
    private MetricConfigurationProvider resolve() {
        for (MetricConfigurationProvider provider : providers) {
            try {
                if (provider.enabled()) {
                    return provider;
                }
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                if (log.isDebugEnabled()) {
                    log.debug("Metrics are not enabled in {}:{}", provider.toString(), e.getMessage());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Metrics are not enabled in any of the providers in the chain. So using the first provider in"
                      + " the chain: ", providers.get(0).toString());
        }

        return providers.get(0);
    }

    @Override
    public boolean enabled() {
        return resolvedProvider.enabled();
    }

    @Override
    public Set<MetricCategory> metricCategories() {
        return resolvedProvider.metricCategories();
    }
}
