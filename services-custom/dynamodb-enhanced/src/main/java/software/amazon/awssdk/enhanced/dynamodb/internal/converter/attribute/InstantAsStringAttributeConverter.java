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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute;

import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Instant} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * Values are stored in ISO-8601 format, with nanosecond precision and a time zone of UTC.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code Instant.EPOCH.plusSeconds(1)} is stored as
 *     an AttributeValue with the String "1970-01-01T00:00:01Z"}</li>
 *     <li>{@code Instant.EPOCH.minusSeconds(1)} is stored as
 *     an AttributeValue with the String "1969-12-31T23:59:59Z"}</li>
 *     <li>{@code Instant.EPOCH.plusMillis(1)} is stored as
 *     an AttributeValue with the String "1970-01-01T00:00:00.001Z"}</li>
 *     <li>{@code Instant.EPOCH.minusMillis(1)} is stored as
 *     an AttributeValue with the String "1969-12-31T23:59:59.999Z"}</li>
 *     <li>{@code Instant.EPOCH.plusNanos(1)} is stored as
 *     an AttributeValue with the String "1970-01-01T00:00:00.000000001Z"}</li>
 *     <li>{@code Instant.EPOCH.minusNanos(1)} is stored as
 *     an AttributeValue with the String "1969-12-31T23:59:59.999999999Z"}</li>
 * </ul>
 * See {@link Instant} for more details on the serialization format.
 * <p>
 * This converter can read any values written by itself, or values with zero offset written by
 * {@link OffsetDateTimeAsStringAttributeConverter}, and values with zero offset and without time zone named written by
 * {@link ZoneOffsetAttributeConverter}. Offset and zoned times will be automatically converted to the
 * equivalent {@link Instant}.
 *
 * <p>
 * This serialization is lexicographically orderable when the year is not negative.
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class InstantAsStringAttributeConverter implements AttributeConverter<Instant> {
    private static final Visitor VISITOR = new Visitor();

    private InstantAsStringAttributeConverter() {
    }

    public static InstantAsStringAttributeConverter create() {
        return new InstantAsStringAttributeConverter();
    }

    @Override
    public EnhancedType<Instant> type() {
        return EnhancedType.of(Instant.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(Instant input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public Instant transformTo(AttributeValue input) {
        try {
            if (input.s() != null) {
                return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
            }

            return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static final class Visitor extends TypeConvertingVisitor<Instant> {
        private Visitor() {
            super(Instant.class, InstantAsStringAttributeConverter.class);
        }

        @Override
        public Instant convertString(String value) {
            return Instant.parse(value);
        }
    }
}
