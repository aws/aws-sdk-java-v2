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

import static software.amazon.awssdk.metrics.internal.cloudwatch.CloudWatchMetricConfig.NAMESPACE_DELIMITER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.Dimensions;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;
import software.amazon.awssdk.util.AwsHostNameUtils;

/**
 * An internal builder used to retrieve the next batch of requests to be sent to
 * Amazon CloudWatch. Calling method {@link #nextUploadUnits()} blocks as
 * necessary.
 */
class BlockingRequestBuilder {
    private static final String OS_METRIC_NAME = MachineMetric.getOsMetricName();
    private final MachineMetricFactory machineMetricFactory = new MachineMetricFactory();
    private final BlockingQueue<MetricDatum> queue;
    private final long timeoutNano;

    BlockingRequestBuilder(CloudWatchMetricConfig config, BlockingQueue<MetricDatum> queue) {
        this.queue = queue;
        this.timeoutNano = TimeUnit.MILLISECONDS.toNanos(config.getQueuePollTimeoutMilli());
    }

    /**
     * Returns the next batch of {@link PutMetricDataRequest} to be sent to
     * Amazon CloudWatch, blocking as necessary to gather and accumulate the
     * necessary statistics. If there is no metrics data, this call blocks
     * indefinitely. If there is metrics data, this call will block up to about
     * {@link CloudWatchMetricConfig#getQueuePollTimeoutMilli()} number of
     * milliseconds.
     */
    Iterable<PutMetricDataRequest> nextUploadUnits() throws InterruptedException {
        final Map<String, MetricDatum> uniqueMetrics = new HashMap<String, MetricDatum>();
        long startNano = System.nanoTime();

        while (true) {
            final long elapsedNano = System.nanoTime() - startNano;
            if (elapsedNano >= timeoutNano) {
                return toPutMetricDataRequests(uniqueMetrics);
            }
            MetricDatum datum = queue.poll(timeoutNano - elapsedNano, TimeUnit.NANOSECONDS);
            if (datum == null) {
                // timed out
                if (uniqueMetrics.size() > 0) {
                    // return whatever we have so far
                    return toPutMetricDataRequests(uniqueMetrics);
                }
                // zero AWS related metrics
                if (AwsSdkMetrics.isMachineMetricExcluded()) {
                    // Short note: nothing to do, so just wait indefinitely.
                    // (Long note: There exists a pedagogical case where the
                    // next statement is executed followed by no subsequent AWS
                    // traffic whatsoever, and then the machine metric is enabled 
                    // via JMX.
                    // In such case, we require the metric generation to be
                    // disabled and then re-enabled (eg via JMX).
                    // So why not always wake up periodically instead of going
                    // into long wait ?
                    // I (hchar@) think we should optimize for the most typical
                    // cases instead of the edge cases. Going into long wait has
                    // the benefit of relatively less runtime footprint.)
                    datum = queue.take();
                    startNano = System.nanoTime();
                }
            }
            // Note at this point datum is null if and only if there is no
            // pending AWS related metrics but machine metrics is enabled
            if (datum != null) {
                summarize(datum, uniqueMetrics);
            }
        }
    }

    /**
     * Summarizes the given datum into the statistics of the respective unique metric.
     */
    private void summarize(MetricDatum datum, Map<String, MetricDatum> uniqueMetrics) {
        Double value = datum.value();
        if (value == null) {
            return;
        }
        List<Dimension> dims = datum.dimensions();
        Collections.sort(dims, DimensionComparator.INSTANCE);
        String metricName = datum.metricName();
        String key = metricName + dims;
        MetricDatum statDatum = uniqueMetrics.get(key);
        if (statDatum == null) {
            statDatum = MetricDatum.builder()
                    .dimensions(datum.dimensions())
                    .metricName(metricName)
                    .unit(datum.unit())
                    .statisticValues(StatisticSet.builder()
                            .maximum(value)
                            .minimum(value)
                            .sampleCount(0.0)
                            .sum(0.0)
                            .build())
                    .build()
            ;
            uniqueMetrics.put(key, statDatum);
        }
        StatisticSet stat = statDatum.statisticValues();

        StatisticSet.Builder newStatBuilder = stat.toBuilder()
                .sampleCount(stat.sampleCount() + 1.0)
                .sum(stat.sum() + value);

        if (value > stat.maximum()) {
            newStatBuilder.maximum(value);
        } else if (value < stat.minimum()) {
            newStatBuilder.minimum(value);
        }

        uniqueMetrics.put(key, statDatum.toBuilder().statisticValues(newStatBuilder.build()).build());
    }

