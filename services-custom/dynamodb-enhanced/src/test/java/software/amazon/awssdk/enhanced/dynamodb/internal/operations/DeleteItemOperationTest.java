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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemComposedClass;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@RunWith(MockitoJUnitRunner.class)
public class DeleteItemOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");
    private static final Expression CONDITION_EXPRESSION;
    private static final Expression MINIMAL_CONDITION_EXPRESSION = Expression.builder().expression("foo = bar").build();

    static {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#test_field_1", "test_field_1");
        expressionNames.put("#test_field_2", "test_field_2");
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":test_value_1", numberValue(1));
        expressionValues.put(":test_value_2", numberValue(2));
        CONDITION_EXPRESSION = Expression.builder()
                                         .expression("#test_field_1 = :test_value_1 OR #test_field_2 = :test_value_2")
                                         .expressionNames(Collections.unmodifiableMap(expressionNames))
                                         .expressionValues(Collections.unmodifiableMap(expressionValues))
                                         .build();
    }

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        FakeItem keyItem = createUniqueFakeItem();
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());
        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder().tableName(TABLE_NAME).build();
        DeleteItemResponse expectedResponse = DeleteItemResponse.builder().build();
        when(mockDynamoDbClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(expectedResponse);

        DeleteItemResponse response = deleteItemOperation.serviceCall(mockDynamoDbClient).apply(deleteItemRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).deleteItem(deleteItemRequest);
    }

    @Test
    public void generateRequest_partitionKeyOnly() {
        FakeItem keyItem = createUniqueFakeItem();
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());

        DeleteItemRequest request = deleteItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        Map<String, AttributeValue> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        DeleteItemRequest expectedRequest = DeleteItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(expectedKeyMap)
            .returnValues(ReturnValue.ALL_OLD)
            .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_partitionAndSortKey() {
        FakeItemWithSort keyItem = createUniqueFakeItemWithSort();
        DeleteItemOperation<FakeItemWithSort> deleteItemOperation = DeleteItemOperation.create(
            DeleteItemEnhancedRequest.builder()
                                     .key(k -> k.partitionValue(keyItem.getId()).sortValue(keyItem.getSort()))
                                     .build());

        DeleteItemRequest request = deleteItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                       PRIMARY_CONTEXT,
                                                                        null);

        Map<String, AttributeValue> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        expectedKeyMap.put("sort", AttributeValue.builder().s(keyItem.getSort()).build());
        DeleteItemRequest expectedRequest = DeleteItemRequest.builder()
                                                       .tableName(TABLE_NAME)
                                                       .key(expectedKeyMap)
                                                       .returnValues(ReturnValue.ALL_OLD)
                                                       .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withConditionExpression() {
        FakeItem keyItem = createUniqueFakeItem();
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder()
                                                                .key(k -> k.partitionValue(keyItem.getId()))
                                                                .conditionExpression(CONDITION_EXPRESSION)
                                                                .build());

        DeleteItemRequest request = deleteItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request.conditionExpression(), is(CONDITION_EXPRESSION.expression()));
        assertThat(request.expressionAttributeNames(), is(CONDITION_EXPRESSION.expressionNames()));
        assertThat(request.expressionAttributeValues(), is(CONDITION_EXPRESSION.expressionValues()));
    }

    @Test
    public void generateRequest_withMinimalConditionExpression() {
        FakeItem keyItem = createUniqueFakeItem();
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder()
                                                                .key(k -> k.partitionValue(keyItem.getId()))
                                                                .conditionExpression(MINIMAL_CONDITION_EXPRESSION)
                                                                .build());

        DeleteItemRequest request = deleteItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request.conditionExpression(), is(MINIMAL_CONDITION_EXPRESSION.expression()));
        assertThat(request.expressionAttributeNames(), is(emptyMap()));
        assertThat(request.expressionAttributeValues(), is(emptyMap()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_noPartitionKey_throwsIllegalArgumentException() {
        DeleteItemOperation<FakeItemComposedClass> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder().key(k -> k.partitionValue("whatever")).build());

        deleteItemOperation.generateRequest(FakeItemComposedClass.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_withIndex_throwsIllegalArgumentException() {
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder().key(k -> k.partitionValue("whatever")).build());

        deleteItemOperation.generateRequest(FakeItem.getTableSchema(), GSI_1_CONTEXT, null);
    }

    @Test
    public void transformResponse_correctlyTransformsIntoAnItem() {
        FakeItem keyItem = createUniqueFakeItem();
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());
        Map<String, AttributeValue> responseMap = new HashMap<>();
        responseMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        responseMap.put("subclass_attribute", AttributeValue.builder().s("test-value").build());
        DeleteItemResponse response = DeleteItemResponse.builder()
                                                        .attributes(responseMap)
                                                        .build();

        FakeItem result = deleteItemOperation.transformResponse(response,
                                                                FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(result.getId(), is(keyItem.getId()));
        assertThat(result.getSubclassAttribute(), is("test-value"));
    }

    @Test
    public void transformResponse_noResults_returnsNull() {
        FakeItem keyItem = createUniqueFakeItem();
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());
        DeleteItemResponse response = DeleteItemResponse.builder()
                                                        .build();

        FakeItem result = deleteItemOperation.transformResponse(response,
                                                                FakeItem.getTableSchema(),
                                                                PRIMARY_CONTEXT,
                                                                null);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void generateRequest_withExtension_doesNotModifyKey() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> keyMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, singletonList("id"));
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder()
                                                                .key(k -> k.partitionValue(baseFakeItem.getId()))
                                                                .build());


        DeleteItemRequest request = deleteItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.key(), is(keyMap));
        verify(mockDynamoDbEnhancedClientExtension, never()).beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class));
    }

    @Test
    public void transformResponse_withExtension_appliesItemModification() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeItemMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, false);
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, false);
        DeleteItemOperation<FakeItem> deleteItemOperation =
            DeleteItemOperation.create(DeleteItemEnhancedRequest.builder()
                                                                .key(k -> k.partitionValue(baseFakeItem.getId()))
                                                                .build());

        DeleteItemResponse response = DeleteItemResponse.builder()
                                                        .attributes(baseFakeItemMap)
                                                        .build();
        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(ReadModification.builder().transformedItem(fakeItemMap).build());

        FakeItem resultItem = deleteItemOperation.transformResponse(response,
                                                                    FakeItem.getTableSchema(),
                                                                    PRIMARY_CONTEXT,
                                                                    mockDynamoDbEnhancedClientExtension);

        assertThat(resultItem, is(fakeItem));
        verify(mockDynamoDbEnhancedClientExtension).afterRead(DefaultDynamoDbExtensionContext.builder()
                                                                                             .tableMetadata(FakeItem.getTableMetadata())
                                                                                             .operationContext(PRIMARY_CONTEXT)
                                                                                             .items(baseFakeItemMap).build());
    }

    @Test
    public void generateTransactWriteItem_basicRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        DeleteItemOperation<FakeItem> deleteItemOperation =
            spy(DeleteItemOperation.create(DeleteItemEnhancedRequest.builder()
                                                                    .key(k -> k.partitionValue(fakeItem.getId()))
                                                                    .build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                                                               .tableName(TABLE_NAME)
                                                               .key(fakeItemMap)
                                                               .build();
        doReturn(deleteItemRequest).when(deleteItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = deleteItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                       context,
                                                                                       mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .delete(Delete.builder()
                                                                          .key(fakeItemMap)
                                                                          .tableName(TABLE_NAME)
                                                                          .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(deleteItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void generateTransactWriteItem_conditionalRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        DeleteItemOperation<FakeItem> deleteItemOperation =
            spy(DeleteItemOperation.create(DeleteItemEnhancedRequest.builder()
                                                                    .key(k -> k.partitionValue(fakeItem.getId()))
                                                                    .build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        String conditionExpression = "condition-expression";
        Map<String, AttributeValue> attributeValues = Collections.singletonMap("key", stringValue("value1"));
        Map<String, String> attributeNames = Collections.singletonMap("key", "value2");

        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                                                               .tableName(TABLE_NAME)
                                                               .key(fakeItemMap)
                                                               .conditionExpression(conditionExpression)
                                                               .expressionAttributeValues(attributeValues)
                                                               .expressionAttributeNames(attributeNames)
                                                               .build();
        doReturn(deleteItemRequest).when(deleteItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = deleteItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                       context,
                                                                                       mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .delete(Delete.builder()
                                                                          .key(fakeItemMap)
                                                                          .tableName(TABLE_NAME)
                                                                          .conditionExpression(conditionExpression)
                                                                          .expressionAttributeNames(attributeNames)
                                                                          .expressionAttributeValues(attributeValues)
                                                                          .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(deleteItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }
}
