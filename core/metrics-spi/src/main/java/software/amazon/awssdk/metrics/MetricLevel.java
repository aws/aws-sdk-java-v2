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

package software.amazon.awssdk.metrics;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The {@code MetricLevel} associated with a {@link SdkMetric}, similar to log levels, defines the 'scenario' in which the metric
 * is useful. This makes it easy to reduce the cost of metric publishing (e.g. by setting it to {@link #INFO}), and then increase
 * it when additional data level is needed for debugging purposes (e.g. by setting it to {@link #TRACE}.
 */
@SdkPublicApi
public enum MetricLevel {
    /**
     * The metric level that includes every other metric level, as well as some highly-technical metrics that may only be useful
     * in very specific performance or failure scenarios.
     */
    TRACE,

    /**
     * The "default" metric level that includes metrics that are useful for identifying <i>why</i> errors or performance issues
     * are occurring within the SDK. This excludes technical metrics that are only useful in very specific performance or failure
     * scenarios.
     */
    INFO,

    /**
     * Includes metrics that report <i>when</i> API call errors are occurring within the SDK. This <b>does not</b> include all
     * of the information that may be generally useful when debugging <i>why</i> errors are occurring (e.g. latency).
     */
    ERROR;

    public boolean includesLevel(MetricLevel level) {
        return this.compareTo(level) <= 0;
    }
}
