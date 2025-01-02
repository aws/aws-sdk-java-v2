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

package software.amazon.awssdk.metrics.publishers.emf.internal;

import static java.lang.Boolean.TRUE;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts {@link MetricCollection} into List of Amazon CloudWatch Embedded Metric Format (EMF) Strings.
 * <p>
 * This class is responsible for transforming {@link MetricCollection} into the EMF format required by CloudWatch.
 * It handles the conversion of different metric types and ensures the output conforms to EMF specifications.
 * </p>
 *
 *
 * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format.html">
 *      CloudWatch Embedded Metric Format Specification</a>
 * @see EmfMetricConfiguration
 */
@SdkInternalApi
public class MetricEmfConverter {
    private static final Logger logger = Logger.loggerFor(MetricEmfConverter.class);
    private static final double ZERO_THRESHOLD = 0.0001;

    /**
     * EMF allows up to 100 elements in an array
     * https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html
     */
    private static final int MAX_ALLOWED_NUMBER_OF_METRICS = 100;
    private final List<String> dimensions = new ArrayList<>();
    private final EmfMetricConfiguration config;
    private final boolean metricCategoriesContainsAll;
    private final Clock clock;

    @SdkTestInternalApi
    public MetricEmfConverter(EmfMetricConfiguration config, Clock clock) {
        this.config = config;
        this.clock = clock;
        this.metricCategoriesContainsAll = config.metricCategories().contains(MetricCategory.ALL);
    }

    public MetricEmfConverter(EmfMetricConfiguration config) {
        this(config, Clock.systemUTC());
    }

    /**
     * <p>
     * Convert SDK Metrics to EMF Format.
     * Transforms a collection of SDK metrics into CloudWatch's Embedded Metric Format (EMF).
     * The method processes standard SDK measurements and structures them according to
     * CloudWatch's EMF specification.
     * </p>
     * Example Output
     * <pre>
     * {
     *   "_aws": {
     *     "Timestamp": 1672963200,
     *     "CloudWatchMetrics": [{
     *       "Namespace": "AwsSdk/JavaSdk2",
     *       "Dimensions": [["ServiceId"]],
     *       "Metrics": [{
     *         "Name": "AvailableConcurrency",
     *       }]
     *     }]
     *   },
     *   "ServiceId": "DynamoDB",
     *   "AvailableConcurrency": 5
     * }
     * </pre>
     *
     * @param metricCollection Collection of SDK metrics to be converted
     * @return List of EMF-formatted metrics ready for CloudWatch
     */
    public List<String> convertMetricCollectionToEmf(MetricCollection metricCollection) {
        Map<SdkMetric<?>, List<MetricRecord<?>>> aggregatedMetrics = new HashMap<>();

        // Process metrics using level-order traversal
        Queue<MetricCollection> queue = new LinkedList<>();
        if (!queue.offer(metricCollection)) {
            logger.warn(() -> "failed to add metricCollection to the queue");
        }

        while (!queue.isEmpty()) {
            MetricCollection current = queue.poll();

            current.stream().forEach(metricRecord -> {
                SdkMetric<?> metric = metricRecord.metric();
                String metricName = metric.name();
                if (isDimension(metric)) {
                    dimensions.add(metricName);
                }

                if (shouldReport(metricRecord) || isDimension(metric)) {
                    aggregatedMetrics.computeIfAbsent(metric, k -> new ArrayList<>())
                                     .add(metricRecord);
                }

            });

            if (current.children() != null) {
                queue.addAll(current.children());
            }
        }

        return createEmfStrings(aggregatedMetrics);
    }

