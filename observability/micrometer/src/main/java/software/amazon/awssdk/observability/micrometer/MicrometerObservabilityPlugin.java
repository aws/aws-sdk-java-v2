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

package software.amazon.awssdk.observability.micrometer;

import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.ObservationRegistry;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.observability.micrometer.internal.handlers.MicrometerExecutionInterceptor;
import software.amazon.awssdk.utils.Validate;

/**
 * A plugin for AWS SDK clients that adds Micrometer observability.
 * Provides metrics and tracing capabilities using Micrometer's Observation API.
 */
@SdkPublicApi
public final class MicrometerObservabilityPlugin implements SdkPlugin {
    private final ObservationRegistry observationRegistry;
    private final String metricNamespace;
    private final boolean enableMetrics;
    private final boolean enableTracing;
    private final Tags customTags;

    private MicrometerObservabilityPlugin(Builder builder) {
        this.observationRegistry = Validate.paramNotNull(builder.observationRegistry, "observationRegistry");
        this.metricNamespace = builder.metricNamespace != null ? builder.metricNamespace : "aws.sdk";
        this.enableMetrics = builder.enableMetrics;
        this.enableTracing = builder.enableTracing;
        this.customTags = builder.customTags != null ? builder.customTags : Tags.empty();
    }

    /**
     * Creates a builder for configuring a MicrometerObservabilityPlugin.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        config.overrideConfiguration(config.overrideConfiguration()
                                           .toBuilder()
                                           .addExecutionInterceptor(new MicrometerExecutionInterceptor(this))
                                           .build());
    }

    /**
     * Gets the Observation registry used by this plugin.
     *
     * @return The ObservationRegistry
     */
    public ObservationRegistry observationRegistry() {
        return observationRegistry;
    }

    /**
     * Gets the metric namespace used by this plugin.
     *
     * @return The metric namespace
     */
    public String metricNamespace() {
        return metricNamespace;
    }

    /**
     * Returns whether metrics collection is enabled.
     *
     * @return true if metrics are enabled, false otherwise
     */
    public boolean isMetricsEnabled() {
        return enableMetrics;
    }

    /**
     * Returns whether tracing is enabled.
     *
     * @return true if tracing is enabled, false otherwise
     */
    public boolean isTracingEnabled() {
        return enableTracing;
    }

    /**
     * Gets the custom tags applied to all observations.
     *
     * @return The custom tags
     */
    public Tags customTags() {
        return customTags;
    }

    /**
     * Builder for creating MicrometerObservabilityPlugin instances.
     */
    public static final class Builder {
        private ObservationRegistry observationRegistry;
        private String metricNamespace;
        private boolean enableMetrics = true;
        private boolean enableTracing = true;
        private Tags customTags;

        private Builder() {
        }

        /**
         * Sets the Micrometer ObservationRegistry to use.
         *
         * @param observationRegistry The observation registry
         * @return This builder
         */
        public Builder observationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        /**
         * Sets the namespace for metrics and spans.
         * Default: "aws.sdk"
         *
         * @param metricNamespace The metric namespace
         * @return This builder
         */
        public Builder metricNamespace(String metricNamespace) {
            this.metricNamespace = metricNamespace;
            return this;
        }

        /**
         * Enables or disables metrics collection.
         * Default: true
         *
         * @param enableMetrics Whether to enable metrics
         * @return This builder
         */
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }

        /**
         * Enables or disables tracing.
         * Default: true
         *
         * @param enableTracing Whether to enable tracing
         * @return This builder
         */
        public Builder enableTracing(boolean enableTracing) {
            this.enableTracing = enableTracing;
            return this;
        }

        /**
         * Sets custom tags to apply to all observations.
         *
         * @param customTags The custom tags
         * @return This builder
         */
        public Builder customTags(Tags customTags) {
            this.customTags = customTags;
            return this;
        }

        /**
         * Builds a new MicrometerObservabilityPlugin with the configured settings.
         *
         * @return A new MicrometerObservabilityPlugin instance
         */
        public MicrometerObservabilityPlugin build() {
            return new MicrometerObservabilityPlugin(this);
        }
    }
}
