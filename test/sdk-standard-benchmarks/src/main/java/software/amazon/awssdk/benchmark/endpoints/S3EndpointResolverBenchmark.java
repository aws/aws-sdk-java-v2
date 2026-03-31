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
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.S3ResolveEndpointInterceptor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * JMH benchmark for S3 endpoint resolution through the standard pipeline.
 * <p>
 * Test cases (by index):
 * <ul>
 *   <li>0 - vanilla virtual addressing@us-west-2</li>
 *   <li>1 - vanilla path style@us-west-2</li>
 *   <li>2 - Data Plane with short zone name</li>
 *   <li>3 - vanilla access point arn@us-west-2</li>
 *   <li>4 - S3 outposts vanilla test</li>
 * </ul>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(4)
public class S3EndpointResolverBenchmark {

    private static final ClientEndpointProvider DEFAULT_ENDPOINT_PROVIDER =
        ClientEndpointProvider.create(URI.create("https://s3.amazonaws.com"), false);

    @Param({"0", "1", "2", "3", "4"})
    private int testCaseIndex;

    private S3EndpointProvider provider;
    private SdkRequest request;
    private ExecutionAttributes executionAttributes;

    @Setup(Level.Trial)
    public void setup() {
        provider = S3EndpointProvider.defaultProvider();

        switch (testCaseIndex) {
            case 0: // vanilla virtual addressing@us-west-2
                setupTestCase("bucket-name", Region.US_WEST_2, false, false,
                              AttributeMap.builder()
                                          .put(S3ClientContextParams.FORCE_PATH_STYLE, false)
                                          .put(S3ClientContextParams.ACCELERATE, false)
                                          .build());
                break;
            case 1: // vanilla path style@us-west-2
                setupTestCase("bucket-name", Region.US_WEST_2, false, false,
                              AttributeMap.builder()
                                          .put(S3ClientContextParams.FORCE_PATH_STYLE, true)
                                          .put(S3ClientContextParams.ACCELERATE, false)
                                          .build());
                break;
            case 2: // Data Plane with short zone name
                setupTestCase("mybucket--abcd-ab1--x-s3", Region.US_EAST_1, false, false,
                              AttributeMap.builder()
                                          .put(S3ClientContextParams.ACCELERATE, false)
                                          .put(S3ClientContextParams.DISABLE_S3_EXPRESS_SESSION_AUTH, false)
                                          .build());
                break;
            case 3: // vanilla access point arn@us-west-2
                setupTestCase("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint",
                              Region.US_WEST_2, false, false,
                              AttributeMap.builder()
                                          .put(S3ClientContextParams.FORCE_PATH_STYLE, false)
                                          .put(S3ClientContextParams.ACCELERATE, false)
                                          .build());
                break;
            case 4: // S3 outposts vanilla test
                setupTestCase("arn:aws:s3-outposts:us-west-2:123456789012:outpost/op-01234567890123456/accesspoint/reports",
                              Region.US_WEST_2, false, false,
                              AttributeMap.builder()
                                          .put(S3ClientContextParams.ACCELERATE, false)
                                          .build());
                break;
            default:
                throw new IllegalArgumentException("Unknown test case index: " + testCaseIndex);
        }
    }

    @Benchmark
    public void resolveEndpoint(Blackhole blackhole) {
        S3EndpointParams params = S3ResolveEndpointInterceptor.ruleParams(request, executionAttributes);
        blackhole.consume(provider.resolveEndpoint(params).join());
    }

    private void setupTestCase(String bucket, Region region, boolean fips, boolean dualStack,
                               AttributeMap clientContextParams) {
        request = GetObjectRequest.builder()
                                  .bucket(bucket)
                                  .key("key")
                                  .build();

        executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION, region);
        executionAttributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fips);
        executionAttributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, dualStack);
        executionAttributes.putAttribute(AwsExecutionAttribute.OPERATION_NAME, "GetObject");
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, clientContextParams);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, DEFAULT_ENDPOINT_PROVIDER);
    }
}
