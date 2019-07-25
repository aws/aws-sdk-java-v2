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

import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.ATTEMPT_METRIC_REGISTRY;
import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.METRIC_REGISTRY;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.MetricUtils;
import software.amazon.awssdk.metrics.metrics.SdkDefaultMetric;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Utilities used for handling shared metric tasks during the request lifecycle.
 */
@SdkInternalApi
public final class MetricUtil {

    private MetricUtil() {
    }

    public static MetricRegistry newRegistry(ExecutionAttributes executionAttributes) {
        MetricRegistry apiCallMR = executionAttributes.getAttribute(METRIC_REGISTRY);
        MetricUtils.counter(apiCallMR, SdkDefaultMetric.ApiCallAttemptCount)
                   .increment();

        // From now on, downstream calls should use this attempt metric registry to record metrics
        MetricRegistry attemptMR = apiCallMR.registerApiCallAttemptMetrics();
        executionAttributes.putAttribute(ATTEMPT_METRIC_REGISTRY, attemptMR);
        return attemptMR;
    }
}
