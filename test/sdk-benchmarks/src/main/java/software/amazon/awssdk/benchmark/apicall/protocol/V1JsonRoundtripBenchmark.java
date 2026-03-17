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
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.nio.ByteBuffer;
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

/**
 * V1 roundtrip benchmark for JSON protocol (aws-json) using DynamoDB PutItem via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class V1JsonRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private AmazonDynamoDB client;
    private PutItemRequest putItemRequest;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = ProtocolRoundtripServer.loadFixture("json-protocol/putitem-response.json");

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet(response);

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                server.getHttpUri().toString(), "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
            .build();

        putItemRequest = new PutItemRequest()
            .withTableName("benchmark-table")
            .withItem(itemMap());
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.shutdown();
        server.stop();
    }

    @Benchmark
    public void putItem(Blackhole bh) {
        bh.consume(client.putItem(putItemRequest));
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", new AttributeValue().withS("benchmark-key"));
        item.put("sk", new AttributeValue().withN("100"));
        item.put("stringField", new AttributeValue().withS("test-value"));
        item.put("numberField", new AttributeValue().withN("123.456"));
        item.put("binaryField", new AttributeValue().withB(ByteBuffer.wrap("hello world".getBytes())));
        item.put("stringSetField", new AttributeValue().withSS("value1", "value2", "value3"));
        item.put("numberSetField", new AttributeValue().withNS("1.1", "2.2", "3.3"));
        item.put("boolField", new AttributeValue().withBOOL(false));
        item.put("nullField", new AttributeValue().withNULL(true));
        Map<String, AttributeValue> deep = new HashMap<>();
        deep.put("level2", new AttributeValue().withN("999"));
        Map<String, AttributeValue> nested = new HashMap<>();
        nested.put("nested", new AttributeValue().withS("nested-value"));
        nested.put("deepNested", new AttributeValue().withM(deep));
        item.put("mapField", new AttributeValue().withM(nested));
        item.put("listField", new AttributeValue().withL(
            new AttributeValue().withS("item1"),
            new AttributeValue().withN("42"),
            new AttributeValue().withBOOL(true),
            new AttributeValue().withNULL(true)));
        return item;
    }
}
