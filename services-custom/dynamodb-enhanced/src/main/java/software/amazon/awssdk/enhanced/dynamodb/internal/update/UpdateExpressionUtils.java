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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.UpdateBehaviorTag;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class UpdateExpressionUtils {

    private UpdateExpressionUtils() {
    }

    public static Map<String, String> expressionNamesFor(String... attributeNames) {
        return Arrays.stream(attributeNames)
                     .collect(Collectors.toMap(UpdateExpression::keyRef, Function.identity()));
    }

    public static Map<String, String> expressionAttributeNamesFrom(Map<String, AttributeValue> attributeValues) {
        return attributeValues.keySet()
                              .stream()
                              .collect(Collectors.toMap(UpdateExpression::keyRef, Function.identity()));
    }

    public static Map<String, AttributeValue> expressionAttributeValuesFrom(Map<String, AttributeValue> attributeValues) {
        return attributeValues.entrySet()
                              .stream()
                              .collect(Collectors.toMap(entry -> UpdateExpression.valueRef(entry.getKey()),
                                                        Map.Entry::getValue));
    }

    /**
     * Creates an UpdateExpression containing the REMOVE action for the supplied attributes.
     */
    public static UpdateExpression removeExpression(Map<String, AttributeValue> attributeValues) {
        List<UpdateAction> removeActions = attributeValues.keySet().stream()
                                                          .map(UpdateAction::removeAttribute)
                                                          .collect(Collectors.toList());
        return UpdateExpression.builder()
                               .actions(removeActions)
                               .build();
    }

    /**
     * Creates an UpdateExpression containing the SET action for the supplied attributes and their values.
     */
    public static UpdateExpression setExpression(Map<String, AttributeValue> attributeValues, TableMetadata tableMetadata) {
        List<UpdateAction> setActions = attributeValues.entrySet().stream()
                                                          .map(e -> setAction(e, tableMetadata))
                                                          .collect(Collectors.toList());
        return UpdateExpression.builder()
                               .actions(setActions)
                               .build();
    }

    private static UpdateAction setAction(Map.Entry<String, AttributeValue> attributeValue, TableMetadata tableMetadata) {
        UpdateBehavior behavior = UpdateBehaviorTag.resolveForAttribute(attributeValue.getKey(), tableMetadata);
        return UpdateAction.setAttribute(attributeValue.getKey(), attributeValue.getValue(), behavior);
    }

    // public static Expression generateUpdateExpression(Map<String, AttributeValue> attributeValuesToUpdate,
    //                                                   TableMetadata tableMetadata) {
    //
    //     List<String> updateSetActions = new ArrayList<>();
    //     List<String> updateRemoveActions = new ArrayList<>();
    //
    //     Map<String, AttributeValue> additionalAttributeValues = new HashMap<>();
    //     List<String> noValueAttributes = new ArrayList<>();
    //
    //     attributeValuesToUpdate.forEach((key, value) -> {
    //         AtomicCounter counter = AtomicCounterTag.resolveForAttribute(key, tableMetadata);
    //         if (isAttributeAtomicCounter(counter)) {
    //             String expression = keyRef(key) + EQUALS + ifNotExists(key, key + counter.startValue().name()) +
    //                                 addValue(key + counter.delta().name());
    //             additionalAttributeValues.put(valueRef(key + counter.startValue().name()),
    //                                           AtomicCounter.CounterAttribute.resolvedValue(
    //                                               counter.startValue().value() - counter.delta().value()));
    //             additionalAttributeValues.put(valueRef(key + counter.delta().name()),
    //                                           AtomicCounter.CounterAttribute.resolvedValue(counter.delta().value()));
    //             noValueAttributes.add(key);
    //             updateSetActions.add(expression);
    //         } else
    //             if (!isNullAttributeValue(value)) {
    //             UpdateBehavior updateBehavior = UpdateBehaviorTag.resolveForAttribute(key, tableMetadata);
    //             String expression = keyRef(key) + EQUALS + behaviorBasedValue(updateBehavior).apply(key);
    //             updateSetActions.add(expression);
    //         } else {
    //             updateRemoveActions.add(keyRef(key));
    //         }
    //     });
    //
    //     Map<String, AttributeValue> expressionAttributeValues =
    //         createExpressionAttributeValues(attributeValuesToUpdate, additionalAttributeValues);
    //
    //     Map<String, String> expressionAttributeNames =
    //         attributeValuesToUpdate.keySet().stream().collect(Collectors.toMap(UpdateExpressionFactory::keyRef, key -> key));
    //
    //     return Expression.builder()
    //                      .expression(createUpdateExpression(updateSetActions, updateRemoveActions))
    //                      .expressionValues(Collections.unmodifiableMap(expressionAttributeValues))
    //                      .expressionNames(expressionAttributeNames)
    //                      .build();
    // }
    //
    // private static String addValue(String value) {
    //     return " + " + valueRef(value);
    // }
    // private static boolean isAttributeAtomicCounter(AtomicCounter counter) {
    //     return counter != null;
    // }
    //
    //
    // private static String createUpdateExpression(List<String> updateSetActions, List<String> updateRemoveActions) {
    //     List<String> updateActions = new ArrayList<>();
    //
    //     if (!updateSetActions.isEmpty()) {
    //         updateActions.add("SET " + String.join(", ", updateSetActions));
    //     }
    //
    //     if (!updateRemoveActions.isEmpty()) {
    //         updateActions.add("REMOVE " + String.join(", ", updateRemoveActions));
    //     }
    //
    //     return String.join(" ", updateActions);
    // }
    //
    // private static Map<String, AttributeValue> createExpressionAttributeValues(Map<String, AttributeValue> attributeValues,
    //                                                                            Map<String, AttributeValue>
    //                                                                            extraAttributeValues) {
    //     Map<String, AttributeValue> expressionAttributeValues =
    //         attributeValues.entrySet()
    //                                .stream()
    //                                .filter(entry -> !isNullAttributeValue(entry.getValue()))
    //                          //      .filter(entry -> !noValueAttributes.contains(entry.getKey()))
    //                                .collect(Collectors.toMap(
    //                                    entry -> valueRef(entry.getKey()),
    //                                    Map.Entry::getValue));
    //     return Expression.joinValues(expressionAttributeValues, extraAttributeValues);
    // }
    //
    // private static Function<String, String> behaviorBasedValue(UpdateBehavior updateBehavior) {
    //     switch (updateBehavior) {
    //         case WRITE_ALWAYS:
    //             return v -> valueRef(v);
    //         case WRITE_IF_NOT_EXISTS:
    //             return k -> ifNotExists(k, k);
    //         default:
    //             throw new IllegalArgumentException("Unsupported update behavior '" + updateBehavior + "'");
    //     }
    // }

}
