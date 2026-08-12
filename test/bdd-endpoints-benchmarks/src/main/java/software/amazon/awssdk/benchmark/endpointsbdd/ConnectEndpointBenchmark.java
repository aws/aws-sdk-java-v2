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

package software.amazon.awssdk.benchmark.endpointsbdd;

import java.util.ArrayList;
import java.util.Collections;
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
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.BytecodeEndpointResolver;

/**
 * Compares Connect endpoint resolution across four resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — SDK v2 rules-based (no BDD)</li>
 *   <li>{@code "baselineBdd"} — SDK v2 original table-driven BDD</li>
 *   <li>{@code "optimizedBdd"} — SDK v2 optimized inlined-branch BDD</li>
 *   <li>{@code "smithyJava"} — smithy-java {@link BytecodeEndpointResolver} compiled from the
 *       smithy model's {@code smithy.rules#endpointBdd} trait via the sifting→cost→reversal
 *       optimization pipeline</li>
 * </ul>
 *
 * <p>Contains both an <em>aggregate</em> benchmark (all cases shuffled per iteration) and
 * per-case benchmarks for independent JIT specialization.
 *
 * <p>For {@code smithyJava}, all parameters are supplied via {@code ADDITIONAL_ENDPOINT_PARAMS}
 * on the {@link software.amazon.smithy.java.context.Context} (the "canned" mode from the
 * smithy-java reference benchmarks). No input-shape extraction occurs.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class ConnectEndpointBenchmark {

    private static final long SHUFFLE_SEED = 20260730L;

    @Param({"rules", "baselineBdd", "optimizedBdd", "smithyJava"})
    private String resolver;

    private ConnectEndpointProvider sdkProvider;
    private EndpointResolver smithyProvider;

    private ConnectEndpointParams case0SdkParams;
    private ConnectEndpointParams case1SdkParams;
    private ConnectEndpointParams case2SdkParams;
    private ConnectEndpointParams case3SdkParams;
    private ConnectEndpointParams case4SdkParams;
    private ConnectEndpointParams case5SdkParams;

    private EndpointResolverParams case0SmithyParams;
    private EndpointResolverParams case1SmithyParams;
    private EndpointResolverParams case2SmithyParams;
    private EndpointResolverParams case3SmithyParams;
    private EndpointResolverParams case4SmithyParams;
    private EndpointResolverParams case5SmithyParams;

    private List<Object> shuffledCases;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        switch (resolver) {
            case "rules":        sdkProvider = new BaselineRulesEndpointResolver(); break;
            case "baselineBdd":  sdkProvider = new BaselineBddEndpointProvider(); break;
            case "optimizedBdd": sdkProvider = new DefaultConnectEndpointProvider(); break;
            case "smithyJava":   smithyProvider = SmithyJavaResolverFactory.forConnect(); break;
            default: throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        // Case 0: us-east-1, standard regional
        case0SdkParams = ConnectEndpointParams.builder()
                                              .region(Region.US_EAST_1).build();
        case0SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "UseFIPS", false, "UseDualStack", false));

        // Case 1: custom endpoint override (no region)
        case1SdkParams = ConnectEndpointParams.builder()
                                              .endpoint("http://localhost:8080").build();
        case1SmithyParams = SmithyJavaResolverFactory.params(null,
                Map.of("Endpoint", "http://localhost:8080", "UseFIPS", false, "UseDualStack", false));

        // Case 2: us-east-1 with FIPS
        case2SdkParams = ConnectEndpointParams.builder()
                                              .region(Region.US_EAST_1).useFips(true).build();
        case2SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "UseFIPS", true, "UseDualStack", false));

        // Case 3: us-west-2 with FIPS and dual-stack
        case3SdkParams = ConnectEndpointParams.builder()
                                              .region(Region.US_WEST_2).useFips(true).useDualStack(true).build();
        case3SmithyParams = SmithyJavaResolverFactory.params("us-west-2",
                Map.of("Region", "us-west-2", "UseFIPS", true, "UseDualStack", true));

        // Case 4: eu-central-1 with dual-stack
        case4SdkParams = ConnectEndpointParams.builder()
                                              .region(Region.EU_CENTRAL_1).useDualStack(true).build();
        case4SmithyParams = SmithyJavaResolverFactory.params("eu-central-1",
                Map.of("Region", "eu-central-1", "UseFIPS", false, "UseDualStack", true));

        // Case 5: cn-north-1 with dual-stack
        case5SdkParams = ConnectEndpointParams.builder()
                                              .region(Region.CN_NORTH_1).useDualStack(true).build();
        case5SmithyParams = SmithyJavaResolverFactory.params("cn-north-1",
                Map.of("Region", "cn-north-1", "UseFIPS", false, "UseDualStack", true));

        shuffledCases = buildShuffledCases();
        random = new Random(SHUFFLE_SEED);
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        Collections.shuffle(shuffledCases, random);
    }

    // ------------------------------------------------------------------------------------- aggregate

    @Benchmark
    public void aggregate(Blackhole bh) {
        if (sdkProvider != null) {
            for (Object p : shuffledCases) {
                bh.consume(sdkProvider.resolveEndpoint((ConnectEndpointParams) p).join());
            }
        } else {
            for (Object p : shuffledCases) {
                bh.consume(smithyProvider.resolveEndpoint((EndpointResolverParams) p));
            }
        }
    }

    // ------------------------------------------------------------------------------------- per-case

    @Benchmark
    public void case0_usEast1(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case0SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case0SmithyParams));
        }
    }

    @Benchmark
    public void case1_customEndpoint(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case1SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case1SmithyParams));
        }
    }

    @Benchmark
    public void case2_usEast1Fips(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case2SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case2SmithyParams));
        }
    }

    @Benchmark
    public void case3_usWest2FipsDualStack(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case3SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case3SmithyParams));
        }
    }

    @Benchmark
    public void case4_euCentral1DualStack(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case4SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case4SmithyParams));
        }
    }

    @Benchmark
    public void case5_cnNorth1DualStack(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case5SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case5SmithyParams));
        }
    }

    // ------------------------------------------------------------------------------------- helpers

    private List<Object> buildShuffledCases() {
        List<Object> cases = new ArrayList<>();
        if (sdkProvider != null) {
            cases.add(case0SdkParams); cases.add(case1SdkParams); cases.add(case2SdkParams);
            cases.add(case3SdkParams); cases.add(case4SdkParams); cases.add(case5SdkParams);
        } else {
            cases.add(case0SmithyParams); cases.add(case1SmithyParams); cases.add(case2SmithyParams);
            cases.add(case3SmithyParams); cases.add(case4SmithyParams); cases.add(case5SmithyParams);
        }
        return cases;
    }
}
