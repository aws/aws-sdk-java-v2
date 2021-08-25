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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@RunWith(MockitoJUnitRunner.class)
public class TransactDeleteItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
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

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .key(key)
                                                                                         .conditionExpression(conditionExpression)
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(returnValues));
    }

    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .key(key)
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        TransactDeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureSetterNull_noNpe() {
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) null)
                                                                                         .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureSetterString() {
        String returnValues = "new-value";
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValues));
    }

}
