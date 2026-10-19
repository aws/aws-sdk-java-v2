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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElementType.HASH;
import static software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElementType.INLINE_FILTER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchResultItem;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorCreateTableLimitRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;
import software.amazon.awssdk.services.dynamodb.model.VectorIndexDescription;

public abstract class SearchVectorsIntegrationTestBase extends DynamoDbEnhancedIntegrationTestBase {


    protected static final String COSINE_INDEX = "cosine-index";
    protected static final String DOT_PRODUCT_INDEX = "dot-product-index";
    protected static final String EUCLIDEAN_INDEX = "euclidean-index";
    protected static final String TABLE_WIDE_INDEX = "table-wide-index";

    protected static final String SHARED_SYNC_TABLE_NAME =
        "JavaTests-SearchVectors-Shared-Sync";
    protected static final String SHARED_ASYNC_TABLE_NAME =
        "JavaTests-SearchVectors-Shared-Async";
    protected static final String NOHASH_TABLE_NAME =
        "JavaTests-SearchVectors-NoHash";
    protected static final String DIMENSION_TABLE_PREFIX =
        "JavaTests-SearchVectors-Dim";
    protected static final String FILTER_ONLY_TABLE_NAME =
        "JavaTests-SearchVectors-FilterOnly";

    private static final int DEFAULT_SEARCH_RETRY_ATTEMPTS = 15;
    private static final int QUICK_SEARCH_RETRY_ATTEMPTS = 3;
    private static final int TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS = 20;
    private static final int VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS = 30;
    private static final int TABLE_DELETION_WAIT_ATTEMPTS = 30;

    private static final ConcurrentHashMap<String, Object> SHARED_TABLE_SETUP_LOCKS = new ConcurrentHashMap<>();
    private static final Set<String> PREPARED_TABLES = ConcurrentHashMap.newKeySet();
    private static final Set<String> SEARCH_READY_TABLES = ConcurrentHashMap.newKeySet();
    private static final Object DEDICATED_TABLE_PREP_LOCK = new Object();
    private static volatile boolean dedicatedTablesPrepared;

    protected static final float[] EXACT_MATCH_VECTOR = {1.0f, 0.0f, 0.0f, 0.0f};
    protected static final float[] NEAR_MATCH_VECTOR = {0.9f, 0.1f, 0.0f, 0.0f};
    protected static final float[] ORTHOGONAL_1_VECTOR = {0.0f, 1.0f, 0.0f, 0.0f};
    protected static final float[] ORTHOGONAL_2_VECTOR = {0.0f, 0.0f, 1.0f, 0.0f};
    protected static final float[] DIFF_PARTITION_VECTOR = {0.5f, 0.5f, 0.5f, 0.5f};
    protected static final float[] QUERY_VECTOR = {0.95f, 0.05f, 0.0f, 0.0f};
    protected static final float[] ALTERNATE_QUERY_VECTOR = {0.0f, 1.0f, 0.0f, 0.0f};
    protected static final float[] DOT_PRODUCT_QUERY_VECTOR = {1.0f, 0.0f, 0.0f, 0.0f};

    protected static final List<EnhancedVectorIndex> VECTOR_INDEX_DEFINITIONS = Arrays.asList(
        EnhancedVectorIndex.builder()
                           .indexName(COSINE_INDEX)
                           .vectorAttributeName("embeddingCosine")
                           .distanceFunction(DistanceFunction.COSINE)
                           .dimensions(4)
                           .projection(p -> p.projectionType(ProjectionType.ALL))
                           .addSearchSchemaElement(b -> b.attributeName("pk")
                                                         .searchSchemaElementType(HASH))
                           .addSearchSchemaElement(b -> b.attributeName("category")
                                                         .searchSchemaElementType(INLINE_FILTER))
                           .build(),

        EnhancedVectorIndex.builder()
                           .indexName(DOT_PRODUCT_INDEX)
                           .vectorAttributeName("embeddingDot")
                           .distanceFunction(DistanceFunction.DOT_PRODUCT)
                           .dimensions(4)
                           .projection(p -> p.projectionType(ProjectionType.KEYS_ONLY))
                           .addSearchSchemaElement(b -> b.attributeName("pk")
                                                         .searchSchemaElementType(HASH))
                           .build(),

        EnhancedVectorIndex.builder()
                           .indexName(EUCLIDEAN_INDEX)
                           .vectorAttributeName("embeddingEuclidean")
                           .distanceFunction(DistanceFunction.EUCLIDEAN)
                           .dimensions(4)
                           .projection(p -> p.projectionType(ProjectionType.INCLUDE)
                                             .nonKeyAttributes("category"))
                           .addSearchSchemaElement(b -> b.attributeName("pk")
                                                         .searchSchemaElementType(HASH))
                           .build()
    );

    protected static final StaticTableSchema<VectorRecord> VECTOR_TABLE_SCHEMA =
        StaticTableSchema.builder(VectorRecord.class)
                         .newItemSupplier(VectorRecord::new)
                         .addAttribute(String.class, a -> a.name("pk")
                                                           .getter(VectorRecord::getPk)
                                                           .setter(VectorRecord::setPk)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sk")
                                                           .getter(VectorRecord::getSk)
                                                           .setter(VectorRecord::setSk)
                                                           .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("category")
                                                           .getter(VectorRecord::getCategory)
                                                           .setter(VectorRecord::setCategory))
                         .addAttribute(String.class, a -> a.name("description")
                                                           .getter(VectorRecord::getDescription)
                                                           .setter(VectorRecord::setDescription))
                         .addAttribute(EnhancedType.listOf(Float.class), a -> a.name("embeddingCosine")
                                                                               .getter(VectorRecord::getEmbeddingCosine)
                                                                               .setter(VectorRecord::setEmbeddingCosine))
                         .addAttribute(EnhancedType.listOf(Float.class), a -> a.name("embeddingDot")
                                                                               .getter(VectorRecord::getEmbeddingDot)
                                                                               .setter(VectorRecord::setEmbeddingDot))
                         .addAttribute(EnhancedType.listOf(Float.class), a -> a.name("embeddingEuclidean")
                                                                               .getter(VectorRecord::getEmbeddingEuclidean)
                                                                               .setter(VectorRecord::setEmbeddingEuclidean))
                         .tags(StaticAttributeTags.vectorIndexes(VECTOR_INDEX_DEFINITIONS))
                         .build();

    private static final Map<Boolean, DocumentTableSchema> DOCUMENT_SCHEMA_CACHE = new HashMap<>();

    protected abstract String sharedTableName();

    protected abstract DescribeTableResponse describeTable(String tableName);

    protected abstract List<SearchResultItem<VectorRecord>> executeSearch(String index, SearchVectorsEnhancedRequest req);

    protected abstract void executePut(VectorRecord record);

    protected abstract void executeDelete(Key key);

    protected abstract List<SearchResultItem<EnhancedDocument>> executeSearchDocument(String index,
                                                                                      SearchVectorsEnhancedRequest req,
                                                                                      DocumentTableSchema schema);

    protected abstract SearchVectorsEnhancedResponse<VectorRecord> executeSearchWithResponse(String index,
                                                                                             SearchVectorsEnhancedRequest req);

    protected abstract List<SearchResultItem<VectorRecord>> executeSearchWithBuilder(
        String index, Consumer<SearchVectorsEnhancedRequest.Builder> builder);

    protected abstract SearchVectorsEnhancedResponse<VectorRecord> executeSearchWithResponseBuilder(
        String index, Consumer<SearchVectorsEnhancedRequest.Builder> builder);

    protected abstract void executeTempTableCreate(String tempTableName,
                                                   TableSchema<?> schema,
                                                   CreateTableEnhancedRequest request);

    protected abstract void createAndWaitForDedicatedTable(String tableName,
                                                           StaticTableSchema<VectorRecord> schema,
                                                           List<EnhancedVectorIndex> indexes);

    protected abstract void deleteDedicatedTable(String tableName);

    protected abstract void dedicatedPutItem(String tableName,
                                             StaticTableSchema<VectorRecord> schema,
                                             VectorRecord record);

    protected abstract VectorRecord dedicatedGetItem(String tableName,
                                                     StaticTableSchema<VectorRecord> schema,
                                                     Key key);

    protected abstract List<SearchResultItem<VectorRecord>> dedicatedSearch(String tableName,
                                                                            StaticTableSchema<VectorRecord> schema,
                                                                            String indexName,
                                                                            SearchVectorsEnhancedRequest request);

    protected abstract SearchVectorsResponse executeLowLevelSearchVectors(SearchVectorsRequest request);

    protected interface DedicatedTableContext {
        String tableName();

        void putItem(VectorRecord record);

        VectorRecord getItem(Key key);

        List<SearchResultItem<VectorRecord>> search(String indexName, SearchVectorsEnhancedRequest request);

        DescribeTableResponse describeTable();
    }

    protected static String createSearchVectorsTableName() {
        return "JavaTests-" + UUID.randomUUID() + "-SearchVectorsTable";
    }

    protected static CreateTableEnhancedRequest createTableRequest() {
        return CreateTableEnhancedRequest.builder()
                                         .vectorIndexes(VECTOR_INDEX_DEFINITIONS)
                                         .build();
    }

    protected static List<VectorRecord> testRecords() {
        List<VectorRecord> records = new ArrayList<>();
        records.add(testRecord("articles", "exact-match", "science",
                               "Exact match article", EXACT_MATCH_VECTOR));
        records.add(testRecord("articles", "near-match", "science",
                               "Near match article", NEAR_MATCH_VECTOR));
        records.add(testRecord("articles", "orthogonal-1", "art",
                               "Orthogonal article one", ORTHOGONAL_1_VECTOR));
        records.add(testRecord("articles", "orthogonal-2", "art",
                               "Orthogonal article two", ORTHOGONAL_2_VECTOR));
        records.add(testRecord("reviews", "diff-partition", "general",
                               "Different partition review", DIFF_PARTITION_VECTOR));

        records.add(testRecord("articles-extended", "article-history", "history",
                               "History article", seededNormalizedVector(4, 11)));
        records.add(testRecord("articles-extended", "article-sports", "sports",
                               "Sports article", seededNormalizedVector(4, 12)));
        records.add(testRecord("articles-extended", "article-tech", "tech",
                               "Technology article", seededNormalizedVector(4, 13)));

        records.add(testRecord("reviews", "review-positive", "positive",
                               "Positive review", seededNormalizedVector(4, 21)));
        records.add(testRecord("reviews", "review-neutral", "neutral",
                               "Neutral review", seededNormalizedVector(4, 22)));
        records.add(testRecord("reviews", "review-negative", "negative",
                               "Negative review", seededNormalizedVector(4, 23)));
        records.add(testRecord("reviews", "review-tech", "tech",
                               "Tech review", seededNormalizedVector(4, 24)));
        records.add(testRecord("reviews", "review-general", "general",
                               "General review", seededNormalizedVector(4, 25)));

        records.add(testRecord("products", "product-laptop", "electronics",
                               "Laptop", seededNormalizedVector(4, 31)));
        records.add(testRecord("products", "product-phone", "electronics",
                               "Phone", seededNormalizedVector(4, 32)));
        records.add(testRecord("products", "product-sofa", "home",
                               "Sofa", seededNormalizedVector(4, 33)));
        records.add(testRecord("products", "product-lamp", "home",
                               "Lamp", seededNormalizedVector(4, 34)));
        records.add(testRecord("products", "product-cooler", "outdoor",
                               "Cooler", seededNormalizedVector(4, 35)));
        records.add(testRecord("products", "product-backpack", "outdoor",
                               "Backpack", seededNormalizedVector(4, 36)));
        records.add(testRecord("products", "product-novel", "books",
                               "Novel", seededNormalizedVector(4, 37)));
        records.add(testRecord("products", "product-textbook", "books",
                               "Textbook", seededNormalizedVector(4, 38)));
        return records;
    }

