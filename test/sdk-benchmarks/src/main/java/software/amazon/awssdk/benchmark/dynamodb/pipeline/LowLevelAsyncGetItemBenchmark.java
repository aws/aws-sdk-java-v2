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

import java.util.Collections;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * Tier C LOW-level async GetItem: single-operation completion latency via mocked async HTTP.
 *
 * <p>Timed path waits on {@code join()} so the measurement includes async scheduling, the SDK
 * pipeline, zero-delay mock completion on the mock executor, unmarshalling, and future completion.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class LowLevelAsyncGetItemBenchmark {

    private DynamoDbMockClientFactory.AsyncMockResources resources;
    private DynamoDbAsyncClient client;
    private GetItemRequest request;

    @Setup(Level.Trial)
    public void setup() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        resources = DynamoDbMockClientFactory.asyncGetItemClient(fixture.attributeMap());
        client = resources.client();
        request = GetItemRequest.builder()
                                .tableName(DynamoDbBenchmarkConstant.TABLE_NAME)
                                .key(Collections.singletonMap(
                                    DynamoDbBenchmarkFixture.PARTITION_KEY_NAME,
                                    fixture.partitionKeyAttribute()))
                                .build();

        // One-time smoke call: verifies async completion + unmarshalling and warms the pipeline
        // before JMH warmup iterations begin.
        GetItemResponse smoke = client.getItem(request).join();
        if (smoke.item() == null || smoke.item().isEmpty()) {
            throw new IllegalStateException("Async GetItem smoke call did not deserialize a non-empty item");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (resources != null) {
            // Closes DynamoDbAsyncClient and MockAsyncHttpClient (executor shutdown).
            resources.close();
        }
    }

    @Benchmark
    public GetItemResponse getItem() {
        return client.getItem(request).join();
    }
}
