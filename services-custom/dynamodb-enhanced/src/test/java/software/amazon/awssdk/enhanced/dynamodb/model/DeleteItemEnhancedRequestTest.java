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
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;

@RunWith(MockitoJUnitRunner.class)
public class DeleteItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        Key key = Key.builder().partitionValue("key").build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder()
                                                                         .key(key)
                                                                         .conditionExpression(conditionExpression)
                                                                         .returnConsumedCapacity(returnConsumedCapacity)
                                                                         .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                         .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnConsumedCapacity(), is(returnConsumedCapacity));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(returnConsumedCapacity.toString()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(returnItemCollectionMetrics));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(returnItemCollectionMetrics.toString()));
    }

    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder()
                                                                         .key(key)
                                                                         .conditionExpression(conditionExpression)
                                                                         .returnConsumedCapacity(returnConsumedCapacity)
                                                                         .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                         .build();

        DeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void equals_keyNotEqual() {
        Key key1 = Key.builder().partitionValue("key1").build();
        Key key2 = Key.builder().partitionValue("key2").build();

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder().key(key1).build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder().key(key2).build();

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

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .conditionExpression(conditionExpression1)
                                                                          .build();

        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .conditionExpression(conditionExpression2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnConsumedCapacityNotEqual() {
        ReturnConsumedCapacity returnConsumedCapacity1 = ReturnConsumedCapacity.TOTAL;
        ReturnConsumedCapacity returnConsumedCapacity2 = ReturnConsumedCapacity.NONE;

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .returnConsumedCapacity(returnConsumedCapacity1)
                                                                          .build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .returnConsumedCapacity(returnConsumedCapacity2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnItemCollectionMetricsNotEqual() {
        ReturnItemCollectionMetrics returnItemCollectionMetrics1 = ReturnItemCollectionMetrics.SIZE;
        ReturnItemCollectionMetrics returnItemCollectionMetrics2 = ReturnItemCollectionMetrics.NONE;

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .returnItemCollectionMetrics(returnItemCollectionMetrics1)
                                                                          .build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .returnItemCollectionMetrics(returnItemCollectionMetrics2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_minimal() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        assertThat(emptyRequest.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesKey() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        Key key = Key.builder().partitionValue("key").build();

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder().key(key).build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesConditionExpression() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .conditionExpression(conditionExpression)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnConsumedCapacity() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .returnConsumedCapacity(returnConsumedCapacity)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnItemCollectionMetrics() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }
}
