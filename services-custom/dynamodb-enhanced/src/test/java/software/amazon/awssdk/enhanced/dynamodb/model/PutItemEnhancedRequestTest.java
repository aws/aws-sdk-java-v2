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
        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class).build();

        PutItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

}
