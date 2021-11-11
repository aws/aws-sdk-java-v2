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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.AtomicCounterTag;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.AtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * This extension enables atomic counter attributes to be written to the database.
 * <p>
 * This extension is loaded by default when you instantiate a
 * {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient} so unless you are using a custom extension
 * there is no need to specify it.
 * <p>
 * To utilize atomic counters, first create a field in your model that will be used to store the counter.
 * This class field should be have {@link Long} type, and you need to tag it as an atomic counter. If you are using the
 * {@link software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema}
 * then you should use the {@link software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAtomicCounter}
 * annotation, otherwise if you are using the {@link software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema},
 * use the {@link StaticAttributeTags#atomicCounter()} static attribute tag. Every time a new update of the record is
 * successfully written to the database, the counter will be automatically updated using either user-supplied
 * increment/decrement delta and start values or the defaults.
 * <p>
 * <b>NOTE: </b>If you only use the updateItem operation to write/update your item to the database, by an implementation
 * quirk the extension is not strictly required to be loaded in order for atomic counters to work. Tagging the attribute(s) in
 * the schema is sufficient.
 */
@SdkPublicApi
public final class AtomicCounterExtension implements DynamoDbEnhancedClientExtension {

    private AtomicCounterExtension() {
    }

    public static AtomicCounterExtension.Builder builder() {
        return new AtomicCounterExtension.Builder();
    }

    /**
     * @param context The {@link DynamoDbExtensionContext.BeforeWrite} context containing the state of the execution.
     * @return WriteModification contains the item with updated attributes.
     */
    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {

        Map<String, AtomicCounter> counters = AtomicCounterTag.resolve(context.tableMetadata());

        if (CollectionUtils.isNullOrEmpty(counters)) {
            return WriteModification.builder().build();
        }

        return WriteModification.builder()
                                .transformedItem(addCounterAttributes(context.items(), counters))
                                .build();
    }

    private Map<String, AttributeValue> addCounterAttributes(Map<String, AttributeValue> items,
                                                             Map<String, AtomicCounter> counters) {
        Map<String, AttributeValue> itemToTransform = new HashMap<>(items);
        counters.forEach((attribute, counter) -> itemToTransform.put(attribute,
                                                                     AtomicCounter.CounterAttribute
                                                                         .resolvedValue(counter.startValue().value())));
        return Collections.unmodifiableMap(itemToTransform);
    }

    public static final class Builder {
        private Builder() {
        }

        public AtomicCounterExtension build() {
            return new AtomicCounterExtension();
        }
    }
}
