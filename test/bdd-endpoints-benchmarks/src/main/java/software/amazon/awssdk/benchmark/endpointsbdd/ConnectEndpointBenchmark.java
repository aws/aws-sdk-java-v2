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
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.GeneratedEndpointResolver;

/**
 * Compares Connect endpoint resolution across five resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — SDK v2 rules-based (no BDD)</li>
 *   <li>{@code "baselineBdd"} — SDK v2 original table-driven BDD</li>
 *   <li>{@code "optimizedBdd"} — SDK v2 optimized inlined-branch BDD</li>
 *   <li>{@code "smithyJavaGenerated"} — smithy-java code-generated {@link GeneratedEndpointResolver},
 *       using the {@link GeneratedEndpointResolver.GeneratedParameters} fast path to skip
 *       {@link EndpointResolverParams} allocation on every call</li>
 * </ul>
 *
 * <p>The BDD trait is taken from the model as-is with no re-optimization.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class ConnectEndpointBenchmark {

    private static final long SHUFFLE_SEED = 20260730L;

    @Param({"rules", "baselineBdd", "optimizedBdd", "smithyJavaGenerated"})
    private String resolver;

    // SDK resolvers — non-null for rules/baselineBdd/optimizedBdd
    private ConnectEndpointProvider sdkProvider;

    // smithy-java resolvers — non-null for smithyJavaGenerated
    private SmithyJavaResolverFactory.Resolvers smithyResolvers;

    // SDK params (rules/baselineBdd/optimizedBdd)
    private ConnectEndpointParams case0SdkParams;
    private ConnectEndpointParams case1SdkParams;
    private ConnectEndpointParams case2SdkParams;
    private ConnectEndpointParams case3SdkParams;
    private ConnectEndpointParams case4SdkParams;
    private ConnectEndpointParams case5SdkParams;

    // Smithy-java resolver params (provides context for generatedParams)
    private EndpointResolverParams case0BytecodeParams;
    private EndpointResolverParams case1BytecodeParams;
    private EndpointResolverParams case2BytecodeParams;
    private EndpointResolverParams case3BytecodeParams;
    private EndpointResolverParams case4BytecodeParams;
    private EndpointResolverParams case5BytecodeParams;

    // Generated resolver params (smithyJavaGenerated) — pre-marshalled, allocation-free at call time
    private GeneratedEndpointResolver.GeneratedParameters case0GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case1GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case2GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case3GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case4GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case5GeneratedParams;

    private List<Object> shuffledCases;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        switch (resolver) {
            case "rules":               sdkProvider = new BaselineRulesEndpointResolver(); break;
            case "baselineBdd":         sdkProvider = new BaselineBddEndpointProvider(); break;
            case "optimizedBdd":        sdkProvider = new OptimizedBddConnectEndpointProvider(); break;
            case "smithyJavaGenerated":
                smithyResolvers = SmithyJavaResolverFactory.forConnect();
                break;
            default: throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        Map<String, Object> p0 = Map.of("Region", "us-east-1", "UseFIPS", false, "UseDualStack", false);
        Map<String, Object> p1 = Map.of("Endpoint", "http://localhost:8080", "UseFIPS", false, "UseDualStack", false);
        Map<String, Object> p2 = Map.of("Region", "us-east-1", "UseFIPS", true, "UseDualStack", false);
        Map<String, Object> p3 = Map.of("Region", "us-west-2", "UseFIPS", true, "UseDualStack", true);
        Map<String, Object> p4 = Map.of("Region", "eu-central-1", "UseFIPS", false, "UseDualStack", true);
        Map<String, Object> p5 = Map.of("Region", "cn-north-1", "UseFIPS", false, "UseDualStack", true);

        // SDK params
        case0SdkParams = ConnectEndpointParams.builder().region(Region.US_EAST_1).build();
        case1SdkParams = ConnectEndpointParams.builder().endpoint("http://localhost:8080").build();
        case2SdkParams = ConnectEndpointParams.builder().region(Region.US_EAST_1).useFips(true).build();
        case3SdkParams = ConnectEndpointParams.builder().region(Region.US_WEST_2).useFips(true).useDualStack(true).build();
        case4SdkParams = ConnectEndpointParams.builder().region(Region.EU_CENTRAL_1).useDualStack(true).build();
        case5SdkParams = ConnectEndpointParams.builder().region(Region.CN_NORTH_1).useDualStack(true).build();

        // Bytecode params
        case0BytecodeParams = SmithyJavaResolverFactory.params("us-east-1", p0);
        case1BytecodeParams = SmithyJavaResolverFactory.params(null, p1);
        case2BytecodeParams = SmithyJavaResolverFactory.params("us-east-1", p2);
        case3BytecodeParams = SmithyJavaResolverFactory.params("us-west-2", p3);
        case4BytecodeParams = SmithyJavaResolverFactory.params("eu-central-1", p4);
        case5BytecodeParams = SmithyJavaResolverFactory.params("cn-north-1", p5);

        // Generated params (only populated when the generated resolver is loaded)
        if (smithyResolvers != null) {
            case0GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p0);
            case1GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p1);
            case2GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p2);
            case3GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p3);
            case4GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p4);
            case5GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p5);
        }

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
            var gen = asGenerated();
            var ctx = case0BytecodeParams.context();
            for (Object p : shuffledCases) {
                bh.consume(gen.resolveEndpoint(ctx, (GeneratedEndpointResolver.GeneratedParameters) p));
            }
        }
    }

    // ------------------------------------------------------------------------------------- per-case

    @Benchmark
    public void case0_usEast1(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case0SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case0BytecodeParams.context(), case0GeneratedParams));
    }

    @Benchmark
    public void case1_customEndpoint(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case1SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case1BytecodeParams.context(), case1GeneratedParams));
    }

    @Benchmark
    public void case2_usEast1Fips(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case2SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case2BytecodeParams.context(), case2GeneratedParams));
    }

    @Benchmark
    public void case3_usWest2FipsDualStack(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case3SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case3BytecodeParams.context(), case3GeneratedParams));
    }

    @Benchmark
    public void case4_euCentral1DualStack(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case4SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case4BytecodeParams.context(), case4GeneratedParams));
    }

    @Benchmark
    public void case5_cnNorth1DualStack(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case5SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case5BytecodeParams.context(), case5GeneratedParams));
    }

    // ------------------------------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    private GeneratedEndpointResolver<GeneratedEndpointResolver.EvaluationState> asGenerated() {
        return (GeneratedEndpointResolver<GeneratedEndpointResolver.EvaluationState>) smithyResolvers.generated;
    }

    private List<Object> buildShuffledCases() {
        List<Object> cases = new ArrayList<>();
        if (sdkProvider != null) {
            cases.add(case0SdkParams); cases.add(case1SdkParams); cases.add(case2SdkParams);
            cases.add(case3SdkParams); cases.add(case4SdkParams); cases.add(case5SdkParams);
        } else {
            cases.add(case0GeneratedParams); cases.add(case1GeneratedParams); cases.add(case2GeneratedParams);
            cases.add(case3GeneratedParams); cases.add(case4GeneratedParams); cases.add(case5GeneratedParams);
        }
        return cases;
    }
}
