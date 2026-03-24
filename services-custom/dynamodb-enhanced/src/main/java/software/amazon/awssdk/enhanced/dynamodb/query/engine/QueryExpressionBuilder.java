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

package software.amazon.awssdk.enhanced.dynamodb.query.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.SortDirection;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.AggregateSpec;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.OrderBySpec;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;

/**
 * Fluent builder for {@link QueryExpressionSpec}. Provides a declarative API for constructing enhanced queries that support
 * single-table scans, cross-table joins, in-memory aggregations, filtering, ordering, and projections -- all executed
 * transparently by the Enhanced Client query engine.
 *
 * <h3>Quick reference</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>Method</th><th>Purpose</th><th>Applies to</th></tr>
 *   <tr><td>{@link #from}</td><td>Set the base (left) table</td><td>All</td></tr>
 *   <tr><td>{@link #join}</td><td>Add right table + keys</td><td>Join</td></tr>
 *   <tr><td>{@link #keyCondition}</td><td>DynamoDB key condition (server)</td><td>All</td></tr>
 *   <tr><td>{@link #where}</td><td>In-memory filter on final rows</td><td>All</td></tr>
 *   <tr><td>{@link #filterBase}</td><td>Pre-join filter on base rows</td><td>Join</td></tr>
 *   <tr><td>{@link #filterJoined}</td><td>Pre-join filter on joined rows</td><td>Join</td></tr>
 *   <tr><td>{@link #groupBy}</td><td>Group by attribute(s)</td><td>Aggregation</td></tr>
 *   <tr><td>{@link #aggregate}</td><td>SUM / COUNT / AVG / MIN / MAX</td><td>Aggregation</td></tr>
 *   <tr><td>{@link #orderBy}</td><td>Sort by row attribute</td><td>All</td></tr>
 *   <tr><td>{@link #orderByAggregate}</td><td>Sort by aggregate output</td><td>Aggregation</td></tr>
 *   <tr><td>{@link #project}</td><td>Projection pushdown</td><td>All</td></tr>
 *   <tr><td>{@link #executionMode}</td><td>STRICT_KEY_ONLY or ALLOW_SCAN</td><td>All</td></tr>
 *   <tr><td>{@link #limit}</td><td>Cap result rows / buckets</td><td>All</td></tr>
 * </table>
 *
 * <h3>Usage patterns</h3>
 *
 * <b>Single-table query with key condition:</b>
 * <pre>{@code
 * QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
 *     .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
 *     .where(Condition.eq("status", "ACTIVE"))
 *     .build();
 * }</pre>
 *
 * <b>Join + aggregation:</b>
 * <pre>{@code
 * QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
 *     .join(ordersTable, JoinType.INNER, "customerId", "customerId")
 *     .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
 *     .filterBase(Condition.eq("region", "EU"))
 *     .filterJoined(Condition.gte("amount", 50))
 *     .where(Condition.eq("status", "ACTIVE"))
 *     .groupBy("customerId")
 *     .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
 *     .orderByAggregate("totalAmount", SortDirection.DESC)
 *     .limit(100)
 *     .build();
 * }</pre>
 *
 * <b>Nested attribute filtering:</b>
 * <pre>{@code
 * QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
 *     .executionMode(ExecutionMode.ALLOW_SCAN)
 *     .where(Condition.eq("address.city", "Seattle"))
 *     .build();
 * }</pre>
 *
 * <h3>Validation</h3>
 * {@link #build()} validates option compatibility and throws {@link IllegalStateException} with a descriptive
 * message for invalid combinations (e.g. {@code groupBy} without {@code aggregate}, or {@code filterBase}
 * without a join).
 *
 * @see QueryExpressionSpec
 * @see Condition
 * @see AggregationFunction
 * @see JoinType
 */
@SdkInternalApi
public final class QueryExpressionBuilder {
    private final MappedTableResource<?> baseTable;
    private MappedTableResource<?> joinedTable;
    private JoinType joinType;
    private String leftJoinKey;
    private String rightJoinKey;
    private QueryConditional baseKeyCondition;
    private Condition condition;
    private Condition baseTableCondition;
    private Condition joinedTableCondition;
    private final List<String> groupByAttributes;
    private final List<AggregateSpec> aggregates;
    private final List<OrderBySpec> orderBy;
    private final List<String> projectAttributes;
    private ExecutionMode executionMode;
    private Integer limit;

