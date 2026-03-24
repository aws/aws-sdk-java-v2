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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Async counterpart to {@link JoinedTableObjectMapSyncFetcher}: loads joined-table rows for join-key values using
 * DynamoDbAsyncClient. Used by the async query engine.
 */
@SdkInternalApi
public final class JoinedTableObjectMapAsyncFetcher {

    private final DynamoDbAsyncClient asyncLowLevel;
    private final ExecutorService joinExecutor;

    public JoinedTableObjectMapAsyncFetcher(DynamoDbAsyncClient asyncLowLevel, ExecutorService joinExecutor) {
        this.asyncLowLevel = asyncLowLevel;
        this.joinExecutor = joinExecutor;
    }

    /**
     * Three-tier routing mirroring the sync engine: PK match -> parallel per-key query, GSI match -> parallel per-key index
     * query, no index -> low-level parallel scan via DynamoDbAsyncClient.
     */
    @SuppressWarnings("unchecked")
    public Map<Object, List<Map<String, Object>>> resolveAndFetchJoinedObjectMaps(
        MappedTableResource<?> joinedTable, Set<Object> joinKeys, String joinedJoinAttr) {
        if (joinKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        TableSchema<Object> joinedSchema = (TableSchema<Object>) joinedTable.tableSchema();
        String primaryPk = joinedSchema.tableMetadata().primaryPartitionKey();

        if (primaryPk.equals(joinedJoinAttr)) {
            return lowLevelQueryByPk(joinedTable.tableName(), primaryPk, joinKeys);
        }

        String matchedIndex = QueryEngineSupport.findIndexForAttribute(joinedSchema, joinedJoinAttr);
        if (matchedIndex != null) {
            return lowLevelQueryByGsi(joinedTable.tableName(), matchedIndex, joinedJoinAttr, joinKeys);
        }

        return parallelScanFallback(joinedTable.tableName(), joinKeys, joinedJoinAttr);
    }

    /**
     * Low-level per-key Query path when the join attribute matches the table's primary key. Executes inline for a single key to
     * avoid thread pool overhead.
     */
    private Map<Object, List<Map<String, Object>>> lowLevelQueryByPk(
        String tableName, String pkAttr, Set<Object> joinKeys) {
        List<Callable<Map.Entry<Object, List<Map<String, Object>>>>> tasks = new ArrayList<>();
        for (Object key : joinKeys) {
            Object keyFinal = key;
            tasks.add(() -> queryByPkForKey(asyncLowLevel, tableName, pkAttr, keyFinal));
        }
        if (tasks.size() == 1) {
            return executeInline(tasks.get(0));
        }
        return executeAndCollect(tasks);
    }

    private static Map.Entry<Object, List<Map<String, Object>>> queryByPkForKey(
        DynamoDbAsyncClient client, String tableName, String pkAttr, Object keyFinal) {
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, AttributeValue> exclusiveStartKey = null;
        do {
            QueryRequest.Builder reqBuilder =
                QueryRequest.builder()
                            .tableName(tableName)
                            .keyConditionExpression("#k = :v")
                            .expressionAttributeNames(Collections.singletonMap("#k", pkAttr))
                            .expressionAttributeValues(Collections.singletonMap(
                                ":v", AttributeValueConversion.toKeyAttributeValue(keyFinal)));
            if (exclusiveStartKey != null) {
                reqBuilder.exclusiveStartKey(exclusiveStartKey);
            }
            QueryResponse response;
            try {
                response = client.query(reqBuilder.build()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
            }
            for (Map<String, AttributeValue> item : response.items()) {
                items.add(AttributeValueConversion.toObjectMap(item));
            }
            exclusiveStartKey = response.lastEvaluatedKey().isEmpty()
                                ? null : response.lastEvaluatedKey();
        } while (exclusiveStartKey != null);
        return new AbstractMap.SimpleEntry<>(keyFinal, items);
    }

    /**
     * Low-level per-key Query path when the join attribute matches a GSI partition key. Executes inline for a single key to avoid
     * thread pool overhead.
     */
    private Map<Object, List<Map<String, Object>>> lowLevelQueryByGsi(
        String tableName, String indexName, String attrName, Set<Object> joinKeys) {
        List<Callable<Map.Entry<Object, List<Map<String, Object>>>>> tasks = new ArrayList<>();
        for (Object key : joinKeys) {
            Object keyFinal = key;
            tasks.add(() -> queryByGsiForKey(asyncLowLevel, tableName, indexName, attrName, keyFinal));
        }
        if (tasks.size() == 1) {
            return executeInline(tasks.get(0));
        }
        return executeAndCollect(tasks);
    }

    private static Map.Entry<Object, List<Map<String, Object>>> queryByGsiForKey(
        DynamoDbAsyncClient client, String tableName, String indexName, String attrName, Object keyFinal) {
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, AttributeValue> exclusiveStartKey = null;
        do {
            QueryRequest.Builder reqBuilder =
                QueryRequest.builder()
                            .tableName(tableName)
                            .indexName(indexName)
                            .keyConditionExpression("#k = :v")
                            .expressionAttributeNames(Collections.singletonMap("#k", attrName))
                            .expressionAttributeValues(Collections.singletonMap(
                                ":v", AttributeValueConversion.toKeyAttributeValue(keyFinal)));
            if (exclusiveStartKey != null) {
                reqBuilder.exclusiveStartKey(exclusiveStartKey);
            }
            QueryResponse response;
            try {
                response = client.query(reqBuilder.build()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
            }
            for (Map<String, AttributeValue> item : response.items()) {
                items.add(AttributeValueConversion.toObjectMap(item));
            }
            exclusiveStartKey = response.lastEvaluatedKey().isEmpty()
                                ? null : response.lastEvaluatedKey();
        } while (exclusiveStartKey != null);
        return new AbstractMap.SimpleEntry<>(keyFinal, items);
    }

    /**
     * Low-level parallel scan using DynamoDbAsyncClient directly. Bypasses the enhanced-client bean round-trip and goes straight
     * from AV map -> Object map (one conversion instead of three).
     */
    private Map<Object, List<Map<String, Object>>> parallelScanFallback(
        String tableName, Set<Object> neededKeys, String joinedJoinAttr) {
        int totalSegments = QueryEngineSupport.PARALLEL_SCAN_SEGMENTS;

        List<Callable<Map<Object, List<Map<String, Object>>>>> tasks = new ArrayList<>();
        for (int seg = 0; seg < totalSegments; seg++) {
            int segment = seg;
            tasks.add(() -> {
                Map<Object, List<Map<String, Object>>> partial = new HashMap<>();
                Map<String, AttributeValue> exclusiveStartKey = null;
                do {
                    ScanRequest.Builder reqBuilder =
                        ScanRequest.builder()
                                   .tableName(tableName)
                                   .segment(segment)
                                   .totalSegments(totalSegments);
                    if (exclusiveStartKey != null) {
                        reqBuilder.exclusiveStartKey(exclusiveStartKey);
                    }
                    ScanResponse response;
                    try {
                        response = asyncLowLevel.scan(reqBuilder.build()).get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
                    }
                    for (Map<String, AttributeValue> item : response.items()) {
                        AttributeValue keyAv = item.get(joinedJoinAttr);
                        if (keyAv == null) {
                            continue;
                        }
                        Object keyObj = AttributeValueConversion.toObject(keyAv);
                        if (keyObj != null && neededKeys.contains(keyObj)) {
                            partial.computeIfAbsent(keyObj, k -> new ArrayList<>())
                                   .add(AttributeValueConversion.toObjectMap(item));
                        }
                    }
                    exclusiveStartKey = response.lastEvaluatedKey().isEmpty()
                                        ? null : response.lastEvaluatedKey();
                } while (exclusiveStartKey != null);
                return partial;
            });
        }

        try {
            List<Future<Map<Object, List<Map<String, Object>>>>> futures = joinExecutor.invokeAll(tasks);
            Map<Object, List<Map<String, Object>>> merged = new HashMap<>();
            for (Future<Map<Object, List<Map<String, Object>>>> f : futures) {
                for (Map.Entry<Object, List<Map<String, Object>>> e : f.get().entrySet()) {
                    List<Map<String, Object>> list = merged.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
                    list.addAll(e.getValue());
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

    private Map<Object, List<Map<String, Object>>> executeAndCollect(
        List<Callable<Map.Entry<Object, List<Map<String, Object>>>>> tasks) {
        try {
            List<Future<Map.Entry<Object, List<Map<String, Object>>>>> futures = joinExecutor.invokeAll(tasks);
            Map<Object, List<Map<String, Object>>> result = new HashMap<>();
            for (Future<Map.Entry<Object, List<Map<String, Object>>>> f : futures) {
                Map.Entry<Object, List<Map<String, Object>>> e = f.get();
                result.put(e.getKey(), e.getValue());
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    private static Map<Object, List<Map<String, Object>>> executeInline(
        Callable<Map.Entry<Object, List<Map<String, Object>>>> task) {
        try {
            Map.Entry<Object, List<Map<String, Object>>> entry = task.call();
            Map<Object, List<Map<String, Object>>> result = new HashMap<>();
            result.put(entry.getKey(), entry.getValue());
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
