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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.policybuilder.iam.IamEffect.ALLOW;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class IamStatementTest {
    private static final IamPrincipal PRINCIPAL_1 = IamPrincipal.create("1", "*");
    private static final IamPrincipal PRINCIPAL_2 = IamPrincipal.create("2", "*");
    private static final IamResource RESOURCE_1 = IamResource.create("1");
    private static final IamResource RESOURCE_2 = IamResource.create("2");
    private static final IamAction ACTION_1 = IamAction.create("1");
    private static final IamAction ACTION_2 = IamAction.create("2");
    private static final IamCondition CONDITION_1 = IamCondition.create("1", "K", "V");
    private static final IamCondition CONDITION_2 = IamCondition.create("2", "K", "V");

    private static final IamStatement FULL_STATEMENT =
        IamStatement.builder()
                    .effect(ALLOW)
                    .sid("Sid")
                    .principals(singletonList(PRINCIPAL_1))
                    .notPrincipals(singletonList(PRINCIPAL_2))
                    .resources(singletonList(RESOURCE_1))
                    .notResources(singletonList(RESOURCE_2))
                    .actions(singletonList(ACTION_1))
                    .notActions(singletonList(ACTION_2))
                    .conditions(singletonList(CONDITION_1))
                    .build();

    @Test
    public void simpleGettersSettersWork() {
        assertThat(FULL_STATEMENT.sid()).isEqualTo("Sid");
        assertThat(FULL_STATEMENT.effect()).isEqualTo(ALLOW);
        assertThat(FULL_STATEMENT.principals()).containsExactly(PRINCIPAL_1);
        assertThat(FULL_STATEMENT.notPrincipals()).containsExactly(PRINCIPAL_2);
        assertThat(FULL_STATEMENT.resources()).containsExactly(RESOURCE_1);
        assertThat(FULL_STATEMENT.notResources()).containsExactly(RESOURCE_2);
        assertThat(FULL_STATEMENT.actions()).containsExactly(ACTION_1);
        assertThat(FULL_STATEMENT.notActions()).containsExactly(ACTION_2);
        assertThat(FULL_STATEMENT.conditions()).containsExactly(CONDITION_1);
    }

    @Test
    public void toBuilderPreservesValues() {
        assertThat(FULL_STATEMENT.toBuilder().build()).isEqualTo(FULL_STATEMENT);
    }

    @Test
    public void toStringIncludesAllValues() {
        assertThat(FULL_STATEMENT.toString())
            .isEqualTo("IamStatement("
                       + "sid=Sid, "
                       + "effect=IamEffect(value=Allow), "
                       + "principals=[IamPrincipal(type=1, id=*)], "
                       + "notPrincipals=[IamPrincipal(type=2, id=*)], "
                       + "actions=[IamAction(value=1)], "
                       + "notActions=[IamAction(value=2)], "
                       + "resources=[IamResource(value=1)], "
                       + "notResources=[IamResource(value=2)], "
                       + "conditions=[IamCondition(operator=1, key=K, value=V)])");
    }

    @Test
    public void effectIsRequired() {
        assertThatThrownBy(() -> IamStatement.builder().build()).hasMessageMatching(".*[Ee]ffect.*");
    }

    @Test
    public void effectGettersSettersWork() {
        assertThat(statement(s -> s.effect(ALLOW)).effect()).isEqualTo(ALLOW);
        assertThat(statement(s -> s.effect("Allow")).effect()).isEqualTo(ALLOW);
    }

    @Test
    public void principalGettersSettersWork() {
        assertThat(statement(s -> s.principals(asList(PRINCIPAL_1, PRINCIPAL_2))).principals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addPrincipal(PRINCIPAL_1)
                                   .addPrincipal(PRINCIPAL_2)).principals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addPrincipal(p -> p.type("1").id("*"))
                                   .addPrincipal(p -> p.type("2").id("*"))).principals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addPrincipal("1", "*")
                                   .addPrincipal("2", "*")).principals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addPrincipal(IamPrincipalType.create("1"), "*")
                                   .addPrincipal(IamPrincipalType.create("2"), "*")).principals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addPrincipals(IamPrincipalType.create("1"), asList("x", "y"))).principals())
            .containsExactly(IamPrincipal.create("1", "x"),
                             IamPrincipal.create("1", "y"));
        assertThat(statement(s -> s.addPrincipals("1", asList("x", "y"))).principals())
            .containsExactly(IamPrincipal.create("1", "x"),
                             IamPrincipal.create("1", "y"));
    }

    @Test
    public void principalsCollectionSettersResetsList() {
        assertThat(statement(s -> s.principals(asList(PRINCIPAL_1, PRINCIPAL_2))
                                   .principals(singletonList(PRINCIPAL_1))).principals())
            .containsExactly(PRINCIPAL_1);
    }

    @Test
    public void notPrincipalGettersSettersWork() {
        assertThat(statement(s -> s.notPrincipals(asList(PRINCIPAL_1, PRINCIPAL_2))).notPrincipals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addNotPrincipal(PRINCIPAL_1)
                                   .addNotPrincipal(PRINCIPAL_2)).notPrincipals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addNotPrincipal(p -> p.type("1").id("*"))
                                   .addNotPrincipal(p -> p.type("2").id("*"))).notPrincipals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addNotPrincipal("1", "*")
                                   .addNotPrincipal("2", "*")).notPrincipals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addNotPrincipal(IamPrincipalType.create("1"), "*")
                                   .addNotPrincipal(IamPrincipalType.create("2"), "*")).notPrincipals())
            .containsExactly(PRINCIPAL_1, PRINCIPAL_2);
        assertThat(statement(s -> s.addNotPrincipals(IamPrincipalType.create("1"), asList("x", "y"))).notPrincipals())
            .containsExactly(IamPrincipal.create("1", "x"),
                             IamPrincipal.create("1", "y"));
        assertThat(statement(s -> s.addNotPrincipals("1", asList("x", "y"))).notPrincipals())
            .containsExactly(IamPrincipal.create("1", "x"),
                             IamPrincipal.create("1", "y"));
    }

    @Test
    public void notPrincipalsCollectionSettersResetsList() {
        assertThat(statement(s -> s.notPrincipals(asList(PRINCIPAL_1, PRINCIPAL_2))
                                   .notPrincipals(singletonList(PRINCIPAL_1))).notPrincipals())
            .containsExactly(PRINCIPAL_1);
    }

    @Test
    public void actionGettersSettersWork() {
        assertThat(statement(s -> s.actions(asList(ACTION_1, ACTION_2))).actions())
            .containsExactly(ACTION_1, ACTION_2);
        assertThat(statement(s -> s.actionIds(asList("1", "2"))).actions())
            .containsExactly(ACTION_1, ACTION_2);
        assertThat(statement(s -> s.addAction(ACTION_1).addAction(ACTION_2)).actions())
            .containsExactly(ACTION_1, ACTION_2);
        assertThat(statement(s -> s.addAction("1").addAction("2")).actions())
            .containsExactly(ACTION_1, ACTION_2);
    }

    @Test
    public void actionCollectionSettersResetsList() {
        assertThat(statement(s -> s.actions(asList(ACTION_1, ACTION_2))
                                .actions(singletonList(ACTION_2))).actions())
            .containsExactly(ACTION_2);
    }

    @Test
    public void notActionGettersSettersWork() {
        assertThat(statement(s -> s.notActions(asList(ACTION_1, ACTION_2))).notActions())
            .containsExactly(ACTION_1, ACTION_2);
        assertThat(statement(s -> s.notActionIds(asList("1", "2"))).notActions())
            .containsExactly(ACTION_1, ACTION_2);
        assertThat(statement(s -> s.addNotAction(ACTION_1).addNotAction(ACTION_2)).notActions())
            .containsExactly(ACTION_1, ACTION_2);
        assertThat(statement(s -> s.addNotAction("1").addNotAction("2")).notActions())
            .containsExactly(ACTION_1, ACTION_2);
    }

    @Test
    public void notActionCollectionSettersResetsList() {
        assertThat(statement(s -> s.notActions(asList(ACTION_1, ACTION_2))
                                   .notActions(singletonList(ACTION_2))).notActions())
            .containsExactly(ACTION_2);
    }

    @Test
    public void resourceGettersSettersWork() {
        assertThat(statement(s -> s.resources(asList(RESOURCE_1, RESOURCE_2))).resources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
        assertThat(statement(s -> s.resourceIds(asList("1", "2"))).resources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
        assertThat(statement(s -> s.addResource(RESOURCE_1).addResource(RESOURCE_2)).resources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
        assertThat(statement(s -> s.addResource("1").addResource("2")).resources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
    }

    @Test
    public void resourceCollectionSettersResetsList() {
        assertThat(statement(s -> s.resources(asList(RESOURCE_1, RESOURCE_2))
                                   .resources(singletonList(RESOURCE_2))).resources())
            .containsExactly(RESOURCE_2);
    }

    @Test
    public void notResourceGettersSettersWork() {
        assertThat(statement(s -> s.notResources(asList(RESOURCE_1, RESOURCE_2))).notResources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
        assertThat(statement(s -> s.notResourceIds(asList("1", "2"))).notResources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
        assertThat(statement(s -> s.addNotResource(RESOURCE_1).addNotResource(RESOURCE_2)).notResources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
        assertThat(statement(s -> s.addNotResource("1").addNotResource("2")).notResources())
            .containsExactly(RESOURCE_1, RESOURCE_2);
    }

    @Test
    public void notResourceCollectionSettersResetsList() {
        assertThat(statement(s -> s.notResources(asList(RESOURCE_1, RESOURCE_2))
                                   .notResources(singletonList(RESOURCE_2))).notResources())
            .containsExactly(RESOURCE_2);
    }

    @Test
    public void conditionGettersSettersWork() {
        assertThat(statement(s -> s.conditions(asList(CONDITION_1, CONDITION_2))).conditions())
            .containsExactly(CONDITION_1, CONDITION_2);
        assertThat(statement(s -> s.addCondition(CONDITION_1)
                                   .addCondition(CONDITION_2)).conditions())
            .containsExactly(CONDITION_1, CONDITION_2);
        assertThat(statement(s -> s.addCondition(p -> p.operator("1").key("K").value("V"))
                                   .addCondition(p -> p.operator("2").key("K").value("V"))).conditions())
            .containsExactly(CONDITION_1, CONDITION_2);
        assertThat(statement(s -> s.addCondition(IamConditionOperator.create("1"), IamConditionKey.create("K"), "V")
                                   .addCondition(IamConditionOperator.create("2"), IamConditionKey.create("K"), "V")).conditions())
            .containsExactly(CONDITION_1, CONDITION_2);
        assertThat(statement(s -> s.addCondition(IamConditionOperator.create("1"), "K", "V")
                                   .addCondition(IamConditionOperator.create("2"), "K", "V")).conditions())
            .containsExactly(CONDITION_1, CONDITION_2);
        assertThat(statement(s -> s.addCondition("1", "K", "V")
                                   .addCondition("2", "K", "V")).conditions())
            .containsExactly(CONDITION_1, CONDITION_2);
        assertThat(statement(s -> s.addConditions(IamConditionOperator.create("1"),
                                                  IamConditionKey.create("K"),
                                                  asList("V1", "V2"))).conditions())
            .containsExactly(IamCondition.create("1", "K", "V1"),
                             IamCondition.create("1", "K", "V2"));
        assertThat(statement(s -> s.addConditions(IamConditionOperator.create("1"), "K", asList("V1", "V2"))).conditions())
            .containsExactly(IamCondition.create("1", "K", "V1"),
                             IamCondition.create("1", "K", "V2"));
        assertThat(statement(s -> s.addConditions("1", "K", asList("V1", "V2"))).conditions())
            .containsExactly(IamCondition.create("1", "K", "V1"),
                             IamCondition.create("1", "K", "V2"));
    }

    @Test
    public void conditionsCollectionSettersResetsList() {
        assertThat(statement(s -> s.conditions(asList(CONDITION_1, CONDITION_1))
                                   .conditions(singletonList(CONDITION_1))).conditions())
            .containsExactly(CONDITION_1);
    }

    private IamStatement statement(Consumer<IamStatement.Builder> statement) {
        return IamStatement.builder().effect(ALLOW).applyMutation(statement).build();
    }
}