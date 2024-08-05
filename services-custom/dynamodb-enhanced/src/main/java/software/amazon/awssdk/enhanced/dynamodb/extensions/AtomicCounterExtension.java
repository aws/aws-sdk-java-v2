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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.valueRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.ifNotExists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.AtomicCounterTag;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.AtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * This extension enables atomic counter attributes to be changed in DynamoDb by creating instructions for modifying
 * an existing value or setting a start value. The extension is loaded by default when you instantiate a
 * {@link DynamoDbEnhancedClient} and only needs to be added to the client if you are adding custom extensions to the client.
 * <p>
 * To utilize atomic counters, first create a field in your model that will be used to store the counter.
 * This class field should of type {@link Long} and you need to tag it as an atomic counter:
 * <ul>
 * <li>If you are using the
 * {@link BeanTableSchema}, you should annotate with
 * {@link DynamoDbAtomicCounter}</li>
 * <li>If you are using the {@link StaticTableSchema},
 * use the {@link StaticAttributeTags#atomicCounter()} static attribute tag.</li>
 * </ul>
 * <p>
 * Every time a new update of the record is successfully written to the database, the counter will be updated automatically.
 * By default, the counter starts at 0 and increments by 1 for each update. The tags provide the capability of adjusting
 * the counter start and increment/decrement values such as described in {@link DynamoDbAtomicCounter}.
 * <p>
 * Example 1: Using a bean based table schema
 * <pre>
 * {@code
 * @DynamoDbBean
 * public class CounterRecord {
 *     @DynamoDbAtomicCounter(delta = 5, startValue = 10)
 *     public Long getCustomCounter() {
 *         return customCounter;
 *     }
 * }
 * }
 * </pre>
 * <p>
 * Example 2: Using a static table schema
 * <pre>
 * {@code
 *     private static final StaticTableSchema<AtomicCounterItem> ITEM_MAPPER =
 *         StaticTableSchema.builder(AtomicCounterItem.class)
 *                          .newItemSupplier(AtomicCounterItem::new)
 *                          .addAttribute(Long.class, a -> a.name("defaultCounter")
 *                                                          .getter(AtomicCounterItem::getDefaultCounter)
 *                                                          .setter(AtomicCounterItem::setDefaultCounter)
 *                                                          .addTag(StaticAttributeTags.atomicCounter()))
 *                          .build();
 * }
 * </pre>
 * <p>
 * <b>NOTES: </b>
 * <ul>
 *     <li>When using putItem, the counter will be reset to its start value.</li>
 *     <li>The extension will remove any existing occurrences of the atomic counter attributes from the record during an
 *     <i>updateItem</i> operation. Manually editing attributes marked as atomic counters will have <b>NO EFFECT</b>.</li>
 * </ul>
 */
@SdkPublicApi
public final class AtomicCounterExtension implements DynamoDbEnhancedClientExtension {

    private static final Logger log = Logger.loggerFor(AtomicCounterExtension.class);

    private AtomicCounterExtension() {
    }

    public static AtomicCounterExtension.Builder builder() {
        return new AtomicCounterExtension.Builder();
    }

    /**
     * @param context The {@link DynamoDbExtensionContext.BeforeWrite} context containing the state of the execution.
     * @return WriteModification contains an update expression representing the counters.
     */
    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {

        Map<String, AtomicCounter> counters = AtomicCounterTag.resolve(context.tableMetadata());

        WriteModification.Builder modificationBuilder = WriteModification.builder();

        if (CollectionUtils.isNullOrEmpty(counters)) {
            return modificationBuilder.build();
        }

        switch (context.operationName()) {
            case PUT_ITEM:
                modificationBuilder.transformedItem(addToItem(counters, context.items()));
                break;
            case UPDATE_ITEM:
                modificationBuilder.updateExpression(createUpdateExpression(counters));
                modificationBuilder.transformedItem(filterFromItem(counters, context.items()));
                break;
            default: break;
        }
        return modificationBuilder.build();
    }

    private UpdateExpression createUpdateExpression(Map<String, AtomicCounter> counters) {
        return UpdateExpression.builder()
                               .actions(counters.entrySet().stream().map(this::counterAction).collect(Collectors.toList()))
                               .build();
    }

    private Map<String, AttributeValue> addToItem(Map<String, AtomicCounter> counters, Map<String, AttributeValue> items) {
        Map<String, AttributeValue> itemToTransform = new HashMap<>(items);
        counters.forEach((attribute, counter) -> itemToTransform.put(attribute, attributeValue(counter.startValue().value())));
        return Collections.unmodifiableMap(itemToTransform);
    }

    private Map<String, AttributeValue> filterFromItem(Map<String, AtomicCounter> counters, Map<String, AttributeValue> items) {
        Map<String, AttributeValue> itemToTransform = new HashMap<>(items);
        List<String> removedAttributes = new ArrayList<>();
        for (String attributeName : counters.keySet()) {
            if (itemToTransform.containsKey(attributeName)) {
                itemToTransform.remove(attributeName);
                removedAttributes.add(attributeName);
            }
        }
        if (!removedAttributes.isEmpty()) {
            log.debug(() -> String.format("Filtered atomic counter attributes from existing update item to avoid collisions: %s",
                                          String.join(",", removedAttributes)));
        }
        return Collections.unmodifiableMap(itemToTransform);
    }

    private SetAction counterAction(Map.Entry<String, AtomicCounter> e) {
        String attributeName = e.getKey();
        AtomicCounter counter = e.getValue();

        String startValueName = attributeName + counter.startValue().name();
        String deltaValueName = attributeName + counter.delta().name();
        String valueExpression = ifNotExists(attributeName, startValueName) + " + " + valueRef(deltaValueName);

        AttributeValue startValue = attributeValue(counter.startValue().value() - counter.delta().value());
        AttributeValue deltaValue = attributeValue(counter.delta().value());

        return SetAction.builder()
                        .path(keyRef(attributeName))
                        .value(valueExpression)
                        .putExpressionName(keyRef(attributeName), attributeName)
                        .putExpressionValue(valueRef(startValueName), startValue)
                        .putExpressionValue(valueRef(deltaValueName), deltaValue)
                        .build();
    }

    private AttributeValue attributeValue(long value) {
        return AtomicCounter.CounterAttribute.resolvedValue(value);
    }

    public static final class Builder {
        private Builder() {
        }

        public AtomicCounterExtension build() {
            return new AtomicCounterExtension();
        }
    }
}
