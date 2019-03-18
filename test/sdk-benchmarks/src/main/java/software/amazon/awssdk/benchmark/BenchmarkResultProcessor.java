/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import software.amazon.awssdk.utils.Logger;


/**
 * Process benchmark score, compare the result with the baseline score and return
 * the names of the benchmarks that exceed the baseline score.
 */
class BenchmarkResultProcessor {

    private static final Logger log = Logger.loggerFor(BenchmarkResultProcessor.class);

    private final Map<String, Double> benchmarkIdToBaselineScore;
    private List<String> failedBenchmarkNames = new ArrayList<>();

    BenchmarkResultProcessor(List<BenchmarkScore> benchmarkScores) {
        this.benchmarkIdToBaselineScore = constructBenchmarkIdToScoreMap(benchmarkScores);
    }

    /**
     * Process benchmark results
     * @param results the results of the benchmark
     * @return the benchmark name that failed the test
     */
    List<String> processBenchmarkResult(Collection<RunResult> results) {

        for (RunResult result : results) {
            String benchmarkId = retrieveBenchmarkId(result);
            Double baseline = benchmarkIdToBaselineScore.getOrDefault(benchmarkId, Double.MAX_VALUE);

            double calibratedScore = calibrateScore(result.getPrimaryResult());
            if (calibratedScore > baseline) {
                failedBenchmarkNames.add(benchmarkId);
            }
        }
        return failedBenchmarkNames;
    }

    /**
     * Calibrate score if needed. Ignoring the result if the score error is
     * greater than the result score.
     */
    private double calibrateScore(Result result) {
        if (Double.isNaN(result.getScoreError())) {
            return result.getScore();
        }

        if (result.getScoreError() > result.getScore()) {
            log.warn(() -> "Ignoring the result since it's not accurate: " + result.getLabel());
            return Double.NaN;
        }

        return result.getScore() - result.getScoreError();
    }

    /**
     *  Retrieve BenchmarkId from the runResult.
     */
    private String retrieveBenchmarkId(RunResult runResult) {
        BenchmarkParams params = runResult.getParams();
        String benchmark = params.getBenchmark();

        String[] split = benchmark.split("\\.");

        String className = split[split.length - 2];
        String benchmarkMethodName = split[split.length - 1];

        StringJoiner stringJoiner = new StringJoiner(".").add(className).add(benchmarkMethodName);

        Collection<String> paramsKeys = params.getParamsKeys();

        if (!CollectionUtils.isNullOrEmpty(paramsKeys)) {
            String paramKey = paramsKeys.iterator().next();
            String paramValue = params.getParam(paramKey);
            stringJoiner.add(paramValue);
        }
        return stringJoiner.toString();
    }

    private Map<String, Double> constructBenchmarkIdToScoreMap(List<BenchmarkScore> benchmarkScores) {
        Map<String, Double> benchmarkIdToScore = new HashMap<>();
        for (BenchmarkScore score : benchmarkScores) {
            String id = score.getBenchmark();
            if (score.getParameter() != null) {
                id += "." + score.getParameter();
            }
            benchmarkIdToScore.put(id, score.getScore());
        }
        return benchmarkIdToScore;
    }
}
