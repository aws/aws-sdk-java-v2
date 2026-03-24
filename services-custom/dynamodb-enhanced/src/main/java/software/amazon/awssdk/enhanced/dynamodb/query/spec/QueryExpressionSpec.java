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

package software.amazon.awssdk.enhanced.dynamodb.query.spec;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;

/**
 * Immutable specification for an enhanced query. Contains all parameters the query engine needs to execute a single-table scan,
 * cross-table join, or aggregation.
 *
 * <p>Prefer using {@link QueryExpressionBuilder} to construct instances -- it provides validation and a
 * fluent API. This class is the internal data carrier passed to the engine.
 *
 * <h3>Field reference</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>Field</th><th>Type</th><th>Applies to</th><th>Description</th></tr>
 *   <tr><td>{@link #baseTable()}</td><td>MappedTableResource</td><td>All</td>
 *       <td>The primary (left) table to query.</td></tr>
 *   <tr><td>{@link #joinedTable()}</td><td>MappedTableResource</td><td>Join</td>
 *       <td>The secondary (right) table for joins. Null for single-table queries.</td></tr>
 *   <tr><td>{@link #joinType()}</td><td>JoinType</td><td>Join</td>
 *       <td>INNER, LEFT, RIGHT, or FULL. Defaults to INNER.</td></tr>
 *   <tr><td>{@link #leftJoinKey()}</td><td>String</td><td>Join</td>
 *       <td>Attribute on the base table used for join matching (SQL: ON base.X = joined.Y).</td></tr>
 *   <tr><td>{@link #rightJoinKey()}</td><td>String</td><td>Join</td>
 *       <td>Attribute on the joined table used for join matching.</td></tr>
 *   <tr><td>{@link #keyCondition()}</td><td>QueryConditional</td><td>All</td>
 *       <td>Server-side DynamoDB key condition. Triggers Query instead of Scan.</td></tr>
 *   <tr><td>{@link #where()}</td><td>Condition</td><td>All</td>
 *       <td>In-memory filter on the final row set (post-join, pre-aggregation).</td></tr>
 *   <tr><td>{@link #filterBase()}</td><td>Condition</td><td>Join</td>
 *       <td>In-memory filter on base rows before join.</td></tr>
 *   <tr><td>{@link #filterJoined()}</td><td>Condition</td><td>Join</td>
 *       <td>In-memory filter on joined rows before combining.</td></tr>
 *   <tr><td>{@link #groupByAttributes()}</td><td>List&lt;String&gt;</td><td>Aggregation</td>
 *       <td>Attributes to group by (SQL GROUP BY).</td></tr>
 *   <tr><td>{@link #aggregates()}</td><td>List&lt;AggregateSpec&gt;</td><td>Aggregation</td>
 *       <td>Aggregation functions (COUNT, SUM, AVG, MIN, MAX) with output aliases.</td></tr>
 *   <tr><td>{@link #orderBy()}</td><td>List&lt;OrderBySpec&gt;</td><td>All</td>
 *       <td>In-memory sort specifications.</td></tr>
 *   <tr><td>{@link #projectAttributes()}</td><td>List&lt;String&gt;</td><td>All</td>
 *       <td>Attribute projection pushed down to DynamoDB.</td></tr>
 *   <tr><td>{@link #executionMode()}</td><td>ExecutionMode</td><td>All</td>
 *       <td>STRICT_KEY_ONLY (default) or ALLOW_SCAN.</td></tr>
 *   <tr><td>{@link #limit()}</td><td>Integer</td><td>All</td>
 *       <td>Maximum result rows / aggregation buckets.</td></tr>
 * </table>
 *
 * @see QueryExpressionBuilder
 */
@SdkInternalApi
public final class QueryExpressionSpec {
    private final MappedTableResource<?> baseTable;
    private final MappedTableResource<?> joinedTable;
    private final JoinType joinType;
    private final String leftJoinKey;
    private final String rightJoinKey;
    private final QueryConditional baseKeyCondition;
    private final Condition condition;
    private final Condition baseTableCondition;
    private final Condition joinedTableCondition;
    private final List<String> groupByAttributes;
    private final List<AggregateSpec> aggregates;
    private final List<OrderBySpec> orderBy;
    private final List<String> projectAttributes;
    private final ExecutionMode executionMode;
    private final Integer limit;

