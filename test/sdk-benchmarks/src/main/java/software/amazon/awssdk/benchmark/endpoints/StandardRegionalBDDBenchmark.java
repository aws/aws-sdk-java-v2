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

package software.amazon.awssdk.benchmark.endpoints;

import java.util.ArrayList;
import java.util.List;
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
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.services.connect.endpoints.internal.BDDEndpointResolverCodegenDag;
import software.amazon.awssdk.services.connect.endpoints.internal.BDDEndpointResolverCodegenDag2;
import software.amazon.awssdk.services.connect.endpoints.internal.BDDEndpointResolverRuntimeDag;
import software.amazon.awssdk.services.connect.endpoints.internal.BDDEndpointResolverRuntimeDag2;
import software.amazon.awssdk.services.connect.endpoints.internal.BddRuntime4;
import software.amazon.awssdk.services.connect.endpoints.internal.BddUnOptRuntime3;
import software.amazon.awssdk.services.connect.endpoints.internal.BddUnOptRuntime4;
import software.amazon.awssdk.services.connect.endpoints.internal.BddUnOptSubgraph;

//use -Djmh.print.inline=true

@State(Scope.Thread)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class StandardRegionalBDDBenchmark {
    // ConnectEndpointProvider bddResolver = new BDDEndpointResolverHandCoded();
    // ConnectEndpointProvider bddResolverTranslatedToRules = new BDDEndpointResolverTranslatedToRules();
    ConnectEndpointProvider bddResolverRuntimeDag = new BDDEndpointResolverRuntimeDag2();
    ConnectEndpointProvider bddResolverCodegenDag = new BDDEndpointResolverCodegenDag2();
    ConnectEndpointProvider bddRuntime4 = new BddRuntime4();
    ConnectEndpointProvider bddUnOptRuntime3 = new BddUnOptRuntime3();
    ConnectEndpointProvider bddUnOptRuntime4 = new BddUnOptRuntime4();
    ConnectEndpointProvider bddUnOptSubgraph = new BddUnOptSubgraph();

    ConnectEndpointProvider defaultProvider = new BDDEndpointResolverRuntimeDag();
    List<ConnectEndpointParams> params = new ArrayList<>();

    public StandardRegionalBDDBenchmark() {
        params.add(ConnectEndpointParams.builder()
                                        .region(Region.US_EAST_1)
                                        .build());
        params.add(ConnectEndpointParams.builder().endpoint("http://localhost:8080").build());
        params.add(ConnectEndpointParams.builder().region(Region.US_EAST_1).useFips(true).build());
        params.add(ConnectEndpointParams.builder().region(Region.US_EAST_1).useFips(true).useDualStack(true).build());
        params.add(ConnectEndpointParams.builder().region(Region.US_EAST_1).useDualStack(true).build());
        params.add(ConnectEndpointParams.builder().region(Region.CN_NORTH_1).useDualStack(true).build());
    }

    public void runTest(Blackhole blackhole, ConnectEndpointProvider endpointProvider) {
        for (ConnectEndpointParams param : params) {
            blackhole.consume(endpointProvider.resolveEndpoint(param).join());
        }
    }

    @Benchmark
    public void baselineRuleResolver(Blackhole blackhole) {
        runTest(blackhole, defaultProvider);
    }

    // @Benchmark
    // public void bddOptRuntime4(Blackhole blackhole) {
    //     runTest(blackhole, bddRuntime4);
    // }
    //
    // @Benchmark
    // public void bddNonOptRuntime4(Blackhole blackhole) {
    //     runTest(blackhole, bddUnOptRuntime4);
    // }

    // @Benchmark
    // public void bddNonOptRuntime3(Blackhole blackhole) {
    //     runTest(blackhole, bddUnOptRuntime3);
    // }
    //
    // @Benchmark
    // public void bddNonOptSubgraph(Blackhole blackhole) {
    //     runTest(blackhole, bddUnOptSubgraph);
    // }

    // @Benchmark
    // public void codegenBdd(Blackhole blackhole) {
    //     runTest(blackhole, bddResolverCodegenDag);
    // }
    //
    // @Benchmark
    // public void runtimeBddLambda(Blackhole blackhole) {
    //     runTest(blackhole, bddResolverRuntimeDag);
    // }
    //
    // @Benchmark
    // public void runtimeBddMethodReference(Blackhole blackhole) {
    //     runTest(blackhole, bddRuntime4);
    // }

    // @Benchmark
    // public void benchmarkBDDHandCoded(Blackhole blackhole) {
    //     runTest(blackhole, bddResolver);
    // }
    //
    // @Benchmark
    // public void benchmarkBDDTranslatedToRules(Blackhole blackhole) {
    //     runTest(blackhole, bddResolverTranslatedToRules);
    // }
}
