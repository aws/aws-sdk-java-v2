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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.ConditionEvaluator;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;
import software.amazon.awssdk.enhanced.dynamodb.query.result.DefaultEnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryLatencyReport;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.AggregateSpec;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * Default implementation of {@link QueryExpressionEngine}. Interprets the spec, runs base Query (or Scan when allowed), optional
 * join via Query on joined table, in-memory filter/aggregation, and produces {@link EnhancedQueryRow}s.
 * <p>
 * Structure:
 * <ul>
 *   <li>{@link #execute} &rarr; {@link #executeJoin}, {@link #executeAggregation}, or {@link #executeBaseOnly}</li>
 *   <li>Join without aggregation: {@link #executeJoin} uses {@link #mergeJoinCacheForDistinctKeys} and
 *       {@link #expandJoinPairsToRows} per page; joined-side data loads via {@link JoinedTableObjectMapSyncFetcher}</li>
 *   <li>Join + aggregation: {@link #executeJoinWithAggregation} (inline path {@link #queryAndAggregateDirect} or batched
 *       {@link #processBaseRowsIntoAggBuckets})</li>
 *   <li>Shared row-shape helpers: {@link JoinRowAliases}</li>
 *   <li>Aggregation/sort/limit: {@link QueryEngineSupport}</li>
 * </ul>
 */
@SdkInternalApi
public final class DefaultQueryExpressionEngine implements QueryExpressionEngine {

    private static final Logger LOG = Logger.loggerFor(DefaultQueryExpressionEngine.class);

    private static final ExecutorService JOIN_LOOKUP_EXECUTOR = Executors.newFixedThreadPool(200);

    private final DynamoDbEnhancedClient enhancedClient;
    private final JoinedTableObjectMapSyncFetcher joinFetcher;

    public DefaultQueryExpressionEngine(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
        this.joinFetcher = new JoinedTableObjectMapSyncFetcher(enhancedClient.dynamoDbClient(), JOIN_LOOKUP_EXECUTOR);
    }

    @Override
    public EnhancedQueryResult execute(QueryExpressionSpec spec) {
        EnhancedQueryExecutionStats stats = new EnhancedQueryExecutionStats();
        long startNanos = System.nanoTime();
        EnhancedQueryResult result = executeInternal(spec, stats);
        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        logDebugStats(stats, totalMs);
        EnhancedQueryTelemetry.logExecutionComplete(stats, totalMs, false);
        return result;
    }

    @Override
    public EnhancedQueryResult execute(QueryExpressionSpec spec,
                                       Consumer<EnhancedQueryLatencyReport> reportConsumer) {
        EnhancedQueryExecutionStats stats = new EnhancedQueryExecutionStats();
        long startNanos = System.nanoTime();
        EnhancedQueryResult result = executeInternal(spec, stats);
        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        if (reportConsumer != null) {
            reportConsumer.accept(stats.toLatencyReport(totalMs));
        }
        logDebugStats(stats, totalMs);
        EnhancedQueryTelemetry.logExecutionComplete(stats, totalMs, false);
        return result;
    }

    private EnhancedQueryResult executeInternal(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        if (spec.hasJoin()) {
            return executeJoin(spec, stats);
        }
        if (!spec.aggregates().isEmpty() && !spec.groupByAttributes().isEmpty()) {
            return executeAggregation(spec, stats);
        }
        return executeBaseOnly(spec, stats);
    }

    private static void logDebugStats(EnhancedQueryExecutionStats stats, long totalMs) {
        LOG.debug(() -> String.format(
            "EnhancedQuery completed in %d ms; DynamoDB API requests: baseQuery=%d, baseScan=%d, joinedQuery=%d, joinedScan=%d,"
            + " total=%d",
            totalMs,
            stats.baseQueryRequestCount(),
            stats.baseScanRequestCount(),
            stats.joinedQueryRequestCount(),
            stats.joinedScanRequestCount(),
            stats.totalDynamoDbRequestCount()));
    }

    // ---- base-only --------------------------------------------------

    @SuppressWarnings("unchecked")
    private EnhancedQueryResult executeBaseOnly(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        DynamoDbTable<Object> baseTable = (DynamoDbTable<Object>) spec.baseTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        Integer limit = spec.limit();

        if (spec.keyCondition() != null) {
            QueryEnhancedRequest.Builder reqBuilder = QueryEnhancedRequest.builder()
                                                                          .queryConditional(spec.keyCondition())
                                                                          .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (limit != null) {
                reqBuilder.limit(limit);
            }
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                reqBuilder.attributesToProject(spec.projectAttributes().toArray(new String[0]));
            }
            List<EnhancedQueryRow> rows = collectBaseRows(baseTable, baseSchema, reqBuilder.build(), spec, limit, stats);
            return new DefaultEnhancedQueryResult(rows);
        }

        if (spec.executionMode() == ExecutionMode.ALLOW_SCAN) {
            ScanEnhancedRequest.Builder scanBuilder = ScanEnhancedRequest.builder()
                                                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (limit != null) {
                scanBuilder.limit(limit);
            }
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                scanBuilder.attributesToProject(spec.projectAttributes());
            }
            List<EnhancedQueryRow> rows = collectBaseRowsFromScan(baseTable, baseSchema, scanBuilder.build(), spec, limit, stats);
            return new DefaultEnhancedQueryResult(rows);
        }

        return new DefaultEnhancedQueryResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private List<EnhancedQueryRow> collectBaseRows(DynamoDbTable<Object> baseTable, TableSchema<Object> baseSchema,
                                                   QueryEnhancedRequest request, QueryExpressionSpec spec,
                                                   Integer limit, EnhancedQueryExecutionStats stats) {
        List<EnhancedQueryRow> rows = new ArrayList<>();
        for (Page<Object> page : baseTable.query(request)) {
            stats.addBaseQuery();
            stats.addConsumedCapacity(page.consumedCapacity(), true, true);
            for (Object item : page.items()) {
                if (limit != null && rows.size() >= limit) {
                    return rows;
                }
                Map<String, Object> objectMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(item, false));
                if (!ConditionEvaluator.evaluate(spec.where(), objectMap)) {
                    continue;
                }
                rows.add(EnhancedQueryRow.builder()
                                         .itemsByAlias(Collections.singletonMap(QueryEngineSupport.BASE_ALIAS, objectMap))
                                         .build());
            }
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<EnhancedQueryRow> collectBaseRowsFromScan(DynamoDbTable<Object> baseTable, TableSchema<Object> baseSchema,
                                                           ScanEnhancedRequest request, QueryExpressionSpec spec,
                                                           Integer limit, EnhancedQueryExecutionStats stats) {
        List<EnhancedQueryRow> rows = new ArrayList<>();
        for (Page<Object> page : baseTable.scan(request)) {
            stats.addBaseScan();
            stats.addConsumedCapacity(page.consumedCapacity(), true, false);
            for (Object item : page.items()) {
                if (limit != null && rows.size() >= limit) {
                    return rows;
                }
                Map<String, Object> objectMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(item, false));
                if (!ConditionEvaluator.evaluate(spec.where(), objectMap)) {
                    continue;
                }
                rows.add(EnhancedQueryRow.builder()
                                         .itemsByAlias(Collections.singletonMap(QueryEngineSupport.BASE_ALIAS, objectMap))
                                         .build());
            }
        }
        return rows;
    }

    // ---- join (no aggregation) --------------------------------------

    @SuppressWarnings("unchecked")
    private EnhancedQueryResult executeJoin(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        if (!spec.aggregates().isEmpty() && !spec.groupByAttributes().isEmpty()) {
            return executeJoinWithAggregation(spec, stats);
        }

        DynamoDbTable<Object> baseTable = (DynamoDbTable<Object>) spec.baseTable();
        DynamoDbTable<Object> joinedTable = (DynamoDbTable<Object>) spec.joinedTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        String baseJoinAttr = spec.leftJoinKey();
        String joinedJoinAttr = spec.rightJoinKey();
        JoinType joinType = spec.joinType();
        Integer limit = spec.limit();

        if (spec.keyCondition() == null && spec.executionMode() != ExecutionMode.ALLOW_SCAN) {
            return new DefaultEnhancedQueryResult(Collections.emptyList());
        }

        List<EnhancedQueryRow> rows = new ArrayList<>();
        Set<Object> keysWithBase = new HashSet<>();
        Map<Object, List<Map<String, Object>>> globalJoinCache = new HashMap<>();
        Set<Object> alreadyFetchedKeys = new HashSet<>();

        if (spec.keyCondition() != null) {
            int maxPage = QueryEngineSupport.MAX_BASE_PAGE_SIZE;
            int pageLimit = limit != null ? Math.min(limit, maxPage) : maxPage;
            QueryEnhancedRequest.Builder reqBuilder = QueryEnhancedRequest.builder()
                                                                          .queryConditional(spec.keyCondition())
                                                                          .limit(pageLimit)
                                                                          .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                reqBuilder.attributesToProject(spec.projectAttributes().toArray(new String[0]));
            }
            QueryEnhancedRequest req = reqBuilder.build();
            for (Page<Object> page : baseTable.query(req)) {
                stats.addBaseQuery();
                stats.addConsumedCapacity(page.consumedCapacity(), true, true);
                List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys = new ArrayList<>();
                for (Object baseItem : page.items()) {
                    Map<String, Object> baseMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(baseItem, false));
                    if (!ConditionEvaluator.evaluate(spec.filterBase(), baseMap)) {
                        continue;
                    }
                    Object joinKeyValue = baseMap.get(baseJoinAttr);
                    if (joinKeyValue != null) {
                        keysWithBase.add(joinKeyValue);
                    }
                    if (joinKeyValue == null) {
                        if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                            rows.add(EnhancedQueryRow.builder()
                                                     .itemsByAlias(JoinRowAliases.leftOuterJoinRowWithEmptyJoined(baseMap))
                                                     .build());
                        }
                        continue;
                    }
                    baseRowsWithKeys.add(new AbstractMap.SimpleEntry<>(baseMap, joinKeyValue));
                }
                if (limit != null && rows.size() >= limit) {
                    break;
                }
                mergeJoinCacheForDistinctKeys(baseRowsWithKeys, alreadyFetchedKeys, globalJoinCache,
                                              joinedTable, joinedJoinAttr, stats);
                DefaultEnhancedQueryResult early = expandJoinPairsToRows(
                    baseRowsWithKeys, globalJoinCache, joinType, spec, limit, rows);
                if (early != null) {
                    return early;
                }
            }
        } else if (spec.executionMode() == ExecutionMode.ALLOW_SCAN) {
            int maxPage = QueryEngineSupport.MAX_BASE_PAGE_SIZE;
            int pageLimit = limit != null ? Math.min(limit, maxPage) : maxPage;
            ScanEnhancedRequest.Builder scanBuilder = ScanEnhancedRequest.builder()
                                                                         .limit(pageLimit)
                                                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                scanBuilder.attributesToProject(spec.projectAttributes());
            }
            ScanEnhancedRequest scanReq = scanBuilder.build();
            for (Page<Object> page : baseTable.scan(scanReq)) {
                stats.addBaseScan();
                stats.addConsumedCapacity(page.consumedCapacity(), true, false);
                List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys = new ArrayList<>();
                for (Object baseItem : page.items()) {
                    Map<String, Object> baseMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(baseItem, false));
                    if (!ConditionEvaluator.evaluate(spec.filterBase(), baseMap)) {
                        continue;
                    }
                    Object joinKeyValue = baseMap.get(baseJoinAttr);
                    if (joinKeyValue != null) {
                        keysWithBase.add(joinKeyValue);
                    }
                    if (joinKeyValue == null) {
                        if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                            rows.add(EnhancedQueryRow.builder()
                                                     .itemsByAlias(JoinRowAliases.leftOuterJoinRowWithEmptyJoined(baseMap))
                                                     .build());
                        }
                        continue;
                    }
                    baseRowsWithKeys.add(new AbstractMap.SimpleEntry<>(baseMap, joinKeyValue));
                }
                if (limit != null && rows.size() >= limit) {
                    break;
                }
                mergeJoinCacheForDistinctKeys(baseRowsWithKeys, alreadyFetchedKeys, globalJoinCache,
                                              joinedTable, joinedJoinAttr, stats);
                DefaultEnhancedQueryResult earlyScan = expandJoinPairsToRows(
                    baseRowsWithKeys, globalJoinCache, joinType, spec, limit, rows);
                if (earlyScan != null) {
                    return earlyScan;
                }
            }
        }

        if (spec.keyCondition() == null && (joinType == JoinType.RIGHT || joinType == JoinType.FULL)) {
            addRightSideOnlyRows(rows, spec, joinedTable, joinedJoinAttr, keysWithBase, limit, stats);
        }

        return new DefaultEnhancedQueryResult(rows);
    }

    /**
     * For keys appearing in {@code baseRowsWithKeys} that have not been fetched yet, loads joined-side object maps and merges
     * them into {@code globalJoinCache}.
     */
    private void mergeJoinCacheForDistinctKeys(
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys,
        Set<Object> alreadyFetchedKeys,
        Map<Object, List<Map<String, Object>>> globalJoinCache,
        DynamoDbTable<Object> joinedTable,
        String joinedJoinAttr,
        EnhancedQueryExecutionStats stats) {

        Set<Object> distinctKeys = baseRowsWithKeys.stream()
                                                   .map(Map.Entry::getValue).collect(Collectors.toSet());
        distinctKeys.removeAll(alreadyFetchedKeys);
        if (!distinctKeys.isEmpty()) {
            Map<Object, List<Map<String, Object>>> joinMap =
                joinFetcher.resolveAndFetchJoinedObjectMaps(joinedTable, distinctKeys, joinedJoinAttr, stats);
            globalJoinCache.putAll(joinMap);
            alreadyFetchedKeys.addAll(distinctKeys);
        }
    }

    /**
     * Materializes join result rows for one base page. Returns a finished result if the row {@code limit} is hit; otherwise
     * {@code null} so the caller continues paging.
     */
    private DefaultEnhancedQueryResult expandJoinPairsToRows(
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys,
        Map<Object, List<Map<String, Object>>> globalJoinCache,
        JoinType joinType,
        QueryExpressionSpec spec,
        Integer limit,
        List<EnhancedQueryRow> rows) {

        for (Map.Entry<Map<String, Object>, Object> e : baseRowsWithKeys) {
            if (limit != null && rows.size() >= limit) {
                return new DefaultEnhancedQueryResult(rows);
            }
            Map<String, Object> baseMap = e.getKey();
            Object joinKeyValue = e.getValue();
            List<Map<String, Object>> joinedItems = globalJoinCache.getOrDefault(joinKeyValue, Collections.emptyList());
            if (joinedItems.isEmpty()) {
                if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                    rows.add(EnhancedQueryRow.builder()
                                             .itemsByAlias(JoinRowAliases.leftOuterJoinRowWithEmptyJoined(baseMap))
                                             .build());
                }
                continue;
            }
            for (Map<String, Object> joinedMap : joinedItems) {
                if (limit != null && rows.size() >= limit) {
                    return new DefaultEnhancedQueryResult(rows);
                }
                if (!ConditionEvaluator.evaluate(spec.filterJoined(), joinedMap)) {
                    continue;
                }
                if (!ConditionEvaluator.evaluate(spec.where(), joinedMap, baseMap)) {
                    continue;
                }
                rows.add(EnhancedQueryRow.builder()
                                         .itemsByAlias(JoinRowAliases.innerJoinRow(baseMap, joinedMap))
                                         .build());
            }
        }
        return null;
    }

    // ---- inline query + aggregation (for large key sets) ---------------

    /**
     * Queries the joined table per-key and aggregates items inline as they arrive. Each per-key query task produces local
     * aggregation buckets that are merged at the end. Avoids storing all joined items in memory (O(groups) instead of O(items)),
     * eliminating GC pressure from millions of intermediate Map allocations.
     */
    @SuppressWarnings("unchecked")
    private Map<List<Object>, Map<String, Object>> queryAndAggregateDirect(
        DynamoDbTable<Object> joinedTable,
        Set<Object> joinKeys,
        String joinedJoinAttr,
        Map<Object, List<Map<String, Object>>> baseRowsByJoinKey,
        QueryExpressionSpec spec,
        List<String> groupByAttrs,
        List<AggregateSpec> aggregateSpecs,
        JoinType joinType,
        Integer limit,
        EnhancedQueryExecutionStats stats) {

        DynamoDbClient lowLevel = enhancedClient.dynamoDbClient();
        TableSchema<Object> joinedSchema = (TableSchema<Object>) joinedTable.tableSchema();
        String primaryPk = joinedSchema.tableMetadata().primaryPartitionKey();
        String indexName = primaryPk.equals(joinedJoinAttr)
                           ? null
                           : QueryEngineSupport.findIndexForAttribute(joinedSchema, joinedJoinAttr);

        List<Callable<Map<List<Object>, Map<String, Object>>>> tasks = new ArrayList<>();
        List<Object> keyList = new ArrayList<>(joinKeys);
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int chunkSize = Math.max(1, (keyList.size() + cores - 1) / cores);

        for (int start = 0; start < keyList.size(); start += chunkSize) {
            int chunkStart = start;
            int chunkEnd = Math.min(start + chunkSize, keyList.size());
            tasks.add(() -> {
                Map<List<Object>, Map<String, Object>> localBuckets = new LinkedHashMap<>();
                for (int ki = chunkStart; ki < chunkEnd; ki++) {
                    Object keyFinal = keyList.get(ki);
                    List<Map<String, Object>> baseRows = baseRowsByJoinKey.getOrDefault(
                        keyFinal, Collections.emptyList());
                    Map<String, AttributeValue> exclusiveStartKey = null;
                    do {
                        QueryRequest.Builder reqBuilder =
                            QueryRequest.builder()
                                        .tableName(joinedTable.tableName())
                                        .keyConditionExpression("#k = :v")
                                        .expressionAttributeNames(Collections.singletonMap("#k", joinedJoinAttr))
                                        .expressionAttributeValues(Collections.singletonMap(
                                            ":v", AttributeValueConversion.toKeyAttributeValue(keyFinal)))
                                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
                        if (indexName != null) {
                            reqBuilder.indexName(indexName);
                        }
                        if (exclusiveStartKey != null) {
                            reqBuilder.exclusiveStartKey(exclusiveStartKey);
                        }
                        stats.addJoinedQuery();
                        QueryResponse response =
                            lowLevel.query(reqBuilder.build());
                        stats.addConsumedCapacity(response.consumedCapacity(), false, true);
                        for (Map<String, AttributeValue> item : response.items()) {
                            Map<String, Object> joinedMap = AttributeValueConversion.toObjectMap(item);
                            if (!ConditionEvaluator.evaluate(spec.filterJoined(), joinedMap)) {
                                continue;
                            }
                            if (baseRows.isEmpty()) {
                                if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                                    List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                                        groupByAttrs, joinedMap, Collections.emptyMap());
                                    Function<List<Object>, Map<String, Object>> bucketFactory =
                                        k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs);
                                    localBuckets.computeIfAbsent(groupKey, bucketFactory);
                                }
                                continue;
                            }
                            for (Map<String, Object> baseMap : baseRows) {
                                if (!ConditionEvaluator.evaluate(spec.where(), joinedMap, baseMap)) {
                                    continue;
                                }
                                List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                                    groupByAttrs, joinedMap, baseMap);
                                Function<List<Object>, Map<String, Object>> bucketFactory =
                                    k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs);
                                Map<String, Object> bucket = localBuckets.computeIfAbsent(groupKey, bucketFactory);
                                QueryEngineSupport.updateBucketTwoMap(bucket, joinedMap, baseMap, aggregateSpecs);
                            }
                        }
                        exclusiveStartKey = response.lastEvaluatedKey().isEmpty()
                                            ? null : response.lastEvaluatedKey();
                    } while (exclusiveStartKey != null);
                }
                return localBuckets;
            });
        }

        try {
            List<Future<Map<List<Object>, Map<String, Object>>>> futures = JOIN_LOOKUP_EXECUTOR.invokeAll(tasks);
            Map<List<Object>, Map<String, Object>> merged = new LinkedHashMap<>();
            for (Future<Map<List<Object>, Map<String, Object>>> f : futures) {
                Map<List<Object>, Map<String, Object>> partial = f.get();
                for (Map.Entry<List<Object>, Map<String, Object>> e : partial.entrySet()) {
                    Map<String, Object> existing = merged.get(e.getKey());
                    if (existing == null) {
                        merged.put(e.getKey(), e.getValue());
                    } else {
                        QueryEngineSupport.mergeBucket(existing, e.getValue(), aggregateSpecs);
                    }
                }
            }
            return merged;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    // ---- join + aggregation -----------------------------------------

    @SuppressWarnings("unchecked")
    private EnhancedQueryResult executeJoinWithAggregation(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        DynamoDbTable<Object> baseTable = (DynamoDbTable<Object>) spec.baseTable();
        DynamoDbTable<Object> joinedTable = (DynamoDbTable<Object>) spec.joinedTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        String baseJoinAttr = spec.leftJoinKey();
        JoinType joinType = spec.joinType();
        Integer limit = spec.limit();
        List<String> groupByAttrs = spec.groupByAttributes();
        List<AggregateSpec> aggregateSpecs = spec.aggregates();

        if (spec.keyCondition() == null && spec.executionMode() != ExecutionMode.ALLOW_SCAN) {
            return new DefaultEnhancedQueryResult(Collections.emptyList());
        }

        // Phase 1: collect all qualifying base rows and their join keys
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys = new ArrayList<>();
        Set<Object> distinctJoinKeys = new HashSet<>();
        Map<List<Object>, Map<String, Object>> emptyBuckets = new LinkedHashMap<>();

        int maxPage = QueryEngineSupport.MAX_BASE_PAGE_SIZE;
        int pageLimit = limit != null ? Math.min(limit, maxPage) : maxPage;
        Iterable<Page<Object>> basePages;
        boolean baseUsesQuery = spec.keyCondition() != null;
        if (baseUsesQuery) {
            basePages = baseTable.query(QueryEnhancedRequest.builder()
                                                            .queryConditional(spec.keyCondition())
                                                            .limit(pageLimit)
                                                            .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                            .build());
        } else {
            basePages = baseTable.scan(ScanEnhancedRequest.builder()
                                                          .limit(pageLimit)
                                                          .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                          .build());
        }

        for (Page<Object> page : basePages) {
            if (baseUsesQuery) {
                stats.addBaseQuery();
                stats.addConsumedCapacity(page.consumedCapacity(), true, true);
            } else {
                stats.addBaseScan();
                stats.addConsumedCapacity(page.consumedCapacity(), true, false);
            }
            for (Object baseItem : page.items()) {
                Map<String, Object> baseMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(baseItem, false));
                if (!ConditionEvaluator.evaluate(spec.filterBase(), baseMap)) {
                    continue;
                }
                Object joinKeyValue = baseMap.get(baseJoinAttr);
                if (joinKeyValue == null) {
                    if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                        List<Object> groupKey = QueryEngineSupport.buildGroupKey(groupByAttrs, baseMap, Collections.emptyMap());
                        emptyBuckets.computeIfAbsent(groupKey, k -> {
                            Map<String, Object> b = QueryEngineSupport.createEmptyBucket(aggregateSpecs);
                            QueryEngineSupport.putRepresentativeBaseIfAbsent(b, baseMap);
                            return b;
                        });
                    }
                    continue;
                }
                distinctJoinKeys.add(joinKeyValue);
                baseRowsWithKeys.add(new AbstractMap.SimpleEntry<>(baseMap, joinKeyValue));
            }
        }

        // Phase 2+3: fetch joined items and aggregate
        Integer bucketCreationLimit = spec.orderBy().isEmpty() ? limit : null;
        Map<List<Object>, Map<String, Object>> buckets;
        if (distinctJoinKeys.size() > QueryEngineSupport.INLINE_AGG_SCAN_THRESHOLD) {
            Map<Object, List<Map<String, Object>>> baseRowsByJoinKey = new HashMap<>();
            for (Map.Entry<Map<String, Object>, Object> entry : baseRowsWithKeys) {
                baseRowsByJoinKey.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                                 .add(entry.getKey());
            }
            buckets = queryAndAggregateDirect(joinedTable, distinctJoinKeys, spec.rightJoinKey(),
                                              baseRowsByJoinKey, spec, groupByAttrs, aggregateSpecs, joinType, limit, stats);
        } else {
            Map<Object, List<Map<String, Object>>> joinedObjectMaps = joinFetcher.resolveAndFetchJoinedObjectMaps(
                joinedTable, distinctJoinKeys, spec.rightJoinKey(), stats);
            buckets = processBaseRowsIntoAggBuckets(
                baseRowsWithKeys, joinedObjectMaps, spec, groupByAttrs, aggregateSpecs, joinType, bucketCreationLimit);
        }

        // Merge empty-key buckets (LEFT/FULL for null join keys)
        for (Map.Entry<List<Object>, Map<String, Object>> e : emptyBuckets.entrySet()) {
            buckets.putIfAbsent(e.getKey(), e.getValue());
        }

        List<EnhancedQueryRow> rows = new ArrayList<>();
        if (spec.orderBy().isEmpty()) {
            for (Map.Entry<List<Object>, Map<String, Object>> e : buckets.entrySet()) {
                if (limit != null && rows.size() >= limit) {
                    break;
                }
                rows.add(QueryEngineSupport.aggregationRowFromBucket(
                    e.getValue(), aggregateSpecs, spec.projectAttributes()));
            }
        } else {
            for (Map.Entry<List<Object>, Map<String, Object>> e : buckets.entrySet()) {
                rows.add(QueryEngineSupport.aggregationRowFromBucket(
                    e.getValue(), aggregateSpecs, spec.projectAttributes()));
            }
            QueryEngineSupport.sortEnhancedQueryRows(rows, spec.orderBy());
            if (limit != null && rows.size() > limit) {
                rows = new ArrayList<>(rows.subList(0, limit));
            }
        }
        return new DefaultEnhancedQueryResult(rows);
    }

    /**
     * Splits base rows across available CPU cores, processes each chunk in parallel with its own local buckets, then merges the
     * partial results.
     */
    private static Map<List<Object>, Map<String, Object>> processBaseRowsIntoAggBuckets(
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys,
        Map<Object, List<Map<String, Object>>> joinedObjectMaps,
        QueryExpressionSpec spec,
        List<String> groupByAttrs,
        List<AggregateSpec> aggregateSpecs,
        JoinType joinType,
        Integer limit) {

        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int totalRows = baseRowsWithKeys.size();
        if (totalRows == 0) {
            return new LinkedHashMap<>();
        }
        int chunkSize = Math.max(1, (totalRows + cores - 1) / cores);

        List<Callable<Map<List<Object>, Map<String, Object>>>> tasks = new ArrayList<>();
        for (int start = 0; start < totalRows; start += chunkSize) {
            int chunkStart = start;
            int chunkEnd = Math.min(start + chunkSize, totalRows);
            tasks.add(() -> {
                Map<List<Object>, Map<String, Object>> localBuckets = new LinkedHashMap<>();
                for (int i = chunkStart; i < chunkEnd; i++) {
                    Map.Entry<Map<String, Object>, Object> entry = baseRowsWithKeys.get(i);
                    Map<String, Object> baseMap = entry.getKey();
                    Object joinKeyValue = entry.getValue();
                    List<Map<String, Object>> joinedMaps = joinedObjectMaps.getOrDefault(joinKeyValue,
                                                                                         Collections.emptyList());
                    if (joinedMaps.isEmpty()) {
                        if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                            List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                                groupByAttrs, baseMap, Collections.emptyMap());
                            localBuckets.computeIfAbsent(groupKey, k -> {
                                Map<String, Object> b = QueryEngineSupport.createEmptyBucket(aggregateSpecs);
                                QueryEngineSupport.putRepresentativeBaseIfAbsent(b, baseMap);
                                return b;
                            });
                        }
                        continue;
                    }
                    for (Map<String, Object> joinedMap : joinedMaps) {
                        if (!ConditionEvaluator.evaluate(spec.filterJoined(), joinedMap)) {
                            continue;
                        }
                        if (!ConditionEvaluator.evaluate(spec.where(), joinedMap, baseMap)) {
                            continue;
                        }
                        List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                            groupByAttrs, joinedMap, baseMap);
                        if (limit != null && !localBuckets.containsKey(groupKey) && localBuckets.size() >= limit) {
                            continue;
                        }
                        Function<List<Object>, Map<String, Object>> bucketFactory =
                            k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs);
                        Map<String, Object> bucket = localBuckets.computeIfAbsent(groupKey, bucketFactory);
                        QueryEngineSupport.updateBucketTwoMap(bucket, joinedMap, baseMap, aggregateSpecs);
                    }
                }
                return localBuckets;
            });
        }

        try {
            List<Future<Map<List<Object>, Map<String, Object>>>> futures = JOIN_LOOKUP_EXECUTOR.invokeAll(tasks);
            Map<List<Object>, Map<String, Object>> merged = new LinkedHashMap<>();
            for (Future<Map<List<Object>, Map<String, Object>>> f : futures) {
                Map<List<Object>, Map<String, Object>> partial = f.get();
                for (Map.Entry<List<Object>, Map<String, Object>> e : partial.entrySet()) {
                    Map<String, Object> existing = merged.get(e.getKey());
                    if (existing == null) {
                        merged.put(e.getKey(), e.getValue());
                    } else {
                        QueryEngineSupport.mergeBucket(existing, e.getValue(), aggregateSpecs);
                    }
                }
            }
            return merged;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    // ---- RIGHT/FULL orphans -----------------------------------------

    private void addRightSideOnlyRows(List<EnhancedQueryRow> rows,
                                      QueryExpressionSpec spec,
                                      DynamoDbTable<Object> joinedTable,
                                      String joinedJoinAttr,
                                      Set<Object> keysWithBase,
                                      Integer limit,
                                      EnhancedQueryExecutionStats stats) {
        DynamoDbClient lowLevel = enhancedClient.dynamoDbClient();
        String tableName = joinedTable.tableName();
        Map<String, AttributeValue> exclusiveStartKey = null;
        do {
            ScanRequest.Builder reqBuilder =
                ScanRequest.builder()
                           .tableName(tableName)
                           .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (exclusiveStartKey != null) {
                reqBuilder.exclusiveStartKey(exclusiveStartKey);
            }
            stats.addJoinedScan();
            ScanResponse response = lowLevel.scan(reqBuilder.build());
            stats.addConsumedCapacity(response.consumedCapacity(), false, false);
            for (Map<String, AttributeValue> item : response.items()) {
                if (limit != null && rows.size() >= limit) {
                    return;
                }
                Map<String, Object> joinedMap = AttributeValueConversion.toObjectMap(item);
                Object joinKey = joinedMap.get(joinedJoinAttr);
                if (joinKey != null && keysWithBase.contains(joinKey)) {
                    continue;
                }
                if (!ConditionEvaluator.evaluate(spec.filterJoined(), joinedMap)) {
                    continue;
                }
                if (!ConditionEvaluator.evaluate(spec.where(), joinedMap)) {
                    continue;
                }
                rows.add(EnhancedQueryRow.builder()
                                         .itemsByAlias(JoinRowAliases.rightOuterJoinRowWithEmptyBase(joinedMap))
                                         .build());
            }
            exclusiveStartKey = response.lastEvaluatedKey().isEmpty()
                                ? null : response.lastEvaluatedKey();
        } while (exclusiveStartKey != null);
    }

    // ---- aggregation (no join) --------------------------------------

    @SuppressWarnings("unchecked")
    private EnhancedQueryResult executeAggregation(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        DynamoDbTable<Object> baseTable = (DynamoDbTable<Object>) spec.baseTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        List<String> groupByAttrs = spec.groupByAttributes();
        List<AggregateSpec> aggregateSpecs = spec.aggregates();
        Integer limit = spec.limit();

        List<Object> items = new ArrayList<>();
        if (spec.keyCondition() != null) {
            QueryEnhancedRequest.Builder reqBuilder = QueryEnhancedRequest.builder()
                                                                          .queryConditional(spec.keyCondition())
                                                                          .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                reqBuilder.attributesToProject(spec.projectAttributes().toArray(new String[0]));
            }
            for (Page<Object> page : baseTable.query(reqBuilder.build())) {
                stats.addBaseQuery();
                stats.addConsumedCapacity(page.consumedCapacity(), true, true);
                items.addAll(page.items());
            }
        } else if (spec.executionMode() == ExecutionMode.ALLOW_SCAN) {
            ScanEnhancedRequest.Builder scanBuilder = ScanEnhancedRequest.builder()
                                                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                scanBuilder.attributesToProject(spec.projectAttributes());
            }
            for (Page<Object> page : baseTable.scan(scanBuilder.build())) {
                stats.addBaseScan();
                stats.addConsumedCapacity(page.consumedCapacity(), true, false);
                items.addAll(page.items());
            }
        }

        Map<List<Object>, Map<String, Object>> buckets = new LinkedHashMap<>();
        for (Object item : items) {
            Map<String, Object> objectMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(item, false));
            if (!ConditionEvaluator.evaluate(spec.where(), objectMap)) {
                continue;
            }
            List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                groupByAttrs, objectMap, Collections.emptyMap());
            Function<List<Object>, Map<String, Object>> bucketFactory =
                k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs);
            Map<String, Object> bucket = buckets.computeIfAbsent(groupKey, bucketFactory);
            QueryEngineSupport.updateBucket(bucket, objectMap, aggregateSpecs);
        }

        List<EnhancedQueryRow> rows = new ArrayList<>();
        if (spec.orderBy().isEmpty()) {
            for (Map<String, Object> bucket : buckets.values()) {
                if (limit != null && rows.size() >= limit) {
                    break;
                }
                rows.add(QueryEngineSupport.aggregationRowFromBucket(
                    bucket, aggregateSpecs, spec.projectAttributes()));
            }
        } else {
            for (Map<String, Object> bucket : buckets.values()) {
                rows.add(QueryEngineSupport.aggregationRowFromBucket(
                    bucket, aggregateSpecs, spec.projectAttributes()));
            }
            QueryEngineSupport.sortEnhancedQueryRows(rows, spec.orderBy());
            if (limit != null && rows.size() > limit) {
                rows = new ArrayList<>(rows.subList(0, limit));
            }
        }
        return new DefaultEnhancedQueryResult(rows);
    }
}
