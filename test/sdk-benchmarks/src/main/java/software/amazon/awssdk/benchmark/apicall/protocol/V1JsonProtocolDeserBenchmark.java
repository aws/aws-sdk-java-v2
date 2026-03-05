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

import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
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

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V1JsonProtocolDeserBenchmark {

    private AbstractAmazonDynamoDB dynamoDb;
    private QueryRequest queryRequest;
    private PutItemRequest putItemRequest;
    private com.amazonaws.services.dynamodbv2.model.GetItemRequest getItemRequest;
    private com.amazonaws.services.dynamodbv2.model.ScanRequest scanRequest;

    @Setup
    public void setup() {
        // Query response
        QueryResult queryResult = new QueryResult()
                .withCount(1)
                .withScannedCount(1);
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", new AttributeValue().withS("test-id"));
        item.put("name", new AttributeValue().withS("test-name"));
        queryResult.withItems(item);

        // PutItem response
        com.amazonaws.services.dynamodbv2.model.PutItemResult putItemResult = 
                new com.amazonaws.services.dynamodbv2.model.PutItemResult();

        // GetItem response
        com.amazonaws.services.dynamodbv2.model.GetItemResult getItemResult = 
                new com.amazonaws.services.dynamodbv2.model.GetItemResult().withItem(item);

        // Scan response
        com.amazonaws.services.dynamodbv2.model.ScanResult scanResult = 
                new com.amazonaws.services.dynamodbv2.model.ScanResult()
                        .withCount(1)
                        .withScannedCount(1)
                        .withItems(item);

        dynamoDb = new AbstractAmazonDynamoDB() {
            @Override
            public QueryResult query(QueryRequest request) {
                return queryResult;
            }

            @Override
            public com.amazonaws.services.dynamodbv2.model.PutItemResult putItem(PutItemRequest request) {
                return putItemResult;
            }

            @Override
            public com.amazonaws.services.dynamodbv2.model.GetItemResult getItem(
                    com.amazonaws.services.dynamodbv2.model.GetItemRequest request) {
                return getItemResult;
            }

            @Override
            public com.amazonaws.services.dynamodbv2.model.ScanResult scan(
                    com.amazonaws.services.dynamodbv2.model.ScanRequest request) {
                return scanResult;
            }
        };

        queryRequest = new QueryRequest()
                .withTableName("test-table")
                .withKeyConditionExpression("id = :id");

        putItemRequest = new PutItemRequest()
                .withTableName("test-table")
                .withItem(item);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", new AttributeValue().withS("test-id"));
        getItemRequest = new com.amazonaws.services.dynamodbv2.model.GetItemRequest()
                .withTableName("test-table")
                .withKey(key);

        scanRequest = new com.amazonaws.services.dynamodbv2.model.ScanRequest()
                .withTableName("test-table");
    }

    @Benchmark
    public QueryResult queryDeserialization(Blackhole bh) {
        QueryResult result = dynamoDb.query(queryRequest);
        // Force actual work by accessing fields
        bh.consume(result.getCount());
        bh.consume(result.getScannedCount());
        bh.consume(result.getItems());
        return result;
    }

    @Benchmark
    public com.amazonaws.services.dynamodbv2.model.PutItemResult putItemSerialization(Blackhole bh) {
        com.amazonaws.services.dynamodbv2.model.PutItemResult result = dynamoDb.putItem(putItemRequest);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public com.amazonaws.services.dynamodbv2.model.GetItemResult getItemDeserialization(Blackhole bh) {
        com.amazonaws.services.dynamodbv2.model.GetItemResult result = dynamoDb.getItem(getItemRequest);
        bh.consume(result.getItem());
        return result;
    }

    @Benchmark
    public com.amazonaws.services.dynamodbv2.model.ScanResult scanDeserialization(Blackhole bh) {
        com.amazonaws.services.dynamodbv2.model.ScanResult result = dynamoDb.scan(scanRequest);
        bh.consume(result.getCount());
        bh.consume(result.getItems());
        return result;
    }
}
