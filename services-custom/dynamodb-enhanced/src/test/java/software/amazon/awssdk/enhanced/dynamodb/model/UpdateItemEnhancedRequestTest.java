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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;

@RunWith(MockitoJUnitRunner.class)
public class UpdateItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject.item(), is(nullValue()));
        assertThat(builtObject.ignoreNulls(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(nullValue()));
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

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                   .item(fakeItem)
                                                                                   .ignoreNulls(true)
                                                                                   .conditionExpression(conditionExpression)
                                                                                   .returnConsumedCapacity(returnConsumedCapacity)
                                                                                   .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                                   .build();

        assertThat(builtObject.item(), is(fakeItem));
        assertThat(builtObject.ignoreNulls(), is(true));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnConsumedCapacity(), is(returnConsumedCapacity));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(returnConsumedCapacity.toString()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(returnItemCollectionMetrics));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(returnItemCollectionMetrics.toString()));
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

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                   .item(fakeItem)
                                                                                   .ignoreNulls(true)
                                                                                   .conditionExpression(conditionExpression)
                                                                                   .returnConsumedCapacity(returnConsumedCapacity)
                                                                                   .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                                   .build();

        UpdateItemEnhancedRequest<FakeItem> copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void equals_self() {
        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject, equalTo(builtObject));
    }

    @Test
    public void equals_differentType() {
        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject, not(equalTo(new Object())));
    }

    @Test
    public void equals_itemNotEqual() {
        FakeItem fakeItem1 = createUniqueFakeItem();
        FakeItem fakeItem2 = createUniqueFakeItem();

        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .item(fakeItem1)
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .item(fakeItem2)
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_ignoreNullsNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .ignoreNulls(true)
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .ignoreNulls(false)
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_conditionExpressionNotEqual() {
        Expression conditionExpression1 = Expression.builder()
                                                    .expression("#key = :value OR #key1 = :value1")
                                                    .putExpressionName("#key", "attribute")
                                                    .putExpressionName("#key1", "attribute3")
                                                    .putExpressionValue(":value", stringValue("wrong"))
                                                    .putExpressionValue(":value1", stringValue("three"))
                                                    .build();

        Expression conditionExpression2 = Expression.builder()
                                                    .expression("#key = :value AND #key1 = :value1")
                                                    .putExpressionName("#key", "attribute")
                                                    .putExpressionName("#key1", "attribute3")
                                                    .putExpressionValue(":value", stringValue("wrong"))
                                                    .putExpressionValue(":value1", stringValue("three"))
                                                    .build();
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .conditionExpression(conditionExpression1)
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .conditionExpression(conditionExpression2)
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnConsumedCapacityNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnConsumedCapacity("return1")
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnConsumedCapacity("return2")
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnItemCollectionMetricsNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnItemCollectionMetrics("return1")
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnItemCollectionMetrics("return2")
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_minimal() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(emptyRequest.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesItem() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .item(createUniqueFakeItem())
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesIgnoreNulls() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .ignoreNulls(true)
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesConditionExpression() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        UpdateItemEnhancedRequest<FakeItem> containsExpression = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                          .conditionExpression(conditionExpression)
                                                                                          .build();

        assertThat(containsExpression.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnConsumedCapacity() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnConsumedCapacity("return1")
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnItemCollectionMetrics() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnItemCollectionMetrics("return1")
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }
}
