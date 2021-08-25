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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@RunWith(MockitoJUnitRunner.class)
public class ConditionCheckTest {
    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void generateTransactWriteItem() {
        FakeItem fakeItem = FakeItem.createUniqueFakeItem();
        Map<String, AttributeValue> keyMap = singletonMap("id", stringValue(fakeItem.getId()));
        String returnValues = "return-values";
        Expression conditionExpression = Expression.builder()
                                                   .expression("expression")
                                                   .expressionNames(singletonMap("key1", "value1"))
                                                   .expressionValues(singletonMap("key2", stringValue("value2")))
                                                   .build();
        ConditionCheck<FakeItem> operation =
            ConditionCheck.builder()
                          .key(k -> k.partitionValue(fakeItem.getId()))
                          .conditionExpression(conditionExpression)
                          .returnValuesOnConditionCheckFailure(returnValues)
                          .build();
        OperationContext context = DefaultOperationContext.create("table-name", TableMetadata.primaryIndexName());

        TransactWriteItem result = operation.generateTransactWriteItem(FakeItem.getTableSchema(), context,
                                                                       mockDynamoDbEnhancedClientExtension);

        TransactWriteItem expectedResult =
            TransactWriteItem.builder()
                             .conditionCheck(
                                 software.amazon.awssdk.services.dynamodb.model.ConditionCheck
                                     .builder()
                                     .tableName("table-name")
                                     .key(keyMap)
                                     .conditionExpression(conditionExpression.expression())
                                     .expressionAttributeValues(conditionExpression.expressionValues())
                                     .expressionAttributeNames(conditionExpression.expressionNames())
                                     .returnValuesOnConditionCheckFailure(returnValues)
                                     .build())
                             .build();
        assertThat(result, is(expectedResult));
        verifyZeroInteractions(mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void builder_minimal() {
        ConditionCheck<Object> builtObject = ConditionCheck.builder().build();

        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        FakeItem fakeItem = FakeItem.createUniqueFakeItem();
        String returnValues = "ALL_OLD";
        Expression conditionExpression = Expression.builder()
                                                   .expression("expression")
                                                   .expressionNames(singletonMap("key1", "value1"))
                                                   .expressionValues(singletonMap("key2", stringValue("value2")))
                                                   .build();
        ConditionCheck<FakeItem> builtObject =
            ConditionCheck.builder()
                          .key(k -> k.partitionValue(fakeItem.getId()))
                          .conditionExpression(conditionExpression)
                          .returnValuesOnConditionCheckFailure(returnValues)
                          .build();

        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(ReturnValuesOnConditionCheckFailure.ALL_OLD));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValues));
    }

    @Test
    public void equals_maximal() {
        FakeItem fakeItem = FakeItem.createUniqueFakeItem();
        String returnValues = "ALL_OLD";
        Expression conditionExpression = Expression.builder()
                                                   .expression("expression")
                                                   .expressionNames(singletonMap("key1", "value1"))
                                                   .expressionValues(singletonMap("key2", stringValue("value2")))
                                                   .build();
        ConditionCheck<FakeItem> builtObject =
            ConditionCheck.builder()
                          .key(k -> k.partitionValue(fakeItem.getId()))
                          .conditionExpression(conditionExpression)
                          .returnValuesOnConditionCheckFailure(returnValues)
                          .build();

        ConditionCheck<Object> copiedObject = builtObject.toBuilder().build();

        assertThat(builtObject, equalTo(copiedObject));
    }

    @Test
    public void equals_minimal() {
        ConditionCheck<FakeItem> builtObject1 = ConditionCheck.builder().build();
        ConditionCheck<FakeItem> builtObject2 = ConditionCheck.builder().build();

        assertThat(builtObject1, equalTo(builtObject2));
    }

    @Test
    public void equals_differentType() {
        ConditionCheck<FakeItem> builtObject1 = ConditionCheck.builder().build();
        software.amazon.awssdk.services.dynamodb.model.ConditionCheck differentType =
            software.amazon.awssdk.services.dynamodb.model.ConditionCheck.builder().build();

        assertThat(builtObject1, not(equalTo(differentType)));
    }

    @Test
    public void equals_self() {
        ConditionCheck<FakeItem> builtObject = ConditionCheck.builder().build();

        assertThat(builtObject, equalTo(builtObject));
    }

    @Test
    public void equals_keyNotEqual() {
        Key key1 = Key.builder().partitionValue("key1").build();
        Key key2 = Key.builder().partitionValue("key2").build();

        ConditionCheck<FakeItem> builtObject1 = ConditionCheck.builder().key(key1).build();
        ConditionCheck<FakeItem> builtObject2 = ConditionCheck.builder().key(key2).build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_conditionExpressionNotEqual() {
        Expression conditionExpression1 = Expression.builder()
                                                   .expression("expression1")
                                                   .expressionNames(singletonMap("key1", "value1"))
                                                   .expressionValues(singletonMap("key2", stringValue("value2")))
                                                   .build();

        Expression conditionExpression2 = Expression.builder()
                                                   .expression("expression2")
                                                   .expressionNames(singletonMap("key1", "value1"))
                                                   .expressionValues(singletonMap("key2", stringValue("value2")))
                                                   .build();

        ConditionCheck<FakeItem> builtObject1 = ConditionCheck.builder().conditionExpression(conditionExpression1).build();
        ConditionCheck<FakeItem> builtObject2 = ConditionCheck.builder().conditionExpression(conditionExpression2).build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnValuesNotEqual() {
        ReturnValuesOnConditionCheckFailure returnValues1 = ReturnValuesOnConditionCheckFailure.NONE;
        ReturnValuesOnConditionCheckFailure returnValues2 = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        ConditionCheck<FakeItem> builtObject1 = ConditionCheck.builder().returnValuesOnConditionCheckFailure(returnValues1).build();
        ConditionCheck<FakeItem> builtObject2 = ConditionCheck.builder().returnValuesOnConditionCheckFailure(returnValues2).build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_maximal() {
        FakeItem fakeItem = FakeItem.createUniqueFakeItem();
        String returnValues = "ALL_OLD";
        Expression conditionExpression = Expression.builder()
                                                   .expression("expression")
                                                   .expressionNames(singletonMap("key1", "value1"))
                                                   .expressionValues(singletonMap("key2", stringValue("value2")))
                                                   .build();
        ConditionCheck<FakeItem> builtObject =
            ConditionCheck.builder()
                          .key(k -> k.partitionValue(fakeItem.getId()))
                          .conditionExpression(conditionExpression)
                          .returnValuesOnConditionCheckFailure(returnValues)
                          .build();

        ConditionCheck<Object> copiedObject = builtObject.toBuilder().build();

        assertThat(builtObject.hashCode(), equalTo(copiedObject.hashCode()));
    }

    @Test
    public void hashCode_minimal() {
        ConditionCheck<FakeItem> builtObject1 = ConditionCheck.builder().build();
        ConditionCheck<FakeItem> builtObject2 = ConditionCheck.builder().build();

        assertThat(builtObject1.hashCode(), equalTo(builtObject2.hashCode()));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureNewValue_enumGetterReturnsUnknownValue() {
        String returnValues = "new-value";
        ConditionCheck<FakeItem> builtObject = ConditionCheck.builder()
                                                             .returnValuesOnConditionCheckFailure(returnValues)
                                                             .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailure(),
                   equalTo(ReturnValuesOnConditionCheckFailure.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureNewValue_stringGetter() {
        String returnValues = "new-value";
        ConditionCheck<FakeItem> builtObject = ConditionCheck.builder()
                                                             .returnValuesOnConditionCheckFailure(returnValues)
                                                             .build();
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValues));
    }
}
