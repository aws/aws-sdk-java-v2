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

package software.amazon.awssdk.benchmark.serde;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;

/**
 * JMH benchmark for REST JSON marshalling (serialization).
 *
 * <p>
 * SampleTime mode is used (instead of AverageTime) to enable percentile
 * collection (p50, p90, p95, p99) for the cross-language output schema.
 * </p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class RestJsonMarshallBenchmark {

    private static final String INTERMEDIATE_MODEL_PATH = "models/awsrestjsondataplane-1999-12-31-intermediate.json";
    private static final String TEST_DATA_PATH = "serde-tests/rest-json/input/rest_json.json";

    @Param({
            "restJson1_CopyObjectRequest_Baseline",
            "restJson1_CopyObjectRequest_M",
            "restJson1_PutObject_S",
            "restJson1_PutObject_M",
            "restJson1_PutObject_L"
    })
    private String testCaseId;

    private SdkPojo request;
    private OperationInfo operationInfo;
    private AwsJsonProtocolFactory protocolFactory;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // 1. Load test cases
        List<BenchmarkTestCaseLoader.MarshallTestCase> allCases = BenchmarkTestCaseLoader
                .loadMarshallTestCases(TEST_DATA_PATH);
        BenchmarkTestCaseLoader.MarshallTestCase testCase = allCases.stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        // 2. Load IntermediateModel
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(INTERMEDIATE_MODEL_PATH);

        // 3. Build SDK request via ShapeModelReflector
        String inputShapeName = testCase.getOperationName() + "Request";
        ShapeModelReflector reflector = new ShapeModelReflector(model, inputShapeName, testCase.getInputData());
        this.request = (SdkPojo) reflector.createShapeObject();

        // 4. Pre-build marshaller config using the protocol factory (same as generated
        // code)
        this.operationInfo = BenchmarkTestCaseLoader.buildOperationInfo(model, testCase);
        this.protocolFactory = AwsJsonProtocolFactory.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ENDPOINT, URI.create("http://localhost"))
                        .build())
                .defaultServiceExceptionSupplier(null)
                .protocol(AwsJsonProtocol.REST_JSON)
                .protocolVersion("1.1")
                .build();
    }

    @Benchmark
    public void marshall(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = protocolFactory
                .createProtocolMarshaller(operationInfo);
        bh.consume(marshaller.marshall(request));
    }
}
