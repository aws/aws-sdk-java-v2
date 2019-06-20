/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute;

import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.Converter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.CollectionSubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.MapSubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.AttributeConverterAware;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Converts between a Java type (or its subclasses) and an {@link ItemAttributeValue}.
 *
 * <p>
 * More specifically, this converts Java types into {@link ItemAttributeValue}s, which are more user-friendly representations of
 * the generated {@link AttributeValue}. An {@link ItemAttributeValue} can always be converted to a generated
 * {@link AttributeValue} using {@link ItemAttributeValue#toGeneratedAttributeValue()}.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>The {@link CollectionSubtypeAttributeConverter} converts a subtype of {@link Collection} into a DynamoDB list
 *     ({@link AttributeValue#l()}.</li>
 *     <li>The {@link MapSubtypeAttributeConverter} converts a subtype of {@link Map} into a DynamoDB map
 *     ({@link AttributeValue#m()}).</li>
 * </ul>
 *
 * <p>
 * See {@link AttributeConverterAware} for more information on how converters are used by the mapper.
 *
 * @param <T> The parent of the Java types supported by this converter.
 *
 * @see AttributeConverterAware
 * @see AttributeConverter
 */
@SdkPublicApi
@ThreadSafe
public interface SubtypeAttributeConverter<T> extends Converter<T> {
    /**
     * Convert the provided Java object into an {@link ItemAttributeValue}. This will raise a {@link RuntimeException} if the
     * conversion fails, or the input is null.
     *
     * <p>
     * Example:
     * {@code
     * CollectionSubtypeAttributeConverter converter = CollectionAttributeConverter.create();
     * assertEquals(converter.toAttributeValue(Arrays.asList("foo")),
     *              ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromString("foo")));
     * }
     */
    ItemAttributeValue toAttributeValue(T input, ConversionContext context);

    /**
     * Equivalent to {@code toAttributeValue(input, ConversionContext.defaultConversionContext())}.
     *
     * @see #toAttributeValue(Object, ConversionContext)
     */
    default ItemAttributeValue toAttributeValue(T input) {
        return toAttributeValue(input, ConversionContext.defaultConversionContext());
    }

    /**
     * Convert the provided {@link ItemAttributeValue} into a Java object that matches the requested type. This will raise a
     * {@link RuntimeException} if the conversion fails, the requested subtype is not supported, or the input is null.
     *
     * <p>
     * Example:
     * {@code
     * CollectionSubtypeAttributeConverter converter = CollectionAttributeConverter.create();
     * ItemAttributeValue list = ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromString("foo"));
     * assertEquals(converter.fromAttributeValue(list, Arrays.asList("foo"));
     * }
     */
    <U extends T> U fromAttributeValue(ItemAttributeValue input, TypeToken<U> desiredType, ConversionContext context);

    /**
     * Equivalent to {@code fromAttributeValue(input, desiredType, ConversionContext.defaultConversionContext())}.
     *
     * @see #fromAttributeValue(ItemAttributeValue, TypeToken, ConversionContext)
     */
    default <U extends T> U fromAttributeValue(ItemAttributeValue input, TypeToken<U> desiredType) {
        return fromAttributeValue(input, desiredType, ConversionContext.defaultConversionContext());
    }
}
