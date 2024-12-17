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

package software.amazon.awssdk.metrics.publishers.emf;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.Logger;

/**
 * A metric publisher implementation that converts metrics into CloudWatch Embedded Metric Format (EMF).
 * EMF allows metrics to be published through CloudWatch Logs using a structured JSON format, which
 * CloudWatch automatically extracts and processes into metrics.
 *
 * <p>This publisher is particularly well-suited for serverless environments like AWS Lambda and container
 * environments like Amazon ECS that have built-in integration with CloudWatch Logs. Using EMF eliminates
 * the need for separate metric publishing infrastructure as metrics are automatically extracted from
 * log entries.</p>
 *
 * <p>The EMF publisher converts metric collections into JSON-formatted log entries that conform to the
 * CloudWatch EMF specification. These logs are written to the "/aws/emf/metrics" log group by default.
 * CloudWatch automatically processes these logs to generate corresponding metrics that can be used for
 * monitoring and alerting.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * EmfMetricPublisher publisher = PublisherBuilder.dimensions(CoreMetric.SERVICE_ID)
 *                                               .build();
 *
 * MetricCollector collector = MetricCollector.create("test");
 * collector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId1234");
 * collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
 *
 * List emfLogs = publisher.convertMetricCollectionToEMF(collector.collect());
 * </pre>
 *
 * <p>The generated EMF logs include:</p>
 * <ul>
 *   <li>Metric values and their associated dimensions</li>
 *   <li>Timestamp information</li>
 *   <li>Namespace (defaults to "AwsSdk/JavaSdk2")</li>
 *   <li>CloudWatch Metrics directives that specify how to process the metrics</li>
 * </ul>
 *
 *
 */

@ThreadSafe
@Immutable
@SdkPublicApi
public final class EmfMetricPublisher implements MetricPublisher {
    private static final String DEFAULT_NAMESPACE = "AwsSdk/JavaSdk2";
    private static final String  DEFAULT_LOG_GROUP_NAME = "/aws/emf/metrics";
    private static final Set<SdkMetric<String>> DEFAULT_DIMENSIONS = Collections.emptySet();
    private static final Set<MetricCategory> DEFAULT_METRIC_CATEGORIES = Collections.singleton(MetricCategory.ALL);
    private static final MetricLevel DEFAULT_METRIC_LEVEL = MetricLevel.INFO;
    private static final double ZERO_THRESHOLD = 0.0001;
    private static final Logger logger = Logger.loggerFor("software.amazon.awssdk.metrics.publishers.emf");
    private final String namespace;
    private final String logGroupName;
    private final Collection<SdkMetric<String>> dimensions;
    private final List<String> dimensionStrings;
    private final List<String> realDimensionStrings = new ArrayList<>();
    private final Collection<MetricCategory> metricCategories;
    private final MetricLevel metricLevel;
    private final boolean metricCategoriesContainsAll;
    private final boolean unitTest;

    private EmfMetricPublisher(Builder builder){
        this.namespace = resolveNamespace(builder);
        this.logGroupName = resolveLogGroupName(builder);
        this.dimensions = resolveDimensions(builder);
        this.metricCategories = resolveMetricCategories(builder);
        this.metricLevel = resolveMetricLevel(builder);
        this.dimensionStrings = resolveDimensionStrings(builder);
        this.metricCategoriesContainsAll = metricCategories.contains(MetricCategory.ALL);
        this.unitTest = builder.unitTest;
    }
    private static String resolveNamespace(Builder builder) {
        return builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
    }
    private static Collection<SdkMetric<String>> resolveDimensions(Builder builder) {
        return builder.dimensions == null ? DEFAULT_DIMENSIONS : new HashSet<>(builder.dimensions);
    }
    private static Collection<MetricCategory> resolveMetricCategories(Builder builder) {
        return builder.metricCategories == null ? DEFAULT_METRIC_CATEGORIES : new HashSet<>(builder.metricCategories);
    }
    private static MetricLevel resolveMetricLevel(Builder builder) {
        return builder.metricLevel == null ? DEFAULT_METRIC_LEVEL : builder.metricLevel;
    }
    private static List<String> resolveDimensionStrings(Builder builder) {
        List<String> dimensionStrings = new ArrayList<>();
        if (builder.dimensions != null) {
            for (SdkMetric<String> dimension : builder.dimensions) {
                dimensionStrings.add(dimension.name());
            }
        } else {
            for (SdkMetric<String> dimension : DEFAULT_DIMENSIONS) {
                dimensionStrings.add(dimension.name());
            }
        }
        return dimensionStrings;
    }

