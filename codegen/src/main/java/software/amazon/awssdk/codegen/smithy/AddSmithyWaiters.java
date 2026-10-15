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

import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.fasterxml.jackson.jr.stree.JrsValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.smithy.traits.WaiterOverridesTrait;
import software.amazon.awssdk.codegen.smithy.traits.WaiterOverridesTrait.WaiterOverride;
import software.amazon.awssdk.codegen.smithy.traits.WaiterOverridesTrait.WaiterOverrideAcceptor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.waiters.Matcher;
import software.amazon.smithy.waiters.PathMatcher;
import software.amazon.smithy.waiters.WaitableTrait;
import software.amazon.smithy.waiters.Waiter;

/**
 * Builds a service's waiter definitions, keyed by waiter name as in {@code waiters-2.json}. Smithy counterpart to
 * loading that file.
 *
 * <p>Each {@code @waitable} waiter is translated, with {@code maxAttempts} from the operation's
 * {@link WaiterOverridesTrait}, because Smithy waiters have no attempt limit. An override entry with acceptors replaces
 * the waiter, and one with {@code skip} drops it. The fixed delay is {@code minDelay}; {@code maxDelay},
 * documentation, and deprecation have no C2J field and are dropped.
 */
final class AddSmithyWaiters {

    private AddSmithyWaiters() {
    }