    protected static void setEmbeddingForIndex(VectorRecord record, String indexName, float[] embedding) {
        setEmbeddingForIndex(record, indexName, toFloatList(embedding));
    }

    protected static void setEmbeddingForIndex(VectorRecord record, String indexName, List<Float> embedding) {
        if (COSINE_INDEX.equals(indexName)) {
            record.setEmbeddingCosine(embedding);
        } else if (DOT_PRODUCT_INDEX.equals(indexName)) {
            record.setEmbeddingDot(embedding);
        } else if (EUCLIDEAN_INDEX.equals(indexName)) {
            record.setEmbeddingEuclidean(embedding);
        } else if (TABLE_WIDE_INDEX.equals(indexName) || indexName.startsWith("vector-index-")) {
            record.setEmbeddingCosine(embedding);
        } else {
            throw new IllegalArgumentException("Unsupported index name for embedding assignment: " + indexName);
        }
    }

    protected static List<Float> embeddingForIndex(VectorRecord record, EnhancedVectorIndex index) {
        String embeddingAttribute = index.vectorAttributeName();
        if ("embeddingDot".equals(embeddingAttribute)) {
            return record.getEmbeddingDot();
        }
        if ("embeddingEuclidean".equals(embeddingAttribute)) {
            return record.getEmbeddingEuclidean();
        }
        return record.getEmbeddingCosine();
    }

    protected static void assertEmbeddingMatches(VectorRecord record,
                                                 EnhancedVectorIndex index,
                                                 float[] expectedEmbedding) {
        List<Float> embedding = embeddingForIndex(record, index);
        assertThat(embedding).hasSize(expectedEmbedding.length);
        IntStream.range(0, expectedEmbedding.length).forEach(i -> {
            // DynamoDB may slightly change float values on read-back, so compare with a small tolerance.
            assertThat(embedding.get(i)).isCloseTo(expectedEmbedding[i], within(0.00001f));
        });
    }

    protected static DocumentTableSchema documentSchema(boolean withCustomConverterProvider) {
        return DOCUMENT_SCHEMA_CACHE.computeIfAbsent(withCustomConverterProvider, custom -> {
            DocumentTableSchema.Builder builder = DocumentTableSchema
                .builder()
                .addIndexPartitionKey(primaryIndexName(), "pk", AttributeValueType.S)
                .addIndexSortKey(primaryIndexName(), "sk", AttributeValueType.S)
                .vectorIndexes(VECTOR_INDEX_DEFINITIONS);
            if (custom) {
                builder.attributeConverterProviders(
                    CustomAttributeForDocumentConverterProvider.create(),
                    DefaultAttributeConverterProvider.create());
            }
            return builder.build();
        });
    }

    protected List<SearchResultItem<EnhancedDocument>> searchVectorsDocument(String index,
                                                                             SearchVectorsEnhancedRequest request) {
        return searchVectorsDocument(index, request, false);
    }

    protected List<SearchResultItem<EnhancedDocument>> searchVectorsDocument(String index,
                                                                             SearchVectorsEnhancedRequest request,
                                                                             boolean withCustomConverter) {
        if (isSharedTableSearchReady()) {
            return executeSearchDocument(index, request, documentSchema(withCustomConverter));
        }
        return awaitNonEmptySearchResults(
            () -> executeSearchDocument(index, request, documentSchema(withCustomConverter)));
    }

    protected boolean isSharedTableSearchReady() {
        return SEARCH_READY_TABLES.contains(sharedTableName());
    }

    protected void withDedicatedTable(
        StaticTableSchema<VectorRecord> schema,
        List<EnhancedVectorIndex> indexes,
        Consumer<DedicatedTableContext> test) {
        String tableName = createSearchVectorsTableName();
        try {
            createAndWaitForDedicatedTable(
                tableName, schema, indexes);
            waitForVectorIndexesActive(
                tableName, indexes.size());
            test.accept(new DedicatedTableContext() {
                @Override
                public String tableName() {
                    return tableName;
                }

                @Override
                public void putItem(VectorRecord record) {
                    dedicatedPutItem(tableName, schema, record);
                }

                @Override
                public VectorRecord getItem(Key key) {
                    return dedicatedGetItem(tableName, schema, key);
                }

                @Override
                public List<SearchResultItem<VectorRecord>> search(String indexName, SearchVectorsEnhancedRequest request) {
                    return dedicatedSearch(tableName, schema, indexName, request);
                }

                @Override
                public DescribeTableResponse describeTable() {
                    return SearchVectorsIntegrationTestBase.this.describeTable(tableName);
                }
            });
        } finally {
            deleteDedicatedTable(tableName);
        }
    }

