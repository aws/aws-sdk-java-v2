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

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.valueRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation.NESTED_OBJECT_UPDATE;
import static software.amazon.awssdk.utils.CollectionUtils.filterMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.UpdateBehaviorTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class UpdateExpressionUtils {

    private static final Pattern PATTERN = Pattern.compile(NESTED_OBJECT_UPDATE);

    private UpdateExpressionUtils() {
    }

    /**
     * A function to specify an initial value if the attribute represented by 'key' does not exist.
     */
    public static String ifNotExists(String key, String initValue) {
        return "if_not_exists(" + keyRef(key) + ", " + valueRef(initValue) + ")";
    }

    /**
     * SET actions for every {@code itemMap} entry that is not DynamoDB NULL. For each attribute, {@link UpdateBehavior} is
     * resolved from {@code tableMetadata}.
     */
    static UpdateExpression generateItemSetExpression(Map<String, AttributeValue> itemMap,
                                                      TableMetadata tableMetadata) {
        Map<String, AttributeValue> setAttributes = filterMap(itemMap, e -> !isNullAttributeValue(e.getValue()));
        return UpdateExpression.builder()
                               .actions(setActionsFor(setAttributes, tableMetadata))
                               .build();
    }

    /**
     * REMOVE actions for NULL-valued {@code itemMap} attributes, except names in {@code nonRemoveAttributes} (e.g. already
     * updated elsewhere when merging expressions).
     */
    static UpdateExpression generateItemRemoveExpression(Map<String, AttributeValue> itemMap,
                                                         Collection<String> nonRemoveAttributes) {
        Map<String, AttributeValue> removeAttributes = filterMap(itemMap, e -> isNullAttributeValue(e.getValue())
                                                                               && !nonRemoveAttributes.contains(e.getKey()));
        return UpdateExpression.builder()
                               .actions(removeActionsFor(removeAttributes))
                               .build();
    }

    /**
     * Creates a list of SET actions for all attributes supplied in the map.
     */
    static List<SetAction> setActionsFor(Map<String, AttributeValue> attributesToSet, TableMetadata tableMetadata) {
        return attributesToSet.entrySet()
                              .stream()
                              .map(entry -> setValue(entry.getKey(),
                                                     entry.getValue(),
                                                     UpdateBehaviorTag.resolveForAttribute(entry.getKey(), tableMetadata)))
                              .collect(Collectors.toList());
    }

    /**
     * Creates a list of REMOVE actions for all attributes supplied in the map.
     */
    static List<RemoveAction> removeActionsFor(Map<String, AttributeValue> attributesToSet) {
        return attributesToSet.entrySet()
                              .stream()
                              .map(entry -> remove(entry.getKey()))
                              .collect(Collectors.toList());
    }

    /**
     * Distinct top-level names from non-null expressions (see {@link UpdateExpressionConverter#findAttributeNames}). Skips
     * {@code null} elements; used to avoid REMOVE when those attributes are updated in other expressions.
     */
    static Set<String> attributesPresentInOtherExpressions(Collection<UpdateExpression> updateExpressions) {
        return updateExpressions.stream()
                                .filter(Objects::nonNull)
                                .map(UpdateExpressionConverter::findAttributeNames)
                                .flatMap(List::stream)
                                .collect(Collectors.toSet());
    }

    /**
     * Resolves an update path by substituting expression attribute name placeholders (e.g. {@code #a} → logical name). The result
     * is the full DynamoDB document path as used for overlap detection.
     */
    static String resolveDocumentPath(String fullAttributePath, Map<String, String> expressionNames) {
        String resolvedPath = fullAttributePath;
        Map<String, String> names = expressionNames == null ? Collections.emptyMap() : expressionNames;

        for (Map.Entry<String, String> entry : names.entrySet()) {
            resolvedPath = resolvedPath.replace(entry.getKey(), entry.getValue());
        }
        return resolvedPath;
    }

    /**
     * Creates a REMOVE action for an attribute, using a token as a placeholder for the attribute name.
     */
    private static RemoveAction remove(String attributeName) {
        return RemoveAction.builder()
                           .path(keyRef(attributeName))
                           .expressionNames(Collections.singletonMap(keyRef(attributeName), attributeName))
                           .build();
    }

    /**
     * Creates a SET action for an attribute, using a token as a placeholder for the attribute name.
     *
     * @see UpdateBehavior for information about the values available.
     */
    private static SetAction setValue(String attributeName, AttributeValue value, UpdateBehavior updateBehavior) {
        return SetAction.builder()
                        .path(keyRef(attributeName))
                        .value(behaviorBasedValue(updateBehavior).apply(attributeName))
                        .expressionNames(expressionNamesFor(attributeName))
                        .expressionValues(Collections.singletonMap(valueRef(attributeName), value))
                        .build();
    }

    /**
     * When we know we want to update the attribute no matter if it exists or not, we simply need to replace the value with
     * a value token in the expression. If we only want to set the value if the attribute doesn't exist, we use
     * the DDB function ifNotExists.
     */
    private static Function<String, String> behaviorBasedValue(UpdateBehavior updateBehavior) {
        switch (updateBehavior) {
            case WRITE_ALWAYS:
                return v -> valueRef(v);
            case WRITE_IF_NOT_EXISTS:
                return k -> ifNotExists(k, k);
            default:
                throw new IllegalArgumentException("Unsupported update behavior '" + updateBehavior + "'");
        }
    }

    /**
     * Simple utility method that can create an ExpressionNames map based on a list of attribute names.
     */
    private static Map<String, String> expressionNamesFor(String attributeNames) {
        if (attributeNames.contains(NESTED_OBJECT_UPDATE)) {
            return Arrays.stream(PATTERN.split(attributeNames)).distinct()
                         .collect(Collectors.toMap(EnhancedClientUtils::keyRef, Function.identity()));
        }

        return Collections.singletonMap(keyRef(attributeNames), attributeNames);
    }
}