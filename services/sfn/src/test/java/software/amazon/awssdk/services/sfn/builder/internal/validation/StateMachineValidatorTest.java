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

import static software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder.eq;

import org.junit.Test;
import software.amazon.awssdk.services.sfn.builder.ErrorCode;
import software.amazon.awssdk.services.sfn.builder.StepFunctionBuilder;
import software.amazon.awssdk.services.sfn.builder.conditions.NotCondition;

public class StateMachineValidatorTest {

    @Test(expected = ValidationException.class)
    public void nothingSet_IsNotValid() {
        StepFunctionBuilder.stateMachine().build();
    }

    @Test(expected = ValidationException.class)
    public void noStates_IsNotValid() {
        StepFunctionBuilder.stateMachine().startAt("Foo").build();
    }

    @Test(expected = ValidationException.class)
    public void startAtStateDoesNotExist_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Foo")
                           .state("Initial", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test
    public void validMinimalStateMachine_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void missingResourceInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void missingTransitionInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void invalidTransitionInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.next("NoSuchState"))
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void negativeTimeoutSecondsInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .timeoutSeconds(-1)
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void zeroTimeoutSecondsInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .timeoutSeconds(0)
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void negativeHeartbeatSecondsInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .heartbeatSeconds(-1)
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void zeroHeartbeatSecondsInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .heartbeatSeconds(0)
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void heartbeatSecondsGreaterThanTimeoutSecondsInTaskState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .heartbeatSeconds(60)
                                                                .timeoutSeconds(30)
                                                                .resource("arn"))
                           .build();
    }

    @Test
    public void retrierInTaskState_OnlyErrorEqualsSet_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_MaxAttemptsNegative_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .maxAttempts(-1)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test
    public void retrierInTaskState_MaxAttemptsZero_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .maxAttempts(0)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_IntervalSecondsNegative_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .intervalSeconds(-1)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_IntervalSecondsZero_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .intervalSeconds(0)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test
    public void retrierInTaskState_IntervalSecondsPositive_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .intervalSeconds(10)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_BackoffRateNegative_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .backoffRate(-1.0)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_BackoffRateLessThanOne_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .backoffRate(0.5)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test
    public void retrierInTaskState_BackoffRateGreaterThanOne_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .backoffRate(1.5)
                                                                                            .retryOnAllErrors())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_RetryAllHasOtherErrorCodes_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .errorEquals("Foo", "Bar", ErrorCode.ALL))
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void retrierInTaskState_RetryAllIsNotLastRetrier_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .retryOnAllErrors())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .errorEquals("Foo", "Bar"))
                                                                .resource("arn"))
                           .build();
    }

    @Test
    public void catcherInTaskState_ValidTransition_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .catcher(StepFunctionBuilder.catcher()
                                                                                            .transition(StepFunctionBuilder.next("Terminal"))
                                                                                            .catchAll())
                                                                .resource("arn"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }


    @Test(expected = ValidationException.class)
    public void catcherInTaskState_InvalidTransition_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .catcher(StepFunctionBuilder.catcher()
                                                                                            .transition(StepFunctionBuilder.next("NoSuchState"))
                                                                                            .catchAll())
                                                                .resource("arn"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void catcherInTaskState_CatchAllHasOtherErrorCodes_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .catcher(StepFunctionBuilder.catcher()
                                                                                            .transition(StepFunctionBuilder.next("Terminal"))
                                                                                            .errorEquals("Foo", "Bar", ErrorCode.ALL))
                                                                .resource("arn"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void catcherInTaskState_CatchAllIsNotLastCatcher_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.taskState()
                                                                .transition(StepFunctionBuilder.end())
                                                                .catcher(StepFunctionBuilder.catcher()
                                                                                            .transition(StepFunctionBuilder.next("Terminal"))
                                                                                            .catchAll())
                                                                .catcher(StepFunctionBuilder.catcher()
                                                                                            .transition(StepFunctionBuilder.next("Terminal"))
                                                                                            .errorEquals("Foo", "Bar"))
                                                                .resource("arn"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void invalidTransitionInWaitState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.seconds(10))
                                                                .transition(StepFunctionBuilder.next("NoSuchState")))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void noWaitForSupplied_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForSeconds_NegativeSeconds_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.seconds(-1))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForSeconds_ZeroSeconds_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.seconds(0))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForTimestamp_NullDate_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.timestamp(null))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForTimestampPath_MissingPath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.timestampPath(null))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForTimestampPath_EmptyPath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.timestampPath(""))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForTimestampPath_InvalidJsonPath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.timestampPath("$."))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForTimestampPath_InvalidReferencePath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.timestampPath("$.foo[*]"))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForSecondsPath_MissingPath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.secondsPath(null))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForSecondsPath_EmptyPath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.secondsPath(""))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForSecondsPath_InvalidJsonPath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.secondsPath("$."))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void waitForSecondsPath_InvalidReferencePath_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.waitState()
                                                                .waitFor(StepFunctionBuilder.secondsPath("$.foo[*]"))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void invalidTransitionInPassState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.passState()
                                                                .transition(StepFunctionBuilder.next("NoSuchState")))
                           .build();
    }

    @Test
    public void validTransitionInPassState_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.passState()
                                                                .transition(StepFunctionBuilder.next("Terminal")))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void missingCauseInFailState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.failState()
                                                                .error("Error"))
                           .build();
    }

    @Test
    public void missingErrorInFailState_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.failState()
                                                                .cause("Cause"))
                           .build();
    }

    @Test
    public void failStateWithErrorAndCause_IsValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.failState()
                                                                .error("Error")
                                                                .cause("Cause"))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void choiceStateWithNoChoices_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .defaultStateName("Terminal"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void choiceStateWithInvalidDefaultState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .choice(StepFunctionBuilder.choice()
                                                                                           .condition(StepFunctionBuilder.eq("$.foo", "bar"))
                                                                                           .transition(StepFunctionBuilder.next("Terminal")))
                                                                .defaultStateName("NoSuchState"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void choiceStateWithInvalidChoiceTransition_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .choice(StepFunctionBuilder.choice()
                                                                                           .condition(StepFunctionBuilder.eq("$.foo", "bar"))
                                                                                           .transition(StepFunctionBuilder.next("NoSuchState")))
                                                                .defaultStateName("Terminal"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void missingVariable_StringEqualsCondition_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .choice(StepFunctionBuilder.choice()
                                                                                           .condition(StepFunctionBuilder.eq(null, "foo"))
                                                                                           .transition(StepFunctionBuilder.next("Terminal")))
                                                                .defaultStateName("Terminal"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void missingExpectedValue_StringEqualsCondition_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .choice(StepFunctionBuilder.choice()
                                                                                           .condition(StepFunctionBuilder.eq("$.foo", (String) null))
                                                                                           .transition(StepFunctionBuilder.next("Terminal")))
                                                                .defaultStateName("Terminal"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void noConditionsInAnd_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .choice(StepFunctionBuilder.choice()
                                                                                           .condition(StepFunctionBuilder.and())
                                                                                           .transition(StepFunctionBuilder.next("Terminal")))
                                                                .defaultStateName("Terminal"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void noConditionSetForNot_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.choiceState()
                                                                .choice(StepFunctionBuilder.choice()
                                                                                           .condition(NotCondition.builder())
                                                                                           .transition(StepFunctionBuilder.next("Terminal")))
                                                                .defaultStateName("Terminal"))
                           .state("Terminal", StepFunctionBuilder.succeedState())
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void parallelStateWithNoBranches_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.parallelState()
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void parallelStateWithInvalidTransition_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.parallelState()
                                                                .branch(StepFunctionBuilder.branch()
                                                                                           .startAt("InitialBranchState")
                                                                                           .state("InitialBranchState", StepFunctionBuilder.succeedState()))
                                                                .transition(StepFunctionBuilder.next("NoSuchState")))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void parallelStateBranchStartAtStateInvalid_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.parallelState()
                                                                .branch(StepFunctionBuilder.branch()
                                                                                           .startAt("NoSuchState")
                                                                                           .state("InitialBranchState", StepFunctionBuilder.succeedState()))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void parallelStateInvalidBranchState_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.parallelState()
                                                                .branch(StepFunctionBuilder.branch()
                                                                                           .startAt("InitialBranchState")
                                                                                           .state("InitialBranchState", StepFunctionBuilder.failState()))
                                                                .transition(StepFunctionBuilder.end()))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void parallelStateInvalidRetrier_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.parallelState()
                                                                .branch(StepFunctionBuilder.branch()
                                                                                           .startAt("InitialBranchState")
                                                                                           .state("InitialBranchState", StepFunctionBuilder.succeedState()))
                                                                .transition(StepFunctionBuilder.end())
                                                                .retrier(StepFunctionBuilder.retrier()
                                                                                            .intervalSeconds(-1)))
                           .build();
    }

    @Test(expected = ValidationException.class)
    public void parallelStateInvalidCatcher_IsNotValid() {
        StepFunctionBuilder.stateMachine()
                           .startAt("Initial")
                           .state("Initial", StepFunctionBuilder.parallelState()
                                                                .branch(StepFunctionBuilder.branch()
                                                                                           .startAt("InitialBranchState")
                                                                                           .state("InitialBranchState", StepFunctionBuilder.succeedState()))
                                                                .transition(StepFunctionBuilder.end())
                                                                .catcher(StepFunctionBuilder.catcher()
                                                                                            .transition(StepFunctionBuilder.next("NoSuchState"))))
                           .build();
    }
}
