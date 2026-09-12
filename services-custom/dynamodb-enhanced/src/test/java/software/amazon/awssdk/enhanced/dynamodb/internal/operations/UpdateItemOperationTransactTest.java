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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
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
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@RunWith(MockitoJUnitRunner.class)
public class UpdateItemOperationTransactTest {
    private static final String TABLE_NAME = "table-name";
    private static final String OA1_NAME = "#AMZN_MAPPED_other_attribute_1";
    private static final String OA2_NAME = "#AMZN_MAPPED_other_attribute_2";
    private static final String OA1_VAL = ":AMZN_MAPPED_other_attribute_1";
    private static final String OA2_VAL = ":AMZN_MAPPED_other_attribute_2";

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

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

        UpdateItemRequest.Builder builder = ddbRequestBuilder(fakeItemMap);
        builder.updateExpression(updateExpression);
        builder.expressionAttributeValues(attributeValues);
        builder.expressionAttributeNames(attributeNames);
        UpdateItemRequest updateItemRequest = builder.build();

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

        UpdateItemRequest.Builder builder = ddbRequestBuilder(fakeItemMap);
        builder.updateExpression(updateExpression);
        builder.expressionAttributeValues(attributeValues);
        builder.expressionAttributeNames(attributeNames);
        builder.conditionExpression(conditionExpression);
        UpdateItemRequest updateItemRequest = builder.build();

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

    @Test
    public void generateTransactWriteItem_returnValuesOnConditionCheckFailure_generatesCorrectRequest() {
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);
        String returnValues = "return-values";

