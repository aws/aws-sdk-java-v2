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

package software.amazon.awssdk.benchmark.dynamodb.live;

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
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/**
 * Tier D LOW-level live GetItem: end-to-end latency including network and DynamoDB service.
 *
 * <p>Requires explicit opt-in ({@code DYNAMODB_BENCHMARK_LIVE=true} or
 * {@code -Ddynamodb.benchmark.live=true}). Not selected by {@code BenchmarkRunner}.
 *
 * <p>The untimed smoke GetItem (DNS/TLS/connection-pool init) is distinct from JMH warmup
 * iterations configured below.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class LowLevelLiveGetItemBenchmark {

    private LiveDynamoDbSupport support;
    private GetItemRequest request;

    @Setup(Level.Trial)
    public void setup() {
        support = LiveDynamoDbSupport.open("get");
        request = GetItemRequest.builder()
                                .tableName(support.tableName())
                                .key(Collections.singletonMap(
                                    DynamoDbBenchmarkFixture.PARTITION_KEY_NAME,
                                    support.fixture().partitionKeyAttribute()))
                                .build();

        // Untimed smoke call: DNS/TLS/connection-pool initialization. Distinct from JMH warmup.
        GetItemResponse smoke = support.client().getItem(request);
        if (smoke.item() == null || smoke.item().isEmpty()) {
            throw new IllegalStateException("Live GetItem smoke call did not return a non-empty item");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (support != null) {
            support.close();
        }
    }

    @Benchmark
    public GetItemResponse getItem() {
        return support.client().getItem(request);
    }
}
