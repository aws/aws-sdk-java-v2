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

import java.util.HashMap;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.Environment;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.model.TracingConfig;
import software.amazon.awssdk.services.lambda.model.TracingMode;

/**
 * Roundtrip benchmark for REST-JSON protocol using Lambda CreateFunction via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class V2RestJsonRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private LambdaClient client;
    private CreateFunctionRequest request;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = ProtocolRoundtripServer.loadFixture("rest-json-protocol/createfunction-response.json");

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet(response);

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = LambdaClient.builder()
            .endpointOverride(server.getHttpUri())
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .httpClient(Apache5HttpClient.create())
            .build();

        Map<String, String> envVars = new HashMap<>();
        envVars.put("ENV_VAR_1", "value1");

        request = CreateFunctionRequest.builder()
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
            .tracingConfig(TracingConfig.builder().mode(TracingMode.ACTIVE).build())
            .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.close();
        server.stop();
    }

    @Benchmark
    public void createFunction(Blackhole bh) {
        bh.consume(client.createFunction(request));
    }
}
