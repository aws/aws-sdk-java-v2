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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
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
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemComposedClass;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@RunWith(MockitoJUnitRunner.class)
public class PutItemOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");
    private static final Expression CONDITION_EXPRESSION;
    private static final Expression CONDITION_EXPRESSION_2;
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

    static {
        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#test_field_3", "test_field_3");
        expressionNames.put("#test_field_4", "test_field_4");
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":test_value_3", numberValue(3));
        expressionValues.put(":test_value_4", numberValue(4));
        CONDITION_EXPRESSION_2 = Expression.builder()
                                         .expression("#test_field_3 = :test_value_3 OR #test_field_4 = :test_value_4")
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
        FakeItem fakeItem = createUniqueFakeItem();
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build());
        PutItemRequest getItemRequest = PutItemRequest.builder().tableName(TABLE_NAME).build();
        PutItemResponse expectedResponse = PutItemResponse.builder().build();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(expectedResponse);

        PutItemResponse response = putItemOperation.serviceCall(mockDynamoDbClient).apply(getItemRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).putItem(getItemRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_withIndex_throwsIllegalArgumentException() {
        FakeItem fakeItem = createUniqueFakeItem();
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build());

        putItemOperation.generateRequest(FakeItem.getTableSchema(), GSI_1_CONTEXT, null);
    }

    @Test
    public void generateRequest_generatesCorrectRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setSubclassAttribute("subclass-value");
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  null);

        Map<String, AttributeValue> expectedItemMap = new HashMap<>();
        expectedItemMap.put("id", AttributeValue.builder().s(fakeItem.getId()).build());
        expectedItemMap.put("subclass_attribute", AttributeValue.builder().s("subclass-value").build());
        PutItemRequest expectedRequest = PutItemRequest.builder()
            .tableName(TABLE_NAME)
            .item(expectedItemMap)
            .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withConditionExpression_generatesCorrectRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        fakeItem.setSubclassAttribute("subclass-value");

        PutItemOperation<FakeItem> putItemOperation =
            PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                          .conditionExpression(CONDITION_EXPRESSION)
                                                          .item(fakeItem)
                                                          .build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  null);

        Map<String, AttributeValue> expectedItemMap = new HashMap<>();
        expectedItemMap.put("id", AttributeValue.builder().s(fakeItem.getId()).build());
        expectedItemMap.put("subclass_attribute", AttributeValue.builder().s("subclass-value").build());
        PutItemRequest expectedRequest =
            PutItemRequest.builder()
                          .tableName(TABLE_NAME)
                          .item(expectedItemMap)
                          .conditionExpression(CONDITION_EXPRESSION.expression())
                          .expressionAttributeNames(CONDITION_EXPRESSION.expressionNames())
                          .expressionAttributeValues(CONDITION_EXPRESSION.expressionValues())
                          .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withMinimalConditionExpression() {
        FakeItem fakeItem = createUniqueFakeItem();
        PutItemOperation<FakeItem> putItemOperation =
            PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                          .item(fakeItem)
                                                          .conditionExpression(MINIMAL_CONDITION_EXPRESSION)
                                                          .build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  null);

        assertThat(request.conditionExpression(), is(MINIMAL_CONDITION_EXPRESSION.expression()));
        assertThat(request.expressionAttributeNames(), is(emptyMap()));
        assertThat(request.expressionAttributeValues(), is(emptyMap()));
    }

    @Test
    public void generateRequest_withConditionExpression_andExtensionWithSingleCondition() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(CONDITION_EXPRESSION_2).build());
        PutItemOperation<FakeItem> putItemOperation =
            PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                          .conditionExpression(CONDITION_EXPRESSION)
                                                          .item(baseFakeItem)
                                                          .build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  mockDynamoDbEnhancedClientExtension);

        Expression expectedCondition = Expression.join(CONDITION_EXPRESSION, CONDITION_EXPRESSION_2, " AND ");
        assertThat(request.conditionExpression(), is(expectedCondition.expression()));
        assertThat(request.expressionAttributeNames(), is(expectedCondition.expressionNames()));
        assertThat(request.expressionAttributeValues(), is(expectedCondition.expressionValues()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_noPartitionKey_throwsIllegalArgumentException() {
        FakeItemComposedClass fakeItem = FakeItemComposedClass.builder().composedAttribute("whatever").build();
        PutItemOperation<FakeItemComposedClass> putItemOperation =
            PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItemComposedClass.class).item(fakeItem).build());

        putItemOperation.generateRequest(FakeItemComposedClass.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void transformResponse_doesNotBlowUp() {
        FakeItem fakeItem = createUniqueFakeItem();
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                    .item(fakeItem)
                                                                                                    .build());
        PutItemResponse response = PutItemResponse.builder().build();

        putItemOperation.transformResponse(response, FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void generateRequest_withExtension_modifiesItemToPut() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, true);
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().transformedItem(fakeMap).build());
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                    .item(baseFakeItem)
                                                                                                    .build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  mockDynamoDbEnhancedClientExtension);

        assertThat(request.item(), is(fakeMap));
        verify(mockDynamoDbEnhancedClientExtension).beforeWrite(
            DefaultDynamoDbExtensionContext.builder()
            .items(baseMap)
            .operationContext(PRIMARY_CONTEXT)
            .tableMetadata(FakeItem.getTableMetadata()).build());
    }

    @Test
    public void generateRequest_withExtension_singleCondition() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        Expression condition = Expression.builder().expression("condition").expressionValues(fakeMap).build();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(condition).build());
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                    .item(baseFakeItem)
                                                                                                    .build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  mockDynamoDbEnhancedClientExtension);

        assertThat(request.conditionExpression(), is("condition"));
        assertThat(request.expressionAttributeValues(), is(fakeMap));
    }

    @Test
    public void generateRequest_withExtension_noModifications() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().build());
        PutItemOperation<FakeItem> putItemOperation = PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                    .item(baseFakeItem)
                                                                                                    .build());

        PutItemRequest request = putItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  mockDynamoDbEnhancedClientExtension);
        assertThat(request.conditionExpression(), is(nullValue()));
        assertThat(request.expressionAttributeValues().size(), is(0));
    }

    @Test
    public void generateTransactWriteItem_basicRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        PutItemOperation<FakeItem> putItemOperation = spy(PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                        .item(fakeItem)
                                                                                                        .build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                                                      .tableName(TABLE_NAME)
                                                      .item(fakeItemMap)
                                                      .build();
        doReturn(putItemRequest).when(putItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = putItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                    context,
                                                                                    mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .put(Put.builder()
                                                                    .item(fakeItemMap)
                                                                    .tableName(TABLE_NAME)
                                                                    .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(putItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void generateTransactWriteItem_conditionalRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        PutItemOperation<FakeItem> putItemOperation = spy(PutItemOperation.create(PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                        .item(fakeItem)
                                                                                                        .build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        String conditionExpression = "condition-expression";
        Map<String, AttributeValue> attributeValues = Collections.singletonMap("key", stringValue("value1"));
        Map<String, String> attributeNames = Collections.singletonMap("key", "value2");

        PutItemRequest putItemRequest = PutItemRequest.builder()
                                                      .tableName(TABLE_NAME)
                                                      .item(fakeItemMap)
                                                      .conditionExpression(conditionExpression)
                                                      .expressionAttributeValues(attributeValues)
                                                      .expressionAttributeNames(attributeNames)
                                                      .build();
        doReturn(putItemRequest).when(putItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = putItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                    context,
                                                                                    mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .put(Put.builder()
                                                                    .item(fakeItemMap)
                                                                    .tableName(TABLE_NAME)
                                                                    .conditionExpression(conditionExpression)
                                                                    .expressionAttributeNames(attributeNames)
                                                                    .expressionAttributeValues(attributeValues)
                                                                    .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(putItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }
}