    /**
     * Processes and normalizes metric values for EMF formatting.
     * The method handles various input types and normalizes them according to EMF requirements:
     * Value Conversion Rules:
     * <ul>
     *   <li>Numbers (Integer, Long, Double, etc.) are converted to their native numeric format</li>
     *   <li>Duration values are converted to milliseconds</li>
     *   <li>Date/Time values are converted to epoch milliseconds</li>
     *   <li>Null values are omitted from the output</li>
     *   <li>Boolean values are converted to 1 (true) or 0 (false)</li>
     *   <li>Non-Dimension metrics with non-numeric values are omitted from the output</li>
     * </ul>
     *
     * @param mRecord The metric record to process
     */
    private void processAndWriteValue(JsonWriter jsonWriter, MetricRecord<?> mRecord) {
        Object value = mRecord.value();
        Class<?> valueClass = mRecord.metric().valueClass();

        if (value == null) {
            return;
        }

        if (Boolean.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue(value.equals(TRUE) ? 1.0 : 0.0);
            return;
        }

        if (Duration.class.isAssignableFrom(valueClass)) {
            Duration duration = (Duration) value;
            double millisValue = duration.toMillis();
            jsonWriter.writeValue(millisValue);
            return;
        }

        if (Double.class.isAssignableFrom(valueClass)) {
            double doubleValue = (Double) value;
            if (Math.abs(doubleValue) < ZERO_THRESHOLD) {
                jsonWriter.writeValue(0.0);
            } else {
                jsonWriter.writeValue(doubleValue);
            }
            return;
        }

        if (Integer.class.isAssignableFrom(valueClass)
            || Long.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue((Integer) value);
        }
    }

    private List<String> createEmfStrings(Map<SdkMetric<?>, List<MetricRecord<?>>> aggregatedMetrics) {
        List<String> emfStrings = new ArrayList<>();
        Map<SdkMetric<?>, List<MetricRecord<?>>> currentMetricBatch = new HashMap<>();

        for (Map.Entry<SdkMetric<?>, List<MetricRecord<?>>> entry : aggregatedMetrics.entrySet()) {
            SdkMetric<?> metric = entry.getKey();
            List<MetricRecord<?>> records = entry.getValue();

            int size = records.size();
            if (size > MAX_ALLOWED_NUMBER_OF_METRICS) {
                records = records.subList(0, 100);
                logger.warn(() -> String.format("Some AWS SDK client-side metric data have been dropped because it exceeds the "
                                        + "cloudwatch requirements. There are %d for metric %s", size, metric.name()));
            }

            if (currentMetricBatch.size() == 100) {
                emfStrings.add(createEmfString(currentMetricBatch));
                currentMetricBatch = new HashMap<>();
            }

            currentMetricBatch.put(metric, records);
        }

        emfStrings.add(createEmfString(currentMetricBatch));

        return emfStrings;
    }


    private String createEmfString(Map<SdkMetric<?>, List<MetricRecord<?>>> metrics) {

        JsonWriter jsonWriter = JsonWriter.create();
        jsonWriter.writeStartObject();

        writeAwsObject(jsonWriter, metrics.keySet());
        writeMetricValues(jsonWriter, metrics);

        jsonWriter.writeEndObject();

        return new String(jsonWriter.getBytes(), StandardCharsets.UTF_8);

    }

    private void writeAwsObject(JsonWriter jsonWriter, Set<SdkMetric<?>> metricNames) {
        jsonWriter.writeFieldName("_aws");
        jsonWriter.writeStartObject();

        jsonWriter.writeFieldName("Timestamp");
        jsonWriter.writeValue(clock.instant().toEpochMilli());

        jsonWriter.writeFieldName("LogGroupName");
        jsonWriter.writeValue(config.logGroupName());

        writeCloudWatchMetricsArray(jsonWriter, metricNames);
        jsonWriter.writeEndObject();
    }

    private void writeCloudWatchMetricsArray(JsonWriter jsonWriter, Set<SdkMetric<?>> metricNames) {
        jsonWriter.writeFieldName("CloudWatchMetrics");
        jsonWriter.writeStartArray();

        writeCloudWatchMetricsObjects(jsonWriter, metricNames);
        jsonWriter.writeEndArray();
    }

    private void writeCloudWatchMetricsObjects(JsonWriter jsonWriter,  Set<SdkMetric<?>> metricNames) {
        jsonWriter.writeStartObject();
        jsonWriter.writeFieldName("Namespace");
        jsonWriter.writeValue(config.namespace());

        writeDimensionSetArray(jsonWriter);

        writeMetricDefinitionArray(jsonWriter, metricNames);
        jsonWriter.writeEndObject();
    }

