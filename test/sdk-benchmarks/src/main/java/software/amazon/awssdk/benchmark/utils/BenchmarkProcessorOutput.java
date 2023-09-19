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

package software.amazon.awssdk.benchmark.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.benchmark.stats.SdkBenchmarkResult;

/**
 * The output object of the benchmark processor. This contains the results of the all the benchmarks that were run, and the
 * list of benchmarks that failed.
 */
public final class BenchmarkProcessorOutput {
    private final Map<String, SdkBenchmarkResult> benchmarkResults;
    private final List<String> failedBenchmarks;

    @JsonCreator
    public BenchmarkProcessorOutput(Map<String, SdkBenchmarkResult> benchmarkResults, List<String> failedBenchmarks) {
        this.benchmarkResults = benchmarkResults;
        this.failedBenchmarks = failedBenchmarks;
    }

    public Map<String, SdkBenchmarkResult> getBenchmarkResults() {
        return benchmarkResults;
    }

    public List<String> getFailedBenchmarks() {
        return failedBenchmarks;
    }
}
