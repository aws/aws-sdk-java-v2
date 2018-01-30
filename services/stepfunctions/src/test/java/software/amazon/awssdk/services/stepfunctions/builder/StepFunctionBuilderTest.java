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

package software.amazon.awssdk.services.stepfunctions.builder;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.and;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.branch;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.catcher;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.choice;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.choiceState;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.end;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.eq;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.failState;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.gt;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.gte;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.lt;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.lte;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.next;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.not;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.or;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.parallelState;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.passState;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.retrier;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.seconds;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.secondsPath;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.stateMachine;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.succeedState;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.taskState;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.timestamp;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.timestampPath;
import static software.amazon.awssdk.services.stepfunctions.builder.StepFunctionBuilder.waitState;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.Test;

public class StepFunctionBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void singleSucceedState() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .timeoutSeconds(30)
                .comment("My Simple State Machine")
                .state("InitialState", succeedState()
                        .comment("Initial State")
                        .inputPath("$.input")
                        .outputPath("$.output"))
                .build();
        assertStateMachine(stateMachine, "SingleSucceedState.json");
    }

    @Test
    public void singleTaskState() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", taskState()
                        .comment("Initial State")
                        .timeoutSeconds(10)
                        .heartbeatSeconds(1)
                        .transition(next("NextState"))
                        .resource("resource-arn")
                        .inputPath("$.input")
                        .resultPath("$.result")
                        .outputPath("$.output"))
                .state("NextState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SimpleTaskState.json");
    }

    @Test
    public void taskStateWithEnd() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", taskState()
                        .resource("resource-arn")
                        .transition(end()))
                .build();

        assertStateMachine(stateMachine, "TaskStateWithEnd.json");
    }

    @Test
    public void singleTaskStateWithRetries() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", taskState()
                        .transition(next("NextState"))
                        .resource("resource-arn")
                        .retriers(retrier()
                                          .errorEquals("Foo", "Bar")
                                          .intervalSeconds(20)
                                          .maxAttempts(3)
                                          .backoffRate(2.0),
                                  retrier()
                                          .retryOnAllErrors()
                                          .intervalSeconds(30)
                                          .maxAttempts(10)
                                          .backoffRate(2.0)))
                .state("NextState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SimpleTaskStateWithRetries.json");
    }

    @Test
    public void singleTaskStateWithCatchers() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", taskState()
                        .transition(next("NextState"))
                        .resource("resource-arn")
                        .catchers(catcher()
                                          .errorEquals("Foo", "Bar")
                                          .transition(next("RecoveryState"))
                                          .resultPath("$.result-path"),
                                  catcher()
                                          .catchAll()
                                          .transition(next("OtherRecoveryState"))))
                .state("NextState", succeedState())
                .state("RecoveryState", succeedState())
                .state("OtherRecoveryState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SimpleTaskStateWithCatchers.json");
    }

    @Test
    public void singlePassStateWithJsonResult() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", passState()
                        .comment("Pass through state")
                        .inputPath("$.input")
                        .outputPath("$.output")
                        .resultPath("$.result")
                        .transition(next("NextState"))
                        .result("{\"Foo\": \"Bar\"}"))
                .state("NextState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SinglePassStateWithJsonResult.json");
    }

    @Test
    public void singlePassStateWithObjectResult() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", passState()
                        .transition(end())
                        .result(new SimplePojo("value")))
                .build();

        assertStateMachine(stateMachine, "SinglePassStateWithObjectResult.json");
    }

    @Test
    public void singleWaitState_WaitForSeconds() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", waitState()
                        .comment("My wait state")
                        .inputPath("$.input")
                        .outputPath("$.output")
                        .waitFor(seconds(10))
                        .transition(next("NextState")))
                .state("NextState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SingleWaitStateWithSeconds.json");
    }

    @Test
    public void singleWaitState_WaitUntilSecondsPath() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", waitState()
                        .waitFor(secondsPath("$.seconds"))
                        .transition(end()))
                .build();

        assertStateMachine(stateMachine, "SingleWaitStateWithSecondsPath.json");
    }

    @Test
    public void singleWaitState_WaitUntilTimestamp() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", waitState()
                        .waitFor(timestamp(Date.from(ZonedDateTime.parse("2016-03-14T01:59:00Z").toInstant())))
                        .transition(end()))
                .build();

        assertStateMachine(stateMachine, "SingleWaitStateWithTimestamp.json");
    }

    @Test
    public void singleWaitState_WaitUntilTimestampWithMillisecond() {
        long millis = ZonedDateTime.parse("2016-03-14T01:59:00.123Z").toInstant().toEpochMilli();
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", waitState()
                        .waitFor(timestamp(new Date(millis)))
                        .transition(end()))
                .build();

        assertStateMachine(stateMachine, "SingleWaitStateWithTimestampWithMilliseconds.json");
    }

    @Test
    public void singleWaitState_WaitUntilTimestampWithTimezone() {
        long epochMilli = ZonedDateTime.parse("2016-03-14T01:59:00.123-08:00").toInstant().toEpochMilli();
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", waitState()
                        .waitFor(timestamp(new Date(epochMilli)))
                        .transition(end()))
                .build();

        assertStateMachine(stateMachine, "SingleWaitStateWithTimestampWithTimezone.json");
    }

    @Test
    public void singleWaitState_WaitUntilTimestampWithPath() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", waitState()
                        .waitFor(timestampPath("$.timestamp"))
                        .transition(end()))
                .build();

        assertStateMachine(stateMachine, "SingleWaitStateWithTimestampWithPath.json");
    }

    @Test
    public void singleFailState() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", failState()
                        .comment("My fail state")
                        .cause("InternalError")
                        .error("java.lang.Exception"))
                .build();

        assertStateMachine(stateMachine, "SingleFailState.json");
    }

    @Test
    public void simpleChoiceState() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .comment("My choice state")
                        .defaultStateName("DefaultState")
                        .inputPath("$.input")
                        .outputPath("$.output")
                        .choice(choice().transition(next("NextState"))
                                        .condition(eq("$.var", "value"))))
                .state("NextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SimpleChoiceState.json");
    }

    @Test
    public void choiceStateWithMultipleChoices() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .defaultStateName("DefaultState")
                        .choices(
                                choice().transition(next("NextState"))
                                        .condition(eq("$.var", "value")),
                                choice().transition(next("OtherNextState"))
                                        .condition(gt("$.number", 10))))
                .state("NextState", succeedState())
                .state("OtherNextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ChoiceStateWithMultipleChoices.json");
    }

    @Test
    public void choiceStateWithAndCondition() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .defaultStateName("DefaultState")
                        .choice(choice().transition(next("NextState"))
                                        .condition(
                                                and(eq("$.var", "value"),
                                                    eq("$.other-var", 10)
                                                   ))))
                .state("NextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ChoiceStateWithAndCondition.json");
    }

    @Test
    public void choiceStateWithOrCondition() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .defaultStateName("DefaultState")
                        .choice(choice().transition(next("NextState"))
                                        .condition(
                                                or(gt("$.var", "value"),
                                                   lte("$.other-var", 10)
                                                  ))))
                .state("NextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ChoiceStateWithOrCondition.json");
    }

    @Test
    public void choiceStateWithNotCondition() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .defaultStateName("DefaultState")
                        .choice(choice().transition(next("NextState"))
                                        .condition(not(gte("$.var", "value")))))
                .state("NextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ChoiceStateWithNotCondition.json");
    }

    @Test
    public void choiceStateWithComplexCondition() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .defaultStateName("DefaultState")
                        .choice(choice().transition(next("NextState"))
                                        .condition(and(
                                                gte("$.var", "value"),
                                                lte("$.other-var", "foo"),
                                                or(
                                                        lt("$.numeric", 9000.1),
                                                        not(gte("$.numeric", 42))
                                                  )
                                                      ))))
                .state("NextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ChoiceStateWithComplexCondition.json");
    }

    @Test
    public void choiceStateWithAllPrimitiveConditions() {
        final Date date = Date.from(ZonedDateTime.parse("2016-03-14T01:59:00.000Z").toInstant());
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", choiceState()
                        .defaultStateName("DefaultState")
                        .choice(choice().transition(next("NextState"))
                                        .condition(and(
                                                eq("$.string", "value"),
                                                gt("$.string", "value"),
                                                gte("$.string", "value"),
                                                lt("$.string", "value"),
                                                lte("$.string", "value"),
                                                eq("$.integral", 42),
                                                gt("$.integral", 42),
                                                gte("$.integral", 42),
                                                lt("$.integral", 42),
                                                lte("$.integral", 42),
                                                eq("$.double", 9000.1),
                                                gt("$.double", 9000.1),
                                                gte("$.double", 9000.1),
                                                lt("$.double", 9000.1),
                                                lte("$.double", 9000.1),
                                                eq("$.timestamp", date),
                                                gt("$.timestamp", date),
                                                gte("$.timestamp", date),
                                                lt("$.timestamp", date),
                                                lte("$.timestamp", date),
                                                eq("$.boolean", true),
                                                eq("$.boolean", false)
                                                      ))))
                .state("NextState", succeedState())
                .state("DefaultState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ChoiceStateWithAllPrimitiveCondition.json");
    }

    @Test
    public void simpleParallelState() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", parallelState()
                        .comment("My parallel state")
                        .inputPath("$.input")
                        .outputPath("$.output")
                        .resultPath("$.result")
                        .transition(next("NextState"))
                        .branches(
                                branch()
                                        .comment("Branch one")
                                        .startAt("BranchOneInitial")
                                        .state("BranchOneInitial", succeedState()),
                                branch()
                                        .comment("Branch two")
                                        .startAt("BranchTwoInitial")
                                        .state("BranchTwoInitial", succeedState())
                                 ))
                .state("NextState", succeedState())
                .build();

        assertStateMachine(stateMachine, "SimpleParallelState.json");
    }

    @Test
    public void parallelStateWithRetriers() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", parallelState()
                        .transition(end())
                        .branches(
                                branch()
                                        .comment("Branch one")
                                        .startAt("BranchOneInitial")
                                        .state("BranchOneInitial", succeedState()),
                                branch()
                                        .comment("Branch two")
                                        .startAt("BranchTwoInitial")
                                        .state("BranchTwoInitial", succeedState())
                                 )
                        .retriers(retrier()
                                          .errorEquals("Foo", "Bar")
                                          .intervalSeconds(10)
                                          .backoffRate(1.0)
                                          .maxAttempts(3),
                                  retrier()
                                          .retryOnAllErrors()
                                          .intervalSeconds(10)
                                          .backoffRate(1.0)
                                          .maxAttempts(3)
                                 ))
                .build();

        assertStateMachine(stateMachine, "ParallelStateWithRetriers.json");
    }

    @Test
    public void parallelStateWithCatchers() {
        final StateMachine stateMachine = stateMachine()
                .startAt("InitialState")
                .state("InitialState", parallelState()
                        .transition(end())
                        .branches(
                                branch()
                                        .comment("Branch one")
                                        .startAt("BranchOneInitial")
                                        .state("BranchOneInitial", succeedState()),
                                branch()
                                        .comment("Branch two")
                                        .startAt("BranchTwoInitial")
                                        .state("BranchTwoInitial", succeedState())
                                 )
                        .catchers(catcher()
                                          .errorEquals("Foo", "Bar")
                                          .transition(next("RecoveryState"))
                                          .resultPath("$.result"),
                                  catcher()
                                          .catchAll()
                                          .transition(next("OtherRecoveryState"))
                                          .resultPath("$.result")
                                 ))
                .state("RecoveryState", succeedState())
                .state("OtherRecoveryState", succeedState())
                .build();

        assertStateMachine(stateMachine, "ParallelStateWithCatchers.json");
    }

    @Test(expected = Exception.class)
    public void stateMachineFromJson_MalformedJson_ThrowsException() {
        StateMachine.fromJson("{");
    }

    private void assertStateMachine(StateMachine stateMachine, String resourcePath) {
        final JsonNode expected = loadExpected(resourcePath);
        assertEquals(expected, serialize(stateMachine));
        assertEquals(expected, serialize(roundTripStateMachine(stateMachine)));
    }

    /**
     * Serializes StateMachine into JSON and deserialize back into a StateMachine from the JSON.
     *
     * @param stateMachine State machine to round trip.
     * @return Round-tripped state machine.
     */
    private StateMachine roundTripStateMachine(StateMachine stateMachine) {
        return StateMachine.fromJson(stateMachine.toJson()).build();
    }

    private JsonNode serialize(StateMachine stateMachine) {
        try {
            return MAPPER.readTree(stateMachine.toJson());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode loadExpected(String resourcePath) {
        return TestResourceLoader.loadAsJson(resourcePath);
    }

}
