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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.valueRef;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.w3c.dom.Attr;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.AddUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.DeleteUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.RemoveUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.SetUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionTest {

    @Test
    public void build_minimal() {
        UpdateExpression expression1 = UpdateExpression.builder().build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    @Test
    public void create_attributesWithinSetAreUnique_ok() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addAction(addAction("attr4", numeric("4")))
                                                       .addAction(addAction("attr5", numeric("5")))
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertThat(resultExpression.expression()).contains("ADD");
        assertThat(resultExpression.expressionNames()).hasSize(7);
        assertThat(resultExpression.expressionNames()).containsValues("attr1", "attr2", "attr3", "attr4");
        assertThat(resultExpression.expressionValues()).hasSize(3);
    }

    @Test
    public void create_attributesWithinSetAreNotUnique_error() {
        assertThatThrownBy(() -> UpdateExpression.builder()
                                   .addRemoveAction(removeAction("attr1"))
                                   .addRemoveAction(removeAction("attr1"))
                                   .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression names");
    }

    @Test
    public void create_attributesBetweenActionsAreUnique_ok() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(removeAction("attr1"))
                                                       .addSetAction(setAction("attr2", string("3")))
                                                       .addDeleteAction(deleteAction("attr3", string("3")))
                                                       .addAction(addAction("attr4", numeric("4")))
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertThat(resultExpression.expression()).contains("REMOVE", "SET", "DELETE", "ADD");
        assertThat(resultExpression.expressionNames()).hasSize(7);
        assertThat(resultExpression.expressionNames()).containsValues("attr1", "attr2", "attr3", "attr4");
        assertThat(resultExpression.expressionValues()).hasSize(3);
    }

    @Test
    public void create_attributesBetweenActionsAreNotUnique_error() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(RemoveUpdateAction.remove("attr1"))
                                                       .addSetAction(SetUpdateAction.setValue("attr2",
                                                                                              AttributeValue.builder().n("3").build(),
                                                                                              UpdateBehavior.WRITE_IF_NOT_EXISTS))
                                                       .addDeleteAction(DeleteUpdateAction.builder().build())
                                                       .addAction(AddUpdateAction.builder().build())
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    @Test
    public void merge_attributesWithinSetAreUnique_ok() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(RemoveUpdateAction.remove("attr1"))
                                                       .addSetAction(SetUpdateAction.setValue("attr2",
                                                                                              AttributeValue.builder().n("3").build(),
                                                                                              UpdateBehavior.WRITE_IF_NOT_EXISTS))
                                                       .addDeleteAction(DeleteUpdateAction.builder().build())
                                                       .addAction(AddUpdateAction.builder().build())
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    @Test
    public void merge_attributesWithinSetAreNotUnique_error() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(RemoveUpdateAction.remove("attr1"))
                                                       .addSetAction(SetUpdateAction.setValue("attr2",
                                                                                              AttributeValue.builder().n("3").build(),
                                                                                              UpdateBehavior.WRITE_IF_NOT_EXISTS))
                                                       .addDeleteAction(DeleteUpdateAction.builder().build())
                                                       .addAction(AddUpdateAction.builder().build())
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    @Test
    public void merge_attributesBetweenActionsAreUnique_ok() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(RemoveUpdateAction.remove("attr1"))
                                                       .addSetAction(SetUpdateAction.setValue("attr2",
                                                                                              AttributeValue.builder().n("3").build(),
                                                                                              UpdateBehavior.WRITE_IF_NOT_EXISTS))
                                                       .addDeleteAction(DeleteUpdateAction.builder().build())
                                                       .addAction(AddUpdateAction.builder().build())
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    @Test
    public void merge_attributesBetweenActionsAreNotUnique_error() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(RemoveUpdateAction.remove("attr1"))
                                                       .addSetAction(SetUpdateAction.setValue("attr2",
                                                                                              AttributeValue.builder().n("3").build(),
                                                                                              UpdateBehavior.WRITE_IF_NOT_EXISTS))
                                                       .addDeleteAction(DeleteUpdateAction.builder().build())
                                                       .addAction(AddUpdateAction.builder().build())
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    @Test
    public void build_maximal() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addRemoveAction(RemoveUpdateAction.remove("attr1"))
                                                       .addSetAction(SetUpdateAction.setValue("attr2",
                                                                                              AttributeValue.builder().n("3").build(),
                                                                                              UpdateBehavior.WRITE_IF_NOT_EXISTS))
                                                       .addDeleteAction(DeleteUpdateAction.builder().build())
                                                       .addAction(AddUpdateAction.builder().build())
                                                       .build();
        Expression resultExpression = expression1.toExpression();
        assertEquals("", resultExpression.expression());
        assertTrue(resultExpression.expressionNames().isEmpty());
        assertTrue(resultExpression.expressionValues().isEmpty());
    }

    private RemoveUpdateAction removeAction(String attr) {
        return RemoveUpdateAction.builder()
                                 .actionExpression("expr(" + attr + ")")
                                 .expressionNames(Collections.singletonMap(keyRef(attr), attr))
                                 .build();
    }

    private SetUpdateAction setAction(String attr, AttributeValue value) {
        String valueName = "set_valueName";
        return SetUpdateAction.builder()
                              .actionExpression("expr(" + attr + ")")
                              .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                              .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                              .build();
    }

    private AddUpdateAction addAction(String attr, AttributeValue value) {
        String valueName = "add_valueName";
        return AddUpdateAction.builder()
                              .actionExpression("expr(" + attr + ")")
                              .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                              .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                              .build();
    }

    private DeleteUpdateAction deleteAction(String attr, AttributeValue value) {
        String valueName = "delete_valueName";
        return DeleteUpdateAction.builder()
                              .actionExpression("expr(" + attr + ")")
                              .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                              .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                              .build();
    }

    private AttributeValue numeric(String n) {
        return AttributeValue.builder().n(n).build();
    }

    private AttributeValue string(String s) {
        return AttributeValue.builder().s(s).build();
    }
}