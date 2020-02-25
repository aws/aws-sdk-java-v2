/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;

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
        Key key = Key.create(stringValue("key"));

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
        Key key = Key.create(stringValue("key"));

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder().key(key).build();

        DeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

}