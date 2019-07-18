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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchPublisher;
import software.amazon.awssdk.metrics.registry.MetricRegistry;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Helper class to convert metrics in {@link MetricRegistry} instances into {@link MetricDatum}s and
 * add them to the queue used by {@link CloudWatchPublisher}.
 */
@SdkInternalApi
public class Producer {
    private static final Logger log = Logger.loggerFor(Producer.class);

    private final BlockingQueue<MetricDatum> queue;
    private final MetricTransformer metricTransformer;

    private Producer(Builder builder) {
        this.queue = Validate.notNull(builder.queue, "Queue cannot be null");
        this.metricTransformer = builder.metricTransformer != null ? builder.metricTransformer
                                                                   : MetricTransformer.getInstance();
    }

    /**
     * Add the metrics (both top-level and perAttempt metrics) to the {@link #queue}.
     * @param metricRegistry
     */
    public void addMetrics(MetricRegistry metricRegistry) {
        List<MetricDatum> results = new ArrayList<>();

        results.addAll(metricTransformer.transform(metricRegistry));

        for (MetricRegistry mr : metricRegistry.apiCallAttemptMetrics()) {
            results.addAll(metricTransformer.transform(mr));
        }

        try {
            queue.addAll(results);
        } catch (IllegalStateException exception) {
            log.debug(() -> "Due to capacity restrictions, some items from the metric registry are not added to the queue.",
                      exception);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to create instances of {@link Producer}
     */
    public static final class Builder {

        private BlockingQueue<MetricDatum> queue;
        private MetricTransformer metricTransformer;

        /**
         * Sets the blocking queue to add the metrics to.
         */
        public Builder queue(BlockingQueue<MetricDatum> queue) {
            this.queue = queue;
            return this;
        }

        /**
         * Sets the {@link MetricTransformer} to transform metrics into {@link MetricDatum}
         */
        public Builder metricTransformer(MetricTransformer metricTransformer) {
            this.metricTransformer = metricTransformer;
            return this;
        }

        public Producer build() {
            return new Producer(this);
        }
    }
}
