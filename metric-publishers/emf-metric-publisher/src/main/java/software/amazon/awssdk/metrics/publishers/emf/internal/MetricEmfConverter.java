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

/**
 * # MetricEmfConverter

 * Converts metrics into Amazon CloudWatch Embedded Metric Format (EMF).
 * This internal class handles the transformation of various metric types
 * into EMF-compatible format.

 * ## Configuration
 * The converter is initialized with an `EmfMetricConfiguration` that defines:
 * - Metric categories to process
 * - Other EMF-specific settings

 * ## Implementation Notes
 * - Boolean values are converted to numeric values (1.0 for true, 0.0 for false)
 * - Null values are converted to 0.0
 * - Very small numeric values (below 0.0001) are normalized to 0.0
 * - Duration values are converted to milliseconds
 *
 * @see EmfMetricConfiguration
 */
@SdkInternalApi
public class MetricEmfConverter {
    private static final Logger logger = Logger.loggerFor(MetricEmfConverter.class);
    private static final double ZERO_THRESHOLD = 0.0001;
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
     * Processes and normalizes metric values for EMF formatting.
     * The method handles various input types and normalizes them according to EMF requirements:
     * - `null` : 0.0
     * - `Boolean` : 1.0 (true) or 0.0 (false)
     * - `Duration` : milliseconds value
     * - `Double`: normalized to 0.0 if below threshold
     *
     * @param mRecord The metric record to process
     */
    private void processAndWriteValue(JsonWriter jsonWriter, MetricRecord<?> mRecord) {
        Object value = mRecord.value();
        Class<?> valueClass = mRecord.metric().valueClass();

        if (value == null) {
            jsonWriter.writeValue(0.0);
        } else if (Boolean.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue(value.equals(true) ? 1.0 : 0.0);
        } else if (Duration.class.isAssignableFrom(valueClass)) {
            Duration duration = (Duration) value;
            double millisValue = duration.toMillis();
            jsonWriter.writeValue(millisValue);
        } else if (Double.class.isAssignableFrom(valueClass)) {
            double doubleValue = (Double) value;
            if (Math.abs(doubleValue) < ZERO_THRESHOLD) {
                jsonWriter.writeValue(0.0);
            } else {
                jsonWriter.writeValue(doubleValue);
            }
        } else if (Integer.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue((Integer) value);
        } else if (Long.class.isAssignableFrom(valueClass)) {
            jsonWriter.writeValue((Long) value);
        }
    }

    /**
     * # Convert SDK Metrics to EMF Format
     * Transforms a collection of SDK metrics into CloudWatch's Embedded Metric Format (EMF).
     * The method processes standard SDK measurements and structures them according to
     * CloudWatch's EMF specification.
     * ## Example Output
     * ```json
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
     *   "ServiceId": "XXXXXXXXXXXXX",
     *   "AvailableConcurrency": 5
     * }
     * ```
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

    private List<String> createEmfStrings(Map<String, List<MetricRecord<?>>> aggregatedMetrics) {
        List<String> emfStrings = new ArrayList<>();
        Map<String, List<MetricRecord<?>>> currentMetricBatch = new HashMap<>();
        Map<String, Class<?>> currentMetricNames = new HashMap<>();

        for (Map.Entry<String, List<MetricRecord<?>>> entry : aggregatedMetrics.entrySet()) {
            String metricName = entry.getKey();
            List<MetricRecord<?>> records = entry.getValue();

            if (records.size() > 100) {
                records = records.subList(0, 100);
                logger.warn(() -> "Some AWS SDK client-side metric data have been dropped because it exceeds the cloudwatch "
                                 + "requirements.");
            }

            if (currentMetricNames.size() >= 100) {
                emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));
                currentMetricBatch = new HashMap<>();
                currentMetricNames = new HashMap<>();
            }

            currentMetricBatch.put(metricName, records);
            if (!String.class.isAssignableFrom(records.get(0).metric().valueClass())) {
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
        jsonWriter.writeValue(clock.instant());


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


        // Write metric definitions
        metricNames.forEach((name, type) -> {
            jsonWriter.writeStartObject();
            jsonWriter.writeFieldName("Name");
            jsonWriter.writeValue(name);

            String unit = getMetricUnit(type);
            if (unit != null) {
                jsonWriter.writeFieldName("Unit");
                jsonWriter.writeValue(unit);
            }

            jsonWriter.writeEndObject();
        });

        jsonWriter.writeEndArray();
    }


    private void writeMetricValues(JsonWriter jsonWriter, Map<String, List<MetricRecord<?>>> metrics) {
        for (Map.Entry<String, List<MetricRecord<?>>> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            List<MetricRecord<?>> records = entry.getValue();

            if (isDimension(metricName)) {
                jsonWriter.writeFieldName(metricName);
                jsonWriter.writeValue((String) records.get(0).value());
            } else {
                if (records.get(0).value() instanceof String) {
                    continue;
                }
                if (records.size() == 1) {
                    jsonWriter.writeFieldName(metricName);
                    processAndWriteValue(jsonWriter, records.get(0));
                } else {
                    jsonWriter.writeFieldName(metricName);
                    jsonWriter.writeStartArray();
                    for (MetricRecord<?> mRecord: records) {
                        processAndWriteValue(jsonWriter, mRecord);
                    }
                    jsonWriter.writeEndArray();
                }
            }
        }
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
