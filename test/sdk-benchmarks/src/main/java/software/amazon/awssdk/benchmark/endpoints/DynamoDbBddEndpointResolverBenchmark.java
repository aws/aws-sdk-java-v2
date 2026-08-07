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
import java.util.Arrays;
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
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.BaselineBddEndpointProvider;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.BaselineRulesEndpointResolver;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.DefaultDynamoDbEndpointProvider;

/**
 * Compares DynamoDB endpoint resolution across three resolver implementations:
 * <ol>
 *   <li>{@link BaselineRulesEndpointResolver} - the classic rules-based resolver (generated without BDD)</li>
 *   <li>{@link BaselineBddEndpointProvider} - the original table-driven BDD resolver (while-loop traversal)</li>
 *   <li>{@link DefaultDynamoDbEndpointProvider} - the optimized BDD resolver (direct control-flow, inlined if-branches)</li>
 * </ol>
 *
 * <p>Covers every non-error case from the generated {@code DynamoDbEndpointProviderTests}, which exercises account ID based
 * endpoints in addition to the standard regional, FIPS, dual-stack and custom endpoint cases.
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
public class DynamoDbBddEndpointResolverBenchmark {
    /**
     * Fixed seed so the per-iteration case ordering is reproducible across JVM runs.
     */
    private static final long SHUFFLE_SEED = 20260730L;

    private final DynamoDbEndpointProvider rulesBasedProvider = new BaselineRulesEndpointResolver();
    private final DynamoDbEndpointProvider baselineBddProvider = new BaselineBddEndpointProvider();
    private final DynamoDbEndpointProvider optimizedBddProvider = new DefaultDynamoDbEndpointProvider();

    private Map<String, DynamoDbEndpointParams> nonErrorCases;
    private List<DynamoDbEndpointParams> shuffledCases;
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

    private void runTest(Blackhole blackhole, DynamoDbEndpointProvider endpointProvider) {
        for (DynamoDbEndpointParams params : shuffledCases) {
            blackhole.consume(endpointProvider.resolveEndpoint(params).join());
        }
    }

    private void setupBenchmarkCases() {
        setupBenchmarkCases0();
        setupBenchmarkCases1();
        setupBenchmarkCases2();
        setupBenchmarkCases3();
        setupBenchmarkCases4();
    }

    private void setupBenchmarkCases0() {
        nonErrorCases.put(
            "0: For region af-south-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("af-south-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "1: For region ap-east-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "2: For region ap-northeast-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-northeast-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "3: For region ap-northeast-2 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-northeast-2"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "4: For region ap-northeast-3 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-northeast-3"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "5: For region ap-south-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-south-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "6: For region ap-southeast-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-southeast-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "7: For region ap-southeast-2 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-southeast-2"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "8: For region ap-southeast-3 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ap-southeast-3"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "9: For region ca-central-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ca-central-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "10: For region ca-central-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("ca-central-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "11: For region eu-central-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("eu-central-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "12: For region eu-north-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("eu-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "13: For region eu-south-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("eu-south-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "14: For region eu-west-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("eu-west-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "15: For region eu-west-2 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("eu-west-2"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "16: For region eu-west-3 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("eu-west-3"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "17: For region local with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "18: For region me-south-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("me-south-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "19: For region sa-east-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("sa-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "20: For region us-east-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "21: For region us-east-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "22: For region us-east-2 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-2"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "23: For region us-east-2 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-2"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "24: For region us-west-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-west-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "25: For region us-west-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-west-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "26: For region us-west-2 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-west-2"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "27: For region us-west-2 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-west-2"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "28: For region us-east-1 with FIPS enabled and DualStack enabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .build());
        nonErrorCases.put(
            "29: For region us-east-1 with FIPS disabled and DualStack enabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .build());
        nonErrorCases.put(
            "30: For region cn-north-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "31: For region cn-northwest-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-northwest-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "32: For region cn-north-1 with FIPS enabled and DualStack enabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .build());
        nonErrorCases.put(
            "33: For region cn-north-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "34: For region cn-north-1 with FIPS disabled and DualStack enabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .build());
        nonErrorCases.put(
            "35: For region us-gov-east-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "36: For region us-gov-east-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "37: For region us-gov-west-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-west-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "38: For region us-gov-west-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-west-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "39: For region us-gov-east-1 with FIPS enabled and DualStack enabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .build());
        nonErrorCases.put(
            "40: For region us-gov-east-1 with FIPS disabled and DualStack enabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .build());
        nonErrorCases.put(
            "41: For region us-iso-east-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "42: For region us-iso-west-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-west-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "43: For region us-iso-east-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "44: For region us-isob-east-1 with FIPS disabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-isob-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "45: For region us-isob-east-1 with FIPS enabled and DualStack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-isob-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .build());
        nonErrorCases.put(
            "46: For custom endpoint with region set and fips disabled and dualstack disabled",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .build());
        nonErrorCases.put(
            "47: For custom endpoint with region not set and fips disabled and dualstack disabled",
            DynamoDbEndpointParams.builder()
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .build());
        nonErrorCases.put(
            "54: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-east-...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "55: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:3333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "56: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:222222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "57: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:222222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "58: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:st...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "59: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:222222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "60: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:st...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "61: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=preferred, Region=us-east-1, Endpoint=...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "65: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "66: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "67: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Account...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "68: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=preferred, Region=us-east-1, Endpoin...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("")
                                  .build());
    }

    private void setupBenchmarkCases1() {
        nonErrorCases.put(
            "72: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "73: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Re...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "74: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=preferred, Region=us-east-1, Endpoint=https://exam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "78: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=disabled, Region=us-east-1...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "79: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:3333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "80: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:222222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "81: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:222222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "82: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:st...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "83: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:222222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "84: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:st...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "85: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=disabled, Region=us-east-1, Endpoint=h...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "89: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "90: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "91: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Account...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "92: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=disabled, Region=us-east-1, Endpoint...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "96: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "97: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Re...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "98: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=disabled, Region=us-east-1, Endpoint=https://examp...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "102: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=required, Region=us-east-...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "103: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "104: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "105: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "106: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "107: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "108: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "109: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=required, Region=us-east-1, Endpoint=...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountId("")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "113: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "114: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "115: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "116: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=required, Region=us-east-1, Endpoin...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "120: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "121: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "122: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=required, Region=us-east-1, Endpoint=https://exam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .endpoint("https://example.com")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "126: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "127: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "128: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "129: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "130: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "131: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "132: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "133: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=preferred, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "137: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "138: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "139: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "140: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=preferred, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "144: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "145: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "146: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=preferred, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "150: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=disabled, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "151: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "152: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "153: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "154: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "155: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "156: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "157: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=disabled, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "161: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "162: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "163: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "164: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=disabled, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("")
                                  .build());
    }

    private void setupBenchmarkCases2() {
        nonErrorCases.put(
            "168: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "169: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "170: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=disabled, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "174: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=required, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "175: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "176: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "177: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "178: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "179: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "180: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "181: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=required, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "185: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "186: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "187: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "188: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=required, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "192: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "193: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "194: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=required, Region=local}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("local"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "195: {UseFIPS=true, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "196: {UseFIPS=true, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "197: {UseFIPS=false, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "198: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "199: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "200: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "201: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "202: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "203: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "204: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "206: {UseFIPS=true, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Acc...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "207: {UseFIPS=true, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "208: {UseFIPS=false, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "209: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "210: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "211: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "212: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=preferred, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "213: {UseFIPS=true, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_name...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "214: {UseFIPS=true, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "215: {UseFIPS=false, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "216: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "217: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "218: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=preferred, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "221: {UseFIPS=false, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=required, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "222: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=required, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .build());
        nonErrorCases.put(
            "223: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "224: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "225: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "226: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "227: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "228: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "232: {UseFIPS=false, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "233: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "239: {UseFIPS=false, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "240: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "241: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("required")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "267: {UseFIPS=true, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "268: {UseFIPS=true, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "269: {UseFIPS=false, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "270: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "271: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "272: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
    }

    private void setupBenchmarkCases3() {
        nonErrorCases.put(
            "273: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "274: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "275: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "276: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "277: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "278: {UseFIPS=true, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Acc...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "279: {UseFIPS=true, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "280: {UseFIPS=false, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "281: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "282: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "283: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "284: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "285: {UseFIPS=true, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_name...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "286: {UseFIPS=true, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "287: {UseFIPS=false, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "288: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "289: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "290: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=disabled, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("disabled")
                                  .build());
        nonErrorCases.put(
            "291: {UseFIPS=true, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=cn-north-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "292: {UseFIPS=true, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=cn-north-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "293: {UseFIPS=false, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=cn-north-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "294: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=cn-nort...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "295: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "296: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "297: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "298: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "299: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "300: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "301: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=preferred, Region=cn-north-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "302: {UseFIPS=true, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Acc...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "303: {UseFIPS=true, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "304: {UseFIPS=false, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "305: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "306: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "307: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "308: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=preferred, Region=cn-north-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "309: {UseFIPS=true, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_name...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "310: {UseFIPS=true, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "311: {UseFIPS=false, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "312: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "313: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "314: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=preferred, Region=cn-north-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("cn-north-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "315: {UseFIPS=true, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-iso-ea...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "316: {UseFIPS=true, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-iso-e...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "317: {UseFIPS=false, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-iso-e...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "318: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-iso-...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "319: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "320: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "321: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "322: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "323: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "324: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "325: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=preferred, Region=us-iso-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "326: {UseFIPS=true, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Acc...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "327: {UseFIPS=true, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "328: {UseFIPS=false, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "329: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "330: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "331: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "332: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=preferred, Region=us-iso-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("")
                                  .build());
    }

    private void setupBenchmarkCases4() {
        nonErrorCases.put(
            "333: {UseFIPS=true, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_name...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "334: {UseFIPS=true, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "335: {UseFIPS=false, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "336: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "337: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "338: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=preferred, Region=us-iso-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-iso-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "339: {UseFIPS=true, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-gov-ea...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "340: {UseFIPS=true, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-gov-e...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "341: {UseFIPS=false, UseDualStack=true, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-gov-e...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "342: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, AccountIdEndpointMode=preferred, Region=us-gov-...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "343: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArnList=[arn:aws:dynamodb:us-east-1:333...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "344: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-east-1:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "345: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "346: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "347: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:dynamodb:us-west-2:22222222...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-west-2:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "348: {UseFIPS=false, UseDualStack=false, AccountId=111111111111, ResourceArn=arn:aws:s3:us-west-2:222222222222:s...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("111111111111")
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .resourceArnList(Arrays.asList("arn:aws:s3:us-east-1:333333333333:stream/testStream"))
                                  .build());
        nonErrorCases.put(
            "349: {UseFIPS=false, UseDualStack=false, AccountId=, AccountIdEndpointMode=preferred, Region=us-gov-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountId("")
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "350: {UseFIPS=true, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Acc...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "351: {UseFIPS=true, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "352: {UseFIPS=false, UseDualStack=true, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, Ac...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "353: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "354: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-west-2:222222222222:table/table_name, A...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-west-2:222222222222:table/table_name")
                                  .build());
        nonErrorCases.put(
            "355: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:s3:us-west-2:222222222222:stream/testStream, Accoun...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:s3:us-west-2:222222222222:stream/testStream")
                                  .build());
        nonErrorCases.put(
            "356: {UseFIPS=false, UseDualStack=false, ResourceArn=, AccountIdEndpointMode=preferred, Region=us-gov-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("")
                                  .build());
        nonErrorCases.put(
            "357: {UseFIPS=true, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_name...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "358: {UseFIPS=true, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(true)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "359: {UseFIPS=false, UseDualStack=true, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_nam...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(true)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "360: {UseFIPS=false, UseDualStack=false, ResourceArnList=[arn:aws:dynamodb:us-east-1:333333333333:table/table_na...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "361: {UseFIPS=false, UseDualStack=false, ResourceArn=arn:aws:dynamodb:us-east-1:222222222222:table/table_name, R...",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .resourceArn("arn:aws:dynamodb:us-east-1:222222222222:table/table_name")
                                  .resourceArnList(Arrays.asList("arn:aws:dynamodb:us-east-1:333333333333:table/table_name"))
                                  .build());
        nonErrorCases.put(
            "362: {UseFIPS=false, UseDualStack=false, AccountIdEndpointMode=preferred, Region=us-gov-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-gov-east-1"))
                                  .useFips(false)
                                  .useDualStack(false)
                                  .accountIdEndpointMode("preferred")
                                  .build());
        nonErrorCases.put(
            "366: {Endpoint=https://111111111111.ddb.us-east-1.api.aws, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .endpoint("https://111111111111.ddb.us-east-1.api.aws")
                                  .build());
        nonErrorCases.put(
            "367: {Endpoint=https://vpce-1a2b3c4d-5e6f.dynamodb.us-east-1.vpce.api.aws, Region=us-east-1}",
            DynamoDbEndpointParams.builder()
                                  .region(Region.of("us-east-1"))
                                  .endpoint("https://vpce-1a2b3c4d-5e6f.dynamodb.us-east-1.vpce.api.aws")
                                  .build());
    }
}