    private QueryExpressionBuilder(MappedTableResource<?> baseTable) {
        this.baseTable = baseTable;
        this.groupByAttributes = new ArrayList<>();
        this.aggregates = new ArrayList<>();
        this.orderBy = new ArrayList<>();
        this.projectAttributes = new ArrayList<>();
    }

    /**
     * Start building an enhanced query from the given base (left) table using the synchronous API. The base table is the primary
     * data source; all queries start here.
     *
     * @param baseTable the sync DynamoDbTable to query
     * @return a new builder instance
     */
    public static QueryExpressionBuilder from(DynamoDbTable<?> baseTable) {
        return new QueryExpressionBuilder(baseTable);
    }

    /**
     * Start building an enhanced query from the given base (left) table using the asynchronous API.
     *
     * @param baseTable the async DynamoDbAsyncTable to query
     * @return a new builder instance
     */
    public static QueryExpressionBuilder from(DynamoDbAsyncTable<?> baseTable) {
        return new QueryExpressionBuilder(baseTable);
    }

    /**
     * Add a join with a second table. The engine fetches rows from the base table first, then looks up matching rows in the
     * joined table by matching {@code leftJoinKey} (on the base table) to {@code rightJoinKey} (on the joined table).
     * Semantically equivalent to SQL: {@code FROM base JOIN joined ON base.leftKey = joined.rightKey}.
     *
     * <p>Supported join types: {@link JoinType#INNER}, {@link JoinType#LEFT}, {@link JoinType#RIGHT},
     * {@link JoinType#FULL}.
     *
     * @param joinedTable  the right-side table to join
     * @param joinType     INNER, LEFT, RIGHT, or FULL
     * @param leftJoinKey  attribute name on the base table used for matching
     * @param rightJoinKey attribute name on the joined table used for matching
     * @return this builder
     */
    public QueryExpressionBuilder join(MappedTableResource<?> joinedTable, JoinType joinType,
                                       String leftJoinKey, String rightJoinKey) {
        this.joinedTable = joinedTable;
        this.joinType = joinType;
        this.leftJoinKey = leftJoinKey;
        this.rightJoinKey = rightJoinKey;
        return this;
    }

    /**
     * Sets the DynamoDB key condition for the base table. This is the <b>only server-side filter</b> in the entire enhanced query
     * -- it is pushed down to DynamoDB as the {@code KeyConditionExpression} in a {@code Query} API call.
     *
     * <p>When set, the engine calls DynamoDB {@code Query}; when absent and
     * {@link ExecutionMode#ALLOW_SCAN} is enabled, the engine falls back to {@code Scan}.
     *
     * <p>Example:
     * <pre>{@code
     * .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("customerId-1")))
     * }</pre>
     *
     * @param keyCondition the DynamoDB key condition (partition key, optionally with sort key range)
     * @return this builder
     */
    public QueryExpressionBuilder keyCondition(QueryConditional keyCondition) {
        this.baseKeyCondition = keyCondition;
        return this;
    }

    /**
     * @deprecated Use {@link #keyCondition(QueryConditional)}.
     */
    @Deprecated
    public QueryExpressionBuilder baseKeyCondition(QueryConditional baseKeyCondition) {
        return keyCondition(baseKeyCondition);
    }

    /**
     * In-memory filter applied to the <b>final</b> row set, after all joins and before aggregation.
     *
     * <ul>
     *   <li><b>Without join:</b> filters base-table items after fetch.</li>
     *   <li><b>With join:</b> filters the combined (base + joined) view. Both base and joined attributes
     *       are accessible in the condition.</li>
     * </ul>
     *
     * <p>Supports dot-path syntax for nested attributes: {@code Condition.eq("address.city", "Seattle")}.
     *
     * @param where the in-memory filter condition
     * @return this builder
     */
    public QueryExpressionBuilder where(Condition where) {
        this.condition = where;
        return this;
    }

    /**
     * @deprecated Use {@link #where(Condition)}.
     */
    @Deprecated
    public QueryExpressionBuilder withCondition(Condition condition) {
        return where(condition);
    }

