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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices.createUniqueFakeItemWithIndices;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import software.amazon.awssdk.core.SdkBytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.CompositeKeyFakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable;
import software.amazon.awssdk.services.dynamodb.paginators.QueryPublisher;

@RunWith(MockitoJUnitRunner.class)
public class QueryOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    private final FakeItem keyItem = createUniqueFakeItem();
    private final QueryOperation<FakeItem> queryOperation =
        QueryOperation.create(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                  .build());

    @Mock
    private DynamoDbClient mockDynamoDbClient;
    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;
    @Mock
    private QueryConditional mockQueryConditional;
    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        QueryRequest queryRequest = QueryRequest.builder().build();
        QueryIterable mockQueryIterable = mock(QueryIterable.class);
        when(mockDynamoDbClient.queryPaginator(any(QueryRequest.class))).thenReturn(mockQueryIterable);

        SdkIterable<QueryResponse> response = queryOperation.serviceCall(mockDynamoDbClient).apply(queryRequest);

        assertThat(response, is(mockQueryIterable));
        verify(mockDynamoDbClient).queryPaginator(queryRequest);
    }

    @Test
    public void getAsyncServiceCall_makesTheRightCallAndReturnsResponse() {
        QueryRequest queryRequest = QueryRequest.builder().build();
        QueryPublisher mockQueryPublisher = mock(QueryPublisher.class);
        when(mockDynamoDbAsyncClient.queryPaginator(any(QueryRequest.class))).thenReturn(mockQueryPublisher);

        SdkPublisher<QueryResponse> response =
            queryOperation.asyncServiceCall(mockDynamoDbAsyncClient).apply(queryRequest);

        assertThat(response, is(mockQueryPublisher));
        verify(mockDynamoDbAsyncClient).queryPaginator(queryRequest);
    }

    @Test
    public void generateRequest_nonDefault_usesQueryConditional() {
        Map<String, AttributeValue> keyItemMap = getAttributeValueMap(keyItem);
        Expression expression = Expression.builder().expression("test-expression").expressionValues(keyItemMap).build();
        when(mockQueryConditional.expression(any(), anyString())).thenReturn(expression);

        QueryOperation<FakeItem> query = QueryOperation.create(QueryEnhancedRequest.builder()
                                                                                   .queryConditional(mockQueryConditional)
                                                                                   .build());
        QueryRequest queryRequest = query.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);

        QueryRequest expectedQueryRequest = QueryRequest.builder()
                                                        .tableName(TABLE_NAME)
                                                        .keyConditionExpression("test-expression")
                                                        .expressionAttributeValues(keyItemMap)
                                                        .build();
        assertThat(queryRequest, is(expectedQueryRequest));
        verify(mockQueryConditional).expression(FakeItem.getTableSchema(), TableMetadata.primaryIndexName());
    }

    @Test
    public void generateRequest_defaultQuery_usesEqualTo() {
        QueryRequest queryRequest = queryOperation.generateRequest(FakeItem.getTableSchema(),
                                                                   PRIMARY_CONTEXT,
                                                                   null);

        QueryRequest expectedQueryRequest = QueryRequest.builder()
            .tableName(TABLE_NAME)
            .keyConditionExpression("#AMZN_MAPPED_id = :AMZN_MAPPED_id")
            .expressionAttributeValues(singletonMap(":AMZN_MAPPED_id",
                                                    AttributeValue.builder().s(keyItem.getId()).build()))
            .expressionAttributeNames(singletonMap("#AMZN_MAPPED_id", "id"))
            .build();
        assertThat(queryRequest, is(expectedQueryRequest));
    }

    @Test
    public void generateRequest_knowsHowToUseAnIndex() {
        FakeItemWithIndices fakeItem = createUniqueFakeItemWithIndices();
        QueryOperation<FakeItemWithIndices> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(fakeItem.getGsiId())))
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);

        assertThat(queryRequest.indexName(), is("gsi_1"));
    }

    @Test
    public void generateRequest_ascending() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .scanIndexForward(true)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.scanIndexForward(), is(true));
    }

    @Test
    public void generateRequest_descending() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .scanIndexForward(false)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.scanIndexForward(), is(false));
    }

    @Test
    public void generateRequest_limit() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .limit(123)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.limit(), is(123));
    }

    @Test
    public void generateRequest_filterExpression_withValues() {
        Map<String, AttributeValue> expressionValues = singletonMap(":test-key", stringValue("test-value"));
        Expression filterExpression = Expression.builder()
                                                .expression("test-expression")
                                                .expressionValues(expressionValues)
                                                .build();

        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .filterExpression(filterExpression)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.filterExpression(), is("test-expression"));
        assertThat(queryRequest.expressionAttributeValues(), hasEntry(":test-key", stringValue("test-value")));
    }

    @Test
    public void generateRequest_filterExpression_withoutValues() {
        Expression filterExpression = Expression.builder().expression("test-expression").build();

        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .filterExpression(filterExpression)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.filterExpression(), is("test-expression"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_filterExpression_withConflictingValues() {
        Map<String, AttributeValue> expressionValues = singletonMap(":AMZN_MAPPED_id", stringValue("test-value"));
        Map<String, String> expressionNames = singletonMap("#AMZN_MAPPED_id", "id");
        Expression filterExpression = Expression.builder()
                                                .expression("test-expression")
                                                .expressionNames(expressionNames)
                                                .expressionValues(expressionValues)
                                                .build();
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .filterExpression(filterExpression)
                                                      .build());
        queryToTest.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_consistentRead() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .consistentRead(true)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.consistentRead(), is(true));
    }

    @Test
    public void generateRequest_projectionExpression() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .attributesToProject("id")
                                                      .addAttributeToProject("version")
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.projectionExpression(), is("#AMZN_MAPPED_id,#AMZN_MAPPED_version"));
        assertThat(queryRequest.expressionAttributeNames().get("#AMZN_MAPPED_id"), is ("id"));
        assertThat(queryRequest.expressionAttributeNames().get("#AMZN_MAPPED_version"), is ("version"));
    }

    @Test
    public void generateRequest_hashKeyOnly_withExclusiveStartKey() {
        FakeItem exclusiveStartKey = createUniqueFakeItem();
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .exclusiveStartKey(FakeItem.getTableSchema()
                                                                                 .itemToMap(exclusiveStartKey,
                                                                                            FakeItem.getTableMetadata()
                                                                                                    .primaryKeys()))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
    }

    @Test
    public void generateRequest_secondaryIndex_exclusiveStartKeyUsesPrimaryAndSecondaryIndex() {
        FakeItemWithIndices exclusiveStartKey = createUniqueFakeItemWithIndices();
        Set<String> keyFields = new HashSet<>(FakeItemWithIndices.getTableSchema().tableMetadata().primaryKeys());
        keyFields.addAll(FakeItemWithIndices.getTableSchema().tableMetadata().indexKeys("gsi_1"));

        QueryOperation<FakeItemWithIndices> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .exclusiveStartKey(FakeItemWithIndices.getTableSchema()
                                                                                            .itemToMap(exclusiveStartKey,
                                                                                                       keyFields))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithIndices.getTableSchema(),
                                                                GSI_1_CONTEXT,
                                                                null);

        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("sort", AttributeValue.builder().s(exclusiveStartKey.getSort()).build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("gsi_id", AttributeValue.builder().s(exclusiveStartKey.getGsiId()).build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("gsi_sort", AttributeValue.builder().s(exclusiveStartKey.getGsiSort()).build()));
    }

    @Test
    public void generateRequest_hashAndSortKey_withExclusiveStartKey() {
        FakeItemWithSort exclusiveStartKey = createUniqueFakeItemWithSort();
        QueryOperation<FakeItemWithSort> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(keyItem.getId())))
                                                      .exclusiveStartKey(
                                                          FakeItemWithSort.getTableSchema()
                                                                          .itemToMap(
                                                                              exclusiveStartKey,
                                                                              FakeItemWithSort.getTableSchema()
                                                                                              .tableMetadata()
                                                                                              .primaryKeys()))
                          .build());

        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("sort", AttributeValue.builder().s(exclusiveStartKey.getSort()).build()));
    }

    @Test
    public void transformResults_multipleItems_returnsCorrectItems() {
        List<FakeItem> queryResultItems = generateFakeItemList();
        List<Map<String, AttributeValue>> queryResultMaps =
            queryResultItems.stream().map(QueryOperationTest::getAttributeValueMap).collect(toList());

        QueryResponse queryResponse = generateFakeQueryResults(queryResultMaps);

        Page<FakeItem> queryResultPage = queryOperation.transformResponse(queryResponse,
                                                                          FakeItem.getTableSchema(),
                                                                          PRIMARY_CONTEXT,
                                                                          null);

        assertThat(queryResultPage.items(), is(queryResultItems));
    }

    @Test
    public void transformResults_multipleItems_setsLastEvaluatedKey() {
        List<FakeItem> queryResultItems = generateFakeItemList();
        FakeItem lastEvaluatedKey = createUniqueFakeItem();
        List<Map<String, AttributeValue>> queryResultMaps =
            queryResultItems.stream().map(QueryOperationTest::getAttributeValueMap).collect(toList());

        QueryResponse queryResponse = generateFakeQueryResults(queryResultMaps,
                                                               getAttributeValueMap(lastEvaluatedKey));

        Page<FakeItem> queryResultPage = queryOperation.transformResponse(queryResponse,
                                                                          FakeItem.getTableSchema(),
                                                                          PRIMARY_CONTEXT,
                                                                          null);

        assertThat(queryResultPage.lastEvaluatedKey(), is(getAttributeValueMap(lastEvaluatedKey)));
    }

    @Test
    public void queryItem_withExtension_correctlyTransformsItem() {
        List<FakeItem> queryResultItems = generateFakeItemList();
        List<FakeItem> modifiedResultItems = generateFakeItemList();

        List<Map<String, AttributeValue>> queryResultMap =
            queryResultItems.stream().map(QueryOperationTest::getAttributeValueMap).collect(toList());

        ReadModification[] readModifications =
            modifiedResultItems.stream()
                              .map(QueryOperationTest::getAttributeValueMap)
                              .map(attributeMap -> ReadModification.builder().transformedItem(attributeMap).build())
                              .collect(toList())
                              .toArray(new ReadModification[]{});

        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(readModifications[0], Arrays.copyOfRange(readModifications, 1, readModifications.length));

        QueryResponse queryResponse = generateFakeQueryResults(queryResultMap);

        Page<FakeItem> queryResultPage = queryOperation.transformResponse(queryResponse,
                                                                          FakeItem.getTableSchema(),
                                                                          PRIMARY_CONTEXT,
                                                                          mockDynamoDbEnhancedClientExtension);

        assertThat(queryResultPage.items(), is(modifiedResultItems));
        InOrder inOrder = Mockito.inOrder(mockDynamoDbEnhancedClientExtension);
        queryResultMap.forEach(
            attributeMap -> inOrder.verify(mockDynamoDbEnhancedClientExtension)
                                   .afterRead(
                                       DefaultDynamoDbExtensionContext.builder()
                                                                      .tableMetadata(FakeItem.getTableMetadata())
                                                                      .operationContext(PRIMARY_CONTEXT)
                                                                      .tableSchema(FakeItem.getTableSchema())
                                                                      .items(attributeMap).build()));
    }

    private static QueryResponse generateFakeQueryResults(List<Map<String, AttributeValue>> queryItemMapsPage) {
        return QueryResponse.builder().items(queryItemMapsPage).build();
    }

    private static QueryResponse generateFakeQueryResults(List<Map<String, AttributeValue>> queryItemMapsPage,
                                                          Map<String, AttributeValue> lastEvaluatedKey) {
        return QueryResponse.builder().items(queryItemMapsPage).lastEvaluatedKey(lastEvaluatedKey).build();

    }

    private static List<FakeItem> generateFakeItemList() {
        return IntStream.range(0, 3).mapToObj(ignored -> createUniqueFakeItem()).collect(toList());
    }

    private static Map<String, AttributeValue> getAttributeValueMap(FakeItem fakeItem) {
        return singletonMap("id", AttributeValue.builder().s(fakeItem.getId()).build());
    }

    private static final OperationContext GSI_COMPOSITE_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi1");

    @Test
    public void generateRequest_gsiCompositeKeys_partitionKeysOnly() {
        Key compositeKey = Key.builder()
                              .partitionValues(Arrays.asList(
                                  AttributeValue.builder().s("gsiKey1").build(),
                                  AttributeValue.builder().s("gsiKey2").build()))
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        Map<String, String> names = new HashMap<>();
        names.put("#AMZN_MAPPED_gsiKey1", "gsiKey1");
        names.put("#AMZN_MAPPED_gsiKey2", "gsiKey2");

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":AMZN_MAPPED_gsiKey1", AttributeValue.builder().s("gsiKey1").build());
        values.put(":AMZN_MAPPED_gsiKey2", AttributeValue.builder().s("gsiKey2").build());

        QueryRequest expectedQueryRequest = QueryRequest.builder()
                                                        .tableName(TABLE_NAME)
                                                        .indexName("gsi1")
                                                        .keyConditionExpression("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2")
                                                        .expressionAttributeNames(names)
                                                        .expressionAttributeValues(values)
                                                        .build();

        assertThat(queryRequest, is(expectedQueryRequest));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_partitionAndSortKeys() {
        Key compositeKey = Key.builder()
                              .partitionValues(Arrays.asList(
                                  AttributeValue.builder().s("key1").build(),
                                  AttributeValue.builder().s("key2").build()))
                              .sortValues(Arrays.asList(
                                  AttributeValue.builder().s("sort1").build(),
                                  AttributeValue.builder().s("sort2").build()))
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND #AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2"));
        assertThat(queryRequest.indexName(), is("gsi1"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_sortBetween() {
        Key keyFrom = Key.builder()
                         .partitionValues(Arrays.asList(
                             AttributeValue.builder().s("key1").build(),
                             AttributeValue.builder().s("key2").build()))
                         .sortValues(Arrays.asList(
                             AttributeValue.builder().s("sort1").build(),
                             AttributeValue.builder().s("sortA").build()))
                         .build();

        Key keyTo = Key.builder()
                       .partitionValues(Arrays.asList(
                           AttributeValue.builder().s("key1").build(),
                           AttributeValue.builder().s("key2").build()))
                       .sortValues(Arrays.asList(
                           AttributeValue.builder().s("sort1").build(),
                           AttributeValue.builder().s("sortZ").build()))
                       .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional.sortBetween(keyFrom, keyTo))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND #AMZN_MAPPED_gsiSort2 BETWEEN :AMZN_MAPPED_gsiSort2 AND :AMZN_MAPPED_gsiSort22"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_sortBeginsWith() {
        Key key = Key.builder()
                     .partitionValues(Arrays.asList(
                         AttributeValue.builder().s("key1").build(),
                         AttributeValue.builder().s("key2").build()))
                     .sortValues(Arrays.asList(
                         AttributeValue.builder().s("sort1").build(),
                         AttributeValue.builder().s("prefix").build()))
                     .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional.sortBeginsWith(key))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND begins_with(#AMZN_MAPPED_gsiSort2, :AMZN_MAPPED_gsiSort2)"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_exclusiveStartKey() {
        CompositeKeyFakeItem exclusiveStartKey = CompositeKeyFakeItem.builder()
            .id("id1")
            .gsiKey1("gsiKey1")
            .gsiKey2("gsiKey2")
            .gsiSort1("gsiSort1")
            .gsiSort2("gsiSort2")
            .build();

        Set<String> keyFields = new HashSet<>(CompositeKeyFakeItem.SCHEMA.tableMetadata().primaryKeys());
        keyFields.addAll(CompositeKeyFakeItem.SCHEMA.tableMetadata().indexKeys("gsi1"));

        Key compositeKey = Key.builder()
                              .partitionValues(Arrays.asList(
                                  AttributeValue.builder().s("key1").build(),
                                  AttributeValue.builder().s("key2").build()))
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .exclusiveStartKey(CompositeKeyFakeItem.SCHEMA.itemToMap(exclusiveStartKey, keyFields))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s("id1").build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("gsiKey1", AttributeValue.builder().s("gsiKey1").build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("gsiKey2", AttributeValue.builder().s("gsiKey2").build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("gsiSort1", AttributeValue.builder().s("gsiSort1").build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("gsiSort2", AttributeValue.builder().s("gsiSort2").build()));
    }

    @Test
    public void transformResults_gsiCompositeKeys_multipleItems() {
        List<CompositeKeyFakeItem> queryResultItems = generateCompositeKeyFakeItemList();
        List<Map<String, AttributeValue>> queryResultMaps =
            queryResultItems.stream().map(QueryOperationTest::getCompositeKeyAttributeValueMap).collect(toList());

        QueryResponse queryResponse = generateFakeQueryResults(queryResultMaps);

        QueryOperation<CompositeKeyFakeItem> queryOperation = QueryOperation.create(
            QueryEnhancedRequest.builder()
                                .queryConditional(keyEqualTo(Key.builder()
                                                                .partitionValues(Arrays.asList(
                                                                    AttributeValue.builder().s("key1").build(),
                                                                    AttributeValue.builder().s("key2").build()))
                                                                .build()))
                                .build());

        Page<CompositeKeyFakeItem> queryResultPage = queryOperation.transformResponse(queryResponse,
                                                                                  CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryResultPage.items(), is(queryResultItems));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_backwardCompatibility() {
        FakeItemWithIndices fakeItem = createUniqueFakeItemWithIndices();
        QueryOperation<FakeItemWithIndices> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k.partitionValue(fakeItem.getGsiId())))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);

        assertThat(queryRequest.indexName(), is("gsi_1"));
        assertThat(queryRequest.keyConditionExpression(), is("#AMZN_MAPPED_gsi_id = :AMZN_MAPPED_gsi_id"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_consumerBuilder() {
        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k
                                                          .partitionValues(Arrays.asList(
                                                              AttributeValue.builder().s("key1").build(),
                                                              AttributeValue.builder().s("key2").build()))
                                                          .sortValues(Collections.singletonList(
                                                              AttributeValue.builder().s("sort1").build()))))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_gsiCompositeKeys_incompletePartitionKeys() {
        Key incompleteKey = Key.builder()
                               .partitionValues(Collections.singletonList(
                                   AttributeValue.builder().s("key1").build()))
                               .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(incompleteKey))
                                                      .build());

        queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_gsiCompositeKeys_tooManySortKeys() {
        Key invalidKey = Key.builder()
                            .partitionValues(Arrays.asList(
                                AttributeValue.builder().s("key1").build(),
                                AttributeValue.builder().s("key2").build()))
                            .sortValues(Arrays.asList(
                                AttributeValue.builder().s("sort1").build(),
                                AttributeValue.builder().s("sort2").build(),
                                AttributeValue.builder().s("sort3").build()))
                            .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(invalidKey))
                                                      .build());

        queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);
    }

    private static List<CompositeKeyFakeItem> generateCompositeKeyFakeItemList() {
        return IntStream.range(0, 3).mapToObj(i ->
            CompositeKeyFakeItem.builder()
                .id("id" + i)
                .gsiKey1("gsiKey1_" + i)
                .gsiKey2("gsiKey2_" + i)
                .gsiSort1("gsiSort1_" + i)
                .gsiSort2("gsiSort2_" + i)
                .build()
        ).collect(toList());
    }

    private static Map<String, AttributeValue> getCompositeKeyAttributeValueMap(CompositeKeyFakeItem item) {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("id", AttributeValue.builder().s(item.getId()).build());
        map.put("gsiKey1", AttributeValue.builder().s(item.getGsiKey1()).build());
        map.put("gsiKey2", AttributeValue.builder().s(item.getGsiKey2()).build());
        map.put("gsiSort1", AttributeValue.builder().s(item.getGsiSort1()).build());
        map.put("gsiSort2", AttributeValue.builder().s(item.getGsiSort2()).build());
        return map;
    }

    @Test
    public void generateRequest_gsiCompositeKeys_partitionValuesFromStrings() {
        Key compositeKey = Key.builder()
                              .addPartitionValue("gsiKey1")
                              .addPartitionValue("gsiKey2")
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
        assertThat(queryRequest.indexName(), is("gsi1"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_partitionAndSortFromStrings() {
        Key compositeKey = Key.builder()
                              .addPartitionValue("key1")
                              .addPartitionValue("key2")
                              .addSortValue("sort1")
                              .addSortValue("sort2")
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND #AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_partitionValuesFromNumbers() {
        Key compositeKey = Key.builder()
                              .addPartitionValue(123)
                              .addPartitionValue(456)
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiKey1").n(), is("123"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiKey2").n(), is("456"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_sortBetweenFromNumbers() {
        Key keyFrom = Key.builder()
                         .addPartitionValue(123)
                         .addPartitionValue(456)
                         .addSortValue(100)
                         .addSortValue(200)
                         .build();

        Key keyTo = Key.builder()
                       .addPartitionValue(123)
                       .addPartitionValue(456)
                       .addSortValue(100)
                       .addSortValue(300)
                       .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional.sortBetween(keyFrom, keyTo))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND #AMZN_MAPPED_gsiSort2 BETWEEN :AMZN_MAPPED_gsiSort2 AND :AMZN_MAPPED_gsiSort22"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_partitionValuesFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");

        Key compositeKey = Key.builder()
                              .addPartitionValue(bytes1)
                              .addPartitionValue(bytes2)
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiKey1").b(), is(bytes1));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiKey2").b(), is(bytes2));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_sortBeginsWithFromBinary() {
        SdkBytes bytes1 = SdkBytes.fromUtf8String("binary1");
        SdkBytes bytes2 = SdkBytes.fromUtf8String("binary2");
        SdkBytes sortBytes = SdkBytes.fromUtf8String("prefix");

        Key key = Key.builder()
                     .addPartitionValue(bytes1)
                     .addPartitionValue(bytes2)
                     .addSortValue(sortBytes)
                     .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional.sortBeginsWith(key))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND begins_with(#AMZN_MAPPED_gsiSort1, :AMZN_MAPPED_gsiSort1)"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_mixedTypes() {
        Key compositeKey = Key.builder()
                              .addPartitionValue("key1")
                              .addPartitionValue("key2")
                              .addSortValue(123)
                              .addSortValue(456)
                              .build();

        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(compositeKey))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1 AND #AMZN_MAPPED_gsiSort2 = :AMZN_MAPPED_gsiSort2"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiKey1").s(), is("key1"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiKey2").s(), is("key2"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiSort1").n(), is("123"));
        assertThat(queryRequest.expressionAttributeValues().get(":AMZN_MAPPED_gsiSort2").n(), is("456"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_consumerBuilderFromStrings() {
        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k
                                                          .addPartitionValue("key1")
                                                          .addPartitionValue("key2")
                                                          .addSortValue("sort1")))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1"));
    }

    @Test
    public void generateRequest_gsiCompositeKeys_consumerBuilderFromNumbers() {
        QueryOperation<CompositeKeyFakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(keyEqualTo(k -> k
                                                          .addPartitionValue(123)
                                                          .addPartitionValue(456)
                                                          .addSortValue(789)))
                                                      .build());

        QueryRequest queryRequest = queryToTest.generateRequest(CompositeKeyFakeItem.SCHEMA, GSI_COMPOSITE_CONTEXT, null);

        assertThat(queryRequest.keyConditionExpression(),
                   is("#AMZN_MAPPED_gsiKey1 = :AMZN_MAPPED_gsiKey1 AND #AMZN_MAPPED_gsiKey2 = :AMZN_MAPPED_gsiKey2 AND #AMZN_MAPPED_gsiSort1 = :AMZN_MAPPED_gsiSort1"));
    }
}