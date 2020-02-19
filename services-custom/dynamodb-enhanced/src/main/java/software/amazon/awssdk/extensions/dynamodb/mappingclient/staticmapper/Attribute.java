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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.isNullAttributeValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.nullAttributeValue;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.AttributeValueType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
@SuppressWarnings("WeakerAccess")
public class Attribute<T> {
    private final String attributeName;
    private final Function<T, AttributeValue> getAttributeMethod;
    private final BiConsumer<T, AttributeValue> updateItemMethod;
    private final StaticTableMetadata tableMetadata;
    private final AttributeValueType attributeValueType;

    private Attribute(String attributeName,
                      Function<T, AttributeValue> getAttributeMethod,
                      BiConsumer<T, AttributeValue> updateItemMethod,
                      StaticTableMetadata tableMetadata,
                      AttributeValueType attributeValueType) {
        this.attributeName = attributeName;
        this.getAttributeMethod = getAttributeMethod;
        this.updateItemMethod = updateItemMethod;
        this.tableMetadata = tableMetadata;
        this.attributeValueType = attributeValueType;
    }

    public static <T, R> AttributeSupplier<T> create(
        String attributeName,
        Function<T, R> getAttributeMethod,
        BiConsumer<T, R> updateItemMethod,
        AttributeType<R> attributeType) {

        Function<T, AttributeValue> getAttributeValueWithTransform = item -> {
            R value = getAttributeMethod.apply(item);
            return value == null ? nullAttributeValue() : attributeType.objectToAttributeValue(value);
        };

        // When setting a value on the java object, do not explicitly set nulls as this can cause an NPE to be thrown
        // if the target attribute type is a primitive.
        BiConsumer<T, AttributeValue> updateItemWithTransform = (item, attributeValue) -> {
            // If the attributeValue is nul, do not attempt to marshal
            if (isNullAttributeValue(attributeValue)) {
                return;
            }

            R value = attributeType.attributeValueToObject(attributeValue);

            if (value != null) {
                updateItemMethod.accept(item, value);
            }
        };

        return new AttributeSupplier<>(attributeName,
                                       getAttributeValueWithTransform,
                                       updateItemWithTransform,
                                       attributeType.attributeValueType());
    }

    /**
     * Return a transformed copy of this attribute that knows how to get/set from a different type of object given a
     * function that can convert the containing object itself. It does this by modifying the get/set functions of
     * type T to type R given a transformation function F(T) = R.
     * @param transform A function that converts the object storing the attribute from the source type to the
     *                  destination type.
     * @param createComponent A consumer to create a new instance of the component object when required. A null value
     *                       will bypass this logic.
     * @param <R> The type being transformed to.
     * @return A new Attribute that be contained by an object of type R.
     */
    public <R> Attribute<R> transform(Function<R, T> transform, Consumer<R> createComponent) {
        return new Attribute<>(attributeName,
            item -> {
                T otherItem = transform.apply(item);

                // If the containing object is null don't attempt to read attributes from it
                return otherItem == null ?
                    nullAttributeValue() : getAttributeMethod.apply(otherItem);
            },
            (item, value) -> {
                if (createComponent != null) {
                    // Lazily instantiate the component object once there is a value to write into it
                    createComponent.accept(item);
                }
                updateItemMethod.accept(transform.apply(item), value);
            },
            tableMetadata,
            attributeValueType);
    }

    String attributeName() {
        return attributeName;
    }

    Function<T, AttributeValue> attributeGetterMethod() {
        return getAttributeMethod;
    }

    BiConsumer<T, AttributeValue> updateItemMethod() {
        return updateItemMethod;
    }

    StaticTableMetadata tableMetadata() {
        return tableMetadata;
    }

    public static class AttributeSupplier<T> implements Supplier<Attribute<T>> {
        private final String attributeName;
        private final Function<T, AttributeValue> getAttributeValue;
        private final BiConsumer<T, AttributeValue> updateItem;
        private final StaticTableMetadata.Builder tableMetadataBuilder = StaticTableMetadata.builder();
        private final AttributeValueType attributeValueType;

        private AttributeSupplier(String attributeName,
                                  Function<T, AttributeValue> getAttributeValue,
                                  BiConsumer<T, AttributeValue> updateItem,
                                  AttributeValueType attributeValueType) {
            this.attributeName = attributeName;
            this.getAttributeValue = getAttributeValue;
            this.updateItem = updateItem;
            this.attributeValueType = attributeValueType;
        }

        @Override
        public Attribute<T> get() {
            return new Attribute<>(attributeName,
                                   getAttributeValue,
                                   updateItem,
                                   tableMetadataBuilder.build(),
                                   attributeValueType);
        }

        public AttributeSupplier<T> as(AttributeTag... attributeTags) {
            Arrays.stream(attributeTags).forEach(attributeTag ->
                attributeTag.setTableMetadataForAttribute(attributeName, attributeValueType, tableMetadataBuilder));
            return this;
        }
    }
}
