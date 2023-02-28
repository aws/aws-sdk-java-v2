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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import software.amazon.awssdk.enhanced.dynamodb.Expression;

import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.StringUtils;

/**
 * When converting, SetAction, DeleteAction and AddAction work similarly. Advanced test cases thus only need to test
 * using one of these types of action. RemoveAction, lacking expression values, work slightly differently.
 *
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class UpdateExpressionConverterTest {

    private static final String KEY_TOKEN = "#PRE_";
    private static final String VALUE_TOKEN = ":PRE_";

    @Test
    void convert_emptyExpression() {
        UpdateExpression updateExpression = UpdateExpression.builder().build();
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        assertThat(expression).isNull();
    }

    @Test
    void convert_removeAction_single() {
        UpdateExpression updateExpression = createUpdateExpression(removeAction("attribute1", null));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        assertThat(expression.expression()).isEqualTo("REMOVE attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_removeActions() {
        UpdateExpression updateExpression = createUpdateExpression(removeAction("attribute1", null),
                                                                   removeAction("attribute2", null));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        assertThat(expression.expression()).isEqualTo("REMOVE attribute1, attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_removeActions_uniqueNameTokens() {
        UpdateExpression updateExpression = createUpdateExpression(removeAction("attribute1", KEY_TOKEN),
                                                                   removeAction("attribute2", KEY_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, String> expectedExpressionNames = new HashMap<>();
        expectedExpressionNames.put("#PRE_attribute1", "attribute1");
        expectedExpressionNames.put("#PRE_attribute2", "attribute2");

        assertThat(expression.expression()).isEqualTo("REMOVE #PRE_attribute1, #PRE_attribute2");
        assertThat(expression.expressionNames()).containsExactlyEntriesOf(expectedExpressionNames);
        assertThat(expression.expressionValues()).isEmpty();
    }

    /**
     * Attributes with the same name are simply added to the list. There is no check to compare the contents
     * of two remove action paths.
     *
     * This would fail when calling DDB; note that UpdateItemOperation always prefixes attribute names and the probability of
     * this use case is low.
     */
    @Test
    void convert_removeActions_duplicateAttributes_createsDdbFailExpression() {
        UpdateExpression updateExpression = createUpdateExpression(removeAction("attribute1", null),
                                                                   removeAction("attribute1", null));

        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("REMOVE attribute1, attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).isEmpty();
    }

    /**
     * The joining logic in {@link Expression#joinNames(Map, Map)} will be merge the two entries, since here the
     * same key will point at the same value.
     *
     * This would fail when calling DDB; note that UpdateItemOperation always prefixes attribute names and the probability of
     * this use case is low.
     */
    @Test
    void convert_removeActions_duplicateAttributes_uniqueNameTokens_createsDdbFailExpression() {
        UpdateExpression updateExpression = createUpdateExpression(removeAction("attribute1", KEY_TOKEN),
                                                                   removeAction("attribute1", KEY_TOKEN));

        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("REMOVE #PRE_attribute1, #PRE_attribute1");
        assertThat(expression.expressionNames()).hasSize(1);
        assertThat(expression.expressionValues()).isEmpty();
    }

    /**
     * This is the same case as using a name prefix with the same attribute name, i.e. `duplicateAttributes_uniqueNameTokens`
     */
    @Test
    void convert_removeActions_duplicateAttributes_duplicateNameTokens_createsDdbFailExpression() {
        UpdateExpression updateExpression = createUpdateExpression(
            RemoveAction.builder().path("attribute_ref").putExpressionName("attribute_ref", "attribute1").build(),
            RemoveAction.builder().path("attribute_ref").putExpressionName("attribute_ref", "attribute1").build());

        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("REMOVE attribute_ref, attribute_ref");
        assertThat(expression.expressionNames()).hasSize(1);
        assertThat(expression.expressionValues()).isEmpty();
    }

    @Test
    void convert_removeActions_uniqueAttributes_duplicateNameTokens_error() {
        UpdateExpression updateExpression = createUpdateExpression(
            RemoveAction.builder().path("attribute_ref").putExpressionName("attribute_ref", "attribute1").build(),
            RemoveAction.builder().path("attribute_ref").putExpressionName("attribute_ref", "attribute2").build());

        assertThatThrownBy(() -> UpdateExpressionConverter.toExpression(updateExpression))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression names")
            .hasMessageContaining("attribute_ref");
    }

    @Test
    void convert_setAction_single() {
        UpdateExpression updateExpression = createUpdateExpression(setAction("attribute1", string("val1"), null, VALUE_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("SET attribute1 = :PRE_attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry(":PRE_attribute1", string("val1"));
    }

    @Test
    void convert_setActions() {
        UpdateExpression updateExpression = createUpdateExpression(setAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   setAction("attribute2", string("val2"), null, VALUE_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("SET attribute1 = :PRE_attribute1, attribute2 = :PRE_attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_setActions_uniqueNameTokens() {
        UpdateExpression updateExpression = createUpdateExpression(setAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   setAction("attribute2", string("val2"), KEY_TOKEN, VALUE_TOKEN));
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
    void convert_deleteAction_single() {
        UpdateExpression updateExpression = createUpdateExpression(deleteAction("attribute1", string("val1"), null, VALUE_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("DELETE attribute1 :PRE_attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry(":PRE_attribute1", string("val1"));
    }

    @Test
    void convert_deleteActions() {
        UpdateExpression updateExpression = createUpdateExpression(deleteAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute2", string("val2"), null, VALUE_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("DELETE attribute1 :PRE_attribute1, attribute2 :PRE_attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_deleteActions_uniqueNameTokens() {
        UpdateExpression updateExpression = createUpdateExpression(deleteAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   deleteAction("attribute2", string("val2"), KEY_TOKEN, VALUE_TOKEN));
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

    /**
     * The joining logic in {@link Expression#joinValues(Map, Map)} will be throw an error, because the same key
     * is pointing to different values. Actions that use expression value maps (Add, Delete, Set) exhibit the same behavior.
     */
    @Test
    void convert_deleteActions_duplicateAttributes_error() {
        UpdateExpression updateExpression = createUpdateExpression(deleteAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute1", string("val2"), null, VALUE_TOKEN));

        assertThatThrownBy(() -> UpdateExpressionConverter.toExpression(updateExpression))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(":PRE_attribute1");
    }

    /**
     * The joining logic in {@link Expression#joinValues(Map, Map)} will be throw an error, because the same key
     * is pointing to different values. Actions that use expression value maps (Add, Delete, Set) exhibit the same behavior.
     */
    @Test
    void convert_deleteActions_duplicateAttributes_uniqueNameTokens_error() {
        UpdateExpression updateExpression = createUpdateExpression(deleteAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   deleteAction("attribute1", string("val2"), KEY_TOKEN, VALUE_TOKEN));

        assertThatThrownBy(() -> UpdateExpressionConverter.toExpression(updateExpression))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(":PRE_attribute1");
    }

    /**
     * The joining logic in {@link Expression#joinValues(Map, Map)} will be merge the two entries, since here the
     * same key will point at the same value.
     *
     * This would fail when calling DDB; note that UpdateItemOperation always prefixes attribute names and the probability of
     * this use case is low.
     */
    @Test
    void convert_deleteActions_duplicateAttributes_duplicateValueTokens_createsDdbFailExpression() {
        UpdateExpression updateExpression = createUpdateExpression(
            DeleteAction.builder().path("attribute1").value("attribute_ref").putExpressionValue("attribute_ref", string("val1")).build(),
            DeleteAction.builder().path("attribute1").value("attribute_ref").putExpressionValue("attribute_ref", string("val1")).build());

        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("DELETE attribute1 attribute_ref, attribute1 attribute_ref");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry("attribute_ref", string("val1"));
    }

    @Test
    void convert_deleteActions_uniqueAttributes_duplicateValueTokens_error() {
        UpdateExpression updateExpression = createUpdateExpression(
            DeleteAction.builder().path("attribute1").value("attribute_ref").putExpressionValue("attribute_ref", string("val1")).build(),
            DeleteAction.builder().path("attribute2").value("attribute_ref").putExpressionValue("attribute_ref", string("val2")).build());

        assertThatThrownBy(() -> UpdateExpressionConverter.toExpression(updateExpression))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining("attribute_ref");
    }

    @Test
    void convert_addAction_single() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), null, VALUE_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);
        assertThat(expression.expression()).isEqualTo("ADD attribute1 :PRE_attribute1");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsEntry(":PRE_attribute1", string("val1"));
    }

    @Test
    void convert_addActions() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   addAction("attribute2", string("val2"), null, VALUE_TOKEN));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        Map<String, AttributeValue> expectedExpressionValues = new HashMap<>();
        expectedExpressionValues.put(":PRE_attribute1", string("val1"));
        expectedExpressionValues.put(":PRE_attribute2", string("val2"));

        assertThat(expression.expression()).isEqualTo("ADD attribute1 :PRE_attribute1, attribute2 :PRE_attribute2");
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).containsExactlyEntriesOf(expectedExpressionValues);
    }

    @Test
    void convert_addActions_uniqueNameTokens() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   addAction("attribute2", string("val2"), KEY_TOKEN, VALUE_TOKEN));
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
    void convert_mixedActions() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute2", string("val2"), null, VALUE_TOKEN),
                                                                   removeAction("attribute3", null),
                                                                   setAction("attribute4", string("val4"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute5", string("val5"), null, VALUE_TOKEN),
                                                                   setAction("attribute6", string("val6"), null, VALUE_TOKEN),
                                                                   removeAction("attribute7", null),
                                                                   addAction("attribute8", string("val8"), null, VALUE_TOKEN),
                                                                   removeAction("attribute9", null));
        Expression expression = UpdateExpressionConverter.toExpression(updateExpression);

        assertThat(expression.expression()).isEqualTo(
            "SET attribute4 = :PRE_attribute4, attribute6 = :PRE_attribute6 " +
            "REMOVE attribute3, attribute7, attribute9 " +
            "DELETE attribute2 :PRE_attribute2, attribute5 :PRE_attribute5 " +
            "ADD attribute1 :PRE_attribute1, attribute8 :PRE_attribute8"
        );
        assertThat(expression.expressionNames()).isEmpty();
        assertThat(expression.expressionValues()).hasSize(6);
    }

    @Test
    void convert_mixedActions_uniqueNameTokens() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   deleteAction("attribute2", string("val2"), KEY_TOKEN, VALUE_TOKEN),
                                                                   removeAction("attribute3", KEY_TOKEN),
                                                                   setAction("attribute4", string("val4"), KEY_TOKEN, VALUE_TOKEN),
                                                                   deleteAction("attribute5", string("val5"), KEY_TOKEN, VALUE_TOKEN),
                                                                   setAction("attribute6", string("val6"), KEY_TOKEN, VALUE_TOKEN),
                                                                   removeAction("attribute7", KEY_TOKEN),
                                                                   addAction("attribute8", string("val8"), KEY_TOKEN, VALUE_TOKEN),
                                                                   removeAction("attribute9", KEY_TOKEN));
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
    void convert_mixedActions_duplicateAttributes_error() {
        UpdateExpression updateExpression = createUpdateExpression(deleteAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   addAction("attribute1", string("val2"), KEY_TOKEN, VALUE_TOKEN));
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
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute2", string("val2"), null, VALUE_TOKEN),
                                                                   removeAction("attribute3", null),
                                                                   setAction("attribute4", string("val4"), null, VALUE_TOKEN));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_noComposedNames_tokens() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   deleteAction("attribute2", string("val2"), KEY_TOKEN, VALUE_TOKEN),
                                                                   removeAction("attribute3", KEY_TOKEN),
                                                                   setAction("attribute4", string("val4"), KEY_TOKEN, VALUE_TOKEN));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_noComposedNames_duplicates() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute1", string("val2"), KEY_TOKEN, VALUE_TOKEN));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute1");
    }

    @Test
    void findAttributeNames_composedNames_noTokens() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1.#nested", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute2.nested", string("val2"), null, VALUE_TOKEN),
                                                                   removeAction("attribute3[1]", null),
                                                                   setAction("attribute4[1].nested", string("val4"), null, VALUE_TOKEN));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_composedNames_tokens() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1.nested[1]", string("val1"), KEY_TOKEN, VALUE_TOKEN),
                                                                   deleteAction("attribute2.nested", string("val2"), KEY_TOKEN, VALUE_TOKEN),
                                                                   removeAction("attribute3[1]", KEY_TOKEN),
                                                                   setAction("attribute4[1].nested", string("val4"), KEY_TOKEN, VALUE_TOKEN));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute2", "attribute3", "attribute4");
    }

    @Test
    void findAttributeNames_composedNames_duplicates() {
        UpdateExpression updateExpression = createUpdateExpression(addAction("attribute1[1]", string("val1"), null, VALUE_TOKEN),
                                                                   deleteAction("attribute1.nested", string("val2"), KEY_TOKEN, VALUE_TOKEN));
        List<String> attributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
        assertThat(attributes).containsExactlyInAnyOrder("attribute1", "attribute1");
    }

    private static RemoveAction removeAction(String attributeName, String keyToken) {
        RemoveAction.Builder builder = RemoveAction.builder()
                                                   .path(attributeName);
        if (!StringUtils.isEmpty(keyToken)) {
            builder.path(keyRef(attributeName, keyToken));
            builder.putExpressionName(keyRef(attributeName, keyToken), attributeName);
        }
        return builder.build();
    }

    private static SetAction setAction(String attributeName, AttributeValue value, String keyToken, String valueToken) {
        SetAction.Builder builder = SetAction.builder()
                                             .path(attributeName)
                                             .value(valueRef(attributeName, valueToken))
                                             .putExpressionValue(valueRef(attributeName, valueToken), value);
        if (!StringUtils.isEmpty(keyToken)) {
            builder.path(keyRef(attributeName, keyToken));
            builder.putExpressionName(keyRef(attributeName, keyToken), attributeName);
        }
        return builder.build();
    }

    private static DeleteAction deleteAction(String attributeName, AttributeValue value, String keyToken, String valueToken) {
        DeleteAction.Builder builder = DeleteAction.builder()
                                                   .path(attributeName)
                                                   .value(valueRef(attributeName, valueToken))
                                                   .putExpressionValue(valueRef(attributeName, valueToken), value);
        if (!StringUtils.isEmpty(keyToken)) {
            builder.path(keyRef(attributeName, keyToken));
            builder.putExpressionName(keyRef(attributeName, keyToken), attributeName);
        }
        return builder.build();
    }

    private static AddAction addAction(String attributeName, AttributeValue value, String keyToken, String valueToken) {
        AddAction.Builder builder = AddAction.builder()
                                             .path(attributeName)
                                             .value(valueRef(attributeName, valueToken))
                                             .putExpressionValue(valueRef(attributeName, valueToken), value);
        if (!StringUtils.isEmpty(keyToken)) {
            builder.path(keyRef(attributeName, keyToken));
            builder.putExpressionName(keyRef(attributeName, keyToken), attributeName);
        }
        return builder.build();
    }

    private UpdateExpression createUpdateExpression(UpdateAction... actions) {
        return UpdateExpression.builder().actions(actions).build();
    }

    private AttributeValue string(String s) {
        return AttributeValue.builder().s(s).build();
    }

    private static String keyRef(String key, String token) {
        return token + key;
    }

    private static String valueRef(String value, String valueToken) {
        return valueToken + value;
    }
}