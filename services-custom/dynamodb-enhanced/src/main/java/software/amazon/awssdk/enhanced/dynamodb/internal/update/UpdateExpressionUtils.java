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
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class UpdateExpressionUtils {

    private UpdateExpressionUtils() {
    }

    /**
     * A function to specify an initial value if the attribute represented by 'key' does not exist.
     */
    public static String ifNotExists(String key, String initValue) {
        return "if_not_exists(" + keyRef(key) + ", " + valueRef(initValue) + ")";
    }

    /**
     * Creates a list of SET actions for all attributes supplied in the map.
     */
    public static List<SetAction> setActionsFor(Map<String, AttributeValue> attributesToSet, TableMetadata tableMetadata) {
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
    public static List<RemoveAction> removeActionsFor(Map<String, AttributeValue> attributesToSet) {
        return attributesToSet.entrySet()
                              .stream()
                              .map(entry -> remove(entry.getKey()))
                              .collect(Collectors.toList());
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
    private static Map<String, String> expressionNamesFor(String... attributeNames) {
        return Arrays.stream(attributeNames)
                     .collect(Collectors.toMap(EnhancedClientUtils::keyRef, Function.identity()));
    }

}