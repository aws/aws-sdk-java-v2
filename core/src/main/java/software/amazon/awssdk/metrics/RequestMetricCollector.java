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

import software.amazon.awssdk.Request;

/**
 * A service provider interface that can be used to implement an AWS SDK
 * request/response metric collector.
 *
 * @see AwsSdkMetrics
 */
public abstract class RequestMetricCollector {
    /** A convenient instance of a no-op request metric collector. */
    public static final RequestMetricCollector NONE = new RequestMetricCollector() {
        @Override
        public void collectMetrics(Request<?> request, Object response) {
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    };

    /**
     * Used to collect the metric at the end of a request/response cycle.
     *
     * @see Request#getAwsRequestMetrics()
     */
    public abstract void collectMetrics(Request<?> request, Object response);

    public boolean isEnabled() {
        return true;
    }

    /**
     * Can be used to serve as a factory for the request metric collector.
     */
    public interface Factory {
        /**
         * Returns an instance of the collector; or null if if failed to create
         * one.
         */
        RequestMetricCollector getRequestMetricCollector();
    }
}
