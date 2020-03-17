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

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class ScanEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder().build();

        assertThat(builtObject.exclusiveStartKey(), is(nullValue()));
        assertThat(builtObject.consistentRead(), is(nullValue()));
        assertThat(builtObject.filterExpression(), is(nullValue()));
        assertThat(builtObject.limit(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));

        Map<String, AttributeValue> expressionValues = singletonMap(":test-key", stringValue("test-value"));
        Expression filterExpression = Expression.builder()
                                                .expression("test-expression")
                                                .expressionValues(expressionValues)
                                                .build();

        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder()
                                                             .exclusiveStartKey(exclusiveStartKey)
                                                             .consistentRead(false)
                                                             .filterExpression(filterExpression)
                                                             .limit(3)
                                                             .build();

        assertThat(builtObject.exclusiveStartKey(), is(exclusiveStartKey));
        assertThat(builtObject.consistentRead(), is(false));
        assertThat(builtObject.filterExpression(), is(filterExpression));
        assertThat(builtObject.limit(), is(3));
    }

    @Test
    public void toBuilder() {
        ScanEnhancedRequest builtObject = ScanEnhancedRequest.builder().exclusiveStartKey(null).build();

        ScanEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

}
