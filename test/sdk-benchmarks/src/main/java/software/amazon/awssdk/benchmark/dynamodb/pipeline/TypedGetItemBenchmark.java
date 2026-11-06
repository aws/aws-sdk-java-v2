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
import software.amazon.awssdk.benchmark.dynamodb.fixture.BenchmarkItem;
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockClientFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Tier C TYPED sync GetItem: Enhanced Client mapping plus full mocked SDK pipeline.
 *
 * <p>No low-level {@code GetItemRequest} is pre-built; the timed path includes Enhanced mapping
 * into the low-level request and mapping of the response back to {@link BenchmarkItem}.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class TypedGetItemBenchmark {

    private DynamoDbClient lowLevelClient;
    private DynamoDbTable<BenchmarkItem> table;
    private Key key;

    @Setup(Level.Trial)
    public void setup() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        lowLevelClient = DynamoDbMockClientFactory.syncGetItemClient(fixture.attributeMap());
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(lowLevelClient)
                                                                      .build();
        table = enhancedClient.table(DynamoDbBenchmarkConstant.TABLE_NAME, fixture.tableSchema());
        key = Key.builder()
                 .partitionValue(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE)
                 .build();

        // One-time smoke call: verifies response→bean mapping and warms the SDK pipeline before
        // JMH warmup iterations begin.
        BenchmarkItem smoke = table.getItem(key);
        if (smoke == null || smoke.getPk() == null) {
            throw new IllegalStateException("Typed GetItem smoke call did not map a non-null item");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (lowLevelClient != null) {
            lowLevelClient.close();
        }
    }

    @Benchmark
    public BenchmarkItem getItem() {
        return table.getItem(key);
    }
}
