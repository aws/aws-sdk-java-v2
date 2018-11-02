/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.benchmark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import software.amazon.awssdk.benchmark.coldstart.ClientCreationBenchmark;
import software.amazon.awssdk.utils.ImmutableMap;


class BenchmarkResultProcessor {

    private static final Map<String, Double> BENCHMARK_NAME_TO_BASELINE_SCORE =
        ImmutableMap.of(ClientCreationBenchmark.class.getSimpleName() + ".defaultClient", 10.0);
    private List<String> failedBenchmarkNames = new ArrayList<>();

    List<String> processBenchmarkResult(Collection<RunResult> results) {

        for (RunResult result : results) {
            String benchmarkName = retrieveBenchmarkName(result);
            Double baseline = BENCHMARK_NAME_TO_BASELINE_SCORE.getOrDefault(benchmarkName, Double.MAX_VALUE);

            double calibratedScore = calibrateScore(result.getPrimaryResult());
            if (calibratedScore > baseline) {
                failedBenchmarkNames.add(benchmarkName);
            }
        }
        return failedBenchmarkNames;
    }

    private double calibrateScore(Result result) {
        return result.getScore() - result.getScoreError();
    }

    private String retrieveBenchmarkName(RunResult runResult) {
        BenchmarkParams params = runResult.getParams();
        String benchmark = params.getBenchmark();

        String[] split = benchmark.split("\\.");

        String className = split[split.length - 2];
        String benchmarkMethodName = split[split.length - 1];

        return new StringJoiner(".").add(className).add(benchmarkMethodName).toString();
    }
}
