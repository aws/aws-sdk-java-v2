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

package software.amazon.awssdk.enhanced.dynamodb;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Converts between a specific Java type and an {@link AttributeValue}.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>The {@link StringAttributeConverter} converts a {@link String} into a DynamoDB string
 *     ({@link software.amazon.awssdk.services.dynamodb.model.AttributeValue#s()}).</li>
 *     <li>The {@link InstantAsStringAttributeConverter} converts an {@link Instant} into a DynamoDB string
 *     ({@link software.amazon.awssdk.services.dynamodb.model.AttributeValue#s()}).</li>
 * </ul>
 */
@SdkPublicApi
@ThreadSafe
public interface AttributeConverter<T> {
    /**
     * Convert the provided Java object into an {@link AttributeValue}. This will raise a {@link RuntimeException} if the
     * conversion fails, or the input is null.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     * InstantAsStringAttributeConverter converter = InstantAsStringAttributeConverter.create();
     * assertEquals(converter.transformFrom(Instant.EPOCH),
     *              EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z").toAttributeValue());
     * }
     * </pre>
     */
    AttributeValue transformFrom(T input);

    /**
     * Convert the provided {@link AttributeValue} into a Java object. This will raise a {@link RuntimeException} if the
     * conversion fails, or the input is null.
     *
     * <p>
     * <pre>
     * Example:
     * {@code
     * InstantAsStringAttributeConverter converter = InstantAsStringAttributeConverter.create();
     * assertEquals(converter.transformTo(EnhancedAttributeValue.fromString("1970-01-01T00:00:00Z").toAttributeValue()),
     *              Instant.EPOCH);
     * }
     * </pre>
     */
    T transformTo(AttributeValue input);

    /**
     * The type supported by this converter.
     */
    EnhancedType<T> type();

    /**
     * The {@link AttributeValueType} that a converter stores and reads values
     * from DynamoDB via the {@link AttributeValue} class.
     */
    AttributeValueType attributeValueType();
}
