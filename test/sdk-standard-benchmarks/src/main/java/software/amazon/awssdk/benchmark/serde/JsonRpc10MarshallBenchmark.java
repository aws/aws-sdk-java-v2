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
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;

/**
 * JMH benchmark for JSON RPC 1.0 marshalling (serialization).
 *
 * <p>
 * SampleTime mode is used (instead of AverageTime) to enable percentile
 * collection (p50, p90, p95, p99) for the cross-language output schema.
 * </p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class JsonRpc10MarshallBenchmark {

        private static final URI ENDPOINT = URI.create("http://localhost/");
        private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
        private static final String INTERMEDIATE_MODEL_PATH = "models/awsjsonrpc10dataplane-1999-12-31-intermediate.json";
        private static final String TEST_DATA_PATH = "serde-tests/json-rpc-1-0/input/json_1_0.json";

        @Param({
                        "awsJson1_0_GetItemInput_Baseline",
                        "awsQuery_GetMetricDataRequest_S",
                        "awsQuery_GetMetricDataRequest_M",
                        "awsQuery_GetMetricDataRequest_L",
                        "awsJson1_0_HealthcheckRequest_Example",
                        "awsJson1_0_PutItemRequest_Baseline",
                        "awsJson1_0_PutItemRequest_ShallowMap_S",
                        "awsJson1_0_PutItemRequest_ShallowMap_M",
                        "awsJson1_0_PutItemRequest_ShallowMap_L",
                        "awsJson1_0_PutItemRequest_Nested_M",
                        "awsJson1_0_PutItemRequest_Nested_L",
                        "awsJson1_0_PutItemRequest_MixedItem_S",
                        "awsJson1_0_PutItemRequest_MixedItem_M",
                        "awsJson1_0_PutItemRequest_MixedItem_L",
                        "awsJson1_0_PutItemRequest_BinaryData_S",
                        "awsJson1_0_PutItemRequest_BinaryData_M",
                        "awsJson1_0_PutItemRequest_BinaryData_L",
                        "rpcv2Cbor_PutItemRequest_Baseline",
                        "rpcv2Cbor_PutItemRequest_ShallowMap_S",
                        "rpcv2Cbor_PutItemRequest_ShallowMap_M",
                        "rpcv2Cbor_PutItemRequest_ShallowMap_L",
                        "rpcv2Cbor_PutItemRequest_Nested_M",
                        "rpcv2Cbor_PutItemRequest_Nested_L",
                        "rpcv2Cbor_PutItemRequest_MixedItem_S",
                        "rpcv2Cbor_PutItemRequest_MixedItem_M",
                        "rpcv2Cbor_PutItemRequest_MixedItem_L",
                        "rpcv2Cbor_PutItemRequest_BinaryData_S",
                        "rpcv2Cbor_PutItemRequest_BinaryData_M",
                        "rpcv2Cbor_PutItemRequest_BinaryData_L",
                        "awsQuery_PutMetricDataRequest_Baseline",
                        "awsQuery_PutMetricDataRequest_S",
                        "awsQuery_PutMetricDataRequest_M",
                        "awsQuery_PutMetricDataRequest_L"
        })
        private String testCaseId;

        private SdkPojo request;
        private OperationInfo operationInfo;
        private AwsJsonProtocolMetadata protocolMetadata;

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

                // 4. Pre-build marshaller config
                this.operationInfo = BenchmarkTestCaseLoader.buildOperationInfo(model, testCase);
                this.protocolMetadata = AwsJsonProtocolMetadata.builder()
                                .protocol(AwsJsonProtocol.AWS_JSON)
                                .contentType(CONTENT_TYPE)
                                .build();
        }

        @Benchmark
        public void marshall(Blackhole bh) {
                ProtocolMarshaller<SdkHttpFullRequest> marshaller = JsonProtocolMarshallerBuilder.create()
                                .endpoint(ENDPOINT)
                                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY
                                                .createWriter(CONTENT_TYPE))
                                .contentType(CONTENT_TYPE)
                                .operationInfo(operationInfo)
                                .sendExplicitNullForPayload(false)
                                .protocolMetadata(protocolMetadata)
                                .build();
                bh.consume(marshaller.marshall(request));
        }
}