    protected static EnhancedVectorIndex vectorIndexWithDimensions(int dims) {
        return EnhancedVectorIndex.builder()
                                  .indexName("vector-index-" + dims)
                                  .vectorAttributeName("embedding")
                                  .distanceFunction(DistanceFunction.COSINE)
                                  .dimensions(dims)
                                  .projection(p -> p.projectionType(ProjectionType.ALL))
                                  .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                .searchSchemaElementType(HASH))
                                  .build();
    }

    protected static StaticTableSchema<VectorRecord> tableSchemaForVectorIndex(EnhancedVectorIndex index) {
        StaticTableSchema.Builder<VectorRecord> builder =
            StaticTableSchema.builder(VectorRecord.class)
                             .newItemSupplier(VectorRecord::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(VectorRecord::getPk)
                                                               .setter(VectorRecord::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("sk")
                                                               .getter(VectorRecord::getSk)
                                                               .setter(VectorRecord::setSk)
                                                               .tags(primarySortKey()))
                             .addAttribute(String.class, a -> a.name("category")
                                                               .getter(VectorRecord::getCategory)
                                                               .setter(VectorRecord::setCategory))
                             .addAttribute(String.class, a -> a.name("description")
                                                               .getter(VectorRecord::getDescription)
                                                               .setter(VectorRecord::setDescription));

        String embeddingAttribute = index.vectorAttributeName();
        builder.addAttribute(EnhancedType.listOf(Float.class), a -> {
            a.name(embeddingAttribute);
            if ("embeddingDot".equals(embeddingAttribute)) {
                a.getter(VectorRecord::getEmbeddingDot).setter(VectorRecord::setEmbeddingDot);
            } else if ("embeddingEuclidean".equals(embeddingAttribute)) {
                a.getter(VectorRecord::getEmbeddingEuclidean).setter(VectorRecord::setEmbeddingEuclidean);
            } else {
                a.getter(VectorRecord::getEmbeddingCosine).setter(VectorRecord::setEmbeddingCosine);
            }
        });

        return builder.tags(StaticAttributeTags.vectorIndexes(Collections.singletonList(index))).build();
    }

    protected static EnhancedVectorIndex tableWideVectorIndex() {
        return EnhancedVectorIndex.builder()
                                  .indexName(TABLE_WIDE_INDEX)
                                  .vectorAttributeName("embeddingCosine")
                                  .distanceFunction(DistanceFunction.COSINE)
                                  .dimensions(4)
                                  .projection(p -> p.projectionType(ProjectionType.ALL))
                                  .build();
    }

    protected static StaticTableSchema<VectorCreateTableLimitRecord> createTableLimitTestSchema() {
        StaticTableSchema.Builder<VectorCreateTableLimitRecord> builder =
            StaticTableSchema.builder(VectorCreateTableLimitRecord.class)
                             .newItemSupplier(VectorCreateTableLimitRecord::new)
                             .addAttribute(String.class, a -> a.name("pk")
                                                               .getter(VectorCreateTableLimitRecord::getPk)
                                                               .setter(VectorCreateTableLimitRecord::setPk)
                                                               .tags(primaryPartitionKey()))
                             .addAttribute(String.class, a -> a.name("sk")
                                                               .getter(VectorCreateTableLimitRecord::getSk)
                                                               .setter(VectorCreateTableLimitRecord::setSk)
                                                               .tags(primarySortKey()))
                             .addAttribute(EnhancedType.listOf(Float.class),
                                           a -> a.name("embeddingCosine")
                                                 .getter(VectorCreateTableLimitRecord::getEmbeddingCosine)
                                                 .setter(VectorCreateTableLimitRecord::setEmbeddingCosine));
        IntStream.range(0, 19).forEach(i -> {
            int index = i;
            builder.addAttribute(String.class, a -> a.name("filter" + index)
                                                     .getter(r -> r.getFilterAttribute(index))
                                                     .setter((r, v) -> r.setFilterAttribute(index, v)));
        });
        IntStream.range(0, 21).forEach(i -> {
            int index = i;
            builder.addAttribute(String.class, a -> a.name("proj" + index)
                                                     .getter(r -> r.getProjectionAttribute(index))
                                                     .setter((r, v) -> r.setProjectionAttribute(index, v)));
        });
        return builder.build();
    }

    protected static EnhancedVectorIndex buildVectorIndexWithInlineFilters(int inlineFilterCount) {
        EnhancedVectorIndex.Builder builder = EnhancedVectorIndex.builder()
                                                                 .indexName("inline-filter-limit-index")
                                                                 .vectorAttributeName("embeddingCosine")
                                                                 .distanceFunction(DistanceFunction.COSINE)
                                                                 .dimensions(4)
                                                                 .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                 .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                               .searchSchemaElementType(HASH));
        IntStream.range(0, inlineFilterCount)
                 .forEach(i -> builder.addSearchSchemaElement(b -> b.attributeName("filter" + i)
                                                                    .searchSchemaElementType(INLINE_FILTER)));
        return builder.build();
    }

    protected static EnhancedVectorIndex buildVectorIndexWithProjectedAttributes(int projectedAttributeCount) {
        List<String> projectedNames = IntStream.range(0, projectedAttributeCount)
                                               .mapToObj(i -> "proj" + i)
                                               .collect(Collectors.toList());
        return EnhancedVectorIndex.builder()
                                  .indexName("projection-limit-index")
                                  .vectorAttributeName("embeddingCosine")
                                  .distanceFunction(DistanceFunction.COSINE)
                                  .dimensions(4)
                                  .projection(p -> p.projectionType(ProjectionType.INCLUDE)
                                                    .nonKeyAttributes(projectedNames))
                                  .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                .searchSchemaElementType(HASH))
                                  .build();
    }

    protected static float[] generateNormalizedVector(int dimensions) {
        return seededNormalizedVector(dimensions, 1);
    }

    protected static boolean tableExists(DynamoDbClient client, String name) {
        try {
            client.describeTable(r -> r.tableName(name));
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    protected static boolean tableExists(DynamoDbAsyncClient client, String name) {
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

    protected boolean tableExistsCheck(String tableName) {
        try {
            describeTable(tableName);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        } catch (CompletionException e) {
            if (e.getCause() instanceof ResourceNotFoundException) {
                return false;
            }
            throw e;
        }
    }

    protected void waitForVectorIndexesActive(String tableName, int expectedCount) {
        for (int attempt = 0; attempt < VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS; attempt++) {
            List<VectorIndexDescription> indexes = describeTable(tableName).table().vectorIndexes();
            if (vectorIndexesActive(indexes, expectedCount)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw vectorIndexWaitTimeout(tableName, expectedCount);
    }

    protected static void waitForVectorIndexesActive(DynamoDbClient client,
                                                     String tableName,
                                                     int expectedCount) {
        for (int attempt = 0; attempt < VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS; attempt++) {
            List<VectorIndexDescription> indexes =
                client.describeTable(r -> r.tableName(tableName)).table().vectorIndexes();
            if (vectorIndexesActive(indexes, expectedCount)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw vectorIndexWaitTimeout(tableName, expectedCount);
    }

    protected static void waitForVectorIndexesActive(DynamoDbAsyncClient client,
                                                     String tableName,
                                                     int expectedCount) {
        for (int attempt = 0; attempt < VECTOR_INDEX_ACTIVE_WAIT_ATTEMPTS; attempt++) {
            List<VectorIndexDescription> indexes =
                client.describeTable(r -> r.tableName(tableName)).join().table().vectorIndexes();
            if (vectorIndexesActive(indexes, expectedCount)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw vectorIndexWaitTimeout(tableName, expectedCount);
    }

    private static boolean vectorIndexesActive(List<VectorIndexDescription> indexes, int expectedCount) {
        return indexes.size() == expectedCount
               && indexes.stream().allMatch(i -> i.indexStatus() == IndexStatus.ACTIVE);
    }

    private static AssertionError vectorIndexWaitTimeout(String tableName, int expectedCount) {
        return new AssertionError("Timed out waiting for " + expectedCount
                                  + " active vector indexes on table: " + tableName);
    }

    protected static SearchVectorsEnhancedRequest sharedTableSearchReadyRequest() {
        return baseSearchRequest()
            .searchConditionExpression(hashCondition("articles"))
            .build();
    }

    protected static void prepareSyncSharedTable(DynamoDbClient client,
                                                 String tableName,
                                                 Runnable createTable,
                                                 Runnable seedTable,
                                                 Supplier<List<SearchResultItem<VectorRecord>>> search) {
        synchronized (sharedTableSetupLock(tableName)) {
            prepareSharedTable(
                () -> sharedTableConfigured(client, tableName),
                () -> recreateSyncSharedTable(client, tableName, createTable),
                () -> waitForVectorIndexesActive(client, tableName, VECTOR_INDEX_DEFINITIONS.size()),
                seedTable,
                search);
            SEARCH_READY_TABLES.add(tableName);
        }
    }

    protected static void prepareAsyncSharedTable(DynamoDbAsyncClient client,
                                                  String tableName,
                                                  Runnable createTable,
                                                  Runnable seedTable,
                                                  Supplier<List<SearchResultItem<VectorRecord>>> search) {
        synchronized (sharedTableSetupLock(tableName)) {
            prepareSharedTable(
                () -> sharedTableConfigured(client, tableName),
                () -> recreateAsyncSharedTable(client, tableName, createTable),
                () -> waitForVectorIndexesActive(client, tableName, VECTOR_INDEX_DEFINITIONS.size()),
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
                                           Supplier<List<SearchResultItem<VectorRecord>>> search) {
        if (tableConfigured.get()
            && !awaitSearchResultCount(search, 1, QUICK_SEARCH_RETRY_ATTEMPTS).isEmpty()) {
            return;
        }

        if (!tableConfigured.get()) {
            recreateTable.run();
            waitForIndexes.run();
            seedTable.run();
            awaitTableSearchReady(search, 1);
            return;
        }

        waitForIndexes.run();
        seedTable.run();
        if (!awaitSearchResultCount(search, 1, TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS).isEmpty()) {
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
        return vectorIndexesActive(indexes, VECTOR_INDEX_DEFINITIONS.size());
    }

    private static boolean sharedTableConfigured(DynamoDbAsyncClient client, String tableName) {
        if (!tableExists(client, tableName)) {
            return false;
        }
        List<VectorIndexDescription> indexes =
            client.describeTable(r -> r.tableName(tableName)).join().table().vectorIndexes();
        return vectorIndexesActive(indexes, VECTOR_INDEX_DEFINITIONS.size());
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

    private static void awaitTableSearchReady(Supplier<List<SearchResultItem<VectorRecord>>> search,
                                              int expectedCount) {
        if (awaitSearchResultCount(search, expectedCount, TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS).size() < expectedCount) {
            throw new AssertionError("Timed out waiting for table search results after seeding");
        }
    }

    protected void prepareDedicatedIntegrationTablesOnce() {
        synchronized (DEDICATED_TABLE_PREP_LOCK) {
            if (dedicatedTablesPrepared) {
                return;
            }
            for (int dimensions : new int[] {128, 512, 1024}) {
                prepareDimensionTable(dimensions);
            }
            prepareNoHashTable();
            dedicatedTablesPrepared = true;
        }
    }

    protected static String dimensionTableName(int dimensions) {
        return DIMENSION_TABLE_PREFIX + dimensions;
    }

    protected void prepareDimensionTable(int dimensions) {
        String tableName = dimensionTableName(dimensions);
        String prepKey = "dimension:" + tableName;
        if (PREPARED_TABLES.contains(prepKey)) {
            return;
        }

        EnhancedVectorIndex index = vectorIndexWithDimensions(dimensions);
        StaticTableSchema<VectorRecord> schema = tableSchemaForVectorIndex(index);

        if (tableExistsCheck(tableName)) {
            waitForVectorIndexesActive(tableName, 1);
            seedDimensionTable(tableName, schema, index, dimensions);
            if (dimensionTableSearchReady(tableName, schema, index, dimensions)) {
                PREPARED_TABLES.add(prepKey);
                return;
            }
            deleteDedicatedTable(tableName);
            waitUntilDedicatedTableNotExists(tableName);
        }

        createAndWaitForDedicatedTable(tableName, schema, Collections.singletonList(index));
        waitForVectorIndexesActive(tableName, 1);
        seedDimensionTable(tableName, schema, index, dimensions);
        awaitDimensionTableSearchReady(tableName, schema, index, dimensions);
        PREPARED_TABLES.add(prepKey);
    }

    private void seedDimensionTable(String tableName,
                                    StaticTableSchema<VectorRecord> schema,
                                    EnhancedVectorIndex index,
                                    int dimensions) {
        float[] matchingVector = generateNormalizedVector(dimensions);
        float[] otherVector = seededNormalizedVector(dimensions, 2);
        String matchingSk = "item-" + dimensions;
        String otherSk = "other-" + dimensions;

        VectorRecord matchingRecord = new VectorRecord()
            .setPk("dimension-test")
            .setSk(matchingSk)
            .setCategory("test")
            .setDescription("Dimension test item");
        setEmbeddingForIndex(matchingRecord, index.indexName(), matchingVector);

        VectorRecord otherRecord = new VectorRecord()
            .setPk("dimension-test")
            .setSk(otherSk)
            .setCategory("other")
            .setDescription("Other dimension test item");
        setEmbeddingForIndex(otherRecord, index.indexName(), otherVector);

        dedicatedPutItem(tableName, schema, matchingRecord);
        dedicatedPutItem(tableName, schema, otherRecord);

        Key matchKey = Key.builder()
                          .partitionValue("dimension-test")
                          .sortValue(matchingSk).build();
        assertEmbeddingMatches(dedicatedGetItem(tableName, schema, matchKey), index, matchingVector);

        Key otherKey = Key.builder()
                          .partitionValue("dimension-test")
                          .sortValue(otherSk).build();
        assertEmbeddingMatches(dedicatedGetItem(tableName, schema, otherKey), index, otherVector);
    }

    private SearchVectorsEnhancedRequest dimensionSearchRequest(int dimensions) {
        return SearchVectorsEnhancedRequest.builder()
                                           .searchVector(generateNormalizedVector(dimensions))
                                           .topK(2)
                                           .searchConditionExpression(hashCondition("dimension-test"))
                                           .build();
    }

    private boolean dimensionTableSearchReady(String tableName,
                                              StaticTableSchema<VectorRecord> schema,
                                              EnhancedVectorIndex index,
                                              int dimensions) {
        return awaitSearchResultCount(
            () -> dedicatedSearch(tableName, schema, index.indexName(), dimensionSearchRequest(dimensions)),
            2,
            TABLE_WARMUP_SEARCH_RETRY_ATTEMPTS).size() == 2;
    }

    private void awaitDimensionTableSearchReady(String tableName,
                                                StaticTableSchema<VectorRecord> schema,
                                                EnhancedVectorIndex index,
                                                int dimensions) {
        if (!dimensionTableSearchReady(tableName, schema, index, dimensions)) {
            throw new AssertionError("Timed out waiting for dimension table search results: " + tableName);
        }
    }

    protected void prepareNoHashTable() {
        if (PREPARED_TABLES.contains(NOHASH_TABLE_NAME)) {
            return;
        }

        EnhancedVectorIndex noHashIndex = tableWideVectorIndex();
        StaticTableSchema<VectorRecord> noHashSchema = tableSchemaForVectorIndex(noHashIndex);
        SearchVectorsEnhancedRequest warmupRequest = SearchVectorsEnhancedRequest.builder()
                                                                                 .searchVector(QUERY_VECTOR)
                                                                                 .topK(10)
                                                                                 .build();
        Supplier<List<SearchResultItem<VectorRecord>>> warmupSearch =
            () -> dedicatedSearch(NOHASH_TABLE_NAME, noHashSchema, TABLE_WIDE_INDEX, warmupRequest);

        if (tableExistsCheck(NOHASH_TABLE_NAME)
            && awaitSearchResultCount(warmupSearch, 2, QUICK_SEARCH_RETRY_ATTEMPTS).size() == 2) {
            PREPARED_TABLES.add(NOHASH_TABLE_NAME);
            return;
        }

        if (!tableExistsCheck(NOHASH_TABLE_NAME)) {
            createAndWaitForDedicatedTable(
                NOHASH_TABLE_NAME, noHashSchema, Collections.singletonList(noHashIndex));
            waitForVectorIndexesActive(NOHASH_TABLE_NAME, 1);
        }

        dedicatedPutItem(NOHASH_TABLE_NAME, noHashSchema,
                         testRecord("articles", "wide-articles", "science", "Wide articles item", QUERY_VECTOR));
        dedicatedPutItem(NOHASH_TABLE_NAME, noHashSchema,
                         testRecord("reviews", "wide-reviews", "general", "Wide reviews item", QUERY_VECTOR));
        awaitTableSearchReady(warmupSearch, 2);
        PREPARED_TABLES.add(NOHASH_TABLE_NAME);
    }

    protected void waitUntilDedicatedTableNotExists(String tableName) {
        for (int attempt = 0; attempt < TABLE_DELETION_WAIT_ATTEMPTS; attempt++) {
            if (!tableExistsCheck(tableName)) {
                return;
            }
            sleepBetweenRetries(attempt);
        }
        throw new AssertionError("Timed out waiting for table deletion: " + tableName);
    }

    protected static DynamoDbClient createSearchVectorsClient() {
        return createDynamoDbClient();
    }

    protected static DynamoDbAsyncClient createAsyncSearchVectorsClient() {
        return createAsyncDynamoDbClient();
    }

    protected static List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    protected static List<AttributeValue> toSearchVectorAttributeValues(float[] searchVector) {
        List<AttributeValue> result = new ArrayList<>(searchVector.length);
        for (float value : searchVector) {
            result.add(AttributeValue.builder().n(Float.toString(value)).build());
        }
        return result;
    }

    protected static SearchVectorsEnhancedRequest.Builder baseSearchRequest() {
        return SearchVectorsEnhancedRequest.builder()
                                           .searchVector(QUERY_VECTOR)
                                           .topK(5);
    }

    protected static Expression hashCondition(String pkValue) {
        return Expression.builder()
                         .expression("#pk = :pkval")
                         .putExpressionName("#pk", "pk")
                         .putExpressionValue(":pkval", AttributeValue.builder().s(pkValue).build())
                         .build();
    }

    protected static Expression hashAndFilterCondition(String pkValue, String categoryValue) {
        return Expression.builder()
                         .expression("#pk = :pkval AND #cat = :catval")
                         .putExpressionName("#pk", "pk")
                         .putExpressionName("#cat", "category")
                         .putExpressionValue(":pkval", AttributeValue.builder().s(pkValue).build())
                         .putExpressionValue(":catval", AttributeValue.builder().s(categoryValue).build())
                         .build();
    }

    protected static Expression inlineFilterOnlyCondition(String categoryValue) {
        return Expression.builder()
                         .expression("#cat = :catval")
                         .putExpressionName("#cat", "category")
                         .putExpressionValue(":catval", AttributeValue.builder().s(categoryValue).build())
                         .build();
    }

    protected static void assertThrowsContaining(ThrowingCallable callable,
                                                 Class<? extends Throwable> exceptionType,
                                                 String messageSubstring) {
        assertThatThrownBy(callable)
            .satisfies(thrown -> {
                Throwable cause = thrown instanceof CompletionException ? thrown.getCause() : thrown;
                assertThat(cause).isInstanceOf(exceptionType)
                                 .hasMessageContaining(messageSubstring);
            });
    }

    protected void failsAtService(ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .satisfies(thrown -> {
                Throwable cause = thrown instanceof CompletionException ? thrown.getCause() : thrown;
                assertThat(cause).isInstanceOf(DynamoDbException.class);
            });
    }

    protected List<SearchResultItem<VectorRecord>> searchVectors(String indexName, SearchVectorsEnhancedRequest request) {
        if (isSharedTableSearchReady()) {
            return executeSearch(indexName, request);
        }
        return awaitNonEmptySearchResults(() -> executeSearch(indexName, request));
    }

    protected SearchVectorsEnhancedResponse<VectorRecord> searchVectorsWithResponse(String indexName,
                                                                                    SearchVectorsEnhancedRequest request) {
        SearchVectorsEnhancedResponse<VectorRecord> response = executeSearchWithResponse(indexName, request);
        if (isSharedTableSearchReady() || !response.results().isEmpty()) {
            return response;
        }
        for (int attempt = 0; response.results().isEmpty() && attempt < DEFAULT_SEARCH_RETRY_ATTEMPTS; attempt++) {
            sleepBetweenRetries(attempt);
            response = executeSearchWithResponse(indexName, request);
        }
        return response;
    }

    protected SearchVectorsEnhancedResponse<VectorRecord> searchVectorsWithResponse(
        String indexName, Consumer<SearchVectorsEnhancedRequest.Builder> builder) {
        SearchVectorsEnhancedResponse<VectorRecord> response = executeSearchWithResponseBuilder(indexName, builder);
        if (isSharedTableSearchReady() || !response.results().isEmpty()) {
            return response;
        }
        for (int attempt = 0; response.results().isEmpty() && attempt < DEFAULT_SEARCH_RETRY_ATTEMPTS; attempt++) {
            sleepBetweenRetries(attempt);
            response = executeSearchWithResponseBuilder(indexName, builder);
        }
        return response;
    }

    protected static <T> List<SearchResultItem<T>> awaitNonEmptySearchResults(
        Supplier<List<SearchResultItem<T>>> search) {
        return awaitSearchResultCount(search, 1);
    }

    protected static <T> List<SearchResultItem<T>> awaitSearchResultCount(
        Supplier<List<SearchResultItem<T>>> search, int expectedCount) {
        return awaitSearchResultCount(search, expectedCount, DEFAULT_SEARCH_RETRY_ATTEMPTS);
    }

    protected static <T> List<SearchResultItem<T>> awaitSearchResultCount(
        Supplier<List<SearchResultItem<T>>> search,
        int expectedCount,
        int maxAttempts) {
        List<SearchResultItem<T>> results = search.get();
        for (int attempt = 0; results.size() < expectedCount && attempt < maxAttempts; attempt++) {
            sleepBetweenRetries(attempt);
            results = search.get();
        }
        return results;
    }

    // Uses exponential backoff with a maximum delay of 1 second between retries.
    private static void sleepBetweenRetries(int attempt) {
        try {
            long delay = Math.min(50L << Math.min(attempt, 4), 1_000L);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected List<SearchResultItem<VectorRecord>> searchVectorsUntilContains(
        String indexName,
        SearchVectorsEnhancedRequest request,
        String expectedSortKey) {
        List<SearchResultItem<VectorRecord>> results =
            executeSearch(indexName, request);
        for (int attempt = 0; attempt < DEFAULT_SEARCH_RETRY_ATTEMPTS; attempt++) {
            boolean found = results.stream()
                                   .anyMatch(r -> r.item() != null
                                                  && expectedSortKey.equals(
                                       r.item().getSk()));
            if (found) {
                return results;
            }
            sleepBetweenRetries(attempt);
            results = executeSearch(indexName, request);
        }
        return results;
    }

    protected List<SearchResultItem<VectorRecord>> searchVectorsUntilAbsent(
        String indexName,
        SearchVectorsEnhancedRequest request,
        String absentSortKey) {
        List<SearchResultItem<VectorRecord>> results =
            executeSearch(indexName, request);
        for (int attempt = 0; attempt < DEFAULT_SEARCH_RETRY_ATTEMPTS; attempt++) {
            boolean found = results.stream()
                                   .anyMatch(r -> r.item() != null
                                                  && absentSortKey.equals(
                                       r.item().getSk()));
            if (!found) {
                return results;
            }
            sleepBetweenRetries(attempt);
            results = executeSearch(indexName, request);
        }
        return results;
    }

    protected static double scoreFor(List<SearchResultItem<VectorRecord>> results, String sortKey) {
        return results.stream()
                      .filter(r -> sortKey.equals(r.item().getSk()))
                      .findFirst()
                      .map(SearchResultItem::score)
                      .orElseThrow(() -> new AssertionError("No result with sort key: " + sortKey));
    }

    protected static void assertScoresOrderedByRelevance(List<SearchResultItem<VectorRecord>> results,
                                                         DistanceFunction distanceFunction) {
        assertThat(results)
            .extracting(SearchResultItem::score)
            .isSortedAccordingTo(relevanceScoreComparator(distanceFunction));
    }

    protected static void assertMoreRelevantThan(List<SearchResultItem<VectorRecord>> results,
                                                 DistanceFunction distanceFunction,
                                                 String moreRelevantSortKey,
                                                 String lessRelevantSortKey) {
        double moreRelevantScore = scoreFor(results, moreRelevantSortKey);
        double lessRelevantScore = scoreFor(results, lessRelevantSortKey);

        if (distanceFunction == DistanceFunction.DOT_PRODUCT) {
            assertThat(moreRelevantScore)
                .as("Dot product score for '%s' should be >= score for '%s'",
                    moreRelevantSortKey, lessRelevantSortKey)
                .isGreaterThanOrEqualTo(lessRelevantScore);
        } else {
            assertThat(moreRelevantScore)
                .as("%s score for '%s' should be <= score for '%s'",
                    distanceFunction, moreRelevantSortKey, lessRelevantSortKey)
                .isLessThanOrEqualTo(lessRelevantScore);
        }
    }

    protected static void assertArticlesCosineSearchResults(List<SearchResultItem<VectorRecord>> results) {
        assertThat(results).hasSize(4);
        assertThat(results.stream().map(r -> r.item().getSk()).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("exact-match", "near-match", "orthogonal-1", "orthogonal-2");
        results.forEach(r -> assertThat(r.item().getPk()).isEqualTo("articles"));

        assertScoresOrderedByRelevance(results, DistanceFunction.COSINE);
        assertThat(results.stream().map(r -> r.item().getSk()).limit(2).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("exact-match", "near-match");
        assertThat(results.stream().map(r -> r.item().getSk()).skip(2).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("orthogonal-1", "orthogonal-2");
        assertMoreRelevantThan(results, DistanceFunction.COSINE, "exact-match", "orthogonal-1");
        assertMoreRelevantThan(results, DistanceFunction.COSINE, "near-match", "orthogonal-2");
    }

    protected static void assertProjectedAttributes(VectorRecord vectorRecord,
                                                    boolean expectCategory,
                                                    boolean expectDescription) {
        assertThat(vectorRecord.getPk()).isNotNull();
        assertThat(vectorRecord.getSk()).isNotNull();

        if (expectCategory) {
            assertThat(vectorRecord.getCategory()).isNotNull();
        } else {
            assertThat(vectorRecord.getCategory()).isNull();
        }
        if (expectDescription) {
            assertThat(vectorRecord.getDescription()).isNotNull();
        } else {
            assertThat(vectorRecord.getDescription()).isNull();
        }
    }

    private static VectorRecord testRecord(String pk, String sk, String category, String description, float[] vector) {
        return new VectorRecord()
            .setPk(pk)
            .setSk(sk)
            .setCategory(category)
            .setDescription(description)
            .setEmbeddingCosine(toFloatList(vector))
            .setEmbeddingDot(toFloatList(vector))
            .setEmbeddingEuclidean(toFloatList(vector));
    }

    private static float[] seededNormalizedVector(int dimensions, int seed) {
        float[] vector = new float[dimensions];
        vector[Math.floorMod(seed, dimensions)] = 1.0f;
        return vector;
    }

    private static Comparator<Double> relevanceScoreComparator(DistanceFunction distanceFunction) {
        switch (distanceFunction) {
            case DOT_PRODUCT:
                return Comparator.reverseOrder();
            case COSINE:
            case EUCLIDEAN:
                return Comparator.naturalOrder();
            default:
                throw new IllegalArgumentException("Unsupported distance function: " + distanceFunction);
        }
    }

    @Nested
    class TableSetupTests {

        @Test
        void createTable_withVectorIndexes_succeeds() {
            DescribeTableResponse response = describeTable(sharedTableName());
            assertThat(response.table().tableStatus().toString()).hasToString("ACTIVE");
            assertThat(response.table().tableName()).isEqualTo(sharedTableName());
        }

        @Test
        void describeTable_exposesVectorIndexMetadata() {
            DescribeTableResponse response = describeTable(sharedTableName());
            List<VectorIndexDescription> vectorIndexes = response.table().vectorIndexes();
            assertThat(vectorIndexes)
                .hasSize(3)
                .anySatisfy(index -> {
                    assertThat(index.indexName()).isEqualTo(COSINE_INDEX);
                    assertThat(index.distanceFunction()).hasToString("COSINE");
                    assertThat(index.dimensions()).isEqualTo(4);
                    assertThat(index.vectorAttribute().attributeName()).isEqualTo("embeddingCosine");
                    assertThat(index.searchSchema())
                        .hasSize(2)
                        .anySatisfy(e -> {
                            assertThat(e.attributeName()).isEqualTo("pk");
                            assertThat(e.searchSchemaElementType()).hasToString("HASH");
                        })
                        .anySatisfy(e -> {
                            assertThat(e.attributeName()).isEqualTo("category");
                            assertThat(e.searchSchemaElementType()).hasToString("INLINE_FILTER");
                        });
                })
                .anySatisfy(index -> {
                    assertThat(index.indexName()).isEqualTo(DOT_PRODUCT_INDEX);
                    assertThat(index.distanceFunction()).hasToString("DOT_PRODUCT");
                    assertThat(index.dimensions()).isEqualTo(4);
                    assertThat(index.vectorAttribute().attributeName()).isEqualTo("embeddingDot");
                    assertThat(index.searchSchema())
                        .hasSize(1)
                        .anySatisfy(e -> {
                            assertThat(e.attributeName()).isEqualTo("pk");
                            assertThat(e.searchSchemaElementType()).hasToString("HASH");
                        });
                })
                .anySatisfy(index -> {
                    assertThat(index.indexName()).isEqualTo(EUCLIDEAN_INDEX);
                    assertThat(index.distanceFunction()).hasToString("EUCLIDEAN");
                    assertThat(index.dimensions()).isEqualTo(4);
                    assertThat(index.vectorAttribute().attributeName()).isEqualTo("embeddingEuclidean");
                    assertThat(index.searchSchema())
                        .hasSize(1)
                        .anySatisfy(e -> {
                            assertThat(e.attributeName()).isEqualTo("pk");
                            assertThat(e.searchSchemaElementType()).hasToString("HASH");
                        });
                });
        }
    }

    @Nested
    class SearchHappyPathTests {

        @Test
        void searchVectors_cosine_resultsOrderedByRelevance() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            assertScoresOrderedByRelevance(results, DistanceFunction.COSINE);
        }

        @Test
        void searchVectors_dotProduct_resultsOrderedByRelevance() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(DOT_PRODUCT_INDEX, request);

            assertThat(results).isNotEmpty();
            assertScoresOrderedByRelevance(results, DistanceFunction.DOT_PRODUCT);
        }

        @Test
        void searchVectors_euclidean_resultsOrderedByRelevance() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(EUCLIDEAN_INDEX, request);

            assertThat(results).isNotEmpty();
            assertScoresOrderedByRelevance(results, DistanceFunction.EUCLIDEAN);
        }

        @Test
        void searchVectors_scoresAreNonNegative() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertThat(r.score()).isGreaterThanOrEqualTo(0.0));
        }

        @Test
        void searchVectors_identicalVector_bestScore() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest
                .builder()
                .searchVector(EXACT_MATCH_VECTOR)
                .topK(5)
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).item().getSk()).isEqualTo("exact-match");
        }

        @Test
        void searchVectors_returnsTypedItemsWithScores() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item()).isNotNull();
                assertThat(r.item().getPk()).isNotNull();
                assertThat(r.item().getSk()).isNotNull();
            });
        }

        @Test
        void searchVectors_projectionAll_returnsAllAttributes() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertProjectedAttributes(r.item(), true, true));
        }

        @Test
        void searchVectors_projectionKeysOnly_returnsOnlyKeys() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(DOT_PRODUCT_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertProjectedAttributes(r.item(), false, false));
        }

        @Test
        void searchVectors_projectionInclude_returnsKeysAndCategory() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(EUCLIDEAN_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertProjectedAttributes(r.item(), true, false));
        }

        @Test
        void searchVectors_withProjectionExpression() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .attributesToProject("pk", "category")
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item().getPk()).isNotNull();
                assertThat(r.item().getCategory()).isNotNull();
                assertThat(r.item().getDescription()).isNull();
            });
        }

        @Test
        void searchVectorsWithResponse_returnsConsumedCapacity() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .build();

            SearchVectorsEnhancedResponse<VectorRecord> response =
                searchVectorsWithResponse(COSINE_INDEX, request);

            assertArticlesCosineSearchResults(response.results());
            assertThat(response.consumedCapacity()).isNotNull();
        }

        @Test
        void searchVectorsWithResponse_returnConsumedCapacityIndexes_returnsVectorCapacity() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                .build();

            SearchVectorsEnhancedResponse<VectorRecord> response =
                searchVectorsWithResponse(COSINE_INDEX, request);

            assertArticlesCosineSearchResults(response.results());
            assertThat(response.consumedCapacity()).isNotNull();
            assertThat(response.consumedCapacity().vectorSearchRequestBytes()).isNotNull();
        }

        @Test
        void searchVectorsWithResponse_noCapacityRequested_returnsNull() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            SearchVectorsEnhancedResponse<VectorRecord> response =
                searchVectorsWithResponse(COSINE_INDEX, request);

            assertArticlesCosineSearchResults(response.results());
            assertThat(response.consumedCapacity()).isNull();
        }

        @Test
        void searchVectors_withHashCondition() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertThat(r.item().getPk()).isEqualTo("articles"));
        }

        @Test
        void searchVectors_withHashAndInlineFilter() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashAndFilterCondition("articles", "science"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item().getPk()).isEqualTo("articles");
                assertThat(r.item().getCategory()).isEqualTo("science");
            });
        }

        @Test
        void searchVectors_withTopK() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .topK(1)
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).hasSize(1);
        }

        @Test
        void searchVectors_withBuilderConsumer_returnsOrderedCosineResults() {
            Consumer<SearchVectorsEnhancedRequest.Builder> requestBuilder =
                b -> b.searchVector(QUERY_VECTOR)
                      .topK(5)
                      .searchConditionExpression(hashCondition("articles"));

            List<SearchResultItem<VectorRecord>> results =
                awaitNonEmptySearchResults(() -> executeSearchWithBuilder(COSINE_INDEX, requestBuilder));

            assertArticlesCosineSearchResults(results);
        }

        @Test
        void searchVectorsWithResponse_withBuilderConsumer_returnsOrderedCosineResults() {
            SearchVectorsEnhancedResponse<VectorRecord> response =
                searchVectorsWithResponse(COSINE_INDEX,
                                          b -> b.searchVector(QUERY_VECTOR)
                                                .topK(5)
                                                .searchConditionExpression(hashCondition("articles")));

            assertArticlesCosineSearchResults(response.results());
        }

        @Test
        void searchVectors_emptyResults() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("nonexistent-partition"))
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isEmpty();
        }

        @Test
        void searchVectors_twoQueryVectorsOnCosineIndex_topResultSortKeyDiffers() {
            SearchVectorsEnhancedRequest primaryQuery = SearchVectorsEnhancedRequest.builder()
                                                                                    .searchVector(QUERY_VECTOR)
                                                                                    .topK(5)
                                                                                    .searchConditionExpression(hashCondition(
                                                                                        "articles"))
                                                                                    .build();
            SearchVectorsEnhancedRequest alternateQuery = SearchVectorsEnhancedRequest.builder()
                                                                                      .searchVector(ALTERNATE_QUERY_VECTOR)
                                                                                      .topK(5)
                                                                                      .searchConditionExpression(hashCondition(
                                                                                          "articles"))
                                                                                      .build();

            List<SearchResultItem<VectorRecord>> primaryResults = searchVectors(COSINE_INDEX, primaryQuery);
            List<SearchResultItem<VectorRecord>> alternateResults = searchVectors(COSINE_INDEX, alternateQuery);

            assertThat(primaryResults).isNotEmpty();
            assertThat(alternateResults).isNotEmpty();
            assertThat(primaryResults.get(0).item().getSk())
                .isNotEqualTo(alternateResults.get(0).item().getSk());
        }

        @Test
        void searchVectors_dotProductIndex_resultsOrderedByScoreDescending() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(DOT_PRODUCT_QUERY_VECTOR)
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(DOT_PRODUCT_INDEX, request);

            assertThat(results).isNotEmpty();
            assertThat(results)
                .extracting(SearchResultItem::score)
                .isSortedAccordingTo(Comparator.reverseOrder());
            assertThat(results.get(0).item().getSk()).isEqualTo("exact-match");
        }

        @Test
        void searchVectors_euclideanIndex_zeroQueryVector_nearestItemRanksFirst() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(new float[] {0.0f, 0.0f, 0.0f, 0.0f})
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(EUCLIDEAN_INDEX, request);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).item().getSk()).isEqualTo("near-match");
        }

        @Test
        void searchVectors_sameQueryVectorAcrossIndexes_returnsSameSortKeySet() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            Set<String> cosineSortKeys = searchVectors(COSINE_INDEX, request).stream()
                                                                             .map(r -> r.item().getSk())
                                                                             .collect(Collectors.toSet());
            Set<String> dotSortKeys = searchVectors(DOT_PRODUCT_INDEX, request).stream()
                                                                               .map(r -> r.item().getSk())
                                                                               .collect(Collectors.toSet());
            Set<String> euclideanSortKeys = searchVectors(EUCLIDEAN_INDEX, request).stream()
                                                                                   .map(r -> r.item().getSk())
                                                                                   .collect(Collectors.toSet());

            assertThat(cosineSortKeys).isEqualTo(dotSortKeys).isEqualTo(euclideanSortKeys);
        }

        @Test
        void searchVectors_sameItemAcrossIndexes_cosineScoresBounded_dotScoresUnbounded() {
            VectorRecord amplifiedRecord = new VectorRecord()
                .setPk("articles")
                .setSk("dot-unbounded-item")
                .setCategory("science")
                .setDescription("Amplified dot embedding")
                .setEmbeddingDot(toFloatList(new float[] {3.0f, 0.0f, 0.0f, 0.0f}));
            executePut(amplifiedRecord);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(DOT_PRODUCT_QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                double cosineScore = scoreFor(
                    searchVectors(COSINE_INDEX, request),
                    "exact-match");
                List<SearchResultItem<VectorRecord>>
                    dotResults =
                    searchVectorsUntilContains(
                        DOT_PRODUCT_INDEX, request,
                        "dot-unbounded-item");
                double dotScore = scoreFor(
                    dotResults, "dot-unbounded-item");

                assertThat(cosineScore).isBetween(0.0, 1.0);
                assertThat(dotScore).isGreaterThan(1.0);
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("dot-unbounded-item")
                                 .build());
            }
        }

        @Test
        void searchVectors_emptyAttributesToProject_usesIndexLevelProjection() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<VectorRecord>> cosineResults = searchVectors(COSINE_INDEX, request);
            List<SearchResultItem<VectorRecord>> dotResults = searchVectors(DOT_PRODUCT_INDEX, request);
            List<SearchResultItem<VectorRecord>> euclideanResults = searchVectors(EUCLIDEAN_INDEX, request);

            assertThat(cosineResults).isNotEmpty();
            assertThat(dotResults).isNotEmpty();
            assertThat(euclideanResults).isNotEmpty();
            cosineResults.forEach(r -> assertProjectedAttributes(r.item(), true, true));
            dotResults.forEach(r -> assertProjectedAttributes(r.item(), false, false));
            euclideanResults.forEach(r -> assertProjectedAttributes(r.item(), true, false));
        }

        @Test
        void searchVectors_topK100WithFourMatchingItems_returnsFourResults() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .topK(100)
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).hasSize(4);
        }

        @Test
        void searchVectors_topKNotSet_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "missing field `TopK`");
        }
    }

    @Nested
    class WritePathTests {

        @Test
        void writePath_validVectorPersistedAndSearchable() {
            VectorRecord newRecord = new VectorRecord()
                .setPk("articles").setSk("write-test-item").setCategory("science")
                .setDescription("Write path test")
                .setEmbeddingCosine(toFloatList(QUERY_VECTOR))
                .setEmbeddingDot(toFloatList(QUERY_VECTOR))
                .setEmbeddingEuclidean(toFloatList(QUERY_VECTOR));
            executePut(newRecord);

            SearchVectorsEnhancedRequest request =
                SearchVectorsEnhancedRequest.builder()
                                            .searchVector(QUERY_VECTOR)
                                            .topK(10)
                                            .searchConditionExpression(hashCondition("articles"))
                                            .build();

            try {
                List<SearchResultItem<VectorRecord>>
                    results = searchVectorsUntilContains(
                    COSINE_INDEX, request,
                    "write-test-item");
                List<String> sks = results.stream()
                                          .map(r -> r.item().getSk())
                                          .collect(Collectors.toList());
                assertThat(sks).contains("write-test-item");
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("write-test-item")
                                 .build());
            }
        }

        @Test
        void writePath_updateVectorThenSearch_cosineEmbeddingOnlyUpdated_rankingReflectsNewVector() {
            VectorRecord vectorRecord = new VectorRecord()
                .setPk("articles")
                .setSk("update-test-item")
                .setCategory("science")
                .setDescription("Update path test")
                .setEmbeddingCosine(toFloatList(ORTHOGONAL_2_VECTOR))
                .setEmbeddingDot(toFloatList(ORTHOGONAL_2_VECTOR))
                .setEmbeddingEuclidean(toFloatList(ORTHOGONAL_2_VECTOR));
            executePut(vectorRecord);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                vectorRecord.setEmbeddingCosine(
                    toFloatList(QUERY_VECTOR));
                executePut(vectorRecord);

                List<String> sortKeys =
                    searchVectorsUntilContains(
                        COSINE_INDEX, request,
                        "update-test-item")
                        .stream()
                        .map(r -> r.item().getSk())
                        .collect(Collectors.toList());
                assertThat(sortKeys)
                    .contains("update-test-item");
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("update-test-item")
                                 .build());
            }
        }

        @Test
        void writePath_cosineEmbeddingOnlySet_itemAppearsInCosineIndexResults() {
            VectorRecord record = new VectorRecord()
                .setPk("articles")
                .setSk("cosine-only-item")
                .setCategory("science")
                .setDescription("Cosine embedding only");
            setEmbeddingForIndex(record, COSINE_INDEX, QUERY_VECTOR);
            executePut(record);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                List<String> sortKeys =
                    searchVectorsUntilContains(
                        COSINE_INDEX, request,
                        "cosine-only-item")
                        .stream()
                        .map(r -> r.item().getSk())
                        .collect(Collectors.toList());
                assertThat(sortKeys)
                    .contains("cosine-only-item");
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("cosine-only-item")
                                 .build());
            }
        }

        @Test
        void writePath_dotEmbeddingAbsent_itemAbsentFromDotProductIndexResults() {
            VectorRecord record = new VectorRecord()
                .setPk("articles")
                .setSk("no-dot-item")
                .setCategory("science")
                .setDescription("Missing dot embedding")
                .setEmbeddingCosine(toFloatList(QUERY_VECTOR))
                .setEmbeddingEuclidean(toFloatList(QUERY_VECTOR));
            executePut(record);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                List<String> sortKeys = searchVectors(DOT_PRODUCT_INDEX, request).stream()
                                                                                 .map(r -> r.item().getSk())
                                                                                 .collect(Collectors.toList());
                assertThat(sortKeys).doesNotContain("no-dot-item");
            } finally {
                executeDelete(Key.builder().partitionValue("articles").sortValue("no-dot-item").build());
            }
        }

        @Test
        void writePath_deleteItem_itemAbsentFromSearchResults() {
            VectorRecord record = new VectorRecord()
                .setPk("articles")
                .setSk("delete-test-item")
                .setCategory("science")
                .setDescription("Delete path test")
                .setEmbeddingCosine(toFloatList(QUERY_VECTOR))
                .setEmbeddingDot(toFloatList(QUERY_VECTOR))
                .setEmbeddingEuclidean(toFloatList(QUERY_VECTOR));
            executePut(record);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            searchVectorsUntilContains(COSINE_INDEX, request, "delete-test-item");

            executeDelete(Key.builder()
                             .partitionValue("articles")
                             .sortValue("delete-test-item")
                             .build());

            List<String> sortKeys = searchVectorsUntilAbsent(
                COSINE_INDEX, request, "delete-test-item")
                .stream()
                .map(r -> r.item().getSk())
                .collect(Collectors.toList());
            assertThat(sortKeys).doesNotContain("delete-test-item");
        }

        @Test
        void writePath_updateClearsEmbedding_itemAbsentFromSearchResults() {
            VectorRecord record = new VectorRecord()
                .setPk("articles")
                .setSk("clear-embedding-item")
                .setCategory("science")
                .setDescription("Clear embedding test")
                .setEmbeddingCosine(toFloatList(QUERY_VECTOR))
                .setEmbeddingDot(toFloatList(QUERY_VECTOR))
                .setEmbeddingEuclidean(toFloatList(QUERY_VECTOR));
            executePut(record);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                searchVectorsUntilContains(COSINE_INDEX, request, "clear-embedding-item");

                record.setEmbeddingCosine(null);
                record.setEmbeddingDot(null);
                record.setEmbeddingEuclidean(null);
                executePut(record);

                List<String> sortKeys = searchVectorsUntilAbsent(
                    COSINE_INDEX, request, "clear-embedding-item")
                    .stream()
                    .map(r -> r.item().getSk())
                    .collect(Collectors.toList());
                assertThat(sortKeys).doesNotContain("clear-embedding-item");
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("clear-embedding-item")
                                 .build());
            }
        }

        @Test
        void writePath_updateNonVectorFieldsOnly_itemStillSearchable() {
            VectorRecord record = new VectorRecord()
                .setPk("articles")
                .setSk("metadata-update-item")
                .setCategory("science")
                .setDescription("Original description")
                .setEmbeddingCosine(toFloatList(QUERY_VECTOR))
                .setEmbeddingDot(toFloatList(QUERY_VECTOR))
                .setEmbeddingEuclidean(toFloatList(QUERY_VECTOR));
            executePut(record);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                searchVectorsUntilContains(COSINE_INDEX, request, "metadata-update-item");

                record.setDescription("Updated description only");
                executePut(record);

                // Retry until the updated description is visible in search results,
                // not just until the item exists (it may return stale data).
                SearchResultItem<VectorRecord> result = null;
                for (int attempt = 0; attempt < DEFAULT_SEARCH_RETRY_ATTEMPTS; attempt++) {
                    List<SearchResultItem<VectorRecord>> results =
                        executeSearch(COSINE_INDEX, request);
                    result = results.stream()
                                    .filter(r -> "metadata-update-item".equals(r.item().getSk()))
                                    .findFirst()
                                    .orElse(null);
                    if (result != null && "Updated description only".equals(result.item().getDescription())) {
                        break;
                    }
                    sleepBetweenRetries(attempt);
                }
                assertThat(result).isNotNull();
                assertThat(result.item().getDescription())
                    .isEqualTo("Updated description only");
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("metadata-update-item")
                                 .build());
            }
        }

        @Test
        void writePath_missingInlineFilterAttribute_itemSearchableWithHashOnlyCondition() {
            VectorRecord record = new VectorRecord()
                .setPk("articles")
                .setSk("no-category-item")
                .setDescription("Missing inline filter attribute")
                .setEmbeddingCosine(toFloatList(QUERY_VECTOR))
                .setEmbeddingDot(toFloatList(QUERY_VECTOR))
                .setEmbeddingEuclidean(toFloatList(QUERY_VECTOR));
            executePut(record);

            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(10)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            try {
                List<String> sortKeys =
                    searchVectorsUntilContains(
                        COSINE_INDEX, request, "no-category-item")
                        .stream()
                        .map(r -> r.item().getSk())
                        .collect(Collectors.toList());
                assertThat(sortKeys).contains("no-category-item");
            } finally {
                executeDelete(Key.builder()
                                 .partitionValue("articles")
                                 .sortValue("no-category-item")
                                 .build());
            }
        }
    }

    @Nested
    class DocumentApiTests {

        @Test
        void searchVectors_documentApi_returnsEnhancedDocuments() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<EnhancedDocument>> results = searchVectorsDocument(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).item().getString("pk")).isNotNull();
        }

        @Test
        void searchVectors_documentApi_scoresPresent() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<EnhancedDocument>> results = searchVectorsDocument(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertThat(r.score()).isGreaterThanOrEqualTo(0.0));
        }

        @Test
        void searchVectors_documentApi_projectionBehavior() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .attributesToProject("pk", "category")
                .build();

            List<SearchResultItem<EnhancedDocument>> results = searchVectorsDocument(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item().getString("pk")).isNotNull();
                assertThat(r.item().getString("description")).isNull();
            });
        }

        @Test
        void searchVectors_documentApi_withCondition() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<EnhancedDocument>> results = searchVectorsDocument(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> assertThat(r.item().getString("pk")).isEqualTo("articles"));
        }

        @Test
        void searchVectors_documentApi_withoutCustomConverterProvider_returnsProjectedAttributes() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .attributesToProject("pk", "category")
                .build();

            List<SearchResultItem<EnhancedDocument>> results =
                searchVectorsDocument(COSINE_INDEX, request, false);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item().getString("pk")).isEqualTo("articles");
                assertThat(r.item().getString("category")).isNotNull();
            });
        }

        @Test
        void searchVectors_documentApi_withCustomConverterProvider_searchSucceedsAndConverterApplied() {
            EnhancedDocument docWithInteger = EnhancedDocument.builder()
                                                              .putNumber("testInteger", 7)
                                                              .build();
            Map<String, AttributeValue> mapped = documentSchema(true).itemToMap(docWithInteger, true);
            assertThat(mapped.get("testInteger").n()).isEqualTo("17");

            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            List<SearchResultItem<EnhancedDocument>> defaultResults =
                searchVectorsDocument(COSINE_INDEX, request, false);
            List<SearchResultItem<EnhancedDocument>> customResults =
                searchVectorsDocument(COSINE_INDEX, request, true);

            assertThat(customResults).isNotEmpty();
            assertThat(customResults.stream().map(r -> r.item().getString("sk")).collect(Collectors.toList()))
                .containsExactlyElementsOf(defaultResults.stream()
                                                         .map(r -> r.item().getString("sk"))
                                                         .collect(Collectors.toList()));
        }

        @Test
        void searchVectors_documentApi_minimalSchemaBuilder_matchesDefaultProviderBehavior() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            DocumentTableSchema minimalSchema = DocumentTableSchema.builder()
                                                                   .addIndexPartitionKey(primaryIndexName(), "pk",
                                                                                         AttributeValueType.S)
                                                                   .addIndexSortKey(primaryIndexName(), "sk",
                                                                                    AttributeValueType.S)
                                                                   .vectorIndexes(VECTOR_INDEX_DEFINITIONS)
                                                                   .build();

            List<SearchResultItem<EnhancedDocument>> defaultResults =
                searchVectorsDocument(COSINE_INDEX, request, false);
            List<SearchResultItem<EnhancedDocument>> minimalResults =
                awaitNonEmptySearchResults(
                    () -> executeSearchDocument(COSINE_INDEX, request, minimalSchema));

            assertThat(minimalResults.stream().map(r -> r.item().getString("sk")).collect(Collectors.toList()))
                .containsExactlyElementsOf(defaultResults.stream()
                                                         .map(r -> r.item().getString("sk"))
                                                         .collect(Collectors.toList()));
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class NoHashSchemaTests {
        private final EnhancedVectorIndex noHashIndex =
            tableWideVectorIndex();
        private final StaticTableSchema<VectorRecord> noHashSchema =
            tableSchemaForVectorIndex(noHashIndex);

        @Test
        void createTable_vectorIndexWithoutSearchSchema_describeTableOmitsSearchSchema() {
            DescribeTableResponse response =
                describeTable(NOHASH_TABLE_NAME);
            assertThat(response.table().vectorIndexes())
                .hasSize(1);
            assertThat(response.table().vectorIndexes()
                               .get(0).searchSchema()).isEmpty();
        }

        @Test
        void searchVectors_tableWideIndex_withoutHashCondition_returnsResultsFromAllPartitions() {
            SearchVectorsEnhancedRequest request =
                SearchVectorsEnhancedRequest.builder()
                                            .searchVector(QUERY_VECTOR)
                                            .topK(10)
                                            .build();

            List<SearchResultItem<VectorRecord>> results =
                awaitSearchResultCount(
                    () -> dedicatedSearch(
                        NOHASH_TABLE_NAME, noHashSchema,
                        TABLE_WIDE_INDEX, request),
                    2);

            Set<String> partitionKeys = results.stream()
                                               .map(r -> r.item().getPk())
                                               .collect(Collectors.toSet());

            assertThat(partitionKeys)
                .containsExactlyInAnyOrder(
                    "articles", "reviews");
        }

        @Test
        void searchVectors_tableWideIndex_withHashCondition_failsAtService() {
            SearchVectorsEnhancedRequest request =
                SearchVectorsEnhancedRequest.builder()
                                            .searchVector(QUERY_VECTOR)
                                            .topK(5)
                                            .searchConditionExpression(
                                                hashCondition("articles"))
                                            .build();

            failsAtService(
                () -> dedicatedSearch(
                    NOHASH_TABLE_NAME, noHashSchema,
                    TABLE_WIDE_INDEX, request));
        }
    }

    @Nested
    class DimensionVariantTests {

        @Test
        void searchVectors_128DimensionIndex_putAndSearch_returnsMatchingItemWithScore() {
            verifyDimensionRoundTrip(128);
        }

        @Test
        void searchVectors_512DimensionIndex_putAndSearch_returnsMatchingItemWithScore() {
            verifyDimensionRoundTrip(512);
        }

        @Test
        void searchVectors_1024DimensionIndex_putAndSearch_returnsMatchingItemWithScore() {
            verifyDimensionRoundTrip(1024);
        }

        @Test
        void searchVectors_4096DimensionIndex_putAndSearch_returnsMatchingItemWithScore() {
            prepareDimensionTable(4096);
            verifyDimensionRoundTrip(4096);
        }

        private void verifyDimensionRoundTrip(int dimensions) {
            String tableName = dimensionTableName(dimensions);
            EnhancedVectorIndex index =
                vectorIndexWithDimensions(dimensions);
            StaticTableSchema<VectorRecord> schema =
                tableSchemaForVectorIndex(index);
            float[] matchingVector =
                generateNormalizedVector(dimensions);
            String matchingSk = "item-" + dimensions;
            String otherSk = "other-" + dimensions;

            VectorIndexDescription indexDescription =
                describeTable(tableName).table().vectorIndexes().get(0);
            assertThat(indexDescription.indexName())
                .isEqualTo(index.indexName());
            assertThat(indexDescription.dimensions())
                .isEqualTo(dimensions);
            assertThat(indexDescription.distanceFunction())
                .hasToString("COSINE");
            assertThat(indexDescription
                           .vectorAttribute().attributeName())
                .isEqualTo("embedding");
            assertThat(indexDescription.searchSchema())
                .hasSize(1)
                .anySatisfy(element -> {
                    assertThat(element.attributeName())
                        .isEqualTo("pk");
                    assertThat(element
                                   .searchSchemaElementType())
                        .hasToString("HASH");
                });

            SearchVectorsEnhancedRequest request =
                SearchVectorsEnhancedRequest.builder()
                                            .searchVector(matchingVector)
                                            .topK(2)
                                            .searchConditionExpression(
                                                hashCondition("dimension-test"))
                                            .build();

            List<SearchResultItem<VectorRecord>> results =
                awaitSearchResultCount(
                    () -> dedicatedSearch(
                        tableName, schema,
                        index.indexName(), request),
                    2,
                    DEFAULT_SEARCH_RETRY_ATTEMPTS);

            assertThat(results).hasSize(2);

            VectorRecord retrievedMatch =
                results.get(0).item();
            assertThat(retrievedMatch.getPk())
                .isEqualTo("dimension-test");
            assertThat(retrievedMatch.getSk())
                .isEqualTo(matchingSk);
            assertThat(retrievedMatch.getCategory())
                .isEqualTo("test");
            assertThat(retrievedMatch.getDescription())
                .isEqualTo("Dimension test item");

            VectorRecord retrievedOther =
                results.get(1).item();
            assertThat(retrievedOther.getSk())
                .isEqualTo(otherSk);
            assertThat(retrievedOther.getCategory())
                .isEqualTo("other");
            assertThat(retrievedOther.getDescription())
                .isEqualTo("Other dimension test item");

            assertScoresOrderedByRelevance(
                results, DistanceFunction.COSINE);
            assertMoreRelevantThan(
                results, DistanceFunction.COSINE,
                matchingSk, otherSk);
        }
    }

    @Nested
    class CreateTableLimitTests {

        @Test
        void createTable_tooManyVectorIndexes_failsAtService() {
            List<EnhancedVectorIndex> sixIndexes =
                IntStream.range(0, 6)
                         .mapToObj(i -> EnhancedVectorIndex.builder()
                                                           .indexName("vector-index-" + i)
                                                           .vectorAttributeName("embedding-" + i)
                                                           .distanceFunction(DistanceFunction.COSINE)
                                                           .dimensions(4)
                                                           .projection(p -> p.projectionType(ProjectionType.ALL))
                                                           .addSearchSchemaElement(b -> b
                                                               .attributeName("pk")
                                                               .searchSchemaElementType(HASH))
                                                           .build()
                         ).collect(Collectors.toList());
            CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                           .vectorIndexes(sixIndexes)
                                                                           .build();
            String tempTableName = createSearchVectorsTableName();

            assertThrowsContaining(() -> executeTempTableCreate(tempTableName, VECTOR_TABLE_SCHEMA, request),
                                   DynamoDbException.class,
                                   "One or more parameter values were invalid: "
                                   + "VectorIndex count exceeds the per-table limit of 5");
        }

        @Test
        void createTable_dimensionsExceedsLimit_failsAtService() {
            CreateTableEnhancedRequest request =
                CreateTableEnhancedRequest.builder()
                                          .vectorIndexes(
                                              EnhancedVectorIndex.builder()
                                                                 .indexName("big-dims-index")
                                                                 .vectorAttributeName("embedding")
                                                                 .distanceFunction(DistanceFunction.COSINE)
                                                                 .dimensions(5000)
                                                                 .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                 .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                               .searchSchemaElementType(HASH))
                                                                 .build())
                                          .build();
            String tempTableName = createSearchVectorsTableName();

            assertThrowsContaining(() -> executeTempTableCreate(tempTableName, VECTOR_TABLE_SCHEMA, request),
                                   DynamoDbException.class,
                                   "One or more parameter values were invalid: "
                                   + "Number of dimensions must be between 1 and 4096 inclusive.");
        }

        @Test
        void createTable_tooManyInlineFilters_failsAtService() {
            EnhancedVectorIndex vectorIndex = buildVectorIndexWithInlineFilters(19);
            CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                           .vectorIndexes(vectorIndex)
                                                                           .build();
            String tempTableName = createSearchVectorsTableName();

            assertThrowsContaining(() -> executeTempTableCreate(tempTableName,
                                                                createTableLimitTestSchema(),
                                                                request),
                                   DynamoDbException.class,
                                   "One or more parameter values were invalid: Value '19' at 'SearchSchema' failed"
                                   + " to satisfy constraint: Member must have INLINE_FILTER count less than or equal to 18");
        }

        @Test
        void createTable_duplicateVectorIndexNames_failsAtService() {
            EnhancedVectorIndex firstIndex = EnhancedVectorIndex.builder()
                                                                .indexName("duplicate-index")
                                                                .vectorAttributeName("embeddingCosine")
                                                                .distanceFunction(DistanceFunction.COSINE)
                                                                .dimensions(4)
                                                                .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                              .searchSchemaElementType(HASH))
                                                                .build();
            EnhancedVectorIndex secondIndex = EnhancedVectorIndex.builder()
                                                                 .indexName("duplicate-index")
                                                                 .vectorAttributeName("embeddingDot")
                                                                 .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                                 .dimensions(4)
                                                                 .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                 .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                               .searchSchemaElementType(HASH))
                                                                 .build();
            CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                           .vectorIndexes(firstIndex, secondIndex)
                                                                           .build();
            String tempTableName = createSearchVectorsTableName();

            assertThrowsContaining(() -> executeTempTableCreate(tempTableName, VECTOR_TABLE_SCHEMA, request),
                                   DynamoDbException.class,
                                   "One or more parameter values were invalid: "
                                   + "Duplicate index name: duplicate-index");
        }

        @Test
        void createTable_tooManyProjectedAttributes_failsAtService() {
            EnhancedVectorIndex vectorIndex = buildVectorIndexWithProjectedAttributes(21);
            CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                           .vectorIndexes(vectorIndex)
                                                                           .build();
            String tempTableName = createSearchVectorsTableName();

            assertThrowsContaining(() -> executeTempTableCreate(tempTableName,
                                                                createTableLimitTestSchema(),
                                                                request),
                                   DynamoDbException.class,
                                   "1 validation error detected: Value '[proj0, proj1, proj2, proj3, proj4, "
                                   + "proj5, proj6, proj7, proj8, proj9, proj10, proj11, proj12, proj13, proj14, proj15, proj16, "
                                   + "proj17, proj18, proj19, proj20]' at 'vectorIndexes.1.member.projection.nonKeyAttributes' "
                                   + "failed to satisfy constraint: Member must have length less than or equal to 20");
        }
    }

    @Nested
    class LowLevelClientSmokeTests {

        @Test
        void searchVectors_lowLevelClient_returnsResults() {
            SearchVectorsRequest request = SearchVectorsRequest.builder()
                                                               .tableName(sharedTableName())
                                                               .indexName(COSINE_INDEX)
                                                               .searchVector(toSearchVectorAttributeValues(QUERY_VECTOR))
                                                               .topK(5)
                                                               .searchConditionExpression("#pk = :pkval")
                                                               .expressionAttributeNames(
                                                                   Collections.singletonMap("#pk", "pk"))
                                                               .expressionAttributeValues(
                                                                   Collections.singletonMap(
                                                                       ":pkval",
                                                                       AttributeValue.builder().s("articles").build()))
                                                               .build();

            SearchVectorsResponse response = executeLowLevelSearchVectors(request);

            assertThat(response.searchResults()).isNotEmpty();
            assertThat(response.searchResults().get(0).score()).isNotNull();
            assertThat(response.searchResults().get(0).item().get("pk").s())
                .isEqualTo("articles");
        }
    }

    @Nested
    class NegativeTests {

        @Test
        void searchVectors_emptySearchVector_failsAtService() {
            SearchVectorsEnhancedRequest request =
                SearchVectorsEnhancedRequest.builder()
                                            .searchVector(new float[0])
                                            .topK(5)
                                            .searchConditionExpression(hashCondition("articles"))
                                            .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "1 validation error detected: Value at 'SearchVector' failed to satisfy constraint: "
                                   + "Member must have length greater than or equal to 1");
        }

        @Test
        void searchVectors_nullSearchVector_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "missing field `SearchVector`");
        }

        @Test
        void searchVectors_topKZero_failsAtService() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .topK(0).build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "validation error detected: Value at 'TopK' failed to satisfy constraint: "
                                   + "Member must have value greater than or equal to 1");
        }

        @Test
        void searchVectors_topKNegative_failsAtService() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .topK(-1).build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "1 validation error detected: Value at 'TopK' failed to satisfy constraint: "
                                   + "Member must have value greater than or equal to 1");
        }

        @Test
        void searchVectors_topKExceedsLimit_failsAtService() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .topK(101).build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "Provided TopK value '101' is out of valid range. "
                                   + "The value must be between 1 and 100 inclusive");
        }

        @Test
        void searchVectors_wrongDimensionCount_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(new float[] {1.0f, 0.0f, 0.0f})
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "Input search vector dimension 3 does not match vector index dimension 4");
        }

        @Test
        void writePath_invalidDimensionCount_failsAtService() {
            VectorRecord badRecord = new VectorRecord()
                .setPk("articles")
                .setSk("bad-dims")
                .setCategory("science")
                .setDescription("Bad dimensions")
                .setEmbeddingCosine(toFloatList(new float[] {1.0f, 0.0f, 0.0f}))
                .setEmbeddingDot(toFloatList(new float[] {1.0f, 0.0f, 0.0f}))
                .setEmbeddingEuclidean(toFloatList(new float[] {1.0f, 0.0f, 0.0f}));

            assertThrowsContaining(() -> executePut(badRecord),
                                   DynamoDbException.class,
                                   "Invalid size for parameter embeddingCosine, Expected: 4, Actual: 3");
        }

        @Test
        void searchVectors_nonExistentIndex_failsAtService() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .build();

            assertThrowsContaining(() -> searchVectors("no-such-index", request),
                                   IllegalArgumentException.class,
                                   "Attempt to execute an operation that requires a vector index without "
                                   + "defining the index in the table metadata. Index name: no-such-index");
        }

        @Test
        void searchVectors_missingHashCondition_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(5)
                                                                               .build();
            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "SearchConditionExpression must be provided when SearchSchema has a HASH key");
        }

        @Test
        void searchVectors_searchVectorContainsNaN_throwsIllegalArgumentExceptionBeforeServiceCall() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(new float[] {1.0f, Float.NaN, 0.0f
                                                                                   , 0.0f})
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   IllegalArgumentException.class,
                                   "NaN is not supported by the default converters.");
        }

        @Test
        void searchVectors_searchVectorContainsInfinity_throwsIllegalArgumentExceptionBeforeServiceCall() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(new float[] {1.0f,
                                                                                                          Float.POSITIVE_INFINITY, 0.0f, 0.0f})
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   IllegalArgumentException.class,
                                   "Infinite numbers are not supported by the default converters.");
        }

        @Test
        void writePath_embeddingContainsNaN_throwsIllegalArgumentExceptionBeforeServiceCall() {
            VectorRecord badRecord = new VectorRecord()
                .setPk("articles")
                .setSk("nan-embedding")
                .setCategory("science")
                .setDescription("NaN embedding")
                .setEmbeddingCosine(toFloatList(new float[] {1.0f, Float.NaN, 0.0f, 0.0f}));

            assertThrowsContaining(() -> executePut(badRecord),
                                   IllegalArgumentException.class,
                                   "NaN is not supported by the default converters.");
        }

        @Test
        void writePath_embeddingContainsInfinity_throwsIllegalArgumentExceptionBeforeServiceCall() {
            VectorRecord badRecord = new VectorRecord()
                .setPk("articles")
                .setSk("infinity-embedding")
                .setCategory("science")
                .setDescription("Infinity embedding")
                .setEmbeddingCosine(toFloatList(new float[] {1.0f, Float.POSITIVE_INFINITY, 0.0f, 0.0f}));

            assertThrowsContaining(() -> executePut(badRecord),
                                   IllegalArgumentException.class,
                                   "Infinite numbers are not supported by the default converters.");
        }

        @Test
        void searchVectors_inlineFilterExpressionWithoutHashCondition_failsAtService() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(inlineFilterOnlyCondition("science"))
                .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "SearchConditionExpression must have all HASH attributes"
                                   + " in configured SearchSchema");
        }

        @Test
        void searchVectors_malformedSearchConditionExpression_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(5)
                                                                               .searchConditionExpression(Expression.builder()
                                                                                                                    .expression("#pk === :pkval")
                                                                                                                    .putExpressionName("#pk", "pk")
                                                                                                                    .putExpressionValue(":pkval", AttributeValue.builder().s("articles").build())
                                                                                                                    .build())
                                                                               .build();

            failsAtService(() -> searchVectors(COSINE_INDEX, request));
        }

        @Test
        void searchVectors_emptySearchConditionExpression_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(QUERY_VECTOR)
                                                                               .topK(5)
                                                                               .searchConditionExpression(Expression.builder().expression("").build())
                                                                               .build();

            failsAtService(() -> searchVectors(COSINE_INDEX, request));
        }

        @Test
        void searchVectors_documentApi_projectNonExistentAttribute_returnsNullForMissingField() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .attributesToProject("pk", "nonExistentAttribute")
                .build();

            List<SearchResultItem<EnhancedDocument>> results = searchVectorsDocument(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item()).isNotNull();
                assertThat(r.item().getString("pk")).isEqualTo("articles");
                assertThat(r.item().getString("nonExistentAttribute")).isNull();
            });
        }

        @Test
        void searchVectors_projectionExpressionLeadingDot_returnsResultsWithNullItems() {
            SearchVectorsEnhancedRequest request = baseSearchRequest()
                .searchConditionExpression(hashCondition("articles"))
                .attributesToProject(".pk")
                .build();

            List<SearchResultItem<VectorRecord>> results = searchVectors(COSINE_INDEX, request);

            assertThat(results).isNotEmpty();
            results.forEach(r -> {
                assertThat(r.item()).isNull();
                assertThat(r.score()).isGreaterThanOrEqualTo(0.0);
            });
        }

        @Test
        void searchVectors_queryVectorLongerThanIndexDimensions_failsAtService() {
            SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                               .searchVector(new float[] {1.0f, 0.0f, 0.0f,
                                                                                                          0.0f, 0.0f, 0.0f})
                                                                               .topK(5)
                                                                               .searchConditionExpression(hashCondition(
                                                                                   "articles"))
                                                                               .build();

            assertThrowsContaining(() -> searchVectors(COSINE_INDEX, request),
                                   DynamoDbException.class,
                                   "Input search vector dimension 6 does not match vector index dimension 4");
        }

        @Test
        void createTable_searchSchemaWithInlineFilterOnly_noHashElement_succeeds() {
            EnhancedVectorIndex filterOnlyIndex =
                EnhancedVectorIndex.builder()
                                   .indexName("filter-only-index")
                                   .vectorAttributeName("embeddingCosine")
                                   .distanceFunction(DistanceFunction.COSINE)
                                   .dimensions(4)
                                   .projection(p -> p.projectionType(
                                       ProjectionType.ALL))
                                   .addSearchSchemaElement(
                                       b -> b.attributeName("category")
                                             .searchSchemaElementType(
                                                 INLINE_FILTER))
                                   .build();
            CreateTableEnhancedRequest request =
                CreateTableEnhancedRequest.builder()
                                          .vectorIndexes(filterOnlyIndex)
                                          .build();

            if (!tableExistsCheck(FILTER_ONLY_TABLE_NAME)) {
                executeTempTableCreate(
                    FILTER_ONLY_TABLE_NAME,
                    VECTOR_TABLE_SCHEMA, request);
            }
            DescribeTableResponse response =
                describeTable(FILTER_ONLY_TABLE_NAME);
            assertThat(response.table().vectorIndexes())
                .hasSize(1);
            assertThat(response.table().vectorIndexes()
                               .get(0).indexName())
                .isEqualTo("filter-only-index");
        }
    }
}
