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
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@RunWith(MockitoJUnitRunner.class)
public class UpdateItemOperationTransactTest {
    private static final String TABLE_NAME = "table-name";

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

}
