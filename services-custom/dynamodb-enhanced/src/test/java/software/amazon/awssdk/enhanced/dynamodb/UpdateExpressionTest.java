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
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.valueRef;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.AddUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.DeleteUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.RemoveUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.SetUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateActionType;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class UpdateExpressionTest {

    @Test
    public void build_minimal() {
        UpdateExpression expression = UpdateExpression.builder().build();
        Expression resultExpression = expression.toExpression();
        assertThat(resultExpression.expression()).isEqualTo("");
        assertThat(resultExpression.expressionNames()).isEmpty();
        assertThat(resultExpression.expressionValues()).isEmpty();
    }

    @Test
    public void create_attributesWithinActionAreUnique_ok() {
        UpdateExpression expression = UpdateExpression.builder()
                                                      .addAction(addAction("attr1", numeric("4")))
                                                      .addAction(addAction("attr2", numeric("5")))
                                                      .build();
        assertThat(expression.updateActionFor("attr1")).isPresent();
        assertThat(expression.updateActionFor("attr2")).isPresent();
        Expression resultExpression = expression.toExpression();
        assertThat(resultExpression.expression()).contains("ADD");
        assertThat(resultExpression.expression()).doesNotContain("SET", "REMOVE", "DELETE");
    }

    @Test
    public void create_attributesWithinActionAreNotUnique_error() {
        assertThatThrownBy(() -> UpdateExpression.builder()
                                                 .addAction(removeAction("attr1"))
                                                 .addAction(removeAction("attr1"))
                                                 .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicate")
            .hasMessageContaining("attr1");
    }

    @Test
    public void create_attributesBetweenActionsAreUnique_ok() {
        UpdateExpression expression = UpdateExpression.builder()
                                                      .addAction(removeAction("attr1"))
                                                      .addAction(setAction("attr2", string("2")))
                                                      .addAction(deleteAction("attr3", string("3")))
                                                      .addAction(addAction("attr4", numeric("4")))
                                                      .build();
        assertThat(expression.updateActionFor("attr1")).isPresent();
        assertThat(expression.updateActionFor("attr2")).isPresent();
        assertThat(expression.updateActionFor("attr3")).isPresent();
        assertThat(expression.updateActionFor("attr4")).isPresent();
        Expression resultExpression = expression.toExpression();
        assertThat(resultExpression.expression()).contains("REMOVE", "SET", "DELETE", "ADD");
    }

    @Test
    public void create_attributesBetweenActionsAreNotUnique_error() {
        assertThatThrownBy(() -> UpdateExpression.builder()
                                                 .addAction(removeAction("attr1"))
                                                 .addAction(deleteAction("attr1", string("3")))
                                                 .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicate")
            .hasMessageContaining("attr1");
    }

    @Test
    public void merge_uniqueAttributes_ok() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                      .addAction(removeAction("attr1"))
                                                      .addAction(setAction("attr2", string("2")))
                                                      .addAction(deleteAction("attr3", string("3")))
                                                      .addAction(addAction("attr4", numeric("4")))
                                                      .build();
        UpdateExpression expression2 = UpdateExpression.builder()
                                                       .addAction(removeAction("attrA"))
                                                       .addAction(setAction("attrB", string("2")))
                                                       .addAction(deleteAction("attrC", string("3")))
                                                       .addAction(addAction("attrD", numeric("4")))
                                                       .build();
        UpdateExpression expression = UpdateExpression.mergeExpressions(expression1, expression2);
        assertThat(expression.updateActionFor("attr1")).isPresent();
        assertThat(expression.updateActionFor("attr2")).isPresent();
        assertThat(expression.updateActionFor("attr3")).isPresent();
        assertThat(expression.updateActionFor("attr4")).isPresent();
        assertThat(expression.updateActionFor("attrA")).isPresent();
        assertThat(expression.updateActionFor("attrB")).isPresent();
        assertThat(expression.updateActionFor("attrC")).isPresent();
        assertThat(expression.updateActionFor("attrD")).isPresent();
        Expression resultExpression = expression.toExpression();
        assertThat(resultExpression.expression()).contains("REMOVE", "SET", "DELETE", "ADD");
    }

    @Test
    public void merge_nonUniqueAttributes_error() {
        UpdateExpression expression1 = UpdateExpression.builder()
                                                       .addAction(removeAction("attr1"))
                                                       .build();
        UpdateExpression expression2 = UpdateExpression.builder()
                                                       .addAction(addAction("attr1", numeric("4")))
                                                       .build();
        assertThatThrownBy(() -> UpdateExpression.mergeExpressions(expression1, expression2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicate")
            .hasMessageContaining("attr1");
    }

    private static UpdateAction removeActionSimple(String attr) {
        return UpdateAction.builder()
                           .type(UpdateActionType.REMOVE)
                           .attributeName(attr)
                           .expression("expr(" + attr + ")")
                           .expressionNames(Collections.singletonMap(keyRef(attr), attr))
                           .build();
    }

    private static UpdateAction setActionSimple(String attr, AttributeValue value) {
        String valueName = attr + "_for_";
        return UpdateAction.builder()
                           .type(UpdateActionType.SET)
                           .attributeName(attr)
                           .expression("expr(" + attr + ")")
                           .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                           .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                           .build();
    }

    private static UpdateAction addActionSimple(String attr, AttributeValue value) {
        String valueName = attr + "_for_";
        return UpdateAction.builder()
                           .type(UpdateActionType.ADD)
                           .attributeName(attr)
                           .expression("expr(" + attr + ")")
                           .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                           .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                           .build();
    }

    private static UpdateAction deleteActionSimple(String attr, AttributeValue value) {
        String valueName = attr + "_for_";
        return UpdateAction.builder()
                           .type(UpdateActionType.DELETE)
                           .attributeName(attr)
                           .expression("expr(" + attr + ")")
                           .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                           .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                           .build();
    }

    private static UpdateAction removeAction(String attr) {
        return RemoveUpdateAction.builder()
                                 .attributeName(attr)
                                 .expression("expr(" + attr + ")")
                                 .expressionNames(Collections.singletonMap(keyRef(attr), attr))
                                 .build();
    }

    private static UpdateAction setAction(String attr, AttributeValue value) {
        String valueName = attr + "_for_";
        return SetUpdateAction.builder()
                              .attributeName(attr)
                              .expression("expr(" + attr + ")")
                              .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                              .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                              .build();
    }

    private static UpdateAction addAction(String attr, AttributeValue value) {
        String valueName = attr + "_for_";
        return AddUpdateAction.builder()
                              .attributeName(attr)
                              .expression("expr(" + attr + ")")
                              .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                              .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                              .build();
    }

    private static UpdateAction deleteAction(String attr, AttributeValue value) {
        String valueName = attr + "_for_";
        return DeleteUpdateAction.builder()
                                 .attributeName(attr)
                                 .expression("expr(" + attr + ")")
                                 .expressionNames(UpdateExpressionUtils.expressionNamesFor(attr, valueName))
                                 .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                                 .build();
    }

    private static AttributeValue numeric(String n) {
        return AttributeValue.builder().n(n).build();
    }

    private static AttributeValue string(String s) {
        return AttributeValue.builder().s(s).build();
    }
}