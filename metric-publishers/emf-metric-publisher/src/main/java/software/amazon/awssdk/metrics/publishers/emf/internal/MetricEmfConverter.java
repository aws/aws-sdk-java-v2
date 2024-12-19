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
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
 * @since 2.0
 * @see EmfMetricConfiguration
 */
@SdkInternalApi
public class MetricEmfConverter {
    private static final Logger logger = Logger.loggerFor(MetricEmfConverter.class);
    private static final double ZERO_THRESHOLD = 0.0001;
    private final List<String> realDimensionStrings = new ArrayList<>();
    private final EmfMetricConfiguration config;
    private final boolean metricCategoriesContainsAll;

    public MetricEmfConverter(EmfMetricConfiguration config) {
        this.config = config;
        this.metricCategoriesContainsAll = config.getMetricCategories().contains(MetricCategory.ALL);
    }

    /**
     * Processes and normalizes metric values for EMF formatting.
     * The method handles various input types and normalizes them according to EMF requirements:
     * - `null` : 0.0
     * - `Boolean` : 1.0 (true) or 0.0 (false)
     * - `Duration` : milliseconds value
     * - `Double`: normalized to 0.0 if below threshold
     *
     * @param metricValue The metric value to process
     * @return The processed value in EMF-compatible format
     */
    private Object processValue(Object metricValue) {
        if (metricValue == null) {
            return 0.0;
        }

        if (metricValue instanceof Boolean) {
            return metricValue.equals(true) ? 1.0 : 0.0;
        }

        if (metricValue instanceof Duration) {
            String durationStr = metricValue.toString();
            metricValue = Double.parseDouble(durationStr.substring(2, durationStr.length() - 1)) * 1000;
        }

        if (metricValue instanceof Double) {
            double doubleValue = (double) metricValue;
            if (Math.abs(doubleValue) < ZERO_THRESHOLD) {
                metricValue = 0.0;
            }
        }

        return metricValue;
    }

    private void writeProcessedValue(JsonWriter jsonWriter, Object processedValue) {
        if (processedValue instanceof Double) {
            jsonWriter.writeValue((Double) processedValue);
        } else if (processedValue instanceof Integer) {
            jsonWriter.writeValue((Integer) processedValue);
        } else if (processedValue instanceof Long) {
            jsonWriter.writeValue((Long) processedValue);
        }
    }


    /**
     * # Convert SDK Metrics to EMF Format
     *
     * Transforms a collection of SDK metrics into CloudWatch's Embedded Metric Format (EMF).
     * The method processes standard SDK measurements and structures them according to
     * CloudWatch's EMF specification.
     *
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
     * @return List of EMF-formatted metrics ready for CloudWatch ingestion
     */
    public List<String> convertMetricCollectionToEmf(MetricCollection metricCollection) {
        Map<String, List<Object>> aggregatedMetrics = new HashMap<>();

        // Process metrics using level-order traversal
        Queue<MetricCollection> queue = new LinkedList<>();
        if (!queue.offer(metricCollection)) {
            logger.warn(() -> "failed to add metricCollection to the queue");
        }

        while (!queue.isEmpty()) {
            MetricCollection current = queue.poll();

            current.stream().forEach(r -> {
                String metricName = r.metric().name();
                Object metricValue = r.value();

                if (isDimension(metricName)) {
                    realDimensionStrings.add(metricName);
                }

                if (shouldReport(r) || isDimension(metricName)) {
                    aggregatedMetrics.computeIfAbsent(metricName, k -> new ArrayList<>())
                                     .add(metricValue);
                }

            });

            if (current.children() != null) {
                queue.addAll(current.children());
            }
        }

        return createEmfStrings(aggregatedMetrics);
    }

