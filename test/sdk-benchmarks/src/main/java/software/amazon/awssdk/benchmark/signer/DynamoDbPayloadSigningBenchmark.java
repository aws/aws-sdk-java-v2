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

package software.amazon.awssdk.benchmark.signer;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.utils.MockHttpClient;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class DynamoDbPayloadSigningBenchmark {

    @Param({"10240", "409600"})
    private int payloadSize;

    private DynamoDbClient client;
    private PutItemRequest putRequest;

    @Setup(Level.Trial)
    public void setup() {
        client = DynamoDbClient.builder()
                               .region(Region.US_EAST_1)
                               .endpointOverride(URI.create("https://dynamodb.us-east-1.amazonaws.com"))
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("AKIATEST", "testSecretKey")))
                               .httpClient(new MockHttpClient("{}", "{}"))
                               .build();

        byte[] data = new byte[payloadSize];
        new Random(42).nextBytes(data);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("test-id").build());
        item.put("data", AttributeValue.builder().b(SdkBytes.fromByteArray(data)).build());

        putRequest = PutItemRequest.builder()
                                   .tableName("test-table")
                                   .item(item)
                                   .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        client.close();
    }

    @Benchmark
    public void putItem(Blackhole blackhole) {
        PutItemResponse response = client.putItem(putRequest);
        blackhole.consume(response);
    }
}
