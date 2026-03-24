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

package software.amazon.awssdk.enhanced.dynamodb.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.SortDirection;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.AggregateSpec;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.OrderBySpec;
import software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;

/**
 * Unit tests for {@link QueryExpressionBuilder}. Verifies that the fluent builder produces a {@link QueryExpressionSpec} with the
 * correct base table, join, conditions, group-by, aggregates, order-by, projection, execution mode, and limit.
 * <p>
 * Uses a mock {@link DynamoDbTable} for the base (and optionally joined) table; no DynamoDB is used.
 */
@SuppressWarnings("unchecked")
public class QueryExpressionBuilderTest {

    private DynamoDbTable<Object> baseTable;
    private DynamoDbTable<Object> joinedTable;

    /**
     * Creates mock tables before each test so that {@link QueryExpressionBuilder#from(DynamoDbTable)} and
     * {@link #join(DynamoDbTable, JoinType, String, String)} have valid table references.
     */
    @Before
    public void setUp() {
        baseTable = mock(DynamoDbTable.class);
        when(baseTable.tableName()).thenReturn("base-table");
        joinedTable = mock(DynamoDbTable.class);
        when(joinedTable.tableName()).thenReturn("joined-table");
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#from(DynamoDbTable)} and {@link QueryExpressionBuilder#build()} produce a spec
     * whose {@link QueryExpressionSpec#baseTable()} is the given table and {@link QueryExpressionSpec#hasJoin()} is false.
     */
    @Test
    public void fromAndBuild_setsBaseTableNoJoin() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable).build();

        assertThat(spec.baseTable()).isSameAs(baseTable);
        assertThat(spec.hasJoin()).isFalse();
        assertThat(spec.joinedTable()).isNull();
        assertThat(spec.executionMode()).isEqualTo(ExecutionMode.STRICT_KEY_ONLY);
    }

    /**
     * Asserts that after {@link QueryExpressionBuilder#join(DynamoDbTable, JoinType, String, String)}, the built spec has the
     * joined table, join type, and join attributes set.
     */
    @Test
    public void join_setsJoinedTableAndAttributes() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .join(joinedTable, JoinType.INNER, "customerId", "customerId")
                                                         .build();

        assertThat(spec.hasJoin()).isTrue();
        assertThat(spec.joinedTable()).isSameAs(joinedTable);
        assertThat(spec.joinType()).isEqualTo(JoinType.INNER);
        assertThat(spec.leftJoinKey()).isEqualTo("customerId");
        assertThat(spec.rightJoinKey()).isEqualTo("customerId");
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#withCondition(Condition)} is stored in the spec as the single-table filter
     * ({@link QueryExpressionSpec#condition()}).
     */
    @Test
    public void withCondition_setsCondition() {
        Condition cond = Condition.eq("status", "ACTIVE");
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable).where(cond).build();

        assertThat(spec.where()).isSameAs(cond);
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#groupBy(String...)} adds attributes to
     * {@link QueryExpressionSpec#groupByAttributes()}.
     */
    @Test
    public void groupBy_setsGroupByAttributes() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .groupBy("customerId", "category")
                                                         .build();

        assertThat(spec.groupByAttributes()).containsExactly("customerId", "category");
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#aggregate(AggregationFunction, String, String)} adds an {@link AggregateSpec}
     * with the given function, attribute, and output name.
     */
    @Test
    public void aggregate_addsAggregateSpec() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .build();

        assertThat(spec.aggregates()).hasSize(1);
        assertThat(spec.aggregates().get(0).function()).isEqualTo(AggregationFunction.COUNT);
        assertThat(spec.aggregates().get(0).attribute()).isEqualTo("orderId");
        assertThat(spec.aggregates().get(0).outputName()).isEqualTo("orderCount");
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#orderBy(String, SortDirection)} and
     * {@link QueryExpressionBuilder#orderByAggregate(String, SortDirection)} add {@link OrderBySpec} entries to the spec.
     */
    @Test
    public void orderBy_andOrderByAggregate_addOrderBySpecs() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .orderBy("name", SortDirection.ASC)
                                                         .orderByAggregate("orderCount", SortDirection.DESC)
                                                         .build();

        assertThat(spec.orderBy()).hasSize(2);
        assertThat(spec.orderBy().get(0).attributeOrAggregateName()).isEqualTo("name");
        assertThat(spec.orderBy().get(0).direction()).isEqualTo(SortDirection.ASC);
        assertThat(spec.orderBy().get(0).isByAggregate()).isFalse();
        assertThat(spec.orderBy().get(1).attributeOrAggregateName()).isEqualTo("orderCount");
        assertThat(spec.orderBy().get(1).direction()).isEqualTo(SortDirection.DESC);
        assertThat(spec.orderBy().get(1).isByAggregate()).isTrue();
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#project(String...)} sets {@link QueryExpressionSpec#projectAttributes()}.
     */
    @Test
    public void project_setsProjectAttributes() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .project("customerId", "name", "orderId")
                                                         .build();

        assertThat(spec.projectAttributes()).containsExactly("customerId", "name", "orderId");
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#executionMode(ExecutionMode)} sets the spec's execution mode (STRICT_KEY_ONLY or
     * ALLOW_SCAN).
     */
    @Test
    public void executionMode_setsExecutionMode() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .build();

        assertThat(spec.executionMode()).isEqualTo(ExecutionMode.ALLOW_SCAN);
    }

    /**
     * Asserts that when execution mode is not set, the default is {@link ExecutionMode#STRICT_KEY_ONLY}.
     */
    @Test
    public void defaultExecutionMode_isStrictKeyOnly() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable).build();
        assertThat(spec.executionMode()).isEqualTo(ExecutionMode.STRICT_KEY_ONLY);
    }

    /**
     * Asserts that {@link QueryExpressionBuilder#limit(int)} sets {@link QueryExpressionSpec#limit()}.
     */
    @Test
    public void limit_setsLimit() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable).limit(100).build();
        assertThat(spec.limit()).isEqualTo(100);
    }

    /**
     * Asserts that a full builder chain produces a spec with all components set.
     */
    @Test
    public void fullChain_producesCompleteSpec() {
        Condition baseCond = Condition.eq("region", "EU");
        Condition joinedCond = Condition.gt("amount", 0);

        QueryExpressionSpec spec = QueryExpressionBuilder.from(baseTable)
                                                         .join(joinedTable, JoinType.LEFT, "customerId", "customerId")
                                                         .filterBase(baseCond)
                                                         .filterJoined(joinedCond)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "cnt")
                                                         .orderByAggregate("cnt", SortDirection.DESC)
                                                         .project("customerId", "name")
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .limit(10)
                                                         .build();

        assertThat(spec.baseTable()).isSameAs(baseTable);
        assertThat(spec.hasJoin()).isTrue();
        assertThat(spec.filterBase()).isSameAs(baseCond);
        assertThat(spec.filterJoined()).isSameAs(joinedCond);
        assertThat(spec.groupByAttributes()).containsExactly("customerId");
        assertThat(spec.aggregates()).hasSize(1);
        assertThat(spec.orderBy()).hasSize(1);
        assertThat(spec.projectAttributes()).containsExactly("customerId", "name");
        assertThat(spec.limit()).isEqualTo(10);
    }
}
