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

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.Converter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.InstantAsIntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.AttributeConverterAware;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Converts between a specific Java type and an {@link ItemAttributeValue}.
 *
 * <p>
 * More specifically, this converts Java types into {@link ItemAttributeValue}s, which are more user-friendly representations of
 * the generated {@link AttributeValue}. An {@link ItemAttributeValue} can always be converted to a generated
 * {@link AttributeValue} using {@link ItemAttributeValue#toGeneratedAttributeValue()}.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>The {@link StringAttributeConverter} converts a {@link String} into a DynamoDB string
 *     ({@link AttributeValue#s()}).</li>
 *     <li>The {@link InstantAsIntegerAttributeConverter} converts an {@link Instant} into a DynamoDB number
 *     ({@link AttributeValue#n()}).</li>
 *     <li>The {@link InstantAsStringAttributeConverter} converts an {@link Instant} into a DynamoDB string
 *     ({@link AttributeValue#s()}).</li>
 * </ul>
 *
 * <p>
 * Unlike {@link SubtypeAttributeConverter}, this does not support subtypes of a specific type.
 *
 * <p>
 * See {@link AttributeConverterAware} for more information on how converters are used by the mapper.
 *
 * @param <T> The Java type supported by this converter.
 *
 * @see AttributeConverterAware
 * @see SubtypeAttributeConverter
 */
@SdkPublicApi
@ThreadSafe
public interface AttributeConverter<T> extends Converter<T> {
    /**
     * Convert the provided Java object into an {@link ItemAttributeValue}. This will raise a {@link RuntimeException} if the
     * conversion fails, or the input is null.
     *
     * <p>
     * Example:
     * {@code
     * InstantAsStringAttributeConverter converter = InstantAsStringAttributeConverter.create();
     * assertEquals(converter.toAttributeValue(Instant.EPOCH),
     *              ItemAttributeValue.fromString("1970-01-01T00:00:00Z"));
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
     * Convert the provided {@link ItemAttributeValue} into a Java object. This will raise a {@link RuntimeException} if the
     * conversion fails, or the input is null.
     *
     * <p>
     * Example:
     * {@code
     * InstantAsStringAttributeConverter converter = InstantAsStringAttributeConverter.create();
     * assertEquals(converter.fromAttributeValue(ItemAttributeValue.fromString("1970-01-01T00:00:00Z")),
     *              Instant.EPOCH);
     * }
     */
    T fromAttributeValue(ItemAttributeValue input, ConversionContext context);

    /**
     * Equivalent to {@code fromAttributeValue(input, ConversionContext.defaultConversionContext())}.
     *
     * @see #fromAttributeValue(ItemAttributeValue, ConversionContext)
     */
    default T fromAttributeValue(ItemAttributeValue input) {
        return fromAttributeValue(input, ConversionContext.defaultConversionContext());
    }
}
