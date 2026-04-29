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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Structured logging for enhanced query executions. Uses a dedicated logger name so operators can enable {@code INFO} for
 * {@value #LOGGER_NAME} without raising verbosity for the rest of the Enhanced Client.
 */
@SdkInternalApi
public final class EnhancedQueryTelemetry {

    /**
     * SLF4J logger name; set to {@code INFO} to record per-execution request counts and wall-clock time.
     */
    public static final String LOGGER_NAME = "software.amazon.awssdk.enhanced.dynamodb.query.telemetry";

    private static final Logger TELEMETRY_LOG = Logger.loggerFor(LOGGER_NAME);

    private EnhancedQueryTelemetry() {
    }

    /**
     * Logs one line per completed enhanced query: wall-clock time and DynamoDB {@code Query}/{@code Scan} round-trip counts
     * (including pagination and per-join-key lookups). Combine with table billing mode and item sizes for cost analysis.
     */
    public static void logExecutionComplete(EnhancedQueryExecutionStats stats, long totalWallClockMs, boolean async) {
        TELEMETRY_LOG.info(() -> {
            long total = stats.totalDynamoDbRequestCount();
            long bq = stats.baseQueryRequestCount();
            long bs = stats.baseScanRequestCount();
            long jq = stats.joinedQueryRequestCount();
            long js = stats.joinedScanRequestCount();
            double totalRcu = stats.totalRcuConsumed();
            double bqRcu = stats.baseQueryRcuConsumed();
            double bsRcu = stats.baseScanRcuConsumed();
            double jqRcu = stats.joinedQueryRcuConsumed();
            double jsRcu = stats.joinedScanRcuConsumed();
            String mode = async ? "async" : "sync";
            return String.format(
                "EnhancedQuery (%s) completed: wallClockMs=%d, dynamoDbRoundTrips={total=%d, baseQuery=%d, baseScan=%d, "
                + "joinedQuery=%d, joinedScan=%d}, consumedRcu={total=%.3f, baseQuery=%.3f, baseScan=%.3f, "
                + "joinedQuery=%.3f, joinedScan=%.3f}.",
                mode, totalWallClockMs, total, bq, bs, jq, js, totalRcu, bqRcu, bsRcu, jqRcu, jsRcu);
        });
    }
}