    private void writeDimensionSetArray(JsonWriter jsonWriter) {
        jsonWriter.writeFieldName("Dimensions");
        jsonWriter.writeStartArray();
        jsonWriter.writeStartArray();
        for (String dimension : dimensions) {
            jsonWriter.writeValue(dimension);
        }
        jsonWriter.writeEndArray();
        jsonWriter.writeEndArray();
    }

    private void writeMetricDefinitionArray(JsonWriter jsonWriter, Set<SdkMetric<?>> sdkMetrics) {
        jsonWriter.writeFieldName("Metrics");
        jsonWriter.writeStartArray();

        sdkMetrics.forEach(sdkMetric -> writeMetricDefinition(jsonWriter, sdkMetric));

        jsonWriter.writeEndArray();
    }

    private void writeMetricDefinition(JsonWriter jsonWriter, SdkMetric<?> sdkMetric) {
        if (!isNumericMetric(sdkMetric)) {
            return;
        }

        jsonWriter.writeStartObject();
        jsonWriter.writeFieldName("Name");
        jsonWriter.writeValue(sdkMetric.name());

        String unit = getMetricUnit(sdkMetric.valueClass());
        if (unit != null) {
            jsonWriter.writeFieldName("Unit");
            jsonWriter.writeValue(unit);
        }

        jsonWriter.writeEndObject();
    }

    private void writeMetricValues(JsonWriter jsonWriter, Map<SdkMetric<?>, List<MetricRecord<?>>> metrics) {
        metrics.forEach((metric, records) -> {
            if (isDimension(metric)) {
                writeDimensionValue(jsonWriter, metric, records);
            } else {
                writeMetricRecord(jsonWriter, metric, records);
            }
        });
    }

    private void writeDimensionValue(JsonWriter jsonWriter, SdkMetric<?> metric, List<MetricRecord<?>> records) {
        Validate.validState(records.size() == 1 && String.class.isAssignableFrom(metric.valueClass()),
                            "Metric (%s) is configured as a dimension, and the value must be a single string",
                            metric.name());

        jsonWriter.writeFieldName(metric.name());
        jsonWriter.writeValue((String) records.get(0).value());
    }

    private void writeMetricRecord(JsonWriter jsonWriter, SdkMetric<?> metric, List<MetricRecord<?>> records) {
        if (!isNumericMetric(metric)) {
            return;
        }

        jsonWriter.writeFieldName(metric.name());

        if (records.size() == 1) {
            processAndWriteValue(jsonWriter, records.get(0));
        } else {
            writeMetricArray(jsonWriter, records);
        }
    }

    private boolean isNumericMetric(SdkMetric<?> metric) {
        return Integer.class.isAssignableFrom(metric.valueClass())
            || Boolean.class.isAssignableFrom(metric.valueClass())
            || Long.class.isAssignableFrom(metric.valueClass())
            || Duration.class.isAssignableFrom(metric.valueClass())
            || Double.class.isAssignableFrom(metric.valueClass());
    }

    private void writeMetricArray(JsonWriter jsonWriter, List<MetricRecord<?>> records) {
        jsonWriter.writeStartArray();
        for (MetricRecord<?> mRecord : records) {
            processAndWriteValue(jsonWriter, mRecord);
        }
        jsonWriter.writeEndArray();
    }

    private boolean isDimension(SdkMetric<?> metric) {
        return config.dimensions().contains(metric);
    }


    private String getMetricUnit(Class<?> type) {
        if (Duration.class.isAssignableFrom(type)) {
            return "Milliseconds";
        }
        return null;
    }

    private boolean shouldReport(MetricRecord<?> metricRecord) {
        return isSupportedCategory(metricRecord) && isSupportedLevel(metricRecord);
    }

    private boolean isSupportedCategory(MetricRecord<?> metricRecord) {
        return metricCategoriesContainsAll ||
               metricRecord.metric()
                           .categories()
                           .stream()
                           .anyMatch(config.metricCategories()::contains);
    }

    private boolean isSupportedLevel(MetricRecord<?> metricRecord) {
        return config.metricLevel().includesLevel(metricRecord.metric().level());
    }
}
