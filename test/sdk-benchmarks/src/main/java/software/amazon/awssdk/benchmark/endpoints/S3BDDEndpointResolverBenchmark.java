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

import java.util.HashMap;
import java.util.Map;
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
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.internal.BDDResolverRuntimeDAG2;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.BDDResolverRuntimeDAGWithCache;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineRulesResolver;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineRulesResolverOldStdLib;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt2Runtime3;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt2Runtime4;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt2Subgraphs;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt3Runtime4;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt3Subgraph2;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt3Subgraph2_1;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOptRuntime3;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedInlineSwitches;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedMethodHandleArray;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedRuntimeDag2;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedRuntimeDag2WithCache;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedRuntimeDag3;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedRuntimeDag4;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedRuntimeDag5;
import software.amazon.awssdk.services.s3.endpoints.internal.BddOptimizedRuntimeWithSwitches;
import software.amazon.awssdk.services.s3.endpoints.internal.BddResolverRuntimeDag3WithCache;
import software.amazon.awssdk.services.s3.endpoints.internal.BddStartingPointBaselineStdLib;
import software.amazon.awssdk.services.s3.endpoints.internal.RulesResolverWithCache;
import software.amazon.awssdk.services.s3.endpoints.internal.BddResolverCodegenDag2WithCache;

