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
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;
import software.amazon.awssdk.utils.Logger;

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
    // private final Level logLevel;
    private final String namespace;
    private final String logGroupName;
    private final Collection<SdkMetric<String>> dimensions;
    private final ArrayList<String> dimensionStrings;
    private final ArrayList<String> realDimensionStrings = new ArrayList<>();
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
    private static ArrayList<String> resolveDimensionStrings(Builder builder) {
        ArrayList<String> dimensionStrings = new ArrayList<>();
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


    private Object processValue(String metricName, Object metricValue) {
        if (metricValue == null) {
            return 0.0;
        }

        // Changes boolean value from true/false to 1/0
        if (metricValue instanceof Boolean) {
            return metricValue.equals(true) ? 1.0 : 0.0;
        }

        // Changes duration value to a number of ms
        if (hasUnit(metricName)) {
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
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        JsonFactory factory = new JsonFactory();

        try{
            JsonGenerator generator = factory.createGenerator(byteStream);
            generator.writeStartObject();

            // Start with _aws section
            generator.writeFieldName("_aws");
            generator.writeStartObject();

            // Write Timestamp
            if (unitTest) {
                generator.writeNumberField("Timestamp", 12345678);
            } else {
                generator.writeNumberField("Timestamp", Instant.now().toEpochMilli());
            }
            generator.writeStringField("LogGroupName", logGroupName);

            // Write CloudWatchMetrics array
            generator.writeFieldName("CloudWatchMetrics");
            generator.writeStartArray();
            generator.writeStartObject();

            // Write Namespace
            generator.writeStringField("Namespace", namespace);

            // Write Dimensions array
            generator.writeFieldName("Dimensions");
            generator.writeStartArray();
            generator.writeStartArray();
            for (String dimension : realDimensionStrings) {
                generator.writeString(dimension);
            }
            generator.writeEndArray();
            generator.writeEndArray();

            // Write Metrics array
            generator.writeFieldName("Metrics");
            generator.writeStartArray();


            // Write metric definitions
            for (String metricName : metricNames) {
                generator.writeStartObject();
                generator.writeStringField("Name", metricName);

                // Add Unit if available
                if (hasUnit(metricName)) {
                    generator.writeStringField("Unit", getMetricUnit(metricName));
                }

                generator.writeEndObject();
            }

            generator.writeEndArray(); // End Metrics array
            generator.writeEndObject(); // End CloudWatchMetrics object
            generator.writeEndArray(); // End CloudWatchMetrics array
            generator.writeEndObject(); // End _aws object

            // Write metric values
            for (Map.Entry<String, List<Object>> entry : metrics.entrySet()) {
                String metricName = entry.getKey();
                List<Object> values = entry.getValue();

                // For dimension metrics, write the last value
                if (isDimension(metricName)) {
                    generator.writeObjectField(metricName, values.get(values.size() - 1));
                } else {
                    //skip string values
                    if (values.get(0) instanceof String){
                        continue;
                    }
                    // For regular metrics, if there's only one value, write it directly
                    if (values.size() == 1) {
                        generator.writeObjectField(metricName, processValue(metricName,values.get(0)));
                    } else {
                        // If there are multiple values, write as an array
                        generator.writeFieldName(metricName);
                        generator.writeStartArray();
                        for (Object value : values) {
                            generator.writeObject(processValue(metricName,value));

                        }
                        generator.writeEndArray();
                    }
                }
            }

            generator.writeEndObject(); // End root object
            generator.close();

            return new String(byteStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to convert metrics to EMF format", e);
        }

    }



public List<String> convertMetricCollectionToEMF(MetricCollection metricCollection) {
    // Map to store aggregated metrics
    Map<String, List<Object>> aggregatedMetrics = new HashMap<>();

    // Process metrics using level-order traversal
    Queue<MetricCollection> queue = new LinkedList<>();
    queue.offer(metricCollection);

    while (!queue.isEmpty()) {
        MetricCollection current = queue.poll();

        // Process all metrics in current collection
        current.stream().forEach(record -> {
            if(!shouldReport(record)){
                return;
            }
            String metricName = record.metric().name();
            Object metricValue = record.value();

            // Store dimension and metric name for later use in Metrics array
            if (isDimension(metricName)) {
                realDimensionStrings.add(metricName);
            }

            // Add value to aggregated metrics
            aggregatedMetrics.computeIfAbsent(metricName, k -> new ArrayList<>())
                             .add(metricValue);
        });

        // Add children to queue
        if (current.children() != null) {
            queue.addAll(current.children());
        }
    }

    return createEmfStrings(aggregatedMetrics);
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

        public Builder dimensions(Collection<SdkMetric<String>> dimensions) {
            this.dimensions = new ArrayList<>(dimensions);
            return this;
        }

        @SafeVarargs
        public final Builder dimensions(SdkMetric<String>... dimensions) {
            return dimensions(Arrays.asList(dimensions));
        }

        public Builder metricCategories(Collection<MetricCategory> metricCategories) {
            this.metricCategories = new ArrayList<>(metricCategories);
            return this;
        }

        public Builder metricCategories(MetricCategory... metricCategories) {
            return metricCategories(Arrays.asList(metricCategories));
        }

        public Builder metricLevel(MetricLevel metricLevel) {
            this.metricLevel = metricLevel;
            return this;
        }

        public Builder logGroupName(String logGroupName) {
            this.logGroupName = logGroupName;
            return this;
        }

        public Builder unitTest(boolean unitTest) {
            this.unitTest = unitTest;
            return this;
        }

        public EmfMetricPublisher build() {
            // if (this.logGroupName == null || this.logGroupName.trim().isEmpty()) {
            //     throw new IllegalStateException("logGroupName is required");
            // }
            return new EmfMetricPublisher(this);
        }

    }
}
