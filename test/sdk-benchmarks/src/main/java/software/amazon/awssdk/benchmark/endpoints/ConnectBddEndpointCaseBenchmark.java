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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
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
 * Benchmarks individual Connect endpoint resolution cases across all three resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — {@link BaselineRulesEndpointResolver}: classic rules-based resolver (no BDD)</li>
 *   <li>{@code "baselineBdd"} — {@link BaselineBddEndpointProvider}: original table-driven BDD (while-loop traversal)</li>
 *   <li>{@code "optimizedBdd"} — {@link DefaultConnectEndpointProvider}: optimized BDD (inlined if-branches)</li>
 * </ul>
 *
 * <p>Each {@code @Benchmark} method covers one endpoint resolution case from
 * {@link ConnectBddEndpointResolverBenchmark}, allowing per-case measurement and branch-predictor
 * specialization that the aggregated benchmark cannot provide.
 *
 * <p>This benchmark exists only to measure and optimize BDD endpoint resolution. It is not registered with
 * {@code BenchmarkRunner} and is not intended to ship.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class ConnectBddEndpointCaseBenchmark {

    /**
     * Selects which resolver implementation to benchmark.
     * <ul>
     *   <li>{@code "rules"} — rules-based (no BDD)</li>
     *   <li>{@code "baselineBdd"} — original table-driven BDD</li>
     *   <li>{@code "optimizedBdd"} — optimized inlined-branch BDD</li>
     * </ul>
     */
    @Param({"rules", "baselineBdd", "optimizedBdd"})
    private String resolver;

    private ConnectEndpointProvider provider;

    // Pre-built params for each case — built once in @Setup so allocation is excluded from measurements.
    private ConnectEndpointParams case0Params;
    private ConnectEndpointParams case1Params;
    private ConnectEndpointParams case2Params;
    private ConnectEndpointParams case3Params;
    private ConnectEndpointParams case4Params;
    private ConnectEndpointParams case5Params;

    @Setup(Level.Trial)
    public void setup() {
        switch (resolver) {
            case "rules":
                provider = new BaselineRulesEndpointResolver();
                break;
            case "baselineBdd":
                provider = new BaselineBddEndpointProvider();
                break;
            case "optimizedBdd":
                provider = new DefaultConnectEndpointProvider();
                break;
            default:
                throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        case0Params = ConnectEndpointParams.builder()
                                           .region(Region.US_EAST_1)
                                           .build();
        case1Params = ConnectEndpointParams.builder()
                                           .endpoint("http://localhost:8080")
                                           .build();
        case2Params = ConnectEndpointParams.builder()
                                           .region(Region.US_EAST_1)
                                           .useFips(true)
                                           .build();
        case3Params = ConnectEndpointParams.builder()
                                           .region(Region.US_WEST_2)
                                           .useFips(true)
                                           .useDualStack(true)
                                           .build();
        case4Params = ConnectEndpointParams.builder()
                                           .region(Region.EU_CENTRAL_1)
                                           .useDualStack(true)
                                           .build();
        case5Params = ConnectEndpointParams.builder()
                                           .region(Region.CN_NORTH_1)
                                           .useDualStack(true)
                                           .build();
    }

    /** Case 0: us-east-1, no FIPS, no dual-stack — standard regional endpoint. */
    @Benchmark
    public void case0_usEast1(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case0Params).join());
    }

    /** Case 1: custom endpoint override (no region). */
    @Benchmark
    public void case1_customEndpoint(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case1Params).join());
    }

    /** Case 2: us-east-1 with FIPS. */
    @Benchmark
    public void case2_usEast1Fips(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case2Params).join());
    }

    /** Case 3: us-west-2 with FIPS and dual-stack. */
    @Benchmark
    public void case3_usWest2FipsDualStack(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case3Params).join());
    }

    /** Case 4: eu-central-1 with dual-stack. */
    @Benchmark
    public void case4_euCentral1DualStack(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case4Params).join());
    }

    /** Case 5: cn-north-1 with dual-stack. */
    @Benchmark
    public void case5_cnNorth1DualStack(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case5Params).join());
    }
}
