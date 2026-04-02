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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Base64;
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
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;

/**
 * JMH benchmark for RPC v2 CBOR unmarshalling (deserialization).
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
public class RpcV2CborUnmarshallBenchmark {

        private static final String CONTENT_TYPE = "application/cbor";
        private static final String TEST_DATA_PATH = "serde-tests/rpc-v2-cbor/output/rpc_v2_cbor.json";

        @Param({
                        "rpcv2Cbor_GetItemOutput_Baseline",
                        "rpcv2Cbor_GetItemOutput_S",
                        "rpcv2Cbor_GetItemOutput_M",
                        "rpcv2Cbor_GetItemOutput_L",
                        "rpcv2Cbor_GetItemOutputBinary_S",
                        "rpcv2Cbor_GetItemOutputBinary_M",
                        "rpcv2Cbor_GetItemOutputBinary_L",
                        "awsJson1_0_HealthcheckResponse_Example"
        })
        private String testCaseId;

        private JsonProtocolUnmarshaller unmarshaller;
        private byte[] responseBytes;
        private int statusCode;
        private SdkResponse emptyResponse;

        @Setup(Level.Trial)
        public void setup() throws Exception {
                // 1. Load test cases
                List<BenchmarkTestCaseLoader.UnmarshallTestCase> allCases = BenchmarkTestCaseLoader
                                .loadUnmarshallTestCases(TEST_DATA_PATH);
                BenchmarkTestCaseLoader.UnmarshallTestCase testCase = allCases.stream()
                                .filter(tc -> tc.getId().equals(testCaseId))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

                // 2. Pre-store response bytes (CBOR responses are base64-encoded)
                this.responseBytes = Base64.getDecoder().decode(testCase.getResponseBody().trim());
                this.statusCode = testCase.getStatusCode() != null ? testCase.getStatusCode() : 200;

                // 3. Pre-construct unmarshaller
                this.unmarshaller = JsonProtocolUnmarshaller.builder()
                                .enableFastUnmarshalling(true)
                                .protocolUnmarshallDependencies(
                                                SmithyRpcV2CborProtocolFactory.defaultProtocolUnmarshallDependencies())
                                .build();

                // 4. Resolve response builder via reflection at setup time
                String fqcn = "software.amazon.awssdk.services.rpccbordataplane.model."
                                + testCase.getOperationName() + "Response";
                Class<?> responseClass = Class.forName(fqcn);
                Method builderMethod = responseClass.getMethod("builder");
                SdkResponse.Builder builder = (SdkResponse.Builder) builderMethod.invoke(null);
                this.emptyResponse = builder.build();
        }

        @Benchmark
        public void unmarshall(Blackhole bh) throws Exception {
                SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                .statusCode(statusCode)
                                .putHeader("Content-Type", CONTENT_TYPE)
                                .content(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)))
                                .build();
                bh.consume(unmarshaller.unmarshall((SdkPojo) emptyResponse.toBuilder(), response));
        }
}
