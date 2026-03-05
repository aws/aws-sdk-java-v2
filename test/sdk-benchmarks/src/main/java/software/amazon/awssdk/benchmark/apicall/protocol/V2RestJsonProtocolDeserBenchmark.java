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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V2RestJsonProtocolDeserBenchmark {

    private LambdaClient lambda;
    private InvokeRequest invokeRequest;
    private ListFunctionsRequest listFunctionsRequest;

    @Setup
    public void setup() {
        InvokeResponse invokeResponse = InvokeResponse.builder()
                .statusCode(200)
                .payload(SdkBytes.fromUtf8String("{}"))
                .build();

        ListFunctionsResponse listFunctionsResponse = ListFunctionsResponse.builder().build();

        lambda = new LambdaClient() {
            @Override
            public String serviceName() {
                return "Lambda";
            }

            @Override
            public void close() {
            }

            @Override
            public InvokeResponse invoke(InvokeRequest request) {
                return invokeResponse;
            }

            @Override
            public ListFunctionsResponse listFunctions(ListFunctionsRequest request) {
                return listFunctionsResponse;
            }
        };

        invokeRequest = InvokeRequest.builder()
                .functionName("test-function")
                .payload(SdkBytes.fromUtf8String("{}"))
                .build();

        listFunctionsRequest = ListFunctionsRequest.builder().build();
    }

    @Benchmark
    public InvokeResponse invokeSerialization(Blackhole bh) {
        InvokeResponse result = lambda.invoke(invokeRequest);
        bh.consume(result.statusCode());
        bh.consume(result.payload());
        return result;
    }

    @Benchmark
    public ListFunctionsResponse listFunctionsDeserialization(Blackhole bh) {
        ListFunctionsResponse result = lambda.listFunctions(listFunctionsRequest);
        bh.consume(result.functions());
        return result;
    }
}
