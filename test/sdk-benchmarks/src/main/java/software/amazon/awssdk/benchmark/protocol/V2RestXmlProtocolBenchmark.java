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

package software.amazon.awssdk.benchmark.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
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
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlGenerator;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.internal.unmarshall.XmlProtocolUnmarshaller;
import software.amazon.awssdk.services.cloudfront.model.Aliases;
import software.amazon.awssdk.services.cloudfront.model.AllowedMethods;
import software.amazon.awssdk.services.cloudfront.model.CachedMethods;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.Method;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.ViewerProtocolPolicy;

/**
 * Isolated ser/de benchmark for V2 CloudFront (REST-XML protocol).
 * Measures only XML parsing + object construction -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V2RestXmlProtocolBenchmark {

    private static final String XMLNS = "http://cloudfront.amazonaws.com/doc/2020-05-31/";
    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .requestUri("/2020-05-31/distribution")
        .httpMethod(SdkHttpMethod.POST)
        .hasExplicitPayloadMember(true)
        .hasPayloadMembers(true)
        .putAdditionalMetadata(AwsXmlProtocolFactory.ROOT_MARSHALL_LOCATION_ATTRIBUTE, null)
        .putAdditionalMetadata(AwsXmlProtocolFactory.XML_NAMESPACE_ATTRIBUTE, XMLNS)
        .build();

    private XmlProtocolUnmarshaller unmarshaller;
    private byte[] responseBytes;
    private CreateDistributionRequest request;

    @Setup
    public void setup() throws Exception {
        unmarshaller = XmlProtocolUnmarshaller.create();
        responseBytes = loadFixture("fixtures/rest-xml-protocol/create-distribution-response.xml");
        request = createRequest();
    }

    @Benchmark
    public void createDistributionDeser(Blackhole bh) {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .putHeader("ETag", "E2QWRUHEXAMPLE")
            .putHeader("Location", "https://cloudfront.amazonaws.com/2020-05-31/distribution/EDFDVBD6EXAMPLE")
            .content(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)))
            .build();
        bh.consume(unmarshaller.unmarshall(CreateDistributionResponse.builder(), response));
    }

    @Benchmark
    public void createDistributionSer(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = XmlProtocolMarshaller.builder()
            .endpoint(ENDPOINT)
            .xmlGenerator(XmlGenerator.create(XMLNS, false))
            .operationInfo(OP_INFO)
            .build();
        bh.consume(marshaller.marshall(request));
    }

    private static CreateDistributionRequest createRequest() {
        return CreateDistributionRequest.builder()
            .distributionConfig(DistributionConfig.builder()
                .callerReference("benchmark-ref-2024")
                .aliases(Aliases.builder()
                    .quantity(2)
                    .items("www.example.com", "cdn.example.com")
                    .build())
                .defaultRootObject("index.html")
                .origins(Origins.builder()
                    .quantity(1)
                    .items(Origin.builder()
                        .id("myS3Origin")
                        .domainName("mybucket.s3.amazonaws.com")
                        .originPath("/production")
                        .s3OriginConfig(S3OriginConfig.builder()
                            .originAccessIdentity(
                                "origin-access-identity/cloudfront/E127EXAMPLE51Z")
                            .build())
                        .build())
                    .build())
                .defaultCacheBehavior(DefaultCacheBehavior.builder()
                    .targetOriginId("myS3Origin")
                    .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                    .allowedMethods(AllowedMethods.builder()
                        .quantity(3)
                        .items(Method.GET, Method.HEAD, Method.OPTIONS)
                        .cachedMethods(CachedMethods.builder()
                            .quantity(2)
                            .items(Method.GET, Method.HEAD)
                            .build())
                        .build())
                    .compress(true)
                    .build())
                .build())
            .build();
    }

    private static byte[] loadFixture(String path) throws IOException {
        return software.amazon.awssdk.utils.IoUtils.toByteArray(
            V2RestXmlProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
