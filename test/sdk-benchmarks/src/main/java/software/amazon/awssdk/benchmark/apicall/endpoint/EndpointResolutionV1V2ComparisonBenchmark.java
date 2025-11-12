/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.apicall.endpoint;

import com.amazonaws.internal.DefaultServiceEndpointBuilder;
import com.amazonaws.regions.Regions;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.services.lambda.endpoints.LambdaEndpointParams;
import software.amazon.awssdk.services.lambda.endpoints.internal.DefaultLambdaEndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;

/**
 * Benchmark comparing endpoint resolution between v1 and v2 SDKs.
 * Tests both simple (Lambda - 10 rules) and complex (S3 - 131 rules) services.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Fork(2)
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class EndpointResolutionV1V2ComparisonBenchmark {

    private com.amazonaws.regions.Region v1Region;
    private DefaultLambdaEndpointProvider v2LambdaProvider;
    private LambdaEndpointParams v2LambdaParams;
    private DefaultS3EndpointProvider v2S3Provider;
    private S3EndpointParams v2S3Params;

    @Setup
    public void setup() {
        v1Region = com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1);

        v2LambdaProvider = new DefaultLambdaEndpointProvider();
        v2LambdaParams = LambdaEndpointParams.builder()
                                             .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                                             .build();

        v2S3Provider = new DefaultS3EndpointProvider();
        v2S3Params = S3EndpointParams.builder()
                                     .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                                     .build();
    }

    @Benchmark
    public void v1LambdaEndpointResolution(Blackhole blackhole) {
        DefaultServiceEndpointBuilder builder = new DefaultServiceEndpointBuilder("lambda", "https");
        builder.withRegion(v1Region);
        blackhole.consume(builder.getServiceEndpoint());
    }

    @Benchmark
    public void v2LambdaEndpointResolution(Blackhole blackhole) {
        blackhole.consume(v2LambdaProvider.resolveEndpoint(v2LambdaParams).join());
    }

    @Benchmark
    public void v1S3EndpointResolution(Blackhole blackhole) {
        DefaultServiceEndpointBuilder builder = new DefaultServiceEndpointBuilder("s3", "https");
        builder.withRegion(v1Region);
        blackhole.consume(builder.getServiceEndpoint());
    }

    @Benchmark
    public void v2S3EndpointResolution(Blackhole blackhole) {
        blackhole.consume(v2S3Provider.resolveEndpoint(v2S3Params).join());
    }
}
