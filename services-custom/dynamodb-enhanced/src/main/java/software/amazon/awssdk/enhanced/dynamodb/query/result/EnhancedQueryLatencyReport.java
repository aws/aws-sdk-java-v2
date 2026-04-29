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

package software.amazon.awssdk.enhanced.dynamodb.query.result;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Captures basic latency measurements and DynamoDB request counts for enhanced query execution.
 * <p>
 * Timing values are expressed in milliseconds. Request counts reflect underlying DynamoDB {@code Query} and {@code Scan} API
 * round-trips (including pagination pages).
 */
@SdkInternalApi
public class EnhancedQueryLatencyReport {

    private final long baseQueryMs;
    private final long joinedLookupsMs;
    private final long inMemoryProcessingMs;
    private final long totalMs;
    private final long baseQueryRequestCount;
    private final long baseScanRequestCount;
    private final long joinedQueryRequestCount;
    private final long joinedScanRequestCount;
    private final double baseQueryRcu;
    private final double baseScanRcu;
    private final double joinedQueryRcu;
    private final double joinedScanRcu;

    public EnhancedQueryLatencyReport(long baseQueryMs,
                                      long joinedLookupsMs,
                                      long inMemoryProcessingMs,
                                      long totalMs) {
        this(baseQueryMs, joinedLookupsMs, inMemoryProcessingMs, totalMs,
             0L, 0L, 0L, 0L, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    public EnhancedQueryLatencyReport(long baseQueryMs,
                                      long joinedLookupsMs,
                                      long inMemoryProcessingMs,
                                      long totalMs,
                                      long baseQueryRequestCount,
                                      long baseScanRequestCount,
                                      long joinedQueryRequestCount,
                                      long joinedScanRequestCount) {
        this(baseQueryMs, joinedLookupsMs, inMemoryProcessingMs, totalMs,
             baseQueryRequestCount, baseScanRequestCount, joinedQueryRequestCount, joinedScanRequestCount,
             0.0d, 0.0d, 0.0d, 0.0d);
    }

    public EnhancedQueryLatencyReport(long baseQueryMs,
                                      long joinedLookupsMs,
                                      long inMemoryProcessingMs,
                                      long totalMs,
                                      long baseQueryRequestCount,
                                      long baseScanRequestCount,
                                      long joinedQueryRequestCount,
                                      long joinedScanRequestCount,
                                      double baseQueryRcu,
                                      double baseScanRcu,
                                      double joinedQueryRcu,
                                      double joinedScanRcu) {
        this.baseQueryMs = baseQueryMs;
        this.joinedLookupsMs = joinedLookupsMs;
        this.inMemoryProcessingMs = inMemoryProcessingMs;
        this.totalMs = totalMs;
        this.baseQueryRequestCount = baseQueryRequestCount;
        this.baseScanRequestCount = baseScanRequestCount;
        this.joinedQueryRequestCount = joinedQueryRequestCount;
        this.joinedScanRequestCount = joinedScanRequestCount;
        this.baseQueryRcu = baseQueryRcu;
        this.baseScanRcu = baseScanRcu;
        this.joinedQueryRcu = joinedQueryRcu;
        this.joinedScanRcu = joinedScanRcu;
    }

    public long baseQueryMs() {
        return baseQueryMs;
    }

    public long joinedLookupsMs() {
        return joinedLookupsMs;
    }

    public long inMemoryProcessingMs() {
        return inMemoryProcessingMs;
    }

    public long totalMs() {
        return totalMs;
    }

    /**
     * Number of DynamoDB {@code Query} calls issued against the base table (including pagination).
     */
    public long baseQueryRequestCount() {
        return baseQueryRequestCount;
    }

    /**
     * Number of DynamoDB {@code Scan} calls issued against the base table (including pagination).
     */
    public long baseScanRequestCount() {
        return baseScanRequestCount;
    }

    /**
     * Number of DynamoDB {@code Query} calls issued against the joined table (including pagination and per-key lookups).
     */
    public long joinedQueryRequestCount() {
        return joinedQueryRequestCount;
    }

    /**
     * Number of DynamoDB {@code Scan} calls issued against the joined table (including parallel segment scans).
     */
    public long joinedScanRequestCount() {
        return joinedScanRequestCount;
    }

    /**
     * Consumed capacity units for base-table {@code Query} calls.
     */
    public double baseQueryRcuConsumed() {
        return baseQueryRcu;
    }

    /**
     * Consumed capacity units for base-table {@code Scan} calls.
     */
    public double baseScanRcuConsumed() {
        return baseScanRcu;
    }

    /**
     * Consumed capacity units for joined-table {@code Query} calls.
     */
    public double joinedQueryRcuConsumed() {
        return joinedQueryRcu;
    }

    /**
     * Consumed capacity units for joined-table {@code Scan} calls.
     */
    public double joinedScanRcuConsumed() {
        return joinedScanRcu;
    }

    /**
     * Sum of all {@link #baseQueryRequestCount()}, {@link #baseScanRequestCount()}, {@link #joinedQueryRequestCount()}, and
     * {@link #joinedScanRequestCount()}.
     */
    public long totalDynamoDbRequestCount() {
        return baseQueryRequestCount + baseScanRequestCount + joinedQueryRequestCount + joinedScanRequestCount;
    }

    /**
     * Sum of all consumed capacity units tracked across base/join and query/scan categories.
     */
    public double totalRcuConsumed() {
        return baseQueryRcu + baseScanRcu + joinedQueryRcu + joinedScanRcu;
    }
}
