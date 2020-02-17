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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.Converter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.InstantAsIntegerAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.InstantAsStringAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.StringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Converts between a specific Java type and an {@link AttributeValue}.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>The {@link StringAttributeConverter} converts a {@link String} into a DynamoDB string
 *     ({@link software.amazon.awssdk.services.dynamodb.model.AttributeValue#s()}).</li>
 *     <li>The {@link InstantAsIntegerAttributeConverter} converts an {@link Instant} into a DynamoDB number
 *     ({@link software.amazon.awssdk.services.dynamodb.model.AttributeValue#n()}).</li>
 *     <li>The {@link InstantAsStringAttributeConverter} converts an {@link Instant} into a DynamoDB string
 *     ({@link software.amazon.awssdk.services.dynamodb.model.AttributeValue#s()}).</li>
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
     * Convert the provided Java object into an {@link AttributeValue}. This will raise a {@link RuntimeException} if the
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
    AttributeValue transformFrom(T input);

    /**
     * Convert the provided {@link AttributeValue} into a Java object. This will raise a {@link RuntimeException} if the
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
    T transformTo(AttributeValue input);
}
