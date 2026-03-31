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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.Aliases;
import software.amazon.awssdk.services.cloudfront.model.AllowedMethods;
import software.amazon.awssdk.services.cloudfront.model.CachedMethods;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.Method;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.ViewerProtocolPolicy;

/**
 * Roundtrip benchmark for REST-XML protocol using CloudFront CreateDistribution via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class V2RestXmlRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private CloudFrontClient client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = ProtocolRoundtripServer.loadFixture("rest-xml-protocol/create-distribution-response.xml");

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet(response, "text/xml");

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = CloudFrontClient.builder()
            .endpointOverride(server.getHttpUri())
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")))
            .httpClient(Apache5HttpClient.create())
            .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.close();
        server.stop();
    }

    @Benchmark
    public void createDistribution(Blackhole bh) {
        S3OriginConfig s3Config = S3OriginConfig.builder()
            .originAccessIdentity(
                "origin-access-identity/cloudfront/E127EXAMPLE51Z")
            .build();

        Origin origin = Origin.builder()
            .id("myS3Origin")
            .domainName("mybucket.s3.amazonaws.com")
            .originPath("/production")
            .s3OriginConfig(s3Config)
            .build();

        CachedMethods cached = CachedMethods.builder()
            .quantity(2)
            .items(Method.GET, Method.HEAD)
            .build();

        AllowedMethods allowed = AllowedMethods.builder()
            .quantity(3)
            .items(Method.GET, Method.HEAD, Method.OPTIONS)
            .cachedMethods(cached)
            .build();

        DefaultCacheBehavior cacheBehavior =
            DefaultCacheBehavior.builder()
                .targetOriginId("myS3Origin")
                .viewerProtocolPolicy(
                    ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .allowedMethods(allowed)
                .compress(true)
                .build();

        DistributionConfig config = DistributionConfig.builder()
            .callerReference("benchmark-ref-2024")
            .aliases(Aliases.builder()
                .quantity(2)
                .items("www.example.com", "cdn.example.com")
                .build())
            .defaultRootObject("index.html")
            .origins(Origins.builder()
                .quantity(1)
                .items(origin)
                .build())
            .defaultCacheBehavior(cacheBehavior)
            .build();

        CreateDistributionRequest request =
            CreateDistributionRequest.builder()
                .distributionConfig(config)
                .build();

        bh.consume(client.createDistribution(request));
    }
}