    private static String resolveLogGroupName(Builder builder) {
        return builder.logGroupName == null ? DEFAULT_LOG_GROUP_NAME: builder.logGroupName;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static EmfMetricPublisher create() {
        return builder().build();
    }


    /**
     * Processes and normalizes metric values for EMF formatting.
     *
     * @param metricValue The value of the metric to be processed, can be Boolean, Double, or a duration string
     * @return Object containing the processed metric value:
     *         - For null input: returns 0.0
     *         - For Boolean: converts true to 1.0 and false to 0.0
     *         - For duration values: converts to milliseconds
     *         - For Double values: normalizes very small values (less than ZERO_THRESHOLD) to 0.0
     *         - For other cases: returns the original value
     */
    private Object processValue(Object metricValue) {
        if (metricValue == null) {
            return 0.0;
        }

        // Changes boolean value from true/false to 1/0
        if (metricValue instanceof Boolean) {
            return metricValue.equals(true) ? 1.0 : 0.0;
        }

        // Changes duration value to a number of ms
        if (metricValue instanceof Duration) {
            String durationStr = metricValue.toString();
            metricValue = Double.parseDouble(durationStr.substring(2, durationStr.length() - 1)) * 1000;
        }

        //normalize
        if (metricValue instanceof Double) {
            double doubleValue = (double) metricValue;
            if (Math.abs(doubleValue) < ZERO_THRESHOLD) {
                metricValue = 0.0;
            }
        }

        return metricValue;
    }

    private void writeProcessedValue(JsonWriter jsonWriter, Object processedValue){
        if (processedValue instanceof Double) {
            jsonWriter.writeValue((Double) processedValue);
        } else if (processedValue instanceof Integer) {
            jsonWriter.writeValue((Integer) processedValue);
        } else if (processedValue instanceof Long) {
            jsonWriter.writeValue((Long) processedValue);
        }
    }


    /**
     * Converts a collection of SDK metrics into EMF (Embedded Metric Format) metrics.
     * This method transforms standard SDK metric measurements into the EMF format
     * that can be processed by CloudWatch.
     *
     * @param metricCollection The collection of SDK metrics to be converted to EMF format
     * @return A list of EMF-formatted metrics ready for publishing to CloudWatch
     *
     * <p>The example emf string generated:</>
     * <pre>
        * { "_aws": { "Timestamp": 1672963200, "CloudWatchMetrics": [{ "Namespace": "AwsSdk/JavaSdk2", "Dimensions": [["ServiceId"]], "Metrics": [{ "Name": "AvailableConcurrency", "Unit": "Count" }] }] }, "ServiceId": "XXXXXXXXXXXXX", "AvailableConcurrency": 5 }
     * </pre>
     */
    public List<String> convertMetricCollectionToEMF(MetricCollection metricCollection) {
        // Map to store aggregated metrics
        Map<String, List<Object>> aggregatedMetrics = new HashMap<>();

        // Process metrics using level-order traversal
        Queue<MetricCollection> queue = new LinkedList<>();
        queue.offer(metricCollection);

        while (!queue.isEmpty()) {
            MetricCollection current = queue.poll();

            // Process all metrics in current collection
            current.stream().forEach(r -> {
                String metricName = r.metric().name();
                Object metricValue = r.value();

                // Store dimension and metric name for later use in Metrics array
                if (isDimension(metricName)) {
                    realDimensionStrings.add(metricName);
                }

                // Add value to aggregated metrics
                if(shouldReport(r) || isDimension(metricName)){
                    aggregatedMetrics.computeIfAbsent(metricName, k -> new ArrayList<>())
                                     .add(metricValue);
                }

            });

            // Add children to queue
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

        // Process metric names and their values
        for (Map.Entry<String, List<Object>> entry : aggregatedMetrics.entrySet()) {
            String metricName = entry.getKey();
            List<Object> values = entry.getValue();

            // Drop large value arrays into chunks of 100
            if (values.size() > 100) {
                values = values.subList(0, 100);
                logger.warn(()-> "Some AWS SDK client-side metric data have been dropped because it exceeds the cloudwatch "
                                 + "requirements. This usually occurs because you have generated too many requests for the publisher to handle in a timely fashion.");
            }
            // If adding this metric would exceed 100 metrics, create new batch
            if (currentMetricNames.size() >= 100) {
                emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));
                currentMetricBatch = new HashMap<>();
                currentMetricNames = new HashSet<>();
            }

            currentMetricBatch.put(metricName, values);
            if(!(values.get(0) instanceof String)){
                currentMetricNames.add(metricName);
            }

        }

        emfStrings.add(createEmfString(currentMetricBatch, currentMetricNames));

        return emfStrings;
    }


