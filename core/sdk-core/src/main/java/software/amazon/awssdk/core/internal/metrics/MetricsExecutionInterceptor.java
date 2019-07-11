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

package software.amazon.awssdk.core.internal.metrics;

import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.METRIC_CONFIGURATION_PROVIDER;
import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.METRIC_PUBLISHER_CONFIGURATION;
import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.METRIC_REGISTRY;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.provider.MetricConfigurationProvider;
import software.amazon.awssdk.metrics.publisher.MetricPublisherConfiguration;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Execution Interceptor to report metrics to the registered publishers.
 */
@SdkInternalApi
public final class MetricsExecutionInterceptor implements ExecutionInterceptor {

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        System.out.println("MetricsExecutionInterceptor afterExecution");
        afterFinalApiCall(executionAttributes);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        System.out.println("MetricsExecutionInterceptor onExecutionFailure");
        afterFinalApiCall(executionAttributes);
    }

    // Handle logic to report event after finishing the request execution (including retries)
    private void afterFinalApiCall(ExecutionAttributes executionAttributes) {
        MetricRegistry registry = executionAttributes.getAttribute(METRIC_REGISTRY);
        printMetrics(registry);
        for (MetricRegistry mr : registry.apiCallAttemptMetrics()) {
            printMetrics(mr);
        }

        MetricConfigurationProvider configurationProvider = executionAttributes.getAttribute(METRIC_CONFIGURATION_PROVIDER);
        MetricPublisherConfiguration publisherConfiguration = executionAttributes.getAttribute(METRIC_PUBLISHER_CONFIGURATION);

        if (configurationProvider.enabled()) {
            publisherConfiguration.publishers().forEach(p -> p.registerMetrics(registry));
        }
    }

    private void printMetrics(MetricRegistry registry) {
        System.out.println("=====================");
        registry.getMetrics().keySet().forEach(System.out::println);
    }
}
