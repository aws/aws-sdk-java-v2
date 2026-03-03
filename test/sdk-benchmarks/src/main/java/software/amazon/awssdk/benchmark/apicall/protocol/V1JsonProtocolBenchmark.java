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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

/**
 * Benchmarks JSON protocol using real V1 DynamoDB operations with maximal type coverage.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class V1JsonProtocolBenchmark {

    private ProtocolBenchmarkWireMock wireMock;
    private AmazonDynamoDB client;
    private PutItemRequest putItemRequest;
    private QueryRequest queryRequest;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        String putItemResponse = ProtocolBenchmarkWireMock.loadFixture("json-protocol/putitem-response.json");
        String queryResponse = ProtocolBenchmarkWireMock.loadFixture("json-protocol/query-response.json");
        
        wireMock = new ProtocolBenchmarkWireMock();
        wireMock.start();
        wireMock.stubDynamoDbResponses(putItemResponse, queryResponse);
        
        client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(wireMock.baseUrl(), "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials("test", "test")))
                .build();

        // Create PutItem request with all AttributeValue types
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", new AttributeValue().withS("benchmark-key"));
        item.put("sk", new AttributeValue().withN("100"));
        item.put("stringField", new AttributeValue().withS("test-value"));
        item.put("numberField", new AttributeValue().withN("123.456"));
        item.put("binaryField", new AttributeValue().withB(ByteBuffer.wrap("hello".getBytes())));
        item.put("stringSetField", new AttributeValue().withSS("a", "b", "c"));
        item.put("numberSetField", new AttributeValue().withNS("1.1", "2.2"));
        item.put("binarySetField", new AttributeValue().withBS(
                ByteBuffer.wrap("bin1".getBytes()),
                ByteBuffer.wrap("bin2".getBytes())));
        
        // Map with nested structure
        Map<String, AttributeValue> nestedMap = new HashMap<>();
        nestedMap.put("nested", new AttributeValue().withS("nested-value"));
        Map<String, AttributeValue> deepMap = new HashMap<>();
        deepMap.put("level2", new AttributeValue().withN("999"));
        nestedMap.put("deepNested", new AttributeValue().withM(deepMap));
        item.put("mapField", new AttributeValue().withM(nestedMap));
        
        // List with mixed types
        List<AttributeValue> list = new ArrayList<>();
        list.add(new AttributeValue().withS("item1"));
        list.add(new AttributeValue().withN("42"));
        list.add(new AttributeValue().withBOOL(true));
        list.add(new AttributeValue().withNULL(true));
        item.put("listField", new AttributeValue().withL(list));
        
        item.put("nullField", new AttributeValue().withNULL(true));
        item.put("boolField", new AttributeValue().withBOOL(false));

        putItemRequest = new PutItemRequest()
                .withTableName("benchmark-table")
                .withItem(item);

        queryRequest = new QueryRequest()
                .withTableName("benchmark-table")
                .withKeyConditionExpression("pk = :pk");
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (wireMock != null) {
            wireMock.stop();
        }
        if (client != null) {
            client.shutdown();
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
