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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateActionType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Contains sets of {@link UpdateAction} that represent the four DynamoDB update actions: SET, ADD, REMOVE and DELETE.
 * <p>
 * Use this class to build an immutable UpdateExpression with one or more UpdateAction. An UpdateExpression may be merged
 * with another.
 * <p>
 * In order to convert it to the format that DynamoDB accepts, the {@link #toExpression()} method will create an Expression
 * with a coalesced string representation of its actions, and the ExpressionNames and ExpressionValues maps associated
 * with all present actions.
 * <p>
 * <b>Note:</b> Once an Expression has been obtained, you cannot combine it with another update Expression since they can't be
 * reliably combined using a token.
 * <p>
 * <b>Creating expressions</b> Create expressions from scratch or use the built-in support:
 * <ol>
 * <li> Add an UpdateAction of the appropriate type and use static methods to get a syntactically correct expression
 * <li> Add an UpdateAction of the appropriate type and directly supply the low level code and the correct values for the
 * ExpressionNames and ExpressionValues
 * </ol>
 * Examples of adding actions using built-in syntax support:
 * <pre>
 * {@code
 *
 * UpdateExpression.builder()
 *                 .addAction(UpdateAction.setAttribute("attr1", value1, UpdateBehavior.WRITE_ALWAYS))
 *                 .addAction(UpdateAction.removeAttribute("attr2"))
 *                 .build();
 * }
 * </pre>
 *
 * Examples of adding actions supplying values directly:
 * <pre>
 * {@code
 *
 * UpdateExpression.builder()
 *                 .addAction(SetUpdateAction.builder()
 *                                           .attributeName("attr1")
 *                                           .expression("#ref_attr1 = :ref_value1")
 *                                           .expressionNames(expressionNameMap)
 *                                           .expressionValues(expressionValueMap)
 *                                           .build())
 *                 .addAction(RemoveUpdateAction.builder()
 *                                              .attributeName("attr2")
 *                                              .expression("#ref_attr2")
 *                                              .expressionNames(expressionNameMap)
 *                                              .build())
 *                 .build();
 * }
 * </pre>
 *
 * See {@link UpdateAction} for details on how to create actions.
 * <p>
 * <b>Validation</b> When an UpdateExpression is created or merged with another, the code validates the integrity of the
 * expression to ensure a successful database update.
 * <ul>
 * <li> The same attribute MAY NOT be chosen for updates in more than one action expression.
 * <li> The same attribute MAY NOT have more than one value.
 * </ul>
 * <p>
 * <b>Merging</b>
 * When two UpdateExpression are joined, the actions of each group of UpdateAction, should they exist, are combined; all SET
 * actions from each expression are concatenated, all REMOVE actions etc.The same validations that are applied when creating an
 * UpdateExpression are also applied at merge time.
 */
@SdkPublicApi
public final class UpdateExpression {

    private final Map<String, UpdateAction> actions;

    private final Map<String, String> expressionNames;
    private final Map<String, AttributeValue> expressionValues;

    public static final String COMMA = ", ";
    public static final String SPACE = " ";

    private UpdateExpression(Builder builder) {
        this.actions = mapAndValidate(builder.actions);
        this.expressionNames = validateAndMergeExpressionNames();
        this.expressionValues = validateAndMergeExpressionValues();
    }

    private Map<String, UpdateAction> actions() {
        return actions;
    }

    public Optional<UpdateAction> updateActionFor(String attributeName) {
        return Optional.ofNullable(actions.get(attributeName));
    }

    /**
     * Returns a builder for this class
     */
    public static Builder builder() {
        return new Builder();
    }

    public static String keyRef(String key) {
        return "#AMZN_MAPPED_" + EnhancedClientUtils.cleanAttributeName(key);
    }

    public static String listKeyRef(String key, int index) {
        return keyRef(key) + "[" + index + "]";
    }

    public static String valueRef(String value) {
        return ":AMZN_MAPPED_" + EnhancedClientUtils.cleanAttributeName(value);
    }

    /**
     * Coalesces two update expressions into a single expression.
     *
     * @param primaryExpression The first expression to coalesce
     * @param secondaryExpression The second expression to coalesce
     * @return The coalesced expression
     */
    public static UpdateExpression mergeExpressions(UpdateExpression primaryExpression, UpdateExpression secondaryExpression) {
        if (primaryExpression == null) {
            return secondaryExpression;
        }

        if (secondaryExpression == null) {
            return primaryExpression;
        }

        List<String> duplicates = secondaryExpression.actions().keySet().stream()
                                                     .filter(attribute -> primaryExpression.actions().containsKey(attribute))
                                                     .collect(Collectors.toList());
        validateNoDuplicates(duplicates);

        Collection<UpdateAction> mergedUpdateActions = new ArrayList<>(primaryExpression.actions().values());
        mergedUpdateActions.addAll(secondaryExpression.actions().values());
        return builder().actions(mergedUpdateActions).build();
    }

    private static Map<String, UpdateAction> mapAndValidate(Collection<UpdateAction> unvalidatedActions) {
        Map<String, UpdateAction> mappedValidatedActions = new HashMap<>();
        if (unvalidatedActions == null) {
            return mappedValidatedActions;
        }

        List<String> duplicates = new ArrayList<>();
        for (UpdateAction unvalidatedAction : unvalidatedActions) {
            if (mappedValidatedActions.containsKey(unvalidatedAction.attributeName())) {
                duplicates.add(unvalidatedAction.attributeName());
            } else {
                mappedValidatedActions.put(unvalidatedAction.attributeName(), unvalidatedAction);
            }
        }
        validateNoDuplicates(duplicates);
        return mappedValidatedActions;
    }

    private static void validateNoDuplicates(List<String> duplicates) {
        if (duplicates.isEmpty()) {
            return;
        }
        throw new IllegalArgumentException(String.format("UpdateExpression must only contain one unique action per attribute. "
                                                         + "Found the following duplicate attributes: %s",
                                                         String.join(", ", duplicates)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdateExpression that = (UpdateExpression) o;

        return actions != null ? actions.equals(that.actions) : that.actions == null;
    }

    @Override
    public int hashCode() {
        int result = actions != null ? actions.hashCode() : 0;
        return result;
    }

    /**
     * Returns an {@link Expression} where all update actions in the UpdateExpression have been concatenated according
     * to the rules of DDB Update Expressions, and all expression names and values have been combined into single maps,
     * respectively.
     *
     * Observe that the resulting expression string should never be joined with another expression string, independently
     * of whether it represents an update expression, conditional expression or another type of expression, since once
     * the string is generated that update expression is the final format accepted by DDB.
     *
     * @return an Expression representing the concatenation of all actions in this UpdateExpression
     */
    public Expression toExpression() {
        List<String> actionTypeExpressions = new ArrayList<>();

        for (UpdateActionType actionType : UpdateActionType.values()) {
            List<String> actionsForType = actions.values().stream()
                                                 .filter(action -> action.type() == actionType)
                                                 .map(UpdateAction::expression)
                                                 .collect(Collectors.toList());

            if (!CollectionUtils.isNullOrEmpty(actionsForType)) {
                actionTypeExpressions.add(actionType.name() + SPACE + String.join(COMMA, actionsForType));
            }
        }

        return Expression.builder()
                         .expression(String.join(SPACE, actionTypeExpressions))
                         .expressionNames(expressionNames)
                         .expressionValues(expressionValues)
                         .build();
    }

    private Map<String, String> validateAndMergeExpressionNames() {
        return actions.values().stream()
                      .map(UpdateAction::expressionNames)
                      .reduce(Expression::joinNames)
                      .orElseGet(HashMap::new);
    }

    private Map<String, AttributeValue> validateAndMergeExpressionValues() {
        return actions.values().stream()
                      .map(UpdateAction::expressionValues)
                      .reduce(Expression::joinValues)
                      .orElseGet(HashMap::new);
    }

    public static final class Builder {
        private Collection<UpdateAction> actions;

        private Builder() {
        }

        public Builder addAction(UpdateAction action) {
            if (this.actions == null) {
                this.actions = new ArrayList<>();
            }
            actions.add(action);
            return this;
        }

        public Builder actions(Collection<UpdateAction> actions) {
            this.actions = actions;
            return this;
        }

        public Builder actions(UpdateAction... actions) {
            this.actions = Arrays.asList(actions);
            return this;
        }

        public UpdateExpression build() {
            return new UpdateExpression(this);
        }
    }
}
