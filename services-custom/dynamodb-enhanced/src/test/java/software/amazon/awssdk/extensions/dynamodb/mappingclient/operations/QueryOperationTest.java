/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices.createUniqueFakeItemWithIndices;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryConditional;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryEnhancedRequest;
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
        OperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        OperationContext.create(TABLE_NAME, "gsi_1");

    private final FakeItem keyItem = createUniqueFakeItem();
    private final QueryOperation<FakeItem> queryOperation =
        QueryOperation.create(QueryEnhancedRequest.builder()
                                                  .queryConditional(QueryConditional.equalTo(Key.create(stringValue(keyItem.getId()))))
                                                  .build());

    @Mock
    private DynamoDbClient mockDynamoDbClient;
    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;
    @Mock
    private QueryConditional mockQueryConditional;
    @Mock
    private MapperExtension mockMapperExtension;

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
                                                      .queryConditional(QueryConditional.equalTo(Key.create(stringValue(fakeItem.getGsiId()))))
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);

        assertThat(queryRequest.indexName(), is("gsi_1"));
    }

    @Test
    public void generateRequest_ascending() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
                                                      .filterExpression(filterExpression)
                                                      .build());
        queryToTest.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_consistentRead() {
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
                                                      .consistentRead(true)
                                                      .build());
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.consistentRead(), is(true));
    }

    @Test
    public void generateRequest_hashKeyOnly_withExclusiveStartKey() {
        FakeItem exclusiveStartKey = createUniqueFakeItem();
        QueryOperation<FakeItem> queryToTest =
            QueryOperation.create(QueryEnhancedRequest.builder()
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional
                                                                            .equalTo(Key.create(stringValue(keyItem.getId()))))
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
                                                      .queryConditional(QueryConditional.equalTo(Key.create(stringValue(keyItem.getId()))))
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
                              .collect(Collectors.toList())
                              .toArray(new ReadModification[]{});

        when(mockMapperExtension.afterRead(anyMap(), any(), any()))
            .thenReturn(readModifications[0], Arrays.copyOfRange(readModifications, 1, readModifications.length));

        QueryResponse queryResponse = generateFakeQueryResults(queryResultMap);

        Page<FakeItem> queryResultPage = queryOperation.transformResponse(queryResponse,
                                                                          FakeItem.getTableSchema(),
                                                                          PRIMARY_CONTEXT,
                                                                          mockMapperExtension);

        assertThat(queryResultPage.items(), is(modifiedResultItems));
        InOrder inOrder = Mockito.inOrder(mockMapperExtension);
        queryResultMap.forEach(
            attributeMap -> inOrder.verify(mockMapperExtension)
                                   .afterRead(attributeMap, PRIMARY_CONTEXT, FakeItem.getTableMetadata()));
    }

    private static QueryResponse generateFakeQueryResults(List<Map<String, AttributeValue>> queryItemMapsPage) {
        return QueryResponse.builder().items(queryItemMapsPage).build();
    }

    private static QueryResponse generateFakeQueryResults(List<Map<String, AttributeValue>> queryItemMapsPage,
                                                          Map<String, AttributeValue> lastEvaluatedKey) {
        return QueryResponse.builder().items(queryItemMapsPage).lastEvaluatedKey(lastEvaluatedKey).build();

    }

    private static List<FakeItem> generateFakeItemList() {
        return IntStream.range(0, 3).mapToObj(ignored -> FakeItem.createUniqueFakeItem()).collect(toList());
    }

    private static Map<String, AttributeValue> getAttributeValueMap(FakeItem fakeItem) {
        return singletonMap("id", AttributeValue.builder().s(fakeItem.getId()).build());
    }
}
