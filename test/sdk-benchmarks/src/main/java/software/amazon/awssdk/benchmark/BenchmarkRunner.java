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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.utils.Logger;

public class BenchmarkRunner {

    private static final List<String> BENCHMARKS_TO_RUN =
        Arrays.asList("Ec2ProtocolBenchmark", "JsonProtocolBenchmark", "QueryProtocolBenchmark", "XmlProtocolBenchmark",
                      "V2OptimizedClientCreationBenchmark", "V1ClientCreationBenchmark", "V2DefaultClientCreationBenchmark",
                      "ApacheHttpClientBenchmark", "UrlConnectionHttpClientClientBenchmark");
    private static final Logger log = Logger.loggerFor(BenchmarkRunner.class);
    private final List<String> benchmarksToRun;

    private BenchmarkRunner(List<String> benchmarksToRun) {
        this.benchmarksToRun = benchmarksToRun;
    }

    public static void main(String... args) throws RunnerException, IOException {
        BenchmarkRunner runner = new BenchmarkRunner(BENCHMARKS_TO_RUN);
        runner.runBenchmark();
    }

    private void runBenchmark() throws RunnerException {
        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder();

        benchmarksToRun.forEach(optionsBuilder::include);

        log.info(() -> "benchmarks to run " + benchmarksToRun);

        Collection<RunResult> results = new Runner(optionsBuilder.build()).run();
        //TODO: process the results and compare with baseline, consider using Statistics#isDifferent
    }
}
