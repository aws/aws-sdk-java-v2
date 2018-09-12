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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sfn.builder.ErrorCode;
import software.amazon.awssdk.services.sfn.builder.StateMachine;
import software.amazon.awssdk.services.sfn.builder.conditions.BinaryCondition;
import software.amazon.awssdk.services.sfn.builder.conditions.Condition;
import software.amazon.awssdk.services.sfn.builder.conditions.NAryCondition;
import software.amazon.awssdk.services.sfn.builder.conditions.NotCondition;
import software.amazon.awssdk.services.sfn.builder.internal.PropertyName;
import software.amazon.awssdk.services.sfn.builder.states.Branch;
import software.amazon.awssdk.services.sfn.builder.states.Catcher;
import software.amazon.awssdk.services.sfn.builder.states.Choice;
import software.amazon.awssdk.services.sfn.builder.states.ChoiceState;
import software.amazon.awssdk.services.sfn.builder.states.FailState;
import software.amazon.awssdk.services.sfn.builder.states.NextStateTransition;
import software.amazon.awssdk.services.sfn.builder.states.ParallelState;
import software.amazon.awssdk.services.sfn.builder.states.PassState;
import software.amazon.awssdk.services.sfn.builder.states.Retrier;
import software.amazon.awssdk.services.sfn.builder.states.State;
import software.amazon.awssdk.services.sfn.builder.states.StateVisitor;
import software.amazon.awssdk.services.sfn.builder.states.SucceedState;
import software.amazon.awssdk.services.sfn.builder.states.TaskState;
import software.amazon.awssdk.services.sfn.builder.states.Transition;
import software.amazon.awssdk.services.sfn.builder.states.TransitionState;
import software.amazon.awssdk.services.sfn.builder.states.WaitFor;
import software.amazon.awssdk.services.sfn.builder.states.WaitForSeconds;
import software.amazon.awssdk.services.sfn.builder.states.WaitForSecondsPath;
import software.amazon.awssdk.services.sfn.builder.states.WaitForTimestamp;
import software.amazon.awssdk.services.sfn.builder.states.WaitForTimestampPath;
import software.amazon.awssdk.services.sfn.builder.states.WaitState;

/**
 * Validator for a {@link StateMachine} object.
 * // TODO Does not check max nesting.
 * // TODO Does not validate ARNs against a regex
 */
@SdkInternalApi
public class StateMachineValidator {

    private final ProblemReporter problemReporter = new ProblemReporter();
    private final StateMachine stateMachine;

