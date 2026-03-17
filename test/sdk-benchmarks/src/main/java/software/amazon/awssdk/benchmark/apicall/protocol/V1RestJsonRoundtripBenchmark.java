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
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.TracingConfig;
import com.amazonaws.services.lambda.model.TracingMode;
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

/**
 * V1 roundtrip benchmark for REST-JSON protocol using Lambda CreateFunction via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class V1RestJsonRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private AWSLambda client;
    private CreateFunctionRequest request;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = ProtocolRoundtripServer.loadFixture("rest-json-protocol/createfunction-response.json");

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet()
            .routeByUri("/2015-03-31/functions", "application/json", response);

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = AWSLambdaClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                server.getHttpUri().toString(), "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
            .build();

        Map<String, String> envVars = new HashMap<>();
        envVars.put("ENV_VAR_1", "value1");

        request = new CreateFunctionRequest()
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
            .withTracingConfig(new TracingConfig().withMode(TracingMode.Active));
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.shutdown();
        server.stop();
    }

    @Benchmark
    public void createFunction(Blackhole bh) {
        bh.consume(client.createFunction(request));
    }
}
