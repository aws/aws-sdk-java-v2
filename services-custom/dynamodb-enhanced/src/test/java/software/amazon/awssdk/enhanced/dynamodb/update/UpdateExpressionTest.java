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

import java.util.Arrays;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class UpdateExpressionTest {

    private static final AttributeValue VAL = AttributeValue.builder().n("5").build();

    private static final RemoveAction removeAction = RemoveAction.builder().path("").build();
    private static final SetAction setAction = SetAction.builder()
                                                        .path("")
                                                        .value("")
                                                        .putExpressionValue("", VAL)
                                                        .build();
    private static final DeleteAction deleteAction = DeleteAction.builder()
                                                                 .path("")
                                                                 .value("")
                                                                 .putExpressionValue("", VAL)
                                                                 .build();
    private static final AddAction addAction = AddAction.builder()
                                                        .path("")
                                                        .value("")
                                                        .putExpressionValue("", VAL)
                                                        .build();

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(UpdateExpression.class)
                      .withPrefabValues(AttributeValue.class,
                                        AttributeValue.builder().s("1").build(),
                                        AttributeValue.builder().s("2").build())
                      .verify();
    }

    @Test
    void build_minimal() {
        UpdateExpression updateExpression = UpdateExpression.builder().build();
        assertThat(updateExpression.removeActions()).isEmpty();
        assertThat(updateExpression.setActions()).isEmpty();
        assertThat(updateExpression.deleteActions()).isEmpty();
        assertThat(updateExpression.addActions()).isEmpty();
    }

    @Test
    void build_maximal_single() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .addAction(removeAction)
                                                            .addAction(setAction)
                                                            .addAction(deleteAction)
                                                            .addAction(addAction)
                                                            .build();
        assertThat(updateExpression.removeActions()).containsExactly(removeAction);
        assertThat(updateExpression.setActions()).containsExactly(setAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction);
    }

    @Test
    void build_maximal_plural() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();

        assertThat(updateExpression.removeActions()).containsExactly(removeAction);
        assertThat(updateExpression.setActions()).containsExactly(setAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction);
    }

    @Test
    void build_plural_is_not_additive() {
        List<RemoveAction> removeActions = Arrays.asList(removeAction, removeAction);
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeActions)
                                                            .actions(setAction, deleteAction, addAction)
                                                            .build();

        assertThat(updateExpression.removeActions()).isEmpty();
        assertThat(updateExpression.setActions()).containsExactly(setAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction);
    }

    @Test
    void unknown_action_generates_error() {
        assertThatThrownBy(() -> UpdateExpression.builder().actions(new UnknownUpdateAction()).build())
            .hasMessageContaining("Do not recognize UpdateAction")
            .hasMessageContaining("UnknownUpdateAction");
    }

    @Test
    void merge_null_expression() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();
        UpdateExpression result = UpdateExpression.mergeExpressions(updateExpression, null);
        assertThat(result.removeActions()).containsExactly(removeAction);
        assertThat(result.setActions()).containsExactly(setAction);
        assertThat(result.deleteActions()).containsExactly(deleteAction);
        assertThat(result.addActions()).containsExactly(addAction);
    }

    @Test
    void merge_empty_expression() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();
        UpdateExpression result = UpdateExpression.mergeExpressions(updateExpression, UpdateExpression.builder().build());
        assertThat(result.removeActions()).containsExactly(removeAction);
        assertThat(result.setActions()).containsExactly(setAction);
        assertThat(result.deleteActions()).containsExactly(deleteAction);
        assertThat(result.addActions()).containsExactly(addAction);
    }

    @Test
    void merge_expression_with_one_action_type() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();

        RemoveAction extraRemoveAction = RemoveAction.builder().path("a").build();
        UpdateExpression additionalExpression = UpdateExpression.builder()
                                                                .addAction(extraRemoveAction)
                                                                .build();
        UpdateExpression result = UpdateExpression.mergeExpressions(updateExpression, additionalExpression);
        assertThat(result.removeActions()).containsExactly(removeAction, extraRemoveAction);
        assertThat(result.setActions()).containsExactly(setAction);
        assertThat(result.deleteActions()).containsExactly(deleteAction);
        assertThat(result.addActions()).containsExactly(addAction);
    }

    @Test
    void merge_expression_with_all_action_types() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();

        RemoveAction extraRemoveAction = RemoveAction.builder().path("a").build();
        SetAction extraSetAction = SetAction.builder().path("").value("").putExpressionValue("", VAL).build();
        DeleteAction extraDeleteAction = DeleteAction.builder().path("").value("").putExpressionValue("", VAL).build();
        AddAction extraAddAction = AddAction.builder().path("").value("").putExpressionValue("", VAL).build();
        UpdateExpression additionalExpression = UpdateExpression.builder()
                                                                .actions(extraRemoveAction, extraSetAction, extraDeleteAction, extraAddAction)
                                                                .build();
        UpdateExpression result = UpdateExpression.mergeExpressions(updateExpression, additionalExpression);
        assertThat(result.removeActions()).containsExactly(removeAction, extraRemoveAction);
        assertThat(result.setActions()).containsExactly(setAction, extraSetAction);
        assertThat(result.deleteActions()).containsExactly(deleteAction, extraDeleteAction);
        assertThat(result.addActions()).containsExactly(addAction, extraAddAction);
    }

    private static final class UnknownUpdateAction implements UpdateAction {

    }
}