        UpdateItemOperation<FakeItem> updateItemOperation =
            spy(UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                            .item(fakeItem)
                                                                            .returnValuesOnConditionCheckFailure(returnValues)
                                                                            .build()));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        doReturn(ddbRequest(fakeItemMap, b -> {})).when(updateItemOperation).generateRequest(any(), any(), any());

        TransactWriteItem actualResult = updateItemOperation.generateTransactWriteItem(FakeItem.getTableSchema(),
                                                                                       context,
                                                                                       mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult = TransactWriteItem.builder()
                                                            .update(Update.builder()
                                                                          .key(fakeItemMap)
                                                                          .tableName(TABLE_NAME)
                                                                          .returnValuesOnConditionCheckFailure(returnValues)
                                                                          .build())
                                                            .build();
        assertThat(actualResult, is(expectedResult));
        verify(updateItemOperation).generateRequest(FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void generateTransactWriteItem_withSetAction_includesSetUpdateExpression() {
        FakeItem fakeItem = createUniqueFakeItem();
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(
                                SetAction.builder()
                                         .path("attr")
                                         .value(":value")
                                         .putExpressionValue(":value", AttributeValue.builder().s("updated").build())
                                         .build())
                            .build();

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem)
                                                                        .ignoreNulls(true)
                                                                        .updateExpression(requestExpression)
                                                                        .build());
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);

        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        assertThat(request.updateExpression(), is("SET attr = :value"));
        assertThat(request.expressionAttributeValues(), is(Collections.singletonMap(":value", stringValue("updated"))));
        assertThat(transactWriteItem.update().updateExpression(), is("SET attr = :value"));
    }

    @Test
    public void generateTransactWriteItem_withRequestUpdateExpression_only_generatesUpdateItemRequest() {
        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("only_from_request")
                                                .value(":v")
                                                .putExpressionValue(":v", AttributeValue.builder().s("req").build())
                                                .build())
                            .build();

        FakeItem fakeItem = createUniqueFakeItem();
        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem)
                                                                        .ignoreNulls(true)
                                                                        .updateExpression(requestExpression)
                                                                        .build());
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        context,
                                                                        null);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItem.getTableSchema(), context, null);

        assertThat(request.updateExpression(), is("SET only_from_request = :v"));
        assertThat(request.expressionAttributeValues(), is(singletonMap(":v", AttributeValue.builder().s("req").build())));
        assertThat(transactWriteItem.update().updateExpression(), is("SET only_from_request = :v"));
        assertThat(transactWriteItem.update().expressionAttributeValues(),
                   is(singletonMap(":v", AttributeValue.builder().s("req").build())));
    }

    @Test
    public void generateTransactWriteItem_extensionAndRequest_mergeStrategyLegacy_mergesExpressions() {
        Map<String, AttributeValue> deleteActionMap = singletonMap(":val", AttributeValue.builder().s("s").build());
        DeleteAction deleteAction = DeleteAction.builder().path("attr1")
                                                .value(":val")
                                                .expressionValues(deleteActionMap)
                                                .build();
        UpdateExpression extensionExpression = UpdateExpression.builder().addAction(deleteAction).build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attr2")
                                                .value(":v2")
                                                .putExpressionValue(":v2", AttributeValue.builder().n("1").build())
                                                .build())
                            .build();

        FakeItem fakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem)
                                                                        .ignoreNulls(true)
                                                                        .updateExpression(requestExpression)
                                                                        .updateExpressionMergeStrategy(
                                                                            UpdateExpressionMergeStrategy.LEGACY)
                                                                        .build());
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        Map<String, AttributeValue> expectedValues = Expression.joinValues(deleteActionMap, singletonMap(":v2", numberValue(1)));
        assertThat(request.updateExpression(), is("SET attr2 = :v2 DELETE attr1 :val"));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().updateExpression(), is("SET attr2 = :v2 DELETE attr1 :val"));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
    }

    @Test
    public void generateTransactWriteItem_extensionAndRequest_mergeStrategyPrioritize_nonOverlapping_mergesBoth() {
        Map<String, AttributeValue> deleteActionMap = singletonMap(":val", AttributeValue.builder().s("s").build());
        DeleteAction deleteAction = DeleteAction.builder().path("attr1")
                                                .value(":val")
                                                .expressionValues(deleteActionMap)
                                                .build();
        UpdateExpression extensionExpression = UpdateExpression.builder().addAction(deleteAction).build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attr2")
                                                .value(":v2")
                                                .putExpressionValue(":v2", AttributeValue.builder().n("1").build())
                                                .build())
                            .build();

        FakeItem fakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem)
                                                                        .ignoreNulls(true)
                                                                        .updateExpression(requestExpression)
                                                                        .updateExpressionMergeStrategy(
                                                                            UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE)
                                                                        .build());
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        Map<String, AttributeValue> expectedValues = Expression.joinValues(deleteActionMap, singletonMap(":v2", numberValue(1)));
        assertThat(request.updateExpression(), is("SET attr2 = :v2 DELETE attr1 :val"));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().updateExpression(), is("SET attr2 = :v2 DELETE attr1 :val"));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
    }

    @Test
    public void generateTransactWriteItem_extensionAndRequest_mergeStrategyPrioritize_requestWinsOnSamePath() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("subclass_attribute")
                                                .value(":extVal")
                                                .putExpressionValue(":extVal", AttributeValue.builder().s("fromExt").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("subclass_attribute")
                                                .value(":reqVal")
                                                .putExpressionValue(":reqVal", AttributeValue.builder().s("fromReq").build())
                                                .build())
                            .build();

        FakeItem fakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem)
                                                                        .ignoreNulls(true)
                                                                        .updateExpression(requestExpression)
                                                                        .updateExpressionMergeStrategy(
                                                                            UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE)
                                                                        .build());
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        Map<String, AttributeValue> expectedValues =
            singletonMap(":reqVal", AttributeValue.builder().s("fromReq").build());
        assertThat(request.updateExpression(), is("SET subclass_attribute = :reqVal"));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().updateExpression(), is("SET subclass_attribute = :reqVal"));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
    }

    @Test
    public void generateTransactWriteItem_extensionAndRequest_mergeStrategyLegacy_samePath_keepsBothSetActions() {
        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("subclass_attribute")
                                                .value(":extVal")
                                                .putExpressionValue(":extVal", AttributeValue.builder().s("fromExt").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("subclass_attribute")
                                                .value(":reqVal")
                                                .putExpressionValue(":reqVal", AttributeValue.builder().s("fromReq").build())
                                                .build())
                            .build();

        FakeItem fakeItem = createUniqueFakeItem();
        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItem> updateItemOperation =
            UpdateItemOperation.create(TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem)
                                                                        .ignoreNulls(true)
                                                                        .updateExpression(requestExpression)
                                                                        .updateExpressionMergeStrategy(
                                                                            UpdateExpressionMergeStrategy.LEGACY)
                                                                        .build());
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItem.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        Map<String, AttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(":extVal", AttributeValue.builder().s("fromExt").build());
        expectedValues.put(":reqVal", AttributeValue.builder().s("fromReq").build());
        assertThat(request.updateExpression(),
                   is("SET subclass_attribute = :extVal, subclass_attribute = :reqVal"));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().updateExpression(),
                   is("SET subclass_attribute = :extVal, subclass_attribute = :reqVal"));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
    }

    @Test
    public void generateTransactWriteItem_pojoAndRequest_mergeStrategyLegacy_mergesExpressions() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attr_from_request")
                                                .value(":r")
                                                .putExpressionValue(":r", AttributeValue.builder().s("req").build())
                                                .build())
                            .build();

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(transactRequestWithSort(item,
                                                               b -> b.ignoreNulls(false)
                                                                     .updateExpression(requestExpression)
                                                                     .updateExpressionMergeStrategy(
                                                                         UpdateExpressionMergeStrategy.LEGACY)));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        context,
                                                                        null);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItemWithSort.getTableSchema(), context, null);

        String setPojoFirst =
            "SET " + OA1_NAME + " = " + OA1_VAL + ", " + OA2_NAME + " = " + OA2_VAL + ", attr_from_request = :r";
        String setPojoSecond =
            "SET " + OA2_NAME + " = " + OA2_VAL + ", " + OA1_NAME + " = " + OA1_VAL + ", attr_from_request = :r";
        Map<String, AttributeValue> expectedValues =
            Expression.joinValues(pojoAttributeValues(), singletonMap(":r", AttributeValue.builder().s("req").build()));
        Map<String, String> expectedNames = pojoExpressionNames();

        assertThat(request.updateExpression(), either(is(setPojoFirst)).or(is(setPojoSecond)));
        assertThat(transactWriteItem.update().updateExpression(), is(request.updateExpression()));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(transactWriteItem.update().expressionAttributeNames(), is(expectedNames));
    }

    @Test
    public void generateTransactWriteItem_pojoAndExtension_mergeStrategyLegacy_mergesExpressions() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");

        Map<String, AttributeValue> deleteActionMap = singletonMap(":val", AttributeValue.builder().s("s").build());
        DeleteAction deleteAction = DeleteAction.builder().path("attr1")
                                                .value(":val")
                                                .expressionValues(deleteActionMap)
                                                .build();
        UpdateExpression extensionExpression = UpdateExpression.builder().addAction(deleteAction).build();

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(transactRequestWithSort(item,
                                                               b -> b.ignoreNulls(false)
                                                                     .updateExpressionMergeStrategy(
                                                                         UpdateExpressionMergeStrategy.LEGACY)));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItemWithSort.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        String setPojoFirst =
            "SET " + OA1_NAME + " = " + OA1_VAL + ", " + OA2_NAME + " = " + OA2_VAL + " DELETE attr1 :val";
        String setPojoSecond =
            "SET " + OA2_NAME + " = " + OA2_VAL + ", " + OA1_NAME + " = " + OA1_VAL + " DELETE attr1 :val";
        Map<String, AttributeValue> expectedValues = Expression.joinValues(pojoAttributeValues(), deleteActionMap);
        Map<String, String> expectedNames = pojoExpressionNames();

        assertThat(request.updateExpression(), either(is(setPojoFirst)).or(is(setPojoSecond)));
        assertThat(transactWriteItem.update().updateExpression(), is(request.updateExpression()));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(transactWriteItem.update().expressionAttributeNames(), is(expectedNames));
    }

    @Test
    public void generateTransactWriteItem_pojoExtensionAndRequest_mergeStrategyLegacy_mergesExpressions() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");

        Map<String, AttributeValue> deleteActionMap = singletonMap(":val", AttributeValue.builder().s("s").build());
        DeleteAction deleteAction = DeleteAction.builder().path("attr1")
                                                .value(":val")
                                                .expressionValues(deleteActionMap)
                                                .build();
        UpdateExpression extensionExpression = UpdateExpression.builder().addAction(deleteAction).build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("attr2")
                                                .value(":v2")
                                                .putExpressionValue(":v2", AttributeValue.builder().n("1").build())
                                                .build())
                            .build();

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(transactRequestWithSort(item,
                                                               b -> b.ignoreNulls(false)
                                                                     .updateExpression(requestExpression)
                                                                     .updateExpressionMergeStrategy(
                                                                         UpdateExpressionMergeStrategy.LEGACY)));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItemWithSort.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        String setPojoFirst =
            "SET " + OA1_NAME + " = " + OA1_VAL + ", " + OA2_NAME + " = " + OA2_VAL + ", attr2 = :v2 DELETE attr1 :val";
        String setPojoSecond =
            "SET " + OA2_NAME + " = " + OA2_VAL + ", " + OA1_NAME + " = " + OA1_VAL + ", attr2 = :v2 DELETE attr1 :val";
        Map<String, AttributeValue> expectedValues =
            Expression.joinValues(Expression.joinValues(pojoAttributeValues(), deleteActionMap),
                                  singletonMap(":v2", numberValue(1)));
        Map<String, String> expectedNames = pojoExpressionNames();

        assertThat(request.updateExpression(), either(is(setPojoFirst)).or(is(setPojoSecond)));
        assertThat(transactWriteItem.update().updateExpression(), is(request.updateExpression()));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(transactWriteItem.update().expressionAttributeNames(), is(expectedNames));
    }

    @Test
    public void generateTransactWriteItem_pojoAndRequest_mergeStrategyPrioritize_requestWinsOverlappingPath() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("other_attribute_1")
                                                .value(":reqVal")
                                                .putExpressionValue(":reqVal", AttributeValue.builder().s("fromReq").build())
                                                .build())
                            .build();

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(transactRequestWithSort(item,
                                                               b -> b.ignoreNulls(false)
                                                                     .updateExpression(requestExpression)
                                                                     .updateExpressionMergeStrategy(
                                                                         UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE)));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        context,
                                                                        null);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItemWithSort.getTableSchema(), context, null);

        String expectedExpr = "SET other_attribute_1 = :reqVal, " + OA2_NAME + " = " + OA2_VAL;
        Map<String, AttributeValue> expectedValues =
            Expression.joinValues(singletonMap(":reqVal", AttributeValue.builder().s("fromReq").build()),
                                  singletonMap(OA2_VAL, AttributeValue.builder().s("value-2").build()));
        Map<String, String> expectedNames = singletonMap(OA2_NAME, "other_attribute_2");

        assertThat(request.updateExpression(), is(expectedExpr));
        assertThat(transactWriteItem.update().updateExpression(), is(expectedExpr));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(transactWriteItem.update().expressionAttributeNames(), is(expectedNames));
    }

    @Test
    public void generateTransactWriteItem_pojoAndExtension_mergeStrategyPrioritize_extensionWinsOverlappingPath() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("other_attribute_1")
                                                .value(":extVal")
                                                .putExpressionValue(":extVal", AttributeValue.builder().s("fromExt").build())
                                                .build())
                            .build();

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(transactRequestWithSort(item,
                                                               b -> b.ignoreNulls(false)
                                                                     .updateExpressionMergeStrategy(
                                                                         UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE)));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItemWithSort.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        String expectedExpr = "SET other_attribute_1 = :extVal, " + OA2_NAME + " = " + OA2_VAL;
        Map<String, AttributeValue> expectedValues =
            Expression.joinValues(singletonMap(":extVal", AttributeValue.builder().s("fromExt").build()),
                                  singletonMap(OA2_VAL, AttributeValue.builder().s("value-2").build()));
        Map<String, String> expectedNames = singletonMap(OA2_NAME, "other_attribute_2");

        assertThat(request.updateExpression(), is(expectedExpr));
        assertThat(transactWriteItem.update().updateExpression(), is(expectedExpr));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(transactWriteItem.update().expressionAttributeNames(), is(expectedNames));
    }

    @Test
    public void generateTransactWriteItem_pojoExtensionAndRequest_mergeStrategyPrioritize_resolvesPathPriority() {
        FakeItemWithSort item = createUniqueFakeItemWithSort();
        item.setOtherAttribute1("value-1");
        item.setOtherAttribute2("value-2");

        UpdateExpression extensionExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("other_attribute_1")
                                                .value(":extVal")
                                                .putExpressionValue(":extVal", AttributeValue.builder().s("fromExt").build())
                                                .build())
                            .build();

        UpdateExpression requestExpression =
            UpdateExpression.builder()
                            .addAction(SetAction.builder()
                                                .path("isolated_attr")
                                                .value(":reqVal")
                                                .putExpressionValue(":reqVal", AttributeValue.builder().s("fromReq").build())
                                                .build())
                            .build();

        when(mockDynamoDbEnhancedClientExtension.beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class)))
            .thenReturn(WriteModification.builder().updateExpression(extensionExpression).build());

        UpdateItemOperation<FakeItemWithSort> updateItemOperation =
            UpdateItemOperation.create(transactRequestWithSort(item,
                                                               b -> b.ignoreNulls(false)
                                                                     .updateExpression(requestExpression)
                                                                     .updateExpressionMergeStrategy(
                                                                         UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE)));
        OperationContext context = DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

        UpdateItemRequest request = updateItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                        context,
                                                                        mockDynamoDbEnhancedClientExtension);
        TransactWriteItem transactWriteItem = updateItemOperation.generateTransactWriteItem(
            FakeItemWithSort.getTableSchema(), context, mockDynamoDbEnhancedClientExtension);

        String expectedExpr =
            "SET isolated_attr = :reqVal, other_attribute_1 = :extVal, " + OA2_NAME + " = " + OA2_VAL;
        Map<String, AttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(":reqVal", AttributeValue.builder().s("fromReq").build());
        expectedValues.put(":extVal", AttributeValue.builder().s("fromExt").build());
        expectedValues.put(OA2_VAL, AttributeValue.builder().s("value-2").build());
        Map<String, String> expectedNames = singletonMap(OA2_NAME, "other_attribute_2");

        assertThat(request.updateExpression(), is(expectedExpr));
        assertThat(transactWriteItem.update().updateExpression(), is(expectedExpr));
        assertThat(request.expressionAttributeValues(), is(expectedValues));
        assertThat(transactWriteItem.update().expressionAttributeValues(), is(expectedValues));
        assertThat(request.expressionAttributeNames(), is(expectedNames));
        assertThat(transactWriteItem.update().expressionAttributeNames(), is(expectedNames));
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

    private static TransactUpdateItemEnhancedRequest<FakeItemWithSort> transactRequestWithSort(
        FakeItemWithSort item,
        Consumer<TransactUpdateItemEnhancedRequest.Builder<FakeItemWithSort>> modify) {
        TransactUpdateItemEnhancedRequest.Builder<FakeItemWithSort> builder =
            TransactUpdateItemEnhancedRequest.builder(FakeItemWithSort.class).item(item);
        modify.accept(builder);
        return builder.build();
    }

    private static Map<String, AttributeValue> pojoAttributeValues() {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put(OA1_VAL, AttributeValue.builder().s("value-1").build());
        m.put(OA2_VAL, AttributeValue.builder().s("value-2").build());
        return m;
    }

    private static Map<String, String> pojoExpressionNames() {
        Map<String, String> m = new HashMap<>();
        m.put(OA1_NAME, "other_attribute_1");
        m.put(OA2_NAME, "other_attribute_2");
        return m;
    }
}