    public StateMachineValidator(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public StateMachine validate() {
        ValidationContext context = ValidationContext.builder()
                                                     .problemReporter(problemReporter)
                                                     .parentContext(null)
                                                     .identifier("Root")
                                                     .location(Location.StateMachine)
                                                     .build();
        context.assertStringNotEmpty(stateMachine.getStartAt(), PropertyName.START_AT);
        context.assertIsPositiveIfPresent(stateMachine.getTimeoutSeconds(), PropertyName.TIMEOUT_SECONDS);
        context.assertNotEmpty(stateMachine.getStates(), PropertyName.STATES);

        validateStates(context, stateMachine.getStates());

        if (!stateMachine.getStates().containsKey(stateMachine.getStartAt())) {
            problemReporter.report(new Problem(context,
                                               String.format("%s state does not exist.", PropertyName.START_AT)));
        }

        // If basic validation failed then the graph may not be in a good state to be able to validate
        if (!problemReporter.hasProblems()) {
            new GraphValidator(context, stateMachine).validate();
        }

        if (problemReporter.hasProblems()) {
            throw problemReporter.getException();
        }

        return stateMachine;
    }

    private void validateStates(ValidationContext parentContext, Map<String, State> states) {
        for (Map.Entry<String, State> entry : states.entrySet()) {
            parentContext.assertStringNotEmpty(entry.getKey(), "State Name");
            entry.getValue().accept(new StateValidationVisitor(states, parentContext.state(entry.getKey())));
        }
    }

    /**
     * Validates the DFS does not contain unrecoverable cycles (i.e. cycles with no branching logic) or
     * does not contain a path to a terminal state.
     */
    private final class GraphValidator {
        private final Map<String, State> parentVisited;
        private final String initialState;
        private final Map<String, State> states;
        private final Map<String, State> visited = new HashMap<String, State>();
        private final ValidationContext currentContext;

        GraphValidator(ValidationContext context, StateMachine stateMachine) {
            this(context,
                 Collections.<String, State>emptyMap(),
                 stateMachine.getStartAt(),
                 stateMachine.getStates());
        }

        private GraphValidator(ValidationContext context,
                               Map<String, State> parentVisited,
                               String initialState,
                               Map<String, State> states) {
            this.currentContext = context;
            this.parentVisited = parentVisited;
            this.initialState = initialState;
            this.states = states;
        }

        public boolean validate() {
            boolean pathToTerminal = visit(initialState);
            if (parentVisited.isEmpty() && !pathToTerminal) {
                problemReporter.report(new Problem(currentContext, "No path to a terminal state exists."));
            }
            return pathToTerminal;
        }

        private boolean visit(String stateName) {
            ValidationContext stateContext = currentContext.state(stateName);
            final State state = states.get(stateName);
            if (!parentVisited.containsKey(stateName) && visited.containsKey(stateName)) {
                problemReporter.report(new Problem(stateContext, "Cycle detected."));
                return false;
            } else if (parentVisited.containsKey(stateName)) {
                // Cycle but to parent so we may be okay
                return false;
            }
            visited.put(stateName, state);
            if (state instanceof ParallelState) {
                validateParallelState(stateContext, (ParallelState) state);
            }
            if (state.isTerminalState()) {
                return true;
            } else if (state instanceof TransitionState) {
                final Transition transition = ((TransitionState) state).getTransition();
                return visit(((NextStateTransition) transition).getNextStateName());
            } else if (state instanceof ChoiceState) {
                return validateChoiceState(stateContext, (ChoiceState) state);
            } else {
                throw new RuntimeException("Unexpected state type: " + state.getClass().getName());
            }
        }

        private void validateParallelState(ValidationContext stateContext, ParallelState state) {
            int index = 0;
            for (Branch branch : state.getBranches()) {
                new GraphValidator(stateContext.branch(index),
                                   Collections.<String, State>emptyMap(),
                                   branch.getStartAt(),
                                   branch.getStates()).validate();
                index++;
            }
        }

        private boolean validateChoiceState(ValidationContext stateContext, ChoiceState choiceState) {
            final Map<String, State> merged = mergeParentVisited();
            boolean hasPathToTerminal = new GraphValidator(stateContext, merged, choiceState.getDefaultStateName(), states)
                    .validate();
            int index = 0;
            for (Choice choice : choiceState.getChoices()) {
                final String nextStateName = ((NextStateTransition) choice.getTransition()).getNextStateName();
                // It's important hasPathToTerminal is last in the OR so it doesn't short circuit the choice validation
                hasPathToTerminal = new GraphValidator(stateContext.choice(index), merged, nextStateName, states).validate()
                                    || hasPathToTerminal;
                index++;
            }
            return hasPathToTerminal;
        }

        private Map<String, State> mergeParentVisited() {
            final Map<String, State> merged = new HashMap<String, State>(parentVisited.size() + visited.size());
            merged.putAll(parentVisited);
            merged.putAll(visited);
            return merged;
        }

    }

    /**
     * Validates all the supported states and their nested properties.
     */
    private class StateValidationVisitor extends StateVisitor<Void> {

        private final ValidationContext currentContext;
        private final Map<String, State> states;

        private StateValidationVisitor(Map<String, State> states, ValidationContext context) {
            this.states = states;
            this.currentContext = context;
        }

        @Override
        public Void visit(ChoiceState choiceState) {
            currentContext.assertIsValidInputPath(choiceState.getInputPath());
            currentContext.assertIsValidOutputPath(choiceState.getOutputPath());
            if (choiceState.getDefaultStateName() != null) {
                currentContext.assertStringNotEmpty(choiceState.getDefaultStateName(), PropertyName.DEFAULT_STATE);
                assertContainsState(choiceState.getDefaultStateName());
            }

            currentContext.assertNotEmpty(choiceState.getChoices(), PropertyName.CHOICES);
            int index = 0;
            for (Choice choice : choiceState.getChoices()) {
                ValidationContext choiceContext = currentContext.choice(index);
                validateTransition(choiceContext, choice.getTransition());
                validateCondition(choiceContext, choice.getCondition());
                index++;
            }
            return null;
        }

        private void validateCondition(ValidationContext context, Condition condition) {
            context.assertNotNull(condition, "Condition");
            if (condition instanceof BinaryCondition) {
                validateBinaryCondition(context, (BinaryCondition) condition);
            } else if (condition instanceof NAryCondition) {
                validateNAryCondition(context, (NAryCondition) condition);
            } else if (condition instanceof NotCondition) {
                validateCondition(context, ((NotCondition) condition).getCondition());
            } else if (condition != null) {
                throw new RuntimeException("Unsupported condition type: " + condition.getClass());
            }
        }

        private void validateNAryCondition(ValidationContext context, NAryCondition condition) {
            context.assertNotEmpty(condition.getConditions(), "Conditions");
            for (Condition nestedCondition : condition.getConditions()) {
                validateCondition(context, nestedCondition);
            }
        }

        private void validateBinaryCondition(ValidationContext context, BinaryCondition condition) {
            context.assertStringNotEmpty(condition.getVariable(), PropertyName.VARIABLE);
            context.assertIsValidJsonPath(condition.getVariable(), PropertyName.VARIABLE);
            context.assertNotNull(condition.getExpectedValue(), "ExpectedValue");
        }

        @Override
        public Void visit(FailState failState) {
            currentContext.assertStringNotEmpty(failState.getCause(), PropertyName.CAUSE);
            return null;
        }

        @Override
        public Void visit(ParallelState parallelState) {
            currentContext.assertIsValidInputPath(parallelState.getInputPath());
            currentContext.assertIsValidOutputPath(parallelState.getOutputPath());
            currentContext.assertIsValidResultPath(parallelState.getResultPath());
            validateTransition(parallelState.getTransition());
            validateRetriers(parallelState.getRetriers());
            validateCatchers(parallelState.getCatchers());
            validateBranches(parallelState);
            return null;
        }

        private void validateBranches(ParallelState parallelState) {
            currentContext.assertNotEmpty(parallelState.getBranches(), PropertyName.BRANCHES);
            int index = 0;
            for (Branch branch : parallelState.getBranches()) {
                ValidationContext branchContext = currentContext.branch(index);
                validateStates(branchContext, branch.getStates());
                if (!branch.getStates().containsKey(branch.getStartAt())) {
                    problemReporter.report(new Problem(branchContext, String.format("%s references a non existent state.",
                                                                                    PropertyName.START_AT)));
                }
                index++;
            }
        }

        @Override
        public Void visit(PassState passState) {
            currentContext.assertIsValidInputPath(passState.getInputPath());
            currentContext.assertIsValidOutputPath(passState.getOutputPath());
            currentContext.assertIsValidResultPath(passState.getResultPath());
            validateTransition(passState.getTransition());
            return null;
        }

        @Override
        public Void visit(SucceedState succeedState) {
            currentContext.assertIsValidInputPath(succeedState.getInputPath());
            currentContext.assertIsValidOutputPath(succeedState.getOutputPath());
            return null;
        }

        @Override
        public Void visit(TaskState taskState) {
            currentContext.assertIsValidInputPath(taskState.getInputPath());
            currentContext.assertIsValidOutputPath(taskState.getOutputPath());
            currentContext.assertIsValidResultPath(taskState.getResultPath());
            currentContext.assertIsPositiveIfPresent(taskState.getTimeoutSeconds(), PropertyName.TIMEOUT_SECONDS);
            currentContext.assertIsPositiveIfPresent(taskState.getHeartbeatSeconds(), PropertyName.HEARTBEAT_SECONDS);
            if (taskState.getTimeoutSeconds() != null && taskState.getHeartbeatSeconds() != null) {
                if (taskState.getHeartbeatSeconds() >= taskState.getTimeoutSeconds()) {
                    problemReporter.report(new Problem(currentContext, String.format("%s must be smaller than %s",
                                                                                     PropertyName.HEARTBEAT_SECONDS,
                                                                                     PropertyName.TIMEOUT_SECONDS)));
                }
            }

            currentContext.assertStringNotEmpty(taskState.getResource(), PropertyName.RESOURCE);
            validateRetriers(taskState.getRetriers());
            validateCatchers(taskState.getCatchers());
            validateTransition(taskState.getTransition());
            return null;
        }

        private void validateRetriers(List<Retrier> retriers) {
            boolean hasRetryAll = false;
            int index = 0;
            for (Retrier retrier : retriers) {
                ValidationContext retrierContext = currentContext.retrier(index);
                if (hasRetryAll) {
                    problemReporter.report(
                            new Problem(retrierContext,
                                        String.format("When %s is used in must be in the last Retrier", ErrorCode.ALL)));
                }
                // MaxAttempts may be zero
                retrierContext.assertIsNotNegativeIfPresent(retrier.getMaxAttempts(), PropertyName.MAX_ATTEMPTS);
                retrierContext.assertIsPositiveIfPresent(retrier.getIntervalSeconds(), PropertyName.INTERVAL_SECONDS);
                if (retrier.getBackoffRate() != null && retrier.getBackoffRate() < 1.0) {
                    problemReporter.report(new Problem(retrierContext, String.format("%s must be greater than or equal to 1.0",
                                                                                     PropertyName.BACKOFF_RATE)));
                }
                hasRetryAll = validateErrorEquals(retrierContext, retrier.getErrorEquals());
                index++;
            }
        }


        private void validateCatchers(List<Catcher> catchers) {
            boolean hasCatchAll = false;
            int index = 0;
            for (Catcher catcher : catchers) {
                ValidationContext catcherContext = currentContext.catcher(index);
                catcherContext.assertIsValidResultPath(catcher.getResultPath());
                if (hasCatchAll) {
                    problemReporter.report(
                            new Problem(catcherContext,
                                        String.format("When %s is used in must be in the last Catcher", ErrorCode.ALL)));
                }
                validateTransition(catcherContext, catcher.getTransition());
                hasCatchAll = validateErrorEquals(catcherContext, catcher.getErrorEquals());
                index++;
            }
        }

        private boolean validateErrorEquals(ValidationContext currentContext, List<String> errorEquals) {
            currentContext.assertNotEmpty(errorEquals, PropertyName.ERROR_EQUALS);
            if (errorEquals.contains(ErrorCode.ALL)) {
                if (errorEquals.size() != 1) {
                    problemReporter.report(new Problem(currentContext, String.format(
                            "When %s is used in %s, it must be the only error code in the array",
                            ErrorCode.ALL, PropertyName.ERROR_EQUALS)));
                }
                return true;
            }
            return false;
        }

        @Override
        public Void visit(WaitState waitState) {
            currentContext.assertIsValidInputPath(waitState.getInputPath());
            currentContext.assertIsValidOutputPath(waitState.getOutputPath());
            validateTransition(waitState.getTransition());
            validateWaitFor(waitState.getWaitFor());
            return null;
        }

        private void validateWaitFor(WaitFor waitFor) {
            currentContext.assertNotNull(waitFor, "WaitFor");
            if (waitFor instanceof WaitForSeconds) {
                currentContext.assertIsPositiveIfPresent(((WaitForSeconds) waitFor).getSeconds(), PropertyName.SECONDS);
            } else if (waitFor instanceof WaitForSecondsPath) {
                assertWaitForPath(((WaitForSecondsPath) waitFor).getSecondsPath(), PropertyName.SECONDS_PATH);
            } else if (waitFor instanceof WaitForTimestamp) {
                currentContext.assertNotNull(((WaitForTimestamp) waitFor).getTimestamp(), PropertyName.TIMESTAMP);
            } else if (waitFor instanceof WaitForTimestampPath) {
                assertWaitForPath(((WaitForTimestampPath) waitFor).getTimestampPath(), PropertyName.TIMESTAMP_PATH);
            } else if (waitFor != null) {
                throw new RuntimeException("Unsupported WaitFor strategy: " + waitFor.getClass());
            }
        }

        /**
         * TimestampPath and SecondsPath must have a valid reference path.
         */
        private void assertWaitForPath(String pathValue, String propertyName) {
            currentContext.assertNotNull(pathValue, propertyName);
            currentContext.assertIsValidReferencePath(pathValue, propertyName);
        }

        private void validateTransition(Transition transition) {
            validateTransition(currentContext, transition);
        }

        private void validateTransition(ValidationContext context, Transition transition) {
            context.assertNotNull(transition, "Transition");
            if (transition instanceof NextStateTransition) {
                final String nextStateName = ((NextStateTransition) transition).getNextStateName();
                context.assertNotNull(nextStateName, PropertyName.NEXT);
                assertContainsState(context, nextStateName);
            }
        }

        private void assertContainsState(String nextStateName) {
            assertContainsState(currentContext, nextStateName);
        }

        private void assertContainsState(ValidationContext context, String nextStateName) {
            if (!states.containsKey(nextStateName)) {
                problemReporter.report(new Problem(context, String.format("%s is not a valid state", nextStateName)));
            }
        }
    }

}
