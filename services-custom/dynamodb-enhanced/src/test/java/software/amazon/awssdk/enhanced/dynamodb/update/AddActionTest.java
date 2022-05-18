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

package software.amazon.awssdk.enhanced.dynamodb.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class AddActionTest {

    private static final String PATH = "path string";
    private static final String VALUE = "value string";
    private static final String VALUE_TOKEN = ":valueToken";
    private static final String ATTRIBUTE_TOKEN = "#attributeToken";
    private static final String ATTRIBUTE_NAME = "attribute1";
    private static final AttributeValue NUMERIC_VALUE = AttributeValue.builder().n("5").build();

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(AddAction.class)
                      .usingGetClass()
                      .withPrefabValues(AttributeValue.class,
                                        AttributeValue.builder().s("1").build(),
                                        AttributeValue.builder().s("2").build())
                      .verify();
    }

    @Test
    void build_minimal() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .build();
        assertThat(action.path()).isEqualTo(PATH);
        assertThat(action.value()).isEqualTo(VALUE);
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
        assertThat(action.expressionNames()).isEmpty();
    }

    @Test
    void build_maximal() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .expressionValues(Collections.singletonMap(VALUE_TOKEN, NUMERIC_VALUE))
                                    .expressionNames(Collections.singletonMap(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME))
                                    .build();
        assertThat(action.path()).isEqualTo(PATH);
        assertThat(action.value()).isEqualTo(VALUE);
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
    }

    @Test
    void copy() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .expressionValues(Collections.singletonMap(VALUE_TOKEN, NUMERIC_VALUE))
                                    .expressionNames(Collections.singletonMap(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME))
                                    .build();
        AddAction copy = action.toBuilder().build();
        assertThat(action).isEqualTo(copy);
    }
}