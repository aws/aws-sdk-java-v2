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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.internal.MetricSystemSetting;

/**
 * A {@link MetricConfigurationProvider} implementation that uses system properties to configure the metrics feature.
 * <p>
 * As Java system properties can only be set during application start time or using application code,
 * it is not possible to change the behavior at runtime without code changes.
 */
@SdkProtectedApi
public final class SystemSettingsMetricConfigurationProvider implements MetricConfigurationProvider {

    private final boolean enabled;
    private final Set<MetricCategory> categories;

    private SystemSettingsMetricConfigurationProvider() {
        this.enabled = resolveEnabled();
        this.categories = resolveCategories();
    }

    public static SystemSettingsMetricConfigurationProvider create() {
        return new SystemSettingsMetricConfigurationProvider();
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public Set<MetricCategory> metricCategories() {
        return Collections.unmodifiableSet(categories);
    }

    private boolean resolveEnabled() {
        return MetricSystemSetting.AWS_JAVA_SDK_METRICS_ENABLED.getBooleanValue().orElse(false);
    }

    /**
     * @return the set of {@link MetricCategory} computed using the
     *          {@link MetricSystemSetting#AWS_JAVA_SDK_METRICS_CATEGORY} property.
     */
    private Set<MetricCategory> resolveCategories() {
        Set<MetricCategory> result = new HashSet<>();

        String category = MetricSystemSetting.AWS_JAVA_SDK_METRICS_CATEGORY
            .getStringValue()
            .orElse("default");

        String[] list = category.trim().split(",");
        for (String entry : list) {
            result.add(MetricCategory.fromString(entry));
        }

        return Collections.unmodifiableSet(result);
    }

}
