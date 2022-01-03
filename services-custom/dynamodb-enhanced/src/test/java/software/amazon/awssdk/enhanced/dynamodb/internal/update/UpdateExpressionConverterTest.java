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

package software.amazon.awssdk.enhanced.dynamodb.internal.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Expression;

import software.amazon.awssdk.enhanced.dynamodb.update.AddUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class UpdateExpressionConverterTest {

    private static final String KEY_TOKEN = "#PRE_";
    private static final String VALUE_TOKEN = ":PRE_";

    @Test
    void convert_emptyExpression() {
        UpdateExpression updateExpression = UpdateExpression.builder().build();
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEmpty();
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_single_removeAction() {
        UpdateExpression updateExpression = updateExpression(removeAction("attribute1", false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("REMOVE attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_multiple_removeAction() {
        UpdateExpression updateExpression = updateExpression(removeAction("attribute1", false),
                                                             removeAction("attribute2", false));

        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("REMOVE attribute1, attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_multiple_removeAction_useNameTokens() {
        UpdateExpression updateExpression = updateExpression(removeAction("attribute1", true),
                                                             removeAction("attribute2", true));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, String> expectedExpressionNames = new HashMap<>();
        expectedExpressionNames.put("#PRE_attribute1", "attribute1");
        expectedExpressionNames.put("#PRE_attribute2", "attribute2");

        assertThat(expression.expression()).isEqualTo("REMOVE #PRE_attribute1, #PRE_attribute2");
        assertThat(expression.expressionNames()).containsExactlyEntriesOf(expectedExpressionNames);
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_single_setAction() {
        UpdateExpression updateExpression = updateExpression(setAction("attribute1", string("val1"),false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("SET attribute1 = :PRE_attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry(":PRE_attribute1", string("val1"));
    }

    @Test
    void convert_multiple_setAction() {
        UpdateExpression updateExpression = updateExpression(setAction("attribute1", string("val1"), false),
                                                             setAction("attribute2", string("val2"), false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("SET attribute1 = :PRE_attribute1, attribute2 = :PRE_attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_multiple_setAction_useNameTokens() {
        UpdateExpression updateExpression = updateExpression(setAction("attribute1", string("val1"), true),
                                                             setAction("attribute2", string("val2"), true));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, String> expectedExpressionNames = new HashMap<>();
        expectedExpressionNames.put("#PRE_attribute1", "attribute1");
        expectedExpressionNames.put("#PRE_attribute2", "attribute2");

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("SET #PRE_attribute1 = :PRE_attribute1, #PRE_attribute2 = :PRE_attribute2");
        assertThat(expression.expressionNames()).containsExactlyEntriesOf(expectedExpressionNames);
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_single_deleteAction() {
        UpdateExpression updateExpression = updateExpression(deleteAction("attribute1", string("val1"),false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("DELETE attribute1 :PRE_attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry(":PRE_attribute1", string("val1"));
    }

    @Test
    void convert_multiple_deleteAction() {
        UpdateExpression updateExpression = updateExpression(deleteAction("attribute1", string("val1"), false),
                                                             deleteAction("attribute2", string("val2"), false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("DELETE attribute1 :PRE_attribute1, attribute2 :PRE_attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_multiple_deleteAction_useNameTokens() {
        UpdateExpression updateExpression = updateExpression(deleteAction("attribute1", string("val1"), true),
                                                             deleteAction("attribute2", string("val2"), true));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, String> expectedExpressionNames = new HashMap<>();
        expectedExpressionNames.put("#PRE_attribute1", "attribute1");
        expectedExpressionNames.put("#PRE_attribute2", "attribute2");

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("DELETE #PRE_attribute1 :PRE_attribute1, #PRE_attribute2 :PRE_attribute2");
        assertThat(expression.expressionNames()).containsExactlyEntriesOf(expectedExpressionNames);
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_single_addAction() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"),false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("ADD attribute1 :PRE_attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry(":PRE_attribute1", string("val1"));
    }

    @Test
    void convert_multiple_addAction() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"), false),
                                                             addAction("attribute2", string("val2"), false));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("ADD attribute1 :PRE_attribute1, attribute2 :PRE_attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_multiple_addAction_useNameTokens() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"), true),
                                                             addAction("attribute2", string("val2"), true));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, String> expectedExpressionNames = new HashMap<>();
        expectedExpressionNames.put("#PRE_attribute1", "attribute1");
        expectedExpressionNames.put("#PRE_attribute2", "attribute2");

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("ADD #PRE_attribute1 :PRE_attribute1, #PRE_attribute2 :PRE_attribute2");
        assertThat(expression.expressionNames()).containsExactlyEntriesOf(expectedExpressionNames);
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_multiple_mixedAction_useNameTokens() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"), true),
                                                             deleteAction("attribute2", string("val2"), true),
                                                             removeAction("attribute3", true),
                                                             setAction("attribute4", string("val4"), true),
                                                             deleteAction("attribute5", string("val5"), true),
                                                             setAction("attribute6", string("val6"), true),
                                                             removeAction("attribute7", true),
                                                             addAction("attribute8", string("val8"), true),
                                                             removeAction("attribute9", true));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        assertThat(expression.expression()).isEqualTo(
            "SET #PRE_attribute4 = :PRE_attribute4, #PRE_attribute6 = :PRE_attribute6 " +
            "REMOVE #PRE_attribute3, #PRE_attribute7, #PRE_attribute9 " +
            "DELETE #PRE_attribute2 :PRE_attribute2, #PRE_attribute5 :PRE_attribute5 " +
            "ADD #PRE_attribute1 :PRE_attribute1, #PRE_attribute8 :PRE_attribute8"
        );
        assertThat(expression.expressionNames()).hasSize(9);
        assertThat(expression.expressionValues()).hasSize(6);
    }

    @Test
    void convert_multiple_actions_with_duplicates() {
        UpdateExpression updateExpression = updateExpression(deleteAction("attribute1", string("val1"), true),
                                                             deleteAction("attribute1", string("val2"), true));

        assertThatThrownBy(() -> UpdateExpressionConverter.toExpression(updateExpression))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(":PRE_attribute1");
    }

    @Test
    void convert_multiple_actions_with_duplicates_removes() {
        UpdateExpression updateExpression = updateExpression(removeAction("attribute1", true),
                                                             removeAction("attribute1", true));

        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("REMOVE #PRE_attribute1, #PRE_attribute1");
        assertThat(expression.expressionNames()).hasSize(1);
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_multiple_actions_with_duplicates_diffact() {
        UpdateExpression updateExpression = updateExpression(deleteAction("attribute1", string("val1"), true),
                                                             addAction("attribute1", string("val2"), true));
        assertThatThrownBy(() -> UpdateExpressionConverter.toExpression(updateExpression))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(":PRE_attribute1");
    }

    @Test
    void findAttributeNames_emptyExpression_returnEmptyList() {
        UpdateExpression updateExpression = UpdateExpression.builder().build();
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).isEmpty();
    }

    @Test
    void findAttributeNames_noComposedNames_noTokens() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"), false),
                                                             deleteAction("attribute2", string("val2"), false),
                                                             removeAction("attribute3", false),
                                                             setAction("attribute4", string("val4"), false));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_noComposedNames_tokens() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"), true),
                                                             deleteAction("attribute2", string("val2"), true),
                                                             removeAction("attribute3", true),
                                                             setAction("attribute4", string("val4"), true));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_noComposedNames_duplicates() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1", string("val1"), false),
                                                             deleteAction("attribute1", string("val2"), true));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute1");
    }

    @Test
    void findAttributeNames_composedNames_noTokens() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1.#nested", string("val1"), false),
                                                             deleteAction("attribute2.nested", string("val2"), false),
                                                             removeAction("attribute3[1]", false),
                                                             setAction("attribute4[1].nested", string("val4"), false));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_composedNames_tokens() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1.nested[1]", string("val1"), true),
                                                             deleteAction("attribute2.nested", string("val2"), true),
                                                             removeAction("attribute3[1]", true),
                                                             setAction("attribute4[1].nested", string("val4"), true));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_composedNames_duplicates() {
        UpdateExpression updateExpression = updateExpression(addAction("attribute1[1]", string("val1"), false),
                                                             deleteAction("attribute1.nested", string("val2"), true));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute1");
    }

    private static RemoveUpdateAction removeAction(String attributeName, boolean useToken) {
        RemoveUpdateAction.Builder builder = RemoveUpdateAction.builder()
                                                               .path(attributeName);
        if (useToken) {
            builder.path(keyRef(attributeName));
            builder.putExpressionName(keyRef(attributeName), attributeName);
        }
        return builder.build();
    }

    private static SetUpdateAction setAction(String attributeName, AttributeValue value, boolean useToken) {
        SetUpdateAction.Builder builder = SetUpdateAction.builder()
                                                         .path(attributeName)
                                                         .value(valueRef(attributeName))
                                                         .putExpressionValue(valueRef(attributeName), value);
        if (useToken) {
            builder.path(keyRef(attributeName));
            builder.putExpressionName(keyRef(attributeName), attributeName);
        }
        return builder.build();
    }

    private static DeleteUpdateAction deleteAction(String attributeName, AttributeValue value, boolean useToken) {
        DeleteUpdateAction.Builder builder = DeleteUpdateAction.builder()
                                                         .path(attributeName)
                                                         .value(valueRef(attributeName))
                                                         .putExpressionValue(valueRef(attributeName), value);
        if (useToken) {
            builder.path(keyRef(attributeName));
            builder.putExpressionName(keyRef(attributeName), attributeName);
        }
        return builder.build();
    }

    private static AddUpdateAction addAction(String attributeName, AttributeValue value, boolean useToken) {
        AddUpdateAction.Builder builder = AddUpdateAction.builder()
                                                               .path(attributeName)
                                                               .value(valueRef(attributeName))
                                                               .putExpressionValue(valueRef(attributeName), value);
        if (useToken) {
            builder.path(keyRef(attributeName));
            builder.putExpressionName(keyRef(attributeName), attributeName);
        }
        return builder.build();
    }

    private UpdateExpression updateExpression(UpdateAction... actions) {
        return UpdateExpression.builder().actions(actions).build();
    }

    private AttributeValue string(String s) {
        return AttributeValue.builder().s(s).build();
    }

    private static String keyRef(String key) {
        return KEY_TOKEN + key;
    }

    private static String valueRef(String value) {
        return VALUE_TOKEN + value;
    }
}