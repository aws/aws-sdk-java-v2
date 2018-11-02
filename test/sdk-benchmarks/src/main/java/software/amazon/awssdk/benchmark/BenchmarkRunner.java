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


import java.util.Collection;
import java.util.List;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class BenchmarkRunner {

    public static void main(String... args) throws RunnerException {
        if (args.length == 0) {
            System.out.println("no argument");
            System.exit(0);
        }

        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder();
        //optionsBuilder.include(ClientCreationBenchmark.class.getSimpleName());

        for(String arg : args) {
            optionsBuilder.include(arg).verbosity(VerboseMode.EXTRA);
        }

        Collection<RunResult> results = new Runner(optionsBuilder.build()).run();
        BenchmarkResultProcessor processor = new BenchmarkResultProcessor();
        List<String> failedBenchmarkResults = processor.processBenchmarkResult(results);

        if (!failedBenchmarkResults.isEmpty()) {
            throw new RuntimeException("Failed the benchmark regression " + failedBenchmarkResults);
        } else {
            System.out.println("Passing the performance regression tests!");
        }
    }
}
