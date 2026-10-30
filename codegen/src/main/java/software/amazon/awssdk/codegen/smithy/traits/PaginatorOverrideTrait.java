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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * {@code software.amazon.awssdk.codegen#paginatorOverride}: the paginator to generate for an operation instead of the
 * one {@code @paginated} produces, for a {@code paginators-1.json} entry that {@code @paginated} cannot express, or
 * {@code skip} to generate none. Each member means what the {@code paginators-1.json} field of the same name means.
 */
@SdkInternalApi
public final class PaginatorOverrideTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("software.amazon.awssdk.codegen#paginatorOverride");

    private static final String SKIP = "skip";
    private static final String INPUT_TOKEN = "inputToken";
    private static final String OUTPUT_TOKEN = "outputToken";
    private static final String RESULT_KEY = "resultKey";
    private static final String MORE_RESULTS = "moreResults";
    private static final String LIMIT_KEY = "limitKey";

    private final ObjectNode node;
    private final boolean skip;
    private final List<String> inputToken;
    private final List<String> outputToken;
    private final List<String> resultKey;
    private final String moreResults;
    private final String limitKey;

    public PaginatorOverrideTrait(ObjectNode node) {
        super(ID, node);
        node.expectNoAdditionalProperties(Arrays.asList(SKIP, INPUT_TOKEN, OUTPUT_TOKEN, RESULT_KEY, MORE_RESULTS,
                                                        LIMIT_KEY));
        this.node = node;
        this.skip = node.getMember(SKIP).map(n -> n.expectBooleanNode().getValue()).orElse(false);
        this.inputToken = strings(node, INPUT_TOKEN);
        this.outputToken = strings(node, OUTPUT_TOKEN);
        this.resultKey = strings(node, RESULT_KEY);
        this.moreResults = node.getMember(MORE_RESULTS).map(n -> n.expectStringNode().getValue()).orElse(null);
        this.limitKey = node.getMember(LIMIT_KEY).map(n -> n.expectStringNode().getValue()).orElse(null);

        if (skip && node.getMembers().size() > 1) {
            throw new ExpectationNotMetException(ID + ": skip cannot be combined with other members", node);
        }
        if (!skip && (isNullOrEmpty(inputToken) || isNullOrEmpty(outputToken))) {
            throw new ExpectationNotMetException(
                ID + " needs inputToken and outputToken unless skip is true. Remove the trait to use @paginated.", node);
        }
    }

    /**
     * True to generate no paginator for the operation. The other members are then absent.
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * Empty only when {@link #isSkip()}.
     */
    public List<String> getInputToken() {
        return inputToken == null ? Collections.emptyList() : inputToken;
    }

    /**
     * Empty only when {@link #isSkip()}.
     */
    public List<String> getOutputToken() {
        return outputToken == null ? Collections.emptyList() : outputToken;
    }

    public Optional<List<String>> getResultKey() {
        return Optional.ofNullable(resultKey);
    }

    public Optional<String> getMoreResults() {
        return Optional.ofNullable(moreResults);
    }

    public Optional<String> getLimitKey() {
        return Optional.ofNullable(limitKey);
    }

    @Override
    protected Node createNode() {
        return node;
    }

    private static List<String> strings(ObjectNode node, String member) {
        return node.getMember(member)
                   .map(n -> n.expectArrayNode().getElements().stream()
                              .map(element -> element.expectStringNode().getValue())
                              .collect(Collectors.toList()))
                   .map(Collections::unmodifiableList)
                   .orElse(null);
    }

    private static boolean isNullOrEmpty(List<String> list) {
        return list == null || list.isEmpty();
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
            return new PaginatorOverrideTrait(value.expectObjectNode());
        }
    }
}
