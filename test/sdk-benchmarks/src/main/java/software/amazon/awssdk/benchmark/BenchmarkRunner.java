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


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {

    private static final String BENCHMARK_ARG = "-benchmarks";

    public static void main(String... args) throws RunnerException {
        if (args.length == 0) {
            System.out.println("No Argument");
            System.exit(0);
        }

        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder();
        Map<String, Double> benchmarkIdentifierToBaselineScore = parseBenchmarkArgs(args);

        Set<String> benchNames = parseBenchmarkNames(benchmarkIdentifierToBaselineScore);

        System.out.println("benchmarks to include " + benchNames);

        benchNames.forEach(optionsBuilder::include);

        Collection<RunResult> results = new Runner(optionsBuilder.build()).run();
        BenchmarkResultProcessor processor = new BenchmarkResultProcessor(benchmarkIdentifierToBaselineScore);
        List<String> failedBenchmarkResults = processor.processBenchmarkResult(results);

        if (!failedBenchmarkResults.isEmpty()) {
            throw new RuntimeException("Failed the benchmark regression " + failedBenchmarkResults);
        }

        System.out.println("Passed the performance regression tests!");
    }

    /**
     * {ApiCallHttpClientBenchmark.run.UrlConnectionHttpClient=3.0} -> [ApiCallHttpClientBenchmark]
     */
    private static Set<String> parseBenchmarkNames(Map<String, Double> benchmarkNamesToBaselineScore) {
        Set<String> benchNames = new HashSet<>();
        benchmarkNamesToBaselineScore.keySet().forEach(b -> {
            String[] split = b.split("\\.");
            benchNames.add(split[0]);
        });
        return benchNames;
    }

    /**
     * {ApiCallHttpClientBenchmark.run.UrlConnectionHttpClient=3.0}
     */
    private static Map<String, Double> parseBenchmarkArgs(String[] args) {
        Iterator<String> iterator = Arrays.asList(args).iterator();
        Map<String, Double> benchmarkNamesToBaselineScore = new HashMap<>();

        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.equalsIgnoreCase(BENCHMARK_ARG)) {
                String value = iterator.next();

                String[] splitedValue = value.split(",");

                for (String benchMarkNameToBaseline : splitedValue) {
                    String[] split = benchMarkNameToBaseline.split(":");
                    benchmarkNamesToBaselineScore.put(split[0], Double.parseDouble(split[1]));
                }

                return benchmarkNamesToBaselineScore;
            }
        }

        return benchmarkNamesToBaselineScore;
    }
}
