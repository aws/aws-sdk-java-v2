/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;

@RunWith(MockitoJUnitRunner.class)
public class QueryTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        OperationContext.of(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        OperationContext.of(TABLE_NAME, "gsi_1");

    private final FakeItem keyItem = createUniqueFakeItem();
    private final Query<FakeItem> queryOperation =
        Query.of(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))));

    @Mock
    private DynamoDbClient mockDynamoDbClient;
    @Mock
    private QueryConditional mockQueryConditional;
    @Mock
    private MapperExtension mockMapperExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        QueryRequest queryRequest = QueryRequest.builder().build();
        QueryIterable mockQueryIterable = mock(QueryIterable.class);
        when(mockDynamoDbClient.queryPaginator(any(QueryRequest.class))).thenReturn(mockQueryIterable);

        QueryIterable response = queryOperation.serviceCall(mockDynamoDbClient).apply(queryRequest);

        assertThat(response, is(mockQueryIterable));
        verify(mockDynamoDbClient).queryPaginator(queryRequest);
    }

    @Test
    public void generateRequest_nonDefault_usesQueryConditional() {
        Map<String, AttributeValue> keyItemMap = getAttributeValueMap(keyItem);
        Expression expression = Expression.builder().expression("test-expression").expressionValues(keyItemMap).build();
        when(mockQueryConditional.expression(any(), anyString())).thenReturn(expression);

        Query<FakeItem> query = Query.of(mockQueryConditional);
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
        Query<FakeItemWithIndices> queryToTest =
            Query.of(QueryConditional.equalTo(Key.of(stringValue(fakeItem.getGsiId()))));
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);

        assertThat(queryRequest.indexName(), is("gsi_1"));
    }

    @Test
    public void generateRequest_ascending() {
        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .scanIndexForward(true)
                 .build();
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.scanIndexForward(), is(true));
    }

    @Test
    public void generateRequest_descending() {
        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .scanIndexForward(false)
                 .build();
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.scanIndexForward(), is(false));
    }

    @Test
    public void generateRequest_limit() {
        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .limit(123)
                 .build();
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

        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .filterExpression(filterExpression)
                 .build();
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.filterExpression(), is("test-expression"));
        assertThat(queryRequest.expressionAttributeValues(), hasEntry(":test-key", stringValue("test-value")));
    }

    @Test
    public void generateRequest_filterExpression_withoutValues() {
        Expression filterExpression = Expression.builder().expression("test-expression").build();

        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .filterExpression(filterExpression)
                 .build();
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
        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .filterExpression(filterExpression)
                 .build();
        queryToTest.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_consistentRead() {
        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .consistentRead(true)
                 .build();
        QueryRequest queryRequest = queryToTest.generateRequest(FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.consistentRead(), is(true));
    }

    @Test
    public void generateRequest_hashKeyOnly_withExclusiveStartKey() {
        FakeItem exclusiveStartKey = createUniqueFakeItem();
        Query<FakeItem> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .exclusiveStartKey(FakeItem.getTableSchema().itemToMap(exclusiveStartKey,
                                                                        FakeItem.getTableMetadata().primaryKeys()))
                 .build();

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

        Query<FakeItemWithIndices> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .exclusiveStartKey(FakeItemWithIndices.getTableSchema().itemToMap(exclusiveStartKey, keyFields))
                 .build();

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
        Query<FakeItemWithSort> queryToTest =
            Query.builder()
                 .queryConditional(QueryConditional.equalTo(Key.of(stringValue(keyItem.getId()))))
                 .exclusiveStartKey(
                     FakeItemWithSort.getTableSchema()
                                        .itemToMap(
                                            exclusiveStartKey,
                                            FakeItemWithSort.getTableSchema().tableMetadata().primaryKeys()))
                 .build();

        QueryRequest queryRequest = queryToTest.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
        assertThat(queryRequest.exclusiveStartKey(),
                   hasEntry("sort", AttributeValue.builder().s(exclusiveStartKey.getSort()).build()));
    }

    @Test
    public void transformResults_firstPageMultipleItems_iteratesAndReturnsCorrectItems() {
        List<FakeItem> queryResultItems = generateFakeItemList();
        List<Map<String, AttributeValue>> queryResultMaps =
            queryResultItems.stream().map(QueryTest::getAttributeValueMap).collect(toList());

        QueryIterable queryIterable = generateFakeQueryResults(singletonList(queryResultMaps));

        Iterable<Page<FakeItem>> queryResultPages = queryOperation.transformResponse(queryIterable,
                                                                                     FakeItem.getTableSchema(),
                                                                                     PRIMARY_CONTEXT, null);
        Iterator<Page<FakeItem>> queryResultPageIterator = queryResultPages.iterator();

        assertThat(queryResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page = queryResultPageIterator.next();
        assertThat(queryResultPageIterator.hasNext(), is(false));
        assertThat(page.items(), is(queryResultItems));
    }

    @Test
    public void transformResults_firstPageMultipleItems_setsLastEvaluatedKey() {
        List<FakeItem> queryResultItems = generateFakeItemList();
        FakeItem lastEvaluatedKey = createUniqueFakeItem();
        List<Map<String, AttributeValue>> queryResultMaps =
            queryResultItems.stream().map(QueryTest::getAttributeValueMap).collect(toList());

        QueryIterable queryIterable = generateFakeQueryResults(singletonList(queryResultMaps),
                                                               getAttributeValueMap(lastEvaluatedKey));

        Iterable<Page<FakeItem>> queryResultPages = queryOperation.transformResponse(queryIterable,
                                                                                     FakeItem.getTableSchema(),
                                                                                     PRIMARY_CONTEXT, null);
        Iterator<Page<FakeItem>> queryResultPageIterator = queryResultPages.iterator();

        assertThat(queryResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page = queryResultPageIterator.next();
        assertThat(queryResultPageIterator.hasNext(), is(false));
        assertThat(page.items(), is(queryResultItems));
        assertThat(page.lastEvaluatedKey(), is(getAttributeValueMap(lastEvaluatedKey)));
    }

    @Test
    public void queryItem_twoPagesMultipleItems_iteratesAndReturnsCorrectItems() {
        List<FakeItem> queryResultItems1 = generateFakeItemList();
        List<FakeItem> queryResultItems2 = generateFakeItemList();

        List<Map<String, AttributeValue>> queryResultMaps1 =
            queryResultItems1.stream().map(QueryTest::getAttributeValueMap).collect(toList());
        List<Map<String, AttributeValue>> queryResultMaps2 =
            queryResultItems2.stream().map(QueryTest::getAttributeValueMap).collect(toList());

        QueryIterable queryIterable = generateFakeQueryResults(asList(queryResultMaps1, queryResultMaps2));

        Iterable<Page<FakeItem>> queryResultPages = queryOperation.transformResponse(queryIterable,
                                                                                     FakeItem.getTableSchema(),
                                                                                     PRIMARY_CONTEXT,  null);
        Iterator<Page<FakeItem>> queryResultPageIterator = queryResultPages.iterator();

        assertThat(queryResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page1 = queryResultPageIterator.next();
        assertThat(queryResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page2 = queryResultPageIterator.next();
        assertThat(queryResultPageIterator.hasNext(), is(false));
        assertThat(page1.items(), is(queryResultItems1));
        assertThat(page2.items(), is(queryResultItems2));
    }

    @Test
    public void queryItem_withExtension_correctlyTransformsItems() {
        List<FakeItem> queryResultItems1 = generateFakeItemList();
        List<FakeItem> queryResultItems2 = generateFakeItemList();
        List<FakeItem> modifiedResultItems1 = generateFakeItemList();
        List<FakeItem> modifiedResultItems2 = generateFakeItemList();

        List<Map<String, AttributeValue>> queryResultMaps1 =
            queryResultItems1.stream().map(QueryTest::getAttributeValueMap).collect(toList());
        List<Map<String, AttributeValue>> queryResultMaps2 =
            queryResultItems2.stream().map(QueryTest::getAttributeValueMap).collect(toList());

        ReadModification[] readModifications =
            Stream.concat(modifiedResultItems1.stream(), modifiedResultItems2.stream())
                  .map(QueryTest::getAttributeValueMap)
                  .map(attributeMap -> ReadModification.builder().transformedItem(attributeMap).build())
                  .collect(Collectors.toList())
                  .toArray(new ReadModification[]{});
        when(mockMapperExtension.afterRead(anyMap(), any(), any()))
            .thenReturn(readModifications[0], Arrays.copyOfRange(readModifications, 1, readModifications.length));

        QueryIterable queryIterable = generateFakeQueryResults(asList(queryResultMaps1, queryResultMaps2));

        Iterable<Page<FakeItem>> queryResultPages = queryOperation.transformResponse(queryIterable,
                                                                                     FakeItem.getTableSchema(),
                                                                                     PRIMARY_CONTEXT,
                                                                                     mockMapperExtension);
        Iterator<Page<FakeItem>> queryResultPageIterator = queryResultPages.iterator();

        assertThat(queryResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page1 = queryResultPageIterator.next();
        assertThat(queryResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page2 = queryResultPageIterator.next();
        assertThat(queryResultPageIterator.hasNext(), is(false));
        assertThat(page1.items(), is(modifiedResultItems1));
        assertThat(page2.items(), is(modifiedResultItems2));

        InOrder inOrder = Mockito.inOrder(mockMapperExtension);
        Stream.concat(queryResultMaps1.stream(), queryResultMaps2.stream())
              .forEach(attributeMap ->
                   inOrder.verify(mockMapperExtension).afterRead(attributeMap,
                                                                 PRIMARY_CONTEXT,
                                                                 FakeItem.getTableMetadata()));
    }

    private static QueryIterable generateFakeQueryResults(List<List<Map<String, AttributeValue>>> queryItemMapsPages) {
        List<QueryResponse> queryResponses =
            queryItemMapsPages.stream().map(page -> QueryResponse.builder().items(page).build()).collect(toList());

        QueryIterable mockQueryIterable = mock(QueryIterable.class);
        when(mockQueryIterable.iterator()).thenReturn(queryResponses.iterator());
        return mockQueryIterable;
    }

    private static QueryIterable generateFakeQueryResults(List<List<Map<String, AttributeValue>>> queryItemMapsPages,
                                                   Map<String, AttributeValue> lastEvaluatedKey) {
        List<QueryResponse> queryResponses =
            queryItemMapsPages.stream()
                              .map(page -> QueryResponse.builder()
                                                        .items(page)
                                                        .lastEvaluatedKey(lastEvaluatedKey)
                                                        .build())
                              .collect(toList());

        QueryIterable mockQueryIterable = mock(QueryIterable.class);
        when(mockQueryIterable.iterator()).thenReturn(queryResponses.iterator());
        return mockQueryIterable;
    }

    private static List<FakeItem> generateFakeItemList() {
        return IntStream.range(0, 3).mapToObj(ignored -> FakeItem.createUniqueFakeItem()).collect(toList());
    }

    private static Map<String, AttributeValue> getAttributeValueMap(FakeItem fakeItem) {
        return singletonMap("id", AttributeValue.builder().s(fakeItem.getId()).build());
    }
}