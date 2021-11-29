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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.AddUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.DeleteUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.RemoveUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.SetUpdateAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Contains sets of UpdateAction that represent the four DynamoDB update actions: SET, ADD, REMOVE and DELETE.
 *
 * Use this class to build an immutable UpdateExpression with one or more UpdateAction. An UpdateExpression may be merged
 * with another.
 *
 * In order to convert it to the format that DynamoDB accepts, the toExpression() method will create an Expression
 * with a coalesced string representation of its actions, and the ExpressionNames and ExpressionValues maps associated
 * with all present actions.
 *
 * Note: Once an Expression has been obtained, you cannot combine it with another update Expression since they can't be
 * reliably combined using a token.
 *
 * Creating expressions
 * The builder supports three levels of granularity when you create a new UpdateExpression, going from fluent/less verbose
 * to more verbose with maximum flexibility:
 * 1. Use one of the ..For() methods such as removeFor() and directly supply the attribute name(s) and any other required
 * parameters.
 * 2. Add an UpdateAction of the appropriate type and use the pre-built methods to get a syntactically correct expression
 * 3. Add an UpdateAction of the appropriate type and directly supply the low level code and the correct values for the
 * ExpressionNames and ExpressionValues
 *
 * Examples of adding actions for attributes directly:
 * UpdateExpression.builder()
 *                 .addSetActionFor("attr1", value1, UpdateBehavior.WRITE_ALWAYS)
 *                 .removeActions("attr2", "attr3")
 *                 .build();
 *
 * Examples of adding actions using build in syntax support:
 * UpdateExpression.builder()
 *                 .addSetAction(SetUpdateAction.setValue("attr1", value1, UpdateBehavior.WRITE_ALWAYS))
 *                 .addRemoveAction(RemoveUpdateAction.remove("attr2"))
 *                 .build();
 *
 * Examples of adding actions supplying values directly:
 * UpdateExpression.builder()
 *                 .addSetAction(SetUpdateAction.builder()
 *                                              .expression()
 *                                              .expressionNames()
 *                                              .expressionValues()
 *                                              .build())
 *                 .addRemoveAction(RemoveUpdateAction.builder()
 *                                              .expression()
 *                                              .expressionNames()
 *                                              .expressionValues()
 *                                              .build())
 *                 .build();
 *
 * It's recommended to use the first or second version. See the respective UpdateAction in order to understand in detail how each
 * action works.
 *
 * Validation
 * When an UpdateExpression is created or merged with another, the code validates the integrity of the expression to ensure
 * a successful database update.
 * - The same attribute MAY NOT be chosen for updates in more than one action expression. This is checked by verifying that
 * attribute only has one representation in the AttributeNames map.
 * - The same attribute MAY NOT have more than one value. This is checked by verifying that attribute only has one
 * representation in the AttributeValues map.
 *
 * Merging
 * When two UpdateExpression are joined, the actions of each group of UpdateAction, should they exist, are combined; all SET
 * actions from each expression are concatenated, all REMOVE actions etc.
 *
 * The same validations that are applied when an UpdateExpression is created are also applied at merge time.
 */
@SdkPublicApi
public final class UpdateExpression {

    private final Set<RemoveUpdateAction> removeActions;
    private final Set<SetUpdateAction> setActions;
    private final Set<DeleteUpdateAction> deleteActions;
    private final Set<AddUpdateAction> addActions;

    private final Map<String, String> expressionNames;
    private final Map<String, AttributeValue> expressionValues;

    public static final String EQUALS = " = ";
    public static final String ACTION_SEPARATOR = ", ";
    public static final String ACTION_TYPE_SEPARATOR = " ";