    /**
     * Consolidates the input metrics into a list of PutMetricDataRequest, each
     * within the maximum size limit imposed by CloudWatch.
     */
    private Iterable<PutMetricDataRequest> toPutMetricDataRequests(Map<String, MetricDatum> uniqueMetrics) {
        // Opportunistically generates some machine metrics whenever there
        // is metrics consolidation
        for (MetricDatum datum : machineMetricFactory.generateMetrics()) {
            summarize(datum, uniqueMetrics);
        }
        List<PutMetricDataRequest> list = new ArrayList<PutMetricDataRequest>();
        List<MetricDatum> data = new ArrayList<MetricDatum>();
        for (MetricDatum m : uniqueMetrics.values()) {
            data.add(m);
            if (data.size() == CloudWatchMetricConfig.MAX_METRICS_DATUM_SIZE) {
                list.addAll(newPutMetricDataRequests(data));
                data.clear();
            }
        }

        if (data.size() > 0) {
            list.addAll(newPutMetricDataRequests(data));
        }
        return list;
    }

    private List<PutMetricDataRequest> newPutMetricDataRequests(Collection<MetricDatum> data) {
        List<PutMetricDataRequest> list = new ArrayList<PutMetricDataRequest>();
        final String ns = AwsSdkMetrics.getMetricNameSpace();
        PutMetricDataRequest req = newPutMetricDataRequest(data, ns);
        list.add(req);
        final boolean perHost = AwsSdkMetrics.isPerHostMetricEnabled();
        String perHostNameSpace = null;
        String hostName = null;
        Dimension hostDim = null;
        final boolean singleNamespace = AwsSdkMetrics.isSingleMetricNamespace();
        if (perHost) {
            hostName = AwsSdkMetrics.getHostMetricName();
            hostName = hostName == null ? "" : hostName.trim();
            if (hostName.length() == 0) {
                hostName = AwsHostNameUtils.localHostName();
            }
            hostDim = dimension(Dimensions.Host, hostName);
            if (singleNamespace) {
                req = newPutMetricDataRequest(data, ns, hostDim);
            } else {
                perHostNameSpace = ns + NAMESPACE_DELIMITER + hostName;
                req = newPutMetricDataRequest(data, perHostNameSpace);
            }
            list.add(req);
        }
        String jvmMetricName = AwsSdkMetrics.getJvmMetricName();
        if (jvmMetricName != null) {
            jvmMetricName = jvmMetricName.trim();
            if (jvmMetricName.length() > 0) {
                if (singleNamespace) {
                    Dimension jvmDim = dimension(Dimensions.JVM, jvmMetricName);
                    if (perHost) {
                        // If OS metrics are already included at the per host level,
                        // there is little reason, if any, to include them at the
                        // JVM level.  Hence the filtering.
                        req = newPutMetricDataRequest(
                                filterOsMetrics(data), ns, hostDim, jvmDim);
                    } else {
                        req = newPutMetricDataRequest(data, ns, jvmDim);
                    }

                } else {
                    String perJvmNameSpace = perHostNameSpace == null
                                             ? ns + NAMESPACE_DELIMITER + jvmMetricName
                                             : perHostNameSpace + NAMESPACE_DELIMITER + jvmMetricName;
                    // If OS metrics are already included at the per host level,
                    // there is little reason, if any, to include them at the
                    // JVM level.  Hence the filtering.
                    req = newPutMetricDataRequest(perHost ? filterOsMetrics(data) : data, perJvmNameSpace);
                }
                list.add(req);
            }
        }
        return list;
    }

    /**
     * Return a collection of metrics almost the same as the input except with
     * all OS metrics removed.
     */
    private Collection<MetricDatum> filterOsMetrics(Collection<MetricDatum> data) {
        Collection<MetricDatum> output = new ArrayList<MetricDatum>(data.size());
        for (MetricDatum datum : data) {
            if (!OS_METRIC_NAME.equals(datum.metricName())) {
                output.add(datum);
            }
        }
        return output;
    }

    private PutMetricDataRequest newPutMetricDataRequest(
            Collection<MetricDatum> data, final String namespace,
            final Dimension... extraDims) {
        if (extraDims != null) {
            // Need to add some extra dimensions.
            // To do so, we copy the metric data to avoid mutability problems.
            Collection<MetricDatum> newData = new ArrayList<MetricDatum>(data.size());
            for (MetricDatum md : data) {
                MetricDatum.Builder newMdBuilder = cloneMetricDatum(md).toBuilder();
                for (Dimension dim : extraDims) {
                    newMdBuilder.dimensions(dim);  // add the extra dimensions to the new metric datum
                }
                newData.add(newMdBuilder.build());
            }
            data = newData;
        }
        return PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(data)
                .build()
                .withRequestMetricCollector(RequestMetricCollector.NONE)
                ;
    }

    /**
     * Returns a metric datum cloned from the given one.
     * Made package private only for testing purposes.
     */
    final MetricDatum cloneMetricDatum(MetricDatum md) {
        return MetricDatum.builder()
                .dimensions(md.dimensions()) // a new collection is created
                .metricName(md.metricName())
                .statisticValues(md.statisticValues())
                .timestamp(md.timestamp())
                .unit(md.unit())
                .value(md.value())
                .build();
    }

    private Dimension dimension(Dimensions name, String value) {
        return Dimension.builder().name(name.toString()).value(value).build();
    }
}
