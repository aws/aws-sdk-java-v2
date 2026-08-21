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

package software.amazon.awssdk.benchmark.dynamodb.pipeline;

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
import software.amazon.awssdk.benchmark.dynamodb.DynamoDbBenchmarkConstant;
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockClientFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * Tier C LOW-level sync PutItem: deterministic full SDK client-side pipeline via MockHttpClient.
 *
 * <p>Timed path: endpoint resolution → marshalling → SigV4 → HTTP construction → mock HTTP →
 * unmarshalling. Setup (client, fixture, request) is outside the timed method.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class LowLevelPutItemBenchmark {

    private DynamoDbClient client;
    private PutItemRequest request;

    @Setup(Level.Trial)
    public void setup() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        client = DynamoDbMockClientFactory.syncPutItemClient();
        request = PutItemRequest.builder()
                                .tableName(DynamoDbBenchmarkConstant.TABLE_NAME)
                                .item(fixture.attributeMap())
                                .build();

        // One-time smoke call: verifies the mock PutItem response unmarshals successfully and
        // intentionally warms/initializes the SDK pipeline before JMH warmup iterations begin.
        PutItemResponse smoke = client.putItem(request);
        if (smoke == null) {
            throw new IllegalStateException("PutItem smoke call returned null");
        }
    }

    /**
     * Closes the DynamoDbClient. The injected {@code MockHttpClient} is treated as a shared
     * instance by the SDK (not closed by the client); {@code MockHttpClient#close()} is a no-op.
     * Each JMH fork runs in a separate JVM with its own client, so there is no cross-fork shared state.
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Benchmark
    public PutItemResponse putItem() {
        return client.putItem(request);
    }
}
