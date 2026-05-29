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

package software.amazon.awssdk.enhanced.dynamodb.query.engine;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryLatencyReport;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

/**
 * Counts underlying DynamoDB {@code Query} and {@code Scan} API round-trips (including pagination) for one {@code enhancedQuery}
 * execution. Safe to increment from parallel join/aggregation workers.
 */
@SdkInternalApi
public final class EnhancedQueryExecutionStats {

    private final AtomicLong baseQueryCount = new AtomicLong();
    private final AtomicLong baseScanCount = new AtomicLong();
    private final AtomicLong joinedQueryCount = new AtomicLong();
    private final AtomicLong joinedScanCount = new AtomicLong();
    private final DoubleAdder baseQueryRcu = new DoubleAdder();
    private final DoubleAdder baseScanRcu = new DoubleAdder();
    private final DoubleAdder joinedQueryRcu = new DoubleAdder();
    private final DoubleAdder joinedScanRcu = new DoubleAdder();

    public void addBaseQuery() {
        baseQueryCount.incrementAndGet();
    }

    public void addBaseScan() {
        baseScanCount.incrementAndGet();
    }

    public void addJoinedQuery() {
        joinedQueryCount.incrementAndGet();
    }

    public void addJoinedScan() {
        joinedScanCount.incrementAndGet();
    }

    public void addBaseQueryRcu(double capacityUnits) {
        baseQueryRcu.add(capacityUnits);
    }

    public void addBaseScanRcu(double capacityUnits) {
        baseScanRcu.add(capacityUnits);
    }

    public void addJoinedQueryRcu(double capacityUnits) {
        joinedQueryRcu.add(capacityUnits);
    }

    public void addJoinedScanRcu(double capacityUnits) {
        joinedScanRcu.add(capacityUnits);
    }

    public void addConsumedCapacity(ConsumedCapacity consumedCapacity, boolean base, boolean query) {
        if (consumedCapacity == null || consumedCapacity.capacityUnits() == null) {
            return;
        }

        double capacityUnits = consumedCapacity.capacityUnits();
        if (base) {
            if (query) {
                addBaseQueryRcu(capacityUnits);
            } else {
                addBaseScanRcu(capacityUnits);
            }
        } else if (query) {
            addJoinedQueryRcu(capacityUnits);
        } else {
            addJoinedScanRcu(capacityUnits);
        }
    }

    public long baseQueryRequestCount() {
        return baseQueryCount.get();
    }

    public long baseScanRequestCount() {
        return baseScanCount.get();
    }

    public long joinedQueryRequestCount() {
        return joinedQueryCount.get();
    }

    public long joinedScanRequestCount() {
        return joinedScanCount.get();
    }

    public double baseQueryRcuConsumed() {
        return baseQueryRcu.sum();
    }

    public double baseScanRcuConsumed() {
        return baseScanRcu.sum();
    }

    public double joinedQueryRcuConsumed() {
        return joinedQueryRcu.sum();
    }

    public double joinedScanRcuConsumed() {
        return joinedScanRcu.sum();
    }

    public long totalDynamoDbRequestCount() {
        return baseQueryRequestCount() + baseScanRequestCount() + joinedQueryRequestCount() + joinedScanRequestCount();
    }

    public double totalRcuConsumed() {
        return baseQueryRcuConsumed() + baseScanRcuConsumed() + joinedQueryRcuConsumed() + joinedScanRcuConsumed();
    }

    public EnhancedQueryLatencyReport toLatencyReport(long totalMs) {
        return new EnhancedQueryLatencyReport(0L, 0L, 0L, totalMs,
                                              baseQueryRequestCount(), baseScanRequestCount(),
                                              joinedQueryRequestCount(), joinedScanRequestCount(),
                                              baseQueryRcuConsumed(), baseScanRcuConsumed(),
                                              joinedQueryRcuConsumed(), joinedScanRcuConsumed());
    }
}