    /**
     * In-memory filter applied to base (left) items <b>before</b> the join. Rows that do not match are excluded before the engine
     * looks up their counterparts in the joined table, reducing work.
     *
     * <p>Only valid when a {@link #join} is configured; calling this without a join causes
     * {@link #build()} to throw {@link IllegalStateException}. For single-table filtering, use {@link #where(Condition)}
     * instead.
     *
     * @param filterBase the pre-join filter for the base table
     * @return this builder
     */
    public QueryExpressionBuilder filterBase(Condition filterBase) {
        this.baseTableCondition = filterBase;
        return this;
    }

    /**
     * @deprecated Use {@link #filterBase(Condition)}.
     */
    @Deprecated
    public QueryExpressionBuilder withBaseTableCondition(Condition condition) {
        return filterBase(condition);
    }

    /**
     * In-memory filter applied to joined (right) items <b>before</b> they are combined with base rows. Eliminates non-matching
     * joined rows early, reducing memory and processing time.
     *
     * <p>Only valid when a {@link #join} is configured; calling this without a join causes
     * {@link #build()} to throw {@link IllegalStateException}.
     *
     * @param filterJoined the pre-join filter for the joined table
     * @return this builder
     */
    public QueryExpressionBuilder filterJoined(Condition filterJoined) {
        this.joinedTableCondition = filterJoined;
        return this;
    }

    /**
     * @deprecated Use {@link #filterJoined(Condition)}.
     */
    @Deprecated
    public QueryExpressionBuilder withJoinedTableCondition(Condition condition) {
        return filterJoined(condition);
    }

    /**
     * Group results by one or more attributes for aggregation. Each unique combination of the specified attributes becomes a
     * bucket; aggregation functions are computed per bucket. Semantically equivalent to SQL {@code GROUP BY}.
     *
     * <p>Requires at least one {@link #aggregate} call; {@link #build()} throws {@link IllegalStateException}
     * if {@code groupBy} is set without any aggregate.
     *
     * @param attributes one or more attribute names to group by
     * @return this builder
     */
    public QueryExpressionBuilder groupBy(String... attributes) {
        this.groupByAttributes.addAll(Arrays.asList(attributes));
        return this;
    }

    /**
     * Define an aggregation function to compute over the result set (or per group when {@link #groupBy} is set). Multiple
     * aggregates can be added; each produces an output accessible via {@link EnhancedQueryRow#getAggregate(String)}.
     *
     * <p>Supported functions: {@link AggregationFunction#COUNT}, {@link AggregationFunction#SUM},
     * {@link AggregationFunction#AVG}, {@link AggregationFunction#MIN}, {@link AggregationFunction#MAX}.
     *
     * @param function   the aggregation function
     * @param attribute  the source attribute name to aggregate over
     * @param outputName the alias for the result (used in {@code getAggregate(outputName)})
     * @return this builder
     */
    public QueryExpressionBuilder aggregate(AggregationFunction function, String attribute, String outputName) {
        this.aggregates.add(AggregateSpec.builder()
                                         .function(function)
                                         .attribute(attribute)
                                         .outputName(outputName)
                                         .build());
        return this;
    }

    /**
     * Sort the final result set by a row attribute. Sorting is performed in-memory after all filtering and joining. Multiple
     * {@code orderBy} / {@code orderByAggregate} calls are applied in the order specified (first call is the primary sort key).
     *
     * @param attribute the attribute name to sort by
     * @param direction {@link SortDirection#ASC} or {@link SortDirection#DESC}
     * @return this builder
     */
    public QueryExpressionBuilder orderBy(String attribute, SortDirection direction) {
        this.orderBy.add(OrderBySpec.byAttribute(attribute, direction));
        return this;
    }

    /**
     * Sort the final result set by an aggregate output value. Only meaningful when at least one {@link #aggregate} is defined.
     * The {@code aggregateOutputName} must match an alias specified in a prior {@code aggregate()} call.
     *
     * @param aggregateOutputName the aggregate alias to sort by
     * @param direction           {@link SortDirection#ASC} or {@link SortDirection#DESC}
     * @return this builder
     */
    public QueryExpressionBuilder orderByAggregate(String aggregateOutputName, SortDirection direction) {
        this.orderBy.add(OrderBySpec.byAggregate(aggregateOutputName, direction));
        return this;
    }

    /**
     * Restrict returned attributes (projection pushdown). The specified attribute names are forwarded to the DynamoDB
     * {@code ProjectionExpression}, reducing the data transferred from the service. Nested paths (e.g. {@code "address.city"})
     * are supported.
     *
     * @param attributes one or more attribute names to include in results
     * @return this builder
     */
    public QueryExpressionBuilder project(String... attributes) {
        this.projectAttributes.addAll(Arrays.asList(attributes));
        return this;
    }

