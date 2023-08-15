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

package software.amazon.awssdk.policybuilder.iam;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class IamConditionTest {
    private static final IamCondition FULL_CONDITION =
        IamCondition.builder()
                    .operator("Operator")
                    .key("Key")
                    .value("Value") // TODO: Id?
                    .build();

    @Test
    public void constructorsWork() {
        assertThat(IamCondition.create("Operator", "Key", "Value")).isEqualTo(FULL_CONDITION);

        assertThat(IamCondition.create(IamConditionOperator.create("Operator"),
                                       IamConditionKey.create("Key"),
                                       "Value"))
            .isEqualTo(FULL_CONDITION);

        assertThat(IamCondition.create(IamConditionOperator.create("Operator"),
                                       "Key",
                                       "Value"))
            .isEqualTo(FULL_CONDITION);

        assertThat(IamCondition.createAll("Operator",
                                          "Key",
                                          asList("Value1", "Value2")))
            .containsExactly(IamCondition.create("Operator", "Key", "Value1"),
                             IamCondition.create("Operator", "Key", "Value2"));

        assertThat(IamCondition.createAll(IamConditionOperator.create("Operator"),
                                          "Key",
                                          asList("Value1", "Value2")))
            .containsExactly(IamCondition.create("Operator", "Key", "Value1"),
                             IamCondition.create("Operator", "Key", "Value2"));

        assertThat(IamCondition.createAll(IamConditionOperator.create("Operator"),
                                          IamConditionKey.create("Key"),
                                          asList("Value1", "Value2")))
            .containsExactly(IamCondition.create("Operator", "Key", "Value1"),
                             IamCondition.create("Operator", "Key", "Value2"));
    }

    @Test
    public void simpleGettersSettersWork() {
        assertThat(FULL_CONDITION.operator().value()).isEqualTo("Operator");
        assertThat(FULL_CONDITION.key().value()).isEqualTo("Key");
        assertThat(FULL_CONDITION.value()).isEqualTo("Value");
    }

    @Test
    public void toBuilderPreservesValues() {
        assertThat(FULL_CONDITION.toBuilder().build()).isEqualTo(FULL_CONDITION);
    }

    @Test
    public void operatorSettersWork() {
        assertThat(condition(c -> c.operator("Operator")).operator().value()).isEqualTo("Operator");
        assertThat(condition(c -> c.operator(IamConditionOperator.create("Operator"))).operator().value()).isEqualTo("Operator");
    }

    @Test
    public void keySettersWork() {
        assertThat(condition(c -> c.key("Key")).key().value()).isEqualTo("Key");
        assertThat(condition(c -> c.key(IamConditionKey.create("Key"))).key().value()).isEqualTo("Key");
    }

    public IamCondition condition(Consumer<IamCondition.Builder> condition) {
        return IamCondition.builder()
                           .operator("Operator")
                           .key("Key")
                           .value("Value")
                           .applyMutation(condition)
                           .build();
    }
}