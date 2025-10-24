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
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import com.amazonaws.services.dynamodbv2.document.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
@State(Scope.Benchmark)
public class JsonConversionBenchmark {

    private String jsonString;
    private EnhancedDocument v2Document;
    private Item v1Item;

    @Setup
    public void setup() throws IOException {
        jsonString = new String(Files
                                    .readAllBytes(Paths.get("src/main/java/software/amazon/awssdk/benchmark/enhanced/dynamodb/large_data.json")));
        v2Document = EnhancedDocument.fromJson(jsonString);
        v1Item = Item.fromJSON(jsonString);
    }

    @Benchmark
    public String v1ToJson() {
        return v1Item.toJSON();
    }

    @Benchmark
    public String v2ToJson() {
        return v2Document.toJson();
    }
}
