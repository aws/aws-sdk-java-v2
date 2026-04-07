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
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.Environment;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.model.TracingConfig;
import software.amazon.awssdk.services.lambda.model.TracingMode;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V2RestJsonProtocolBenchmark {

    private static final String CONTENT_TYPE = "application/json";
    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .requestUri("/2015-03-31/functions")
        .httpMethod(SdkHttpMethod.POST)
        .hasExplicitPayloadMember(false)
        .hasImplicitPayloadMembers(true)
        .hasPayloadMembers(true)
        .build();

    private static final AwsJsonProtocolMetadata METADATA =
        AwsJsonProtocolMetadata.builder()
            .protocol(AwsJsonProtocol.REST_JSON)
            .protocolVersion("1.1")
            .contentType(CONTENT_TYPE)
            .build();

    private JsonProtocolUnmarshaller unmarshaller;
    private byte[] responseBytes;
    private CreateFunctionRequest request;

    @Setup
    public void setup() throws Exception {
        unmarshaller = JsonProtocolUnmarshaller.builder()
            .enableFastUnmarshalling(true)
            .protocolUnmarshallDependencies(JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies())
            .build();

        responseBytes = loadFixture("fixtures/rest-json-protocol/createfunction-response.json");
        request = createRequest();
    }

    @Benchmark
    public void createFunctionDeser(Blackhole bh) throws Exception {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .putHeader("Content-Type", CONTENT_TYPE)
            .content(AbortableInputStream.create(
                new ByteArrayInputStream(responseBytes)))
            .build();
        bh.consume(unmarshaller.unmarshall(CreateFunctionResponse.builder(), response));
    }

    @Benchmark
    public void createFunctionSer(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller =
            JsonProtocolMarshallerBuilder.create()
                .endpoint(ENDPOINT)
                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY
                    .createWriter(CONTENT_TYPE))
                .contentType(CONTENT_TYPE)
                .operationInfo(OP_INFO)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(METADATA)
                .build();
        bh.consume(marshaller.marshall(request));
    }

    private static CreateFunctionRequest createRequest() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("ENV_VAR_1", "value1");

        return CreateFunctionRequest.builder()
            .functionName("benchmark-function")
            .runtime(Runtime.JAVA8)
            .role("arn:aws:iam::123456789012:role/lambda-role")
            .handler("com.example.Handler::handleRequest")
            .code(FunctionCode.builder()
                .s3Bucket("my-deploy-bucket")
                .s3Key("code/function.zip")
                .build())
            .description("Benchmark test function")
            .timeout(30)
            .memorySize(512)
            .publish(false)
            .environment(Environment.builder().variables(envVars).build())
            .tracingConfig(TracingConfig.builder()
                .mode(TracingMode.ACTIVE)
                .build())
            .build();
    }

    private static byte[] loadFixture(String path) throws IOException {
        return software.amazon.awssdk.utils.IoUtils.toByteArray(
            V2RestJsonProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