    private QueryExpressionSpec(Builder b) {
        this.baseTable = b.baseTable;
        this.joinedTable = b.joinedTable;
        this.joinType = b.joinType != null ? b.joinType : JoinType.INNER;
        this.leftJoinKey = b.leftJoinKey;
        this.rightJoinKey = b.rightJoinKey;
        this.baseKeyCondition = b.baseKeyCondition;
        this.condition = b.condition;
        this.baseTableCondition = b.baseTableCondition;
        this.joinedTableCondition = b.joinedTableCondition;
        this.groupByAttributes = b.groupByAttributes == null ? Collections.emptyList() :
                                 Collections.unmodifiableList(b.groupByAttributes);
        this.aggregates = b.aggregates == null ? Collections.emptyList() : Collections.unmodifiableList(b.aggregates);
        this.orderBy = b.orderBy == null ? Collections.emptyList() : Collections.unmodifiableList(b.orderBy);
        this.projectAttributes = b.projectAttributes == null ? Collections.emptyList() :
                                 Collections.unmodifiableList(b.projectAttributes);
        this.executionMode = b.executionMode != null ? b.executionMode : ExecutionMode.STRICT_KEY_ONLY;
        this.limit = b.limit;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The primary (left) table to query. Always non-null.
     */
    public MappedTableResource<?> baseTable() {
        return baseTable;
    }

    /**
     * The secondary (right) table for joins, or null for single-table queries.
     */
    public MappedTableResource<?> joinedTable() {
        return joinedTable;
    }

    /**
     * Returns true if a joined table is configured.
     */
    public boolean hasJoin() {
        return joinedTable != null;
    }

    /**
     * The join type (INNER, LEFT, RIGHT, FULL). Defaults to INNER when not explicitly set.
     */
    public JoinType joinType() {
        return joinType;
    }

    /**
     * Join key attribute name on the left/base table. Semantically equivalent to SQL {@code ON left.<key> = right.<key>}.
     */
    public String leftJoinKey() {
        return leftJoinKey;
    }

    /**
     * Join key attribute name on the right/joined table. Semantically equivalent to SQL {@code ON left.<key> = right.<key>}.
     */
    public String rightJoinKey() {
        return rightJoinKey;
    }

    /**
     * @deprecated Use {@link #leftJoinKey()}.
     */
    @Deprecated
    public String baseJoinAttribute() {
        return leftJoinKey;
    }

    /**
     * @deprecated Use {@link #rightJoinKey()}.
     */
    @Deprecated
    public String joinedJoinAttribute() {
        return rightJoinKey;
    }

    /**
     * Optional key condition for the base table. When set, the engine uses Query with this condition; when null and
     * {@link #executionMode()} is {@link ExecutionMode#ALLOW_SCAN}, the engine may use Scan.
     */
    public QueryConditional keyCondition() {
        return baseKeyCondition;
    }

    /**
     * @deprecated Use {@link #keyCondition()}.
     */
    @Deprecated
    public QueryConditional baseKeyCondition() {
        return baseKeyCondition;
    }

    /**
     * In-memory filter applied to the final row set. Without join: filters base items. With join: filters the combined (base +
     * joined) view. Applied before aggregation.
     */
    public Condition where() {
        return condition;
    }

    /**
     * @deprecated Use {@link #where()}.
     */
    @Deprecated
    public Condition condition() {
        return condition;
    }

    /**
     * In-memory filter applied to base (left) table rows before joining. Only meaningful when a join is configured.
     */
    public Condition filterBase() {
        return baseTableCondition;
    }

    /**
     * @deprecated Use {@link #filterBase()}.
     */
    @Deprecated
    public Condition baseTableCondition() {
        return baseTableCondition;
    }

    /**
     * In-memory filter applied to joined (right) table rows before combining with base rows. Only meaningful when a join is
     * configured.
     */
    public Condition filterJoined() {
        return joinedTableCondition;
    }

    /**
     * @deprecated Use {@link #filterJoined()}.
     */
    @Deprecated
    public Condition joinedTableCondition() {
        return joinedTableCondition;
    }

    /**
     * Attributes to group by for aggregation (SQL GROUP BY). Empty list means no grouping.
     */
    public List<String> groupByAttributes() {
        return groupByAttributes;
    }

    /**
     * Aggregation function definitions (COUNT, SUM, AVG, MIN, MAX). Empty list means no aggregation.
     */
    public List<AggregateSpec> aggregates() {
        return aggregates;
    }

    /**
     * In-memory sort specifications. Applied after filtering and aggregation.
     */
    public List<OrderBySpec> orderBy() {
        return orderBy;
    }

    /**
     * Attribute names for DynamoDB projection pushdown. Empty list means all attributes.
     */
    public List<String> projectAttributes() {
        return projectAttributes;
    }

    /**
     * Execution mode: STRICT_KEY_ONLY (default) or ALLOW_SCAN.
     */
    public ExecutionMode executionMode() {
        return executionMode;
    }

    /**
     * Optional limit on number of result rows (or buckets when grouping).
     */
    public Integer limit() {
        return limit;
    }

    public static final class Builder {
        private MappedTableResource<?> baseTable;
        private MappedTableResource<?> joinedTable;
        private JoinType joinType;
        private String leftJoinKey;
        private String rightJoinKey;
        private QueryConditional baseKeyCondition;
        private Condition condition;
        private Condition baseTableCondition;
        private Condition joinedTableCondition;
        private List<String> groupByAttributes;
        private List<AggregateSpec> aggregates;
        private List<OrderBySpec> orderBy;
        private List<String> projectAttributes;
        private ExecutionMode executionMode;
        private Integer limit;

        private Builder() {
        }

        public Builder baseTable(MappedTableResource<?> baseTable) {
            this.baseTable = baseTable;
            return this;
        }

        public Builder joinedTable(MappedTableResource<?> joinedTable) {
            this.joinedTable = joinedTable;
            return this;
        }

        public Builder joinType(JoinType joinType) {
            this.joinType = joinType;
            return this;
        }

        public Builder leftJoinKey(String leftJoinKey) {
            this.leftJoinKey = leftJoinKey;
            return this;
        }

        public Builder rightJoinKey(String rightJoinKey) {
            this.rightJoinKey = rightJoinKey;
            return this;
        }

        /**
         * @deprecated Use {@link #leftJoinKey(String)}.
         */
        @Deprecated
        public Builder baseJoinAttribute(String baseJoinAttribute) {
            return leftJoinKey(baseJoinAttribute);
        }

        /**
         * @deprecated Use {@link #rightJoinKey(String)}.
         */
        @Deprecated
        public Builder joinedJoinAttribute(String joinedJoinAttribute) {
            return rightJoinKey(joinedJoinAttribute);
        }

        /**
         * Set the key condition for the base table (DynamoDB {@code Query} key condition). When set, the engine uses
         * {@code Query}; when null and {@link ExecutionMode#ALLOW_SCAN}, the engine may use {@code Scan}.
         */
        public Builder keyCondition(QueryConditional keyCondition) {
            this.baseKeyCondition = keyCondition;
            return this;
        }

        /**
         * Filter applied to the final row set in-memory.
         * <p>
         * With no join: applies to base items. With join: applies to the combined (base + joined) view.
         */
        public Builder where(Condition where) {
            this.condition = where;
            return this;
        }

        /**
         * In-memory filter applied to base (left) items before joining.
         */
        public Builder filterBase(Condition filterBase) {
            this.baseTableCondition = filterBase;
            return this;
        }

        /**
         * In-memory filter applied to joined (right) items before joining.
         */
        public Builder filterJoined(Condition filterJoined) {
            this.joinedTableCondition = filterJoined;
            return this;
        }

        /**
         * @deprecated Use {@link #keyCondition(QueryConditional)}.
         */
        @Deprecated
        public Builder baseKeyCondition(QueryConditional baseKeyCondition) {
            return keyCondition(baseKeyCondition);
        }

        /**
         * @deprecated Use {@link #where(Condition)}.
         */
        @Deprecated
        public Builder condition(Condition condition) {
            return where(condition);
        }

        /**
         * @deprecated Use {@link #filterBase(Condition)}.
         */
        @Deprecated
        public Builder baseTableCondition(Condition baseTableCondition) {
            return filterBase(baseTableCondition);
        }

        /**
         * @deprecated Use {@link #filterJoined(Condition)}.
         */
        @Deprecated
        public Builder joinedTableCondition(Condition joinedTableCondition) {
            return filterJoined(joinedTableCondition);
        }

        public Builder groupByAttributes(List<String> groupByAttributes) {
            this.groupByAttributes = groupByAttributes;
            return this;
        }

        public Builder aggregates(List<AggregateSpec> aggregates) {
            this.aggregates = aggregates;
            return this;
        }

        public Builder orderBy(List<OrderBySpec> orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder projectAttributes(List<String> projectAttributes) {
            this.projectAttributes = projectAttributes;
            return this;
        }

        public Builder executionMode(ExecutionMode executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public QueryExpressionSpec build() {
            return new QueryExpressionSpec(this);
        }
    }
}