    private String createEmfString(Map<String, List<Object>> metrics, Set<String> metricNames) {

        try {
            JsonWriter jsonWriter = JsonWriter.create();
            jsonWriter.writeStartObject();

            // Start with _aws section
            jsonWriter.writeFieldName("_aws");
            jsonWriter.writeStartObject();

            // Write Timestamp
            if (unitTest) {

                jsonWriter.writeFieldName("Timestamp");
                jsonWriter.writeValue(12345678);
            } else {

                jsonWriter.writeFieldName("Timestamp");
                jsonWriter.writeValue(Instant.now().toEpochMilli());
            }
            jsonWriter.writeFieldName("LogGroupName");
            jsonWriter.writeValue(logGroupName);

            // Write CloudWatchMetrics array
            jsonWriter.writeFieldName("CloudWatchMetrics");
            jsonWriter.writeStartArray();
            jsonWriter.writeStartObject();

            // Write Namespace
            jsonWriter.writeFieldName("Namespace");
            jsonWriter.writeValue(namespace);

            // Write Dimensions array
            jsonWriter.writeFieldName("Dimensions");
            jsonWriter.writeStartArray();
            jsonWriter.writeStartArray();
            for (String dimension : realDimensionStrings) {
                jsonWriter.writeValue(dimension);
            }
            jsonWriter.writeEndArray();
            jsonWriter.writeEndArray();

            // Write Metrics array
            jsonWriter.writeFieldName("Metrics");
            jsonWriter.writeStartArray();


            // Write metric definitions
            for (String metricName : metricNames) {
                jsonWriter.writeStartObject();
                jsonWriter.writeFieldName("Name");
                jsonWriter.writeValue(metricName);

                // Add Unit if available
                if (hasUnit(metricName)) {
                    jsonWriter.writeFieldName("Unit");
                    jsonWriter.writeValue(getMetricUnit(metricName));
                }

                jsonWriter.writeEndObject();
            }

            jsonWriter.writeEndArray(); // End Metrics array
            jsonWriter.writeEndObject(); // End CloudWatchMetrics object
            jsonWriter.writeEndArray(); // End CloudWatchMetrics array
            jsonWriter.writeEndObject(); // End _aws object

            // Write metric values
            for (Map.Entry<String, List<Object>> entry : metrics.entrySet()) {
                String metricName = entry.getKey();
                List<Object> values = entry.getValue();

                // For dimension metrics, write the last value
                if (isDimension(metricName)) {
                    jsonWriter.writeFieldName(metricName);
                    jsonWriter.writeValue((String) values.get(values.size() - 1));
                } else {
                    //skip string values
                    if (values.get(0) instanceof String) {
                        continue;
                    }
                    // For regular metrics, if there's only one value, write it directly
                    if (values.size() == 1) {
                        jsonWriter.writeFieldName(metricName);
                        writeProcessedValue(jsonWriter,processValue(values.get(0)));
                    } else {
                        // If there are multiple values, write as an array
                        jsonWriter.writeFieldName(metricName);
                        jsonWriter.writeStartArray();
                        for (Object value : values) {
                            writeProcessedValue(jsonWriter,processValue(value));
                        }
                        jsonWriter.writeEndArray();
                    }
                }
            }

            jsonWriter.writeEndObject(); // End root object

            return new String(jsonWriter.getBytes(), StandardCharsets.UTF_8);

        } catch(SdkClientException e){
            logger.error(()-> "Failed to create EMF format string");
            throw e;
        }
    }


    private boolean isDimension(String metricName) {
        return metricName != null && dimensionStrings.contains(metricName);
    }

    private boolean hasUnit(String metricName) {
        // Implement logic to determine if metric should have a unit
        return metricName.contains("Duration");
    }

    private String getMetricUnit(String metricName) {
        if (metricName.endsWith("Duration")) {
            return "Milliseconds";
        }
        // Add other unit mappings
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
                           .anyMatch(metricCategories::contains);
    }

