/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sfn.builder.internal.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.eq;

import org.junit.Test;
import software.amazon.awssdk.services.sfn.builder.StateMachine;
import software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder;

public class CycleTest {

    @Test
    public void singleTerminalState_HasNoCycle_IsValid() {
        assertNoCycle(StepFunctionBuilder.stateMachine()
                                         .startAt("Initial")
                                         .state("Initial", StepFunctionBuilder.succeedState()));
    }

    @Test(expected = ValidationException.class)
    public void simpleStateMachine_WithCycle_IsNotValid() {
        assertCycle(StepFunctionBuilder.stateMachine()
                                       .startAt("Initial")
                                       .state("Initial", StepFunctionBuilder.passState()
                                                                            .transition(StepFunctionBuilder.next("Next")))
                                       .state("Next", StepFunctionBuilder.passState()
                                                                         .transition(StepFunctionBuilder.next("Initial"))));
    }

    @Test(expected = ValidationException.class)
    public void choiceStateWithOnlyCycles_IsNotValid() {
        assertDoesNotHaveTerminalPath(StepFunctionBuilder.stateMachine()
                                                         .startAt("Initial")
                                                         .state("Initial", StepFunctionBuilder.passState()
                                                                                              .transition(StepFunctionBuilder.next("Choice")))
                                                         .state("Choice", StepFunctionBuilder.choiceState()
                                                                                             .defaultStateName("Default")
                                                                                             .choice(StepFunctionBuilder.choice()
                                                                                                                        .transition(StepFunctionBuilder.next("Initial"))
                                                                                                                        .condition(StepFunctionBuilder.eq("$.foo", "bar")))
                                                                                             .choice(StepFunctionBuilder.choice()
                                                                                                                        .transition(StepFunctionBuilder.next("Default"))
                                                                                                                        .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                                                         .state("Default", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("Choice"))));
    }

    @Test
    public void choiceStateWithPathToTerminal_IsValid() {
        assertHasPathToTerminal(StepFunctionBuilder.stateMachine()
                                                   .startAt("Initial")
                                                   .state("Initial", StepFunctionBuilder.passState()
                                                                                        .transition(StepFunctionBuilder.next("Choice")))
                                                   .state("Choice", StepFunctionBuilder.choiceState()
                                                                                       .defaultStateName("Default")
                                                                                       .choice(StepFunctionBuilder.choice()
                                                                                                                  .transition(StepFunctionBuilder.next("Initial"))
                                                                                                                  .condition(StepFunctionBuilder.eq("$.foo", "bar")))
                                                                                       .choice(StepFunctionBuilder.choice()
                                                                                                                  .transition(StepFunctionBuilder.next("Default"))
                                                                                                                  .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                                                   .state("Default", StepFunctionBuilder.passState().transition(StepFunctionBuilder.end())));
    }

    @Test(expected = ValidationException.class)
    public void choiceStateWithClosedCycle_IsNotValid() {
        assertCycle(StepFunctionBuilder.stateMachine()
                                       .startAt("Initial")
                                       .state("Initial", StepFunctionBuilder.passState()
                                                                            .transition(StepFunctionBuilder.next("Choice")))
                                       .state("Choice", StepFunctionBuilder.choiceState()
                                                                           .defaultStateName("Terminal")
                                                                           .choice(StepFunctionBuilder.choice()
                                                                                                      .transition(StepFunctionBuilder.next("Terminal"))
                                                                                                      .condition(StepFunctionBuilder.eq("$.foo", "bar")))
                                                                           .choice(StepFunctionBuilder.choice()
                                                                                                      .transition(StepFunctionBuilder.next("NonTerminal"))
                                                                                                      .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                                       .state("Terminal", StepFunctionBuilder.passState().transition(StepFunctionBuilder.end()))
                                       .state("NonTerminal", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("Cyclic")))
                                       .state("Cyclic", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("NonTerminal"))));
    }

    /**
     * While the nested ChoiceTwo state only has cycles, it has a cycle out of the choice state to
     * a state that contains a path to a terminal. The validator doesn't validate that the path out actually
     * has a path to the terminal so there are some invalid state machines that will pass validation.
     */
    @Test
    public void choiceStateWithPathOut_IsValid() {
        assertNoCycle(
            StepFunctionBuilder.stateMachine()
                               .startAt("Initial")
                               .state("Initial", StepFunctionBuilder.passState()
                                                                    .transition(StepFunctionBuilder.next("ChoiceOne")))
                               .state("ChoiceOne", StepFunctionBuilder.choiceState()
                                                                      .defaultStateName("DefaultOne")
                                                                      .choice(StepFunctionBuilder.choice()
                                                                                                 .transition(StepFunctionBuilder.next("ChoiceTwo"))
                                                                                                 .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                               .state("DefaultOne", StepFunctionBuilder.succeedState())
                               .state("ChoiceTwo", StepFunctionBuilder.choiceState()
                                                                      .defaultStateName("DefaultTwo")
                                                                      .choice(StepFunctionBuilder.choice()
                                                                                                 .transition(StepFunctionBuilder.next("ChoiceOne"))
                                                                                                 .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                               .state("DefaultTwo", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("ChoiceTwo"))));
    }

    @Test
    public void parallelState_NoCycles() {
        assertNoCycle(StepFunctionBuilder.stateMachine()
                                         .startAt("Initial")
                                         .state("Initial", StepFunctionBuilder.parallelState()
                                                                              .branch(StepFunctionBuilder.branch()
                                                                                                         .startAt("BranchOneStart")
                                                                                                         .state("BranchOneStart", StepFunctionBuilder.succeedState()))
                                                                              .branch(StepFunctionBuilder.branch()
                                                                                                         .startAt("BranchTwoStart")
                                                                                                         .state("BranchTwoStart", StepFunctionBuilder.passState()
                                                                                                                                                     .transition(StepFunctionBuilder.next("NextState")))
                                                                                                         .state("NextState", StepFunctionBuilder.succeedState()))
                                                                              .transition(StepFunctionBuilder.end())));
    }

    @Test(expected = ValidationException.class)
    public void parallelState_WithCycles_IsNotValid() {
        assertCycle(StepFunctionBuilder.stateMachine()
                                       .startAt("Parallel")
                                       .state("Parallel", StepFunctionBuilder.parallelState()
                                                                             .branch(StepFunctionBuilder.branch()
                                                                                                        .startAt("BranchOneInitial")
                                                                                                        .state("BranchOneInitial", StepFunctionBuilder.passState()
                                                                                                                                                      .transition(StepFunctionBuilder.next("CyclicState")))
                                                                                                        .state("CyclicState", StepFunctionBuilder.passState()
                                                                                                                                                 .transition(StepFunctionBuilder.next("BranchOneInitial"))))
                                                                             .transition(StepFunctionBuilder.end())));
    }

    @Test(expected = ValidationException.class)
    public void parallelState_WithChoiceThatHasNoTerminalPath_IsNotValid() {
        assertDoesNotHaveTerminalPath(
            StepFunctionBuilder.stateMachine()
                               .startAt("Parallel")
                               .state("Parallel", StepFunctionBuilder.parallelState()
                                                                     .transition(StepFunctionBuilder.end())
                                                                     .branch(StepFunctionBuilder.branch()
                                                                                                .startAt("Initial")
                                                                                                .state("Initial", StepFunctionBuilder.passState()
                                                                                                                                     .transition(StepFunctionBuilder.next("Choice")))
                                                                                                .state("Choice", StepFunctionBuilder.choiceState()
                                                                                                                                    .defaultStateName("Default")
                                                                                                                                    .choice(StepFunctionBuilder.choice()
                                                                                                                                                               .transition(StepFunctionBuilder.next("Initial"))
                                                                                                                                                               .condition(StepFunctionBuilder.eq("$.foo", "bar")))
                                                                                                                                    .choice(StepFunctionBuilder.choice()
                                                                                                                                                               .transition(StepFunctionBuilder.next("Default"))
                                                                                                                                                               .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                                                                                                .state("Default", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("Choice"))))));
    }

    @Test
    public void parallelState_ChoiceStateWithTerminalPath_IsValid() {
        assertHasPathToTerminal(
            StepFunctionBuilder.stateMachine()
                               .startAt("Parallel")
                               .state("Parallel", StepFunctionBuilder.parallelState()
                                                                     .transition(StepFunctionBuilder.end())
                                                                     .branch(StepFunctionBuilder.branch()
                                                                                                .startAt("Initial")
                                                                                                .state("Initial", StepFunctionBuilder.passState()
                                                                                                                                     .transition(StepFunctionBuilder.next("Choice")))
                                                                                                .state("Choice", StepFunctionBuilder.choiceState()
                                                                                                                                    .defaultStateName("Default")
                                                                                                                                    .choice(StepFunctionBuilder.choice()
                                                                                                                                                               .transition(StepFunctionBuilder.next("Initial"))
                                                                                                                                                               .condition(StepFunctionBuilder.eq("$.foo", "bar")))
                                                                                                                                    .choice(StepFunctionBuilder.choice()
                                                                                                                                                               .transition(StepFunctionBuilder.next("Default"))
                                                                                                                                                               .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                                                                                                .state("Default", StepFunctionBuilder.passState().transition(StepFunctionBuilder.end())))));
    }

    @Test(expected = ValidationException.class)
    public void parallelState_BranchContainsChoiceStateWithClosedCycle_IsNotValid() {
        assertCycle(
            StepFunctionBuilder.stateMachine()
                               .startAt("Initial")
                               .state("Initial", StepFunctionBuilder.passState()
                                                                    .transition(StepFunctionBuilder.next("Choice")))
                               .state("Choice", StepFunctionBuilder.choiceState()
                                                                   .defaultStateName("Terminal")
                                                                   .choice(StepFunctionBuilder.choice()
                                                                                              .transition(StepFunctionBuilder.next("Terminal"))
                                                                                              .condition(StepFunctionBuilder.eq("$.foo", "bar")))
                                                                   .choice(StepFunctionBuilder.choice()
                                                                                              .transition(StepFunctionBuilder.next("NonTerminal"))
                                                                                              .condition(StepFunctionBuilder.eq("$.foo", "bar"))))
                               .state("Terminal", StepFunctionBuilder.passState().transition(StepFunctionBuilder.end()))
                               .state("NonTerminal", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("Cyclic")))
                               .state("Cyclic", StepFunctionBuilder.passState().transition(StepFunctionBuilder.next("NonTerminal"))));
    }

    private void assertCycle(StateMachine.Builder stateMachineBuilder) {
        try {
            validate(stateMachineBuilder);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Cycle detected"));
        }
    }

    private void assertDoesNotHaveTerminalPath(StateMachine.Builder stateMachineBuilder) {
        try {
            validate(stateMachineBuilder);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("No path to a terminal state exists in the state machine"));
        }
    }

    private void assertHasPathToTerminal(StateMachine.Builder stateMachineBuilder) {
        validate(stateMachineBuilder);
    }

    private void assertNoCycle(StateMachine.Builder stateMachineBuilder) {
        validate(stateMachineBuilder);
    }

    private void validate(StateMachine.Builder stateMachineBuilder) {
        new StateMachineValidator(stateMachineBuilder.build()).validate();
    }
}
