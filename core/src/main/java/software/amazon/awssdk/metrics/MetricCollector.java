/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics;


/**
 * Metric Collector SPI.  This is a class instead of interface so in case of
 * adding new methods in the future we can do that safetly without breaking
 * any existing clients that implements this SPI.
 */
public abstract class MetricCollector {
    /** A convenient instance of a no-op request metric collector. */
    public static final MetricCollector NONE = new MetricCollector() {
        @Override
        public boolean start() {
            return true;
        }

        @Override
        public void stop() {

        }

        /** Always returns false. */
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public RequestMetricCollector getRequestMetricCollector() {
            return RequestMetricCollector.NONE;
        }

        @Override
        public ServiceMetricCollector getServiceMetricCollector() {
            return ServiceMetricCollector.NONE;
        }
    };

    /**
     * Starts the request metric collector.
     *
     * @return true if the collector is successfully started; false otherwise.
     */
    public abstract boolean start();

    /**
     * Stops the request metric collector.
     */
    public abstract void stop();

    /** Returns true if this collector is enabled; false otherwise. */
    public abstract boolean isEnabled();

    public abstract RequestMetricCollector getRequestMetricCollector();

    public abstract ServiceMetricCollector getServiceMetricCollector();

    /**
     * Can be used to serve as a factory for the request metric collector.
     */
    public static interface Factory {
        /**
         * Returns an instance of the collector; or null if if failed to create
         * one.
         */
        public MetricCollector getInstance();
    }
}
