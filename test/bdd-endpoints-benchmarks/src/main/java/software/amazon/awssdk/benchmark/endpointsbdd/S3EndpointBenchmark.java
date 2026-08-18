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
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.GeneratedEndpointResolver;

/**
 * Compares S3 endpoint resolution across five resolver implementations:
 * <ul>
 *   <li>{@code "rules"} — SDK v2 rules-based (no BDD)</li>
 *   <li>{@code "baselineBdd"} — SDK v2 original table-driven BDD</li>
 *   <li>{@code "optimizedBdd"} — SDK v2 optimized inlined-branch BDD</li>
 *   <li>{@code "smithyJavaGenerated"} — smithy-java code-generated resolver with pre-marshalled
 *       {@link GeneratedEndpointResolver.GeneratedParameters} fast path</li>
 * </ul>
 *
 * <p>Cases mirror the smithy-java reference S3 benchmarks: virtual addressing, path style,
 * S3 Express data plane, access point ARN, and S3 Outposts. All smithy-java params are
 * passed via {@code ADDITIONAL_ENDPOINT_PARAMS} (canned mode, no input-shape extraction).
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class S3EndpointBenchmark {

    private static final long SHUFFLE_SEED = 20260730L;

    @Param({"rules", "baselineBdd", "optimizedBdd", "smithyJavaGenerated"})
    private String resolver;

    private S3EndpointProvider sdkProvider;
    private SmithyJavaResolverFactory.Resolvers smithyResolvers;

    private S3EndpointParams case0SdkParams;
    private S3EndpointParams case1SdkParams;
    private S3EndpointParams case2SdkParams;
    private S3EndpointParams case3SdkParams;
    private S3EndpointParams case4SdkParams;

    private EndpointResolverParams case0BytecodeParams;
    private EndpointResolverParams case1BytecodeParams;
    private EndpointResolverParams case2BytecodeParams;
    private EndpointResolverParams case3BytecodeParams;
    private EndpointResolverParams case4BytecodeParams;

    private GeneratedEndpointResolver.GeneratedParameters case0GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case1GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case2GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case3GeneratedParams;
    private GeneratedEndpointResolver.GeneratedParameters case4GeneratedParams;

    private List<Object> shuffledCases;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        switch (resolver) {
            case "rules":               sdkProvider = new BaselineRulesEndpointResolver(); break;
            case "baselineBdd":         sdkProvider = new BaselineBddEndpointProvider(); break;
            case "optimizedBdd":        sdkProvider = new OptimizedBddS3EndpointProvider(); break;
            case "smithyJavaGenerated":
                smithyResolvers = SmithyJavaResolverFactory.forS3();
                break;
            default: throw new IllegalArgumentException("Unknown resolver: " + resolver);
        }

        Map<String, Object> p0 = Map.of("Accelerate", false, "Bucket", "bucket-name",
                                        "ForcePathStyle", false, "Region", "us-west-2",
                                        "UseDualStack", false, "UseFIPS", false);
        Map<String, Object> p1 = Map.of("Accelerate", false, "Bucket", "bucket-name",
                                        "ForcePathStyle", true, "Region", "us-west-2",
                                        "UseDualStack", false, "UseFIPS", false);
        Map<String, Object> p2 = Map.of("Region", "us-east-1", "Bucket", "mybucket--abcd-ab1--x-s3",
                                        "UseFIPS", false, "UseDualStack", false,
                                        "Accelerate", false, "UseS3ExpressControlEndpoint", false);
        Map<String, Object> p3 = Map.of("Accelerate", false,
                                        "Bucket", "arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint",
                                        "ForcePathStyle", false, "Region", "us-west-2",
                                        "UseDualStack", false, "UseFIPS", false);
        Map<String, Object> p4 = Map.of("Region", "us-west-2", "UseFIPS", false, "UseDualStack", false,
                                        "Accelerate", false,
                                        "Bucket", "arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports");

        case0SdkParams = S3EndpointParams.builder().bucket("bucket-name").region(Region.US_WEST_2)
                                         .useFips(false).useDualStack(false).forcePathStyle(false)
                                         .accelerate(false).useGlobalEndpoint(false).disableMultiRegionAccessPoints(false).build();
        case1SdkParams = S3EndpointParams.builder().bucket("bucket-name").region(Region.US_WEST_2)
                                         .useFips(false).useDualStack(false).forcePathStyle(true)
                                         .accelerate(false).useGlobalEndpoint(false).disableMultiRegionAccessPoints(false).build();
        case2SdkParams = S3EndpointParams.builder().bucket("mybucket--abcd-ab1--x-s3").region(Region.US_EAST_1)
                                         .useFips(false).useDualStack(false).forcePathStyle(false)
                                         .accelerate(false).useGlobalEndpoint(false).disableMultiRegionAccessPoints(false)
                                         .disableS3ExpressSessionAuth(false).build();
        case3SdkParams = S3EndpointParams.builder()
                                         .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                         .region(Region.US_WEST_2).useFips(false).useDualStack(false)
                                         .forcePathStyle(false).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false).build();
        case4SdkParams = S3EndpointParams.builder()
                                         .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                         .region(Region.US_WEST_2).useFips(false).useDualStack(false)
                                         .forcePathStyle(false).accelerate(false)
                                         .useGlobalEndpoint(false).disableMultiRegionAccessPoints(false).build();

        case0BytecodeParams = SmithyJavaResolverFactory.params("us-west-2", p0);
        case1BytecodeParams = SmithyJavaResolverFactory.params("us-west-2", p1);
        case2BytecodeParams = SmithyJavaResolverFactory.params("us-east-1", p2);
        case3BytecodeParams = SmithyJavaResolverFactory.params("us-west-2", p3);
        case4BytecodeParams = SmithyJavaResolverFactory.params("us-west-2", p4);

        if (smithyResolvers != null) {
            case0GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p0);
            case1GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p1);
            case2GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p2);
            case3GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p3);
            case4GeneratedParams = SmithyJavaResolverFactory.generatedParams(smithyResolvers.generated, p4);
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
                bh.consume(sdkProvider.resolveEndpoint((S3EndpointParams) p).join());
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
    public void case0_virtualAddressing(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case0SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case0BytecodeParams.context(), case0GeneratedParams));
    }

    @Benchmark
    public void case1_pathStyle(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case1SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case1BytecodeParams.context(), case1GeneratedParams));
    }

    @Benchmark
    public void case2_s3ExpressDataPlane(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case2SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case2BytecodeParams.context(), case2GeneratedParams));
    }

    @Benchmark
    public void case3_accessPointArn(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case3SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case3BytecodeParams.context(), case3GeneratedParams));
    }

    @Benchmark
    public void case4_outposts(Blackhole bh) {
        if (sdkProvider != null)   bh.consume(sdkProvider.resolveEndpoint(case4SdkParams).join());
        else                       bh.consume(asGenerated().resolveEndpoint(case4BytecodeParams.context(), case4GeneratedParams));
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
            cases.add(case3SdkParams); cases.add(case4SdkParams);
        } else {
            cases.add(case0GeneratedParams); cases.add(case1GeneratedParams); cases.add(case2GeneratedParams);
            cases.add(case3GeneratedParams); cases.add(case4GeneratedParams);
        }
        return cases;
    }
}