    static Map<String, WaiterDefinition> waiters(Model model, ServiceShape service) {
        Map<String, WaiterDefinition> waiters = new TreeMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            String operationName = operation.getId().getName();
            Map<String, Waiter> smithyWaiters = operation.getTrait(WaitableTrait.class)
                                                         .map(WaitableTrait::getWaiters)
                                                         .orElse(Collections.emptyMap());
            Map<String, WaiterOverride> overrides = operation.getTrait(WaiterOverridesTrait.class)
                                                             .map(WaiterOverridesTrait::getWaiters)
                                                             .orElse(Collections.emptyMap());
            Set<String> names = new LinkedHashSet<>(smithyWaiters.keySet());
            names.addAll(overrides.keySet());
            for (String name : names) {
                WaiterDefinition definition = definition(service, operationName, name, smithyWaiters.get(name),
                                                         overrides.get(name));
                if (definition == null) {
                    continue;
                }
                if (waiters.put(name, definition) != null) {
                    throw new IllegalStateException(String.format(
                        "Service %s has more than one waiter named %s", service.getId(), name));
                }
            }
        }
        return waiters;
    }

    private static WaiterDefinition definition(ServiceShape service, String operationName, String name,
                                               Waiter waiter, WaiterOverride override) {
        if (override != null && override.isSkip()) {
            return null;
        }
        if (override != null && override.getAcceptors().isPresent()) {
            return fromOverride(operationName, override);
        }
        if (waiter == null) {
            throw new IllegalStateException(String.format(
                "%s on operation %s of %s sets only maxAttempts for waiter %s, but the operation's @waitable has no "
                + "waiter of that name. Give delay and acceptors to define it, or remove the entry.",
                WaiterOverridesTrait.ID, operationName, service.getId(), name));
        }
        if (override == null) {
            throw new IllegalStateException(String.format(
                "Waiter %s on operation %s of %s has no maxAttempts. Smithy waiters have no attempt limit, so it comes "
                + "from the %s trait, which smithy-build.json adds; regenerate smithy-build.json.",
                name, operationName, service.getId(), WaiterOverridesTrait.ID));
        }
        return translate(service, operationName, name, waiter, override.getMaxAttempts().get());
    }

    private static WaiterDefinition translate(ServiceShape service, String operationName, String name, Waiter waiter,
                                              int maxAttempts) {
        List<Acceptor> acceptors = new ArrayList<>();
        for (software.amazon.smithy.waiters.Acceptor acceptor : waiter.getAcceptors()) {
            Acceptor translated = acceptor.getMatcher().accept(new AcceptorTranslator(service, operationName, name));
            translated.setState(acceptor.getState().toString());
            acceptors.add(translated);
        }
        WaiterDefinition definition = new WaiterDefinition();
        definition.setOperation(operationName);
        definition.setDelay(waiter.getMinDelay());
        definition.setMaxAttempts(maxAttempts);
        definition.setAcceptors(acceptors);
        return definition;
    }

    private static WaiterDefinition fromOverride(String operationName, WaiterOverride override) {
        List<Acceptor> acceptors = new ArrayList<>();
        for (WaiterOverrideAcceptor overrideAcceptor : override.getAcceptors().get()) {
            Acceptor acceptor = new Acceptor();
            acceptor.setMatcher(overrideAcceptor.getMatcher());
            acceptor.setArgument(overrideAcceptor.getArgument().orElse(null));
            acceptor.setState(overrideAcceptor.getState());
            acceptor.setExpected(toJrs(overrideAcceptor.getExpected()));
            acceptors.add(acceptor);
        }
        WaiterDefinition definition = new WaiterDefinition();
        definition.setOperation(operationName);
        definition.setDelay(override.getDelay().get());
        definition.setMaxAttempts(override.getMaxAttempts().get());
        definition.setAcceptors(acceptors);
        return definition;
    }

    /**
     * The value types the C2J loader produces for {@code expected}, which the waiter emitter checks by class.
     */
    private static JrsValue toJrs(Node node) {
        if (node.isBooleanNode()) {
            return node.expectBooleanNode().getValue() ? JrsBoolean.TRUE : JrsBoolean.FALSE;
        }
        if (node.isNumberNode()) {
            NumberNode number = node.expectNumberNode();
            return new JrsNumber(number.isNaturalNumber() ? Long.valueOf(number.getValue().longValue())
                                                          : number.getValue());
        }
        return new JrsString(node.expectStringNode().getValue());
    }

    /**
     * Maps a Smithy matcher to the C2J acceptor that generates the same Java: {@code success} to {@code error} with
     * the negated boolean, {@code errorType} to {@code error} with the type copied as written, and each output
     * comparator to {@code path}, {@code pathAll}, or {@code pathAny}.
     */
    private static final class AcceptorTranslator implements Matcher.Visitor<Acceptor> {
        private final ServiceShape service;
        private final String operationName;
        private final String waiterName;

        private AcceptorTranslator(ServiceShape service, String operationName, String waiterName) {
            this.service = service;
            this.operationName = operationName;
            this.waiterName = waiterName;
        }

        @Override
        public Acceptor visitSuccess(Matcher.SuccessMember success) {
            return acceptor("error", null, success.getValue() ? JrsBoolean.FALSE : JrsBoolean.TRUE);
        }

        @Override
        public Acceptor visitErrorType(Matcher.ErrorTypeMember errorType) {
            String value = errorType.getValue();
            if (value.indexOf('#') >= 0) {
                throw unsupported("an absolute errorType '" + value + "'");
            }
            return acceptor("error", null, new JrsString(value));
        }

        @Override
        public Acceptor visitOutput(Matcher.OutputMember output) {
            PathMatcher path = output.getValue();
            switch (path.getComparator()) {
                case STRING_EQUALS:
                    return acceptor("path", path.getPath(), new JrsString(path.getExpected()));
                case BOOLEAN_EQUALS:
                    return acceptor("path", path.getPath(),
                                    Boolean.parseBoolean(path.getExpected()) ? JrsBoolean.TRUE : JrsBoolean.FALSE);
                case ALL_STRING_EQUALS:
                    return acceptor("pathAll", path.getPath(), new JrsString(path.getExpected()));
                case ANY_STRING_EQUALS:
                    return acceptor("pathAny", path.getPath(), new JrsString(path.getExpected()));
                default:
                    throw unsupported("the comparator " + path.getComparator());
            }
        }

        @Override
        public Acceptor visitInputOutput(Matcher.InputOutputMember inputOutput) {
            throw unsupported("an inputOutput matcher");
        }

        @Override
        public Acceptor visitUnknown(Matcher.UnknownMember unknown) {
            throw unsupported("the matcher '" + unknown.getMemberName() + "'");
        }

        private IllegalStateException unsupported(String what) {
            return new IllegalStateException(String.format(
                "Waiter %s on operation %s of %s uses %s, which the Java waiter runtime cannot evaluate",
                waiterName, operationName, service.getId(), what));
        }

        private static Acceptor acceptor(String matcher, String argument, JrsValue expected) {
            Acceptor acceptor = new Acceptor();
            acceptor.setMatcher(matcher);
            acceptor.setArgument(argument);
            acceptor.setExpected(expected);
            return acceptor;
        }
    }
}
