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
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.BaselineBddEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.BaselineRulesEndpointResolver;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.DefaultDynamoDbEndpointProvider;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.BytecodeEndpointResolver;

/**
 * Compares DynamoDB endpoint resolution across four resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — SDK v2 rules-based (no BDD)</li>
 *   <li>{@code "baselineBdd"} — SDK v2 original table-driven BDD</li>
 *   <li>{@code "optimizedBdd"} — SDK v2 optimized inlined-branch BDD</li>
 *   <li>{@code "smithyJava"} — smithy-java {@link BytecodeEndpointResolver}</li>
 * </ul>
 *
 * <p>Cases: standard regional, FIPS+dual-stack, account-ID preferred, account-ID+CN (fallback),
 * custom endpoint override.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class DynamoDbEndpointBenchmark {

    private static final long SHUFFLE_SEED = 20260730L;

    @Param({"rules", "baselineBdd", "optimizedBdd", "smithyJava"})
    private String resolver;

    private DynamoDbEndpointProvider sdkProvider;
    private EndpointResolver smithyProvider;

    private DynamoDbEndpointParams case0SdkParams;
    private DynamoDbEndpointParams case1SdkParams;
    private DynamoDbEndpointParams case2SdkParams;
    private DynamoDbEndpointParams case3SdkParams;
    private DynamoDbEndpointParams case4SdkParams;

    private EndpointResolverParams case0SmithyParams;
    private EndpointResolverParams case1SmithyParams;
    private EndpointResolverParams case2SmithyParams;
    private EndpointResolverParams case3SmithyParams;
    private EndpointResolverParams case4SmithyParams;

    private List<Object> shuffledCases;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        switch (resolver) {
            case "rules":        sdkProvider = new BaselineRulesEndpointResolver(); break;
            case "baselineBdd":  sdkProvider = new BaselineBddEndpointProvider(); break;
            case "optimizedBdd": sdkProvider = new DefaultDynamoDbEndpointProvider(); break;
            case "smithyJava":   smithyProvider = SmithyJavaResolverFactory.forDynamoDb(); break;
            default: throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        // Case 0: standard regional — us-east-1, no FIPS, no dual-stack
        case0SdkParams = DynamoDbEndpointParams.builder()
                                               .region(Region.US_EAST_1).useFips(false).useDualStack(false)
                                               .build();
        case0SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "UseFIPS", false, "UseDualStack", false));

        // Case 1: FIPS + dual-stack
        case1SdkParams = DynamoDbEndpointParams.builder()
                                               .region(Region.US_EAST_1).useFips(true).useDualStack(true)
                                               .build();
        case1SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "UseFIPS", true, "UseDualStack", true));

        // Case 2: account ID based endpoint, preferred mode
        case2SdkParams = DynamoDbEndpointParams.builder()
                                               .region(Region.US_EAST_1).useFips(false).useDualStack(false)
                                               .accountId("111111111111").accountIdEndpointMode("preferred")
                                               .build();
        case2SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "UseFIPS", false, "UseDualStack", false,
                       "AccountId", "111111111111", "AccountIdEndpointMode", "preferred"));

        // Case 3: account ID + CN partition (falls back to regional)
        case3SdkParams = DynamoDbEndpointParams.builder()
                                               .region(Region.CN_NORTH_1).useFips(false).useDualStack(false)
                                               .accountId("111111111111").accountIdEndpointMode("preferred")
                                               .build();
        case3SmithyParams = SmithyJavaResolverFactory.params("cn-north-1",
                Map.of("Region", "cn-north-1", "UseFIPS", false, "UseDualStack", false,
                       "AccountId", "111111111111", "AccountIdEndpointMode", "preferred"));

        // Case 4: custom endpoint override
        case4SdkParams = DynamoDbEndpointParams.builder()
                                               .region(Region.US_EAST_1).useFips(false).useDualStack(false)
                                               .accountIdEndpointMode("disabled")
                                               .endpoint("https://localhost:8000")
                                               .build();
        case4SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "UseFIPS", false, "UseDualStack", false,
                       "AccountIdEndpointMode", "disabled", "Endpoint", "https://localhost:8000"));

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
                bh.consume(sdkProvider.resolveEndpoint((DynamoDbEndpointParams) p).join());
            }
        } else {
            for (Object p : shuffledCases) {
                bh.consume(smithyProvider.resolveEndpoint((EndpointResolverParams) p));
            }
        }
    }

    // ------------------------------------------------------------------------------------- per-case

    @Benchmark
    public void case0_regional(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case0SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case0SmithyParams));
        }
    }

    @Benchmark
    public void case1_fipsDualStack(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case1SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case1SmithyParams));
        }
    }

    @Benchmark
    public void case2_accountIdPreferred(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case2SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case2SmithyParams));
        }
    }

    @Benchmark
    public void case3_accountIdChinaFallback(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case3SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case3SmithyParams));
        }
    }

    @Benchmark
    public void case4_customEndpoint(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case4SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case4SmithyParams));
        }
    }

    // ------------------------------------------------------------------------------------- helpers

    private List<Object> buildShuffledCases() {
        List<Object> cases = new ArrayList<>();
        if (sdkProvider != null) {
            cases.add(case0SdkParams); cases.add(case1SdkParams); cases.add(case2SdkParams);
            cases.add(case3SdkParams); cases.add(case4SdkParams);
        } else {
            cases.add(case0SmithyParams); cases.add(case1SmithyParams); cases.add(case2SmithyParams);
            cases.add(case3SmithyParams); cases.add(case4SmithyParams);
        }
        return cases;
    }
}
