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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;

/**
 * Interface to configure the options in metrics feature.
 *
 * This interface acts as a feature flag for metrics. The methods in the interface are called for each request.
 * This gives flexibility for metrics feature to be enabled/disabled at runtime and configuration changes
 * can be picked up at runtime without need for deploying the application (depending on the implementation).
 *
 * @see SystemSettingsMetricConfigurationProvider
 */
@SdkPublicApi
public interface MetricConfigurationProvider {

    /**
     * @return true if the metrics feature is enabled.
     *         false if the feature is disabled.
     */
    boolean enabled();

    /**
     * Return the set of {@link MetricCategory} that are enabled for metrics collection.
     * Only metrics belonging to these categories will be collected.
     */
    Set<MetricCategory> metricCategories();
}
