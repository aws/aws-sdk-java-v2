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

package software.amazon.awssdk.benchmark.apicall.protocol;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.Aliases;
import com.amazonaws.services.cloudfront.model.AllowedMethods;
import com.amazonaws.services.cloudfront.model.CachedMethods;
import com.amazonaws.services.cloudfront.model.CreateDistributionRequest;
import com.amazonaws.services.cloudfront.model.DefaultCacheBehavior;
import com.amazonaws.services.cloudfront.model.DistributionConfig;
import com.amazonaws.services.cloudfront.model.Method;
import com.amazonaws.services.cloudfront.model.Origin;
import com.amazonaws.services.cloudfront.model.Origins;
import com.amazonaws.services.cloudfront.model.S3OriginConfig;
import com.amazonaws.services.cloudfront.model.ViewerProtocolPolicy;
import java.util.Collections;
import java.util.Map;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * V1 roundtrip benchmark for REST-XML protocol using CloudFront CreateDistribution via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class V1RestXmlRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private AmazonCloudFront client;
    private CreateDistributionRequest request;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = ProtocolRoundtripServer.loadFixture("rest-xml-protocol/create-distribution-response.xml");

        Map<String, String> headers = Collections.singletonMap("ETag", "E2QWRUHEXAMPLE");
        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet()
            .routeByUri("/2020-05-31/distribution", "application/xml", response, headers);

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = AmazonCloudFrontClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                server.getHttpUri().toString(), "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
            .build();

        request = new CreateDistributionRequest()
            .withDistributionConfig(new DistributionConfig()
                .withCallerReference("benchmark-ref-2024")
                .withAliases(new Aliases()
                    .withQuantity(2)
                    .withItems("www.example.com", "cdn.example.com"))
                .withDefaultRootObject("index.html")
                .withOrigins(new Origins()
                    .withQuantity(1)
                    .withItems(new Origin()
                        .withId("myS3Origin")
                        .withDomainName("mybucket.s3.amazonaws.com")
                        .withOriginPath("/production")
                        .withS3OriginConfig(new S3OriginConfig()
                            .withOriginAccessIdentity("origin-access-identity/cloudfront/E127EXAMPLE51Z"))))
                .withDefaultCacheBehavior(new DefaultCacheBehavior()
                    .withTargetOriginId("myS3Origin")
                    .withViewerProtocolPolicy(ViewerProtocolPolicy.RedirectToHttps)
                    .withAllowedMethods(new AllowedMethods()
                        .withQuantity(3)
                        .withItems(Method.GET, Method.HEAD, Method.OPTIONS)
                        .withCachedMethods(new CachedMethods()
                            .withQuantity(2)
                            .withItems(Method.GET, Method.HEAD)))
                    .withCompress(true)));
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.shutdown();
        server.stop();
    }

    @Benchmark
    public void createDistribution(Blackhole bh) {
        bh.consume(client.createDistribution(request));
    }
}