    private UpdateExpression(Builder builder) {
        this.setActions = builder.setActions != null ? Collections.unmodifiableSet(builder.setActions) : Collections.emptySet();
        this.addActions = builder.addActions != null ? Collections.unmodifiableSet(builder.addActions) : Collections.emptySet();
        this.removeActions = builder.removeActions != null ? Collections.unmodifiableSet(builder.removeActions) :
                             Collections.emptySet();
        this.deleteActions = builder.deleteActions != null ? Collections.unmodifiableSet(builder.deleteActions) :
                             Collections.emptySet();

        this.expressionNames = validateAndMergeExpressionNames();
        this.expressionValues = validateAndMergeExpressionValues();
    }

    /**
     * Returns a builder for this class
     */
    public static Builder builder() {
        return new Builder();
    }

    //should move these to internal lib
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
     * @param expression1 The first expression to coalesce
     * @param expression2 The second expression to coalesce
     * @return The coalesced expression
     */
    public static UpdateExpression mergeExpressions(UpdateExpression expression1, UpdateExpression expression2) {
        if (expression1 == null) {
            return expression2;
        }

        if (expression2 == null) {
            return expression1;
        }

        return UpdateExpression.builder()
                               .build();
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

        if (removeActions != null ? ! removeActions.equals(that.removeActions) : that.removeActions != null) {
            return false;
        }
        if (setActions != null ? ! setActions.equals(that.setActions) : that.setActions != null) {
            return false;
        }
        if (deleteActions != null ? ! deleteActions.equals(that.deleteActions) : that.deleteActions != null) {
            return false;
        }
        return addActions != null ? addActions.equals(that.addActions) : that.addActions == null;
    }

    @Override
    public int hashCode() {
        int result = removeActions != null ? removeActions.hashCode() : 0;
        result = 31 * result + (setActions != null ? setActions.hashCode() : 0);
        result = 31 * result + (deleteActions != null ? deleteActions.hashCode() : 0);
        result = 31 * result + (addActions != null ? addActions.hashCode() : 0);
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
        List<String> expressions = new ArrayList<>();

        if (!CollectionUtils.isNullOrEmpty(removeActions)) {
            expressions.add(extractActionExpressions(removeActions, RemoveUpdateAction.NAME));
        }
        if (!CollectionUtils.isNullOrEmpty(setActions)) {
            expressions.add(extractActionExpressions(setActions, SetUpdateAction.NAME));
        }
        if (!CollectionUtils.isNullOrEmpty(deleteActions)) {
            expressions.add(extractActionExpressions(deleteActions, DeleteUpdateAction.NAME));
        }
        if (!CollectionUtils.isNullOrEmpty(addActions)) {
            expressions.add(extractActionExpressions(addActions, AddUpdateAction.NAME));
        }

        return Expression.builder()
                         .expression(String.join(ACTION_TYPE_SEPARATOR, expressions))
                         .expressionNames(expressionNames)
                         .expressionValues(expressionValues)
                         .build();
    }


    private static String extractActionExpressions(Set<? extends UpdateAction> updateActions, String actionName) {
        List<String> actionExpressions = updateActions.stream()
                                                      .map(UpdateAction::actionExpression)
                                                      .collect(Collectors.toList());
        return actionName + String.join(ACTION_SEPARATOR, actionExpressions);
    }

    private Map<String, String> validateAndMergeExpressionNames() {
        Map<String, String> combinedExpressionNames = new HashMap<>();
        Map<String, String> removeNames = mergeExpressionNames(removeActions);
        Map<String, String> setNames = mergeExpressionNames(setActions);
        Map<String, String> deleteNames = mergeExpressionNames(deleteActions);
        Map<String, String> addNames = mergeExpressionNames(addActions);
        combinedExpressionNames = Expression.joinNames(combinedExpressionNames, removeNames);
        combinedExpressionNames = Expression.joinNames(combinedExpressionNames, setNames);
        combinedExpressionNames = Expression.joinNames(combinedExpressionNames, deleteNames);
        combinedExpressionNames = Expression.joinNames(combinedExpressionNames, addNames);
        return combinedExpressionNames;
    }

