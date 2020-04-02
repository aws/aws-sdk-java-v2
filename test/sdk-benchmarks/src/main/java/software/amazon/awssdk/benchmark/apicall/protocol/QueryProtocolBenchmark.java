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

package software.amazon.awssdk.benchmark.apicall.protocol;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.ERROR_XML_BODY;
import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.QUERY_ALL_TYPES_REQUEST;
import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.XML_BODY;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.utils.MockHttpClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;

/**
 * Benchmarking for running with different protocols.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class QueryProtocolBenchmark implements SdkProtocolBenchmark {

    private ProtocolQueryClient client;

    @Setup(Level.Trial)
    public void setup() {
        client = ProtocolQueryClient.builder()
                                    .httpClient(new MockHttpClient(XML_BODY, ERROR_XML_BODY))
                                    .build();
    }

    @Benchmark
    public void successfulResponse(Blackhole blackhole) {
        blackhole.consume(client.allTypes(QUERY_ALL_TYPES_REQUEST));
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(QueryProtocolBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        new Runner(opt).run();
    }
}
