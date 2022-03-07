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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@RunWith(MockitoJUnitRunner.class)
public class UpdateItemOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final String OTHER_ATTRIBUTE_1_NAME = "#AMZN_MAPPED_other_attribute_1";
    private static final String OTHER_ATTRIBUTE_2_NAME = "#AMZN_MAPPED_other_attribute_2";
    private static final String SUBCLASS_ATTRIBUTE_NAME = "#AMZN_MAPPED_subclass_attribute";
    private static final String OTHER_ATTRIBUTE_1_VALUE = ":AMZN_MAPPED_other_attribute_1";
    private static final String OTHER_ATTRIBUTE_2_VALUE = ":AMZN_MAPPED_other_attribute_2";
    private static final String SUBCLASS_ATTRIBUTE_VALUE = ":AMZN_MAPPED_subclass_attribute";

    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT = DefaultOperationContext.create(TABLE_NAME, "gsi_1");
    private static final Expression CONDITION_EXPRESSION;
    private static final Expression MINIMAL_CONDITION_EXPRESSION = Expression.builder().expression("foo = bar").build();
    private static final Map<String, AttributeValue> EXPRESSION_VALUES;
    private static final Map<String, String> EXPRESSION_NAMES;

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
        EXPRESSION_VALUES = new HashMap<>();
        EXPRESSION_VALUES.put(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        EXPRESSION_VALUES.put(OTHER_ATTRIBUTE_2_VALUE, AttributeValue.builder().s("value-2").build());
        EXPRESSION_NAMES = new HashMap<>();
        EXPRESSION_NAMES.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        EXPRESSION_NAMES.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
    }

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        FakeItem item = createUniqueFakeItem();
        UpdateItemOperation<FakeItem> updateItemOperation = UpdateItemOperation.create(
            UpdateItemEnhancedRequest.builder(FakeItem.class).item(item).build());
        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder().tableName(TABLE_NAME).build();
        UpdateItemResponse expectedResponse = UpdateItemResponse.builder().build();
        when(mockDynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(expectedResponse);

        UpdateItemResponse response = updateItemOperation.serviceCall(mockDynamoDbClient).apply(updateItemRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).updateItem(updateItemRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_withIndex_throwsIllegalArgumentException() {
        FakeItem item = createUniqueFakeItem();
        UpdateItemOperation<FakeItem> updateItemOperation = UpdateItemOperation.create(
            UpdateItemEnhancedRequest.builder(FakeItem.class).item(item).build());

        updateItemOperation.generateRequest(FakeItem.getTableSchema(), GSI_1_CONTEXT, null);
    }

    @Test
    public void generateRequest_nullValuesNotIgnoredByDefault() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b -> { }));
        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        String expectedUpdateExpression = "SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                         " REMOVE " + OTHER_ATTRIBUTE_2_NAME;
        UpdateItemRequest.Builder expectedRequestBuilder = ddbRequestBuilder(ddbKey(item.getId(), item.getSort()));
        expectedRequestBuilder.expressionAttributeValues(expressionValuesFor(OTHER_ATTRIBUTE_1_VALUE));
        expectedRequestBuilder.expressionAttributeNames(expressionNamesFor(OTHER_ATTRIBUTE_1_NAME, OTHER_ATTRIBUTE_2_NAME));
        expectedRequestBuilder.updateExpression(expectedUpdateExpression);

        assertThat(request, is(expectedRequestBuilder.build()));
    }

    @Test
    public void generateRequest_withConditionExpression() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b -> b.conditionExpression(CONDITION_EXPRESSION)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        String expectedUpdateExpression = "SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                         " REMOVE " + OTHER_ATTRIBUTE_2_NAME;
        UpdateItemRequest.Builder expectedRequestBuilder = ddbRequestBuilder(ddbKey(item.getId(), item.getSort()));
        expectedRequestBuilder.expressionAttributeValues(expressionValuesFor(CONDITION_EXPRESSION.expressionValues(), OTHER_ATTRIBUTE_1_VALUE));
        expectedRequestBuilder.expressionAttributeNames(expressionNamesFor(CONDITION_EXPRESSION.expressionNames(),
                                                                           OTHER_ATTRIBUTE_1_NAME, OTHER_ATTRIBUTE_2_NAME));
        expectedRequestBuilder.updateExpression(expectedUpdateExpression);
        expectedRequestBuilder.conditionExpression(CONDITION_EXPRESSION.expression());

        assertThat(request, is(expectedRequestBuilder.build()));
    }

    @Test
    public void generateRequest_withMinimalConditionExpression() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b -> b.conditionExpression(MINIMAL_CONDITION_EXPRESSION)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        assertThat(request.conditionExpression(), is(MINIMAL_CONDITION_EXPRESSION.expression()));
        assertThat(request.expressionAttributeNames(), is(expressionNamesFor(OTHER_ATTRIBUTE_1_NAME, OTHER_ATTRIBUTE_2_NAME)));
        assertThat(request.expressionAttributeValues(), is(expressionValuesFor(OTHER_ATTRIBUTE_1_VALUE)));
    }

    @Test
    public void generateRequest_explicitlyUnsetIgnoreNulls() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b-> b.ignoreNulls(false)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        String expectedUpdateExpression = "SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                          " REMOVE " + OTHER_ATTRIBUTE_2_NAME;
        UpdateItemRequest.Builder expectedRequestBuilder = ddbRequestBuilder(ddbKey(item.getId(), item.getSort()));
        expectedRequestBuilder.expressionAttributeValues(expressionValuesFor(OTHER_ATTRIBUTE_1_VALUE));
        expectedRequestBuilder.expressionAttributeNames(expressionNamesFor(OTHER_ATTRIBUTE_1_NAME, OTHER_ATTRIBUTE_2_NAME));
        expectedRequestBuilder.updateExpression(expectedUpdateExpression);

        assertThat(request, is(expectedRequestBuilder.build()));
    }

    @Test
    public void generateRequest_multipleSetters() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b-> b.ignoreNulls(false)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        UpdateItemRequest.Builder expectedRequestBuilder = ddbRequestBuilder(ddbKey(item.getId(), item.getSort()));
        expectedRequestBuilder.expressionAttributeValues(expressionValuesFor(OTHER_ATTRIBUTE_1_VALUE, OTHER_ATTRIBUTE_2_VALUE));
        expectedRequestBuilder.expressionAttributeNames(expressionNamesFor(OTHER_ATTRIBUTE_1_NAME, OTHER_ATTRIBUTE_2_NAME));

        String updateExpression1 = "SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                  ", " + OTHER_ATTRIBUTE_2_NAME + " = " + OTHER_ATTRIBUTE_2_VALUE;
        UpdateItemRequest expectedRequest1 = expectedRequestBuilder.updateExpression(updateExpression1).build();
        String updateExpression2 = "SET " + OTHER_ATTRIBUTE_2_NAME + " = " + OTHER_ATTRIBUTE_2_VALUE +
                                  ", " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE;
        UpdateItemRequest expectedRequest2 = expectedRequestBuilder.updateExpression(updateExpression2).build();

        assertThat(request, either(is(expectedRequest1)).or(is(expectedRequest2)));
    }

    @Test
    public void generateRequest_multipleDeletes() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b-> b.ignoreNulls(false)));
        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        UpdateItemRequest.Builder expectedRequestBuilder = ddbRequestBuilder(ddbKey(item.getId(), item.getSort()));
        expectedRequestBuilder.expressionAttributeNames(expressionNamesFor(OTHER_ATTRIBUTE_1_NAME, OTHER_ATTRIBUTE_2_NAME));
        String updateExpression1 = "REMOVE " + OTHER_ATTRIBUTE_1_NAME + ", " + OTHER_ATTRIBUTE_2_NAME;
        UpdateItemRequest expectedRequest1 = expectedRequestBuilder.updateExpression(updateExpression1).build();
        String updateExpression2 = "REMOVE " + OTHER_ATTRIBUTE_2_NAME + ", " + OTHER_ATTRIBUTE_1_NAME;
        UpdateItemRequest expectedRequest2 = expectedRequestBuilder.updateExpression(updateExpression2).build();

        assertThat(request,either(is(expectedRequest1)).or(is(expectedRequest2)));
    }

    @Test
    public void generateRequest_canIgnoreNullValues() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b-> b.ignoreNulls(true)));
        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);

        String expectedUpdateExpression = "SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE;

        UpdateItemRequest.Builder expectedRequestBuilder = ddbRequestBuilder(ddbKey(item.getId(), item.getSort()));
        expectedRequestBuilder.expressionAttributeValues(expressionValuesFor(OTHER_ATTRIBUTE_1_VALUE));
        expectedRequestBuilder.expressionAttributeNames(expressionNamesFor(OTHER_ATTRIBUTE_1_NAME));
        expectedRequestBuilder.updateExpression(expectedUpdateExpression);
        UpdateItemRequest expectedRequest = expectedRequestBuilder.build();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_keyOnlyItem() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(requestFakeItemWithSort(item, b-> b.ignoreNulls(true)));
        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(), PRIMARY_CONTEXT, null);
        UpdateItemRequest expectedRequest = ddbRequestBuilder(ddbKey(item.getId(), item.getSort())).build();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withExtension_modifiesKeyPortionOfItem() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();

        Map<String, AttributeValue> baseMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, false);
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, false);
        Map<String, AttributeValue> keyMap = FakeItem.getTableSchema().itemToMap(fakeItem, singletonList("id"));

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().transformedItem(fakeMap).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class).item(baseFakeItem).build());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.key(), is(keyMap));
        verify(mockDynamoDbEnhancedClientExtension).beforeWrite(extensionContext(baseMap, b -> b.operationName(OperationName.UPDATE_ITEM)));
    }

    @Test
    public void generateRequest_withExtension_transformedItemModifiesUpdateExpression() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseMap = new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));

        Map<String, AttributeValue> fakeMap = new HashMap<>(baseMap);
        fakeMap.put("subclass_attribute", AttributeValue.builder().s("1").build());

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().transformedItem(fakeMap).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(fakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.updateExpression(), is("SET " + SUBCLASS_ATTRIBUTE_NAME + " = " + SUBCLASS_ATTRIBUTE_VALUE));
        assertThat(request.expressionAttributeValues(), hasEntry(SUBCLASS_ATTRIBUTE_VALUE,
            AttributeValue.builder().s("1").build()));
        assertThat(request.expressionAttributeNames(), hasEntry(SUBCLASS_ATTRIBUTE_NAME, "subclass_attribute"));
    }

    @Test
    public void generateRequest_withExtensions_singleCondition() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        Expression condition = Expression.builder().expression("condition").expressionValues(fakeMap).build();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(condition).build());
        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.conditionExpression(), is("condition"));
        assertThat(request.expressionAttributeValues(), is(fakeMap));
    }

    @Test
    public void generateRequest_withExtensions_singleUpdateExpression() {
        Map<String, AttributeValue> deleteActionMap = singletonMap(":val", AttributeValue.builder().s("s").build());
        DeleteAction deleteAction = DeleteAction.builder().path("attr1")
                                                .value(":val")
                                                .expressionValues(deleteActionMap)
                                                .build();
        UpdateExpression updateExpression = UpdateExpression.builder().addAction(deleteAction).build();

        FakeItem item = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(updateExpression).build());
        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(item, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.updateExpression(), is("DELETE attr1 :val"));
        assertThat(request.expressionAttributeValues(), is(deleteActionMap));
    }

    @Test
    public void generateRequest_withExtensions_conditionAndUpdateExpression() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        Expression condition = Expression.builder().expression("condition").expressionValues(fakeMap).build();

        Map<String, AttributeValue> deleteActionMap = singletonMap(":val", AttributeValue.builder().s("s").build());
        DeleteAction deleteAction = DeleteAction.builder().path("attr1")
                                                .value(":val")
                                                .expressionValues(deleteActionMap)
                                                .build();
        UpdateExpression updateExpression = UpdateExpression.builder().addAction(deleteAction).build();

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder()
                                         .additionalConditionalExpression(condition)
                                         .updateExpression(updateExpression)
                                         .build());
        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.conditionExpression(), is("condition"));
        assertThat(request.updateExpression(), is("DELETE attr1 :val"));
        assertThat(request.expressionAttributeValues(), is(Expression.joinValues(fakeMap, deleteActionMap)));
    }

    @Test
    public void generateRequest_withExtensions_conflictingExpressionValue_throwsRuntimeException() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        baseFakeItem.setSubclassAttribute("something");
        Map<String, AttributeValue> values = singletonMap(SUBCLASS_ATTRIBUTE_VALUE,
                                                          AttributeValue.builder().s("1").build());
        Expression condition1 = Expression.builder().expression("condition1").expressionValues(values).build();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(condition1).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        try {
            updateItemOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, mockDynamoDbEnhancedClientExtension);

            fail("Exception should be thrown");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("subclass_attribute"));
        }
    }

    @Test
    public void generateRequest_withExtensions_conflictingExpressionName_throwsRuntimeException() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        baseFakeItem.setSubclassAttribute("something");
        Map<String, String> names = singletonMap(SUBCLASS_ATTRIBUTE_NAME, "conflict");
        Expression condition1 = Expression.builder().expression("condition1").expressionNames(names).build();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(condition1).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        try {
            updateItemOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, mockDynamoDbEnhancedClientExtension);

            fail("Exception should be thrown");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("subclass_attribute"));
        }
    }

    @Test
    public void generateRequest_withExtension_correctlyCoalescesIdenticalExpressionValues() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        baseFakeItem.setSubclassAttribute("something");
        Map<String, AttributeValue> values = singletonMap(SUBCLASS_ATTRIBUTE_VALUE,
                                                           AttributeValue.builder().s("something").build());
        Expression condition = Expression.builder().expression("condition").expressionValues(values).build();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(condition).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.expressionAttributeValues(), is(values));
    }

    @Test
    public void generateRequest_withExtension_correctlyCoalescesIdenticalExpressionNames() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        baseFakeItem.setSubclassAttribute("something");
        Map<String, String> names = singletonMap(SUBCLASS_ATTRIBUTE_NAME, "subclass_attribute");
        Expression condition = Expression.builder().expression("condition").expressionNames(names).build();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().additionalConditionalExpression(condition).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.expressionAttributeNames(), is(names));
    }

    @Test
    public void generateRequest_withExtension_noModifications() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);
        assertThat(request.conditionExpression(), is(nullValue()));
        assertThat(request.expressionAttributeValues().size(), is(0));
    }

    @Test
    public void generateRequest_withExtension_conditionAndModification() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseMap = new HashMap<>(FakeItem.getTableSchema().itemToMap(baseFakeItem, true));

        Map<String, AttributeValue> fakeMap = new HashMap<>(baseMap);
        fakeMap.put("subclass_attribute", AttributeValue.builder().s("1").build());

        Map<String, AttributeValue> conditionValues = new HashMap<>();
        conditionValues.put(":condition_value", AttributeValue.builder().s("2").build());

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder()
                                         .transformedItem(fakeMap)
                                         .additionalConditionalExpression(Expression.builder()
                                                                                    .expression("condition")
                                                                                    .expressionValues(conditionValues)
                                                                                    .build())
                                         .build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(baseFakeItem, b -> b.ignoreNulls(true)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.updateExpression(), is("SET " + SUBCLASS_ATTRIBUTE_NAME + " = " + SUBCLASS_ATTRIBUTE_VALUE));
        assertThat(request.expressionAttributeValues(), hasEntry(SUBCLASS_ATTRIBUTE_VALUE,
                                                                 AttributeValue.builder().s("1").build()));
        assertThat(request.expressionAttributeValues(), hasEntry(":condition_value",
                                                                 AttributeValue.builder().s("2").build()));
        assertThat(request.expressionAttributeNames(), hasEntry(SUBCLASS_ATTRIBUTE_NAME, "subclass_attribute"));
    }

    @Test
    public void generateRequest_withReturnConsumedCapacity_unknownValue_generatesCorrectRequest() {
        FakeItem item = createUniqueFakeItem();
        String returnConsumedCapacity = UUID.randomUUID().toString();

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(item, b -> b.ignoreNulls(true)
                                                                   .returnConsumedCapacity(returnConsumedCapacity)));
        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
        UpdateItemRequest expectedRequest = ddbRequest(ddbKey(item.getId()), b -> b.returnConsumedCapacity(returnConsumedCapacity));
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withReturnConsumedCapacity_knownValue_generatesCorrectRequest() {
        FakeItem item = createUniqueFakeItem();
        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(item, b -> b.ignoreNulls(true)
                                                                   .returnConsumedCapacity(returnConsumedCapacity)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
        UpdateItemRequest expectedRequest = ddbRequest(ddbKey(item.getId()), b -> b.returnConsumedCapacity(returnConsumedCapacity));

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withReturnItemCollectionMetrics_unknownValue_generatesCorrectRequest() {
        FakeItem item = createUniqueFakeItem();
        String returnItemCollectionMetrics = UUID.randomUUID().toString();

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(item, b -> b.ignoreNulls(true)
                                                                   .returnItemCollectionMetrics(returnItemCollectionMetrics)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
        UpdateItemRequest expectedRequest = ddbRequest(ddbKey(item.getId()), b -> b.returnItemCollectionMetrics(returnItemCollectionMetrics));

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withReturnItemCollectionMetrics_knownValue_generatesCorrectRequest() {
        FakeItem item = createUniqueFakeItem();
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(item, b -> b.ignoreNulls(true)
                                                                   .returnItemCollectionMetrics(returnItemCollectionMetrics)));

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(), PRIMARY_CONTEXT, null);
        UpdateItemRequest expectedRequest = ddbRequest(ddbKey(item.getId()), b -> b.returnItemCollectionMetrics(returnItemCollectionMetrics));

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void transformResponse_withExtension_returnsCorrectTransformedItem() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, true);
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);

        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(
            ReadModification.builder().transformedItem(fakeMap).build());

        FakeItem resultItem = transformResponse(baseFakeItem);

        assertThat(resultItem, is(fakeItem));
        verify(mockDynamoDbEnhancedClientExtension).afterRead(extensionContext(baseFakeMap));
    }

    @Test
    public void transformResponse_withNoOpExtension_returnsCorrectItem() {
        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(ReadModification.builder().build());

        FakeItem baseFakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, true);

        FakeItem resultItem = transformResponse(baseFakeItem);

        assertThat(resultItem, is(baseFakeItem));
        verify(mockDynamoDbEnhancedClientExtension).afterRead(extensionContext(baseFakeMap));
    }

    @Test(expected = IllegalStateException.class)
    public void transformResponse_afterReadThrowsException_throwsIllegalStateException() {
        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenThrow(RuntimeException.class);

        transformResponse(createUniqueFakeItem());
    }

    private Map<String, AttributeValue> ddbKey(String partitionKey) {
        return singletonMap("id", AttributeValue.builder().s(partitionKey).build());
    }

    private Map<String, AttributeValue> ddbKey(String partitionKey, String sortKey) {
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(partitionKey).build());
        expectedKey.put("sort", AttributeValue.builder().s(sortKey).build());
        return expectedKey;
    }

    private UpdateItemRequest ddbRequest(Map<String, AttributeValue> keys, Consumer<UpdateItemRequest.Builder> modify) {
        UpdateItemRequest.Builder builder = ddbBaseRequestBuilder(keys);
        modify.accept(builder);
        return builder.build();
    }

    private UpdateItemRequest.Builder ddbRequestBuilder(Map<String, AttributeValue> keys) {
        return ddbBaseRequestBuilder(keys);
    }

    private UpdateItemRequest.Builder ddbBaseRequestBuilder(Map<String, AttributeValue> keys) {
        return UpdateItemRequest.builder().tableName(TABLE_NAME)
                                .key(keys)
                                .returnValues(ReturnValue.ALL_NEW);
    }

    private static Map<String, AttributeValue> expressionValuesFor(Map<String, AttributeValue> initValues, String... tokens) {
        Map<String, AttributeValue> expressionValues = new HashMap<>(initValues);
        expressionValues.putAll(expressionValuesFor(tokens));
        return expressionValues;
    }

    private static Map<String, AttributeValue> expressionValuesFor(String... tokens) {
        return Stream.of(tokens).collect(Collectors.toMap(String::toString, EXPRESSION_VALUES::get));
    }

    private static Map<String, String> expressionNamesFor(Map<String, String> initValues, String... tokens) {
        Map<String, String> expressionNames = new HashMap<>(initValues);
        expressionNames.putAll(expressionNamesFor(tokens));
        return expressionNames;
    }

    private static Map<String, String> expressionNamesFor(String... tokens) {
        return Stream.of(tokens).collect(Collectors.toMap(String::toString, EXPRESSION_NAMES::get));
    }

    private UpdateItemEnhancedRequest<FakeItem> requestFakeItem(
            FakeItem item, Consumer<UpdateItemEnhancedRequest.Builder<FakeItem>> modify) {
        UpdateItemEnhancedRequest.Builder<FakeItem> builder = UpdateItemEnhancedRequest.builder(FakeItem.class).item(item);
        modify.accept(builder);
        return builder.build();
    }

    private UpdateItemEnhancedRequest<FakeItemWithSort> requestFakeItemWithSort(
            FakeItemWithSort item, Consumer<UpdateItemEnhancedRequest.Builder<FakeItemWithSort>> modify) {
        UpdateItemEnhancedRequest.Builder<FakeItemWithSort> builder = UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                                               .item(item);
        modify.accept(builder);
        return builder.build();
    }

    private DefaultDynamoDbExtensionContext extensionContext(Map<String, AttributeValue> fakeMap) {
        return extensionContext(fakeMap, b -> {});
    }

    private DefaultDynamoDbExtensionContext extensionContext(Map<String, AttributeValue> fakeMap,
                                                             Consumer<DefaultDynamoDbExtensionContext.Builder> modify) {
        DefaultDynamoDbExtensionContext.Builder builder = DefaultDynamoDbExtensionContext.builder()
                                                                                         .tableMetadata(FakeItem.getTableMetadata())
                                                                                         .tableSchema(FakeItem.getTableSchema())
                                                                                         .operationContext(PRIMARY_CONTEXT)
                                                                                         .items(fakeMap);
        modify.accept(builder);
        return builder.build();
    }

    private FakeItem transformResponse(FakeItem item) {
        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(requestFakeItem(item, b -> b.ignoreNulls(true)));

        Map<String, AttributeValue> itemMap = FakeItem.getTableSchema().itemToMap(item, true);
        return updateItemOperation.transformResponse(UpdateItemResponse.builder().attributes(itemMap).build(),
                                                     FakeItem.getTableSchema(),
                                                     PRIMARY_CONTEXT,
                                                     mockDynamoDbEnhancedClientExtension).attributes();
    }
}
