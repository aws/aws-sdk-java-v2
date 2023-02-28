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

package software.amazon.awssdk.enhanced.dynamodb.internal.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 *
 * In order to convert it to the format that DynamoDB accepts, the toExpression() method will create an Expression
 * with a coalesced string representation of its actions, and the ExpressionNames and ExpressionValues maps associated
 * with all present actions.
 *
 * Note: Once an Expression has been obtained, you cannot combine it with another update Expression since they can't be
 * reliably combined using a token.
 *
 *
 * Validation
 * When an UpdateExpression is created or merged with another, the code validates the integrity of the expression to ensure
 * a successful database update.
 * - The same attribute MAY NOT be chosen for updates in more than one action expression. This is checked by verifying that
 * attribute only has one representation in the AttributeNames map.
 * - The same attribute MAY NOT have more than one value. This is checked by verifying that attribute only has one
 * representation in the AttributeValues map.
 */
@SdkInternalApi
public final class UpdateExpressionConverter {

    private static final String REMOVE = "REMOVE ";
    private static final String SET = "SET ";
    private static final String DELETE = "DELETE ";
    private static final String ADD = "ADD ";

    private static final String ACTION_SEPARATOR = ", ";
    private static final String GROUP_SEPARATOR = " ";
    private static final char DOT = '.';
    private static final char LEFT_BRACKET = '[';

    private UpdateExpressionConverter() {
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
    public static Expression toExpression(UpdateExpression expression) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        Map<String, AttributeValue> expressionValues = mergeExpressionValues(expression);
        Map<String, String> expressionNames = mergeExpressionNames(expression);
        List<String> groupExpressions = groupExpressions(expression);

        return Expression.builder()
                         .expression(String.join(GROUP_SEPARATOR, groupExpressions))
                         .expressionNames(expressionNames)
                         .expressionValues(expressionValues)
                         .build();
    }

    /**
     * Attempts to find the list of attributes associated with update actions for the supplied {@link UpdateExpression} by
     * looking at
     * the combined collection of paths and ExpressionName values. Because attribute names can be composed of nested
     * attribute references and list references, the leftmost part will be returned if composition is detected.
     * <p>
     * Examples: The expression contains a {@link DeleteAction} with a path value of 'MyAttribute[1]'; the list returned
     * will have 'MyAttribute' as an element.}
     *
     * @return A list of top level attribute names that have update actions associated.
     */
    public static List<String> findAttributeNames(UpdateExpression updateExpression) {
        if (updateExpression == null) {
            return Collections.emptyList();
        }
        List<String> attributeNames = listPathsWithoutTokens(updateExpression);
        List<String> attributeNamesFromTokens = listAttributeNamesFromTokens(updateExpression);
        attributeNames.addAll(attributeNamesFromTokens);
        return attributeNames;
    }

    private static List<String> groupExpressions(UpdateExpression expression) {
        List<String> groupExpressions = new ArrayList<>();
        if (!expression.setActions().isEmpty()) {
            groupExpressions.add(SET + expression.setActions().stream()
                                                 .map(a -> String.format("%s = %s", a.path(), a.value()))
                                                 .collect(Collectors.joining(ACTION_SEPARATOR)));
        }
        if (!expression.removeActions().isEmpty()) {
            groupExpressions.add(REMOVE + expression.removeActions().stream()
                                                    .map(RemoveAction::path)
                                                    .collect(Collectors.joining(ACTION_SEPARATOR)));
        }
        if (!expression.deleteActions().isEmpty()) {
            groupExpressions.add(DELETE + expression.deleteActions().stream()
                                                    .map(a -> String.format("%s %s", a.path(), a.value()))
                                                    .collect(Collectors.joining(ACTION_SEPARATOR)));
        }
        if (!expression.addActions().isEmpty()) {
            groupExpressions.add(ADD + expression.addActions().stream()
                                                 .map(a -> String.format("%s %s", a.path(), a.value()))
                                                 .collect(Collectors.joining(ACTION_SEPARATOR)));
        }
        return groupExpressions;
    }

    private static Stream<Map<String, String>> streamOfExpressionNames(UpdateExpression expression) {
        return Stream.concat(expression.setActions().stream().map(SetAction::expressionNames),
                             Stream.concat(expression.removeActions().stream().map(RemoveAction::expressionNames),
                                           Stream.concat(expression.deleteActions().stream()
                                                                   .map(DeleteAction::expressionNames),
                                                         expression.addActions().stream()
                                                                   .map(AddAction::expressionNames))));
    }

    private static Map<String, AttributeValue> mergeExpressionValues(UpdateExpression expression) {
        return streamOfExpressionValues(expression)
            .reduce(Expression::joinValues)
            .orElseGet(Collections::emptyMap);
    }

    private static Stream<Map<String, AttributeValue>> streamOfExpressionValues(UpdateExpression expression) {
        return Stream.concat(expression.setActions().stream().map(SetAction::expressionValues),
                             Stream.concat(expression.deleteActions().stream().map(DeleteAction::expressionValues),
                                           expression.addActions().stream().map(AddAction::expressionValues)));
    }

    private static Map<String, String> mergeExpressionNames(UpdateExpression expression) {
        return streamOfExpressionNames(expression)
            .reduce(Expression::joinNames)
            .orElseGet(Collections::emptyMap);
    }

    private static List<String> listPathsWithoutTokens(UpdateExpression expression) {
        return Stream.concat(expression.setActions().stream().map(SetAction::path),
                             Stream.concat(expression.removeActions().stream().map(RemoveAction::path),
                                           Stream.concat(expression.deleteActions().stream().map(DeleteAction::path),
                                                         expression.addActions().stream().map(AddAction::path))))
                     .map(UpdateExpressionConverter::removeNestingAndListReference)
                     .filter(attributeName -> !attributeName.contains("#"))
                     .collect(Collectors.toList());
    }

    private static List<String> listAttributeNamesFromTokens(UpdateExpression updateExpression) {
        return mergeExpressionNames(updateExpression).values().stream()
                                                     .map(UpdateExpressionConverter::removeNestingAndListReference)
                                                     .collect(Collectors.toList());
    }

    private static String removeNestingAndListReference(String attributeName) {
        return attributeName.substring(0, getRemovalIndex(attributeName));
    }

    private static int getRemovalIndex(String attributeName) {
        for (int i = 0; i < attributeName.length(); i++) {
            char c = attributeName.charAt(i);
            if (c == DOT || c == LEFT_BRACKET) {
                return attributeName.indexOf(c);
            }
        }
        return attributeName.length();
    }
}
