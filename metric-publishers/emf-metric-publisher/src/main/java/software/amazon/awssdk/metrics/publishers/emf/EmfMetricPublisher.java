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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.emf.internal.EmfMetricConfiguration;
import software.amazon.awssdk.metrics.publishers.emf.internal.MetricEmfConverter;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A metric publisher implementation that converts metrics into CloudWatch Embedded Metric Format (EMF).
 * EMF allows metrics to be published through CloudWatch Logs using a structured JSON format, which
 * CloudWatch automatically extracts and processes into metrics.
 *
 * <p>
 * This publisher is particularly well-suited for serverless environments like AWS Lambda and container
 * environments like Amazon ECS that have built-in integration with CloudWatch Logs. Using EMF eliminates
 * the need for separate metric publishing infrastructure as metrics are automatically extracted from
 * log entries.
 * </p>
 *
 * <p>
 * The EMF publisher converts metric collections into JSON-formatted log entries that conform to the
 * CloudWatch EMF specification. These logs are written to the "/aws/emf/metrics" log group by default.
 * CloudWatch automatically processes these logs to generate corresponding metrics that can be used for
 * monitoring and alerting.
 * </p>
 *
 * @snippet
 * // Create a CloudWatchMetricPublisher using a custom namespace.
 * MetricPublisher emfMetricPublisher = EmfMetricPublisher.builder()
 *                                                     .logGroupName("myLogGroupName")
 *                                                     .namespace("myApplication")
 *                                                     .build();
 *
 * @see MetricPublisher The base interface for metric publishers
 * @see MetricCollection For the collection of metrics to be published
 * @see EmfMetricConfiguration For configuration options
 * @see MetricEmfConverter  For the conversion logic
 *
 */

@ThreadSafe
@Immutable
@SdkPublicApi
public final class EmfMetricPublisher implements MetricPublisher {

    private static final Logger logger = Logger.loggerFor(EmfMetricPublisher.class);
    private final MetricEmfConverter metricConverter;


    private EmfMetricPublisher(Builder builder) {
        Validate.paramNotNull(builder.logGroupName, "logGroupName");

        EmfMetricConfiguration config = new EmfMetricConfiguration.Builder()
            .namespace(builder.namespace)
            .logGroupName(builder.logGroupName)
            .dimensions(builder.dimensions)
            .metricLevel(builder.metricLevel)
            .metricCategories(builder.metricCategories)
            .build();

        this.metricConverter = new MetricEmfConverter(config);
    }


    public static Builder builder() {
        return new Builder();
    }


    @Override
    public void publish(MetricCollection metricCollection) {
        if (metricCollection == null) {
            logger.warn(() -> "Null metric collection passed to the publisher");
            return;
        }
        try {
            List<String> emfStrings = metricConverter.convertMetricCollectionToEmf(metricCollection);
            for (String emfString : emfStrings) {
                logger.info(() -> emfString);
            }
        } catch (Exception e) {
            logger.warn(() -> "Failed to log metrics in EMF format", e);
        }
    }

    @Override
    public void close() {
    }

    public static final class Builder {
        private String namespace;
        private String logGroupName;
        private Collection<SdkMetric<String>> dimensions;
        private Collection<MetricCategory> metricCategories;
        private MetricLevel metricLevel;

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
         * <p>If this is not specified,  {@link CoreMetric#SERVICE_ID} and {@link CoreMetric#OPERATION_NAME} will be used.
         */
        public Builder dimensions(Collection<SdkMetric<String>> dimensions) {
            this.dimensions = new ArrayList<>(dimensions);
            return this;
        }

        /**
         * @see #dimensions(SdkMetric[])
         */
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
         * Configure the LogGroupName key that will be put into the emf log to this publisher. This is a required key for
         * using the CloudWatch agent to send embedded metric format logs that tells the agent which log group to use.
         *
         * <p> This field is required and must not be null or empty.
         * @throws NullPointerException if logGroupName is null
         */
        public Builder logGroupName(String logGroupName) {
            this.logGroupName = logGroupName;
            return this;
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
         * Build a {@link EmfMetricPublisher} using the configuration currently configured on this publisher.
         */
        public EmfMetricPublisher build() {
            return new EmfMetricPublisher(this);
        }

    }
}