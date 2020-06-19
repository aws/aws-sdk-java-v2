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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform.DetailedMetricAggregator.DetailedMetrics;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

/**
 * Aggregates {@link MetricCollection}s by: (1) the minute in which they occurred, and (2) the dimensions in the collection
 * associated with that metric. Allows retrieving the aggregated values as a list of {@link PutMetricDataRequest}s.
 *
 * <p>It would be too expensive to upload every {@code MetricCollection} as a unique {@code PutMetricDataRequest}, so this
 * class aggregates the data so that multiple {@code MetricCollection}s can be placed in the same {@code PutMetricDataRequest}.
 *
 * <p><b>Warning:</b> This class is *not* thread-safe.
 */
@SdkInternalApi
@NotThreadSafe
public class MetricCollectionAggregator {
    /**
     * The maximum number of {@link MetricDatum}s allowed in {@link PutMetricDataRequest#metricData()}. This limit is imposed by
     * CloudWatch.
     */
    public static final int MAX_METRIC_DATA_PER_REQUEST = 20;

    /**
     * The maximum number of unique {@link MetricDatum#values()} allowed in a single {@link PutMetricDataRequest}. This limit is
     * not imposed directly by CloudWatch, but they do impose a 40KB limit for a single request. This value was determined by
     * trial-and-error to roughly equate to a 40KB limit when we are also at the {@link #MAX_METRIC_DATA_PER_REQUEST}.
     */
    public static final int MAX_VALUES_PER_REQUEST = 300;

    /**
     * The {@link PutMetricDataRequest#namespace()} that should be used for all {@link PutMetricDataRequest}s returned from
     * {@link #getRequests()}.
     */
    private final String namespace;

    /**
     * The {@link TimeBucketedMetrics} that actually performs the data aggregation whenever
     * {@link #addCollection(MetricCollection)} is called.
     */
    private final TimeBucketedMetrics timeBucketedMetrics;

    public MetricCollectionAggregator(String namespace,
                                      Set<SdkMetric<String>> dimensions,
                                      Set<MetricCategory> metricCategories,
                                      Set<SdkMetric<?>> detailedMetrics) {
        this.namespace = namespace;
        this.timeBucketedMetrics = new TimeBucketedMetrics(dimensions, metricCategories, detailedMetrics);
    }

    /**
     * Add a collection to this aggregator.
     */
    public void addCollection(MetricCollection collection) {
        timeBucketedMetrics.addMetrics(collection);
    }

    /**
     * Get all {@link PutMetricDataRequest}s that can be generated from the data that was added via
     * {@link #addCollection(MetricCollection)}. This method resets the state of this {@code MetricCollectionAggregator}.
     */
    public List<PutMetricDataRequest> getRequests() {
        List<PutMetricDataRequest> requests = new ArrayList<>();

        List<MetricDatum> requestMetricDatums = new ArrayList<>();
        ValuesInRequestCounter valuesInRequestCounter = new ValuesInRequestCounter();

        Map<Instant, Collection<MetricAggregator>> metrics = timeBucketedMetrics.timeBucketedMetrics();

        for (Map.Entry<Instant, Collection<MetricAggregator>> entry : metrics.entrySet()) {
            Instant timeBucket = entry.getKey();
            for (MetricAggregator metric : entry.getValue()) {
                if (requestMetricDatums.size() >= MAX_METRIC_DATA_PER_REQUEST) {
                    requests.add(newPutRequest(requestMetricDatums));
                    requestMetricDatums.clear();
                }

                metric.ifSummary(summaryAggregator -> requestMetricDatums.add(summaryMetricDatum(timeBucket, summaryAggregator)));

                metric.ifDetailed(detailedAggregator -> {
                    int startIndex = 0;
                    Collection<DetailedMetrics> detailedMetrics = detailedAggregator.detailedMetrics();

                    while (startIndex < detailedMetrics.size()) {
                        if (valuesInRequestCounter.get() >= MAX_VALUES_PER_REQUEST) {
                            requests.add(newPutRequest(requestMetricDatums));
                            requestMetricDatums.clear();
                            valuesInRequestCounter.reset();
                        }

                        MetricDatum data = detailedMetricDatum(timeBucket, detailedAggregator,
                                                               startIndex, MAX_VALUES_PER_REQUEST - valuesInRequestCounter.get());
                        int valuesAdded = data.values().size();
                        startIndex += valuesAdded;
                        valuesInRequestCounter.add(valuesAdded);
                        requestMetricDatums.add(data);
                    }
                });
            }
        }

        if (!requestMetricDatums.isEmpty()) {
            requests.add(newPutRequest(requestMetricDatums));
        }

        timeBucketedMetrics.reset();

        return requests;
    }

    private MetricDatum detailedMetricDatum(Instant timeBucket,
                                            DetailedMetricAggregator metric,
                                            int metricStartIndex,
                                            int maxElements) {
        List<Double> values = new ArrayList<>();
        List<Double> counts = new ArrayList<>();

        Stream<DetailedMetrics> boundedMetrics = metric.detailedMetrics()
                                                       .stream()
                                                       .skip(metricStartIndex)
                                                       .limit(maxElements);

        boundedMetrics.forEach(detailedMetrics -> {
            values.add(detailedMetrics.metricValue());
            counts.add((double) detailedMetrics.metricCount());
        });

        return MetricDatum.builder()
                          .timestamp(timeBucket)
                          .metricName(metric.metric().name())
                          .dimensions(metric.dimensions())
                          .unit(metric.unit())
                          .values(values)
                          .counts(counts)
                          .build();
    }

    private MetricDatum summaryMetricDatum(Instant timeBucket,
                                           SummaryMetricAggregator metric) {
        StatisticSet stats = StatisticSet.builder()
                                         .minimum(metric.min())
                                         .maximum(metric.max())
                                         .sum(metric.sum())
                                         .sampleCount((double) metric.count())
                                         .build();
        return MetricDatum.builder()
                          .timestamp(timeBucket)
                          .metricName(metric.metric().name())
                          .dimensions(metric.dimensions())
                          .unit(metric.unit())
                          .statisticValues(stats)
                          .build();
    }

    private PutMetricDataRequest newPutRequest(List<MetricDatum> metricData) {
        return PutMetricDataRequest.builder()
                                   .namespace(namespace)
                                   .metricData(metricData)
                                   .build();
    }

    private static class ValuesInRequestCounter {
        private int valuesInRequest;

        private void add(int i) {
            valuesInRequest += i;
        }

        private int get() {
            return valuesInRequest;
        }

        private void reset() {
            valuesInRequest = 0;
        }
    }
}