    private List<String> createEmfStrings(Map<String, List<Object>> aggregatedMetrics) {
        List<String> emfStrings = new ArrayList<>();
        Map<String, List<Object>> currentMetricBatch = new HashMap<>();
        Set<String> currentMetricNames = new HashSet<>();

        for (Map.Entry<String, List<Object>> entry : aggregatedMetrics.entrySet()) {
            String metricName = entry.getKey();
            List<Object> values = entry.getValue();

            if (values.size() > 100) {
                values = values.subList(0, 100);
                logger.warn(() -> "Some AWS SDK client-side metric data have been dropped because it exceeds the cloudwatch "
                                 + "requirements.");
            }

            if (currentMetricNames.size() >= 100) {
                emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));
                currentMetricBatch = new HashMap<>();
                currentMetricNames = new HashSet<>();
            }

            currentMetricBatch.put(metricName, values);
            if (!(values.get(0) instanceof String)) {
                currentMetricNames.add(metricName);
            }

        }

        emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));

        return emfStrings;
    }


    private String createEmfString(Map<String, List<Object>> metrics, Set<String> metricNames) {

        JsonWriter jsonWriter = JsonWriter.create();
        jsonWriter.writeStartObject();

        writeAwsObject(jsonWriter, metricNames);
        writeMetricValues(jsonWriter, metrics);

        jsonWriter.writeEndObject(); // End root object

        return new String(jsonWriter.getBytes(), StandardCharsets.UTF_8);

    }

    private void writeAwsObject(JsonWriter jsonWriter, Set<String> metricNames) {
        jsonWriter.writeFieldName("_aws");
        jsonWriter.writeStartObject();


        jsonWriter.writeFieldName("Timestamp");
        //Unit Test
        jsonWriter.writeValue(12345678);


        jsonWriter.writeFieldName("LogGroupName");
        jsonWriter.writeValue(config.getLogGroupName());

        writeCloudWatchMetricsArray(jsonWriter, metricNames);
        jsonWriter.writeEndObject();
    }

    private void writeCloudWatchMetricsArray(JsonWriter jsonWriter, Set<String> metricNames) {
        jsonWriter.writeFieldName("CloudWatchMetrics");
        jsonWriter.writeStartArray();

        writeCloudWatchMetricsObjects(jsonWriter, metricNames);
        jsonWriter.writeEndArray();
    }

    private void writeCloudWatchMetricsObjects(JsonWriter jsonWriter,  Set<String> metricNames) {
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

    private void writeMetricDefinitionArray(JsonWriter jsonWriter,  Set<String> metricNames) {
        jsonWriter.writeFieldName("Metrics");
        jsonWriter.writeStartArray();


        // Write metric definitions
        for (String metricName : metricNames) {
            jsonWriter.writeStartObject();
            jsonWriter.writeFieldName("Name");
            jsonWriter.writeValue(metricName);

            if (hasUnit(metricName)) {
                jsonWriter.writeFieldName("Unit");
                jsonWriter.writeValue(getMetricUnit(metricName));
            }

            jsonWriter.writeEndObject();
        }

        jsonWriter.writeEndArray();
    }


    private void writeMetricValues(JsonWriter jsonWriter, Map<String, List<Object>> metrics) {
        for (Map.Entry<String, List<Object>> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            List<Object> values = entry.getValue();

            if (isDimension(metricName)) {
                jsonWriter.writeFieldName(metricName);
                jsonWriter.writeValue((String) values.get(0));
            } else {
                if (values.get(0) instanceof String) {
                    continue;
                }
                if (values.size() == 1) {
                    jsonWriter.writeFieldName(metricName);
                    writeProcessedValue(jsonWriter, processValue(values.get(0)));
                } else {
                    jsonWriter.writeFieldName(metricName);
                    jsonWriter.writeStartArray();
                    for (Object value : values) {
                        writeProcessedValue(jsonWriter, processValue(value));
                    }
                    jsonWriter.writeEndArray();
                }
            }
        }
    }


    private boolean isDimension(String metricName) {
        return metricName != null && config.getDimensionStrings().contains(metricName);
    }

    private boolean hasUnit(String metricName) {
        return metricName.contains("Duration");
    }

    private String getMetricUnit(String metricName) {
        if (metricName.endsWith("Duration")) {
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
