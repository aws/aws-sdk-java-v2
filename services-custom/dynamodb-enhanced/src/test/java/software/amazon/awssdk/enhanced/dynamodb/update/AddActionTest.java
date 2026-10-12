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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void build_withNullPath_throwsNullPointerException() {
        assertThatThrownBy(() -> AddAction.builder()
                                          .path(null)
                                          .value(VALUE)
                                          .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                          .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("path");
    }

    @Test
    void build_withNullValue_throwsNullPointerException() {
        assertThatThrownBy(() -> AddAction.builder()
                                          .path(PATH)
                                          .value(null)
                                          .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                          .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    @Test
    void build_withNullExpressionValues_throwsNullPointerException() {
        assertThatThrownBy(() -> AddAction.builder()
                                          .path(PATH)
                                          .value(VALUE)
                                          .expressionValues(null)
                                          .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expressionValues");
    }

    @Test
    void builder_expressionNames_withNullMap_setsToNull() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .expressionNames(null)
                                    .build();
        assertThat(action.expressionNames()).isEmpty();
    }

    @Test
    void builder_putExpressionName_withNullExpressionNames_createsNewMap() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .putExpressionName(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME)
                                    .build();
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
    }

    @Test
    void builder_expressionValues_withNullMap_setsToNull() {
        assertThatThrownBy(() -> AddAction.builder()
                                          .path(PATH)
                                          .value(VALUE)
                                          .expressionValues(null)
                                          .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expressionValues");
    }

    @Test
    void builder_putExpressionValue_withNullExpressionValues_createsNewMap() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .build();
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
    }

    @Test
    void builder_putExpressionValue_whenExpressionValuesIsNull_createsNewMap() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .build();
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
    }

    @Test
    void builder_putExpressionName_whenExpressionNamesIsNull_createsNewMap() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .putExpressionName(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME)
                                    .build();
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
    }

    @Test
    void builder_putExpressionValue_withInitiallyNullExpressionValues_createsNewHashMap() {
        AddAction.Builder builder = AddAction.builder()
                                             .path(PATH)
                                             .value(VALUE);
        // expressionValues is initially null
        builder.putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE);
        AddAction action = builder.build();
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
    }

    @Test
    void builder_putExpressionName_withInitiallyNullExpressionNames_createsNewHashMap() {
        AddAction.Builder builder = AddAction.builder()
                                             .path(PATH)
                                             .value(VALUE)
                                             .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE);
        // expressionNames is initially null
        builder.putExpressionName(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
        AddAction action = builder.build();
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
    }

    @Test
    void builder_putExpressionValue_whenFieldIsNull_createsNewMap() {
        AddAction.Builder builder = AddAction.builder()
                                             .path(PATH)
                                             .value(VALUE);
        // Ensure expressionValues is null initially
        builder.putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE);
        AddAction action = builder.build();
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
    }

    @Test
    void builder_putExpressionName_whenFieldIsNull_createsNewMap() {
        AddAction.Builder builder = AddAction.builder()
                                             .path(PATH)
                                             .value(VALUE)
                                             .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE);
        builder.putExpressionName(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
        AddAction action = builder.build();
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
    }

    @Test
    void builder_putExpressionName_whenExpressionNamesIsNotNull_addsToExistingMap() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .expressionNames(Collections.singletonMap("existing", "value"))
                                    .putExpressionName(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME)
                                    .build();
        assertThat(action.expressionNames()).containsEntry(ATTRIBUTE_TOKEN, ATTRIBUTE_NAME);
        assertThat(action.expressionNames()).containsEntry("existing", "value");
    }

    @Test
    void builder_putExpressionValue_whenExpressionValuesIsNotNull_addsToExistingMap() {
        AddAction action = AddAction.builder()
                                    .path(PATH)
                                    .value(VALUE)
                                    .expressionValues(Collections.singletonMap("existing", NUMERIC_VALUE))
                                    .putExpressionValue(VALUE_TOKEN, NUMERIC_VALUE)
                                    .build();
        assertThat(action.expressionValues()).containsEntry(VALUE_TOKEN, NUMERIC_VALUE);
        assertThat(action.expressionValues()).containsEntry("existing", NUMERIC_VALUE);
    }
}