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

package software.amazon.awssdk.benchmark.coldstart;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Measures how long {@link SdkWarmUp#prime(Class[])} takes to run: the one-time upfront work an application pays
 * during initialization to get the first-call saving that {@link V2ColdStartAfterWarmUpBenchmark} shows against
 * {@link V2ColdStartNoWarmUpBenchmark}.
 *
 * <p>Priming makes a real STS {@code GET} per sync HTTP client on the classpath, so the score includes network time,
 * varies by host, and is an upper bound (this module has several HTTP clients; typical apps have one). Do not add it
 * to {@code baseline.json}.
 *
 * <p>See {@link V2ColdStartNoWarmUpBenchmark} for the single-shot JMH parameters. Priming is once-per-JVM, so only the
 * first invocation in a fork is a real measurement.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(20)
public class V2SdkWarmUpExecutionTimeBenchmark {

    @Benchmark
    public void prime() throws Exception {
        SdkWarmUp.prime(DynamoDbClient.class);
    }

    public static void main(String... args) throws RunnerException, CommandLineOptionException {
        Options opt = new OptionsBuilder()
            .parent(new CommandLineOptions())
            .include(V2SdkWarmUpExecutionTimeBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
