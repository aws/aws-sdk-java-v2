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

import java.util.HashMap;
import java.util.Map;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V2JsonProtocolDeserBenchmark {

    private DynamoDbClient dynamoDb;
    private QueryRequest queryRequest;
    private software.amazon.awssdk.services.dynamodb.model.PutItemRequest putItemRequest;
    private software.amazon.awssdk.services.dynamodb.model.GetItemRequest getItemRequest;
    private software.amazon.awssdk.services.dynamodb.model.ScanRequest scanRequest;

    @Setup
    public void setup() {
        // Query response
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("test-id").build());
        item.put("name", AttributeValue.builder().s("test-name").build());

        QueryResponse queryResponse = QueryResponse.builder()
                .count(1)
                .scannedCount(1)
                .items(item)
                .build();

        // PutItem response
        software.amazon.awssdk.services.dynamodb.model.PutItemResponse putItemResponse = 
                software.amazon.awssdk.services.dynamodb.model.PutItemResponse.builder().build();

        // GetItem response
        software.amazon.awssdk.services.dynamodb.model.GetItemResponse getItemResponse = 
                software.amazon.awssdk.services.dynamodb.model.GetItemResponse.builder().item(item).build();

        // Scan response
        software.amazon.awssdk.services.dynamodb.model.ScanResponse scanResponse = 
                software.amazon.awssdk.services.dynamodb.model.ScanResponse.builder()
                        .count(1)
                        .scannedCount(1)
                        .items(item)
                        .build();

        dynamoDb = new DynamoDbClient() {
            @Override
            public String serviceName() {
                return "DynamoDB";
            }

            @Override
            public void close() {
            }

            @Override
            public QueryResponse query(QueryRequest request) {
                return queryResponse;
            }

            @Override
            public software.amazon.awssdk.services.dynamodb.model.PutItemResponse putItem(
                    software.amazon.awssdk.services.dynamodb.model.PutItemRequest request) {
                return putItemResponse;
            }

            @Override
            public software.amazon.awssdk.services.dynamodb.model.GetItemResponse getItem(
                    software.amazon.awssdk.services.dynamodb.model.GetItemRequest request) {
                return getItemResponse;
            }

            @Override
            public software.amazon.awssdk.services.dynamodb.model.ScanResponse scan(
                    software.amazon.awssdk.services.dynamodb.model.ScanRequest request) {
                return scanResponse;
            }
        };

        queryRequest = QueryRequest.builder()
                .tableName("test-table")
                .keyConditionExpression("id = :id")
                .build();

        putItemRequest = software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
                .tableName("test-table")
                .item(item)
                .build();

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s("test-id").build());
        getItemRequest = software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
                .tableName("test-table")
                .key(key)
                .build();

        scanRequest = software.amazon.awssdk.services.dynamodb.model.ScanRequest.builder()
                .tableName("test-table")
                .build();
    }

    @Benchmark
    public QueryResponse queryDeserialization(Blackhole bh) {
        QueryResponse result = dynamoDb.query(queryRequest);
        // Force actual work by accessing fields
        bh.consume(result.count());
        bh.consume(result.scannedCount());
        bh.consume(result.items());
        return result;
    }

    @Benchmark
    public software.amazon.awssdk.services.dynamodb.model.PutItemResponse putItemSerialization(Blackhole bh) {
        software.amazon.awssdk.services.dynamodb.model.PutItemResponse result = dynamoDb.putItem(putItemRequest);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public software.amazon.awssdk.services.dynamodb.model.GetItemResponse getItemDeserialization(Blackhole bh) {
        software.amazon.awssdk.services.dynamodb.model.GetItemResponse result = dynamoDb.getItem(getItemRequest);
        bh.consume(result.item());
        return result;
    }

    @Benchmark
    public software.amazon.awssdk.services.dynamodb.model.ScanResponse scanDeserialization(Blackhole bh) {
        software.amazon.awssdk.services.dynamodb.model.ScanResponse result = dynamoDb.scan(scanRequest);
        bh.consume(result.count());
        bh.consume(result.items());
        return result;
    }
}
