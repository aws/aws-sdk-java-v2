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

package software.amazon.awssdk.observability.micrometer;

import io.micrometer.observation.ObservationRegistry;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.observability.micrometer.internal.MicrometerExecutionInterceptor;

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


/**
 * SDK plugin that integrates AWS SDK metrics with Micrometer's Observation API.
 */
public class MicrometerPlugin implements SdkPlugin {

    private final ObservationRegistry observationRegistry;

    /**
     * Constructs a new MicrometerPlugin with the specified observation registry.
     *
     * @param observationRegistry The Micrometer observation registry to use for metrics
     */
    public MicrometerPlugin(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        config.overrideConfiguration(b -> b
            .addMetricPublisher(new NoOpMetricPublisher())
            .addExecutionInterceptor(MicrometerExecutionInterceptor.builder()
                                         .observationRegistry(observationRegistry)
                                         .captureRequestHeader("bucket")
                                         .captureResponseHeader("eTag")
                                                                   .build()));
    }

    /**
     * A no-operation implementation of MetricPublisher. This is used to ensure the SDK collects metrics that can be captured by
     * the interceptor.
     */
    private static class NoOpMetricPublisher implements MetricPublisher {
        @Override
        public void publish(MetricCollection metricCollection) {
        }

        @Override
        public void close() {
        }
    }
}
