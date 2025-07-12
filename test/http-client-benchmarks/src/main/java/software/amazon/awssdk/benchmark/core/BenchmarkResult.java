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

package software.amazon.awssdk.benchmark.core;


import java.time.Instant;

/**
 * Holds the results of a single benchmark run.
 */
public class BenchmarkResult {
    private final String clientType;

    private final String benchmarkName;

    private final double throughput;

    private final double avgLatency;

    private final double p99Latency;

    private final int threadCount;

    private final Instant timestamp;

    public BenchmarkResult(String clientType, String benchmarkName,
                           double throughput, double avgLatency,
                           double p99Latency, int threadCount) {
        this.clientType = clientType;
        this.benchmarkName = benchmarkName;
        this.throughput = throughput;
        this.avgLatency = avgLatency;
        this.p99Latency = p99Latency;
        this.threadCount = threadCount;
        this.timestamp = Instant.now();
    }

    // Getters
    public String getClientType() { return clientType; }
    public String getBenchmarkName() { return benchmarkName; }
    public double getThroughput() { return throughput; }
    public double getAvgLatency() { return avgLatency; }
    public double getP99Latency() { return p99Latency; }
    public int getThreadCount() { return threadCount; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("%s.%s: %.2f ops/sec, avg=%.2fms, p99=%.2fms",
                             clientType, benchmarkName, throughput, avgLatency, p99Latency);
    }
}
