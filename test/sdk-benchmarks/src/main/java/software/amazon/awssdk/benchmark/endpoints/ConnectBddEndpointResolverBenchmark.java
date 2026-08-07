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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.services.connect.endpoints.internal.BaselineBddEndpointProvider;
import software.amazon.awssdk.services.connect.endpoints.internal.BaselineRulesEndpointResolver;
import software.amazon.awssdk.services.connect.endpoints.internal.DefaultConnectEndpointProvider;

/**
 * Compares Amazon Connect endpoint resolution across three resolver implementations:
 * <ol>
 *   <li>{@link BaselineRulesEndpointResolver} - the classic rules-based resolver (generated without BDD)</li>
 *   <li>{@link BaselineBddEndpointProvider} - the original table-driven BDD resolver (while-loop traversal)</li>
 *   <li>{@link DefaultConnectEndpointProvider} - the optimized BDD resolver (direct control-flow, inlined if-branches)</li>
 * </ol>
 *
 * <p>Connect uses the standard regional rule set, so this covers the common case shared by the majority of services. The
 * cases are carried over from the original BDD proof of concept.
 *
 * <p>This benchmark exists only to measure and optimize BDD endpoint resolution. It is not registered with
 * {@code BenchmarkRunner} and is not intended to ship.
 */
@State(Scope.Thread)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ConnectBddEndpointResolverBenchmark {
    /**
     * Fixed seed so the per-iteration case ordering is reproducible across JVM runs.
     */
    private static final long SHUFFLE_SEED = 20260730L;

    private final ConnectEndpointProvider rulesBasedProvider = new BaselineRulesEndpointResolver();
    private final ConnectEndpointProvider baselineBddProvider = new BaselineBddEndpointProvider();
    private final ConnectEndpointProvider optimizedBddProvider = new DefaultConnectEndpointProvider();

    private Map<String, ConnectEndpointParams> nonErrorCases;
    private List<ConnectEndpointParams> shuffledCases;
    private Random random;

    @Setup(Level.Trial)
    public void setupTrial() {
        nonErrorCases = new HashMap<>();
        setupBenchmarkCases();
        shuffledCases = new ArrayList<>(nonErrorCases.values());
        random = new Random(SHUFFLE_SEED);
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        // Shuffle order between iterations so the resolvers don't benefit from a fixed branch history.
        Collections.shuffle(shuffledCases, random);
    }

    @Benchmark
    public void rulesBasedResolver(Blackhole blackhole) {
        runTest(blackhole, rulesBasedProvider);
    }

    @Benchmark
    public void baselineBddResolver(Blackhole blackhole) {
        runTest(blackhole, baselineBddProvider);
    }

    @Benchmark
    public void optimizedBddResolver(Blackhole blackhole) {
        runTest(blackhole, optimizedBddProvider);
    }

    private void runTest(Blackhole blackhole, ConnectEndpointProvider endpointProvider) {
        for (ConnectEndpointParams params : shuffledCases) {
            blackhole.consume(endpointProvider.resolveEndpoint(params).join());
        }
    }

    private void setupBenchmarkCases() {
        nonErrorCases.put(
            "0: us-east-1",
            ConnectEndpointParams.builder()
                                 .region(Region.US_EAST_1)
                                 .build());
        nonErrorCases.put(
            "1: custom endpoint",
            ConnectEndpointParams.builder()
                                 .endpoint("http://localhost:8080")
                                 .build());
        nonErrorCases.put(
            "2: us-east-1 with FIPS",
            ConnectEndpointParams.builder()
                                 .region(Region.US_EAST_1)
                                 .useFips(true)
                                 .build());
        nonErrorCases.put(
            "3: us-west-2 with FIPS and dualstack",
            ConnectEndpointParams.builder()
                                 .region(Region.US_WEST_2)
                                 .useFips(true)
                                 .useDualStack(true)
                                 .build());
        nonErrorCases.put(
            "4: eu-central-1 with dualstack",
            ConnectEndpointParams.builder()
                                 .region(Region.EU_CENTRAL_1)
                                 .useDualStack(true)
                                 .build());
        nonErrorCases.put(
            "5: cn-north-1 with dualstack",
            ConnectEndpointParams.builder()
                                 .region(Region.CN_NORTH_1)
                                 .useDualStack(true)
                                 .build());
    }
}