@State(Scope.Thread)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class S3BDDEndpointResolverBenchmark {
    S3EndpointProvider baselineRulesProvider = new BaselineRulesResolver();
    S3EndpointProvider baselineRulesProviderOldStdLib = new BaselineRulesResolverOldStdLib();
    S3EndpointProvider rulesProviderWithCache = new RulesResolverWithCache();
    // S3EndpointProvider bddResolverRuntimeDag = new BDDEndpointResolverDag();
    // S3EndpointProvider bddResolverCodegenDag = new BDDEndpointResolverCodegenDag();
    S3EndpointProvider bddResolverRuntimeDag2 = new BDDResolverRuntimeDAG2();
    S3EndpointProvider bddWithCache = new BDDResolverRuntimeDAGWithCache();
    S3EndpointProvider bddInlineWithCache = new BddResolverCodegenDag2WithCache();

    //uses switch statements AND optimized (sifted/reversed) BDD.
    S3EndpointProvider bddRuntime3WithCache = new BddResolverRuntimeDag3WithCache();
    S3EndpointProvider optimizedBddRuntimeWithSwitches = new BddOptimizedRuntimeWithSwitches();

    // uses array of condition/result lambdas, but optimized bdd
    S3EndpointProvider optimizedBddRuntime2WithCache = new BddOptimizedRuntimeDag2WithCache();
    S3EndpointProvider optimizedBddRuntime2 = new BddOptimizedRuntimeDag2();

    // optimized BDD with condition/result switches inlined (eliminates Registry structure)
    S3EndpointProvider optimizedBddRuntimeInline = new BddOptimizedInlineSwitches();

    S3EndpointProvider optimizedBddRuntimeMethodHandleArray = new BddOptimizedMethodHandleArray();
    //
    // optimized bdd with boolean boxing fixes + tighter runtime loop.  still using lambdas
    S3EndpointProvider bddRuntime3 = new BddOptimizedRuntimeDag3();

    // optimized bdd with boolean boxing fixes + tighter runtime loop.  using method references in array.
    S3EndpointProvider bddRuntime4 = new BddOptimizedRuntimeDag4();

    // optimized bdd with optmized URI creation + boolean and loop optimizations.  Uses lambdas
    S3EndpointProvider bddRuntime5 = new BddOptimizedRuntimeDag5();

    // cost optimized bdd.  boolean and loop optimizations.  Uses lambdas.  NO uriCreate optimizations
    S3EndpointProvider bddCostOptRuntime3 = new BddCostOptRuntime3();

    // cost optimized round 2 bdd.  boolean and loop optimizations.  Uses lambdas.  NO uriCreate optimizations
    S3EndpointProvider bddCostOpt2Runtime3 = new BddCostOpt2Runtime3();

    // cost optimized round 2 bdd.  boolean and loop optimizations.  Method references  NO uriCreate optimizations
    S3EndpointProvider bddCostOpt2Runtime4 = new BddCostOpt2Runtime4();



    // cost optimized round 2 bdd.  Uses static cond/result methods + codegen nodes.  Uses Ruby codegenerated subgraphs
    S3EndpointProvider bddCostOpt2Subgraphs = new BddCostOpt2Subgraphs();

    // cost optimized round 2 bdd.  boolean and loop optimizations.  Method references  NO uriCreate optimizations
    S3EndpointProvider bddCostOpt3Runtime4 = new BddCostOpt3Runtime4();

    // cost optimized round 2 bdd.  boolean and loop optimizations.  Method references. subgraph optimization 2 - nodes in BDD
    // replaced with special result functions that collapse subgraph/trees to bigger if/else blocks
    // extra optimized loop, removes complimented checks.
    S3EndpointProvider bddCostOpt3Subgraph2 = new BddCostOpt3Subgraph2();

    // same as bddCostOpt3Subgraph2 but has inlined conditions
    S3EndpointProvider bddCostOpt3Subgraph2_1 = new BddCostOpt3Subgraph2_1();

    // this was the naive first attempt at BDD runtime.  Uses Object[] for registry. no loop optimization.
    // Uses baseline (non-optimized) std lib.
    S3EndpointProvider naiveBdd = new BddStartingPointBaselineStdLib();

    Map<String, S3EndpointParams> nonErrorCases;
    Map<String, S3EndpointParams>  errorCases;


    public S3BDDEndpointResolverBenchmark() {
        setupBenchmarkCases();
        // setupSimpleCasesOnly();
        System.out.println("Number of test cases: " + nonErrorCases.size());
    }

    public void runTest(Blackhole blackhole, S3EndpointProvider endpointResolver) {
        for (S3EndpointParams param: nonErrorCases.values()) {
            blackhole.consume(endpointResolver.resolveEndpoint(param).join());
        }
    }

    @Benchmark
    public void bddMethodReferences(Blackhole blackhole) {
        runTest(blackhole, bddRuntime4);
    }

    @Benchmark
    public void bddCostOptimized3(Blackhole blackhole) {
        runTest(blackhole, bddCostOpt3Runtime4);
    }

    @Benchmark
    public void bddCostOptimized2(Blackhole blackhole) {
        runTest(blackhole, bddCostOpt3Runtime4);
    }

    @Benchmark
    public void bddCostOpt3Subgraph2(Blackhole blackhole) {
        runTest(blackhole, bddCostOpt3Subgraph2);
    }

    @Benchmark
    public void bddCostOpt3Subgraph2_1(Blackhole blackhole) {
        runTest(blackhole, bddCostOpt3Subgraph2_1);
    }

    @Benchmark
    public void naiveBdd(Blackhole blackhole) {
        runTest(blackhole, naiveBdd);
    }

    @Benchmark
    public void baselineRulesResolver(Blackhole blackhole) {
        runTest(blackhole, baselineRulesProvider);
    }

    @Benchmark
    public void baselineRulesResolverOldStdLib(Blackhole blackhole) {
        runTest(blackhole, baselineRulesProviderOldStdLib);
    }

    // @Benchmark
    // public void bddCostOptimized2(Blackhole blackhole) {
    //     runTest(blackhole, bddCostOpt2Runtime3);
    // }
    //
    // @Benchmark
    // public void bddWithSubgraphs(Blackhole blackhole) {
    //     runTest(blackhole, bddCostOpt2Subgraphs);
    // }

    // @Benchmark
    // public void rulesResolverWithCache(Blackhole blackhole) {
    //     runTest(blackhole, rulesProviderWithCache);
    // }
    //
    // @Benchmark
    // public void bddRuntimeDagWithCache(Blackhole blackhole) {
    //     runTest(blackhole, bddWithCache);
    // }
    //
    // @Benchmark
    // public void smithyJavaBddResolver(Blackhole blackhole) {
    //     for (EndpointResolverParams p : smithyParams) {
    //         blackhole.consume(smithyBddResolver.resolveEndpoint(p));
    //     }
    // }

    private void setupBasicGetObjectTestOnly() {
        nonErrorCases = new HashMap<>();

        nonErrorCases.put("0: Basic getObject, standard Bucket", S3EndpointParams
            .builder()
            .region(Region.of("us-east-1"))
            .useFips(false)
            .useDualStack(false)
            .accelerate(false)
            .disableAccessPoints(false)
            .bucket("mybucket")
            .build());
    }

    // setup all non-error S3 test cases (note that these were 1 off code-generated from endpoint-test-cases-1.json)
    private void setupBenchmarkCases() {
        nonErrorCases = new HashMap<>();

        nonErrorCases.put("6: Access points (disable access points explicitly false)", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-east-1"))
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(false)
                                                                                                       .accelerate(false)
                                                                                                       .disableAccessPoints(false)
                                                                                                       .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                       .build());

        nonErrorCases.put("12: Access point ARN with FIPS & Dualstack", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .useFips(true)
                                                                                        .useDualStack(true)
                                                                                        .accelerate(false)
                                                                                        .disableAccessPoints(false)
                                                                                        .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                        .build());

        nonErrorCases.put("13: Access point ARN with Dualstack", S3EndpointParams.builder()
                                                                                 .region(Region.of("us-east-1"))
                                                                                 .useFips(false)
                                                                                 .useDualStack(true)
                                                                                 .accelerate(false)
                                                                                 .disableAccessPoints(false)
                                                                                 .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                 .build());

        nonErrorCases.put("14: vanilla MRAP", S3EndpointParams.builder()
                                                              .bucket("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                              .region(Region.of("us-east-1"))
                                                              .disableMultiRegionAccessPoints(false)
                                                              .useFips(false)
                                                              .useDualStack(false)
                                                              .accelerate(false)
                                                              .build());

        nonErrorCases.put("19: Dual-stack endpoint with path-style forced", S3EndpointParams.builder()
                                                                                            .bucket("bucketname")
                                                                                            .region(Region.of("us-west-2"))
                                                                                            .forcePathStyle(true)
                                                                                            .useFips(false)
                                                                                            .accelerate(false)
                                                                                            .useDualStack(true)
                                                                                            .build());

        nonErrorCases.put("22: implicit path style bucket + dualstack", S3EndpointParams.builder()
                                                                                        .accelerate(false)
                                                                                        .bucket("99_ab")
                                                                                        .region(Region.of("us-west-2"))
                                                                                        .useDualStack(true)
                                                                                        .useFips(false)
                                                                                        .build());

        nonErrorCases.put("24: don't allow URL injections in the bucket", S3EndpointParams.builder()
                                                                                          .bucket("example.com#")
                                                                                          .region(Region.of("us-west-2"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .accelerate(false)
                                                                                          .build());

        nonErrorCases.put("25: URI encode bucket names in the path", S3EndpointParams.builder()
                                                                                     .bucket("bucket name")
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .useDualStack(false)
                                                                                     .useFips(false)
                                                                                     .accelerate(false)
                                                                                     .build());

        nonErrorCases.put("26: scheme is respected", S3EndpointParams.builder()
                                                                     .accelerate(false)
                                                                     .bucket("99_ab")
                                                                     .endpoint("http://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                     .region(Region.of("af-south-1"))
                                                                     .useDualStack(false)
                                                                     .useFips(false)
                                                                     .build());

        nonErrorCases.put("27: scheme is respected (virtual addressing)", S3EndpointParams.builder()
                                                                                          .accelerate(false)
                                                                                          .bucket("bucketname")
                                                                                          .endpoint("http://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com/foo")
                                                                                          .region(Region.of("af-south-1"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        nonErrorCases.put("28: path style + implicit private link", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("99_ab")
                                                                                    .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                    .region(Region.of("af-south-1"))
                                                                                    .useDualStack(false)
                                                                                    .useFips(false)
                                                                                    .build());

        nonErrorCases.put("30: using an IPv4 address forces path style", S3EndpointParams.builder()
                                                                                         .accelerate(false)
                                                                                         .bucket("bucketname")
                                                                                         .endpoint("https://123.123.0.1")
                                                                                         .region(Region.of("af-south-1"))
                                                                                         .useDualStack(false)
                                                                                         .useFips(false)
                                                                                         .build());

        nonErrorCases.put("32: vanilla access point arn with region mismatch and UseArnRegion unset", S3EndpointParams.builder()
                                                                                                                      .accelerate(false)
                                                                                                                      .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                                      .forcePathStyle(false)
                                                                                                                      .region(Region.of("us-east-1"))
                                                                                                                      .useDualStack(false)
                                                                                                                      .useFips(false)
                                                                                                                      .build());

        nonErrorCases.put("33: vanilla access point arn with region mismatch and UseArnRegion=true", S3EndpointParams.builder()
                                                                                                                     .accelerate(false)
                                                                                                                     .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                                     .forcePathStyle(false)
                                                                                                                     .useArnRegion(true)
                                                                                                                     .region(Region.of("us-east-1"))
                                                                                                                     .useDualStack(false)
                                                                                                                     .useFips(false)
                                                                                                                     .build());

        nonErrorCases.put("34: subdomains are not allowed in virtual buckets", S3EndpointParams.builder()
                                                                                               .bucket("bucket.name")
                                                                                               .region(Region.of("us-east-1"))
                                                                                               .build());

        nonErrorCases.put("35: bucket names with 3 characters are allowed in virtual buckets", S3EndpointParams.builder()
                                                                                                               .bucket("aaa")
                                                                                                               .region(Region.of("us-east-1"))
                                                                                                               .build());

        nonErrorCases.put("36: bucket names with fewer than 3 characters are not allowed in virtual host", S3EndpointParams.builder()
                                                                                                                           .bucket("aa")
                                                                                                                           .region(Region.of("us-east-1"))
                                                                                                                           .build());

        nonErrorCases.put("37: bucket names with uppercase characters are not allowed in virtual host", S3EndpointParams.builder()
                                                                                                                        .bucket("BucketName")
                                                                                                                        .region(Region.of("us-east-1"))
                                                                                                                        .build());

        nonErrorCases.put("38: subdomains are allowed in virtual buckets on http endpoints", S3EndpointParams.builder()
                                                                                                             .bucket("bucket.name")
                                                                                                             .region(Region.of("us-east-1"))
                                                                                                             .endpoint("http://example.com")
                                                                                                             .build());

        nonErrorCases.put("40: UseGlobalEndpoints=true, region=us-east-1 uses the global endpoint", S3EndpointParams.builder()
                                                                                                                    .region(Region.of("us-east-1"))
                                                                                                                    .useGlobalEndpoint(true)
                                                                                                                    .useFips(false)
                                                                                                                    .useDualStack(false)
                                                                                                                    .accelerate(false)
                                                                                                                    .build());

        nonErrorCases.put("41: UseGlobalEndpoints=true, region=us-west-2 uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                      .region(Region.of("us-west-2"))
                                                                                                                      .useGlobalEndpoint(true)
                                                                                                                      .useFips(false)
                                                                                                                      .useDualStack(false)
                                                                                                                      .accelerate(false)
                                                                                                                      .build());

        nonErrorCases.put("42: UseGlobalEndpoints=true, region=cn-north-1 uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                       .region(Region.of("cn-north-1"))
                                                                                                                       .useGlobalEndpoint(true)
                                                                                                                       .useFips(false)
                                                                                                                       .useDualStack(false)
                                                                                                                       .accelerate(false)
                                                                                                                       .build());

        nonErrorCases.put("43: UseGlobalEndpoints=true, region=us-east-1, fips=true uses the regional endpoint with fips", S3EndpointParams.builder()
                                                                                                                                           .region(Region.of("us-east-1"))
                                                                                                                                           .useGlobalEndpoint(true)
                                                                                                                                           .useFips(true)
                                                                                                                                           .useDualStack(false)
                                                                                                                                           .accelerate(false)
                                                                                                                                           .build());

        nonErrorCases.put("44: UseGlobalEndpoints=true, region=us-east-1, dualstack=true uses the regional endpoint with dualstack", S3EndpointParams.builder()
                                                                                                                                                     .region(Region.of("us-east-1"))
                                                                                                                                                     .useGlobalEndpoint(true)
                                                                                                                                                     .useFips(false)
                                                                                                                                                     .useDualStack(true)
                                                                                                                                                     .accelerate(false)
                                                                                                                                                     .build());

        nonErrorCases.put("45: UseGlobalEndpoints=true, region=us-east-1, dualstack and fips uses the regional endpoint with", S3EndpointParams.builder()
                                                                                                                                               .region(Region.of("us-east-1"))
                                                                                                                                               .useGlobalEndpoint(true)
                                                                                                                                               .useFips(true)
                                                                                                                                               .useDualStack(true)
                                                                                                                                               .accelerate(false)
                                                                                                                                               .build());

        nonErrorCases.put("46: UseGlobalEndpoints=true, region=us-east-1 with custom endpoint, uses custom", S3EndpointParams.builder()
                                                                                                                             .region(Region.of("us-east-1"))
                                                                                                                             .endpoint("https://example.com")
                                                                                                                             .useGlobalEndpoint(true)
                                                                                                                             .useFips(false)
                                                                                                                             .useDualStack(false)
                                                                                                                             .accelerate(false)
                                                                                                                             .build());

        nonErrorCases.put("47: UseGlobalEndpoints=true, region=us-west-2 with custom endpoint, uses custom", S3EndpointParams.builder()
                                                                                                                             .region(Region.of("us-west-2"))
                                                                                                                             .endpoint("https://example.com")
                                                                                                                             .useGlobalEndpoint(true)
                                                                                                                             .useFips(false)
                                                                                                                             .useDualStack(false)
                                                                                                                             .accelerate(false)
                                                                                                                             .build());

        nonErrorCases.put("48: UseGlobalEndpoints=true, region=us-east-1 with accelerate on non bucket case uses the global endpoint and", S3EndpointParams.builder()
                                                                                                                                                           .region(Region.of("us-east-1"))
                                                                                                                                                           .useGlobalEndpoint(true)
                                                                                                                                                           .useFips(false)
                                                                                                                                                           .useDualStack(false)
                                                                                                                                                           .accelerate(true)
                                                                                                                                                           .build());

        nonErrorCases.put("49: aws-global region uses the global endpoint", S3EndpointParams.builder()
                                                                                            .region(Region.of("aws-global"))
                                                                                            .useFips(false)
                                                                                            .useDualStack(false)
                                                                                            .accelerate(false)
                                                                                            .build());

        nonErrorCases.put("50: aws-global region with fips uses the regional endpoint", S3EndpointParams.builder()
                                                                                                        .region(Region.of("aws-global"))
                                                                                                        .useFips(true)
                                                                                                        .useDualStack(false)
                                                                                                        .accelerate(false)
                                                                                                        .build());

        nonErrorCases.put("51: aws-global region with dualstack uses the regional endpoint", S3EndpointParams.builder()
                                                                                                             .region(Region.of("aws-global"))
                                                                                                             .useFips(false)
                                                                                                             .useDualStack(true)
                                                                                                             .accelerate(false)
                                                                                                             .build());

        nonErrorCases.put("52: aws-global region with fips and dualstack uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                      .region(Region.of("aws-global"))
                                                                                                                      .useFips(true)
                                                                                                                      .useDualStack(true)
                                                                                                                      .accelerate(false)
                                                                                                                      .build());

        nonErrorCases.put("53: aws-global region with accelerate on non-bucket case, uses global endpoint and ignores accelerate", S3EndpointParams.builder()
                                                                                                                                                   .region(Region.of("aws-global"))
                                                                                                                                                   .useFips(false)
                                                                                                                                                   .useDualStack(false)
                                                                                                                                                   .accelerate(true)
                                                                                                                                                   .build());

        nonErrorCases.put("54: aws-global region with custom endpoint, uses custom", S3EndpointParams.builder()
                                                                                                     .region(Region.of("aws-global"))
                                                                                                     .endpoint("https://example.com")
                                                                                                     .useGlobalEndpoint(false)
                                                                                                     .useFips(false)
                                                                                                     .useDualStack(false)
                                                                                                     .accelerate(false)
                                                                                                     .build());

        nonErrorCases.put("55: virtual addressing, aws-global region uses the global endpoint", S3EndpointParams.builder()
                                                                                                                .region(Region.of("aws-global"))
                                                                                                                .bucket("bucket-name")
                                                                                                                .useFips(false)
                                                                                                                .useDualStack(false)
                                                                                                                .accelerate(false)
                                                                                                                .build());

        nonErrorCases.put("56: virtual addressing, aws-global region with Prefix, and Key uses the global endpoint. Prefix and Key", S3EndpointParams.builder()
                                                                                                                                                     .region(Region.of("aws-global"))
                                                                                                                                                     .bucket("bucket-name")
                                                                                                                                                     .useFips(false)
                                                                                                                                                     .useDualStack(false)
                                                                                                                                                     .accelerate(false)
                                                                                                                                                     .prefix("prefix")
                                                                                                                                                     .key("key")
                                                                                                                                                     .build());

        nonErrorCases.put("57: virtual addressing, aws-global region with Copy Source, and Key uses the global endpoint. Copy Source and", S3EndpointParams.builder()
                                                                                                                                                           .region(Region.of("aws-global"))
                                                                                                                                                           .bucket("bucket-name")
                                                                                                                                                           .useFips(false)
                                                                                                                                                           .useDualStack(false)
                                                                                                                                                           .accelerate(false)
                                                                                                                                                           .copySource("/copy/source")
                                                                                                                                                           .key("key")
                                                                                                                                                           .build());

        nonErrorCases.put("58: virtual addressing, aws-global region with fips uses the regional fips endpoint", S3EndpointParams.builder()
                                                                                                                                 .region(Region.of("aws-global"))
                                                                                                                                 .bucket("bucket-name")
                                                                                                                                 .useFips(true)
                                                                                                                                 .useDualStack(false)
                                                                                                                                 .accelerate(false)
                                                                                                                                 .build());

        nonErrorCases.put("59: virtual addressing, aws-global region with dualstack uses the regional dualstack endpoint", S3EndpointParams.builder()
                                                                                                                                           .region(Region.of("aws-global"))
                                                                                                                                           .bucket("bucket-name")
                                                                                                                                           .useFips(false)
                                                                                                                                           .useDualStack(true)
                                                                                                                                           .accelerate(false)
                                                                                                                                           .build());

        nonErrorCases.put("60: virtual addressing, aws-global region with fips/dualstack uses the regional fips/dualstack endpoint", S3EndpointParams.builder()
                                                                                                                                                     .region(Region.of("aws-global"))
                                                                                                                                                     .bucket("bucket-name")
                                                                                                                                                     .useFips(true)
                                                                                                                                                     .useDualStack(true)
                                                                                                                                                     .accelerate(false)
                                                                                                                                                     .build());

        nonErrorCases.put("61: virtual addressing, aws-global region with accelerate uses the global accelerate endpoint", S3EndpointParams.builder()
                                                                                                                                           .region(Region.of("aws-global"))
                                                                                                                                           .bucket("bucket-name")
                                                                                                                                           .useFips(false)
                                                                                                                                           .useDualStack(false)
                                                                                                                                           .accelerate(true)
                                                                                                                                           .build());

        nonErrorCases.put("62: virtual addressing, aws-global region with custom endpoint", S3EndpointParams.builder()
                                                                                                            .region(Region.of("aws-global"))
                                                                                                            .endpoint("https://example.com")
                                                                                                            .bucket("bucket-name")
                                                                                                            .useFips(false)
                                                                                                            .useDualStack(false)
                                                                                                            .accelerate(false)
                                                                                                            .build());

        nonErrorCases.put("63: virtual addressing, UseGlobalEndpoint and us-east-1 region uses the global endpoint", S3EndpointParams.builder()
                                                                                                                                     .region(Region.of("us-east-1"))
                                                                                                                                     .useGlobalEndpoint(true)
                                                                                                                                     .bucket("bucket-name")
                                                                                                                                     .useFips(false)
                                                                                                                                     .useDualStack(false)
                                                                                                                                     .accelerate(false)
                                                                                                                                     .build());

        nonErrorCases.put("64: virtual addressing, UseGlobalEndpoint and us-west-2 region uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                                       .region(Region.of("us-west-2"))
                                                                                                                                       .useGlobalEndpoint(true)
                                                                                                                                       .bucket("bucket-name")
                                                                                                                                       .useFips(false)
                                                                                                                                       .useDualStack(false)
                                                                                                                                       .accelerate(false)
                                                                                                                                       .build());

        nonErrorCases.put("65: virtual addressing, UseGlobalEndpoint and us-east-1 region and fips uses the regional fips endpoint", S3EndpointParams.builder()
                                                                                                                                                     .region(Region.of("us-east-1"))
                                                                                                                                                     .useGlobalEndpoint(true)
                                                                                                                                                     .bucket("bucket-name")
                                                                                                                                                     .useFips(true)
                                                                                                                                                     .useDualStack(false)
                                                                                                                                                     .accelerate(false)
                                                                                                                                                     .build());

        nonErrorCases.put("66: virtual addressing, UseGlobalEndpoint and us-east-1 region and dualstack uses the regional dualstack", S3EndpointParams.builder()
                                                                                                                                                      .region(Region.of("us-east-1"))
                                                                                                                                                      .useGlobalEndpoint(true)
                                                                                                                                                      .bucket("bucket-name")
                                                                                                                                                      .useFips(false)
                                                                                                                                                      .useDualStack(true)
                                                                                                                                                      .accelerate(false)
                                                                                                                                                      .build());

        nonErrorCases.put("67: virtual addressing, UseGlobalEndpoint and us-east-1 region and accelerate uses the global accelerate", S3EndpointParams.builder()
                                                                                                                                                      .region(Region.of("us-east-1"))
                                                                                                                                                      .useGlobalEndpoint(true)
                                                                                                                                                      .bucket("bucket-name")
                                                                                                                                                      .useFips(false)
                                                                                                                                                      .useDualStack(false)
                                                                                                                                                      .accelerate(true)
                                                                                                                                                      .build());

        nonErrorCases.put("68: virtual addressing, UseGlobalEndpoint and us-east-1 region with custom endpoint", S3EndpointParams.builder()
                                                                                                                                 .region(Region.of("us-east-1"))
                                                                                                                                 .endpoint("https://example.com")
                                                                                                                                 .useGlobalEndpoint(true)
                                                                                                                                 .bucket("bucket-name")
                                                                                                                                 .useFips(false)
                                                                                                                                 .useDualStack(false)
                                                                                                                                 .accelerate(false)
                                                                                                                                 .build());

        nonErrorCases.put("69: ForcePathStyle, aws-global region uses the global endpoint", S3EndpointParams.builder()
                                                                                                            .region(Region.of("aws-global"))
                                                                                                            .bucket("bucket-name")
                                                                                                            .forcePathStyle(true)
                                                                                                            .useFips(false)
                                                                                                            .useDualStack(false)
                                                                                                            .accelerate(false)
                                                                                                            .build());

        nonErrorCases.put("70: ForcePathStyle, aws-global region with fips is invalid", S3EndpointParams.builder()
                                                                                                        .region(Region.of("aws-global"))
                                                                                                        .bucket("bucket-name")
                                                                                                        .forcePathStyle(true)
                                                                                                        .useFips(true)
                                                                                                        .useDualStack(false)
                                                                                                        .accelerate(false)
                                                                                                        .build());

        nonErrorCases.put("71: ForcePathStyle, aws-global region with dualstack uses regional dualstack endpoint", S3EndpointParams.builder()
                                                                                                                                   .region(Region.of("aws-global"))
                                                                                                                                   .bucket("bucket-name")
                                                                                                                                   .forcePathStyle(true)
                                                                                                                                   .useFips(false)
                                                                                                                                   .useDualStack(true)
                                                                                                                                   .accelerate(false)
                                                                                                                                   .build());

        nonErrorCases.put("72: ForcePathStyle, aws-global region custom endpoint uses the custom endpoint", S3EndpointParams.builder()
                                                                                                                            .region(Region.of("aws-global"))
                                                                                                                            .endpoint("https://example.com")
                                                                                                                            .bucket("bucket-name")
                                                                                                                            .forcePathStyle(true)
                                                                                                                            .useFips(false)
                                                                                                                            .useDualStack(false)
                                                                                                                            .accelerate(false)
                                                                                                                            .build());

        nonErrorCases.put("73: ForcePathStyle, UseGlobalEndpoint us-east-1 region uses the global endpoint", S3EndpointParams.builder()
                                                                                                                             .region(Region.of("us-east-1"))
                                                                                                                             .bucket("bucket-name")
                                                                                                                             .useGlobalEndpoint(true)
                                                                                                                             .forcePathStyle(true)
                                                                                                                             .useFips(false)
                                                                                                                             .useDualStack(false)
                                                                                                                             .accelerate(false)
                                                                                                                             .build());

        nonErrorCases.put("74: ForcePathStyle, UseGlobalEndpoint us-west-2 region uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                               .region(Region.of("us-west-2"))
                                                                                                                               .bucket("bucket-name")
                                                                                                                               .useGlobalEndpoint(true)
                                                                                                                               .forcePathStyle(true)
                                                                                                                               .useFips(false)
                                                                                                                               .useDualStack(false)
                                                                                                                               .accelerate(false)
                                                                                                                               .build());

        nonErrorCases.put("75: ForcePathStyle, UseGlobalEndpoint us-east-1 region, dualstack uses the regional dualstack endpoint", S3EndpointParams.builder()
                                                                                                                                                    .region(Region.of("us-east-1"))
                                                                                                                                                    .bucket("bucket-name")
                                                                                                                                                    .useGlobalEndpoint(true)
                                                                                                                                                    .forcePathStyle(true)
                                                                                                                                                    .useFips(false)
                                                                                                                                                    .useDualStack(true)
                                                                                                                                                    .accelerate(false)
                                                                                                                                                    .build());

        nonErrorCases.put("76: ForcePathStyle, UseGlobalEndpoint us-east-1 region custom endpoint uses the custom endpoint", S3EndpointParams.builder()
                                                                                                                                             .region(Region.of("us-east-1"))
                                                                                                                                             .bucket("bucket-name")
                                                                                                                                             .endpoint("https://example.com")
                                                                                                                                             .useGlobalEndpoint(true)
                                                                                                                                             .forcePathStyle(true)
                                                                                                                                             .useFips(false)
                                                                                                                                             .useDualStack(false)
                                                                                                                                             .accelerate(false)
                                                                                                                                             .build());

        nonErrorCases.put("77: ARN with aws-global region and UseArnRegion uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                        .region(Region.of("aws-global"))
                                                                                                                        .useArnRegion(true)
                                                                                                                        .useFips(false)
                                                                                                                        .useDualStack(false)
                                                                                                                        .accelerate(false)
                                                                                                                        .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                                                                        .build());

        nonErrorCases.put("79: Endpoint override, accesspoint with HTTP, port", S3EndpointParams.builder()
                                                                                                .endpoint("http://beta.example.com:1234")
                                                                                                .region(Region.of("us-west-2"))
                                                                                                .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                .build());

        nonErrorCases.put("80: Endpoint override, accesspoint with http, path, query, and port", S3EndpointParams.builder()
                                                                                                                 .region(Region.of("us-west-2"))
                                                                                                                 .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                                 .endpoint("http://beta.example.com:1234/path")
                                                                                                                 .useFips(false)
                                                                                                                 .useDualStack(false)
                                                                                                                 .accelerate(false)
                                                                                                                 .build());

        nonErrorCases.put("84: custom endpoint without FIPS/dualstack", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-west-2"))
                                                                                        .endpoint("http://beta.example.com:1234/path")
                                                                                        .useFips(false)
                                                                                        .useDualStack(false)
                                                                                        .build());

        nonErrorCases.put("86: non bucket + FIPS", S3EndpointParams.builder()
                                                                   .region(Region.of("us-west-2"))
                                                                   .useFips(true)
                                                                   .useDualStack(false)
                                                                   .build());

        nonErrorCases.put("87: standard non bucket endpoint", S3EndpointParams.builder()
                                                                              .region(Region.of("us-west-2"))
                                                                              .useFips(false)
                                                                              .useDualStack(false)
                                                                              .build());

        nonErrorCases.put("88: non bucket endpoint with FIPS + Dualstack", S3EndpointParams.builder()
                                                                                           .region(Region.of("us-west-2"))
                                                                                           .useFips(true)
                                                                                           .useDualStack(true)
                                                                                           .build());

        nonErrorCases.put("89: non bucket endpoint with dualstack", S3EndpointParams.builder()
                                                                                    .region(Region.of("us-west-2"))
                                                                                    .useFips(false)
                                                                                    .useDualStack(true)
                                                                                    .build());

        nonErrorCases.put("90: use global endpoint + IP address endpoint override", S3EndpointParams.builder()
                                                                                                    .region(Region.of("us-east-1"))
                                                                                                    .bucket("bucket")
                                                                                                    .useFips(false)
                                                                                                    .useDualStack(false)
                                                                                                    .endpoint("http://127.0.0.1")
                                                                                                    .useGlobalEndpoint(true)
                                                                                                    .build());

        nonErrorCases.put("91: non-dns endpoint + global endpoint", S3EndpointParams.builder()
                                                                                    .region(Region.of("us-east-1"))
                                                                                    .bucket("bucket!")
                                                                                    .useFips(false)
                                                                                    .useDualStack(false)
                                                                                    .useGlobalEndpoint(true)
                                                                                    .build());

        nonErrorCases.put("92: endpoint override + use global endpoint", S3EndpointParams.builder()
                                                                                         .region(Region.of("us-east-1"))
                                                                                         .bucket("bucket!")
                                                                                         .useFips(false)
                                                                                         .useDualStack(false)
                                                                                         .useGlobalEndpoint(true)
                                                                                         .endpoint("http://foo.com")
                                                                                         .build());

        nonErrorCases.put("93: FIPS + dualstack + non-bucket endpoint", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .bucket("bucket!")
                                                                                        .useFips(true)
                                                                                        .useDualStack(true)
                                                                                        .build());

        nonErrorCases.put("94: FIPS + dualstack + non-DNS endpoint", S3EndpointParams.builder()
                                                                                     .region(Region.of("us-east-1"))
                                                                                     .bucket("bucket!")
                                                                                     .forcePathStyle(true)
                                                                                     .useFips(true)
                                                                                     .useDualStack(true)
                                                                                     .build());

        nonErrorCases.put("97: FIPS + bucket endpoint + force path style", S3EndpointParams.builder()
                                                                                           .region(Region.of("us-east-1"))
                                                                                           .bucket("bucket!")
                                                                                           .forcePathStyle(true)
                                                                                           .useFips(true)
                                                                                           .useDualStack(false)
                                                                                           .useGlobalEndpoint(true)
                                                                                           .build());

        nonErrorCases.put("98: bucket + FIPS + force path style", S3EndpointParams.builder()
                                                                                  .region(Region.of("us-east-1"))
                                                                                  .bucket("bucket")
                                                                                  .forcePathStyle(true)
                                                                                  .useFips(true)
                                                                                  .useDualStack(true)
                                                                                  .useGlobalEndpoint(true)
                                                                                  .build());

        nonErrorCases.put("99: FIPS + dualstack + use global endpoint", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .bucket("bucket")
                                                                                        .useFips(true)
                                                                                        .useDualStack(true)
                                                                                        .useGlobalEndpoint(true)
                                                                                        .build());

        nonErrorCases.put("101: FIPS + path based endpoint", S3EndpointParams.builder()
                                                                             .region(Region.of("us-east-1"))
                                                                             .bucket("bucket!")
                                                                             .useFips(true)
                                                                             .useDualStack(false)
                                                                             .accelerate(false)
                                                                             .useGlobalEndpoint(true)
                                                                             .build());

        nonErrorCases.put("102: accelerate + dualstack + global endpoint", S3EndpointParams.builder()
                                                                                           .region(Region.of("us-east-1"))
                                                                                           .bucket("bucket")
                                                                                           .useFips(false)
                                                                                           .useDualStack(true)
                                                                                           .accelerate(true)
                                                                                           .useGlobalEndpoint(true)
                                                                                           .build());

        nonErrorCases.put("103: dualstack + global endpoint + non URI safe bucket", S3EndpointParams.builder()
                                                                                                    .region(Region.of("us-east-1"))
                                                                                                    .bucket("bucket!")
                                                                                                    .accelerate(false)
                                                                                                    .useDualStack(true)
                                                                                                    .useFips(false)
                                                                                                    .useGlobalEndpoint(true)
                                                                                                    .build());

        nonErrorCases.put("104: FIPS + uri encoded bucket", S3EndpointParams.builder()
                                                                            .region(Region.of("us-east-1"))
                                                                            .bucket("bucket!")
                                                                            .forcePathStyle(true)
                                                                            .accelerate(false)
                                                                            .useDualStack(false)
                                                                            .useFips(true)
                                                                            .useGlobalEndpoint(true)
                                                                            .build());

        nonErrorCases.put("106: FIPS + Dualstack + global endpoint + non-dns bucket", S3EndpointParams.builder()
                                                                                                      .region(Region.of("us-east-1"))
                                                                                                      .bucket("bucket!")
                                                                                                      .accelerate(false)
                                                                                                      .useDualStack(true)
                                                                                                      .useFips(true)
                                                                                                      .useGlobalEndpoint(true)
                                                                                                      .build());

        nonErrorCases.put("111: aws-global signs as us-east-1", S3EndpointParams.builder()
                                                                                .region(Region.of("aws-global"))
                                                                                .bucket("bucket!")
                                                                                .useFips(true)
                                                                                .accelerate(false)
                                                                                .useDualStack(true)
                                                                                .build());

        nonErrorCases.put("112: aws-global signs as us-east-1", S3EndpointParams.builder()
                                                                                .region(Region.of("aws-global"))
                                                                                .bucket("bucket")
                                                                                .useDualStack(false)
                                                                                .useFips(false)
                                                                                .accelerate(false)
                                                                                .endpoint("https://foo.com")
                                                                                .build());

        nonErrorCases.put("113: aws-global + dualstack + path-only bucket", S3EndpointParams.builder()
                                                                                            .region(Region.of("aws-global"))
                                                                                            .bucket("bucket!")
                                                                                            .useDualStack(true)
                                                                                            .useFips(false)
                                                                                            .accelerate(false)
                                                                                            .build());

        nonErrorCases.put("114: aws-global + path-only bucket", S3EndpointParams.builder()
                                                                                .region(Region.of("aws-global"))
                                                                                .bucket("bucket!")
                                                                                .build());

        nonErrorCases.put("116: aws-global, endpoint override & path only-bucket", S3EndpointParams.builder()
                                                                                                   .region(Region.of("aws-global"))
                                                                                                   .bucket("bucket!")
                                                                                                   .useDualStack(false)
                                                                                                   .useFips(false)
                                                                                                   .accelerate(false)
                                                                                                   .endpoint("http://foo.com")
                                                                                                   .build());

        nonErrorCases.put("118: accelerate, dualstack + aws-global", S3EndpointParams.builder()
                                                                                     .region(Region.of("aws-global"))
                                                                                     .bucket("bucket")
                                                                                     .useDualStack(true)
                                                                                     .useFips(false)
                                                                                     .accelerate(true)
                                                                                     .build());

        nonErrorCases.put("119: FIPS + aws-global + path only bucket. This is not supported by S3 but we allow garbage in garbage out", S3EndpointParams.builder()
                                                                                                                                                        .region(Region.of("aws-global"))
                                                                                                                                                        .bucket("bucket!")
                                                                                                                                                        .forcePathStyle(true)
                                                                                                                                                        .useDualStack(true)
                                                                                                                                                        .useFips(true)
                                                                                                                                                        .accelerate(false)
                                                                                                                                                        .build());

        nonErrorCases.put("122: ip address causes path style to be forced", S3EndpointParams.builder()
                                                                                            .region(Region.of("aws-global"))
                                                                                            .bucket("bucket")
                                                                                            .endpoint("http://192.168.1.1")
                                                                                            .build());

        nonErrorCases.put("124: FIPS + path-only (TODO: consider making this an error)", S3EndpointParams.builder()
                                                                                                         .region(Region.of("aws-global"))
                                                                                                         .bucket("bucket!")
                                                                                                         .useFips(true)
                                                                                                         .build());

        nonErrorCases.put("139: use global endpoint virtual addressing", S3EndpointParams.builder()
                                                                                         .region(Region.of("us-east-2"))
                                                                                         .bucket("bucket")
                                                                                         .endpoint("http://example.com")
                                                                                         .useGlobalEndpoint(true)
                                                                                         .build());

        nonErrorCases.put("140: global endpoint + ip address", S3EndpointParams.builder()
                                                                               .region(Region.of("us-east-2"))
                                                                               .bucket("bucket")
                                                                               .endpoint("http://192.168.0.1")
                                                                               .useGlobalEndpoint(true)
                                                                               .build());

        nonErrorCases.put("141: invalid outpost type", S3EndpointParams.builder()
                                                                       .region(Region.of("us-east-2"))
                                                                       .bucket("bucket!")
                                                                       .useGlobalEndpoint(true)
                                                                       .build());

        nonErrorCases.put("142: invalid outpost type", S3EndpointParams.builder()
                                                                       .region(Region.of("us-east-2"))
                                                                       .bucket("bucket")
                                                                       .accelerate(true)
                                                                       .useGlobalEndpoint(true)
                                                                       .build());

        nonErrorCases.put("143: use global endpoint + custom endpoint", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-2"))
                                                                                        .bucket("bucket!")
                                                                                        .useGlobalEndpoint(true)
                                                                                        .endpoint("http://foo.com")
                                                                                        .build());

        nonErrorCases.put("144: use global endpoint, not us-east-1, force path style", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-east-2"))
                                                                                                       .bucket("bucket!")
                                                                                                       .useGlobalEndpoint(true)
                                                                                                       .forcePathStyle(true)
                                                                                                       .endpoint("http://foo.com")
                                                                                                       .build());

        nonErrorCases.put("145: vanilla virtual addressing@us-west-2", S3EndpointParams.builder()
                                                                                       .accelerate(false)
                                                                                       .bucket("bucket-name")
                                                                                       .forcePathStyle(false)
                                                                                       .region(Region.of("us-west-2"))
                                                                                       .useDualStack(false)
                                                                                       .useFips(false)
                                                                                       .build());

        nonErrorCases.put("146: virtual addressing + dualstack@us-west-2", S3EndpointParams.builder()
                                                                                           .accelerate(false)
                                                                                           .bucket("bucket-name")
                                                                                           .forcePathStyle(false)
                                                                                           .region(Region.of("us-west-2"))
                                                                                           .useDualStack(true)
                                                                                           .useFips(false)
                                                                                           .build());

        nonErrorCases.put("147: accelerate + dualstack@us-west-2", S3EndpointParams.builder()
                                                                                   .accelerate(true)
                                                                                   .bucket("bucket-name")
                                                                                   .forcePathStyle(false)
                                                                                   .region(Region.of("us-west-2"))
                                                                                   .useDualStack(true)
                                                                                   .useFips(false)
                                                                                   .build());

        nonErrorCases.put("148: accelerate (dualstack=false)@us-west-2", S3EndpointParams.builder()
                                                                                         .accelerate(true)
                                                                                         .bucket("bucket-name")
                                                                                         .forcePathStyle(false)
                                                                                         .region(Region.of("us-west-2"))
                                                                                         .useDualStack(false)
                                                                                         .useFips(false)
                                                                                         .build());

        nonErrorCases.put("149: virtual addressing + fips@us-west-2", S3EndpointParams.builder()
                                                                                      .accelerate(false)
                                                                                      .bucket("bucket-name")
                                                                                      .forcePathStyle(false)
                                                                                      .region(Region.of("us-west-2"))
                                                                                      .useDualStack(false)
                                                                                      .useFips(true)
                                                                                      .build());

        nonErrorCases.put("150: virtual addressing + dualstack + fips@us-west-2", S3EndpointParams.builder()
                                                                                                  .accelerate(false)
                                                                                                  .bucket("bucket-name")
                                                                                                  .forcePathStyle(false)
                                                                                                  .region(Region.of("us-west-2"))
                                                                                                  .useDualStack(true)
                                                                                                  .useFips(true)
                                                                                                  .build());

        nonErrorCases.put("152: vanilla virtual addressing@cn-north-1", S3EndpointParams.builder()
                                                                                        .accelerate(false)
                                                                                        .bucket("bucket-name")
                                                                                        .forcePathStyle(false)
                                                                                        .region(Region.of("cn-north-1"))
                                                                                        .useDualStack(false)
                                                                                        .useFips(false)
                                                                                        .build());

        nonErrorCases.put("153: virtual addressing + dualstack@cn-north-1", S3EndpointParams.builder()
                                                                                            .accelerate(false)
                                                                                            .bucket("bucket-name")
                                                                                            .forcePathStyle(false)
                                                                                            .region(Region.of("cn-north-1"))
                                                                                            .useDualStack(true)
                                                                                            .useFips(false)
                                                                                            .build());

        nonErrorCases.put("156: vanilla virtual addressing@af-south-1", S3EndpointParams.builder()
                                                                                        .accelerate(false)
                                                                                        .bucket("bucket-name")
                                                                                        .forcePathStyle(false)
                                                                                        .region(Region.of("af-south-1"))
                                                                                        .useDualStack(false)
                                                                                        .useFips(false)
                                                                                        .build());

        nonErrorCases.put("157: virtual addressing + dualstack@af-south-1", S3EndpointParams.builder()
                                                                                            .accelerate(false)
                                                                                            .bucket("bucket-name")
                                                                                            .forcePathStyle(false)
                                                                                            .region(Region.of("af-south-1"))
                                                                                            .useDualStack(true)
                                                                                            .useFips(false)
                                                                                            .build());

        nonErrorCases.put("158: accelerate + dualstack@af-south-1", S3EndpointParams.builder()
                                                                                    .accelerate(true)
                                                                                    .bucket("bucket-name")
                                                                                    .forcePathStyle(false)
                                                                                    .region(Region.of("af-south-1"))
                                                                                    .useDualStack(true)
                                                                                    .useFips(false)
                                                                                    .build());

        nonErrorCases.put("159: accelerate (dualstack=false)@af-south-1", S3EndpointParams.builder()
                                                                                          .accelerate(true)
                                                                                          .bucket("bucket-name")
                                                                                          .forcePathStyle(false)
                                                                                          .region(Region.of("af-south-1"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        nonErrorCases.put("160: virtual addressing + fips@af-south-1", S3EndpointParams.builder()
                                                                                       .accelerate(false)
                                                                                       .bucket("bucket-name")
                                                                                       .forcePathStyle(false)
                                                                                       .region(Region.of("af-south-1"))
                                                                                       .useDualStack(false)
                                                                                       .useFips(true)
                                                                                       .build());

        nonErrorCases.put("161: virtual addressing + dualstack + fips@af-south-1", S3EndpointParams.builder()
                                                                                                   .accelerate(false)
                                                                                                   .bucket("bucket-name")
                                                                                                   .forcePathStyle(false)
                                                                                                   .region(Region.of("af-south-1"))
                                                                                                   .useDualStack(true)
                                                                                                   .useFips(true)
                                                                                                   .build());

        nonErrorCases.put("163: vanilla path style@us-west-2", S3EndpointParams.builder()
                                                                               .accelerate(false)
                                                                               .bucket("bucket-name")
                                                                               .forcePathStyle(true)
                                                                               .region(Region.of("us-west-2"))
                                                                               .useDualStack(false)
                                                                               .useFips(false)
                                                                               .build());

        nonErrorCases.put("164: fips@us-gov-west-2, bucket is not S3-dns-compatible (subdomains)", S3EndpointParams.builder()
                                                                                                                   .accelerate(false)
                                                                                                                   .bucket("bucket.with.dots")
                                                                                                                   .region(Region.of("us-gov-west-1"))
                                                                                                                   .useDualStack(false)
                                                                                                                   .useFips(true)
                                                                                                                   .build());

        nonErrorCases.put("166: path style + dualstack@us-west-2", S3EndpointParams.builder()
                                                                                   .accelerate(false)
                                                                                   .bucket("bucket-name")
                                                                                   .forcePathStyle(true)
                                                                                   .region(Region.of("us-west-2"))
                                                                                   .useDualStack(true)
                                                                                   .useFips(false)
                                                                                   .build());

        nonErrorCases.put("168: path style + invalid DNS name@us-west-2", S3EndpointParams.builder()
                                                                                          .accelerate(false)
                                                                                          .bucket("99a_b")
                                                                                          .forcePathStyle(true)
                                                                                          .region(Region.of("us-west-2"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        nonErrorCases.put("169: no path style + invalid DNS name@us-west-2", S3EndpointParams.builder()
                                                                                             .accelerate(false)
                                                                                             .bucket("99a_b")
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .useDualStack(false)
                                                                                             .useFips(false)
                                                                                             .build());

        nonErrorCases.put("170: vanilla path style@cn-north-1", S3EndpointParams.builder()
                                                                                .accelerate(false)
                                                                                .bucket("bucket-name")
                                                                                .forcePathStyle(true)
                                                                                .region(Region.of("cn-north-1"))
                                                                                .useDualStack(false)
                                                                                .useFips(false)
                                                                                .build());

        nonErrorCases.put("173: path style + dualstack@cn-north-1", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("bucket-name")
                                                                                    .forcePathStyle(true)
                                                                                    .region(Region.of("cn-north-1"))
                                                                                    .useDualStack(true)
                                                                                    .useFips(false)
                                                                                    .build());

        nonErrorCases.put("175: path style + invalid DNS name@cn-north-1", S3EndpointParams.builder()
                                                                                           .accelerate(false)
                                                                                           .bucket("99a_b")
                                                                                           .forcePathStyle(true)
                                                                                           .region(Region.of("cn-north-1"))
                                                                                           .useDualStack(false)
                                                                                           .useFips(false)
                                                                                           .build());

        nonErrorCases.put("176: no path style + invalid DNS name@cn-north-1", S3EndpointParams.builder()
                                                                                              .accelerate(false)
                                                                                              .bucket("99a_b")
                                                                                              .region(Region.of("cn-north-1"))
                                                                                              .useDualStack(false)
                                                                                              .useFips(false)
                                                                                              .build());

        nonErrorCases.put("177: vanilla path style@af-south-1", S3EndpointParams.builder()
                                                                                .accelerate(false)
                                                                                .bucket("bucket-name")
                                                                                .forcePathStyle(true)
                                                                                .region(Region.of("af-south-1"))
                                                                                .useDualStack(false)
                                                                                .useFips(false)
                                                                                .build());

        nonErrorCases.put("178: path style + fips@af-south-1", S3EndpointParams.builder()
                                                                               .accelerate(false)
                                                                               .bucket("bucket-name")
                                                                               .forcePathStyle(true)
                                                                               .region(Region.of("af-south-1"))
                                                                               .useDualStack(false)
                                                                               .useFips(true)
                                                                               .build());

        nonErrorCases.put("180: path style + dualstack@af-south-1", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("bucket-name")
                                                                                    .forcePathStyle(true)
                                                                                    .region(Region.of("af-south-1"))
                                                                                    .useDualStack(true)
                                                                                    .useFips(false)
                                                                                    .build());

        nonErrorCases.put("182: path style + invalid DNS name@af-south-1", S3EndpointParams.builder()
                                                                                           .accelerate(false)
                                                                                           .bucket("99a_b")
                                                                                           .forcePathStyle(true)
                                                                                           .region(Region.of("af-south-1"))
                                                                                           .useDualStack(false)
                                                                                           .useFips(false)
                                                                                           .build());

        nonErrorCases.put("183: no path style + invalid DNS name@af-south-1", S3EndpointParams.builder()
                                                                                              .accelerate(false)
                                                                                              .bucket("99a_b")
                                                                                              .region(Region.of("af-south-1"))
                                                                                              .useDualStack(false)
                                                                                              .useFips(false)
                                                                                              .build());

        nonErrorCases.put("184: virtual addressing + private link@us-west-2", S3EndpointParams.builder()
                                                                                              .accelerate(false)
                                                                                              .bucket("bucket-name")
                                                                                              .forcePathStyle(false)
                                                                                              .endpoint("http://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .useDualStack(false)
                                                                                              .useFips(false)
                                                                                              .build());

        nonErrorCases.put("185: path style + private link@us-west-2", S3EndpointParams.builder()
                                                                                      .accelerate(false)
                                                                                      .bucket("bucket-name")
                                                                                      .forcePathStyle(true)
                                                                                      .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                      .region(Region.of("us-west-2"))
                                                                                      .useDualStack(false)
                                                                                      .useFips(false)
                                                                                      .build());

        nonErrorCases.put("189: SDK::Host + access point ARN@us-west-2", S3EndpointParams.builder()
                                                                                         .accelerate(false)
                                                                                         .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                         .forcePathStyle(false)
                                                                                         .endpoint("https://beta.example.com")
                                                                                         .region(Region.of("us-west-2"))
                                                                                         .useDualStack(false)
                                                                                         .useFips(false)
                                                                                         .build());

        nonErrorCases.put("190: virtual addressing + private link@cn-north-1", S3EndpointParams.builder()
                                                                                               .accelerate(false)
                                                                                               .bucket("bucket-name")
                                                                                               .forcePathStyle(false)
                                                                                               .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                               .region(Region.of("cn-north-1"))
                                                                                               .useDualStack(false)
                                                                                               .useFips(false)
                                                                                               .build());

        nonErrorCases.put("191: path style + private link@cn-north-1", S3EndpointParams.builder()
                                                                                       .accelerate(false)
                                                                                       .bucket("bucket-name")
                                                                                       .forcePathStyle(true)
                                                                                       .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                       .region(Region.of("cn-north-1"))
                                                                                       .useDualStack(false)
                                                                                       .useFips(false)
                                                                                       .build());

        nonErrorCases.put("195: SDK::Host + access point ARN@cn-north-1", S3EndpointParams.builder()
                                                                                          .accelerate(false)
                                                                                          .bucket("arn:aws-cn:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                          .forcePathStyle(false)
                                                                                          .endpoint("https://beta.example.com")
                                                                                          .region(Region.of("cn-north-1"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        nonErrorCases.put("196: virtual addressing + private link@af-south-1", S3EndpointParams.builder()
                                                                                               .accelerate(false)
                                                                                               .bucket("bucket-name")
                                                                                               .forcePathStyle(false)
                                                                                               .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                               .region(Region.of("af-south-1"))
                                                                                               .useDualStack(false)
                                                                                               .useFips(false)
                                                                                               .build());

        nonErrorCases.put("197: path style + private link@af-south-1", S3EndpointParams.builder()
                                                                                       .accelerate(false)
                                                                                       .bucket("bucket-name")
                                                                                       .forcePathStyle(true)
                                                                                       .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                       .region(Region.of("af-south-1"))
                                                                                       .useDualStack(false)
                                                                                       .useFips(false)
                                                                                       .build());

        nonErrorCases.put("201: SDK::Host + access point ARN@af-south-1", S3EndpointParams.builder()
                                                                                          .accelerate(false)
                                                                                          .bucket("arn:aws:s3:af-south-1:123456789012:accesspoint:myendpoint")
                                                                                          .forcePathStyle(false)
                                                                                          .endpoint("https://beta.example.com")
                                                                                          .region(Region.of("af-south-1"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        nonErrorCases.put("202: vanilla access point arn@us-west-2", S3EndpointParams.builder()
                                                                                     .accelerate(false)
                                                                                     .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                     .forcePathStyle(false)
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .useDualStack(false)
                                                                                     .useFips(false)
                                                                                     .build());

        nonErrorCases.put("203: access point arn + FIPS@us-west-2", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                    .forcePathStyle(false)
                                                                                    .region(Region.of("us-west-2"))
                                                                                    .useDualStack(false)
                                                                                    .useFips(true)
                                                                                    .build());

        nonErrorCases.put("205: access point arn + FIPS + DualStack@us-west-2", S3EndpointParams.builder()
                                                                                                .accelerate(false)
                                                                                                .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                .forcePathStyle(false)
                                                                                                .region(Region.of("us-west-2"))
                                                                                                .useDualStack(true)
                                                                                                .useFips(true)
                                                                                                .build());

        nonErrorCases.put("206: vanilla access point arn@cn-north-1", S3EndpointParams.builder()
                                                                                      .accelerate(false)
                                                                                      .bucket("arn:aws-cn:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                      .forcePathStyle(false)
                                                                                      .region(Region.of("cn-north-1"))
                                                                                      .useDualStack(false)
                                                                                      .useFips(false)
                                                                                      .build());

        nonErrorCases.put("210: vanilla access point arn@af-south-1", S3EndpointParams.builder()
                                                                                      .accelerate(false)
                                                                                      .bucket("arn:aws:s3:af-south-1:123456789012:accesspoint:myendpoint")
                                                                                      .forcePathStyle(false)
                                                                                      .region(Region.of("af-south-1"))
                                                                                      .useDualStack(false)
                                                                                      .useFips(false)
                                                                                      .build());

        nonErrorCases.put("211: access point arn + FIPS@af-south-1", S3EndpointParams.builder()
                                                                                     .accelerate(false)
                                                                                     .bucket("arn:aws:s3:af-south-1:123456789012:accesspoint:myendpoint")
                                                                                     .forcePathStyle(false)
                                                                                     .region(Region.of("af-south-1"))
                                                                                     .useDualStack(false)
                                                                                     .useFips(true)
                                                                                     .build());

        nonErrorCases.put("213: access point arn + FIPS + DualStack@af-south-1", S3EndpointParams.builder()
                                                                                                 .accelerate(false)
                                                                                                 .bucket("arn:aws:s3:af-south-1:123456789012:accesspoint:myendpoint")
                                                                                                 .forcePathStyle(false)
                                                                                                 .region(Region.of("af-south-1"))
                                                                                                 .useDualStack(true)
                                                                                                 .useFips(true)
                                                                                                 .build());

        nonErrorCases.put("214: S3 outposts vanilla test", S3EndpointParams.builder()
                                                                           .region(Region.of("us-west-2"))
                                                                           .useFips(false)
                                                                           .useDualStack(false)
                                                                           .accelerate(false)
                                                                           .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                           .build());

        nonErrorCases.put("215: S3 outposts custom endpoint", S3EndpointParams.builder()
                                                                              .region(Region.of("us-west-2"))
                                                                              .useFips(false)
                                                                              .useDualStack(false)
                                                                              .accelerate(false)
                                                                              .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                              .endpoint("https://example.amazonaws.com")
                                                                              .build());

        nonErrorCases.put("218: outposts arn with region mismatch and UseArnRegion=true", S3EndpointParams.builder()
                                                                                                          .accelerate(false)
                                                                                                          .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                                                                                          .forcePathStyle(false)
                                                                                                          .useArnRegion(true)
                                                                                                          .region(Region.of("us-west-2"))
                                                                                                          .useDualStack(false)
                                                                                                          .useFips(false)
                                                                                                          .build());

        nonErrorCases.put("219: outposts arn with region mismatch and UseArnRegion unset", S3EndpointParams.builder()
                                                                                                           .accelerate(false)
                                                                                                           .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                                                                                           .forcePathStyle(false)
                                                                                                           .region(Region.of("us-west-2"))
                                                                                                           .useDualStack(false)
                                                                                                           .useFips(false)
                                                                                                           .build());

        nonErrorCases.put("221: ARN with UseGlobalEndpoint and use-east-1 region uses the regional endpoint", S3EndpointParams.builder()
                                                                                                                              .region(Region.of("us-east-1"))
                                                                                                                              .useGlobalEndpoint(true)
                                                                                                                              .useFips(false)
                                                                                                                              .useDualStack(false)
                                                                                                                              .accelerate(false)
                                                                                                                              .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                                                                              .build());

        nonErrorCases.put("226: object lambda @us-east-1", S3EndpointParams.builder()
                                                                           .region(Region.of("us-east-1"))
                                                                           .useFips(false)
                                                                           .useDualStack(false)
                                                                           .accelerate(false)
                                                                           .useArnRegion(false)
                                                                           .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                           .build());

        nonErrorCases.put("227: object lambda @us-west-2", S3EndpointParams.builder()
                                                                           .region(Region.of("us-west-2"))
                                                                           .useFips(false)
                                                                           .useDualStack(false)
                                                                           .accelerate(false)
                                                                           .useArnRegion(false)
                                                                           .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/mybanner")
                                                                           .build());

        nonErrorCases.put("228: object lambda, colon resource deliminator @us-west-2", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(false)
                                                                                                       .accelerate(false)
                                                                                                       .useArnRegion(false)
                                                                                                       .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:mybanner")
                                                                                                       .build());

        nonErrorCases.put("229: object lambda @us-east-1, client region us-west-2, useArnRegion=true", S3EndpointParams.builder()
                                                                                                                       .region(Region.of("us-west-2"))
                                                                                                                       .useFips(false)
                                                                                                                       .useDualStack(false)
                                                                                                                       .accelerate(false)
                                                                                                                       .useArnRegion(true)
                                                                                                                       .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                                                                       .build());

        nonErrorCases.put("230: object lambda @us-east-1, client region s3-external-1, useArnRegion=true", S3EndpointParams.builder()
                                                                                                                           .region(Region.of("s3-external-1"))
                                                                                                                           .useFips(false)
                                                                                                                           .useDualStack(false)
                                                                                                                           .accelerate(false)
                                                                                                                           .useArnRegion(true)
                                                                                                                           .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                                                                           .build());

        nonErrorCases.put("232: object lambda @us-east-1, client region aws-global, useArnRegion=true", S3EndpointParams.builder()
                                                                                                                        .region(Region.of("aws-global"))
                                                                                                                        .useFips(false)
                                                                                                                        .useDualStack(false)
                                                                                                                        .accelerate(false)
                                                                                                                        .useArnRegion(true)
                                                                                                                        .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                                                                        .build());

        nonErrorCases.put("236: object lambda @us-gov-east-1", S3EndpointParams.builder()
                                                                               .region(Region.of("us-gov-east-1"))
                                                                               .useFips(false)
                                                                               .useDualStack(false)
                                                                               .accelerate(false)
                                                                               .useArnRegion(false)
                                                                               .bucket("arn:aws-us-gov:s3-object-lambda:us-gov-east-1:123456789012:accesspoint/mybanner")
                                                                               .build());

        nonErrorCases.put("237: object lambda @us-gov-east-1, with fips", S3EndpointParams.builder()
                                                                                          .region(Region.of("us-gov-east-1"))
                                                                                          .useFips(true)
                                                                                          .useDualStack(false)
                                                                                          .accelerate(false)
                                                                                          .useArnRegion(false)
                                                                                          .bucket("arn:aws-us-gov:s3-object-lambda:us-gov-east-1:123456789012:accesspoint/mybanner")
                                                                                          .build());

        nonErrorCases.put("249: object lambda with custom endpoint", S3EndpointParams.builder()
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .useFips(false)
                                                                                     .useDualStack(false)
                                                                                     .accelerate(false)
                                                                                     .useArnRegion(false)
                                                                                     .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/mybanner")
                                                                                     .endpoint("https://my-endpoint.com")
                                                                                     .build());

        nonErrorCases.put("251: WriteGetObjectResponse @ us-west-2", S3EndpointParams.builder()
                                                                                     .accelerate(false)
                                                                                     .useObjectLambdaEndpoint(true)
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .useDualStack(false)
                                                                                     .useFips(false)
                                                                                     .build());

        nonErrorCases.put("252: WriteGetObjectResponse with custom endpoint", S3EndpointParams.builder()
                                                                                              .accelerate(false)
                                                                                              .useObjectLambdaEndpoint(true)
                                                                                              .endpoint("https://my-endpoint.com")
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .useDualStack(false)
                                                                                              .useFips(false)
                                                                                              .build());

        nonErrorCases.put("253: WriteGetObjectResponse @ us-east-1", S3EndpointParams.builder()
                                                                                     .accelerate(false)
                                                                                     .useObjectLambdaEndpoint(true)
                                                                                     .region(Region.of("us-east-1"))
                                                                                     .useDualStack(false)
                                                                                     .useFips(false)
                                                                                     .build());

        nonErrorCases.put("254: WriteGetObjectResponse with fips", S3EndpointParams.builder()
                                                                                   .accelerate(false)
                                                                                   .useObjectLambdaEndpoint(true)
                                                                                   .region(Region.of("us-east-1"))
                                                                                   .useDualStack(false)
                                                                                   .useFips(true)
                                                                                   .build());

        nonErrorCases.put("259: WriteGetObjectResponse with an unknown partition", S3EndpointParams.builder()
                                                                                                   .accelerate(false)
                                                                                                   .useObjectLambdaEndpoint(true)
                                                                                                   .region(Region.of("us-east.special"))
                                                                                                   .useDualStack(false)
                                                                                                   .useFips(false)
                                                                                                   .build());

        nonErrorCases.put("260: S3 Outposts bucketAlias Real Outpost Prod us-west-1", S3EndpointParams.builder()
                                                                                                      .region(Region.of("us-west-1"))
                                                                                                      .bucket("test-accessp-o0b1d075431d83bebde8xz5w8ijx1qzlbp3i3kuse10--op-s3")
                                                                                                      .useFips(false)
                                                                                                      .useDualStack(false)
                                                                                                      .accelerate(false)
                                                                                                      .build());

        nonErrorCases.put("261: S3 Outposts bucketAlias Real Outpost Prod ap-east-1", S3EndpointParams.builder()
                                                                                                      .region(Region.of("ap-east-1"))
                                                                                                      .bucket("test-accessp-o0b1d075431d83bebde8xz5w8ijx1qzlbp3i3kuse10--op-s3")
                                                                                                      .useFips(false)
                                                                                                      .useDualStack(false)
                                                                                                      .accelerate(false)
                                                                                                      .build());

        nonErrorCases.put("262: S3 Outposts bucketAlias Ec2 Outpost Prod us-east-1", S3EndpointParams.builder()
                                                                                                     .region(Region.of("us-east-1"))
                                                                                                     .bucket("test-accessp-e0000075431d83bebde8xz5w8ijx1qzlbp3i3kuse10--op-s3")
                                                                                                     .useFips(false)
                                                                                                     .useDualStack(false)
                                                                                                     .accelerate(false)
                                                                                                     .build());

        nonErrorCases.put("263: S3 Outposts bucketAlias Ec2 Outpost Prod me-south-1", S3EndpointParams.builder()
                                                                                                      .region(Region.of("me-south-1"))
                                                                                                      .bucket("test-accessp-e0000075431d83bebde8xz5w8ijx1qzlbp3i3kuse10--op-s3")
                                                                                                      .useFips(false)
                                                                                                      .useDualStack(false)
                                                                                                      .accelerate(false)
                                                                                                      .build());

        nonErrorCases.put("264: S3 Outposts bucketAlias Real Outpost Beta", S3EndpointParams.builder()
                                                                                            .region(Region.of("us-east-1"))
                                                                                            .bucket("test-accessp-o0b1d075431d83bebde8xz5w8ijx1qzlbp3i3kbeta0--op-s3")
                                                                                            .endpoint("https://example.amazonaws.com")
                                                                                            .useFips(false)
                                                                                            .useDualStack(false)
                                                                                            .accelerate(false)
                                                                                            .build());

        nonErrorCases.put("265: S3 Outposts bucketAlias Ec2 Outpost Beta", S3EndpointParams.builder()
                                                                                           .region(Region.of("us-east-1"))
                                                                                           .bucket("161743052723-e00000136899934034jeahy1t8gpzpbwjj8kb7beta0--op-s3")
                                                                                           .endpoint("https://example.amazonaws.com")
                                                                                           .useFips(false)
                                                                                           .useDualStack(false)
                                                                                           .accelerate(false)
                                                                                           .build());

        nonErrorCases.put("270: S3 Snow with bucket", S3EndpointParams.builder()
                                                                      .region(Region.of("snow"))
                                                                      .bucket("bucketName")
                                                                      .endpoint("http://10.0.1.12:433")
                                                                      .useFips(false)
                                                                      .useDualStack(false)
                                                                      .accelerate(false)
                                                                      .build());

        nonErrorCases.put("271: S3 Snow without bucket", S3EndpointParams.builder()
                                                                         .region(Region.of("snow"))
                                                                         .endpoint("https://10.0.1.12:433")
                                                                         .useFips(false)
                                                                         .useDualStack(false)
                                                                         .accelerate(false)
                                                                         .build());

        nonErrorCases.put("272: S3 Snow no port", S3EndpointParams.builder()
                                                                  .region(Region.of("snow"))
                                                                  .bucket("bucketName")
                                                                  .endpoint("http://10.0.1.12")
                                                                  .useFips(false)
                                                                  .useDualStack(false)
                                                                  .accelerate(false)
                                                                  .build());

        nonErrorCases.put("273: S3 Snow dns endpoint", S3EndpointParams.builder()
                                                                       .region(Region.of("snow"))
                                                                       .bucket("bucketName")
                                                                       .endpoint("https://amazonaws.com")
                                                                       .useFips(false)
                                                                       .useDualStack(false)
                                                                       .accelerate(false)
                                                                       .build());

        nonErrorCases.put("274: Data Plane with short zone name", S3EndpointParams.builder()
                                                                                  .region(Region.of("us-east-1"))
                                                                                  .bucket("mybucket--abcd-ab1--x-s3")
                                                                                  .useFips(false)
                                                                                  .useDualStack(false)
                                                                                  .accelerate(false)
                                                                                  .useS3ExpressControlEndpoint(false)
                                                                                  .build());

        nonErrorCases.put("275: Data Plane with short zone name china region", S3EndpointParams.builder()
                                                                                               .region(Region.of("cn-north-1"))
                                                                                               .bucket("mybucket--abcd-ab1--x-s3")
                                                                                               .useFips(false)
                                                                                               .useDualStack(false)
                                                                                               .accelerate(false)
                                                                                               .useS3ExpressControlEndpoint(false)
                                                                                               .build());

        nonErrorCases.put("276: Data Plane with short zone name with AP", S3EndpointParams.builder()
                                                                                          .region(Region.of("us-east-1"))
                                                                                          .bucket("myaccesspoint--abcd-ab1--xa-s3")
                                                                                          .useFips(false)
                                                                                          .useDualStack(false)
                                                                                          .accelerate(false)
                                                                                          .useS3ExpressControlEndpoint(false)
                                                                                          .build());

        nonErrorCases.put("277: Data Plane with short zone name with AP china region", S3EndpointParams.builder()
                                                                                                       .region(Region.of("cn-north-1"))
                                                                                                       .bucket("myaccesspoint--abcd-ab1--xa-s3")
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(false)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("278: Data Plane with short zone names (13 chars)", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                              .useFips(false)
                                                                                              .useDualStack(false)
                                                                                              .accelerate(false)
                                                                                              .useS3ExpressControlEndpoint(false)
                                                                                              .build());

        nonErrorCases.put("279: Data Plane with short zone names (13 chars) with AP", S3EndpointParams.builder()
                                                                                                      .region(Region.of("us-west-2"))
                                                                                                      .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                      .useFips(false)
                                                                                                      .useDualStack(false)
                                                                                                      .accelerate(false)
                                                                                                      .useS3ExpressControlEndpoint(false)
                                                                                                      .build());

        nonErrorCases.put("280: Data Plane with medium zone names (14 chars)", S3EndpointParams.builder()
                                                                                               .region(Region.of("us-west-2"))
                                                                                               .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                               .useFips(false)
                                                                                               .useDualStack(false)
                                                                                               .accelerate(false)
                                                                                               .useS3ExpressControlEndpoint(false)
                                                                                               .build());

        nonErrorCases.put("281: Data Plane with medium zone names (14 chars) with AP", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(false)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("282: Data Plane with long zone names (20 chars)", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                             .useFips(false)
                                                                                             .useDualStack(false)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(false)
                                                                                             .build());

        nonErrorCases.put("283: Data Plane with long zone names (20 chars)", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                             .useFips(false)
                                                                                             .useDualStack(false)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(false)
                                                                                             .build());

        nonErrorCases.put("284: Data Plane with short zone fips", S3EndpointParams.builder()
                                                                                  .region(Region.of("us-east-1"))
                                                                                  .bucket("mybucket--test-ab1--x-s3")
                                                                                  .useFips(true)
                                                                                  .useDualStack(false)
                                                                                  .accelerate(false)
                                                                                  .useS3ExpressControlEndpoint(false)
                                                                                  .build());

        nonErrorCases.put("286: Data Plane with short zone fips with AP", S3EndpointParams.builder()
                                                                                          .region(Region.of("us-east-1"))
                                                                                          .bucket("myaccesspoint--test-ab1--xa-s3")
                                                                                          .useFips(true)
                                                                                          .useDualStack(false)
                                                                                          .accelerate(false)
                                                                                          .useS3ExpressControlEndpoint(false)
                                                                                          .build());

        nonErrorCases.put("288: Data Plane with short zone (13 chars) fips", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                             .useFips(true)
                                                                                             .useDualStack(false)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(false)
                                                                                             .build());

        nonErrorCases.put("289: Data Plane with short zone (13 chars) fips with AP", S3EndpointParams.builder()
                                                                                                     .region(Region.of("us-west-2"))
                                                                                                     .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                     .useFips(true)
                                                                                                     .useDualStack(false)
                                                                                                     .accelerate(false)
                                                                                                     .useS3ExpressControlEndpoint(false)
                                                                                                     .build());

        nonErrorCases.put("290: Data Plane with medium zone (14 chars) fips", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                              .useFips(true)
                                                                                              .useDualStack(false)
                                                                                              .accelerate(false)
                                                                                              .useS3ExpressControlEndpoint(false)
                                                                                              .build());

        nonErrorCases.put("291: Data Plane with medium zone (14 chars) fips with AP", S3EndpointParams.builder()
                                                                                                      .region(Region.of("us-west-2"))
                                                                                                      .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                      .useFips(true)
                                                                                                      .useDualStack(false)
                                                                                                      .accelerate(false)
                                                                                                      .useS3ExpressControlEndpoint(false)
                                                                                                      .build());

        nonErrorCases.put("292: Data Plane with long zone (20 chars) fips", S3EndpointParams.builder()
                                                                                            .region(Region.of("us-west-2"))
                                                                                            .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                            .useFips(true)
                                                                                            .useDualStack(false)
                                                                                            .accelerate(false)
                                                                                            .useS3ExpressControlEndpoint(false)
                                                                                            .build());

        nonErrorCases.put("293: Data Plane with long zone (20 chars) fips with AP", S3EndpointParams.builder()
                                                                                                    .region(Region.of("us-west-2"))
                                                                                                    .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                                    .useFips(true)
                                                                                                    .useDualStack(false)
                                                                                                    .accelerate(false)
                                                                                                    .useS3ExpressControlEndpoint(false)
                                                                                                    .build());

        nonErrorCases.put("294: Data Plane with long AZ", S3EndpointParams.builder()
                                                                          .region(Region.of("us-west-2"))
                                                                          .bucket("mybucket--test1-az1--x-s3")
                                                                          .useFips(false)
                                                                          .useDualStack(false)
                                                                          .accelerate(false)
                                                                          .useS3ExpressControlEndpoint(false)
                                                                          .build());

        nonErrorCases.put("295: Data Plane with long AZ with AP", S3EndpointParams.builder()
                                                                                  .region(Region.of("us-west-2"))
                                                                                  .bucket("myaccesspoint--test1-az1--xa-s3")
                                                                                  .useFips(false)
                                                                                  .useDualStack(false)
                                                                                  .accelerate(false)
                                                                                  .useS3ExpressControlEndpoint(false)
                                                                                  .build());

        nonErrorCases.put("296: Data Plane with long AZ fips", S3EndpointParams.builder()
                                                                               .region(Region.of("us-west-2"))
                                                                               .bucket("mybucket--test1-az1--x-s3")
                                                                               .useFips(true)
                                                                               .useDualStack(false)
                                                                               .accelerate(false)
                                                                               .useS3ExpressControlEndpoint(false)
                                                                               .build());

        nonErrorCases.put("297: Data Plane with long AZ fips with AP", S3EndpointParams.builder()
                                                                                       .region(Region.of("us-west-2"))
                                                                                       .bucket("myaccesspoint--test1-az1--xa-s3")
                                                                                       .useFips(true)
                                                                                       .useDualStack(false)
                                                                                       .accelerate(false)
                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                       .build());

        nonErrorCases.put("298: Control plane with short AZ bucket", S3EndpointParams.builder()
                                                                                     .region(Region.of("us-east-1"))
                                                                                     .bucket("mybucket--test-ab1--x-s3")
                                                                                     .useFips(false)
                                                                                     .useDualStack(false)
                                                                                     .accelerate(false)
                                                                                     .useS3ExpressControlEndpoint(true)
                                                                                     .disableS3ExpressSessionAuth(false)
                                                                                     .build());

        nonErrorCases.put("299: Control plane with short AZ bucket china region", S3EndpointParams.builder()
                                                                                                  .region(Region.of("cn-north-1"))
                                                                                                  .bucket("mybucket--test-ab1--x-s3")
                                                                                                  .useFips(false)
                                                                                                  .useDualStack(false)
                                                                                                  .accelerate(false)
                                                                                                  .useS3ExpressControlEndpoint(true)
                                                                                                  .disableS3ExpressSessionAuth(false)
                                                                                                  .build());

        nonErrorCases.put("300: Control plane with short AZ bucket and fips", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-east-1"))
                                                                                              .bucket("mybucket--test-ab1--x-s3")
                                                                                              .useFips(true)
                                                                                              .useDualStack(false)
                                                                                              .accelerate(false)
                                                                                              .useS3ExpressControlEndpoint(true)
                                                                                              .disableS3ExpressSessionAuth(false)
                                                                                              .build());

        nonErrorCases.put("302: Control plane without bucket", S3EndpointParams.builder()
                                                                               .region(Region.of("us-east-1"))
                                                                               .useFips(false)
                                                                               .useDualStack(false)
                                                                               .accelerate(false)
                                                                               .useS3ExpressControlEndpoint(true)
                                                                               .disableS3ExpressSessionAuth(false)
                                                                               .build());

        nonErrorCases.put("303: Control plane without bucket and fips", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .useFips(true)
                                                                                        .useDualStack(false)
                                                                                        .accelerate(false)
                                                                                        .useS3ExpressControlEndpoint(true)
                                                                                        .disableS3ExpressSessionAuth(false)
                                                                                        .build());

        nonErrorCases.put("304: Data Plane sigv4 auth with short AZ", S3EndpointParams.builder()
                                                                                      .region(Region.of("us-west-2"))
                                                                                      .bucket("mybucket--usw2-az1--x-s3")
                                                                                      .useFips(false)
                                                                                      .useDualStack(false)
                                                                                      .accelerate(false)
                                                                                      .disableS3ExpressSessionAuth(true)
                                                                                      .build());

        nonErrorCases.put("305: Data Plane sigv4 auth with short AZ with AP", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                              .useFips(false)
                                                                                              .useDualStack(false)
                                                                                              .accelerate(false)
                                                                                              .disableS3ExpressSessionAuth(true)
                                                                                              .build());

        nonErrorCases.put("306: Data Plane sigv4 auth with short zone (13 chars)", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-west-2"))
                                                                                                   .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                                   .useFips(false)
                                                                                                   .useDualStack(false)
                                                                                                   .accelerate(false)
                                                                                                   .disableS3ExpressSessionAuth(true)
                                                                                                   .build());

        nonErrorCases.put("307: Data Plane sigv4 auth with short zone (13 chars) with AP", S3EndpointParams.builder()
                                                                                                           .region(Region.of("us-west-2"))
                                                                                                           .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                           .useFips(false)
                                                                                                           .useDualStack(false)
                                                                                                           .accelerate(false)
                                                                                                           .disableS3ExpressSessionAuth(true)
                                                                                                           .build());

        nonErrorCases.put("308: Data Plane sigv4 auth with short AZ fips", S3EndpointParams.builder()
                                                                                           .region(Region.of("us-west-2"))
                                                                                           .bucket("mybucket--usw2-az1--x-s3")
                                                                                           .useFips(true)
                                                                                           .useDualStack(false)
                                                                                           .accelerate(false)
                                                                                           .disableS3ExpressSessionAuth(true)
                                                                                           .build());

        nonErrorCases.put("309: Data Plane sigv4 auth with short AZ fips with AP", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-west-2"))
                                                                                                   .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                                   .useFips(true)
                                                                                                   .useDualStack(false)
                                                                                                   .accelerate(false)
                                                                                                   .disableS3ExpressSessionAuth(true)
                                                                                                   .build());

        nonErrorCases.put("310: Data Plane sigv4 auth with short zone (13 chars) fips", S3EndpointParams.builder()
                                                                                                        .region(Region.of("us-west-2"))
                                                                                                        .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                                        .useFips(true)
                                                                                                        .useDualStack(false)
                                                                                                        .accelerate(false)
                                                                                                        .disableS3ExpressSessionAuth(true)
                                                                                                        .build());

        nonErrorCases.put("311: Data Plane sigv4 auth with short zone (13 chars) fips with AP", S3EndpointParams.builder()
                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                                .useFips(true)
                                                                                                                .useDualStack(false)
                                                                                                                .accelerate(false)
                                                                                                                .disableS3ExpressSessionAuth(true)
                                                                                                                .build());

        nonErrorCases.put("312: Data Plane sigv4 auth with long AZ", S3EndpointParams.builder()
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .bucket("mybucket--test1-az1--x-s3")
                                                                                     .useFips(false)
                                                                                     .useDualStack(false)
                                                                                     .accelerate(false)
                                                                                     .useS3ExpressControlEndpoint(false)
                                                                                     .disableS3ExpressSessionAuth(true)
                                                                                     .build());

        nonErrorCases.put("313: Data Plane sigv4 auth with long AZ with AP", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .bucket("myaccesspoint--test1-az1--xa-s3")
                                                                                             .useFips(false)
                                                                                             .useDualStack(false)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(false)
                                                                                             .disableS3ExpressSessionAuth(true)
                                                                                             .build());

        nonErrorCases.put("314: Data Plane sigv4 auth with medium zone(14 chars)", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-west-2"))
                                                                                                   .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                                   .useFips(false)
                                                                                                   .useDualStack(false)
                                                                                                   .accelerate(false)
                                                                                                   .useS3ExpressControlEndpoint(false)
                                                                                                   .disableS3ExpressSessionAuth(true)
                                                                                                   .build());

        nonErrorCases.put("315: Data Plane sigv4 auth with medium zone(14 chars) with AP", S3EndpointParams.builder()
                                                                                                           .region(Region.of("us-west-2"))
                                                                                                           .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                           .useFips(false)
                                                                                                           .useDualStack(false)
                                                                                                           .accelerate(false)
                                                                                                           .useS3ExpressControlEndpoint(false)
                                                                                                           .disableS3ExpressSessionAuth(true)
                                                                                                           .build());

        nonErrorCases.put("316: Data Plane sigv4 auth with long zone(20 chars)", S3EndpointParams.builder()
                                                                                                 .region(Region.of("us-west-2"))
                                                                                                 .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                                 .useFips(false)
                                                                                                 .useDualStack(false)
                                                                                                 .accelerate(false)
                                                                                                 .useS3ExpressControlEndpoint(false)
                                                                                                 .disableS3ExpressSessionAuth(true)
                                                                                                 .build());

        nonErrorCases.put("317: Data Plane sigv4 auth with long zone(20 chars) with AP", S3EndpointParams.builder()
                                                                                                         .region(Region.of("us-west-2"))
                                                                                                         .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                                         .useFips(false)
                                                                                                         .useDualStack(false)
                                                                                                         .accelerate(false)
                                                                                                         .useS3ExpressControlEndpoint(false)
                                                                                                         .disableS3ExpressSessionAuth(true)
                                                                                                         .build());

        nonErrorCases.put("318: Data Plane sigv4 auth with long AZ fips", S3EndpointParams.builder()
                                                                                          .region(Region.of("us-west-2"))
                                                                                          .bucket("mybucket--test1-az1--x-s3")
                                                                                          .useFips(true)
                                                                                          .useDualStack(false)
                                                                                          .accelerate(false)
                                                                                          .useS3ExpressControlEndpoint(false)
                                                                                          .disableS3ExpressSessionAuth(true)
                                                                                          .build());

        nonErrorCases.put("319: Data Plane sigv4 auth with long AZ fips with AP", S3EndpointParams.builder()
                                                                                                  .region(Region.of("us-west-2"))
                                                                                                  .bucket("myaccesspoint--test1-az1--xa-s3")
                                                                                                  .useFips(true)
                                                                                                  .useDualStack(false)
                                                                                                  .accelerate(false)
                                                                                                  .useS3ExpressControlEndpoint(false)
                                                                                                  .disableS3ExpressSessionAuth(true)
                                                                                                  .build());

        nonErrorCases.put("320: Data Plane sigv4 auth with medium zone (14 chars) fips", S3EndpointParams.builder()
                                                                                                         .region(Region.of("us-west-2"))
                                                                                                         .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                                         .useFips(true)
                                                                                                         .useDualStack(false)
                                                                                                         .accelerate(false)
                                                                                                         .useS3ExpressControlEndpoint(false)
                                                                                                         .disableS3ExpressSessionAuth(true)
                                                                                                         .build());

        nonErrorCases.put("321: Data Plane sigv4 auth with medium zone (14 chars) fips with AP", S3EndpointParams.builder()
                                                                                                                 .region(Region.of("us-west-2"))
                                                                                                                 .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                                 .useFips(true)
                                                                                                                 .useDualStack(false)
                                                                                                                 .accelerate(false)
                                                                                                                 .useS3ExpressControlEndpoint(false)
                                                                                                                 .disableS3ExpressSessionAuth(true)
                                                                                                                 .build());

        nonErrorCases.put("322: Data Plane sigv4 auth with long zone (20 chars) fips", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                                       .useFips(true)
                                                                                                       .useDualStack(false)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .disableS3ExpressSessionAuth(true)
                                                                                                       .build());

        nonErrorCases.put("323: Data Plane sigv4 auth with long zone (20 chars) fips with AP", S3EndpointParams.builder()
                                                                                                               .region(Region.of("us-west-2"))
                                                                                                               .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                                               .useFips(true)
                                                                                                               .useDualStack(false)
                                                                                                               .accelerate(false)
                                                                                                               .useS3ExpressControlEndpoint(false)
                                                                                                               .disableS3ExpressSessionAuth(true)
                                                                                                               .build());

        nonErrorCases.put("324: Control Plane host override", S3EndpointParams.builder()
                                                                              .region(Region.of("us-west-2"))
                                                                              .bucket("mybucket--usw2-az1--x-s3")
                                                                              .useFips(false)
                                                                              .useDualStack(false)
                                                                              .accelerate(false)
                                                                              .useS3ExpressControlEndpoint(true)
                                                                              .disableS3ExpressSessionAuth(true)
                                                                              .endpoint("https://custom.com")
                                                                              .build());

        nonErrorCases.put("325: Control Plane host override with AP", S3EndpointParams.builder()
                                                                                      .region(Region.of("us-west-2"))
                                                                                      .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                      .useFips(false)
                                                                                      .useDualStack(false)
                                                                                      .accelerate(false)
                                                                                      .useS3ExpressControlEndpoint(true)
                                                                                      .disableS3ExpressSessionAuth(true)
                                                                                      .endpoint("https://custom.com")
                                                                                      .build());

        nonErrorCases.put("326: Control Plane host override no bucket", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-west-2"))
                                                                                        .useFips(false)
                                                                                        .useDualStack(false)
                                                                                        .accelerate(false)
                                                                                        .useS3ExpressControlEndpoint(true)
                                                                                        .disableS3ExpressSessionAuth(true)
                                                                                        .endpoint("https://custom.com")
                                                                                        .build());

        nonErrorCases.put("327: Data plane host override non virtual session auth", S3EndpointParams.builder()
                                                                                                    .region(Region.of("us-west-2"))
                                                                                                    .bucket("mybucket--usw2-az1--x-s3")
                                                                                                    .useFips(false)
                                                                                                    .useDualStack(false)
                                                                                                    .accelerate(false)
                                                                                                    .endpoint("https://10.0.0.1")
                                                                                                    .build());

        nonErrorCases.put("328: Data plane host override non virtual session auth with AP", S3EndpointParams.builder()
                                                                                                            .region(Region.of("us-west-2"))
                                                                                                            .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                                            .useFips(false)
                                                                                                            .useDualStack(false)
                                                                                                            .accelerate(false)
                                                                                                            .endpoint("https://10.0.0.1")
                                                                                                            .build());

        nonErrorCases.put("329: Control Plane host override ip", S3EndpointParams.builder()
                                                                                 .region(Region.of("us-west-2"))
                                                                                 .bucket("mybucket--usw2-az1--x-s3")
                                                                                 .useFips(false)
                                                                                 .useDualStack(false)
                                                                                 .accelerate(false)
                                                                                 .useS3ExpressControlEndpoint(true)
                                                                                 .disableS3ExpressSessionAuth(true)
                                                                                 .endpoint("https://10.0.0.1")
                                                                                 .build());

        nonErrorCases.put("330: Control Plane host override ip with AP", S3EndpointParams.builder()
                                                                                         .region(Region.of("us-west-2"))
                                                                                         .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                         .useFips(false)
                                                                                         .useDualStack(false)
                                                                                         .accelerate(false)
                                                                                         .useS3ExpressControlEndpoint(true)
                                                                                         .disableS3ExpressSessionAuth(true)
                                                                                         .endpoint("https://10.0.0.1")
                                                                                         .build());

        nonErrorCases.put("331: Data plane host override", S3EndpointParams.builder()
                                                                           .region(Region.of("us-west-2"))
                                                                           .bucket("mybucket--usw2-az1--x-s3")
                                                                           .useFips(false)
                                                                           .useDualStack(false)
                                                                           .accelerate(false)
                                                                           .endpoint("https://custom.com")
                                                                           .build());

        nonErrorCases.put("332: Data plane host override with AP", S3EndpointParams.builder()
                                                                                   .region(Region.of("us-west-2"))
                                                                                   .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                   .useFips(false)
                                                                                   .useDualStack(false)
                                                                                   .accelerate(false)
                                                                                   .endpoint("https://custom.com")
                                                                                   .build());

        nonErrorCases.put("345: Control plane without bucket and dualstack", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-east-1"))
                                                                                             .useFips(false)
                                                                                             .useDualStack(true)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(true)
                                                                                             .disableS3ExpressSessionAuth(false)
                                                                                             .build());

        nonErrorCases.put("346: Control plane without bucket, fips and dualstack", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-east-1"))
                                                                                                   .useFips(true)
                                                                                                   .useDualStack(true)
                                                                                                   .accelerate(false)
                                                                                                   .useS3ExpressControlEndpoint(true)
                                                                                                   .disableS3ExpressSessionAuth(false)
                                                                                                   .build());

        nonErrorCases.put("347: Data Plane with short AZ and dualstack", S3EndpointParams.builder()
                                                                                         .region(Region.of("us-west-2"))
                                                                                         .bucket("mybucket--usw2-az1--x-s3")
                                                                                         .useFips(false)
                                                                                         .useDualStack(true)
                                                                                         .accelerate(false)
                                                                                         .useS3ExpressControlEndpoint(false)
                                                                                         .build());

        nonErrorCases.put("348: Data Plane with short AZ and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-west-2"))
                                                                                                   .bucket("mybucket--usw2-az1--x-s3")
                                                                                                   .useFips(true)
                                                                                                   .useDualStack(true)
                                                                                                   .accelerate(false)
                                                                                                   .useS3ExpressControlEndpoint(false)
                                                                                                   .build());

        nonErrorCases.put("349: Data Plane sigv4 auth with short AZ and dualstack", S3EndpointParams.builder()
                                                                                                    .region(Region.of("us-west-2"))
                                                                                                    .bucket("mybucket--usw2-az1--x-s3")
                                                                                                    .useFips(false)
                                                                                                    .useDualStack(true)
                                                                                                    .accelerate(false)
                                                                                                    .disableS3ExpressSessionAuth(true)
                                                                                                    .build());

        nonErrorCases.put("350: Data Plane sigv4 auth with short AZ and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                              .region(Region.of("us-west-2"))
                                                                                                              .bucket("mybucket--usw2-az1--x-s3")
                                                                                                              .useFips(true)
                                                                                                              .useDualStack(true)
                                                                                                              .accelerate(false)
                                                                                                              .disableS3ExpressSessionAuth(true)
                                                                                                              .build());

        nonErrorCases.put("351: Data Plane with zone and dualstack", S3EndpointParams.builder()
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .bucket("mybucket--usw2-az12--x-s3")
                                                                                     .useFips(false)
                                                                                     .useDualStack(true)
                                                                                     .accelerate(false)
                                                                                     .useS3ExpressControlEndpoint(false)
                                                                                     .build());

        nonErrorCases.put("352: Data Plane with zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                               .region(Region.of("us-west-2"))
                                                                                               .bucket("mybucket--usw2-az12--x-s3")
                                                                                               .useFips(true)
                                                                                               .useDualStack(true)
                                                                                               .accelerate(false)
                                                                                               .useS3ExpressControlEndpoint(false)
                                                                                               .build());

        nonErrorCases.put("353: Data Plane sigv4 auth with zone and dualstack", S3EndpointParams.builder()
                                                                                                .region(Region.of("us-west-2"))
                                                                                                .bucket("mybucket--usw2-az12--x-s3")
                                                                                                .useFips(false)
                                                                                                .useDualStack(true)
                                                                                                .accelerate(false)
                                                                                                .disableS3ExpressSessionAuth(true)
                                                                                                .build());

        nonErrorCases.put("354: Data Plane sigv4 auth with 9-char zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                                 .region(Region.of("us-west-2"))
                                                                                                                 .bucket("mybucket--usw2-az12--x-s3")
                                                                                                                 .useFips(true)
                                                                                                                 .useDualStack(true)
                                                                                                                 .accelerate(false)
                                                                                                                 .disableS3ExpressSessionAuth(true)
                                                                                                                 .build());

        nonErrorCases.put("355: Data Plane with 13-char zone and dualstack", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                             .useFips(false)
                                                                                             .useDualStack(true)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(false)
                                                                                             .build());

        nonErrorCases.put("356: Data Plane with 13-char zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                                       .useFips(true)
                                                                                                       .useDualStack(true)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("357: Data Plane sigv4 auth with 13-char zone and dualstack", S3EndpointParams.builder()
                                                                                                        .region(Region.of("us-west-2"))
                                                                                                        .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                                        .useFips(false)
                                                                                                        .useDualStack(true)
                                                                                                        .accelerate(false)
                                                                                                        .disableS3ExpressSessionAuth(true)
                                                                                                        .build());

        nonErrorCases.put("358: Data Plane sigv4 auth with 13-char zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                                  .region(Region.of("us-west-2"))
                                                                                                                  .bucket("mybucket--test-zone-ab1--x-s3")
                                                                                                                  .useFips(true)
                                                                                                                  .useDualStack(true)
                                                                                                                  .accelerate(false)
                                                                                                                  .disableS3ExpressSessionAuth(true)
                                                                                                                  .build());

        nonErrorCases.put("359: Data Plane with 14-char zone and dualstack", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                             .useFips(false)
                                                                                             .useDualStack(true)
                                                                                             .accelerate(false)
                                                                                             .useS3ExpressControlEndpoint(false)
                                                                                             .build());

        nonErrorCases.put("360: Data Plane with 14-char zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                                       .useFips(true)
                                                                                                       .useDualStack(true)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("361: Data Plane sigv4 auth with 14-char zone and dualstack", S3EndpointParams.builder()
                                                                                                        .region(Region.of("us-west-2"))
                                                                                                        .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                                        .useFips(false)
                                                                                                        .useDualStack(true)
                                                                                                        .accelerate(false)
                                                                                                        .disableS3ExpressSessionAuth(true)
                                                                                                        .build());

        nonErrorCases.put("362: Data Plane sigv4 auth with 14-char zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                                  .region(Region.of("us-west-2"))
                                                                                                                  .bucket("mybucket--test1-zone-ab1--x-s3")
                                                                                                                  .useFips(true)
                                                                                                                  .useDualStack(true)
                                                                                                                  .accelerate(false)
                                                                                                                  .disableS3ExpressSessionAuth(true)
                                                                                                                  .build());

        nonErrorCases.put("363: Data Plane with long zone (20 cha) and dualstack", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-west-2"))
                                                                                                   .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                                   .useFips(false)
                                                                                                   .useDualStack(true)
                                                                                                   .accelerate(false)
                                                                                                   .useS3ExpressControlEndpoint(false)
                                                                                                   .build());

        nonErrorCases.put("364: Data Plane with long zone (20 char) and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                              .region(Region.of("us-west-2"))
                                                                                                              .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                                              .useFips(true)
                                                                                                              .useDualStack(true)
                                                                                                              .accelerate(false)
                                                                                                              .useS3ExpressControlEndpoint(false)
                                                                                                              .build());

        nonErrorCases.put("365: Data Plane sigv4 auth with long zone (20 char) and dualstack", S3EndpointParams.builder()
                                                                                                               .region(Region.of("us-west-2"))
                                                                                                               .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                                               .useFips(false)
                                                                                                               .useDualStack(true)
                                                                                                               .accelerate(false)
                                                                                                               .disableS3ExpressSessionAuth(true)
                                                                                                               .build());

        nonErrorCases.put("366: Data Plane sigv4 auth with long zone (20 char) and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                                         .region(Region.of("us-west-2"))
                                                                                                                         .bucket("mybucket--test1-long1-zone-ab1--x-s3")
                                                                                                                         .useFips(true)
                                                                                                                         .useDualStack(true)
                                                                                                                         .accelerate(false)
                                                                                                                         .disableS3ExpressSessionAuth(true)
                                                                                                                         .build());

        nonErrorCases.put("367: Control plane and FIPS with dualstack", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .bucket("mybucket--test-ab1--x-s3")
                                                                                        .useFips(true)
                                                                                        .useDualStack(true)
                                                                                        .accelerate(false)
                                                                                        .useS3ExpressControlEndpoint(true)
                                                                                        .build());

        nonErrorCases.put("368: Data plane with zone and dualstack and AP", S3EndpointParams.builder()
                                                                                            .region(Region.of("us-west-2"))
                                                                                            .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                            .useFips(false)
                                                                                            .useDualStack(true)
                                                                                            .accelerate(false)
                                                                                            .useS3ExpressControlEndpoint(false)
                                                                                            .build());

        nonErrorCases.put("369: Data plane with zone and FIPS with dualstack and AP", S3EndpointParams.builder()
                                                                                                      .region(Region.of("us-west-2"))
                                                                                                      .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                                      .useFips(true)
                                                                                                      .useDualStack(true)
                                                                                                      .accelerate(false)
                                                                                                      .useS3ExpressControlEndpoint(false)
                                                                                                      .build());

        nonErrorCases.put("370: Data Plane sigv4 auth with zone and dualstack and AP", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(true)
                                                                                                       .accelerate(false)
                                                                                                       .disableS3ExpressSessionAuth(true)
                                                                                                       .build());

        nonErrorCases.put("371: Data Plane AP sigv4 auth with zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                             .region(Region.of("us-west-2"))
                                                                                                             .bucket("myaccesspoint--usw2-az1--xa-s3")
                                                                                                             .useFips(true)
                                                                                                             .useDualStack(true)
                                                                                                             .accelerate(false)
                                                                                                             .disableS3ExpressSessionAuth(true)
                                                                                                             .build());

        nonErrorCases.put("372: Data Plane with zone (9 char) and AP with dualstack", S3EndpointParams.builder()
                                                                                                      .region(Region.of("us-west-2"))
                                                                                                      .bucket("myaccesspoint--usw2-az12--xa-s3")
                                                                                                      .useFips(false)
                                                                                                      .useDualStack(true)
                                                                                                      .accelerate(false)
                                                                                                      .useS3ExpressControlEndpoint(false)
                                                                                                      .build());

        nonErrorCases.put("373: Data Plane with zone (9 char) and FIPS with AP and dualstack", S3EndpointParams.builder()
                                                                                                               .region(Region.of("us-west-2"))
                                                                                                               .bucket("myaccesspoint--usw2-az12--xa-s3")
                                                                                                               .useFips(true)
                                                                                                               .useDualStack(true)
                                                                                                               .accelerate(false)
                                                                                                               .useS3ExpressControlEndpoint(false)
                                                                                                               .build());

        nonErrorCases.put("374: Data Plane sigv4 auth with (9 char) zone and dualstack with AP", S3EndpointParams.builder()
                                                                                                                 .region(Region.of("us-west-2"))
                                                                                                                 .bucket("myaccesspoint--usw2-az12--xa-s3")
                                                                                                                 .useFips(false)
                                                                                                                 .useDualStack(true)
                                                                                                                 .accelerate(false)
                                                                                                                 .disableS3ExpressSessionAuth(true)
                                                                                                                 .build());

        nonErrorCases.put("375: Access Point sigv4 auth with (9 char) zone and FIPS with dualstack", S3EndpointParams.builder()
                                                                                                                     .region(Region.of("us-west-2"))
                                                                                                                     .bucket("myaccesspoint--usw2-az12--xa-s3")
                                                                                                                     .useFips(true)
                                                                                                                     .useDualStack(true)
                                                                                                                     .accelerate(false)
                                                                                                                     .disableS3ExpressSessionAuth(true)
                                                                                                                     .build());

        nonErrorCases.put("376: Data Plane with zone (13 char) and AP with dualstack", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(true)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("377: Data Plane with zone (13 char) and AP with FIPS and dualstack", S3EndpointParams.builder()
                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                                .useFips(true)
                                                                                                                .useDualStack(true)
                                                                                                                .accelerate(false)
                                                                                                                .useS3ExpressControlEndpoint(false)
                                                                                                                .build());

        nonErrorCases.put("378: Data Plane sigv4 auth with (13 char) zone with AP and dualstack", S3EndpointParams.builder()
                                                                                                                  .region(Region.of("us-west-2"))
                                                                                                                  .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                                  .useFips(false)
                                                                                                                  .useDualStack(true)
                                                                                                                  .accelerate(false)
                                                                                                                  .disableS3ExpressSessionAuth(true)
                                                                                                                  .build());

        nonErrorCases.put("379: Data Plane sigv4 auth with (13 char) zone with AP and FIPS and dualstack", S3EndpointParams.builder()
                                                                                                                           .region(Region.of("us-west-2"))
                                                                                                                           .bucket("myaccesspoint--test-zone-ab1--xa-s3")
                                                                                                                           .useFips(true)
                                                                                                                           .useDualStack(true)
                                                                                                                           .accelerate(false)
                                                                                                                           .disableS3ExpressSessionAuth(true)
                                                                                                                           .build());

        nonErrorCases.put("380: Data Plane with (14 char) zone and AP with dualstack", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(true)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("381: Data Plane with (14 char) zone and AP with FIPS and dualstack", S3EndpointParams.builder()
                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                                .useFips(true)
                                                                                                                .useDualStack(true)
                                                                                                                .accelerate(false)
                                                                                                                .useS3ExpressControlEndpoint(false)
                                                                                                                .build());

        nonErrorCases.put("382: Data Plane sigv4 auth with (14 char) zone and AP with dualstack", S3EndpointParams.builder()
                                                                                                                  .region(Region.of("us-west-2"))
                                                                                                                  .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                                  .useFips(false)
                                                                                                                  .useDualStack(true)
                                                                                                                  .accelerate(false)
                                                                                                                  .disableS3ExpressSessionAuth(true)
                                                                                                                  .build());

        nonErrorCases.put("383: Data Plane with (14 char) zone and AP with FIPS and dualstack", S3EndpointParams.builder()
                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                .bucket("myaccesspoint--test1-zone-ab1--xa-s3")
                                                                                                                .useFips(true)
                                                                                                                .useDualStack(true)
                                                                                                                .accelerate(false)
                                                                                                                .disableS3ExpressSessionAuth(true)
                                                                                                                .build());

        nonErrorCases.put("384: Data Plane with (20 char) zone and AP with dualstack", S3EndpointParams.builder()
                                                                                                       .region(Region.of("us-west-2"))
                                                                                                       .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                                       .useFips(false)
                                                                                                       .useDualStack(true)
                                                                                                       .accelerate(false)
                                                                                                       .useS3ExpressControlEndpoint(false)
                                                                                                       .build());

        nonErrorCases.put("385: Data Plane with (20 char) zone and AP with FIPS and dualstack", S3EndpointParams.builder()
                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                                                .useFips(true)
                                                                                                                .useDualStack(true)
                                                                                                                .accelerate(false)
                                                                                                                .useS3ExpressControlEndpoint(false)
                                                                                                                .build());

        nonErrorCases.put("386: Data plane AP with sigv4 and dualstack", S3EndpointParams.builder()
                                                                                         .region(Region.of("us-west-2"))
                                                                                         .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                         .useFips(false)
                                                                                         .useDualStack(true)
                                                                                         .accelerate(false)
                                                                                         .disableS3ExpressSessionAuth(true)
                                                                                         .build());

        nonErrorCases.put("387: Data plane AP sigv4 with fips and dualstack", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .bucket("myaccesspoint--test1-long1-zone-ab1--xa-s3")
                                                                                              .useFips(true)
                                                                                              .useDualStack(true)
                                                                                              .accelerate(false)
                                                                                              .disableS3ExpressSessionAuth(true)
                                                                                              .build());

        nonErrorCases.put("388: Control plane with dualstack and bucket", S3EndpointParams.builder()
                                                                                          .region(Region.of("us-east-1"))
                                                                                          .bucket("mybucket--test-ab1--x-s3")
                                                                                          .useFips(false)
                                                                                          .useDualStack(true)
                                                                                          .accelerate(false)
                                                                                          .useS3ExpressControlEndpoint(true)
                                                                                          .build());

        errorCases = new HashMap<>();

        errorCases.put("0: region is not a valid DNS-suffix", S3EndpointParams.builder()
                                                                              .region(Region.of("a b"))
                                                                              .useFips(false)
                                                                              .useDualStack(false)
                                                                              .accelerate(false)
                                                                              .build());

        errorCases.put("1: Invalid access point ARN: Not S3", S3EndpointParams.builder()
                                                                              .region(Region.of("us-east-1"))
                                                                              .useFips(false)
                                                                              .useDualStack(false)
                                                                              .accelerate(false)
                                                                              .bucket("arn:aws:not-s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                              .build());

        errorCases.put("2: Invalid access point ARN: invalid resource", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .useFips(false)
                                                                                        .useDualStack(false)
                                                                                        .accelerate(false)
                                                                                        .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint:more-data")
                                                                                        .build());

        errorCases.put("3: Invalid access point ARN: invalid no ap name", S3EndpointParams.builder()
                                                                                          .region(Region.of("us-east-1"))
                                                                                          .useFips(false)
                                                                                          .useDualStack(false)
                                                                                          .accelerate(false)
                                                                                          .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:")
                                                                                          .build());

        errorCases.put("4: Invalid access point ARN: AccountId is invalid", S3EndpointParams.builder()
                                                                                            .region(Region.of("us-east-1"))
                                                                                            .useFips(false)
                                                                                            .useDualStack(false)
                                                                                            .accelerate(false)
                                                                                            .bucket("arn:aws:s3:us-west-2:123456_789012:accesspoint:apname")
                                                                                            .build());

        errorCases.put("5: Invalid access point ARN: access point name is invalid", S3EndpointParams.builder()
                                                                                                    .region(Region.of("us-east-1"))
                                                                                                    .useFips(false)
                                                                                                    .useDualStack(false)
                                                                                                    .accelerate(false)
                                                                                                    .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:ap_name")
                                                                                                    .build());

        errorCases.put("7: Access points: partition does not support FIPS", S3EndpointParams.builder()
                                                                                            .region(Region.of("cn-north-1"))
                                                                                            .useFips(true)
                                                                                            .useDualStack(false)
                                                                                            .accelerate(false)
                                                                                            .bucket("arn:aws:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                            .build());

        errorCases.put("8: Bucket region is invalid", S3EndpointParams.builder()
                                                                      .region(Region.of("us-east-1"))
                                                                      .useFips(false)
                                                                      .useDualStack(false)
                                                                      .accelerate(false)
                                                                      .disableAccessPoints(false)
                                                                      .bucket("arn:aws:s3:us-west -2:123456789012:accesspoint:myendpoint")
                                                                      .build());

        errorCases.put("9: Access points when Access points explicitly disabled (used for CreateBucket)", S3EndpointParams.builder()
                                                                                                                          .region(Region.of("us-east-1"))
                                                                                                                          .useFips(false)
                                                                                                                          .useDualStack(false)
                                                                                                                          .accelerate(false)
                                                                                                                          .disableAccessPoints(true)
                                                                                                                          .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                                                          .build());

        errorCases.put("10: missing arn type", S3EndpointParams.builder()
                                                               .region(Region.of("us-east-1"))
                                                               .useFips(false)
                                                               .useDualStack(false)
                                                               .accelerate(false)
                                                               .disableAccessPoints(true)
                                                               .bucket("arn:aws:s3:us-west-2:123456789012:")
                                                               .build());

        errorCases.put("11: SDK::Host + access point + Dualstack is an error", S3EndpointParams.builder()
                                                                                               .accelerate(false)
                                                                                               .bucket("arn:aws-cn:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                               .forcePathStyle(false)
                                                                                               .endpoint("https://beta.example.com")
                                                                                               .region(Region.of("cn-north-1"))
                                                                                               .useDualStack(true)
                                                                                               .useFips(false)
                                                                                               .build());

        errorCases.put("15: MRAP does not support FIPS", S3EndpointParams.builder()
                                                                         .bucket("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                                         .region(Region.of("us-east-1"))
                                                                         .disableMultiRegionAccessPoints(false)
                                                                         .useFips(true)
                                                                         .useDualStack(false)
                                                                         .accelerate(false)
                                                                         .build());

        errorCases.put("16: MRAP does not support DualStack", S3EndpointParams.builder()
                                                                              .bucket("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                                              .region(Region.of("us-east-1"))
                                                                              .disableMultiRegionAccessPoints(false)
                                                                              .useFips(false)
                                                                              .useDualStack(true)
                                                                              .accelerate(false)
                                                                              .build());

        errorCases.put("17: MRAP does not support S3 Accelerate", S3EndpointParams.builder()
                                                                                  .bucket("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                                                  .region(Region.of("us-east-1"))
                                                                                  .disableMultiRegionAccessPoints(false)
                                                                                  .useFips(false)
                                                                                  .useDualStack(false)
                                                                                  .accelerate(true)
                                                                                  .build());

        errorCases.put("18: MRAP explicitly disabled", S3EndpointParams.builder()
                                                                       .bucket("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                                       .region(Region.of("us-east-1"))
                                                                       .disableMultiRegionAccessPoints(true)
                                                                       .useFips(false)
                                                                       .useDualStack(false)
                                                                       .accelerate(false)
                                                                       .build());

        errorCases.put("20: Dual-stack endpoint + SDK::Host is error", S3EndpointParams.builder()
                                                                                       .bucket("bucketname")
                                                                                       .region(Region.of("us-west-2"))
                                                                                       .forcePathStyle(true)
                                                                                       .useFips(false)
                                                                                       .accelerate(false)
                                                                                       .useDualStack(true)
                                                                                       .endpoint("https://abc.com")
                                                                                       .build());

        errorCases.put("21: path style + ARN bucket", S3EndpointParams.builder()
                                                                      .accelerate(false)
                                                                      .bucket("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                                      .forcePathStyle(true)
                                                                      .region(Region.of("us-west-2"))
                                                                      .useDualStack(false)
                                                                      .useFips(false)
                                                                      .build());

        errorCases.put("23: implicit path style bucket + dualstack", S3EndpointParams.builder()
                                                                                     .accelerate(false)
                                                                                     .bucket("99_ab")
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .useDualStack(true)
                                                                                     .useFips(false)
                                                                                     .endpoint("http://abc.com")
                                                                                     .build());

        errorCases.put("29: invalid Endpoint override", S3EndpointParams.builder()
                                                                        .accelerate(false)
                                                                        .bucket("bucketname")
                                                                        .endpoint("abcde://nota#url")
                                                                        .region(Region.of("af-south-1"))
                                                                        .useDualStack(false)
                                                                        .useFips(false)
                                                                        .build());

        errorCases.put("31: vanilla access point arn with region mismatch and UseArnRegion=false", S3EndpointParams.builder()
                                                                                                                   .accelerate(false)
                                                                                                                   .bucket("arn:aws:s3:us-east-1:123456789012:accesspoint:myendpoint")
                                                                                                                   .forcePathStyle(false)
                                                                                                                   .useArnRegion(false)
                                                                                                                   .region(Region.of("us-west-2"))
                                                                                                                   .useDualStack(false)
                                                                                                                   .useFips(false)
                                                                                                                   .build());

        errorCases.put("39: no region set", S3EndpointParams.builder()
                                                            .bucket("bucket-name")
                                                            .build());

        errorCases.put("78: cross partition MRAP ARN is an error", S3EndpointParams.builder()
                                                                                   .bucket("arn:aws-cn:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                                                                   .region(Region.of("us-west-1"))
                                                                                   .build());

        errorCases.put("81: non-bucket endpoint override with FIPS = error", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-west-2"))
                                                                                             .endpoint("http://beta.example.com:1234/path")
                                                                                             .useFips(true)
                                                                                             .useDualStack(false)
                                                                                             .build());

        errorCases.put("82: FIPS + dualstack + custom endpoint", S3EndpointParams.builder()
                                                                                 .region(Region.of("us-west-2"))
                                                                                 .endpoint("http://beta.example.com:1234/path")
                                                                                 .useFips(true)
                                                                                 .useDualStack(true)
                                                                                 .build());

        errorCases.put("83: dualstack + custom endpoint", S3EndpointParams.builder()
                                                                          .region(Region.of("us-west-2"))
                                                                          .endpoint("http://beta.example.com:1234/path")
                                                                          .useFips(false)
                                                                          .useDualStack(true)
                                                                          .build());

        errorCases.put("85: s3 object lambda with access points disabled", S3EndpointParams.builder()
                                                                                           .region(Region.of("us-west-2"))
                                                                                           .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                           .disableAccessPoints(true)
                                                                                           .build());

        errorCases.put("95: endpoint override + FIPS + dualstack (BUG)", S3EndpointParams.builder()
                                                                                         .region(Region.of("us-east-1"))
                                                                                         .bucket("bucket!")
                                                                                         .forcePathStyle(true)
                                                                                         .useFips(true)
                                                                                         .useDualStack(false)
                                                                                         .endpoint("http://foo.com")
                                                                                         .build());

        errorCases.put("96: endpoint override + non-dns bucket + FIPS (BUG)", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-east-1"))
                                                                                              .bucket("bucket!")
                                                                                              .useFips(true)
                                                                                              .useDualStack(false)
                                                                                              .endpoint("http://foo.com")
                                                                                              .build());

        errorCases.put("100: URI encoded bucket + use global endpoint", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-1"))
                                                                                        .bucket("bucket!")
                                                                                        .useFips(true)
                                                                                        .useDualStack(false)
                                                                                        .useGlobalEndpoint(true)
                                                                                        .endpoint("https://foo.com")
                                                                                        .build());

        errorCases.put("105: endpoint override + non-uri safe endpoint + force path style", S3EndpointParams.builder()
                                                                                                            .region(Region.of("us-east-1"))
                                                                                                            .bucket("bucket!")
                                                                                                            .forcePathStyle(true)
                                                                                                            .accelerate(false)
                                                                                                            .useDualStack(false)
                                                                                                            .useFips(true)
                                                                                                            .endpoint("http://foo.com")
                                                                                                            .useGlobalEndpoint(true)
                                                                                                            .build());

        errorCases.put("107: endpoint override + FIPS + dualstack", S3EndpointParams.builder()
                                                                                    .region(Region.of("us-east-1"))
                                                                                    .useDualStack(true)
                                                                                    .useFips(true)
                                                                                    .useGlobalEndpoint(true)
                                                                                    .endpoint("http://foo.com")
                                                                                    .build());

        errorCases.put("108: non-bucket endpoint override + dualstack + global endpoint", S3EndpointParams.builder()
                                                                                                          .region(Region.of("us-east-1"))
                                                                                                          .useFips(false)
                                                                                                          .useDualStack(true)
                                                                                                          .useGlobalEndpoint(true)
                                                                                                          .endpoint("http://foo.com")
                                                                                                          .build());

        errorCases.put("109: Endpoint override + UseGlobalEndpoint + us-east-1", S3EndpointParams.builder()
                                                                                                 .region(Region.of("us-east-1"))
                                                                                                 .useFips(true)
                                                                                                 .useDualStack(false)
                                                                                                 .useGlobalEndpoint(true)
                                                                                                 .endpoint("http://foo.com")
                                                                                                 .build());

        errorCases.put("110: non-FIPS partition with FIPS set + custom endpoint", S3EndpointParams.builder()
                                                                                                  .region(Region.of("cn-north-1"))
                                                                                                  .useFips(true)
                                                                                                  .useDualStack(false)
                                                                                                  .useGlobalEndpoint(true)
                                                                                                  .build());

        errorCases.put("115: aws-global + fips + custom endpoint", S3EndpointParams.builder()
                                                                                   .region(Region.of("aws-global"))
                                                                                   .bucket("bucket!")
                                                                                   .useDualStack(false)
                                                                                   .useFips(true)
                                                                                   .accelerate(false)
                                                                                   .endpoint("http://foo.com")
                                                                                   .build());

        errorCases.put("117: aws-global + dualstack + custom endpoint", S3EndpointParams.builder()
                                                                                        .region(Region.of("aws-global"))
                                                                                        .useDualStack(true)
                                                                                        .useFips(false)
                                                                                        .accelerate(false)
                                                                                        .endpoint("http://foo.com")
                                                                                        .build());

        errorCases.put("120: aws-global + FIPS + endpoint override.", S3EndpointParams.builder()
                                                                                      .region(Region.of("aws-global"))
                                                                                      .useFips(true)
                                                                                      .endpoint("http://foo.com")
                                                                                      .build());

        errorCases.put("121: force path style, FIPS, aws-global & endpoint override", S3EndpointParams.builder()
                                                                                                      .region(Region.of("aws-global"))
                                                                                                      .bucket("bucket!")
                                                                                                      .forcePathStyle(true)
                                                                                                      .useFips(true)
                                                                                                      .endpoint("http://foo.com")
                                                                                                      .build());

        errorCases.put("123: endpoint override with aws-global region", S3EndpointParams.builder()
                                                                                        .region(Region.of("aws-global"))
                                                                                        .useFips(true)
                                                                                        .useDualStack(true)
                                                                                        .endpoint("http://foo.com")
                                                                                        .build());

        errorCases.put("125: empty arn type", S3EndpointParams.builder()
                                                              .region(Region.of("us-east-2"))
                                                              .bucket("arn:aws:not-s3:us-west-2:123456789012::myendpoint")
                                                              .build());

        errorCases.put("126: path style can't be used with accelerate", S3EndpointParams.builder()
                                                                                        .region(Region.of("us-east-2"))
                                                                                        .bucket("bucket!")
                                                                                        .accelerate(true)
                                                                                        .build());

        errorCases.put("127: invalid region", S3EndpointParams.builder()
                                                              .region(Region.of("us-east-2!"))
                                                              .bucket("bucket.subdomain")
                                                              .endpoint("http://foo.com")
                                                              .build());

        errorCases.put("128: invalid region", S3EndpointParams.builder()
                                                              .region(Region.of("us-east-2!"))
                                                              .bucket("bucket")
                                                              .endpoint("http://foo.com")
                                                              .build());

        errorCases.put("129: empty arn type", S3EndpointParams.builder()
                                                              .region(Region.of("us-east-2"))
                                                              .bucket("arn:aws:s3::123456789012:accesspoint:my_endpoint")
                                                              .build());

        errorCases.put("130: empty arn type", S3EndpointParams.builder()
                                                              .region(Region.of("us-east-2"))
                                                              .bucket("arn:aws:s3:cn-north-1:123456789012:accesspoint:my-endpoint")
                                                              .useArnRegion(true)
                                                              .build());

        errorCases.put("131: invalid arn region", S3EndpointParams.builder()
                                                                  .region(Region.of("us-east-2"))
                                                                  .bucket("arn:aws:s3-object-lambda:us-east_2:123456789012:accesspoint:my-endpoint")
                                                                  .useArnRegion(true)
                                                                  .build());

        errorCases.put("132: invalid ARN outpost", S3EndpointParams.builder()
                                                                   .region(Region.of("us-east-2"))
                                                                   .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost/op_01234567890123456/accesspoint/reports")
                                                                   .useArnRegion(true)
                                                                   .build());

        errorCases.put("133: invalid ARN", S3EndpointParams.builder()
                                                           .region(Region.of("us-east-2"))
                                                           .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost/op-01234567890123456/reports")
                                                           .build());

        errorCases.put("134: invalid ARN", S3EndpointParams.builder()
                                                           .region(Region.of("us-east-2"))
                                                           .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost/op-01234567890123456")
                                                           .build());

        errorCases.put("135: invalid outpost type", S3EndpointParams.builder()
                                                                    .region(Region.of("us-east-2"))
                                                                    .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost/op-01234567890123456/not-accesspoint/reports")
                                                                    .build());

        errorCases.put("136: invalid outpost type", S3EndpointParams.builder()
                                                                    .region(Region.of("us-east-2"))
                                                                    .bucket("arn:aws:s3-outposts:us-east_1:123456789012:outpost/op-01234567890123456/not-accesspoint/reports")
                                                                    .build());

        errorCases.put("137: invalid outpost type", S3EndpointParams.builder()
                                                                    .region(Region.of("us-east-2"))
                                                                    .bucket("arn:aws:s3-outposts:us-east-1:12345_789012:outpost/op-01234567890123456/not-accesspoint/reports")
                                                                    .build());

        errorCases.put("138: invalid outpost type", S3EndpointParams.builder()
                                                                    .region(Region.of("us-east-2"))
                                                                    .bucket("arn:aws:s3-outposts:us-east-1:12345789012:outpost")
                                                                    .build());

        errorCases.put("151: accelerate + fips = error@us-west-2", S3EndpointParams.builder()
                                                                                   .accelerate(true)
                                                                                   .bucket("bucket-name")
                                                                                   .forcePathStyle(false)
                                                                                   .region(Region.of("us-west-2"))
                                                                                   .useDualStack(false)
                                                                                   .useFips(true)
                                                                                   .build());

        errorCases.put("154: accelerate (dualstack=false)@cn-north-1", S3EndpointParams.builder()
                                                                                       .accelerate(true)
                                                                                       .bucket("bucket-name")
                                                                                       .forcePathStyle(false)
                                                                                       .region(Region.of("cn-north-1"))
                                                                                       .useDualStack(false)
                                                                                       .useFips(false)
                                                                                       .build());

        errorCases.put("155: virtual addressing + fips@cn-north-1", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("bucket-name")
                                                                                    .forcePathStyle(false)
                                                                                    .region(Region.of("cn-north-1"))
                                                                                    .useDualStack(false)
                                                                                    .useFips(true)
                                                                                    .build());

        errorCases.put("162: accelerate + fips = error@af-south-1", S3EndpointParams.builder()
                                                                                    .accelerate(true)
                                                                                    .bucket("bucket-name")
                                                                                    .forcePathStyle(false)
                                                                                    .region(Region.of("af-south-1"))
                                                                                    .useDualStack(false)
                                                                                    .useFips(true)
                                                                                    .build());

        errorCases.put("165: path style + accelerate = error@us-west-2", S3EndpointParams.builder()
                                                                                         .accelerate(true)
                                                                                         .bucket("bucket-name")
                                                                                         .forcePathStyle(true)
                                                                                         .region(Region.of("us-west-2"))
                                                                                         .useDualStack(false)
                                                                                         .useFips(false)
                                                                                         .build());

        errorCases.put("167: path style + arn is error@us-west-2", S3EndpointParams.builder()
                                                                                   .accelerate(false)
                                                                                   .bucket("arn:PARTITION:s3-outposts:REGION:123456789012:outpost:op-01234567890123456:bucket:mybucket")
                                                                                   .forcePathStyle(true)
                                                                                   .region(Region.of("us-west-2"))
                                                                                   .useDualStack(false)
                                                                                   .useFips(false)
                                                                                   .build());

        errorCases.put("171: path style + fips@cn-north-1", S3EndpointParams.builder()
                                                                            .accelerate(false)
                                                                            .bucket("bucket-name")
                                                                            .forcePathStyle(true)
                                                                            .region(Region.of("cn-north-1"))
                                                                            .useDualStack(false)
                                                                            .useFips(true)
                                                                            .build());

        errorCases.put("172: path style + accelerate = error@cn-north-1", S3EndpointParams.builder()
                                                                                          .accelerate(true)
                                                                                          .bucket("bucket-name")
                                                                                          .forcePathStyle(true)
                                                                                          .region(Region.of("cn-north-1"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        errorCases.put("174: path style + arn is error@cn-north-1", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("arn:PARTITION:s3-outposts:REGION:123456789012:outpost:op-01234567890123456:bucket:mybucket")
                                                                                    .forcePathStyle(true)
                                                                                    .region(Region.of("cn-north-1"))
                                                                                    .useDualStack(false)
                                                                                    .useFips(false)
                                                                                    .build());

        errorCases.put("179: path style + accelerate = error@af-south-1", S3EndpointParams.builder()
                                                                                          .accelerate(true)
                                                                                          .bucket("bucket-name")
                                                                                          .forcePathStyle(true)
                                                                                          .region(Region.of("af-south-1"))
                                                                                          .useDualStack(false)
                                                                                          .useFips(false)
                                                                                          .build());

        errorCases.put("181: path style + arn is error@af-south-1", S3EndpointParams.builder()
                                                                                    .accelerate(false)
                                                                                    .bucket("arn:PARTITION:s3-outposts:REGION:123456789012:outpost:op-01234567890123456:bucket:mybucket")
                                                                                    .forcePathStyle(true)
                                                                                    .region(Region.of("af-south-1"))
                                                                                    .useDualStack(false)
                                                                                    .useFips(false)
                                                                                    .build());

        errorCases.put("186: SDK::Host + FIPS@us-west-2", S3EndpointParams.builder()
                                                                          .accelerate(false)
                                                                          .bucket("bucket-name")
                                                                          .forcePathStyle(false)
                                                                          .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                          .region(Region.of("us-west-2"))
                                                                          .useDualStack(false)
                                                                          .useFips(true)
                                                                          .build());

        errorCases.put("187: SDK::Host + DualStack@us-west-2", S3EndpointParams.builder()
                                                                               .accelerate(false)
                                                                               .bucket("bucket-name")
                                                                               .forcePathStyle(false)
                                                                               .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                               .region(Region.of("us-west-2"))
                                                                               .useDualStack(true)
                                                                               .useFips(false)
                                                                               .build());

        errorCases.put("188: SDK::HOST + accelerate@us-west-2", S3EndpointParams.builder()
                                                                                .accelerate(true)
                                                                                .bucket("bucket-name")
                                                                                .forcePathStyle(false)
                                                                                .endpoint("http://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                .region(Region.of("us-west-2"))
                                                                                .useDualStack(false)
                                                                                .useFips(false)
                                                                                .build());

        errorCases.put("192: FIPS@cn-north-1", S3EndpointParams.builder()
                                                               .accelerate(false)
                                                               .bucket("bucket-name")
                                                               .forcePathStyle(false)
                                                               .region(Region.of("cn-north-1"))
                                                               .useDualStack(false)
                                                               .useFips(true)
                                                               .build());

        errorCases.put("193: SDK::Host + DualStack@cn-north-1", S3EndpointParams.builder()
                                                                                .accelerate(false)
                                                                                .bucket("bucket-name")
                                                                                .forcePathStyle(false)
                                                                                .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                .region(Region.of("cn-north-1"))
                                                                                .useDualStack(true)
                                                                                .useFips(false)
                                                                                .build());

        errorCases.put("194: SDK::HOST + accelerate@cn-north-1", S3EndpointParams.builder()
                                                                                 .accelerate(true)
                                                                                 .bucket("bucket-name")
                                                                                 .forcePathStyle(false)
                                                                                 .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                 .region(Region.of("cn-north-1"))
                                                                                 .useDualStack(false)
                                                                                 .useFips(false)
                                                                                 .build());

        errorCases.put("198: SDK::Host + FIPS@af-south-1", S3EndpointParams.builder()
                                                                           .accelerate(false)
                                                                           .bucket("bucket-name")
                                                                           .forcePathStyle(false)
                                                                           .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                           .region(Region.of("af-south-1"))
                                                                           .useDualStack(false)
                                                                           .useFips(true)
                                                                           .build());

        errorCases.put("199: SDK::Host + DualStack@af-south-1", S3EndpointParams.builder()
                                                                                .accelerate(false)
                                                                                .bucket("bucket-name")
                                                                                .forcePathStyle(false)
                                                                                .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                .region(Region.of("af-south-1"))
                                                                                .useDualStack(true)
                                                                                .useFips(false)
                                                                                .build());

        errorCases.put("200: SDK::HOST + accelerate@af-south-1", S3EndpointParams.builder()
                                                                                 .accelerate(true)
                                                                                 .bucket("bucket-name")
                                                                                 .forcePathStyle(false)
                                                                                 .endpoint("https://control.vpce-1a2b3c4d-5e6f.s3.us-west-2.vpce.amazonaws.com")
                                                                                 .region(Region.of("af-south-1"))
                                                                                 .useDualStack(false)
                                                                                 .useFips(false)
                                                                                 .build());

        errorCases.put("204: access point arn + accelerate = error@us-west-2", S3EndpointParams.builder()
                                                                                               .accelerate(true)
                                                                                               .bucket("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                                                                               .forcePathStyle(false)
                                                                                               .region(Region.of("us-west-2"))
                                                                                               .useDualStack(false)
                                                                                               .useFips(false)
                                                                                               .build());

        errorCases.put("207: access point arn + FIPS@cn-north-1", S3EndpointParams.builder()
                                                                                  .accelerate(false)
                                                                                  .bucket("arn:aws-cn:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                  .forcePathStyle(false)
                                                                                  .region(Region.of("cn-north-1"))
                                                                                  .useDualStack(false)
                                                                                  .useFips(true)
                                                                                  .build());

        errorCases.put("208: access point arn + accelerate = error@cn-north-1", S3EndpointParams.builder()
                                                                                                .accelerate(true)
                                                                                                .bucket("arn:aws-cn:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                                .forcePathStyle(false)
                                                                                                .region(Region.of("cn-north-1"))
                                                                                                .useDualStack(false)
                                                                                                .useFips(false)
                                                                                                .build());

        errorCases.put("209: access point arn + FIPS + DualStack@cn-north-1", S3EndpointParams.builder()
                                                                                              .accelerate(false)
                                                                                              .bucket("arn:aws-cn:s3:cn-north-1:123456789012:accesspoint:myendpoint")
                                                                                              .forcePathStyle(false)
                                                                                              .region(Region.of("cn-north-1"))
                                                                                              .useDualStack(true)
                                                                                              .useFips(true)
                                                                                              .build());

        errorCases.put("212: access point arn + accelerate = error@af-south-1", S3EndpointParams.builder()
                                                                                                .accelerate(true)
                                                                                                .bucket("arn:aws:s3:af-south-1:123456789012:accesspoint:myendpoint")
                                                                                                .forcePathStyle(false)
                                                                                                .region(Region.of("af-south-1"))
                                                                                                .useDualStack(false)
                                                                                                .useFips(false)
                                                                                                .build());

        errorCases.put("216: outposts arn with region mismatch and UseArnRegion=false", S3EndpointParams.builder()
                                                                                                        .accelerate(false)
                                                                                                        .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                                                                                        .forcePathStyle(false)
                                                                                                        .useArnRegion(false)
                                                                                                        .region(Region.of("us-west-2"))
                                                                                                        .useDualStack(false)
                                                                                                        .useFips(false)
                                                                                                        .build());

        errorCases.put("217: outposts arn with region mismatch, custom region and UseArnRegion=false", S3EndpointParams.builder()
                                                                                                                       .accelerate(false)
                                                                                                                       .bucket("arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                                                                                                       .endpoint("https://example.com")
                                                                                                                       .forcePathStyle(false)
                                                                                                                       .useArnRegion(false)
                                                                                                                       .region(Region.of("us-west-2"))
                                                                                                                       .useDualStack(false)
                                                                                                                       .useFips(false)
                                                                                                                       .build());

        errorCases.put("220: outposts arn with partition mismatch and UseArnRegion=true", S3EndpointParams.builder()
                                                                                                          .accelerate(false)
                                                                                                          .bucket("arn:aws:s3-outposts:cn-north-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                                                                                          .forcePathStyle(false)
                                                                                                          .useArnRegion(true)
                                                                                                          .region(Region.of("us-west-2"))
                                                                                                          .useDualStack(false)
                                                                                                          .useFips(false)
                                                                                                          .build());

        errorCases.put("222: S3 outposts does not support dualstack", S3EndpointParams.builder()
                                                                                      .region(Region.of("us-east-1"))
                                                                                      .useFips(false)
                                                                                      .useDualStack(true)
                                                                                      .accelerate(false)
                                                                                      .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                                      .build());

        errorCases.put("223: S3 outposts does not support fips", S3EndpointParams.builder()
                                                                                 .region(Region.of("us-east-1"))
                                                                                 .useFips(true)
                                                                                 .useDualStack(false)
                                                                                 .accelerate(false)
                                                                                 .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                                 .build());

        errorCases.put("224: S3 outposts does not support accelerate", S3EndpointParams.builder()
                                                                                       .region(Region.of("us-east-1"))
                                                                                       .useFips(false)
                                                                                       .useDualStack(false)
                                                                                       .accelerate(true)
                                                                                       .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports")
                                                                                       .build());

        errorCases.put("225: validates against subresource", S3EndpointParams.builder()
                                                                             .region(Region.of("us-west-2"))
                                                                             .useFips(false)
                                                                             .useDualStack(false)
                                                                             .accelerate(false)
                                                                             .bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:mybucket:object:foo")
                                                                             .build());

        errorCases.put("231: object lambda @us-east-1, client region s3-external-1, useArnRegion=false", S3EndpointParams.builder()
                                                                                                                         .region(Region.of("s3-external-1"))
                                                                                                                         .useFips(false)
                                                                                                                         .useDualStack(false)
                                                                                                                         .accelerate(false)
                                                                                                                         .useArnRegion(false)
                                                                                                                         .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                                                                         .build());

        errorCases.put("233: object lambda @us-east-1, client region aws-global, useArnRegion=false", S3EndpointParams.builder()
                                                                                                                      .region(Region.of("aws-global"))
                                                                                                                      .useFips(false)
                                                                                                                      .useDualStack(false)
                                                                                                                      .accelerate(false)
                                                                                                                      .useArnRegion(false)
                                                                                                                      .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                                                                      .build());

        errorCases.put("234: object lambda @cn-north-1, client region us-west-2 (cross partition), useArnRegion=true", S3EndpointParams.builder()
                                                                                                                                       .region(Region.of("aws-global"))
                                                                                                                                       .useFips(false)
                                                                                                                                       .useDualStack(false)
                                                                                                                                       .accelerate(false)
                                                                                                                                       .useArnRegion(true)
                                                                                                                                       .bucket("arn:aws-cn:s3-object-lambda:cn-north-1:123456789012:accesspoint/mybanner")
                                                                                                                                       .build());

        errorCases.put("235: object lambda with dualstack", S3EndpointParams.builder()
                                                                            .region(Region.of("us-west-2"))
                                                                            .useFips(false)
                                                                            .useDualStack(true)
                                                                            .accelerate(false)
                                                                            .useArnRegion(false)
                                                                            .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/mybanner")
                                                                            .build());

        errorCases.put("238: object lambda @cn-north-1, with fips", S3EndpointParams.builder()
                                                                                    .region(Region.of("cn-north-1"))
                                                                                    .useFips(true)
                                                                                    .useDualStack(false)
                                                                                    .accelerate(false)
                                                                                    .useArnRegion(false)
                                                                                    .bucket("arn:aws-cn:s3-object-lambda:cn-north-1:123456789012:accesspoint/mybanner")
                                                                                    .build());

        errorCases.put("239: object lambda with accelerate", S3EndpointParams.builder()
                                                                             .region(Region.of("us-west-2"))
                                                                             .useFips(false)
                                                                             .useDualStack(false)
                                                                             .accelerate(true)
                                                                             .useArnRegion(false)
                                                                             .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint/mybanner")
                                                                             .build());

        errorCases.put("240: object lambda with invalid arn - bad service and someresource", S3EndpointParams.builder()
                                                                                                             .region(Region.of("us-west-2"))
                                                                                                             .useFips(false)
                                                                                                             .useDualStack(false)
                                                                                                             .accelerate(false)
                                                                                                             .useArnRegion(false)
                                                                                                             .bucket("arn:aws:sqs:us-west-2:123456789012:someresource")
                                                                                                             .build());

        errorCases.put("241: object lambda with invalid arn - invalid resource", S3EndpointParams.builder()
                                                                                                 .region(Region.of("us-west-2"))
                                                                                                 .useFips(false)
                                                                                                 .useDualStack(false)
                                                                                                 .accelerate(false)
                                                                                                 .useArnRegion(false)
                                                                                                 .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:bucket_name:mybucket")
                                                                                                 .build());

        errorCases.put("242: object lambda with invalid arn - missing region", S3EndpointParams.builder()
                                                                                               .region(Region.of("us-west-2"))
                                                                                               .useFips(false)
                                                                                               .useDualStack(false)
                                                                                               .accelerate(false)
                                                                                               .useArnRegion(false)
                                                                                               .bucket("arn:aws:s3-object-lambda::123456789012:accesspoint/mybanner")
                                                                                               .build());

        errorCases.put("243: object lambda with invalid arn - missing account-id", S3EndpointParams.builder()
                                                                                                   .region(Region.of("us-west-2"))
                                                                                                   .useFips(false)
                                                                                                   .useDualStack(false)
                                                                                                   .accelerate(false)
                                                                                                   .useArnRegion(true)
                                                                                                   .bucket("arn:aws:s3-object-lambda:us-west-2::accesspoint/mybanner")
                                                                                                   .build());

        errorCases.put("244: object lambda with invalid arn - account id contains invalid characters", S3EndpointParams.builder()
                                                                                                                       .region(Region.of("us-west-2"))
                                                                                                                       .useFips(false)
                                                                                                                       .useDualStack(false)
                                                                                                                       .accelerate(false)
                                                                                                                       .useArnRegion(true)
                                                                                                                       .bucket("arn:aws:s3-object-lambda:us-west-2:123.45678.9012:accesspoint:mybucket")
                                                                                                                       .build());

        errorCases.put("245: object lambda with invalid arn - missing access point name", S3EndpointParams.builder()
                                                                                                          .region(Region.of("us-west-2"))
                                                                                                          .useFips(false)
                                                                                                          .useDualStack(false)
                                                                                                          .accelerate(false)
                                                                                                          .useArnRegion(true)
                                                                                                          .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint")
                                                                                                          .build());

        errorCases.put("246: object lambda with invalid arn - access point name contains invalid character: *", S3EndpointParams.builder()
                                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                                .useFips(false)
                                                                                                                                .useDualStack(false)
                                                                                                                                .accelerate(false)
                                                                                                                                .useArnRegion(true)
                                                                                                                                .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:*")
                                                                                                                                .build());

        errorCases.put("247: object lambda with invalid arn - access point name contains invalid character: .", S3EndpointParams.builder()
                                                                                                                                .region(Region.of("us-west-2"))
                                                                                                                                .useFips(false)
                                                                                                                                .useDualStack(false)
                                                                                                                                .accelerate(false)
                                                                                                                                .useArnRegion(true)
                                                                                                                                .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:my.bucket")
                                                                                                                                .build());

        errorCases.put("248: object lambda with invalid arn - access point name contains sub resources", S3EndpointParams.builder()
                                                                                                                         .region(Region.of("us-west-2"))
                                                                                                                         .useFips(false)
                                                                                                                         .useDualStack(false)
                                                                                                                         .accelerate(false)
                                                                                                                         .useArnRegion(true)
                                                                                                                         .bucket("arn:aws:s3-object-lambda:us-west-2:123456789012:accesspoint:mybucket:object:foo")
                                                                                                                         .build());

        errorCases.put("250: object lambda arn with region mismatch and UseArnRegion=false", S3EndpointParams.builder()
                                                                                                             .accelerate(false)
                                                                                                             .bucket("arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/mybanner")
                                                                                                             .forcePathStyle(false)
                                                                                                             .useArnRegion(false)
                                                                                                             .region(Region.of("us-west-2"))
                                                                                                             .useDualStack(false)
                                                                                                             .useFips(false)
                                                                                                             .build());

        errorCases.put("255: WriteGetObjectResponse with dualstack", S3EndpointParams.builder()
                                                                                     .accelerate(false)
                                                                                     .useObjectLambdaEndpoint(true)
                                                                                     .region(Region.of("us-east-1"))
                                                                                     .useDualStack(true)
                                                                                     .useFips(false)
                                                                                     .build());

        errorCases.put("256: WriteGetObjectResponse with accelerate", S3EndpointParams.builder()
                                                                                      .accelerate(true)
                                                                                      .useObjectLambdaEndpoint(true)
                                                                                      .region(Region.of("us-east-1"))
                                                                                      .useDualStack(false)
                                                                                      .useFips(false)
                                                                                      .build());

        errorCases.put("257: WriteGetObjectResponse with fips in CN", S3EndpointParams.builder()
                                                                                      .accelerate(false)
                                                                                      .region(Region.of("cn-north-1"))
                                                                                      .useObjectLambdaEndpoint(true)
                                                                                      .useDualStack(false)
                                                                                      .useFips(true)
                                                                                      .build());

        errorCases.put("258: WriteGetObjectResponse with invalid partition", S3EndpointParams.builder()
                                                                                             .accelerate(false)
                                                                                             .useObjectLambdaEndpoint(true)
                                                                                             .region(Region.of("not a valid DNS name"))
                                                                                             .useDualStack(false)
                                                                                             .useFips(false)
                                                                                             .build());

        errorCases.put("266: S3 Outposts bucketAlias - No endpoint set for beta", S3EndpointParams.builder()
                                                                                                  .region(Region.of("us-east-1"))
                                                                                                  .bucket("test-accessp-o0b1d075431d83bebde8xz5w8ijx1qzlbp3i3kbeta0--op-s3")
                                                                                                  .useFips(false)
                                                                                                  .useDualStack(false)
                                                                                                  .accelerate(false)
                                                                                                  .build());

        errorCases.put("267: S3 Outposts bucketAlias Invalid hardware type", S3EndpointParams.builder()
                                                                                             .region(Region.of("us-east-1"))
                                                                                             .bucket("test-accessp-h0000075431d83bebde8xz5w8ijx1qzlbp3i3kuse10--op-s3")
                                                                                             .useFips(false)
                                                                                             .useDualStack(false)
                                                                                             .accelerate(false)
                                                                                             .build());

        errorCases.put("268: S3 Outposts bucketAlias Special character in Outpost Arn", S3EndpointParams.builder()
                                                                                                        .region(Region.of("us-east-1"))
                                                                                                        .bucket("test-accessp-o00000754%1d83bebde8xz5w8ijx1qzlbp3i3kuse10--op-s3")
                                                                                                        .useFips(false)
                                                                                                        .useDualStack(false)
                                                                                                        .accelerate(false)
                                                                                                        .build());

        errorCases.put("269: S3 Outposts bucketAlias - No endpoint set for beta", S3EndpointParams.builder()
                                                                                                  .region(Region.of("us-east-1"))
                                                                                                  .bucket("test-accessp-e0b1d075431d83bebde8xz5w8ijx1qzlbp3i3ebeta0--op-s3")
                                                                                                  .useFips(false)
                                                                                                  .useDualStack(false)
                                                                                                  .accelerate(false)
                                                                                                  .build());

        errorCases.put("285: Data Plane with short zone fips china region", S3EndpointParams.builder()
                                                                                            .region(Region.of("cn-north-1"))
                                                                                            .bucket("mybucket--test-ab1--x-s3")
                                                                                            .useFips(true)
                                                                                            .useDualStack(false)
                                                                                            .accelerate(false)
                                                                                            .useS3ExpressControlEndpoint(false)
                                                                                            .build());

        errorCases.put("287: Data Plane with short zone fips with AP china region", S3EndpointParams.builder()
                                                                                                    .region(Region.of("cn-north-1"))
                                                                                                    .bucket("myaccesspoint--test-ab1--xa-s3")
                                                                                                    .useFips(true)
                                                                                                    .useDualStack(false)
                                                                                                    .accelerate(false)
                                                                                                    .useS3ExpressControlEndpoint(false)
                                                                                                    .build());

        errorCases.put("301: Control plane with short AZ bucket and fips china region", S3EndpointParams.builder()
                                                                                                        .region(Region.of("cn-north-1"))
                                                                                                        .bucket("mybucket--test-ab1--x-s3")
                                                                                                        .useFips(true)
                                                                                                        .useDualStack(false)
                                                                                                        .accelerate(false)
                                                                                                        .useS3ExpressControlEndpoint(true)
                                                                                                        .disableS3ExpressSessionAuth(false)
                                                                                                        .build());

        errorCases.put("333: bad format error", S3EndpointParams.builder()
                                                                .region(Region.of("us-east-1"))
                                                                .bucket("mybucket--usaz1--x-s3")
                                                                .useFips(false)
                                                                .useDualStack(false)
                                                                .accelerate(false)
                                                                .useS3ExpressControlEndpoint(false)
                                                                .build());

        errorCases.put("334: bad AP format error", S3EndpointParams.builder()
                                                                   .region(Region.of("us-east-1"))
                                                                   .bucket("myaccesspoint--usaz1--xa-s3")
                                                                   .useFips(false)
                                                                   .useDualStack(false)
                                                                   .accelerate(false)
                                                                   .useS3ExpressControlEndpoint(false)
                                                                   .build());

        errorCases.put("335: bad format error no session auth", S3EndpointParams.builder()
                                                                                .region(Region.of("us-east-1"))
                                                                                .bucket("mybucket--usaz1--x-s3")
                                                                                .useFips(false)
                                                                                .useDualStack(false)
                                                                                .accelerate(false)
                                                                                .useS3ExpressControlEndpoint(false)
                                                                                .disableS3ExpressSessionAuth(true)
                                                                                .build());

        errorCases.put("336: bad AP format error no session auth", S3EndpointParams.builder()
                                                                                   .region(Region.of("us-east-1"))
                                                                                   .bucket("myaccesspoint--usaz1--xa-s3")
                                                                                   .useFips(false)
                                                                                   .useDualStack(false)
                                                                                   .accelerate(false)
                                                                                   .useS3ExpressControlEndpoint(false)
                                                                                   .disableS3ExpressSessionAuth(true)
                                                                                   .build());

        errorCases.put("337: accelerate error", S3EndpointParams.builder()
                                                                .region(Region.of("us-east-1"))
                                                                .bucket("mybucket--test-ab1--x-s3")
                                                                .useFips(false)
                                                                .useDualStack(false)
                                                                .accelerate(true)
                                                                .useS3ExpressControlEndpoint(false)
                                                                .build());

        errorCases.put("338: accelerate error with AP", S3EndpointParams.builder()
                                                                        .region(Region.of("us-east-1"))
                                                                        .bucket("myaccesspoint--test-ab1--xa-s3")
                                                                        .useFips(false)
                                                                        .useDualStack(false)
                                                                        .accelerate(true)
                                                                        .useS3ExpressControlEndpoint(false)
                                                                        .build());

        errorCases.put("339: Data plane bucket format error", S3EndpointParams.builder()
                                                                              .region(Region.of("us-east-1"))
                                                                              .bucket("my.bucket--test-ab1--x-s3")
                                                                              .useFips(false)
                                                                              .useDualStack(false)
                                                                              .accelerate(false)
                                                                              .useS3ExpressControlEndpoint(false)
                                                                              .build());

        errorCases.put("340: Data plane AP format error", S3EndpointParams.builder()
                                                                          .region(Region.of("us-east-1"))
                                                                          .bucket("my.myaccesspoint--test-ab1--xa-s3")
                                                                          .useFips(false)
                                                                          .useDualStack(false)
                                                                          .accelerate(false)
                                                                          .useS3ExpressControlEndpoint(false)
                                                                          .build());

        errorCases.put("341: host override data plane bucket error session auth", S3EndpointParams.builder()
                                                                                                  .region(Region.of("us-west-2"))
                                                                                                  .bucket("my.bucket--usw2-az1--x-s3")
                                                                                                  .useFips(false)
                                                                                                  .useDualStack(false)
                                                                                                  .accelerate(false)
                                                                                                  .endpoint("https://custom.com")
                                                                                                  .build());

        errorCases.put("342: host override data plane AP error session auth", S3EndpointParams.builder()
                                                                                              .region(Region.of("us-west-2"))
                                                                                              .bucket("my.myaccesspoint--usw2-az1--xa-s3")
                                                                                              .useFips(false)
                                                                                              .useDualStack(false)
                                                                                              .accelerate(false)
                                                                                              .endpoint("https://custom.com")
                                                                                              .build());

        errorCases.put("343: host override data plane bucket error", S3EndpointParams.builder()
                                                                                     .region(Region.of("us-west-2"))
                                                                                     .bucket("my.bucket--usw2-az1--x-s3")
                                                                                     .useFips(false)
                                                                                     .useDualStack(false)
                                                                                     .accelerate(false)
                                                                                     .endpoint("https://custom.com")
                                                                                     .disableS3ExpressSessionAuth(true)
                                                                                     .build());

        errorCases.put("344: host override data plane AP error", S3EndpointParams.builder()
                                                                                 .region(Region.of("us-west-2"))
                                                                                 .bucket("my.myaccesspoint--usw2-az1--xa-s3")
                                                                                 .useFips(false)
                                                                                 .useDualStack(false)
                                                                                 .accelerate(false)
                                                                                 .endpoint("https://custom.com")
                                                                                 .disableS3ExpressSessionAuth(true)
                                                                                 .build());

    }


    // @Benchmark
    // public void bddBoxedBooleanAndLambdas(Blackhole blackhole) {
    //     runTest(blackhole, optimizedBddRuntime2);
    // }

    // @Benchmark
    // public void bddNoBoxingBooleanAndMethodReferences(Blackhole blackhole) {
    //     runTest(blackhole, bddRuntime4);
    // }


    // @Benchmark
    // public void optimizedBddRuntimeInline(Blackhole blackhole) {
    //     runTest(blackhole, optimizedBddRuntimeInline);
    // }

    // @Benchmark
    // public void optimizedBddRuntimeMethodHandleArray(Blackhole blackhole) {
    //     runTest(blackhole, optimizedBddRuntimeMethodHandleArray);
    // }
    //
    // @Benchmark
    // public void optimizedBddRuntimeWithSwitches(Blackhole blackhole) {
    //     runTest(blackhole, optimizedBddRuntimeWithSwitches);
    // }

    // @Benchmark
    // public void rulesResolverWithCache(Blackhole blackhole) {
    //     runTest(blackhole, rulesProviderWithCache);
    // }
    //
    // @Benchmark
    // public void bddRuntimeDagWithCache(Blackhole blackhole) {
    //     runTest(blackhole, bddWithCache);
    // }
    //
    // @Benchmark
    // public void bddRuntimeSwitchesWithCache(Blackhole blackhole) {
    //     runTest(blackhole, bddRuntime3WithCache);
    // }
    //
    // @Benchmark
    // public void optimizedBddRuntimeDagWithCache(Blackhole blackhole) {
    //     runTest(blackhole, optimizedBddRuntime2WithCache);
    // }

    // @Benchmark
    // public void bddInlineWithCache(Blackhole blackhole) {
    //     runTest(blackhole, bddInlineWithCache);
    // }
}
