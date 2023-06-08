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

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.JSON_ALL_TYPES_REQUEST;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestjson.model.ExplicitPayloadAndHeadersException;
import software.amazon.awssdk.services.protocolrestjson.model.ImplicitPayloadException;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.transform.AllTypesRequestMarshaller;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Benchmarking for running with different protocols.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class MarshallingBenchmark {
    private static final AwsJsonProtocolFactory PROTOCOL_FACTORY =
        AwsJsonProtocolFactory.builder()
                              .clientConfiguration(SdkClientConfiguration.builder()
                                                                         .option(SdkClientOption.ENDPOINT, URI.create("https://localhost"))
                                                                         .build())
                              .defaultServiceExceptionSupplier(ProtocolRestJsonException::builder)
                              .protocol(AwsJsonProtocol.REST_JSON)
                              .protocolVersion("1.1")
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ImplicitPayloadException")
                                                   .exceptionBuilderSupplier(ImplicitPayloadException::builder).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("ExplicitPayloadAndHeadersException")
                                                   .exceptionBuilderSupplier(ExplicitPayloadAndHeadersException::builder).build())
                              .registerModeledException(
                                  ExceptionMetadata.builder().errorCode("EmptyModeledException")
                                                   .exceptionBuilderSupplier(EmptyModeledException::builder).build())
                              .build();
    private static final AllTypesRequestMarshaller MARSHALLER = new AllTypesRequestMarshaller(PROTOCOL_FACTORY);

    @Benchmark
    public void oldWay(Blackhole blackhole) {
        blackhole.consume(MARSHALLER.marshall(JSON_ALL_TYPES_REQUEST));
    }

    @Benchmark
    public void newWay(Blackhole blackhole) {
        blackhole.consume(MARSHALLER.fastMarshall(JSON_ALL_TYPES_REQUEST));
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(MarshallingBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
