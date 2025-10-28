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
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
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

    private EnhancedDocument v2Document;
    private Item v1Item;

    private EnhancedDocument entireDoc;
    private EnhancedDocument doc50b;
    private EnhancedDocument doc100b;
    private EnhancedDocument doc500b;
    private EnhancedDocument doc1kb;
    private Item v1Item50b;
    private Item v1Item100b;
    private Item v1Item500b;
    private Item v1Item1kb;

    @Setup
    public void setup() throws IOException {
        String entireJsonString = new String(Files
            .readAllBytes(Paths.get("src/main/java/software/amazon/awssdk/benchmark/enhanced/dynamodb/large_data.json")));
        String payload50b = new String(Files
            .readAllBytes(Paths.get("src/main/java/software/amazon/awssdk/benchmark/enhanced/dynamodb/payload_50b.json")));
        String payload100b = new String(Files
            .readAllBytes(Paths.get("src/main/java/software/amazon/awssdk/benchmark/enhanced/dynamodb/payload_100b.json")));
        String payload500b = new String(Files
            .readAllBytes(Paths.get("src/main/java/software/amazon/awssdk/benchmark/enhanced/dynamodb/payload_500b.json")));
        String payload1kb = new String(Files
            .readAllBytes(Paths.get("src/main/java/software/amazon/awssdk/benchmark/enhanced/dynamodb/payload_1kb.json")));


        v2Document = EnhancedDocument.fromJson(entireJsonString);
        v1Item = Item.fromJSON(entireJsonString);

        v1Item.withJSON("entireDocument", entireJsonString);

        entireDoc = EnhancedDocument.builder()
            .addAttributeConverterProvider(AttributeConverterProvider.defaultProvider())
            .putString("id", "entireDoc")
            .putJson("entireDocument", entireJsonString)
            .build();

        doc50b = EnhancedDocument.builder()
            .addAttributeConverterProvider(AttributeConverterProvider.defaultProvider())
            .putString("id", "test")
            .putJson("data", payload50b)
            .build();

        doc100b = EnhancedDocument.builder()
            .addAttributeConverterProvider(AttributeConverterProvider.defaultProvider())
            .putString("id", "test")
            .putJson("data", payload100b)
            .build();

        doc500b = EnhancedDocument.builder()
            .addAttributeConverterProvider(AttributeConverterProvider.defaultProvider())
            .putString("id", "test")
            .putJson("data", payload500b)
            .build();

        doc1kb = EnhancedDocument.builder()
            .addAttributeConverterProvider(AttributeConverterProvider.defaultProvider())
            .putString("id", "test")
            .putJson("data", payload1kb)
            .build();

        v1Item50b = new Item()
            .withString("id", "test")
            .withJSON("data", payload50b);

        v1Item100b = new Item()
            .withString("id", "test")
            .withJSON("data", payload100b);

        v1Item500b = new Item()
            .withString("id", "test")
            .withJSON("data", payload500b);

        v1Item1kb = new Item()
            .withString("id", "test")
            .withJSON("data", payload1kb);
    }

    // getJson() meant to get a sub structure of a larger json file. testing with different sizes.
    @Benchmark
    public String getJson50b() {
        return doc50b.getJson("data");
    }

    @Benchmark
    public String getJson100b() {
        return doc100b.getJson("data");
    }

    @Benchmark
    public String getJson500b() {
        return doc500b.getJson("data");
    }

    @Benchmark
    public String getJson1kb() {
        return doc1kb.getJson("data");
    }

    @Benchmark
    public String v1GetJson50b() {
        return v1Item50b.getJSON("data");
    }

    @Benchmark
    public String v1GetJson100b() {
        return v1Item100b.getJSON("data");
    }

    @Benchmark
    public String v1GetJson500b() {
        return v1Item500b.getJSON("data");
    }

    @Benchmark
    public String v1GetJson1kb() {
        return v1Item1kb.getJSON("data");
    }
    
    @Benchmark
    public String v1GetJsonEntireDoc() {
        return v1Item.getJSON("entireDocument");
    }

    @Benchmark
    public String v2GetJsonEntireDoc() {
        return entireDoc.getJson("entireDocument");
    }

    // toJson() benchmarking (only for large data set)
    @Benchmark
    public String v1ToJson() {
        return v1Item.toJSON();
    }

    @Benchmark
    public String v2ToJson() {
        return v2Document.toJson();
    }

}
