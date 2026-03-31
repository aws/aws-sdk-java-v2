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

import java.net.URI;
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
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.endpoints.LambdaEndpointParams;
import software.amazon.awssdk.services.lambda.endpoints.LambdaEndpointProvider;
import software.amazon.awssdk.services.lambda.endpoints.internal.LambdaResolveEndpointInterceptor;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * JMH benchmark for Lambda endpoint resolution through the standard pipeline.
 * <p>
 * Test cases (by index):
 * <ul>
 *   <li>0 - For region us-east-1 with FIPS disabled and DualStack disabled</li>
 *   <li>1 - For region us-gov-east-1 with FIPS enabled and DualStack enabled</li>
 * </ul>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class LambdaEndpointResolverBenchmark {

    private static final ClientEndpointProvider DEFAULT_ENDPOINT_PROVIDER =
        ClientEndpointProvider.create(URI.create("https://lambda.us-east-1.amazonaws.com"), false);

    @Param({"0", "1"})
    private int testCaseIndex;

    private LambdaEndpointProvider provider;
    private SdkRequest request;
    private ExecutionAttributes executionAttributes;

    @Setup(Level.Trial)
    public void setup() {
        provider = LambdaEndpointProvider.defaultProvider();
        request = ListFunctionsRequest.builder().build();

        switch (testCaseIndex) {
            case 0: // For region us-east-1 with FIPS disabled and DualStack disabled
                setupTestCase(Region.US_EAST_1, false, false);
                break;
            case 1: // For region us-gov-east-1 with FIPS enabled and DualStack enabled
                setupTestCase(Region.US_GOV_EAST_1, true, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown test case index: " + testCaseIndex);
        }
    }

    @Benchmark
    public void resolveEndpoint(Blackhole blackhole) {
        LambdaEndpointParams params = LambdaResolveEndpointInterceptor.ruleParams(request, executionAttributes);
        blackhole.consume(provider.resolveEndpoint(params).join());
    }

    private void setupTestCase(Region region, boolean fips, boolean dualStack) {
        executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION, region);
        executionAttributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fips);
        executionAttributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, dualStack);
        executionAttributes.putAttribute(AwsExecutionAttribute.OPERATION_NAME, "ListFunctions");
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, AttributeMap.empty());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, DEFAULT_ENDPOINT_PROVIDER);
    }
}
