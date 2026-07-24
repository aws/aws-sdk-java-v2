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

/**
 * Enhanced query API for the DynamoDB Enhanced Client.
 * <p>
 * Provides SQL-like join and aggregation capabilities on top of DynamoDB. Queries are described by a
 * {@link software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec} (built via
 * {@link software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder}) and executed through
 * {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient#enhancedQuery}.
 * <p>
 * Key types:
 * <ul>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder}
 *         &ndash; fluent builder for query specifications</li>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition}
 *         &ndash; in-memory filter conditions with combinators</li>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow}
 *         &ndash; a single result row (join items or aggregate values)</li>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult}
 *         &ndash; iterable result set</li>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.spec.AggregateSpec}
 *         &ndash; aggregation function specification</li>
 * </ul>
 * <p>
 * Filter evaluation phases:
 * <ul>
 *     <li>{@code filterBase} &ndash; applies to base (left) rows before join expansion</li>
 *     <li>{@code filterJoined} &ndash; applies to joined (right) rows before merge</li>
 *     <li>{@code where} &ndash; applies to final merged rows (base + joined view)</li>
 * </ul>
 * <p>
 * Execution behavior:
 * <ul>
 *     <li>{@code keyCondition} is pushed to DynamoDB {@code Query} on the base table</li>
 *     <li>With no {@code keyCondition},
 *     {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode#STRICT_KEY_ONLY}
 *         does not perform a full-table scan and returns an empty result for that query path (no exception for this case)</li>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode#ALLOW_SCAN} permits scan fallback when key
 *         conditions are absent</li>
 *     <li>{@code ExecutionMode} does not change builder validation rules; invalid query shapes (for example, join-only options
 *         used without a configured join) still fail fast at {@code build()} with {@link java.lang.IllegalStateException}</li>
 * </ul>
 * Example:
 * <pre>{@code
 * // STRICT_KEY_ONLY (default): no key condition means no scan fallback; result is empty for this path.
 * QueryExpressionSpec strict = QueryExpressionBuilder.from(customersTable)
 *     .where(Condition.eq("region", "EU"))
 *     .build();
 *
 * // ALLOW_SCAN: same shape may scan when no key condition is provided.
 * QueryExpressionSpec allowScan = QueryExpressionBuilder.from(customersTable)
 *     .executionMode(ExecutionMode.ALLOW_SCAN)
 *     .where(Condition.eq("region", "EU"))
 *     .build();
 * }</pre>
 * <p>
 * Join and aggregation notes:
 * <ul>
 *     <li>Supported join types: {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType#INNER},
 *         {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType#LEFT},
 *         {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType#RIGHT},
 *         {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType#FULL}</li>
 *     <li>{@code groupBy} requires at least one aggregate function (for example,
 *         {@link software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction#COUNT})</li>
 * </ul>
 * <p>
 * Result shape:
 * <ul>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow} may represent joined entity data or
 *         aggregate output values, depending on the query specification</li>
 *     <li>{@link software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult} provides iterable access to these
 *     rows</li>
 * </ul>
 * <p>
 * Local developer tooling:
 * <ul>
 *     <li>{@code run-enhanced-query-benchmark-local.sh} runs the benchmark harness locally and writes CSV output
 *         (defaults to {@code enhanced-query-benchmark-local.csv}, configurable via {@code BENCHMARK_OUTPUT_FILE})</li>
 *     <li>{@code run-enhanced-query-tests-and-print-timing.sh} runs enhanced-query functional tests and prints timing
 *         information to standard output</li>
 * </ul>
 * <p>
 * Best practice: use side-specific predicates in {@code filterBase}/{@code filterJoined} for early pruning, and reserve
 * {@code where} for predicates that need the merged row view.
 * <p>
 * Implementation notes (internal): default engines live under {@code ...query.internal} ({@code DefaultQueryExpressionEngine},
 * {@code DefaultQueryExpressionAsyncEngine}) with shared helpers ({@code QueryEngineSupport}, {@code JoinRowAliases},
 * joined-table fetch types).
 *
 * @see software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder
 */
package software.amazon.awssdk.enhanced.dynamodb.query;
