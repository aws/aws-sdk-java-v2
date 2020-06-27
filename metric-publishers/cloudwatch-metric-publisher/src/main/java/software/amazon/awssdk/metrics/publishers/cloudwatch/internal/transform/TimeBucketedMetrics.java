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

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * "Buckets" metrics by the minute in which they were collected. This allows all metric data for a given 1-minute period to be
 * aggregated under a specific {@link MetricAggregator}.
 */
@SdkInternalApi
class TimeBucketedMetrics {
    /**
     * A map from "the minute during which a metric value happened" to "the dimension and metric associated with the metric
     * values" to "the aggregator for the metric values that occurred within that minute and for that dimension/metric".
     */
    private final Map<Instant, Map<MetricAggregatorKey, MetricAggregator>> timeBucketedMetrics = new HashMap<>();

    /**
     * The dimensions that should be used for aggregating metrics that occur within a given minute. These are optional values.
     * The dimensions will be used if a {@link MetricCollection} includes them, but if it does not, it will be aggregated with
     * whatever dimensions (if any) are available.
     */
    private final Set<SdkMetric<String>> dimensions;

    /**
     * The set of metrics for which {@link DetailedMetricAggregator}s should be used for aggregation. All other metrics will use
     * a {@link SummaryMetricAggregator}.
     */
    private final Set<SdkMetric<?>> detailedMetrics;

    /**
     * The metric categories for which we should aggregate values. Any categories outside of this set will have their values
     * ignored/dropped.
     */
    private final Set<MetricCategory> metricCategories;

    /**
     * The metric levels for which we should aggregate values. Any categories at a more "verbose" level than this one will have
     * their values ignored/dropped.
     */
    private final MetricLevel metricLevel;

    /**
     * True, when the {@link #metricCategories} contains {@link MetricCategory#ALL}.
     */
    private final boolean metricCategoriesContainsAll;



    TimeBucketedMetrics(Set<SdkMetric<String>> dimensions,
                        Set<MetricCategory> metricCategories,
                        MetricLevel metricLevel,
                        Set<SdkMetric<?>> detailedMetrics) {
        this.dimensions = dimensions;
        this.detailedMetrics = detailedMetrics;
        this.metricCategories = metricCategories;
        this.metricLevel = metricLevel;
        this.metricCategoriesContainsAll = metricCategories.contains(MetricCategory.ALL);
    }

    /**
     * Add the provided collection to the proper bucket, based on the metric collection's time.
     */
    public void addMetrics(MetricCollection metrics) {
        Instant bucket = getBucket(metrics);
        addMetricsToBucket(metrics, bucket);
    }

    /**
     * Reset this bucket, clearing all stored values.
     */
    public void reset() {
        timeBucketedMetrics.clear();
    }

    /**
     * Retrieve all values in this collection. The map key is the minute in which the metric values were collected, and the
     * map value are all of the metrics that were aggregated during that minute.
     */
    public Map<Instant, Collection<MetricAggregator>> timeBucketedMetrics() {
        return timeBucketedMetrics.entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().values()));
    }

    private Instant getBucket(MetricCollection metrics) {
        return metrics.creationTime().truncatedTo(MINUTES);
    }

    private void addMetricsToBucket(MetricCollection metrics, Instant bucketId) {
        aggregateMetrics(metrics, timeBucketedMetrics.computeIfAbsent(bucketId, i -> new HashMap<>()));
    }

    private void aggregateMetrics(MetricCollection metrics, Map<MetricAggregatorKey, MetricAggregator> bucket) {
        List<Dimension> dimensions = dimensions(metrics);
        extractAllMetrics(metrics).forEach(metricRecord -> {
            MetricAggregatorKey aggregatorKey = new MetricAggregatorKey(metricRecord.metric(), dimensions);
            valueFor(metricRecord).ifPresent(metricValue -> {
                bucket.computeIfAbsent(aggregatorKey, m -> newAggregator(aggregatorKey))
                      .addMetricValue(MetricValueNormalizer.normalize(metricValue));
            });
        });
    }

    private List<Dimension> dimensions(MetricCollection metricCollection) {
        List<Dimension> result = new ArrayList<>();
        for (MetricRecord<?> metricRecord : metricCollection) {
            if (dimensions.contains(metricRecord.metric())) {
                result.add(Dimension.builder()
                                    .name(metricRecord.metric().name())
                                    .value((String) metricRecord.value())
                                    .build());
            }
        }

        // Sort the dimensions to make sure that the order in the input metric collection doesn't affect the result.
        // We use descending order just so that "ServiceName" is before "OperationName" when we use the default dimensions.
        result.sort(Comparator.comparing(Dimension::name).reversed());
        return result;
    }

    private List<MetricRecord<?>> extractAllMetrics(MetricCollection metrics) {
        List<MetricRecord<?>> result = new ArrayList<>();
        extractAllMetrics(metrics, result);
        return result;
    }

    private void extractAllMetrics(MetricCollection metrics, List<MetricRecord<?>> extractedMetrics) {
        for (MetricRecord<?> metric : metrics) {
            extractedMetrics.add(metric);
        }
        metrics.children().forEach(child -> extractAllMetrics(child, extractedMetrics));
    }

    private MetricAggregator newAggregator(MetricAggregatorKey aggregatorKey) {
        SdkMetric<?> metric = aggregatorKey.metric();
        StandardUnit metricUnit = unitFor(metric);
        if (detailedMetrics.contains(metric)) {
            return new DetailedMetricAggregator(aggregatorKey, metricUnit);
        } else {
            return new SummaryMetricAggregator(aggregatorKey, metricUnit);
        }
    }

    private StandardUnit unitFor(SdkMetric<?> metric) {
        Class<?> metricType = metric.valueClass();

        if (Duration.class.isAssignableFrom(metricType)) {
            return StandardUnit.MILLISECONDS;
        }

        return StandardUnit.NONE;
    }

    private Optional<Double> valueFor(MetricRecord<?> metricRecord) {
        if (!shouldReport(metricRecord)) {
            return Optional.empty();
        }

        Class<?> metricType = metricRecord.metric().valueClass();

        if (Duration.class.isAssignableFrom(metricType)) {
            Duration durationMetricValue = (Duration) metricRecord.value();
            long millis = durationMetricValue.toMillis();
            return Optional.of((double) millis);
        } else if (Number.class.isAssignableFrom(metricType)) {
            Number numberMetricValue = (Number) metricRecord.value();
            return Optional.of(numberMetricValue.doubleValue());
        } else if (Boolean.class.isAssignableFrom(metricType)) {
            Boolean booleanMetricValue = (Boolean) metricRecord.value();
            return Optional.of(booleanMetricValue ? 1.0 : 0.0);
        }

        return Optional.empty();
    }

    private boolean shouldReport(MetricRecord<?> metricRecord) {
        return isSupportedCategory(metricRecord) && isSupportedLevel(metricRecord);
    }

    private boolean isSupportedCategory(MetricRecord<?> metricRecord) {
        return metricCategoriesContainsAll ||
               metricRecord.metric()
                           .categories()
                           .stream()
                           .anyMatch(metricCategories::contains);
    }

    private boolean isSupportedLevel(MetricRecord<?> metricRecord) {
        return metricLevel.includesLevel(metricRecord.metric().level());
    }
}
