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
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.BaselineBddEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.BaselineRulesEndpointResolver;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.DefaultDynamoDbEndpointProvider;

/**
 * Benchmarks individual DynamoDB endpoint resolution cases across all three resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — {@link BaselineRulesEndpointResolver}: classic rules-based resolver (no BDD)</li>
 *   <li>{@code "baselineBdd"} — {@link BaselineBddEndpointProvider}: original table-driven BDD (while-loop traversal)</li>
 *   <li>{@code "optimizedBdd"} — {@link DefaultDynamoDbEndpointProvider}: optimized BDD (inlined if-branches)</li>
 * </ul>
 *
 * <p>Each {@code @Benchmark} method covers one case from {@link DynamoDbBddEndpointResolverBenchmark},
 * matching the cases used by {@code DynamoDbEndpointResolverBenchmark} in the sdk-standard-benchmarks module.
 * Per-case methods allow the JIT to specialize each branch path independently.
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
public class DynamoDbBddEndpointCaseBenchmark {

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

    private DynamoDbEndpointProvider provider;

    // Pre-built params for each case — built once in @Setup so allocation is excluded from measurements.
    private DynamoDbEndpointParams case0Params;
    private DynamoDbEndpointParams case1Params;
    private DynamoDbEndpointParams case2Params;
    private DynamoDbEndpointParams case3Params;
    private DynamoDbEndpointParams case4Params;

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
                provider = new DefaultDynamoDbEndpointProvider();
                break;
            default:
                throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        // Case 0: standard regional endpoint — us-east-1, no FIPS, no dual-stack.
        case0Params = DynamoDbEndpointParams.builder()
                                            .region(Region.US_EAST_1)
                                            .useFips(false)
                                            .useDualStack(false)
                                            .build();

        // Case 1: FIPS + dual-stack — exercises the fips-dualstack partition path.
        case1Params = DynamoDbEndpointParams.builder()
                                            .region(Region.US_EAST_1)
                                            .useFips(true)
                                            .useDualStack(true)
                                            .build();

        // Case 2: account ID based endpoint in preferred mode — takes the account-ID branch.
        case2Params = DynamoDbEndpointParams.builder()
                                            .region(Region.US_EAST_1)
                                            .useFips(false)
                                            .useDualStack(false)
                                            .accountId("111111111111")
                                            .accountIdEndpointMode("preferred")
                                            .build();

        // Case 3: account ID + CN partition — partition does not support account endpoints, falls through to regional.
        case3Params = DynamoDbEndpointParams.builder()
                                            .region(Region.CN_NORTH_1)
                                            .useFips(false)
                                            .useDualStack(false)
                                            .accountId("111111111111")
                                            .accountIdEndpointMode("preferred")
                                            .build();

        // Case 4: custom endpoint override.
        case4Params = DynamoDbEndpointParams.builder()
                                            .region(Region.US_EAST_1)
                                            .useFips(false)
                                            .useDualStack(false)
                                            .accountIdEndpointMode("disabled")
                                            .endpoint("https://localhost:8000")
                                            .build();
    }

    /** Case 0: standard regional endpoint (us-east-1, no FIPS, no dual-stack). */
    @Benchmark
    public void case0_regional(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case0Params).join());
    }

    /** Case 1: FIPS + dual-stack. */
    @Benchmark
    public void case1_fipsDualStack(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case1Params).join());
    }

    /** Case 2: account ID based endpoint, preferred mode (us-east-1). */
    @Benchmark
    public void case2_accountIdPreferred(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case2Params).join());
    }

    /** Case 3: account ID + cn-north-1 (account endpoints not supported; falls back to regional). */
    @Benchmark
    public void case3_accountIdChinaFallback(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case3Params).join());
    }

    /** Case 4: custom endpoint override. */
    @Benchmark
    public void case4_customEndpoint(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case4Params).join());
    }
}
