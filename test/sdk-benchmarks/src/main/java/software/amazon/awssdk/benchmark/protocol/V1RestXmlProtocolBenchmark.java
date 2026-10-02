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

import com.amazonaws.http.HttpResponse;
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
import com.amazonaws.services.cloudfront.model.transform.CreateDistributionRequestMarshaller;
import com.amazonaws.services.cloudfront.model.transform.CreateDistributionResultStaxUnmarshaller;
import com.amazonaws.transform.StaxUnmarshallerContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V1RestXmlProtocolBenchmark {

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private byte[] responseBytes;
    private Map<String, String> responseHeaders;
    private CreateDistributionRequestMarshaller marshaller;
    private CreateDistributionRequest request;

    @Setup
    public void setup() throws Exception {
        responseBytes = loadFixture("fixtures/rest-xml-protocol/create-distribution-response.xml");

        responseHeaders = new HashMap<>();
        responseHeaders.put("ETag", "E2QWRUHEXAMPLE");
        responseHeaders.put("Location", "https://cloudfront.amazonaws.com/2020-05-31/distribution/EDFDVBD6EXAMPLE");

        HttpResponse httpResponse = new HttpResponse(null, null);
        httpResponse.setStatusCode(200);
        for (Map.Entry<String, String> e : responseHeaders.entrySet()) {
            httpResponse.addHeader(e.getKey(), e.getValue());
        }

        marshaller = new CreateDistributionRequestMarshaller();
        request = createRequest();
    }

    @Benchmark
    public void createDistributionDeser(Blackhole bh) throws Exception {
        XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(new ByteArrayInputStream(responseBytes));
        StaxUnmarshallerContext ctx = new StaxUnmarshallerContext(
            reader, responseHeaders);
        ctx.registerMetadataExpression("ResponseMetadata/RequestId", 2, "AWS_REQUEST_ID");
        bh.consume(CreateDistributionResultStaxUnmarshaller.getInstance().unmarshall(ctx));
    }

    @Benchmark
    public void createDistributionSer(Blackhole bh) {
        bh.consume(marshaller.marshall(request));
    }

    private static CreateDistributionRequest createRequest() {
        return new CreateDistributionRequest()
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
                                                    .withOriginAccessIdentity(
                                                        "origin-access-identity/cloudfront/E127EXAMPLE51Z"))))
                                        .withDefaultCacheBehavior(new DefaultCacheBehavior()
                                            .withTargetOriginId("myS3Origin")
                                            .withViewerProtocolPolicy(
                                                ViewerProtocolPolicy.RedirectToHttps)
                                            .withAllowedMethods(new AllowedMethods()
                                                .withQuantity(3)
                                                .withItems(Method.GET, Method.HEAD, Method.OPTIONS)
                                                .withCachedMethods(new CachedMethods()
                                                    .withQuantity(2)
                                                    .withItems(Method.GET, Method.HEAD)))
                                            .withCompress(true)));
    }

    private static byte[] loadFixture(String path) throws IOException {
        return com.amazonaws.util.IOUtils.toByteArray(
            V1RestXmlProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
