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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;

import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class ResolvedImmutableAttribute<T, B, R> {
    private final String attributeName;
    private final BiConsumer<B, AttributeValue> updateBuilderMethod;
    private final StaticTableMetadata tableMetadata;
    private final AttributeConverter attributeConverter;
    private final Function<T, R> getter;
    private final AttributeType<R> attributeType;


    private ResolvedImmutableAttribute(String attributeName,
                                       BiConsumer<B, AttributeValue> updateBuilderMethod,
                                       StaticTableMetadata tableMetadata,
                                       AttributeConverter attributeConverter,
                                       Function<T, R> getter,
                                       AttributeType<R> attributeType) {
        this.attributeName = attributeName;
        this.updateBuilderMethod =  updateBuilderMethod;
        this.tableMetadata = tableMetadata;
        this.attributeConverter = attributeConverter;
        this.getter = getter;
        this.attributeType = attributeType;
    }

    AttributeValue getAttributeValue(T item) {
        R value = getter.apply(item);
        return value == null ? nullAttributeValue()
                             : attributeType.objectToAttributeValue(value);
    }

    public static <T, B, R> ResolvedImmutableAttribute<T, B, R> create(ImmutableAttribute<T, B, R> immutableAttribute,
                                                                    AttributeConverter<R> attributeConverter) {

        AttributeType<R> attributeType = StaticAttributeType.create(attributeConverter);
        Function<T, R> getter = immutableAttribute.getter();

        // When setting a value on the java object, do not explicitly set nulls as this can cause an NPE to be thrown
        // if the target attribute type is a primitive.
        BiConsumer<B, AttributeValue> updateBuilderWithTransform =
            (builder, attributeValue) -> {
                // If the attributeValue is null, do not attempt to marshal
                if (isNullAttributeValue(attributeValue)) {
                    return;
                }

                R value = attributeType.attributeValueToObject(attributeValue);

                if (value != null) {
                    immutableAttribute.setter().accept(builder, value);
                }
            };

        StaticTableMetadata.Builder tableMetadataBuilder = StaticTableMetadata.builder();
        immutableAttribute.tags().forEach(tag -> {
            tag.validateType(immutableAttribute.name(), immutableAttribute.type(),
                             attributeType.attributeValueType());
            tag.modifyMetadata(immutableAttribute.name(), attributeType.attributeValueType()).accept(tableMetadataBuilder);
        });
        return new ResolvedImmutableAttribute<>(immutableAttribute.name(),
                                                updateBuilderWithTransform,
                                                tableMetadataBuilder.build(),
                                                attributeConverter,
                                                getter,
                                                attributeType);
    }

    public <T1, B1> ResolvedImmutableAttribute<T1, B1, R> transform(
        Function<T1, T> transformItem,
        Function<B1, B> transformBuilder) {

        return new ResolvedImmutableAttribute<>(
            attributeName,
            (item, value) -> updateBuilderMethod.accept(transformBuilder.apply(item), value),
            tableMetadata,
            attributeConverter,
            item -> {
                T otherItem = transformItem.apply(item);
                return otherItem == null ? null : getter.apply(otherItem);
            },
            attributeType);
    }

    public String attributeName() {
        return attributeName;
    }

    public Function<T, AttributeValue> attributeGetterMethod() {
        return this::getAttributeValue;
    }

    public BiConsumer<B, AttributeValue> updateItemMethod() {
        return updateBuilderMethod;
    }

    public StaticTableMetadata tableMetadata() {
        return tableMetadata;
    }

    public AttributeConverter attributeConverter() {
        return attributeConverter;
    }
}
