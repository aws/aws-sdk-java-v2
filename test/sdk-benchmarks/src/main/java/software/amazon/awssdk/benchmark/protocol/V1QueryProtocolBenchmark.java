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

import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.transform.AssumeRoleRequestMarshaller;
import com.amazonaws.services.securitytoken.model.transform.AssumeRoleResultStaxUnmarshaller;
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

/**
 * Isolated ser/de benchmark for V1 STS (Query protocol).
 * Measures only XML parsing + form encoding -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V1QueryProtocolBenchmark {

    private static final XMLInputFactory XML_INPUT_FACTORY =
        XMLInputFactory.newInstance();

    private byte[] responseBytes;
    private Map<String, String> responseHeaders;
    private AssumeRoleRequestMarshaller marshaller;
    private AssumeRoleRequest request;

    @Setup
    public void setup() throws Exception {
        responseBytes = loadFixture("fixtures/query-protocol/assumerole-response.xml");
        responseHeaders = new HashMap<>();
        marshaller = new AssumeRoleRequestMarshaller();
        request = createRequest();
    }

    @Benchmark
    public void assumeRoleDeser(Blackhole bh) throws Exception {
        XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(new ByteArrayInputStream(responseBytes));
        StaxUnmarshallerContext ctx = new StaxUnmarshallerContext(reader, responseHeaders);
        ctx.registerMetadataExpression("ResponseMetadata/RequestId", 2, "AWS_REQUEST_ID");

        bh.consume(AssumeRoleResultStaxUnmarshaller.getInstance().unmarshall(ctx));
    }

    @Benchmark
    public void assumeRoleSer(Blackhole bh) {
        bh.consume(marshaller.marshall(request));
    }

    private static AssumeRoleRequest createRequest() {
        return new AssumeRoleRequest()
            .withRoleArn("arn:aws:iam::123456789012:role/benchmark-role")
            .withRoleSessionName("benchmark-session")
            .withDurationSeconds(3600)
            .withExternalId("benchmark-external-id")
            .withPolicy("{\"Version\":\"2012-10-17\"}");
    }

    private static byte[] loadFixture(String path) throws IOException {
        return com.amazonaws.util.IOUtils.toByteArray(
            V1QueryProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
