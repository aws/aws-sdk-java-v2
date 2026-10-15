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

package software.amazon.awssdk.codegen.smithy.traits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * {@code software.amazon.awssdk.codegen#waiterOverrides}: Java-only data for an operation's waiters, keyed by waiter
 * name. Smithy waiters have no attempt limit, so every waiter takes {@code maxAttempts} from here. An entry with
 * {@code delay} and {@code acceptors} replaces the {@code @waitable} waiter, for a {@code waiters-2.json} waiter that
 * {@code @waitable} cannot express. {@code skip} generates no waiter.
 */
@SdkInternalApi
public final class WaiterOverridesTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("software.amazon.awssdk.codegen#waiterOverrides");

    private final ObjectNode node;
    private final Map<String, WaiterOverride> waiters;

    public WaiterOverridesTrait(ObjectNode node) {
        super(ID, node);
        this.node = node;
        Map<String, WaiterOverride> parsed = new LinkedHashMap<>();
        node.getStringMap().forEach((name, value) -> parsed.put(name, new WaiterOverride(name, value.expectObjectNode())));
        this.waiters = Collections.unmodifiableMap(parsed);
    }

    /**
     * The entries, keyed by waiter name.
     */
    public Map<String, WaiterOverride> getWaiters() {
        return waiters;
    }

    @Override
    protected Node createNode() {
        return node;
    }

    /**
     * One waiter's entry: {@code skip} alone, or {@code maxAttempts} with optional {@code delay} and
     * {@code acceptors}, which come together.
     */
    public static final class WaiterOverride {
        private static final String SKIP = "skip";
        private static final String MAX_ATTEMPTS = "maxAttempts";
        private static final String DELAY = "delay";
        private static final String ACCEPTORS = "acceptors";

        private final boolean skip;
        private final Integer maxAttempts;
        private final Integer delay;
        private final List<WaiterOverrideAcceptor> acceptors;

        private WaiterOverride(String name, ObjectNode node) {
            node.expectNoAdditionalProperties(Arrays.asList(SKIP, MAX_ATTEMPTS, DELAY, ACCEPTORS));
            this.skip = node.getMember(SKIP).map(n -> n.expectBooleanNode().getValue()).orElse(false);
            this.maxAttempts = node.getMember(MAX_ATTEMPTS).map(n -> n.expectNumberNode().getValue().intValue())
                                   .orElse(null);
            this.delay = node.getMember(DELAY).map(n -> n.expectNumberNode().getValue().intValue()).orElse(null);
            this.acceptors = node.getMember(ACCEPTORS).map(WaiterOverride::acceptors).orElse(null);

            String context = ID + " entry '" + name + "'";
            if (skip) {
                if (node.getMembers().size() > 1) {
                    throw new ExpectationNotMetException(context + ": skip cannot be combined with other members",
                                                         node);
                }
                return;
            }
            if (maxAttempts == null || maxAttempts < 1) {
                throw new ExpectationNotMetException(context + " needs maxAttempts of at least 1 unless skip is true",
                                                     node);
            }
            if ((delay == null) != (acceptors == null)) {
                throw new ExpectationNotMetException(context + ": delay and acceptors replace the @waitable waiter "
                                                     + "together, so give both or neither", node);
            }
            if (delay != null && delay < 0) {
                throw new ExpectationNotMetException(context + ": delay cannot be negative", node);
            }
            if (acceptors != null && acceptors.isEmpty()) {
                throw new ExpectationNotMetException(context + ": acceptors cannot be empty", node);
            }
        }

        private static List<WaiterOverrideAcceptor> acceptors(Node node) {
            List<WaiterOverrideAcceptor> acceptors = new ArrayList<>();
            node.expectArrayNode().getElements()
                .forEach(element -> acceptors.add(new WaiterOverrideAcceptor(element.expectObjectNode())));
            return Collections.unmodifiableList(acceptors);
        }

        /**
         * True to generate no waiter with this name. The other members are then absent.
         */
        public boolean isSkip() {
            return skip;
        }

        /**
         * Present unless {@link #isSkip()}.
         */
        public Optional<Integer> getMaxAttempts() {
            return Optional.ofNullable(maxAttempts);
        }

        /**
         * The fixed delay in seconds of a replacing waiter. Present exactly when {@link #getAcceptors()} is.
         */
        public Optional<Integer> getDelay() {
            return Optional.ofNullable(delay);
        }

        /**
         * The acceptors of a replacing waiter, in order.
         */
        public Optional<List<WaiterOverrideAcceptor>> getAcceptors() {
            return Optional.ofNullable(acceptors);
        }
    }

    /**
     * A {@code waiters-2.json} acceptor.
     */
    public static final class WaiterOverrideAcceptor {
        private static final String MATCHER = "matcher";
        private static final String ARGUMENT = "argument";
        private static final String STATE = "state";
        private static final String EXPECTED = "expected";
        private static final Set<String> MATCHERS =
            new HashSet<>(Arrays.asList("status", "error", "path", "pathAll", "pathAny"));
        private static final Set<String> STATES = new HashSet<>(Arrays.asList("success", "failure", "retry"));

        private final String matcher;
        private final String argument;
        private final String state;
        private final Node expected;

        private WaiterOverrideAcceptor(ObjectNode node) {
            node.expectNoAdditionalProperties(Arrays.asList(MATCHER, ARGUMENT, STATE, EXPECTED));
            this.matcher = node.expectStringMember(MATCHER).getValue();
            this.argument = node.getStringMember(ARGUMENT).map(n -> n.getValue()).orElse(null);
            this.state = node.expectStringMember(STATE).getValue();
            this.expected = node.expectMember(EXPECTED);
            if (!MATCHERS.contains(matcher)) {
                throw new ExpectationNotMetException(ID + ": unknown acceptor matcher '" + matcher + "'", node);
            }
            if (!STATES.contains(state)) {
                throw new ExpectationNotMetException(ID + ": unknown acceptor state '" + state + "'", node);
            }
            if (!expected.isStringNode() && !expected.isNumberNode() && !expected.isBooleanNode()) {
                throw new ExpectationNotMetException(ID + ": acceptor expected must be a string, number, or boolean",
                                                     expected);
            }
        }

        public String getMatcher() {
            return matcher;
        }

        public Optional<String> getArgument() {
            return Optional.ofNullable(argument);
        }

        public String getState() {
            return state;
        }

        /**
         * A string, number, or boolean node.
         */
        public Node getExpected() {
            return expected;
        }
    }

    /**
     * Registered in {@code META-INF/services} so Smithy builds this class when it loads the trait.
     */
    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return new WaiterOverridesTrait(value.expectObjectNode());
        }
    }
}
