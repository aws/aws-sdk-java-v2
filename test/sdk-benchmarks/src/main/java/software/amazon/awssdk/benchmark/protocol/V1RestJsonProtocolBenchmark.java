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
import com.amazonaws.protocol.json.JsonClientMetadata;
import com.amazonaws.protocol.json.SdkJsonProtocolFactory;
import com.amazonaws.protocol.json.SdkStructuredPlainJsonFactory;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.TracingConfig;
import com.amazonaws.services.lambda.model.TracingMode;
import com.amazonaws.services.lambda.model.transform.CreateFunctionRequestProtocolMarshaller;
import com.amazonaws.services.lambda.model.transform.CreateFunctionResultJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContextImpl;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Isolated ser/de benchmark for V1 Lambda (REST-JSON protocol).
 * Measures only JSON parsing + object construction -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V1RestJsonProtocolBenchmark {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private byte[] responseBytes;
    private HttpResponse httpResponse;
    private SdkJsonProtocolFactory protocolFactory;
    private CreateFunctionRequest request;

    @Setup
    public void setup() throws Exception {
        responseBytes = loadFixture("fixtures/rest-json-protocol/createfunction-response.json");
        httpResponse = new HttpResponse(null, null);
        httpResponse.setStatusCode(200);
        protocolFactory = new SdkJsonProtocolFactory(new JsonClientMetadata()
                                                         .withProtocolVersion("1.1")
                                                         .withContentTypeOverride("application/json"));
        request = createRequest();
    }

    @Benchmark
    public void createFunctionDeser(Blackhole bh) throws Exception {
        JsonParser parser = JSON_FACTORY.createParser(new ByteArrayInputStream(responseBytes));
        JsonUnmarshallerContextImpl ctx = new JsonUnmarshallerContextImpl(
            parser,
            SdkStructuredPlainJsonFactory.JSON_SCALAR_UNMARSHALLERS,
            SdkStructuredPlainJsonFactory.JSON_CUSTOM_TYPE_UNMARSHALLERS,
            httpResponse);

        bh.consume(CreateFunctionResultJsonUnmarshaller.getInstance().unmarshall(ctx));
    }

    @Benchmark
    public void createFunctionSer(Blackhole bh) {
        bh.consume(new CreateFunctionRequestProtocolMarshaller(protocolFactory)
            .marshall(request));
    }

    private static CreateFunctionRequest createRequest() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("ENV_VAR_1", "value1");

        return new CreateFunctionRequest()
            .withFunctionName("benchmark-function")
            .withRuntime(Runtime.Java8)
            .withRole("arn:aws:iam::123456789012:role/lambda-role")
            .withHandler("com.example.Handler::handleRequest")
            .withCode(new FunctionCode()
                .withS3Bucket("my-deploy-bucket")
                .withS3Key("code/function.zip"))
            .withDescription("Benchmark test function")
            .withTimeout(30)
            .withMemorySize(512)
            .withPublish(false)
            .withEnvironment(new Environment().withVariables(envVars))
            .withTracingConfig(new TracingConfig()
                .withMode(TracingMode.Active));
    }

    private static byte[] loadFixture(String path) throws IOException {
        return com.amazonaws.util.IOUtils.toByteArray(
            V1RestJsonProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
