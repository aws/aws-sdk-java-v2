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

import java.nio.charset.StandardCharsets;
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
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.utils.BenchmarkConstantGetMetricData;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.services.protocolsmithyrpcv2.model.GetMetricDataResponse;

/**
 * Benchmarking for running with different protocols.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1) // To reduce difference between each run
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JsonMarshallerBenchmark {

    @State(Scope.Thread)
    public static class MarshallingState {
        @Param({"small", "medium", "big"})
        public String size;

        @Param({"smithy-rpc-v2", "aws-json"})
        public String protocol;

        GetMetricDataResponse data;
        AwsJsonProtocol jsonProtocol;
        private JsonCodec codec;
        private byte[] rawBytes;

        @Setup(Level.Trial)
        public void setup() {
            jsonProtocol = AwsJsonProtocol.SMITHY_RPC_V2_CBOR;
            if (protocol != null) {
                switch (protocol) {
                    case "smithy-rpc-v2":
                        jsonProtocol = AwsJsonProtocol.SMITHY_RPC_V2_CBOR;
                        break;
                    case "aws-json":
                        jsonProtocol = AwsJsonProtocol.AWS_JSON;
                        break;
                    default:
                        throw new IllegalArgumentException("protocol: " + protocol);
                }
            }

            byte[] payload = null;
            String payloadSize = "small";
            if (size != null) {
                payloadSize = size;
            }
            switch (payloadSize) {
                case "small":
                    payload = BenchmarkConstantGetMetricData.smallPayload().getBytes(StandardCharsets.UTF_8);
                    break;
                case "medium":
                    payload = BenchmarkConstantGetMetricData.medPayload().getBytes(StandardCharsets.UTF_8);
                    break;
                case "big":
                    payload = BenchmarkConstantGetMetricData.bigPayload().getBytes(StandardCharsets.UTF_8);
                    break;
                default:
                    throw new IllegalArgumentException("size: " + size);
            }
            codec = new JsonCodec();
            data = (GetMetricDataResponse) codec.unmarshall(AwsJsonProtocol.AWS_JSON,
                                                            GetMetricDataResponse.builder(),
                                                            payload);
            rawBytes = codec.marshall(jsonProtocol, data);
        }
    }

    @Benchmark
    public void marshall(MarshallingState state, Blackhole blackhole) {
        blackhole.consume(state.codec.marshall(state.jsonProtocol, state.data));
    }

    @Benchmark
    public void unmarshall(MarshallingState state, Blackhole blackhole) {
        blackhole.consume(state.codec.unmarshall(state.jsonProtocol, GetMetricDataResponse.builder(), state.rawBytes));
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(JsonMarshallerBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        new Runner(opt).run();
    }
}
