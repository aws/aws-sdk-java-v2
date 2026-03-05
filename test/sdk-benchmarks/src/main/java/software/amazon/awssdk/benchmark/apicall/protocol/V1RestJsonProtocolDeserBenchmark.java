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

import com.amazonaws.services.lambda.AbstractAWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import java.nio.ByteBuffer;
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

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V1RestJsonProtocolDeserBenchmark {

    private AbstractAWSLambda lambda;
    private InvokeRequest invokeRequest;
    private ListFunctionsRequest listFunctionsRequest;

    @Setup
    public void setup() {
        InvokeResult invokeResult = new InvokeResult()
                .withStatusCode(200)
                .withPayload(ByteBuffer.wrap("{}".getBytes()));

        ListFunctionsResult listFunctionsResult = new ListFunctionsResult();

        lambda = new AbstractAWSLambda() {
            @Override
            public InvokeResult invoke(InvokeRequest request) {
                return invokeResult;
            }

            @Override
            public ListFunctionsResult listFunctions(ListFunctionsRequest request) {
                return listFunctionsResult;
            }
        };

        invokeRequest = new InvokeRequest()
                .withFunctionName("test-function")
                .withPayload("{}");

        listFunctionsRequest = new ListFunctionsRequest();
    }

    @Benchmark
    public InvokeResult invokeSerialization(Blackhole bh) {
        InvokeResult result = lambda.invoke(invokeRequest);
        bh.consume(result.getStatusCode());
        bh.consume(result.getPayload());
        return result;
    }

    @Benchmark
    public ListFunctionsResult listFunctionsDeserialization(Blackhole bh) {
        ListFunctionsResult result = lambda.listFunctions(listFunctionsRequest);
        bh.consume(result.getFunctions());
        return result;
    }
}