    private static Map<String, String> mergeExpressionNames(Collection<? extends UpdateAction> updateActions) {
        return updateActions.stream()
                            .map(UpdateAction::expressionNames)
                            .reduce(Expression::joinNames)
                            .orElse(null);
    }

    private Map<String, AttributeValue> validateAndMergeExpressionValues() {
        Map<String, AttributeValue> combinedExpressionValues = new HashMap<>();
        Map<String, AttributeValue> setValues = mergeExpressionValues(setActions);
        Map<String, AttributeValue> deleteValues = mergeExpressionValues(deleteActions);
        Map<String, AttributeValue> addValues = mergeExpressionValues(addActions);
        combinedExpressionValues = Expression.joinValues(combinedExpressionValues, setValues);
        combinedExpressionValues = Expression.joinValues(combinedExpressionValues, deleteValues);
        combinedExpressionValues = Expression.joinValues(combinedExpressionValues, addValues);
        return combinedExpressionValues;
    }

    private static Map<String, AttributeValue> mergeExpressionValues(Collection<? extends UpdateAction> updateActions) {
        return updateActions.stream()
                            .map(UpdateAction::expressionValues)
                            .reduce(Expression::joinValues)
                            .orElse(null);
    }

    public static final class Builder {
        private Set<RemoveUpdateAction> removeActions;
        private Set<SetUpdateAction> setActions;
        private Set<DeleteUpdateAction> deleteActions;
        private Set<AddUpdateAction> addActions;

        private Builder() {
        }

        public Builder removeActions(Set<RemoveUpdateAction> removeActions) {
            this.removeActions = removeActions;
            return this;
        }

        public Builder addRemoveAction(RemoveUpdateAction removeAction) {
            if (removeActions == null) {
                removeActions = new HashSet<>();
            }
            this.removeActions.add(removeAction);
            return this;
        }

        public Builder removeActionsFor(Set<String> attributeNames) {
            this.removeActions = attributeNames.stream()
                                               .map(RemoveUpdateAction::remove)
                                               .collect(Collectors.toSet());
            return this;
        }

        public Builder addRemoveActionFor(String attributeName) {
            if (removeActions == null) {
                removeActions = new HashSet<>();
            }
            removeActions.add(RemoveUpdateAction.remove(attributeName));
            return this;
        }

        public Builder setActions(Set<SetUpdateAction> setActions) {
            this.setActions = setActions;
            return this;
        }

        public Builder addSetAction(SetUpdateAction setAction) {
            if (setActions == null) {
                this.setActions = new HashSet<>();
            }
            setActions.add(setAction);
            return this;
        }

        public Builder addSetActionFor(String attributeName, AttributeValue value, UpdateBehavior updateBehavior) {
            if (setActions == null) {
                setActions = new HashSet<>();
            }
            setActions.add(SetUpdateAction.setValue(attributeName, value, updateBehavior));
            return this;
        }

        public Builder addDeleteAction(DeleteUpdateAction deleteAction) {
            if (deleteActions == null) {
                deleteActions = new HashSet<>();
            }
            deleteActions.add(deleteAction);
            return this;
        }

        public Builder addDeleteActionFor(String attributeName, AttributeValue value) {
            if (deleteActions == null) {
                deleteActions = new HashSet<>();
            }
            deleteActions.add(DeleteUpdateAction.removeElements(attributeName, value));
            return this;
        }

        public Builder addAction(AddUpdateAction addAction) {
            if (addActions == null) {
                addActions = new HashSet<>();
            }
            addActions.add(addAction);
            return this;
        }

        public Builder addActionFor(String attributeName, AttributeValue value) {
            if (addActions == null) {
                addActions = new HashSet<>();
            }
            addActions.add(AddUpdateAction.addValue(attributeName, value));
            return this;
        }

        public UpdateExpression build() {
            return new UpdateExpression(this);
        }
    }
}
