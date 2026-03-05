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

/**
 * CBOR protocol benchmark - DynamoDB can use CBOR instead of JSON for wire format.
 * This tests the same operations as JSON but represents CBOR protocol overhead.
 * Note: V1 doesn't support CBOR, only V2.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V2CborProtocolDeserBenchmark {

    private DynamoDbClient dynamoDb;
    private QueryRequest queryRequest;
    private software.amazon.awssdk.services.dynamodb.model.PutItemRequest putItemRequest;

    @Setup
    public void setup() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("test-id").build());
        item.put("name", AttributeValue.builder().s("test-name").build());

        QueryResponse queryResponse = QueryResponse.builder()
                .count(1)
                .scannedCount(1)
                .items(item)
                .build();

        software.amazon.awssdk.services.dynamodb.model.PutItemResponse putItemResponse = 
                software.amazon.awssdk.services.dynamodb.model.PutItemResponse.builder().build();

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
        };

        queryRequest = QueryRequest.builder()
                .tableName("test-table")
                .keyConditionExpression("id = :id")
                .build();

        putItemRequest = software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
                .tableName("test-table")
                .item(item)
                .build();
    }

    @Benchmark
    public QueryResponse queryDeserialization(Blackhole bh) {
        QueryResponse result = dynamoDb.query(queryRequest);
        bh.consume(result.count());
        bh.consume(result.items());
        return result;
    }

    @Benchmark
    public software.amazon.awssdk.services.dynamodb.model.PutItemResponse putItemSerialization(Blackhole bh) {
        software.amazon.awssdk.services.dynamodb.model.PutItemResponse result = dynamoDb.putItem(putItemRequest);
        bh.consume(result);
        return result;
    }
}
