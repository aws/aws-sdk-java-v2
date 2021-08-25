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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@RunWith(MockitoJUnitRunner.class)
public class TransactUpdateItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        TransactUpdateItemEnhancedRequest<FakeItem> builtObject =
            TransactUpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject.item(), is(nullValue()));
        assertThat(builtObject.ignoreNulls(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        FakeItem fakeItem = createUniqueFakeItem();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactUpdateItemEnhancedRequest<FakeItem> builtObject = TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                                   .item(fakeItem)
                                                                                                   .ignoreNulls(true)
                                                                                                   .conditionExpression(conditionExpression)
                                                                                                   .returnValuesOnConditionCheckFailure(returnValues)
                                                                                                   .build();

        assertThat(builtObject.item(), is(fakeItem));
        assertThat(builtObject.ignoreNulls(), is(true));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(returnValues));
    }

    @Test
    public void toBuilder() {
        FakeItem fakeItem = createUniqueFakeItem();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactUpdateItemEnhancedRequest<FakeItem> builtObject = TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                                   .item(fakeItem)
                                                                                                   .ignoreNulls(true)
                                                                                                   .conditionExpression(conditionExpression)
                                                                                                   .returnValuesOnConditionCheckFailure(returnValues)
                                                                                                   .build();

        TransactUpdateItemEnhancedRequest<FakeItem> copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureSetterNull_noNpe() {
        TransactUpdateItemEnhancedRequest<FakeItem> builtObject = TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                                   .item(createUniqueFakeItem())
                                                                                                   .returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) null)
                                                                                                   .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(nullValue()));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureNewValue_enumGetterReturnsUnknownValue() {
        String returnValues = "new-value";
        TransactUpdateItemEnhancedRequest<FakeItem> builtObject = TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                                   .returnValuesOnConditionCheckFailure(returnValues)
                                                                                                   .build();
        assertThat(builtObject.returnValuesOnConditionCheckFailure(),
                   equalTo(ReturnValuesOnConditionCheckFailure.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureNewValue_stringGetter() {
        String returnValues = "new-value";
        TransactUpdateItemEnhancedRequest<FakeItem> builtObject = TransactUpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                                   .returnValuesOnConditionCheckFailure(returnValues)
                                                                                                   .build();
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValues));
    }
}