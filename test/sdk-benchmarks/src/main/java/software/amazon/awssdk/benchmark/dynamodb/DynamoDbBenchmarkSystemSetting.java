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

package software.amazon.awssdk.benchmark.dynamodb;

import software.amazon.awssdk.utils.SystemSetting;

/**
 * Environment variables and system properties for DynamoDB Tier D live benchmarks.
 *
 * <p>System properties take precedence over environment variables (SDK {@link SystemSetting} rule).
 */
public enum DynamoDbBenchmarkSystemSetting implements SystemSetting {

    /**
     * Explicit opt-in for live DynamoDB benchmarks ({@code true} required).
     */
    LIVE_OPT_IN("dynamodb.benchmark.live", "DYNAMODB_BENCHMARK_LIVE"),

    /**
     * Optional region override for live benchmarks.
     */
    REGION("dynamodb.benchmark.region", "DYNAMODB_BENCHMARK_REGION"),

    /**
     * Whether to attach the lightweight retry observer interceptor (default {@code true}).
     */
    LIVE_RETRY_OBSERVE("dynamodb.benchmark.live.retryObserve", "DYNAMODB_BENCHMARK_LIVE_RETRY_OBSERVE", "true");

    private final String property;
    private final String environmentVariable;
    private final String defaultValue;

    DynamoDbBenchmarkSystemSetting(String property, String environmentVariable) {
        this(property, environmentVariable, null);
    }

    DynamoDbBenchmarkSystemSetting(String property, String environmentVariable, String defaultValue) {
        this.property = property;
        this.environmentVariable = environmentVariable;
        this.defaultValue = defaultValue;
    }

    @Override
    public String property() {
        return property;
    }

    @Override
    public String environmentVariable() {
        return environmentVariable;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
