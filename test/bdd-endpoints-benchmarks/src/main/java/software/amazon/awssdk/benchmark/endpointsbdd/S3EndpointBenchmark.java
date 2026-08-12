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
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineBddEndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineRulesEndpointResolver;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.BytecodeEndpointResolver;

/**
 * Compares S3 endpoint resolution across four resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — SDK v2 rules-based (no BDD)</li>
 *   <li>{@code "baselineBdd"} — SDK v2 original table-driven BDD</li>
 *   <li>{@code "optimizedBdd"} — SDK v2 optimized inlined-branch BDD</li>
 *   <li>{@code "smithyJava"} — smithy-java {@link BytecodeEndpointResolver}</li>
 * </ul>
 *
 * <p>The five cases mirror those in {@code S3BddEndpointCaseBenchmark} and
 * {@code S3EndpointResolverBenchmark}: virtual addressing, path style, S3 Express data plane,
 * access point ARN, and S3 Outposts.
 *
 * <p>For {@code smithyJava}, parameters are passed entirely via {@code ADDITIONAL_ENDPOINT_PARAMS}
 * matching the S3 reference benchmark's "canned" param mode — bucket and key are in the params
 * map, not extracted from the input shape.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class S3EndpointBenchmark {

    private static final long SHUFFLE_SEED = 20260730L;

    @Param({"rules", "baselineBdd", "optimizedBdd", "smithyJava"})
    private String resolver;

    private S3EndpointProvider sdkProvider;
    private EndpointResolver smithyProvider;

    private S3EndpointParams case0SdkParams;
    private S3EndpointParams case1SdkParams;
    private S3EndpointParams case2SdkParams;
    private S3EndpointParams case3SdkParams;
    private S3EndpointParams case4SdkParams;

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
            case "optimizedBdd": sdkProvider = new DefaultS3EndpointProvider(); break;
            case "smithyJava":   smithyProvider = SmithyJavaResolverFactory.forS3(); break;
            default: throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        // Case 0: vanilla virtual addressing@us-west-2
        case0SdkParams = S3EndpointParams.builder()
                                         .bucket("bucket-name").region(Region.US_WEST_2)
                                         .useFips(false).useDualStack(false)
                                         .forcePathStyle(false).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false)
                                         .build();
        case0SmithyParams = SmithyJavaResolverFactory.params("us-west-2",
                Map.of("Accelerate", false, "Bucket", "bucket-name", "ForcePathStyle", false,
                       "Region", "us-west-2", "UseDualStack", false, "UseFIPS", false));

        // Case 1: vanilla path style@us-west-2
        case1SdkParams = S3EndpointParams.builder()
                                         .bucket("bucket-name").region(Region.US_WEST_2)
                                         .useFips(false).useDualStack(false)
                                         .forcePathStyle(true).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false)
                                         .build();
        case1SmithyParams = SmithyJavaResolverFactory.params("us-west-2",
                Map.of("Accelerate", false, "Bucket", "bucket-name", "ForcePathStyle", true,
                       "Region", "us-west-2", "UseDualStack", false, "UseFIPS", false));

        // Case 2: S3 Express data plane with short zone name
        case2SdkParams = S3EndpointParams.builder()
                                         .bucket("mybucket--abcd-ab1--x-s3").region(Region.US_EAST_1)
                                         .useFips(false).useDualStack(false)
                                         .forcePathStyle(false).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false)
                                         .disableS3ExpressSessionAuth(false)
                                         .build();
        case2SmithyParams = SmithyJavaResolverFactory.params("us-east-1",
                Map.of("Region", "us-east-1", "Bucket", "mybucket--abcd-ab1--x-s3",
                       "UseFIPS", false, "UseDualStack", false, "Accelerate", false,
                       "UseS3ExpressControlEndpoint", false));

        // Case 3: vanilla access point ARN@us-west-2
        case3SdkParams = S3EndpointParams.builder()
                                         .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                         .region(Region.US_WEST_2)
                                         .useFips(false).useDualStack(false)
                                         .forcePathStyle(false).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false)
                                         .build();
        case3SmithyParams = SmithyJavaResolverFactory.params("us-west-2",
                Map.of("Accelerate", false,
                       "Bucket", "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint",
                       "ForcePathStyle", false,
                       "Region", "us-west-2", "UseDualStack", false, "UseFIPS", false));

        // Case 4: S3 Outposts vanilla test
        case4SdkParams = S3EndpointParams.builder()
                                         .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                         .region(Region.US_WEST_2)
                                         .useFips(false).useDualStack(false)
                                         .forcePathStyle(false).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false)
                                         .build();
        case4SmithyParams = SmithyJavaResolverFactory.params("us-west-2",
                Map.of("Region", "us-west-2", "UseFIPS", false, "UseDualStack", false,
                       "Accelerate", false,
                       "Bucket", "arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports"));

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
                bh.consume(sdkProvider.resolveEndpoint((S3EndpointParams) p).join());
            }
        } else {
            for (Object p : shuffledCases) {
                bh.consume(smithyProvider.resolveEndpoint((EndpointResolverParams) p));
            }
        }
    }

    // ------------------------------------------------------------------------------------- per-case

    @Benchmark
    public void case0_virtualAddressing(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case0SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case0SmithyParams));
        }
    }

    @Benchmark
    public void case1_pathStyle(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case1SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case1SmithyParams));
        }
    }

    @Benchmark
    public void case2_s3ExpressDataPlane(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case2SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case2SmithyParams));
        }
    }

    @Benchmark
    public void case3_accessPointArn(Blackhole bh) {
        if (sdkProvider != null) {
            bh.consume(sdkProvider.resolveEndpoint(case3SdkParams).join());
        } else {
            bh.consume(smithyProvider.resolveEndpoint(case3SmithyParams));
        }
    }

    @Benchmark
    public void case4_outposts(Blackhole bh) {
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
