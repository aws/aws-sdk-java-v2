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

@RunWith(MockitoJUnitRunner.class)
public class DeleteItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
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

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder()
                                                                         .key(key)
                                                                         .conditionExpression(conditionExpression)
                                                                         .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
    }

    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder().key(key).build();

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
}