    private boolean isSupportedLevel(MetricRecord<?> metricRecord) {
        return metricLevel.includesLevel(metricRecord.metric().level());
    }


    @Override
    public void publish(MetricCollection metricCollection) {
        if (metricCollection == null) {
            return;
        }

        List<String> emfStrings = convertMetricCollectionToEMF(metricCollection);
        for (String emfString : emfStrings) {
            logger.info(() -> emfString);
        }

    }

    @Override
    public void close() {}

    public static final class Builder{
        private String namespace;
        private String logGroupName;
        private Collection<SdkMetric<String>> dimensions;
        private Collection<MetricCategory> metricCategories;
        private MetricLevel metricLevel;
        private boolean unitTest = false;

        private Builder() {
        }

        /**
         * Configure the namespace that will be put into the emf log to this publisher.
         *
         * <p>If this is not specified, {@code AwsSdk/JavaSdk2} will be used.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Configure the {@link SdkMetric} that are used to define the Dimension Set Array that will be put into the emf log to
         * this
         * publisher.
         *
         * <p>If this is not specified, [] will be used.
         */
        public Builder dimensions(Collection<SdkMetric<String>> dimensions) {
            this.dimensions = new ArrayList<>(dimensions);
            return this;
        }

        @SafeVarargs
        public final Builder dimensions(SdkMetric<String>... dimensions) {
            return dimensions(Arrays.asList(dimensions));
        }


        /**
         * Configure the {@link MetricCategory}s that should be uploaded to CloudWatch.
         *
         * <p>If this is not specified, {@link MetricCategory#ALL} is used.
         *
         * <p>All {@link SdkMetric}s are associated with at least one {@code MetricCategory}. This setting determines which
         * category of metrics uploaded to CloudWatch. Any metrics {@link #publish(MetricCollection)}ed that do not fall under
         * these configured categories are ignored.
         *
         * <p>Note: If there are {@link #dimensions(Collection)} configured that do not fall under these {@code MetricCategory}
         * values, the dimensions will NOT be ignored. In other words, the metric category configuration only affects which
         * metrics are uploaded to CloudWatch, not which values can be used for {@code dimensions}.
         */
        public Builder metricCategories(Collection<MetricCategory> metricCategories) {
            this.metricCategories = new ArrayList<>(metricCategories);
            return this;
        }

        /**
         * @see #metricCategories(Collection)
         */
        public Builder metricCategories(MetricCategory... metricCategories) {
            return metricCategories(Arrays.asList(metricCategories));
        }

        /**
         * Configure the {@link MetricLevel} that should be uploaded to CloudWatch.
         *
         * <p>If this is not specified, {@link MetricLevel#INFO} is used.
         *
         * <p>All {@link SdkMetric}s are associated with one {@code MetricLevel}. This setting determines which level of metrics
         * uploaded to CloudWatch. Any metrics {@link #publish(MetricCollection)}ed that do not fall under these configured
         * categories are ignored.
         *
         * <p>Note: If there are {@link #dimensions(Collection)} configured that do not fall under this {@code MetricLevel}
         * values, the dimensions will NOT be ignored. In other words, the metric category configuration only affects which
         * metrics are uploaded to CloudWatch, not which values can be used for {@code dimensions}.
         */
        public Builder metricLevel(MetricLevel metricLevel) {
            this.metricLevel = metricLevel;
            return this;
        }

        /**
         * Configure the LogGroupName key that will be put into the emf log to this publisher. This is a required key for
         * using the CloudWatch agent to send embedded metric format logs that tells the agent which log group to use.
         *
         * <p>If this is not specified, {@code /aws/emf/metrics} will be used.
         */
        public Builder logGroupName(String logGroupName) {
            this.logGroupName = logGroupName;
            return this;
        }


        /**
         * Configure the unitTest field to the publisher. It tells the publisher if this publisher is used in unit test.
         * If so, it will use a fixed timestamp when generating emf string instead of the current time.
         *
         * <p>If this is not specified, {@code false} will be used.
         */
        public Builder unitTest(boolean unitTest) {
            this.unitTest = unitTest;
            return this;
        }

        /**
         * Build a {@link EmfMetricPublisher} using the configuration currently configured on this publisher.
         */
        public EmfMetricPublisher build() {
            return new EmfMetricPublisher(this);
        }

    }
}
