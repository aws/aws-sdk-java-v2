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

package software.amazon.awssdk.benchmark;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.OBJECT_MAPPER;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.compare;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.util.Statistics;
import software.amazon.awssdk.benchmark.stats.SdkBenchmarkParams;
import software.amazon.awssdk.benchmark.stats.SdkBenchmarkResult;
import software.amazon.awssdk.benchmark.stats.SdkBenchmarkStatistics;
import software.amazon.awssdk.utils.Logger;


/**
 * Process benchmark score, compare the result with the baseline score and return
 * the names of the benchmarks that exceed the baseline score.
 */
class BenchmarkResultProcessor {

    private static final Logger log = Logger.loggerFor(BenchmarkResultProcessor.class);
    private static final double TOLERANCE_LEVEL = 0.05;

    private Map<String, SdkBenchmarkResult> baseline;

    private List<String> failedBenchmarkIds = new ArrayList<>();

    BenchmarkResultProcessor() {
        try {
            URL file = BenchmarkResultProcessor.class.getResource("baseline.json");
            List<SdkBenchmarkResult> baselineResults =
                OBJECT_MAPPER.readValue(file, new TypeReference<List<SdkBenchmarkResult>>() {});

            baseline = baselineResults.stream().collect(Collectors.toMap(SdkBenchmarkResult::getId, b -> b));
        } catch (Exception e) {
            throw new RuntimeException("Not able to retrieve baseline result.", e);
        }
    }

    /**
     * Process benchmark results
     *
     * @param results the results of the benchmark
     * @return the benchmark Id that failed the regression
     */
    List<String> processBenchmarkResult(Collection<RunResult> results) {
        List<SdkBenchmarkResult> currentData = new ArrayList<>();
        for (RunResult result : results) {
            String benchmarkId = getBenchmarkId(result.getParams());

            SdkBenchmarkResult baselineResult = baseline.get(benchmarkId);
            SdkBenchmarkResult sdkBenchmarkData = constructSdkBenchmarkResult(result);

            if (baselineResult == null) {
                log.warn(() -> {
                    String benchmarkResultJson = null;
                    try {
                        benchmarkResultJson = OBJECT_MAPPER.writeValueAsString(sdkBenchmarkData);
                    } catch (IOException e) {
                        log.error(() -> "Unable to serialize result data to JSON");
                    }
                    return String.format("Unable to find the baseline for %s. Skipping regression validation. " +
                            "Results were: %s", benchmarkId, benchmarkResultJson);
                });
                continue;
            }

            currentData.add(sdkBenchmarkData);

            if (!validateBenchmarkResult(sdkBenchmarkData, baselineResult)) {
                failedBenchmarkIds.add(benchmarkId);
            }
        }

        log.info(() -> "Current result: " + serializeResult(currentData));
        return failedBenchmarkIds;
    }

    private SdkBenchmarkResult constructSdkBenchmarkResult(RunResult runResult) {
        Statistics statistics = runResult.getPrimaryResult().getStatistics();

        SdkBenchmarkStatistics sdkBenchmarkStatistics = new SdkBenchmarkStatistics(statistics);
        SdkBenchmarkParams sdkBenchmarkParams = new SdkBenchmarkParams(runResult.getParams());

        return new SdkBenchmarkResult(getBenchmarkId(runResult.getParams()),
                                      sdkBenchmarkParams,
                                      sdkBenchmarkStatistics);
    }

    /**
     * Validate benchmark result by comparing it with baseline result statistically.
     *
     * @param baseline the baseline result
     * @param currentResult current result
     * @return true if current result is equal to or better than the baseline result statistically, false otherwise.
     */
    private boolean validateBenchmarkResult(SdkBenchmarkResult currentResult, SdkBenchmarkResult baseline) {
        if (!validateBenchmarkParams(currentResult.getParams(), baseline.getParams())) {
            log.warn(() -> "Baseline result and current result are not comparable due to running from different environments."
                           + "Skipping validation for " + currentResult.getId());
            return true;
        }

        int comparison = compare(currentResult.getStatistics(), baseline.getStatistics());
        log.debug(() -> "comparison result for " + baseline.getId() + " is " + comparison);

        switch (currentResult.getParams().getMode()) {
            case Throughput:
                if (comparison <= 0) {
                    return true;
                }
                return withinTolerance(currentResult.getStatistics().getMean(), baseline.getStatistics().getMean());
            case SampleTime:
            case AverageTime:
            case SingleShotTime:
                if (comparison >= 0) {
                    return true;
                }
                return withinTolerance(currentResult.getStatistics().getMean(), baseline.getStatistics().getMean());
            default:
                log.warn(() -> "Unsupported mode, skipping " + currentResult.getId());
                return true;
        }
    }

    private boolean withinTolerance(double current, double baseline) {
        boolean positive = Math.abs(current - baseline) / baseline < TOLERANCE_LEVEL;
        log.info(() -> "current: " + current + " baseline: " + baseline +
                       "The relative difference is within tolerance? " + positive);
        return positive;
    }

    private String getBenchmarkId(BenchmarkParams params) {
        return params.id().replaceFirst("software.amazon.awssdk.benchmark.", "");
    }

    private boolean validateBenchmarkParams(SdkBenchmarkParams current, SdkBenchmarkParams baseline) {
        if (!Objects.equals(current.getJdkVersion(), baseline.getJdkVersion())) {
            log.warn(() -> "The current benchmark result was generated from a different Jdk version than the one of the "
                           + "baseline, so the results might not be comparable");
            return true;
        }

        return Objects.equals(current.getMode(), baseline.getMode());
    }

    private String serializeResult(List<SdkBenchmarkResult> currentData) {
        try {
            return OBJECT_MAPPER.writeValueAsString(currentData);
        } catch (JsonProcessingException e) {
            log.error(() -> "Failed to serialize current result", e);
        }
        return null;
    }
}
