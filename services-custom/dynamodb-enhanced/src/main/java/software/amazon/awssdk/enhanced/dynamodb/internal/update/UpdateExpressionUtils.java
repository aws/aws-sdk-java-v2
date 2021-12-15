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
import static software.amazon.awssdk.utils.CollectionUtils.filterMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.UpdateBehaviorTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class UpdateExpressionUtils {

    private UpdateExpressionUtils() {
    }

    private static String keyRef(String key) {
        return "#AMZN_MAPPED_" + EnhancedClientUtils.cleanAttributeName(key);
    }

    private static String valueRef(String value) {
        return ":AMZN_MAPPED_" + EnhancedClientUtils.cleanAttributeName(value);
    }

    private static String ifNotExists(String key, String initValue) {
        return "if_not_exists(" + keyRef(key) + ", " + valueRef(initValue) + ")";
    }

    public static UpdateExpression operationExpression(Map<String, AttributeValue> itemMap,
                                                       TableMetadata tableMetadata,
                                                       List<String> nonRemoveAttributes) {

        Map<String, AttributeValue> setAttributes = filterMap(itemMap, e -> !isNullAttributeValue(e.getValue()));
        UpdateExpression setAttributeExpression = UpdateExpression.builder()
                                                                  .actions(setActionsFor(setAttributes, tableMetadata))
                                                                  .build();

        Map<String, AttributeValue> removeAttributes =
            filterMap(itemMap, e -> isNullAttributeValue(e.getValue()) && !nonRemoveAttributes.contains(e.getKey()));

        UpdateExpression removeAttributeExpression = UpdateExpression.builder()
                                                                     .actions(removeActionsFor(removeAttributes))
                                                                     .build();

        setAttributeExpression.mergeExpression(removeAttributeExpression);
        return setAttributeExpression;
    }

    private static List<SetUpdateAction> setActionsFor(Map<String, AttributeValue> attributesToSet, TableMetadata tableMetadata) {
        return attributesToSet.entrySet()
                              .stream()
                              .map(entry -> setValue(entry.getKey(),
                                                     entry.getValue(),
                                                     UpdateBehaviorTag.resolveForAttribute(entry.getKey(), tableMetadata)))
                              .collect(Collectors.toList());
    }

    private static List<RemoveUpdateAction> removeActionsFor(Map<String, AttributeValue> attributesToSet) {
        return attributesToSet.entrySet()
                              .stream()
                              .map(entry -> remove(entry.getKey()))
                              .collect(Collectors.toList());
    }

    private static RemoveUpdateAction remove(String attributeName) {
        return RemoveUpdateAction.builder()
                                 .path(keyRef(attributeName))
                                 .expressionNames(Collections.singletonMap(keyRef(attributeName), attributeName))
                                 .build();
    }

    private static SetUpdateAction setValue(String attributeName, AttributeValue value, UpdateBehavior updateBehavior) {
        return SetUpdateAction.builder()
                              .path(keyRef(attributeName))
                              .value(behaviorBasedValue(updateBehavior).apply(attributeName))
                              .expressionNames(expressionNamesFor(attributeName))
                              .expressionValues(Collections.singletonMap(valueRef(attributeName), value))
                              .build();
    }

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

    private static Map<String, String> expressionNamesFor(String... attributeNames) {
        return Arrays.stream(attributeNames)
                     .collect(Collectors.toMap(UpdateExpressionUtils::keyRef, Function.identity()));
    }

}