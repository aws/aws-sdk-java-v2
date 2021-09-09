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

@RunWith(MockitoJUnitRunner.class)
public class PutItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject.item(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
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

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .item(fakeItem)
                                                                             .conditionExpression(conditionExpression)
                                                                             .build();

        assertThat(builtObject.item(), is(fakeItem));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
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

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .item(fakeItem)
                                                                             .conditionExpression(conditionExpression)
                                                                             .build();

        PutItemEnhancedRequest<FakeItem> copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void equals_itemNotEqual() {
        FakeItem item1 = createUniqueFakeItem();
        FakeItem item2 = createUniqueFakeItem();

        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .item(item1)
                                                                              .build();
        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .item(item2)
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

        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .conditionExpression(conditionExpression1)
                                                                              .build();
        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .conditionExpression(conditionExpression2)
                                                                              .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_minimal() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(emptyRequest.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesItem() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();
        PutItemEnhancedRequest<FakeItem> containsItem = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .item(createUniqueFakeItem())
                                                                              .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesConditionExpression() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        PutItemEnhancedRequest<FakeItem> containsExpression = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .conditionExpression(conditionExpression)
                                                                                    .build();

        assertThat(containsExpression.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }
}
