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
import software.amazon.awssdk.benchmark.dynamodb.fixture.BenchmarkItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/**
 * Tier D TYPED live Query (first page only): Enhanced mapping plus end-to-end DynamoDB latency.
 *
 * <p>Uses the same deterministic fixture/table setup as other live benches. Requires explicit
 * opt-in ({@code DYNAMODB_BENCHMARK_LIVE=true} or {@code -Ddynamodb.benchmark.live=true}).
 * Not selected by {@code BenchmarkRunner}.
 *
 * <p>The untimed smoke Query (DNS/TLS/connection-pool init) is distinct from JMH warmup
 * iterations configured below.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class TypedLiveQueryBenchmark {

    private LiveDynamoDbSupport support;
    private DynamoDbTable<BenchmarkItem> table;
    private QueryConditional queryConditional;

    @Setup(Level.Trial)
    public void setup() {
        support = LiveDynamoDbSupport.open("typedquery");
        table = support.typedTable();
        queryConditional = QueryConditional.keyEqualTo(support.partitionKey());

        // Untimed smoke call: DNS/TLS/connection-pool initialization. Distinct from JMH warmup.
        Page<BenchmarkItem> smoke = table.query(queryConditional).iterator().next();
        if (smoke == null || smoke.items() == null || smoke.items().isEmpty()) {
            throw new IllegalStateException("Typed live Query smoke call did not return at least one item");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (support != null) {
            support.close();
        }
    }

    /**
     * First page only for this phase.
     */
    @Benchmark
    public Page<BenchmarkItem> query() {
        return table.query(queryConditional).iterator().next();
    }
}
