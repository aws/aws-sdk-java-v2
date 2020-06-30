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
import static org.mockito.Mockito.doReturn;
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
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
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
        UpdateItemOperation<FakeItemWithSort> updateItemOperation = UpdateItemOperation.create(
            UpdateItemEnhancedRequest.builder(FakeItemWithSort.class).item(item).build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        Map<String, AttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        expectedNames.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
        UpdateItemRequest.Builder baseExpectedRequest = UpdateItemRequest.builder()
                                                                         .tableName(TABLE_NAME)
                                                                         .expressionAttributeValues(expectedValues)
                                                                         .expressionAttributeNames(expectedNames)
                                                                         .key(expectedKey)
                                                                         .returnValues(ReturnValue.ALL_NEW);
        UpdateItemRequest expectedRequest =
            baseExpectedRequest.updateExpression("SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                                 " REMOVE " + OTHER_ATTRIBUTE_2_NAME)
                               .build();

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withConditionExpression() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .conditionExpression(CONDITION_EXPRESSION)
                                                                .build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        Map<String, AttributeValue> expectedValues = new HashMap<>(CONDITION_EXPRESSION.expressionValues());
        expectedValues.put(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        Map<String, String> expectedNames = new HashMap<>(CONDITION_EXPRESSION.expressionNames());
        expectedNames.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        expectedNames.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
        UpdateItemRequest.Builder baseExpectedRequest =
            UpdateItemRequest.builder()
                             .tableName(TABLE_NAME)
                             .expressionAttributeValues(expectedValues)
                             .expressionAttributeNames(expectedNames)
                             .conditionExpression(CONDITION_EXPRESSION.expression())
                             .key(expectedKey)
                             .returnValues(ReturnValue.ALL_NEW);
        UpdateItemRequest expectedRequest =
            baseExpectedRequest.updateExpression("SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                                 " REMOVE " + OTHER_ATTRIBUTE_2_NAME)
                               .build();

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_withMinimalConditionExpression() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .conditionExpression(MINIMAL_CONDITION_EXPRESSION)
                                                                .build());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        Map<String, AttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        expectedNames.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
        assertThat(request.conditionExpression(), is(MINIMAL_CONDITION_EXPRESSION.expression()));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
    }

    @Test
    public void generateRequest_explicitlyUnsetIgnoreNulls() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .ignoreNulls(false)
                                                                .build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        Map<String, AttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        expectedNames.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
        UpdateItemRequest.Builder baseExpectedRequest = UpdateItemRequest.builder()
                                                                         .tableName(TABLE_NAME)
                                                                         .expressionAttributeValues(expectedValues)
                                                                         .expressionAttributeNames(expectedNames)
                                                                         .key(expectedKey)
                                                                         .returnValues(ReturnValue.ALL_NEW);
        UpdateItemRequest expectedRequest =
            baseExpectedRequest.updateExpression("SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                                 " REMOVE " + OTHER_ATTRIBUTE_2_NAME)
                               .build();

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_multipleSetters() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .ignoreNulls(false)
                                                                .build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        Map<String, AttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        expectedValues.put(OTHER_ATTRIBUTE_2_VALUE, AttributeValue.builder().s("value-2").build());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        expectedNames.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
        UpdateItemRequest.Builder baseExpectedRequest = UpdateItemRequest.builder()
                                                                         .tableName(TABLE_NAME)
                                                                         .expressionAttributeValues(expectedValues)
                                                                         .expressionAttributeNames(expectedNames)
                                                                         .key(expectedKey)
                                                                         .returnValues(ReturnValue.ALL_NEW);
        UpdateItemRequest expectedRequest1 =
            baseExpectedRequest.updateExpression("SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE +
                                                 ", " + OTHER_ATTRIBUTE_2_NAME + " = " + OTHER_ATTRIBUTE_2_VALUE)
                               .build();
        UpdateItemRequest expectedRequest2 =
            baseExpectedRequest.updateExpression("SET " + OTHER_ATTRIBUTE_2_NAME + " = " + OTHER_ATTRIBUTE_2_VALUE +
                                                 ", " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE)
                               .build();

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request, either(is(expectedRequest1)).or(is(expectedRequest2)));
    }

    @Test
    public void generateRequest_multipleDeletes() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .ignoreNulls(false)
                                                                .build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        expectedNames.put(OTHER_ATTRIBUTE_2_NAME, "other_attribute_2");
        UpdateItemRequest.Builder baseExpectedRequest = UpdateItemRequest.builder()
                                                                         .tableName(TABLE_NAME)
                                                                         .expressionAttributeNames(expectedNames)
                                                                         .key(expectedKey)
                                                                         .returnValues(ReturnValue.ALL_NEW);
        UpdateItemRequest expectedRequest1 =
            baseExpectedRequest.updateExpression("REMOVE " + OTHER_ATTRIBUTE_1_NAME + ", " + OTHER_ATTRIBUTE_2_NAME)
                               .build();
        UpdateItemRequest expectedRequest2 =
            baseExpectedRequest.updateExpression("REMOVE " + OTHER_ATTRIBUTE_2_NAME + ", " + OTHER_ATTRIBUTE_1_NAME)
                               .build();

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request,either(is(expectedRequest1)).or(is(expectedRequest2)));
    }

    @Test
    public void generateRequest_canIgnoreNullValues() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .ignoreNulls(true)
                                                                .build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        Map<String, AttributeValue> expectedValues =
            singletonMap(OTHER_ATTRIBUTE_1_VALUE, AttributeValue.builder().s("value-1").build());
        Map<String, String> expectedNames = singletonMap(OTHER_ATTRIBUTE_1_NAME, "other_attribute_1");
        UpdateItemRequest expectedRequest = UpdateItemRequest.builder()
            .tableName(TABLE_NAME)
            .updateExpression("SET " + OTHER_ATTRIBUTE_1_NAME + " = " + OTHER_ATTRIBUTE_1_VALUE)
            .expressionAttributeValues(expectedValues)
            .expressionAttributeNames(expectedNames)
            .key(expectedKey)
            .returnValues(ReturnValue.ALL_NEW)
            .build();


        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_keyOnlyItem() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItemWithSort.class)
                                                                .item(item)
                                                                .ignoreNulls(true)
                                                                .build());
        Map<String, AttributeValue> expectedKey = new HashMap<>();
        expectedKey.put("id", AttributeValue.builder().s(item.getId()).build());
        expectedKey.put("sort", AttributeValue.builder().s(item.getSort()).build());
        UpdateItemRequest expectedRequest = UpdateItemRequest.builder()
                                                             .tableName(TABLE_NAME)
                                                             .key(expectedKey)
                                                             .returnValues(ReturnValue.ALL_NEW)
                                                             .build();


        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

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
        verify(mockDynamoDbEnhancedClientExtension).beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                                               .tableMetadata(FakeItem.getTableMetadata())
                                                                                               .operationContext(PRIMARY_CONTEXT)
                                                                                               .items(baseMap).build());
    }

    @Test
    public void generateRequest_withExtension_modifiesUpdateExpression() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseMap = new HashMap<>(FakeItem.getTableSchema().itemToMap(fakeItem, true));

        Map<String, AttributeValue> fakeMap = new HashMap<>(baseMap);
        fakeMap.put("subclass_attribute", AttributeValue.builder().s("1").build());

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().transformedItem(fakeMap).build());


        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(fakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.updateExpression(), is("SET " + SUBCLASS_ATTRIBUTE_NAME + " = " + SUBCLASS_ATTRIBUTE_VALUE));
        assertThat(request.expressionAttributeValues(), hasEntry(SUBCLASS_ATTRIBUTE_VALUE,
            AttributeValue.builder().s("1").build()));
        assertThat(request.expressionAttributeNames(), hasEntry(SUBCLASS_ATTRIBUTE_NAME, "subclass_attribute"));
    }

    @Test
    public void transformResponse_mapsAttributesReturnedInResponse() {
        FakeItem fakeItem1 = FakeItem.createUniqueFakeItem();
        FakeItem fakeItem2 = FakeItem.createUniqueFakeItem();
        Map<String, AttributeValue> fakeItem2Attributes = FakeItem.getTableSchema().itemToMap(fakeItem2, true);

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class).item(fakeItem1).build());

        FakeItem result = updateItemOperation.transformResponse(
            UpdateItemResponse.builder().attributes(fakeItem2Attributes).build(),
            FakeItem.getTableSchema(),
            PRIMARY_CONTEXT,
            null);

        assertThat(result, is(fakeItem2));
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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(request.conditionExpression(), is("condition"));
        assertThat(request.expressionAttributeValues(), is(fakeMap));
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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

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
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

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
    public void transformResponse_withExtension_returnsCorrectTransformedItem() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, true);
        Map<String, AttributeValue> fakeMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(
            ReadModification.builder().transformedItem(fakeMap).build());
        UpdateItemResponse response = UpdateItemResponse.builder()
                                                        .attributes(baseFakeMap)
                                                        .build();

        FakeItem resultItem = updateItemOperation.transformResponse(response, FakeItem.getTableSchema(),
                                                                    PRIMARY_CONTEXT,
                                                                    mockDynamoDbEnhancedClientExtension);

        assertThat(resultItem, is(fakeItem));
        verify(mockDynamoDbEnhancedClientExtension).afterRead(DefaultDynamoDbExtensionContext.builder()
                                                                                             .tableMetadata(FakeItem.getTableMetadata())
                                                                                             .operationContext(PRIMARY_CONTEXT)
                                                                                             .items(baseFakeMap).build());
    }

    @Test
    public void transformResponse_withNoOpExtension_returnsCorrectItem() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, true);

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                .item(baseFakeItem)
                                                                .ignoreNulls(true)
                                                                .build());

        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenReturn(
            ReadModification.builder().build());
        UpdateItemResponse response = UpdateItemResponse.builder()
                                                        .attributes(baseFakeMap)
                                                        .build();

        FakeItem resultItem = updateItemOperation.transformResponse(response, FakeItem.getTableSchema(),
                                                                    PRIMARY_CONTEXT, mockDynamoDbEnhancedClientExtension);

        assertThat(resultItem, is(baseFakeItem));
        verify(mockDynamoDbEnhancedClientExtension).afterRead(DefaultDynamoDbExtensionContext.builder()
                                                                                             .tableMetadata(FakeItem.getTableMetadata())
                                                                                             .operationContext(PRIMARY_CONTEXT)
                                                                                             .items(baseFakeMap).build());
    }

    @Test(expected = IllegalStateException.class)
    public void transformResponse_afterReadThrowsException_throwsIllegalStateException() {
        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class))).thenThrow(RuntimeException.class);
        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class).item(createUniqueFakeItem()).build());

        UpdateItemResponse response =
            UpdateItemResponse.builder()
                              .attributes(FakeItem.getTableSchema().itemToMap(FakeItem.createUniqueFakeItem(), true))
                              .build();

        updateItemOperation.transformResponse(response, FakeItem.getTableSchema(), PRIMARY_CONTEXT, mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void generateTransactWriteItem_basicRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        UpdateItemOperation<FakeItem> updateItemOperation =
            spy(UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
        String updateExpression = "update-expression";
        Map<String, AttributeValue> attributeValues = Collections.singletonMap("key", stringValue("value1"));
        Map<String, String> attributeNames = Collections.singletonMap("key", "value2");

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                                                               .tableName(TABLE_NAME)
                                                               .key(fakeItemMap)
                                                               .updateExpression(updateExpression)
                                                               .expressionAttributeValues(attributeValues)
                                                               .expressionAttributeNames(attributeNames)
                                                               .build();
        doReturn(updateItemRequest).when(updateItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = updateItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                       context,
                                                                                       mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .update(Update.builder()
                                                                          .key(fakeItemMap)
                                                                          .tableName(TABLE_NAME)
                                                                          .updateExpression(updateExpression)
                                                                          .expressionAttributeNames(attributeNames)
                                                                          .expressionAttributeValues(attributeValues)
                                                                          .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(updateItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void generateTransactWriteItem_conditionalRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        UpdateItemOperation<FakeItem> updateItemOperation =
            spy(UpdateItemOperation.create(UpdateItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
        String updateExpression = "update-expression";
        String conditionExpression = "condition-expression";
        Map<String, AttributeValue> attributeValues = Collections.singletonMap("key", stringValue("value1"));
        Map<String, String> attributeNames = Collections.singletonMap("key", "value2");

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                                                               .tableName(TABLE_NAME)
                                                               .key(fakeItemMap)
                                                               .updateExpression(updateExpression)
                                                               .conditionExpression(conditionExpression)
                                                               .expressionAttributeValues(attributeValues)
                                                               .expressionAttributeNames(attributeNames)
                                                               .build();
        doReturn(updateItemRequest).when(updateItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = updateItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                       context,
                                                                                       mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .update(Update.builder()
                                                                          .key(fakeItemMap)
                                                                          .tableName(TABLE_NAME)
                                                                          .updateExpression(updateExpression)
                                                                          .conditionExpression(conditionExpression)
                                                                          .expressionAttributeNames(attributeNames)
                                                                          .expressionAttributeValues(attributeValues)
                                                                          .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(updateItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }
}
