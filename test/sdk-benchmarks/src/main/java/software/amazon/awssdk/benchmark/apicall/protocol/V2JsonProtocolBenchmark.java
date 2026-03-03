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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Benchmarks JSON protocol using real DynamoDB operations with maximal type coverage.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class V2JsonProtocolBenchmark {

    private ProtocolBenchmarkWireMock wireMock;
    private DynamoDbClient client;
    private PutItemRequest putItemRequest;
    private QueryRequest queryRequest;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        String putItemResponse = ProtocolBenchmarkWireMock.loadFixture("json-protocol/putitem-response.json");
        String queryResponse = ProtocolBenchmarkWireMock.loadFixture("json-protocol/query-response.json");
        
        wireMock = new ProtocolBenchmarkWireMock();
        wireMock.start();
        wireMock.stubDynamoDbResponses(putItemResponse, queryResponse);
        
        client = DynamoDbClient.builder()
                .endpointOverride(URI.create(wireMock.baseUrl()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();

        // Create PutItem request with all AttributeValue types
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.builder().s("benchmark-key").build());
        item.put("sk", AttributeValue.builder().n("100").build());
        item.put("stringField", AttributeValue.builder().s("test-value").build());
        item.put("numberField", AttributeValue.builder().n("123.456").build());
        item.put("binaryField", AttributeValue.builder().b(SdkBytes.fromUtf8String("hello")).build());
        item.put("stringSetField", AttributeValue.builder().ss("a", "b", "c").build());
        item.put("numberSetField", AttributeValue.builder().ns("1.1", "2.2").build());
        item.put("binarySetField", AttributeValue.builder().bs(
                SdkBytes.fromUtf8String("bin1"),
                SdkBytes.fromUtf8String("bin2")).build());
        
        // Map with nested structure
        Map<String, AttributeValue> nestedMap = new HashMap<>();
        nestedMap.put("nested", AttributeValue.builder().s("nested-value").build());
        Map<String, AttributeValue> deepMap = new HashMap<>();
        deepMap.put("level2", AttributeValue.builder().n("999").build());
        nestedMap.put("deepNested", AttributeValue.builder().m(deepMap).build());
        item.put("mapField", AttributeValue.builder().m(nestedMap).build());
        
        // List with mixed types
        item.put("listField", AttributeValue.builder().l(
                AttributeValue.builder().s("item1").build(),
                AttributeValue.builder().n("42").build(),
                AttributeValue.builder().bool(true).build(),
                AttributeValue.builder().nul(true).build()
        ).build());
        
        item.put("nullField", AttributeValue.builder().nul(true).build());
        item.put("boolField", AttributeValue.builder().bool(false).build());

        putItemRequest = PutItemRequest.builder()
                .tableName("benchmark-table")
                .item(item)
                .build();

        queryRequest = QueryRequest.builder()
                .tableName("benchmark-table")
                .keyConditionExpression("pk = :pk")
                .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (wireMock != null) {
            wireMock.stop();
        }
        if (client != null) {
            client.close();
        }
    }

    @Benchmark
    public void putItemSerialization(Blackhole blackhole) {
        blackhole.consume(client.putItem(putItemRequest));
    }

    @Benchmark
    public void queryDeserialization(Blackhole blackhole) {
        blackhole.consume(client.query(queryRequest));
    }
}
