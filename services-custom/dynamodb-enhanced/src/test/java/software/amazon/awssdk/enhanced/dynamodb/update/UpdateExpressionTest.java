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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class UpdateExpressionTest {

    private static final AttributeValue VAL = AttributeValue.builder().n("5").build();

    private static final RemoveUpdateAction removeAction = RemoveUpdateAction.builder().path("").build();
    private static final SetUpdateAction setAction = SetUpdateAction.builder()
                                                                    .path("")
                                                                    .value("")
                                                                    .putExpressionValue("", VAL)
                                                                    .build();
    private static final DeleteUpdateAction deleteAction = DeleteUpdateAction.builder()
                                                                             .path("")
                                                                             .value("")
                                                                             .putExpressionValue("", VAL)
                                                                             .build();
    private static final AddUpdateAction addAction = AddUpdateAction.builder()
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
        updateExpression.mergeExpression(null);
        assertThat(updateExpression.removeActions()).containsExactly(removeAction);
        assertThat(updateExpression.setActions()).containsExactly(setAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction);
    }

    @Test
    void merge_empty_expression() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();
        updateExpression.mergeExpression(UpdateExpression.builder().build());
        assertThat(updateExpression.removeActions()).containsExactly(removeAction);
        assertThat(updateExpression.setActions()).containsExactly(setAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction);
    }

    @Test
    void merge_expression_with_one_action_type() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();

        RemoveUpdateAction extraRemoveAction = RemoveUpdateAction.builder().path("a").build();
        UpdateExpression additionalExpression = UpdateExpression.builder()
                                                                .addAction(extraRemoveAction)
                                                                .build();
        updateExpression.mergeExpression(additionalExpression);
        assertThat(updateExpression.removeActions()).containsExactly(removeAction, extraRemoveAction);
        assertThat(updateExpression.setActions()).containsExactly(setAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction);
    }

    @Test
    void merge_expression_with_all_action_types() {
        UpdateExpression updateExpression = UpdateExpression.builder()
                                                            .actions(removeAction, setAction, deleteAction, addAction)
                                                            .build();

        RemoveUpdateAction extraRemoveAction = RemoveUpdateAction.builder().path("a").build();
        SetUpdateAction extraSetAction = SetUpdateAction.builder().path("").value("").putExpressionValue("", VAL).build();
        DeleteUpdateAction extraDeleteAction = DeleteUpdateAction.builder().path("").value("").putExpressionValue("", VAL).build();
        AddUpdateAction extraAddAction = AddUpdateAction.builder().path("").value("").putExpressionValue("", VAL).build();
        UpdateExpression additionalExpression = UpdateExpression.builder()
                                                                .actions(extraRemoveAction, extraSetAction, extraDeleteAction, extraAddAction)
                                                                .build();
        updateExpression.mergeExpression(additionalExpression);
        assertThat(updateExpression.removeActions()).containsExactly(removeAction, extraRemoveAction);
        assertThat(updateExpression.setActions()).containsExactly(setAction, extraSetAction);
        assertThat(updateExpression.deleteActions()).containsExactly(deleteAction, extraDeleteAction);
        assertThat(updateExpression.addActions()).containsExactly(addAction, extraAddAction);
    }

    private static final class UnknownUpdateAction implements UpdateAction {

    }
}