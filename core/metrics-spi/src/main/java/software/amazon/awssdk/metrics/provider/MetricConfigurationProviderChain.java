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

package software.amazon.awssdk.metrics.provider;

import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Composite {@link MetricConfigurationProvider} that lets you specify a list of providers to get metric configuration.
 * <p>
 * This chain decides the {@link MetricConfigurationProvider} to use at object construction time and cannot be changed
 * later. The resolution policy is as follows:
 *
 * 1) Use the first provider in the chain that has returns true in {@link MetricConfigurationProvider#enabled()} method.
 *
 * 2) If none of the providers has metrics enabled as mentioned in above step, then use the first provider in the chain.
 */
@SdkProtectedApi
public class MetricConfigurationProviderChain implements MetricConfigurationProvider {
    private static final Logger log = Logger.loggerFor(MetricConfigurationProviderChain.class);

    private final MetricConfigurationProvider resolvedProvider;

    public MetricConfigurationProviderChain(MetricConfigurationProvider... providers) {
        Validate.isPositive(providers.length, "Atleast one provider should be set");
        this.resolvedProvider = resolve(providers);
    }

    /**
     * Determine the provider to use from the chain. This is determined at object construction time and cannot
     * be changed later.
     */
    private MetricConfigurationProvider resolve(MetricConfigurationProvider... providers) {
        for (MetricConfigurationProvider provider : providers) {
            try {
                if (provider.enabled()) {
                    return provider;
                }
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                log.debug(() -> "Metrics are not enabled in " + provider.toString(), e);
            }
        }

        log.debug(() -> "Metrics are not enabled in any of the providers in the chain. "
                        + "So using the first provider in the chain: " +  providers[0]);

        return providers[0];
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