    /**
     * Set the execution mode. Defaults to {@link ExecutionMode#STRICT_KEY_ONLY} which only uses DynamoDB {@code Query} and
     * {@code BatchGetItem} operations. Set to {@link ExecutionMode#ALLOW_SCAN} to permit full-table scans when no key condition
     * is provided.
     *
     * @param mode {@link ExecutionMode#STRICT_KEY_ONLY} or {@link ExecutionMode#ALLOW_SCAN}
     * @return this builder
     */
    public QueryExpressionBuilder executionMode(ExecutionMode mode) {
        this.executionMode = mode;
        return this;
    }

    /**
     * Cap the number of result rows returned. When {@link #groupBy} is used, this limits the number of aggregation buckets.
     * Applied after all filtering, joining, and aggregation.
     *
     * @param limit maximum number of rows or buckets
     * @return this builder
     */
    public QueryExpressionBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Build the immutable spec. Validates that options are consistent:
     * <ul>
     *   <li>{@code groupBy} requires at least one {@code aggregate}</li>
     *   <li>{@code filterBase} and {@code filterJoined} require a {@code join}</li>
     *   <li>{@code joinType}, {@code leftJoinKey}, {@code rightJoinKey} require a {@code joinedTable}</li>
     *   <li>{@code joinedTable} requires {@code joinType}, {@code leftJoinKey}, and {@code rightJoinKey}</li>
     *   <li>{@code baseTable} is required</li>
     * </ul>
     *
     * @throws IllegalStateException if incompatible options are specified
     */
    public QueryExpressionSpec build() {
        validate();
        return QueryExpressionSpec.builder()
                                  .baseTable(baseTable)
                                  .joinedTable(joinedTable)
                                  .joinType(joinType)
                                  .leftJoinKey(leftJoinKey)
                                  .rightJoinKey(rightJoinKey)
                                  .keyCondition(baseKeyCondition)
                                  .where(condition)
                                  .filterBase(baseTableCondition)
                                  .filterJoined(joinedTableCondition)
                                  .groupByAttributes(new ArrayList<>(groupByAttributes))
                                  .aggregates(new ArrayList<>(aggregates))
                                  .orderBy(new ArrayList<>(orderBy))
                                  .projectAttributes(new ArrayList<>(projectAttributes))
                                  .executionMode(executionMode)
                                  .limit(limit)
                                  .build();
    }

    private void validate() {
        if (baseTable == null) {
            throw new IllegalStateException("baseTable is required. Use QueryExpressionBuilder.from(table) to set it.");
        }

        boolean hasJoin = joinedTable != null;

        if (!hasJoin && joinType != null) {
            throw new IllegalStateException(
                "joinType is set but no joinedTable was provided. Call .join(table, joinType, leftKey, rightKey).");
        }
        if (!hasJoin && (leftJoinKey != null || rightJoinKey != null)) {
            throw new IllegalStateException(
                "leftJoinKey/rightJoinKey are set but no joinedTable was provided. "
                + "Call .join(table, joinType, leftKey, rightKey).");
        }
        if (hasJoin && joinType == null) {
            throw new IllegalStateException(
                "joinedTable is set but joinType is missing. Call .join(table, joinType, leftKey, rightKey).");
        }
        if (hasJoin && (leftJoinKey == null || rightJoinKey == null)) {
            throw new IllegalStateException(
                "joinedTable is set but leftJoinKey or rightJoinKey is missing. "
                + "Call .join(table, joinType, leftKey, rightKey).");
        }

        if (!hasJoin && baseTableCondition != null) {
            throw new IllegalStateException(
                "filterBase() is only applicable when a join is configured. "
                + "For single-table filtering, use where() instead.");
        }
        if (!hasJoin && joinedTableCondition != null) {
            throw new IllegalStateException(
                "filterJoined() is only applicable when a join is configured.");
        }

        if (!groupByAttributes.isEmpty() && aggregates.isEmpty()) {
            throw new IllegalStateException(
                "groupBy() requires at least one aggregate(). "
                + "Add .aggregate(function, attribute, outputName) to define an aggregation.");
        }
    }
}
