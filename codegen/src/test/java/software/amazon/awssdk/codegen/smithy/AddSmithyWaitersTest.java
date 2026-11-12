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

package software.amazon.awssdk.codegen.smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsString;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class AddSmithyWaitersTest {

    private static final ClassLoader LOADER = AddSmithyWaitersTest.class.getClassLoader();

    private static Map<String, WaiterDefinition> waiters(String operationTraits) {
        String src = "$version: \"2.0\"\nnamespace demo\n\n"
                     + "use smithy.waiters#waitable\n"
                     + "use software.amazon.awssdk.codegen#waiterOverrides\n\n"
                     + "service DemoService { version: \"2024-01-01\", operations: [GetThing] }\n\n"
                     + "@readonly\n"
                     + operationTraits
                     + "operation GetThing { input: GetThingInput, output: GetThingOutput, errors: [NotFound] }\n"
                     + "structure GetThingInput { id: String }\n"
                     + "structure GetThingOutput { status: String, ready: Boolean, statuses: Statuses }\n"
                     + "list Statuses { member: String }\n"
                     + "@error(\"client\") structure NotFound {}\n";
        Model model = Model.assembler(LOADER).discoverModels(LOADER).addUnparsedModel("test.smithy", src)
                           .assemble().unwrap();
        return AddSmithyWaiters.waiters(model, model.expectShape(ShapeId.from("demo#DemoService"), ServiceShape.class));
    }

    @Test
    void waitableWaiter_translatesEachMatcher() {
        WaiterDefinition waiter = waiters(
            "@waitable(ThingReady: { minDelay: 5, acceptors: [\n"
            + "  { state: \"success\", matcher: { output: { path: \"status\", expected: \"READY\", "
            + "comparator: \"stringEquals\" } } }\n"
            + "  { state: \"success\", matcher: { output: { path: \"ready\", expected: \"true\", "
            + "comparator: \"booleanEquals\" } } }\n"
            + "  { state: \"failure\", matcher: { output: { path: \"statuses\", expected: \"FAILED\", "
            + "comparator: \"anyStringEquals\" } } }\n"
            + "  { state: \"success\", matcher: { output: { path: \"statuses\", expected: \"DONE\", "
            + "comparator: \"allStringEquals\" } } }\n"
            + "  { state: \"retry\", matcher: { errorType: \"NotFound\" } }\n"
            + "  { state: \"success\", matcher: { success: true } }\n"
            + "  { state: \"failure\", matcher: { success: false } }\n"
            + "]})\n"
            + "@waiterOverrides(ThingReady: { maxAttempts: 40 })\n").get("ThingReady");

        assertThat(waiter.getOperation()).isEqualTo("GetThing");
        assertThat(waiter.getDelay()).isEqualTo(5);
        assertThat(waiter.getMaxAttempts()).isEqualTo(40);
        assertThat(waiter.getAcceptors())
            .extracting(Acceptor::getMatcher, Acceptor::getArgument, Acceptor::getState, a -> a.getExpected().asText(),
                        a -> a.getExpected().getClass())
            .containsExactly(tuple("path", "status", "success", "READY", JrsString.class),
                             tuple("path", "ready", "success", "true", JrsBoolean.class),
                             tuple("pathAny", "statuses", "failure", "FAILED", JrsString.class),
                             tuple("pathAll", "statuses", "success", "DONE", JrsString.class),
                             tuple("error", null, "retry", "NotFound", JrsString.class),
                             tuple("error", null, "success", "false", JrsBoolean.class),
                             tuple("error", null, "failure", "true", JrsBoolean.class));
    }

    @Test
    void absentMinDelay_usesTheSmithyDefault() {
        WaiterDefinition waiter = waiters(
            "@waitable(ThingReady: { acceptors: [{ state: \"success\", matcher: { success: true } }] })\n"
            + "@waiterOverrides(ThingReady: { maxAttempts: 3 })\n").get("ThingReady");

        assertThat(waiter.getDelay()).isEqualTo(2);
    }

    @Test
    void waiterWithoutOverride_failsForMissingMaxAttempts() {
        assertThatThrownBy(() -> waiters(
            "@waitable(ThingReady: { acceptors: [{ state: \"success\", matcher: { success: true } }] })\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ThingReady")
            .hasMessageContaining("has no maxAttempts")
            .hasMessageContaining("regenerate smithy-build.json");
    }

    @Test
    void overrideWithAcceptors_replacesTheWaitableWaiter() {
        WaiterDefinition waiter = waiters(
            "@waitable(ThingReady: { acceptors: [{ state: \"success\", matcher: { success: true } }] })\n"
            + "@waiterOverrides(ThingReady: { maxAttempts: 20, delay: 5, acceptors: [\n"
            + "  { matcher: \"status\", expected: 200, state: \"success\" }\n"
            + "  { matcher: \"status\", expected: 301, state: \"success\" }\n"
            + "  { matcher: \"error\", expected: \"NotFound\", state: \"retry\" }\n"
            + "  { matcher: \"path\", argument: \"ready\", expected: true, state: \"success\" }\n"
            + "]})\n").get("ThingReady");

        assertThat(waiter.getOperation()).isEqualTo("GetThing");
        assertThat(waiter.getDelay()).isEqualTo(5);
        assertThat(waiter.getMaxAttempts()).isEqualTo(20);
        assertThat(waiter.getAcceptors())
            .extracting(Acceptor::getMatcher, Acceptor::getArgument, Acceptor::getState, a -> a.getExpected().asText(),
                        a -> a.getExpected().getClass())
            .containsExactly(tuple("status", null, "success", "200", JrsNumber.class),
                             tuple("status", null, "success", "301", JrsNumber.class),
                             tuple("error", null, "retry", "NotFound", JrsString.class),
                             tuple("path", "ready", "success", "true", JrsBoolean.class));
    }

    @Test
    void fullOverrideWithoutWaitableWaiter_isUsed() {
        Map<String, WaiterDefinition> waiters = waiters(
            "@waiterOverrides(ThingExists: { maxAttempts: 20, delay: 5, acceptors: [\n"
            + "  { matcher: \"status\", expected: 200, state: \"success\" }\n"
            + "]})\n");

        assertThat(waiters).containsOnlyKeys("ThingExists");
        assertThat(waiters.get("ThingExists").getOperation()).isEqualTo("GetThing");
    }

    @Test
    void maxAttemptsOnlyOverrideWithoutWaitableWaiter_fails() {
        assertThatThrownBy(() -> waiters("@waiterOverrides(ThingExists: { maxAttempts: 20 })\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ThingExists")
            .hasMessageContaining("has no waiter of that name");
    }

    @Test
    void skipOverride_dropsTheWaitableWaiter() {
        Map<String, WaiterDefinition> waiters = waiters(
            "@waitable(ThingReady: { acceptors: [{ state: \"success\", matcher: { success: true } }] })\n"
            + "@waiterOverrides(ThingReady: { skip: true })\n");

        assertThat(waiters).isEmpty();
    }

    @Test
    void inputOutputMatcher_fails() {
        assertThatThrownBy(() -> waiters(
            "@waitable(ThingReady: { acceptors: [{ state: \"success\", matcher: { inputOutput: { "
            + "path: \"input.id == output.status\", expected: \"true\", comparator: \"booleanEquals\" } } }] })\n"
            + "@waiterOverrides(ThingReady: { maxAttempts: 3 })\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("inputOutput");
    }

    @Test
    void absoluteErrorType_fails() {
        assertThatThrownBy(() -> waiters(
            "@waitable(ThingReady: { acceptors: [\n"
            + "  { state: \"success\", matcher: { success: true } }\n"
            + "  { state: \"retry\", matcher: { errorType: \"demo#NotFound\" } }\n"
            + "] })\n"
            + "@waiterOverrides(ThingReady: { maxAttempts: 3 })\n"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("absolute errorType 'demo#NotFound'");
    }
}
