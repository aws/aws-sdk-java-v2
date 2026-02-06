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

package software.amazon.awssdk.benchmark.enhanced.dynamodb;

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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Micro-benchmark to isolate String and ByteArray converter optimization impact.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class ConverterBenchmark {

    private StringAttributeConverter stringConverter;
    private ByteArrayAttributeConverter byteArrayConverter;
    
    private AttributeValue stringAttributeValue;
    private AttributeValue byteAttributeValue;
    
    @Setup
    public void setup() {
        stringConverter = StringAttributeConverter.create();
        byteArrayConverter = ByteArrayAttributeConverter.create();
        
        stringAttributeValue = AttributeValue.builder().s("test-string-value").build();
        byteAttributeValue = AttributeValue.builder().b(SdkBytes.fromUtf8String("test-bytes")).build();
    }

    @Benchmark
    public void stringConverter(Blackhole blackhole) {
        String result = stringConverter.transformTo(stringAttributeValue);
        blackhole.consume(result);
    }

    @Benchmark
    public void byteArrayConverter(Blackhole blackhole) {
        byte[] result = byteArrayConverter.transformTo(byteAttributeValue);
        blackhole.consume(result);
    }
}
