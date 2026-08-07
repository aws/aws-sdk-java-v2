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
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineBddEndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineRulesEndpointResolver;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;

/**
 * Benchmarks individual S3 endpoint resolution cases across all three resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — {@link BaselineRulesEndpointResolver}: classic rules-based resolver (no BDD)</li>
 *   <li>{@code "baselineBdd"} — {@link BaselineBddEndpointProvider}: original table-driven BDD (while-loop traversal)</li>
 *   <li>{@code "optimizedBdd"} — {@link DefaultS3EndpointProvider}: optimized BDD (inlined if-branches)</li>
 * </ul>
 *
 * <p>The five cases mirror those in {@code S3EndpointResolverBenchmark} in the sdk-standard-benchmarks module,
 * covering the most representative S3 resolution paths. Per-case methods let the JIT specialize each branch
 * path independently rather than training branch predictors on a mix of paths.
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
public class S3BddEndpointCaseBenchmark {

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

    private S3EndpointProvider provider;

    // Pre-built params for each case — built once in @Setup so allocation is excluded from measurements.
    private S3EndpointParams case0Params;
    private S3EndpointParams case1Params;
    private S3EndpointParams case2Params;
    private S3EndpointParams case3Params;
    private S3EndpointParams case4Params;

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
                provider = new DefaultS3EndpointProvider();
                break;
            default:
                throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        // Case 0: vanilla virtual addressing@us-west-2
        case0Params = S3EndpointParams.builder()
                                      .bucket("bucket-name")
                                      .region(Region.US_WEST_2)
                                      .useFips(false)
                                      .useDualStack(false)
                                      .forcePathStyle(false)
                                      .accelerate(false)
                                      .useGlobalEndpoint(false)
                                      .disableMultiRegionAccessPoints(false)
                                      .build();

        // Case 1: vanilla path style@us-west-2
        case1Params = S3EndpointParams.builder()
                                      .bucket("bucket-name")
                                      .region(Region.US_WEST_2)
                                      .useFips(false)
                                      .useDualStack(false)
                                      .forcePathStyle(true)
                                      .accelerate(false)
                                      .useGlobalEndpoint(false)
                                      .disableMultiRegionAccessPoints(false)
                                      .build();

        // Case 2: S3 Express data-plane with short zone name (--x-s3 suffix)
        case2Params = S3EndpointParams.builder()
                                      .bucket("mybucket--abcd-ab1--x-s3")
                                      .region(Region.US_EAST_1)
                                      .useFips(false)
                                      .useDualStack(false)
                                      .forcePathStyle(false)
                                      .accelerate(false)
                                      .useGlobalEndpoint(false)
                                      .disableMultiRegionAccessPoints(false)
                                      .disableS3ExpressSessionAuth(false)
                                      .build();

        // Case 3: vanilla access point ARN@us-west-2
        case3Params = S3EndpointParams.builder()
                                      .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                      .region(Region.US_WEST_2)
                                      .useFips(false)
                                      .useDualStack(false)
                                      .forcePathStyle(false)
                                      .accelerate(false)
                                      .useGlobalEndpoint(false)
                                      .disableMultiRegionAccessPoints(false)
                                      .build();

        // Case 4: S3 Outposts vanilla test
        case4Params = S3EndpointParams.builder()
                                      .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                      .region(Region.US_WEST_2)
                                      .useFips(false)
                                      .useDualStack(false)
                                      .forcePathStyle(false)
                                      .accelerate(false)
                                      .useGlobalEndpoint(false)
                                      .disableMultiRegionAccessPoints(false)
                                      .build();
    }

    /** Case 0: vanilla virtual addressing@us-west-2 — the most common S3 path. */
    @Benchmark
    public void case0_virtualAddressing(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case0Params).join());
    }

    /** Case 1: vanilla path style@us-west-2. */
    @Benchmark
    public void case1_pathStyle(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case1Params).join());
    }

    /** Case 2: S3 Express data-plane with short zone name (--x-s3 suffix). */
    @Benchmark
    public void case2_s3ExpressDataPlane(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case2Params).join());
    }

    /** Case 3: vanilla access point ARN@us-west-2. */
    @Benchmark
    public void case3_accessPointArn(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case3Params).join());
    }

    /** Case 4: S3 Outposts vanilla test. */
    @Benchmark
    public void case4_outposts(Blackhole bh) {
        bh.consume(provider.resolveEndpoint(case4Params).join());
    }
}
