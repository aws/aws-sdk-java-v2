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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.MetricValueNormalizer;

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
    private final List<String> realDimensionStrings = new ArrayList<>();
    private final EmfMetricConfiguration config;
    private final boolean metricCategoriesContainsAll;
    private final Clock clock;

    @SdkTestInternalApi
    public MetricEmfConverter(EmfMetricConfiguration config, Clock clock) {
        this.config = config;
        this.clock = clock;
        this.metricCategoriesContainsAll = config.getMetricCategories().contains(MetricCategory.ALL);
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
        Map<String, List<MetricRecord<?>>> aggregatedMetrics = new HashMap<>();

        // Process metrics using level-order traversal
        Queue<MetricCollection> queue = new LinkedList<>();
        if (!queue.offer(metricCollection)) {
            logger.warn(() -> "failed to add metricCollection to the queue");
        }

        while (!queue.isEmpty()) {
            MetricCollection current = queue.poll();

            current.stream().forEach(mRecord -> {
                String metricName = mRecord.metric().name();

                if (isDimension(metricName)) {
                    realDimensionStrings.add(metricName);
                }

                if (shouldReport(mRecord) || isDimension(metricName)) {
                    aggregatedMetrics.computeIfAbsent(metricName, k -> new ArrayList<>())
                                     .add(mRecord);
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
            jsonWriter.writeValue(value.equals(true) ? 1.0 : 0.0);
        } else if (Duration.class.isAssignableFrom(valueClass)) {
            Duration duration = (Duration) value;
            double millisValue = duration.toMillis();
            jsonWriter.writeValue(millisValue);
        } else if (Double.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue(MetricValueNormalizer.normalize((Double) value));
        } else if (Integer.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue((Integer) value);
        } else if (Long.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue((Long) value);
        }
    }

    private List<String> createEmfStrings(Map<String, List<MetricRecord<?>>> aggregatedMetrics) {
        List<String> emfStrings = new ArrayList<>();
        Map<String, List<MetricRecord<?>>> currentMetricBatch = new HashMap<>();
        Map<String, Class<?>> currentMetricNames = new HashMap<>();

        for (Map.Entry<String, List<MetricRecord<?>>> entry : aggregatedMetrics.entrySet()) {
            String metricName = entry.getKey();
            List<MetricRecord<?>> records = entry.getValue();

            if (records.size() > 100) {
                int size = records.size();
                records = records.subList(0, 100);
                logger.warn(() -> "Some AWS SDK client-side metric data have been dropped because it exceeds the cloudwatch "
                                 + "requirements. There are " + size + " values for metric " + metricName);
            }

            if (currentMetricNames.size() >= 100) {
                emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));
                currentMetricBatch = new HashMap<>();
                currentMetricNames = new HashMap<>();
            }

            currentMetricBatch.put(metricName, records);
            if (!isStringMetric(records.get(0))) {
                currentMetricNames.put(metricName, records.get(0).metric().valueClass());
            }

        }

        emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));

        return emfStrings;
    }


    private String createEmfString(Map<String, List<MetricRecord<?>>> metrics, Map<String, Class<?>> metricNames) {

        JsonWriter jsonWriter = JsonWriter.create();
        jsonWriter.writeStartObject();

        writeAwsObject(jsonWriter, metricNames);
        writeMetricValues(jsonWriter, metrics);

        jsonWriter.writeEndObject();

        return new String(jsonWriter.getBytes(), StandardCharsets.UTF_8);

    }

    private void writeAwsObject(JsonWriter jsonWriter, Map<String, Class<?>> metricNames) {
        jsonWriter.writeFieldName("_aws");
        jsonWriter.writeStartObject();


        jsonWriter.writeFieldName("Timestamp");
        jsonWriter.writeValue(clock.instant().toEpochMilli());


        jsonWriter.writeFieldName("LogGroupName");
        jsonWriter.writeValue(config.getLogGroupName());

        writeCloudWatchMetricsArray(jsonWriter, metricNames);
        jsonWriter.writeEndObject();
    }

    private void writeCloudWatchMetricsArray(JsonWriter jsonWriter, Map<String, Class<?>> metricNames) {
        jsonWriter.writeFieldName("CloudWatchMetrics");
        jsonWriter.writeStartArray();

        writeCloudWatchMetricsObjects(jsonWriter, metricNames);
        jsonWriter.writeEndArray();
    }

    private void writeCloudWatchMetricsObjects(JsonWriter jsonWriter,  Map<String, Class<?>> metricNames) {
        jsonWriter.writeStartObject();
        jsonWriter.writeFieldName("Namespace");
        jsonWriter.writeValue(config.getNamespace());

        writeDimensionSetArray(jsonWriter);

        writeMetricDefinitionArray(jsonWriter, metricNames);
        jsonWriter.writeEndObject();
    }

    private void writeDimensionSetArray(JsonWriter jsonWriter) {
        jsonWriter.writeFieldName("Dimensions");
        jsonWriter.writeStartArray();
        jsonWriter.writeStartArray();
        for (String dimension : realDimensionStrings) {
            jsonWriter.writeValue(dimension);
        }
        jsonWriter.writeEndArray();
        jsonWriter.writeEndArray();
    }

    private void writeMetricDefinitionArray(JsonWriter jsonWriter,  Map<String, Class<?>> metricNames) {
        jsonWriter.writeFieldName("Metrics");
        jsonWriter.writeStartArray();

        metricNames.forEach((name, type) -> writeMetricDefinition(jsonWriter, name, type));

        jsonWriter.writeEndArray();
    }

    private void writeMetricDefinition(JsonWriter jsonWriter, String name, Class<?> type) {
        jsonWriter.writeStartObject();
        jsonWriter.writeFieldName("Name");
        jsonWriter.writeValue(name);

        String unit = getMetricUnit(type);
        if (unit != null) {
            jsonWriter.writeFieldName("Unit");
            jsonWriter.writeValue(unit);
        }

        jsonWriter.writeEndObject();
    }

    private void writeMetricValues(JsonWriter jsonWriter, Map<String, List<MetricRecord<?>>> metrics) {
        metrics.forEach((metricName, records) -> {
            if (records.isEmpty()) {
                return;
            }
            if (isDimension(metricName)) {
                writeDimensionValue(jsonWriter, metricName, records);
            } else {
                writeMetricRecord(jsonWriter, metricName, records);
            }
        });
    }

    private void writeDimensionValue(JsonWriter jsonWriter, String metricName, List<MetricRecord<?>> records) {
        if (records.get(0).value() == null) {
            return;
        }

        jsonWriter.writeFieldName(metricName);
        jsonWriter.writeValue((String) records.get(0).value());
    }

    private void writeMetricRecord(JsonWriter jsonWriter, String metricName, List<MetricRecord<?>> records) {
        MetricRecord<?> firstRecord = records.get(0);

        if (!isNumericMetric(firstRecord) || (records.size() == 1 && firstRecord.value() == null)) {
            return;
        }

        jsonWriter.writeFieldName(metricName);

        if (records.size() == 1) {
            processAndWriteValue(jsonWriter, firstRecord);
        } else {
            writeMetricArray(jsonWriter, records);
        }
    }

    private boolean isStringMetric(MetricRecord<?> mRecord) {
        return String.class.isAssignableFrom(mRecord.metric().valueClass());
    }

    private boolean isNumericMetric(MetricRecord<?> mRecord) {
        return Integer.class.isAssignableFrom(mRecord.metric().valueClass())
            || Boolean.class.isAssignableFrom(mRecord.metric().valueClass())
            || Long.class.isAssignableFrom(mRecord.metric().valueClass())
            || Duration.class.isAssignableFrom(mRecord.metric().valueClass())
            || Double.class.isAssignableFrom(mRecord.metric().valueClass());
    }


    private void writeMetricArray(JsonWriter jsonWriter, List<MetricRecord<?>> records) {
        jsonWriter.writeStartArray();
        for (MetricRecord<?> mRecord : records) {
            processAndWriteValue(jsonWriter, mRecord);
        }
        jsonWriter.writeEndArray();
    }


    private boolean isDimension(String metricName) {
        return metricName != null && config.getDimensionStrings().contains(metricName);
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
                           .anyMatch(config.getMetricCategories()::contains);
    }

    private boolean isSupportedLevel(MetricRecord<?> metricRecord) {
        return config.getMetricLevel().includesLevel(metricRecord.metric().level());
    }
}
