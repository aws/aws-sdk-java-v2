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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.InlineFilterOnlyVectorRecordBean;
import software.amazon.awssdk.enhanced.dynamodb.model.InlineFilterOnlyVectorRecordImmutable;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchResultItem;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorIndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorRecordBean;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorRecordImmutable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.VectorIndexDescription;

/**
 * Integration tests verifying that bean and immutable annotation-driven vector index schemas produce correct tables at DynamoDB
 * and support end-to-end search operations.
 *
 * <p>These tests complement the existing {@link SearchVectorsIntegrationTestBase} suite, which
 * exercises the {@code StaticTableSchema} path exclusively. The focus here is:
 * <ul>
 *     <li>No-arg {@code createTable()} derives vector indexes from annotation metadata</li>
 *     <li>{@code describeTable()} returns correct vector index configuration</li>
 *     <li>Put + search round-trip works through the annotation path</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchVectorsBeanSchemaIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {

    private static final String COSINE_INDEX = "cosine-index";
    private static final String FILTER_ONLY_INDEX = "filter-only-index";

    private static final String BEAN_SHARED_SYNC_TABLE = "JavaTests-SearchVectors-Bean-Shared-Sync";
    private static final String BEAN_SHARED_ASYNC_TABLE = "JavaTests-SearchVectors-Bean-Shared-Async";
    private static final String BEAN_INLINE_FILTER_TABLE = "JavaTests-SearchVectors-Bean-FilterOnly";

    private static final int QUICK_SEARCH_RETRY_ATTEMPTS = 3;
    private static final int TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS = 20;
    private static final int VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS = 30;
    private static final int TABLE_DELETION_WAIT_ATTEMPTS = 30;

    private static final ConcurrentHashMap<String, Object> SHARED_TABLE_SETUP_LOCKS = new ConcurrentHashMap<>();
    private static final Set<String> SEARCH_READY_TABLES = ConcurrentHashMap.newKeySet();
    private static volatile boolean beanMetadataVerified;
    private static volatile boolean inlineFilterMetadataVerified;

    private static final float[] QUERY_VECTOR = {0.95f, 0.05f, 0.0f, 0.0f};
    private static final float[] EXACT_MATCH_VECTOR = {1.0f, 0.0f, 0.0f, 0.0f};
    private static final float[] ORTHOGONAL_VECTOR = {0.0f, 1.0f, 0.0f, 0.0f};

    private static final TableSchema<VectorRecordBean> BEAN_SCHEMA =
        BeanTableSchema.create(VectorRecordBean.class);
    private static final TableSchema<VectorRecordImmutable> IMMUTABLE_SCHEMA =
        ImmutableTableSchema.create(VectorRecordImmutable.class);
    private static final TableSchema<InlineFilterOnlyVectorRecordBean> INLINE_FILTER_ONLY_BEAN_SCHEMA =
        BeanTableSchema.create(InlineFilterOnlyVectorRecordBean.class);
    private static final TableSchema<InlineFilterOnlyVectorRecordImmutable> INLINE_FILTER_ONLY_IMMUTABLE_SCHEMA =
        ImmutableTableSchema.create(InlineFilterOnlyVectorRecordImmutable.class);

    private DynamoDbClient dynamoDbClient;
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbAsyncClient asyncClient;
    private DynamoDbEnhancedAsyncClient enhancedAsyncClient;

    private DynamoDbTable<VectorRecordBean> beanSyncTable;
    private DynamoDbTable<VectorRecordImmutable> immutableSyncTable;
    private DynamoDbTable<InlineFilterOnlyVectorRecordBean> inlineFilterBeanTable;
    private DynamoDbTable<InlineFilterOnlyVectorRecordImmutable> inlineFilterImmutableTable;
    private DynamoDbAsyncTable<VectorRecordBean> beanAsyncTable;

    @BeforeAll
    void setup() {
        dynamoDbClient = createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        asyncClient = createAsyncDynamoDbClient();
        enhancedAsyncClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(asyncClient).build();

        beanSyncTable = enhancedClient.table(BEAN_SHARED_SYNC_TABLE, BEAN_SCHEMA);
        immutableSyncTable = enhancedClient.table(BEAN_SHARED_SYNC_TABLE, IMMUTABLE_SCHEMA);
        inlineFilterBeanTable = enhancedClient.table(BEAN_INLINE_FILTER_TABLE, INLINE_FILTER_ONLY_BEAN_SCHEMA);
        inlineFilterImmutableTable = enhancedClient.table(BEAN_INLINE_FILTER_TABLE, INLINE_FILTER_ONLY_IMMUTABLE_SCHEMA);
        beanAsyncTable = enhancedAsyncClient.table(BEAN_SHARED_ASYNC_TABLE, BEAN_SCHEMA);

        prepareSyncSharedBeanTable();
        prepareAsyncSharedBeanTable();
        prepareInlineFilterOnlyTable();
    }

    @AfterAll
    void teardown() {
        try {
            asyncClient.close();
        } finally {
            dynamoDbClient.close();
        }
    }

    private void prepareSyncSharedBeanTable() {
        prepareSyncSharedTable(
            dynamoDbClient,
            BEAN_SHARED_SYNC_TABLE,
            () -> beanSyncTable.createTable(),
            () -> beanSyncSeedRecords().forEach(beanSyncTable::putItem),
            () -> beanSyncTable.vectorIndex(COSINE_INDEX)
                               .searchVectorsWithResponse(beanScienceSearchRequest())
                               .results());
    }

    private void prepareAsyncSharedBeanTable() {
        prepareAsyncSharedTable(
            asyncClient,
            BEAN_SHARED_ASYNC_TABLE,
            () -> beanAsyncTable.createTable().join(),
            () -> beanAsyncSeedRecords().forEach(record -> beanAsyncTable.putItem(record).join()),
            () -> beanAsyncTable.vectorIndex(COSINE_INDEX)
                                .searchVectorsWithResponse(beanScienceSearchRequest()).join()
                                .results());
    }

    private void prepareInlineFilterOnlyTable() {
        synchronized (sharedTableSetupLock(BEAN_INLINE_FILTER_TABLE)) {
            if (SEARCH_READY_TABLES.contains(BEAN_INLINE_FILTER_TABLE)) {
                return;
            }

            Supplier<List<SearchResultItem<InlineFilterOnlyVectorRecordBean>>> search =
                () -> inlineFilterBeanTable.vectorIndex(FILTER_ONLY_INDEX)
                                           .searchVectorsWithResponse(inlineFilterScienceSearchRequest())
                                           .results();

            if (sharedTableConfigured(dynamoDbClient, BEAN_INLINE_FILTER_TABLE)) {
                if (!awaitSearchResultCount(search, 1, QUICK_SEARCH_RETRY_ATTEMPTS).isEmpty()) {
                    SEARCH_READY_TABLES.add(BEAN_INLINE_FILTER_TABLE);
                    return;
                }
                inlineFilterSeedRecords().forEach(inlineFilterBeanTable::putItem);
                if (!awaitSearchResultCount(search, 1, QUICK_SEARCH_RETRY_ATTEMPTS).isEmpty()) {
                    SEARCH_READY_TABLES.add(BEAN_INLINE_FILTER_TABLE);
                    return;
                }
                recreateSyncSharedTable(dynamoDbClient, BEAN_INLINE_FILTER_TABLE,
                                        () -> inlineFilterBeanTable.createTable());
            } else {
                inlineFilterBeanTable.createTable();
                dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(BEAN_INLINE_FILTER_TABLE));
            }

            waitForVectorIndexesActive(dynamoDbClient, BEAN_INLINE_FILTER_TABLE, 1);
            inlineFilterSeedRecords().forEach(inlineFilterBeanTable::putItem);
            awaitTableSearchReady(search, 1);
            SEARCH_READY_TABLES.add(BEAN_INLINE_FILTER_TABLE);
        }
    }

    private static void prepareSyncSharedTable(DynamoDbClient client,
                                               String tableName,
                                               Runnable createTable,
                                               Runnable seedTable,
                                               Supplier<List<SearchResultItem<VectorRecordBean>>> search) {
        synchronized (sharedTableSetupLock(tableName)) {
            prepareSharedTable(
                () -> sharedTableConfigured(client, tableName),
                () -> recreateSyncSharedTable(client, tableName, createTable),
                () -> waitForVectorIndexesActive(client, tableName, 1),
                seedTable,
                search);
            SEARCH_READY_TABLES.add(tableName);
        }
    }

    private static void prepareAsyncSharedTable(DynamoDbAsyncClient client,
                                                String tableName,
                                                Runnable createTable,
                                                Runnable seedTable,
                                                Supplier<List<SearchResultItem<VectorRecordBean>>> search) {
        synchronized (sharedTableSetupLock(tableName)) {
            prepareSharedTable(
                () -> sharedTableConfigured(client, tableName),
                () -> recreateAsyncSharedTable(client, tableName, createTable),
                () -> waitForVectorIndexesActive(client, tableName, 1),
                seedTable,
                search);
            SEARCH_READY_TABLES.add(tableName);
        }
    }

    private static Object sharedTableSetupLock(String tableName) {
        return SHARED_TABLE_SETUP_LOCKS.computeIfAbsent(tableName, ignored -> new Object());
    }

    private static void prepareSharedTable(Supplier<Boolean> tableConfigured,
                                           Runnable recreateTable,
                                           Runnable waitForIndexes,
                                           Runnable seedTable,
                                           Supplier<List<SearchResultItem<VectorRecordBean>>> search) {
        if (tableConfigured.get()) {
            if (!awaitSearchResultCount(search, 1, QUICK_SEARCH_RETRY_ATTEMPTS).isEmpty()) {
                return;
            }
            seedTable.run();
            if (!awaitSearchResultCount(search, 1, QUICK_SEARCH_RETRY_ATTEMPTS).isEmpty()) {
                return;
            }
            recreateTable.run();
            waitForIndexes.run();
            seedTable.run();
            awaitTableSearchReady(search, 1);
            return;
        }

        recreateTable.run();
        waitForIndexes.run();
        seedTable.run();
        awaitTableSearchReady(search, 1);
    }

    private static boolean sharedTableConfigured(DynamoDbClient client, String tableName) {
        if (!tableExists(client, tableName)) {
            return false;
        }
        List<VectorIndexDescription> indexes =
            client.describeTable(r -> r.tableName(tableName)).table().vectorIndexes();
        return vectorIndexesActive(indexes, 1);
    }

    private static boolean sharedTableConfigured(DynamoDbAsyncClient client, String tableName) {
        if (!tableExists(client, tableName)) {
            return false;
        }
        List<VectorIndexDescription> indexes =
            client.describeTable(r -> r.tableName(tableName)).join().table().vectorIndexes();
        return vectorIndexesActive(indexes, 1);
    }

    private static void recreateSyncSharedTable(DynamoDbClient client, String tableName, Runnable createTable) {
        if (tableExists(client, tableName)) {
            client.deleteTable(r -> r.tableName(tableName));
            waitUntilTableNotExists(client, tableName);
        }
        createTable.run();
        client.waiter().waitUntilTableExists(r -> r.tableName(tableName));
    }

    private static void recreateAsyncSharedTable(DynamoDbAsyncClient client, String tableName, Runnable createTable) {
        if (tableExists(client, tableName)) {
            client.deleteTable(r -> r.tableName(tableName)).join();
            waitUntilTableNotExists(client, tableName);
        }
        createTable.run();
        client.waiter().waitUntilTableExists(r -> r.tableName(tableName)).join();
    }

    private static void waitUntilTableNotExists(DynamoDbClient client, String tableName) {
        for (int attempt = 0; attempt < TABLE_DELETION_WAIT_ATTEMPTS; attempt++) {
            if (!tableExists(client, tableName)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw new AssertionError("Timed out waiting for table deletion: " + tableName);
    }

    private static void waitUntilTableNotExists(DynamoDbAsyncClient client, String tableName) {
        for (int attempt = 0; attempt < TABLE_DELETION_WAIT_ATTEMPTS; attempt++) {
            if (!tableExists(client, tableName)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw new AssertionError("Timed out waiting for table deletion: " + tableName);
    }

    private static <T> void awaitTableSearchReady(Supplier<List<SearchResultItem<T>>> search,
                                                  int expectedCount) {
        if (awaitSearchResultCount(search, expectedCount, TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS).size() < expectedCount) {
            throw new AssertionError("Timed out waiting for table search results after seeding");
        }
    }

    private static boolean tableExists(DynamoDbClient client, String name) {
        try {
            client.describeTable(r -> r.tableName(name));
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private static boolean tableExists(DynamoDbAsyncClient client, String name) {
        try {
            client.describeTable(r -> r.tableName(name)).join();
            return true;
        } catch (CompletionException e) {
            if (e.getCause() instanceof ResourceNotFoundException) {
                return false;
            }
            throw e;
        }
    }

    private static List<VectorRecordBean> beanSyncSeedRecords() {
        return Arrays.asList(
            beanRecord("articles", "exact-match", "science", "Exact match article", EXACT_MATCH_VECTOR),
            beanRecord("articles", "orthogonal", "science", "Orthogonal article", ORTHOGONAL_VECTOR),
            beanRecord("articles", "science-item", "science", "A science article", EXACT_MATCH_VECTOR),
            beanRecord("articles", "art-item", "art", "An art article", EXACT_MATCH_VECTOR),
            beanRecord("articles", "response-test", "science", "Response test", EXACT_MATCH_VECTOR));
    }

    private static List<VectorRecordBean> beanAsyncSeedRecords() {
        return Arrays.asList(
            beanRecord("articles", "async-exact-match", "science", "Async exact match", EXACT_MATCH_VECTOR),
            beanRecord("articles", "async-orthogonal", "science", "Async orthogonal", ORTHOGONAL_VECTOR));
    }

    private static List<InlineFilterOnlyVectorRecordBean> inlineFilterSeedRecords() {
        InlineFilterOnlyVectorRecordBean scienceItem = new InlineFilterOnlyVectorRecordBean();
        scienceItem.setPk("articles");
        scienceItem.setSk("science-item");
        scienceItem.setCategory("science");
        scienceItem.setEmbedding(toFloatList(EXACT_MATCH_VECTOR));

        InlineFilterOnlyVectorRecordBean artItem = new InlineFilterOnlyVectorRecordBean();
        artItem.setPk("articles");
        artItem.setSk("art-item");
        artItem.setCategory("art");
        artItem.setEmbedding(toFloatList(EXACT_MATCH_VECTOR));

        InlineFilterOnlyVectorRecordBean scienceReview = new InlineFilterOnlyVectorRecordBean();
        scienceReview.setPk("reviews");
        scienceReview.setSk("science-review");
        scienceReview.setCategory("science");
        scienceReview.setEmbedding(toFloatList(ORTHOGONAL_VECTOR));

        InlineFilterOnlyVectorRecordBean reviewArtItem = new InlineFilterOnlyVectorRecordBean();
        reviewArtItem.setPk("reviews");
        reviewArtItem.setSk("art-item");
        reviewArtItem.setCategory("art");
        reviewArtItem.setEmbedding(toFloatList(EXACT_MATCH_VECTOR));

        return Arrays.asList(scienceItem, artItem, scienceReview, reviewArtItem);
    }

    private static VectorRecordBean beanRecord(String pk,
                                               String sk,
                                               String category,
                                               String description,
                                               float[] embedding) {
        VectorRecordBean record = new VectorRecordBean();
        record.setPk(pk);
        record.setSk(sk);
        record.setCategory(category);
        record.setDescription(description);
        record.setEmbedding(toFloatList(embedding));
        return record;
    }

    private static SearchVectorsEnhancedRequest beanScienceSearchRequest() {
        return SearchVectorsEnhancedRequest.builder()
                                           .searchVector(QUERY_VECTOR)
                                           .topK(5)
                                           .searchConditionExpression(hashCondition("science"))
                                           .build();
    }

    private static SearchVectorsEnhancedRequest inlineFilterScienceSearchRequest() {
        return SearchVectorsEnhancedRequest.builder()
                                           .searchVector(QUERY_VECTOR)
                                           .topK(5)
                                           .searchConditionExpression(inlineFilterOnlyCondition("science"))
                                           .build();
    }

    private static Expression hashCondition(String categoryValue) {
        return Expression.builder()
                         .expression("#cat = :catval")
                         .putExpressionName("#cat", "category")
                         .putExpressionValue(":catval", AttributeValue.builder().s(categoryValue).build())
                         .build();
    }

    private static Expression hashAndFilterCondition(String categoryValue, String descriptionValue) {
        return Expression.builder()
                         .expression("#cat = :catval AND #desc = :descval")
                         .putExpressionName("#cat", "category")
                         .putExpressionName("#desc", "description")
                         .putExpressionValue(":catval", AttributeValue.builder().s(categoryValue).build())
                         .putExpressionValue(":descval", AttributeValue.builder().s(descriptionValue).build())
                         .build();
    }

    private static Expression inlineFilterOnlyCondition(String categoryValue) {
        return Expression.builder()
                         .expression("#cat = :catval")
                         .putExpressionName("#cat", "category")
                         .putExpressionValue(":catval", AttributeValue.builder().s(categoryValue).build())
                         .build();
    }

    private static List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    private static void waitForVectorIndexesActive(DynamoDbClient client, String tableName, int expectedCount) {
        for (int attempt = 0; attempt < VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS; attempt++) {
            DescribeTableResponse response = client.describeTable(r -> r.tableName(tableName));
            if (vectorIndexesActive(response, expectedCount)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw new AssertionError("Vector index(es) on table " + tableName + " did not become ACTIVE within "
                                 + VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS + " attempts");
    }

    private static void waitForVectorIndexesActive(DynamoDbAsyncClient client, String tableName, int expectedCount) {
        for (int attempt = 0; attempt < VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS; attempt++) {
            DescribeTableResponse response = client.describeTable(r -> r.tableName(tableName)).join();
            if (vectorIndexesActive(response, expectedCount)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw new AssertionError("Vector index(es) on table " + tableName + " did not become ACTIVE within "
                                 + VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS + " attempts");
    }

    private static boolean vectorIndexesActive(DescribeTableResponse response, int expectedCount) {
        List<VectorIndexDescription> indexes = response.table().vectorIndexes();
        return indexes != null
               && indexes.size() == expectedCount
               && indexes.stream().allMatch(i -> i.indexStatus() == IndexStatus.ACTIVE);
    }

    private static boolean vectorIndexesActive(List<VectorIndexDescription> indexes, int expectedCount) {
        return indexes != null
               && indexes.size() == expectedCount
               && indexes.stream().allMatch(i -> i.indexStatus() == IndexStatus.ACTIVE);
    }

    private static <T> List<SearchResultItem<T>> awaitSearchResultCount(
        Supplier<List<SearchResultItem<T>>> search, int expectedCount, int maxAttempts) {
        List<SearchResultItem<T>> results = search.get();
        for (int attempt = 0; results.size() < expectedCount && attempt < maxAttempts; attempt++) {
            sleepBetweenRetries(attempt);
            results = search.get();
        }
        return results;
    }

    private static void sleepBetweenRetries(int attempt) {
        try {
            long delay = Math.min(50L << Math.min(attempt, 4), 1_000L);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void assertBeanScoreOrdering(List<SearchResultItem<VectorRecordBean>> results,
                                                String betterMatchSk,
                                                String worseMatchSk) {
        SearchResultItem<VectorRecordBean> betterMatch = findBeanResultBySk(results, betterMatchSk);
        SearchResultItem<VectorRecordBean> worseMatch = findBeanResultBySk(results, worseMatchSk);
        assertThat(betterMatch.score()).isLessThanOrEqualTo(worseMatch.score());
    }

    private static SearchResultItem<VectorRecordBean> findBeanResultBySk(
        List<SearchResultItem<VectorRecordBean>> results, String sortKey) {
        return results.stream()
                      .filter(r -> sortKey.equals(r.item().getSk()))
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("Missing search result for sk: " + sortKey));
    }

    private static void assertImmutableScoreOrdering(List<SearchResultItem<VectorRecordImmutable>> results,
                                                     String betterMatchSk,
                                                     String worseMatchSk) {
        SearchResultItem<VectorRecordImmutable> betterMatch = findImmutableResultBySk(results, betterMatchSk);
        SearchResultItem<VectorRecordImmutable> worseMatch = findImmutableResultBySk(results, worseMatchSk);
        assertThat(betterMatch.score()).isLessThanOrEqualTo(worseMatch.score());
    }

    private static SearchResultItem<VectorRecordImmutable> findImmutableResultBySk(
        List<SearchResultItem<VectorRecordImmutable>> results, String sortKey) {
        return results.stream()
                      .filter(r -> sortKey.equals(r.item().sk()))
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("Missing search result for sk: " + sortKey));
    }

    private List<SearchResultItem<VectorRecordBean>> searchBeanVectors(SearchVectorsEnhancedRequest request) {
        if (SEARCH_READY_TABLES.contains(BEAN_SHARED_SYNC_TABLE)) {
            return beanSyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request)
                                .results();
        }
        return awaitSearchResultCount(
            () -> beanSyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request).results(),
            1,
            TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS);
    }

    private List<SearchResultItem<VectorRecordImmutable>> searchImmutableVectors(SearchVectorsEnhancedRequest request) {
        if (SEARCH_READY_TABLES.contains(BEAN_SHARED_SYNC_TABLE)) {
            return immutableSyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request).results();
        }
        return awaitSearchResultCount(
            () -> immutableSyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request).results(),
            1,
            TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS);
    }

    private List<SearchResultItem<InlineFilterOnlyVectorRecordBean>> searchInlineFilterBeanVectors(
        SearchVectorsEnhancedRequest request) {
        if (SEARCH_READY_TABLES.contains(BEAN_INLINE_FILTER_TABLE)) {
            return inlineFilterBeanTable.vectorIndex(FILTER_ONLY_INDEX).searchVectorsWithResponse(request).results();
        }
        return awaitSearchResultCount(
            () -> inlineFilterBeanTable.vectorIndex(FILTER_ONLY_INDEX).searchVectorsWithResponse(request).results(),
            1,
            TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS);
    }

    private List<SearchResultItem<InlineFilterOnlyVectorRecordImmutable>> searchInlineFilterImmutableVectors(
        SearchVectorsEnhancedRequest request) {
        if (SEARCH_READY_TABLES.contains(BEAN_INLINE_FILTER_TABLE)) {
            return inlineFilterImmutableTable.vectorIndex(FILTER_ONLY_INDEX).searchVectorsWithResponse(request).results();
        }
        return awaitSearchResultCount(
            () -> inlineFilterImmutableTable.vectorIndex(FILTER_ONLY_INDEX).searchVectorsWithResponse(request).results(),
            1,
            TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS);
    }

    private List<SearchResultItem<VectorRecordBean>> searchAsyncBeanVectors(SearchVectorsEnhancedRequest request) {
        if (SEARCH_READY_TABLES.contains(BEAN_SHARED_ASYNC_TABLE)) {
            return beanAsyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request).join().results();
        }
        return awaitSearchResultCount(
            () -> beanAsyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request).join().results(),
            1,
            TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS);
    }

    private void verifyBeanVectorIndexMetadataOnce() {
        if (!beanMetadataVerified) {
            DescribeTableResponse response = dynamoDbClient.describeTable(r -> r.tableName(BEAN_SHARED_SYNC_TABLE));
            assertThat(response.table().tableStatusAsString()).isEqualTo("ACTIVE");
            assertVectorIndexMetadata(response);
            beanMetadataVerified = true;
        }
    }

    private void verifyInlineFilterVectorIndexMetadataOnce() {
        if (!inlineFilterMetadataVerified) {
            DescribeTableResponse response = dynamoDbClient.describeTable(r -> r.tableName(BEAN_INLINE_FILTER_TABLE));
            assertThat(response.table().tableStatusAsString()).isEqualTo("ACTIVE");
            assertInlineFilterOnlyVectorIndexMetadata(response);
            inlineFilterMetadataVerified = true;
        }
    }

    private static void assertVectorIndexMetadata(DescribeTableResponse response) {
        List<VectorIndexDescription> vectorIndexes = response.table().vectorIndexes();
        assertThat(vectorIndexes).hasSize(1);

        VectorIndexDescription index = vectorIndexes.get(0);
        assertThat(index.indexName()).isEqualTo(COSINE_INDEX);
        assertThat(index.distanceFunction()).hasToString("COSINE");
        assertThat(index.dimensions()).isEqualTo(4);
        assertThat(index.vectorAttribute().attributeName()).isEqualTo("embedding");
        assertThat(index.searchSchema())
            .hasSize(2)
            .anySatisfy(e -> {
                assertThat(e.attributeName()).isEqualTo("category");
                assertThat(e.searchSchemaElementType()).hasToString("HASH");
            })
            .anySatisfy(e -> {
                assertThat(e.attributeName()).isEqualTo("description");
                assertThat(e.searchSchemaElementType()).hasToString("INLINE_FILTER");
            });
    }

    private static void assertInlineFilterOnlyVectorIndexMetadata(DescribeTableResponse response) {
        List<VectorIndexDescription> vectorIndexes = response.table().vectorIndexes();
        assertThat(vectorIndexes).hasSize(1);

        VectorIndexDescription index = vectorIndexes.get(0);
        assertThat(index.indexName()).isEqualTo(FILTER_ONLY_INDEX);
        assertThat(index.distanceFunction()).hasToString("COSINE");
        assertThat(index.dimensions()).isEqualTo(4);
        assertThat(index.vectorAttribute().attributeName()).isEqualTo("embedding");
        assertThat(index.searchSchema())
            .hasSize(1)
            .allSatisfy(e -> {
                assertThat(e.attributeName()).isEqualTo("category");
                assertThat(e.searchSchemaElementType()).hasToString("INLINE_FILTER");
            });
    }

    // Bean schema tests

    @Test
    void noArgCreateTable_fromBeanAnnotations_createsCorrectVectorIndex() {
        verifyBeanVectorIndexMetadataOnce();
    }

    @Test
    void putAndSearch_withBeanSchema_returnsOrderedResults() {
        SearchVectorsEnhancedRequest request = beanScienceSearchRequest();

        List<SearchResultItem<VectorRecordBean>> results = searchBeanVectors(request);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertBeanScoreOrdering(results, "exact-match", "orthogonal");
    }

    @Test
    void searchWithHashAndInlineFilter_withBeanSchema_filtersCorrectly() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(QUERY_VECTOR)
                                                                           .topK(5)
                                                                           .searchConditionExpression(
                                                                               hashAndFilterCondition("science",
                                                                                                      "A science article"))
                                                                           .build();

        List<SearchResultItem<VectorRecordBean>> results = searchBeanVectors(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).item().getSk()).isEqualTo("science-item");
        assertThat(results.get(0).item().getDescription()).isEqualTo("A science article");
    }

    @Test
    void vectorIndexHandle_fromBeanTable_supportsSearchWithResponse() {
        SearchVectorsEnhancedRequest request = beanScienceSearchRequest();

        List<SearchResultItem<VectorRecordBean>> results =
            beanSyncTable.vectorIndex(COSINE_INDEX).searchVectorsWithResponse(request).results();

        assertThat(results.stream().map(r -> r.item().getSk())).contains("response-test");
    }

    // Immutable schema tests

    @Test
    void noArgCreateTable_fromImmutableAnnotations_createsCorrectVectorIndex() {
        verifyBeanVectorIndexMetadataOnce();
    }

    @Test
    void putAndSearch_withImmutableSchema_returnsOrderedResults() {
        SearchVectorsEnhancedRequest request = beanScienceSearchRequest();

        List<SearchResultItem<VectorRecordImmutable>> results = searchImmutableVectors(request);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertImmutableScoreOrdering(results, "exact-match", "orthogonal");
    }

    @Test
    void searchWithHashAndInlineFilter_withImmutableSchema_filtersCorrectly() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(QUERY_VECTOR)
                                                                           .topK(5)
                                                                           .searchConditionExpression(
                                                                               hashAndFilterCondition("science",
                                                                                                      "A science article"))
                                                                           .build();

        List<SearchResultItem<VectorRecordImmutable>> results = searchImmutableVectors(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).item().sk()).isEqualTo("science-item");
        assertThat(results.get(0).item().description()).isEqualTo("A science article");
    }

    // Inline filter only schema tests

    @Test
    void noArgCreateTable_fromBeanAnnotations_createsInlineFilterOnlyVectorIndex() {
        verifyInlineFilterVectorIndexMetadataOnce();
    }

    @Test
    void noArgCreateTable_fromImmutableAnnotations_createsInlineFilterOnlyVectorIndex() {
        verifyInlineFilterVectorIndexMetadataOnce();
    }

    @Test
    void putAndSearch_withBeanSchema_inlineFilterOnly_returnsResults() {
        SearchVectorsEnhancedRequest request = inlineFilterScienceSearchRequest();

        List<SearchResultItem<InlineFilterOnlyVectorRecordBean>> results =
            searchInlineFilterBeanVectors(request);

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(r -> r.item().getSk()))
            .containsExactlyInAnyOrder("science-item", "science-review");
        assertThat(results).allMatch(r -> "science".equals(r.item().getCategory()));
    }

    @Test
    void putAndSearch_withImmutableSchema_inlineFilterOnly_returnsResults() {
        SearchVectorsEnhancedRequest request = inlineFilterScienceSearchRequest();

        List<SearchResultItem<InlineFilterOnlyVectorRecordImmutable>> results =
            searchInlineFilterImmutableVectors(request);

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(r -> r.item().sk()))
            .containsExactlyInAnyOrder("science-item", "science-review");
        assertThat(results).allMatch(r -> "science".equals(r.item().category()));
    }

    @Test
    void searchWithInlineFilterCondition_withoutHashKey_returnsResultsAcrossPartitions() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(QUERY_VECTOR)
                                                                           .topK(10)
                                                                           .searchConditionExpression(
                                                                               inlineFilterOnlyCondition("science"))
                                                                           .build();

        List<SearchResultItem<InlineFilterOnlyVectorRecordBean>> results =
            searchInlineFilterBeanVectors(request);

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(r -> r.item().getPk()).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder("articles", "reviews");
    }

    // Async annotated schema tests

    @Test
    void noArgCreateTable_asyncBeanSchema_createsCorrectVectorIndex() {
        DescribeTableResponse response = asyncClient.describeTable(r -> r.tableName(BEAN_SHARED_ASYNC_TABLE)).join();
        assertThat(response.table().tableStatusAsString()).isEqualTo("ACTIVE");
        assertVectorIndexMetadata(response);
    }

    @Test
    void putAndSearch_asyncBeanSchema_returnsResults() {
        SearchVectorsEnhancedRequest request = beanScienceSearchRequest();

        List<SearchResultItem<VectorRecordBean>> results = searchAsyncBeanVectors(request);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertBeanScoreOrdering(results, "async-exact-match", "async-orthogonal");
    }

    // Schema metadata consistency tests

    @Test
    void beanAndImmutableSchemas_produceEquivalentVectorIndexMetadata() {
        Collection<VectorIndexMetadata> beanMetadata = BEAN_SCHEMA.tableMetadata().vectorIndices();
        Collection<VectorIndexMetadata> immutableMetadata = IMMUTABLE_SCHEMA.tableMetadata().vectorIndices();

        assertThat(beanMetadata).hasSize(1);
        assertThat(immutableMetadata).hasSize(1);

        VectorIndexMetadata beanIndex = beanMetadata.iterator().next();
        VectorIndexMetadata immutableIndex = immutableMetadata.iterator().next();

        assertThat(beanIndex.indexName()).isEqualTo(immutableIndex.indexName()).isEqualTo(COSINE_INDEX);
        assertThat(beanIndex.vectorAttributeName()).isEqualTo(immutableIndex.vectorAttributeName()).isEqualTo("embedding");
        assertThat(beanIndex.dimensions()).isEqualTo(immutableIndex.dimensions()).isEqualTo(4);
        assertThat(beanIndex.distanceFunction()).isEqualTo(immutableIndex.distanceFunction()).isEqualTo(DistanceFunction.COSINE);
        assertThat(beanIndex.searchSchemaElements()).hasSameSizeAs(immutableIndex.searchSchemaElements());
    }

    @Test
    void beanSchema_vectorIndexNotInSecondaryIndices() {
        assertThat(BEAN_SCHEMA.tableMetadata().indices().stream()
                              .filter(i -> COSINE_INDEX.equals(i.name()))
                              .findAny()).isEmpty();

        assertThat(BEAN_SCHEMA.tableMetadata().vectorIndices().stream()
                              .filter(vi -> COSINE_INDEX.equals(vi.indexName()))
                              .findAny()).isPresent();
    }

    @Test
    void immutableSchema_vectorIndexNotInSecondaryIndices() {
        assertThat(IMMUTABLE_SCHEMA.tableMetadata().indices().stream()
                                   .filter(i -> COSINE_INDEX.equals(i.name()))
                                   .findAny()).isEmpty();

        assertThat(IMMUTABLE_SCHEMA.tableMetadata().vectorIndices().stream()
                                   .filter(vi -> COSINE_INDEX.equals(vi.indexName()))
                                   .findAny()).isPresent();
    }
}
