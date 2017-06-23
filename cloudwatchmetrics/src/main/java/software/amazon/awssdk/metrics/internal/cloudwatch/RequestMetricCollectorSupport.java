/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.internal.cloudwatch;

import java.util.concurrent.BlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.metrics.spi.MetricType;
import software.amazon.awssdk.metrics.spi.RequestMetricType;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;

/**
 * This is the default implementation of an AWS SDK request metric collection
 * system.
 *
 * @see RequestMetricCollector
 */
@ThreadSafe
public class RequestMetricCollectorSupport extends RequestMetricCollector {
    protected static final Log log = LogFactory.getLog(RequestMetricCollectorSupport.class);
    private final BlockingQueue<MetricDatum> queue;
    private final PredefinedMetricTransformer transformer = new PredefinedMetricTransformer();

    protected RequestMetricCollectorSupport(BlockingQueue<MetricDatum> queue) {
        this.queue = queue;
    }

    /**
     * Collects the metrics at the end of a request/response cycle, transforms
     * the metric data points into a cloud watch metric datum representation,
     * and then adds it to a memory queue so it will get summarized into the
     * necessary statistics and uploaded to Amazon CloudWatch.
     */
    @Override
    public void collectMetrics(Request<?> request, Object response) {
        try {
            collectMetrics0(request, response);
        } catch (Exception ex) { // defensive code
            if (log.isDebugEnabled()) {
                log.debug("Ignoring unexpected failure", ex);
            }
        }
    }

    private void collectMetrics0(Request<?> request, Object response) {
        AwsRequestMetrics arm = request.getAwsRequestMetrics();
        if (arm == null || !arm.isEnabled()) {
            return;
        }
        for (MetricType type : AwsSdkMetrics.getPredefinedMetrics()) {
            if (!(type instanceof RequestMetricType)) {
                continue;
            }
            PredefinedMetricTransformer transformer = getTransformer();
            for (MetricDatum datum : transformer.toMetricData(type, request, response)) {
                try {
                    if (!addMetricsToQueue(datum)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed to add to the metrics queue (due to no space available) for "
                                      + type.name()
                                      + ":"
                                      + request.getServiceName());
                        }
                    }
                } catch (RuntimeException ex) {
                    log.warn("Failed to add to the metrics queue for "
                             + type.name() + ":" + request.getServiceName(),
                             ex);
                }
            }
        }
    }

    /**
     * Adds the given metric to the queue, returning true if successful or false
     * if no space available.
     */
    protected boolean addMetricsToQueue(MetricDatum metric) {
        return queue.offer(metric);
    }

    /** Returns the predefined metrics transformer. */
    protected PredefinedMetricTransformer getTransformer() {
        return transformer;
    }
}
