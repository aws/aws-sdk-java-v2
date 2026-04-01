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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.ConditionEvaluator;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.AggregateSpec;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * Default async implementation of {@link QueryExpressionAsyncEngine}. Executes base Query/Scan, optional join, and aggregation by
 * draining async publishers where needed, then applying the same in-memory rules as the sync engine.
 * <p>
 * Structure:
 * <ul>
 *   <li>{@link #execute} &rarr; {@link #executeJoinAsync}, {@link #executeAggregationAsync} or {@link #executeBaseOnlyAsync}</li>
 *   <li>Simple join: {@link #collectJoinBasePairsForAsync}, {@link #materializeJoinRowsFromCacheAsync},
 *       {@link #drainPublisherToList}; joined-side loads via {@link JoinedTableObjectMapAsyncFetcher}</li>
 *   <li>Join + aggregation: {@link #executeJoinWithAggregationAsync} with
 *       {@link #collectJoinAggregationBaseRowsAndEmptyBuckets} and {@link #processBaseRowsIntoAggBucketsAsync} or
 *       {@link #queryAndAggregateDirectAsync}</li>
 *   <li>Row-shape helpers: {@link JoinRowAliases}; aggregation/sort: {@link QueryEngineSupport}</li>
 * </ul>
 */
@SdkInternalApi
public final class DefaultQueryExpressionAsyncEngine implements QueryExpressionAsyncEngine {

    private static final Logger LOG = Logger.loggerFor(DefaultQueryExpressionAsyncEngine.class);

    private static final ExecutorService ASYNC_JOIN_EXECUTOR = Executors.newFixedThreadPool(200);

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final JoinedTableObjectMapAsyncFetcher joinFetcher;

    public DefaultQueryExpressionAsyncEngine(DynamoDbEnhancedAsyncClient enhancedClient) {
        this.enhancedClient = enhancedClient;
        this.joinFetcher = new JoinedTableObjectMapAsyncFetcher(enhancedClient.dynamoDbAsyncClient(), ASYNC_JOIN_EXECUTOR);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SdkPublisher<EnhancedQueryRow> execute(QueryExpressionSpec spec) {
        long startNanos = System.nanoTime();
        EnhancedQueryExecutionStats stats = new EnhancedQueryExecutionStats();
        SdkPublisher<EnhancedQueryRow> inner = executeInternal(spec, stats);
        return inner.doAfterOnComplete(() -> {
            long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
            logDebugStats(stats, totalMs);
            EnhancedQueryTelemetry.logExecutionComplete(stats, totalMs, true);
        });
    }

    private SdkPublisher<EnhancedQueryRow> executeInternal(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        if (spec.hasJoin()) {
            return executeJoinAsync(spec, stats);
        }
        if (!spec.aggregates().isEmpty() && !spec.groupByAttributes().isEmpty()) {
            return executeAggregationAsync(spec, stats);
        }
        return executeBaseOnlyAsync(spec, stats);
    }

    private static void logDebugStats(EnhancedQueryExecutionStats stats, long totalMs) {
        LOG.debug(() -> String.format(
            "EnhancedQuery (async) completed in %d ms; DynamoDB API requests: baseQuery=%d, baseScan=%d, joinedQuery=%d, "
            + "joinedScan=%d, total=%d",
            totalMs,
            stats.baseQueryRequestCount(),
            stats.baseScanRequestCount(),
            stats.joinedQueryRequestCount(),
            stats.joinedScanRequestCount(),
            stats.totalDynamoDbRequestCount()));
    }

    // ---- base-only --------------------------------------------------

    @SuppressWarnings("unchecked")
    private SdkPublisher<EnhancedQueryRow> executeBaseOnlyAsync(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        MappedTableResource<?> baseTable = spec.baseTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        Integer limit = spec.limit();
        int maxItems = limit != null ? limit : Integer.MAX_VALUE;

        SdkPublisher<Object> baseItems;
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
            baseItems = baseTableItems(baseTable, reqBuilder.build(), null, stats);
        } else if (spec.executionMode() == ExecutionMode.ALLOW_SCAN) {
            ScanEnhancedRequest.Builder scanBuilder = ScanEnhancedRequest.builder()
                                                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (limit != null) {
                scanBuilder.limit(limit);
            }
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                scanBuilder.attributesToProject(spec.projectAttributes());
            }
            baseItems = baseTableItems(baseTable, null, scanBuilder.build(), stats);
        } else {
            return SdkPublisher.fromIterable(Collections.emptyList());
        }
        return baseItems.flatMapIterable(item -> toBaseRows(item, baseSchema, spec))
                        .limit(maxItems);
    }

    @SuppressWarnings("unchecked")
    private SdkPublisher<Object> baseTableItems(MappedTableResource<?> baseTable,
                                                QueryEnhancedRequest queryRequest,
                                                ScanEnhancedRequest scanRequest,
                                                EnhancedQueryExecutionStats stats) {
        if (baseTable instanceof DynamoDbAsyncTable) {
            DynamoDbAsyncTable<Object> asyncTable = (DynamoDbAsyncTable<Object>) baseTable;
            if (queryRequest != null) {
                PagePublisher<Object> pages = asyncTable.query(queryRequest);
                return pages.flatMapIterable(page -> {
                    stats.addBaseQuery();
                    stats.addConsumedCapacity(page.consumedCapacity(), true, true);
                    return page.items();
                });
            }
            PagePublisher<Object> scanPages = asyncTable.scan(scanRequest);
            return scanPages.flatMapIterable(page -> {
                stats.addBaseScan();
                stats.addConsumedCapacity(page.consumedCapacity(), true, false);
                return page.items();
            });
        }
        DynamoDbTable<Object> syncTable = (DynamoDbTable<Object>) baseTable;
        if (queryRequest != null) {
            List<Object> items = new ArrayList<>();
            for (Page<Object> page : syncTable.query(queryRequest)) {
                stats.addBaseQuery();
                stats.addConsumedCapacity(page.consumedCapacity(), true, true);
                items.addAll(page.items());
            }
            return SdkPublisher.fromIterable(items);
        }
        List<Object> items = new ArrayList<>();
        for (Page<Object> page : syncTable.scan(scanRequest)) {
            stats.addBaseScan();
            stats.addConsumedCapacity(page.consumedCapacity(), true, false);
            items.addAll(page.items());
        }
        return SdkPublisher.fromIterable(items);
    }

    private static List<EnhancedQueryRow> toBaseRows(Object item, TableSchema<Object> baseSchema, QueryExpressionSpec spec) {
        Map<String, Object> objectMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(item, false));
        if (!ConditionEvaluator.evaluate(spec.where(), objectMap)) {
            return Collections.emptyList();
        }
        Map<String, Map<String, Object>> itemsByAlias =
            Collections.singletonMap(QueryEngineSupport.BASE_ALIAS, objectMap);
        return Collections.singletonList(EnhancedQueryRow.builder()
                                                         .itemsByAlias(itemsByAlias)
                                                         .build());
    }

    // ---- join (no aggregation) --------------------------------------

    @SuppressWarnings("unchecked")
    private SdkPublisher<EnhancedQueryRow> executeJoinAsync(QueryExpressionSpec spec, EnhancedQueryExecutionStats stats) {
        if (!spec.aggregates().isEmpty() && !spec.groupByAttributes().isEmpty()) {
            return executeJoinWithAggregationAsync(spec, stats);
        }

        MappedTableResource<?> baseTable = spec.baseTable();
        MappedTableResource<?> joinedTable = spec.joinedTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        String baseJoinAttr = spec.leftJoinKey();
        String joinedJoinAttr = spec.rightJoinKey();
        JoinType joinType = spec.joinType();
        Integer limit = spec.limit();

        if (spec.keyCondition() == null && spec.executionMode() != ExecutionMode.ALLOW_SCAN) {
            return SdkPublisher.fromIterable(Collections.emptyList());
        }

        QueryEnhancedRequest queryReq;
        ScanEnhancedRequest scanReq;
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
            queryReq = reqBuilder.build();
            scanReq = null;
        } else {
            int maxPage = QueryEngineSupport.MAX_BASE_PAGE_SIZE;
            int pageLimit = limit != null ? Math.min(limit, maxPage) : maxPage;
            ScanEnhancedRequest.Builder scanBuilder = ScanEnhancedRequest.builder()
                                                                         .limit(pageLimit)
                                                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                scanBuilder.attributesToProject(spec.projectAttributes());
            }
            scanReq = scanBuilder.build();
            queryReq = null;
        }
        SdkPublisher<Object> baseItems = baseTableItems(baseTable, queryReq, scanReq, stats);
        List<Object> baseItemsList = drainPublisherToList(baseItems);

        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys = new ArrayList<>();
        List<EnhancedQueryRow> rows = new ArrayList<>();
        Set<Object> keysWithBase = new HashSet<>();
        collectJoinBasePairsForAsync(baseItemsList, baseSchema, baseJoinAttr, joinType, spec, limit,
                                     baseRowsWithKeys, rows, keysWithBase);

        Set<Object> distinctKeys = baseRowsWithKeys.stream().map(Map.Entry::getValue).collect(Collectors.toSet());
        Map<Object, List<Map<String, Object>>> joinMap =
            joinFetcher.resolveAndFetchJoinedObjectMaps(joinedTable, distinctKeys, joinedJoinAttr, stats);

        materializeJoinRowsFromCacheAsync(baseRowsWithKeys, joinMap, joinType, spec, limit, rows);

        if (spec.keyCondition() == null && (joinType == JoinType.RIGHT || joinType == JoinType.FULL)) {
            addRightSideOnlyRowsAsync(rows, joinedTable, joinedJoinAttr, keysWithBase, spec, limit, stats);
        }
        int max = limit != null ? limit : Integer.MAX_VALUE;
        List<EnhancedQueryRow> result = rows.size() > max ? new ArrayList<>(rows.subList(0, max)) : rows;
        return SdkPublisher.fromIterable(result);
    }

    /**
     * Blocks until all base-table items from the async publisher are collected (same semantics as the previous inline
     * {@code subscribe(...).get()}).
     */
    private static List<Object> drainPublisherToList(SdkPublisher<Object> baseItems) {
        List<Object> baseItemsList = new ArrayList<>();
        try {
            baseItems.subscribe(baseItemsList::add).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
        return baseItemsList;
    }

    /**
     * Phase 1 of simple join: filter base rows, emit LEFT/FULL rows for null join keys, record distinct keys for fetch.
     */
    private static void collectJoinBasePairsForAsync(
        List<Object> baseItemsList,
        TableSchema<Object> baseSchema,
        String baseJoinAttr,
        JoinType joinType,
        QueryExpressionSpec spec,
        Integer limit,
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys,
        List<EnhancedQueryRow> rows,
        Set<Object> keysWithBase) {

        for (Object baseItem : baseItemsList) {
            if (limit != null && rows.size() >= limit) {
                break;
            }
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
    }

    /**
     * Phase 2: expand (base, join key) pairs using the pre-fetched joined-side cache into result rows.
     */
    private static void materializeJoinRowsFromCacheAsync(
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys,
        Map<Object, List<Map<String, Object>>> joinMap,
        JoinType joinType,
        QueryExpressionSpec spec,
        Integer limit,
        List<EnhancedQueryRow> rows) {

        for (Map.Entry<Map<String, Object>, Object> e : baseRowsWithKeys) {
            if (limit != null && rows.size() >= limit) {
                break;
            }
            Map<String, Object> baseMap = e.getKey();
            Object joinKeyValue = e.getValue();
            List<Map<String, Object>> joinedItems = joinMap.getOrDefault(joinKeyValue, Collections.emptyList());
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
                    break;
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
    }

    // ---- inline query + aggregation (for large key sets) ---------------

    /**
     * Queries the joined table per-key and aggregates items inline as they arrive. Each per-key query task produces local
     * aggregation buckets that are merged at the end. Avoids storing all joined items in memory (O(groups) instead of O(items)).
     */
    @SuppressWarnings("unchecked")
    private Map<List<Object>, Map<String, Object>> queryAndAggregateDirectAsync(
        MappedTableResource<?> joinedTable,
        Set<Object> joinKeys,
        String joinedJoinAttr,
        Map<Object, List<Map<String, Object>>> baseRowsByJoinKey,
        QueryExpressionSpec spec,
        List<String> groupByAttrs,
        List<AggregateSpec> aggregateSpecs,
        JoinType joinType,
        Integer limit,
        EnhancedQueryExecutionStats stats) {

        DynamoDbAsyncClient asyncLowLevel = enhancedClient.dynamoDbAsyncClient();
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
                        QueryResponse response;
                        try {
                            response = asyncLowLevel.query(reqBuilder.build()).get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
                        }
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
                                    localBuckets.computeIfAbsent(groupKey,
                                                                 k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs));
                                }
                                continue;
                            }
                            for (Map<String, Object> baseMap : baseRows) {
                                if (!ConditionEvaluator.evaluate(spec.where(), joinedMap, baseMap)) {
                                    continue;
                                }
                                List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                                    groupByAttrs, joinedMap, baseMap);
                                Map<String, Object> bucket = localBuckets.computeIfAbsent(
                                    groupKey,
                                    k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs));
                                QueryEngineSupport.updateBucketTwoMap(
                                    bucket, joinedMap, baseMap, aggregateSpecs);
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
            List<Future<Map<List<Object>, Map<String, Object>>>> futures = ASYNC_JOIN_EXECUTOR.invokeAll(tasks);
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
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    // ---- join + aggregation -----------------------------------------

    @SuppressWarnings("unchecked")
    private SdkPublisher<EnhancedQueryRow> executeJoinWithAggregationAsync(QueryExpressionSpec spec,
                                                                           EnhancedQueryExecutionStats stats) {
        MappedTableResource<?> baseTable = spec.baseTable();
        MappedTableResource<?> joinedTable = spec.joinedTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();
        String baseJoinAttr = spec.leftJoinKey();
        JoinType joinType = spec.joinType();
        Integer limit = spec.limit();
        List<String> groupByAttrs = spec.groupByAttributes();
        List<AggregateSpec> aggregateSpecs = spec.aggregates();

        if (spec.keyCondition() == null && spec.executionMode() != ExecutionMode.ALLOW_SCAN) {
            return SdkPublisher.fromIterable(Collections.emptyList());
        }

        // Phase 1: collect base items
        QueryEnhancedRequest queryReq = null;
        ScanEnhancedRequest scanReq = null;
        int maxPage = QueryEngineSupport.MAX_BASE_PAGE_SIZE;
        int pageLimit = limit != null ? Math.min(limit, maxPage) : maxPage;
        if (spec.keyCondition() != null) {
            queryReq = QueryEnhancedRequest.builder()
                                           .queryConditional(spec.keyCondition())
                                           .limit(pageLimit)
                                           .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                           .build();
        } else {
            scanReq = ScanEnhancedRequest.builder()
                                         .limit(pageLimit)
                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                         .build();
        }
        SdkPublisher<Object> baseItems = baseTableItems(baseTable, queryReq, scanReq, stats);
        List<Object> baseItemsList = drainPublisherToList(baseItems);

        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys = new ArrayList<>();
        Set<Object> distinctJoinKeys = new HashSet<>();
        Map<List<Object>, Map<String, Object>> emptyBuckets = new LinkedHashMap<>();
        collectJoinAggregationBaseRowsAndEmptyBuckets(
            baseItemsList, baseSchema, baseJoinAttr, joinType, spec,
            groupByAttrs, aggregateSpecs, baseRowsWithKeys, distinctJoinKeys, emptyBuckets);

        // Phase 2+3: fetch joined items and aggregate
        Integer bucketCreationLimit = spec.orderBy().isEmpty() ? limit : null;
        Map<List<Object>, Map<String, Object>> buckets;
        if (distinctJoinKeys.size() > QueryEngineSupport.INLINE_AGG_SCAN_THRESHOLD) {
            Map<Object, List<Map<String, Object>>> baseRowsByJoinKey = new HashMap<>();
            for (Map.Entry<Map<String, Object>, Object> entry : baseRowsWithKeys) {
                baseRowsByJoinKey.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                                 .add(entry.getKey());
            }
            buckets = queryAndAggregateDirectAsync(joinedTable, distinctJoinKeys, spec.rightJoinKey(),
                                                   baseRowsByJoinKey, spec, groupByAttrs, aggregateSpecs, joinType, limit, stats);
        } else {
            Map<Object, List<Map<String, Object>>> joinedObjectMaps = joinFetcher.resolveAndFetchJoinedObjectMaps(
                joinedTable, distinctJoinKeys, spec.rightJoinKey(), stats);
            buckets = processBaseRowsIntoAggBucketsAsync(
                baseRowsWithKeys, joinedObjectMaps, spec, groupByAttrs, aggregateSpecs, joinType, bucketCreationLimit);
        }

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
        return SdkPublisher.fromIterable(rows);
    }

    /**
     * Phase 1 for join+aggregation: qualifying base rows, distinct join keys, and LEFT/FULL buckets for null join keys.
     */
    private static void collectJoinAggregationBaseRowsAndEmptyBuckets(
        List<Object> baseItemsList,
        TableSchema<Object> baseSchema,
        String baseJoinAttr,
        JoinType joinType,
        QueryExpressionSpec spec,
        List<String> groupByAttrs,
        List<AggregateSpec> aggregateSpecs,
        List<Map.Entry<Map<String, Object>, Object>> baseRowsWithKeys,
        Set<Object> distinctJoinKeys,
        Map<List<Object>, Map<String, Object>> emptyBuckets) {

        for (Object baseItem : baseItemsList) {
            Map<String, Object> baseMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(baseItem, false));
            if (!ConditionEvaluator.evaluate(spec.filterBase(), baseMap)) {
                continue;
            }
            Object joinKeyValue = baseMap.get(baseJoinAttr);
            if (joinKeyValue == null) {
                if (joinType == JoinType.LEFT || joinType == JoinType.FULL) {
                    List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                        groupByAttrs, baseMap, Collections.emptyMap());
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

    private static Map<List<Object>, Map<String, Object>> processBaseRowsIntoAggBucketsAsync(
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
                        Map<String, Object> bucket = localBuckets.computeIfAbsent(
                            groupKey,
                            k -> QueryEngineSupport.createEmptyBucket(aggregateSpecs));
                        QueryEngineSupport.updateBucketTwoMap(bucket, joinedMap, baseMap, aggregateSpecs);
                    }
                }
                return localBuckets;
            });
        }

        try {
            List<Future<Map<List<Object>, Map<String, Object>>>> futures = ASYNC_JOIN_EXECUTOR.invokeAll(tasks);
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
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    // ---- RIGHT/FULL orphans -----------------------------------------

    private void addRightSideOnlyRowsAsync(List<EnhancedQueryRow> rows,
                                           MappedTableResource<?> joinedTable,
                                           String joinedJoinAttr,
                                           Set<Object> keysWithBase,
                                           QueryExpressionSpec spec,
                                           Integer limit,
                                           EnhancedQueryExecutionStats stats) {
        DynamoDbAsyncClient asyncLowLevel = enhancedClient.dynamoDbAsyncClient();
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
            ScanResponse response;
            try {
                response = asyncLowLevel.scan(reqBuilder.build()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
            }
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
    private SdkPublisher<EnhancedQueryRow> executeAggregationAsync(QueryExpressionSpec spec,
                                                                   EnhancedQueryExecutionStats stats) {
        List<EnhancedQueryRow> rows = new ArrayList<>();
        MappedTableResource<?> baseTable = spec.baseTable();
        TableSchema<Object> baseSchema = (TableSchema<Object>) baseTable.tableSchema();

        SdkPublisher<Object> baseItems;
        if (spec.keyCondition() != null) {
            QueryEnhancedRequest.Builder reqBuilder = QueryEnhancedRequest.builder()
                                                                          .queryConditional(spec.keyCondition())
                                                                          .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                reqBuilder.attributesToProject(spec.projectAttributes().toArray(new String[0]));
            }
            baseItems = baseTableItems(baseTable, reqBuilder.build(), null, stats);
        } else if (spec.executionMode() == ExecutionMode.ALLOW_SCAN) {
            ScanEnhancedRequest.Builder scanBuilder = ScanEnhancedRequest.builder()
                                                                         .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            if (spec.projectAttributes() != null && !spec.projectAttributes().isEmpty()) {
                scanBuilder.attributesToProject(spec.projectAttributes());
            }
            baseItems = baseTableItems(baseTable, null, scanBuilder.build(), stats);
        } else {
            return SdkPublisher.fromIterable(Collections.emptyList());
        }

        Map<List<Object>, Map<String, Object>> buckets = new LinkedHashMap<>();
        CompletableFuture<Void> done = baseItems.subscribe(item -> {
            Map<String, Object> objectMap = AttributeValueConversion.toObjectMap(baseSchema.itemToMap(item, false));
            if (!ConditionEvaluator.evaluate(spec.where(), objectMap)) {
                return;
            }
            List<Object> groupKey = QueryEngineSupport.buildGroupKey(
                spec.groupByAttributes(), objectMap, Collections.emptyMap());
            Map<String, Object> bucket = buckets.computeIfAbsent(
                groupKey,
                k -> QueryEngineSupport.createEmptyBucket(spec.aggregates()));
            QueryEngineSupport.updateBucket(bucket, objectMap, spec.aggregates());
        });
        try {
            done.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
        if (spec.orderBy().isEmpty()) {
            for (Map<String, Object> bucket : buckets.values()) {
                rows.add(QueryEngineSupport.aggregationRowFromBucket(
                    bucket, spec.aggregates(), spec.projectAttributes()));
            }
            Integer limit = spec.limit();
            if (limit != null && rows.size() > limit) {
                rows = new ArrayList<>(rows.subList(0, limit));
            }
        } else {
            for (Map<String, Object> bucket : buckets.values()) {
                rows.add(QueryEngineSupport.aggregationRowFromBucket(
                    bucket, spec.aggregates(), spec.projectAttributes()));
            }
            QueryEngineSupport.sortEnhancedQueryRows(rows, spec.orderBy());
            Integer limit = spec.limit();
            if (limit != null && rows.size() > limit) {
                rows = new ArrayList<>(rows.subList(0, limit));
            }
        }
        return SdkPublisher.fromIterable(rows);
    }
}
