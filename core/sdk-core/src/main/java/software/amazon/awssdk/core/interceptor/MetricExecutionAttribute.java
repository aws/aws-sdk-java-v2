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

package software.amazon.awssdk.core.interceptor;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.provider.MetricConfigurationProvider;
import software.amazon.awssdk.metrics.publisher.MetricPublisherConfiguration;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Contains attributes related to the metrics feature. This information is used to determine the metrics behavior.
 *
 * Only SDK should set these values
 */
@SdkProtectedApi
public final class MetricExecutionAttribute {

    /**
     * The key to store the {@link MetricRegistry} for the ApiCall execution
     *
     * @see MetricRegistry
     */
    public static final ExecutionAttribute<MetricRegistry> METRIC_REGISTRY = new ExecutionAttribute<>("MetricRegistry");

    /**
     * The key to store the {@link MetricRegistry} for an ApiCall Attempt
     *
     * @see MetricRegistry
     */
    public static final ExecutionAttribute<MetricRegistry> ATTEMPT_METRIC_REGISTRY =
        new ExecutionAttribute<>("AttemptMetricRegistry");

    /**
     * The key to store the {@link MetricConfigurationProvider}
     *
     * @see MetricConfigurationProvider
     */
    public static final ExecutionAttribute<MetricConfigurationProvider> METRIC_CONFIGURATION_PROVIDER =
        new ExecutionAttribute<>("MetricConfigurationProvider");

    /**
     * The key to store the {@link MetricPublisherConfiguration}
     *
     * @see MetricPublisherConfiguration
     */
    public static final ExecutionAttribute<MetricPublisherConfiguration> METRIC_PUBLISHER_CONFIGURATION =
        new ExecutionAttribute<>("MetricPublisherConfiguration");

    private MetricExecutionAttribute() {
    }
}
