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
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link ZonedDateTime} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * Values are stored in a ISO-8601-like format, with the non-offset zone IDs being added at the end of the string in square
 * brackets. If the zone ID offset has seconds, then they will also be included, even though this is not part of the ISO-8601
 * standard. For full ISO-8601 compliance, it is better to use {@link OffsetDateTime}s (without second-level precision in its
 * offset) or {@link Instant}s, assuming the time zone information is not strictly required.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code Instant.EPOCH.atZone(ZoneId.of("Europe/Paris"))} is stored as
 *     an AttributeValue with the String "1970-01-01T01:00+01:00[Europe/Paris]"}</li>
 *     <li>{@code OffsetDateTime.MIN.toZonedDateTime()} is stored as
 *     an AttributeValue with the String "-999999999-01-01T00:00+18:00"}</li>
 *     <li>{@code OffsetDateTime.MAX.toZonedDateTime()} is stored as
 *     an AttributeValue with the String "+999999999-12-31T23:59:59.999999999-18:00"}</li>
 *     <li>{@code Instant.EPOCH.atZone(ZoneOffset.UTC)} is stored as
 *     an AttributeValue with the String "1970-01-01T00:00Z"}</li>
 * </ul>
 * See {@link OffsetDateTime} for more details on the serialization format.
 * <p>
 * This converter can read any values written by itself, {@link InstantAsStringAttributeConverter},
 * or {@link OffsetDateTimeAsStringAttributeConverter}. Values written by
 * {@code Instant} converters are treated as if they are in the UTC time zone.
 *
 * <p>
 * This serialization is lexicographically orderable when the year is not negative.
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class ZonedDateTimeAsStringAttributeConverter implements AttributeConverter<ZonedDateTime> {
    private static final Visitor VISITOR = new Visitor();

    public static ZonedDateTimeAsStringAttributeConverter create() {
        return new ZonedDateTimeAsStringAttributeConverter();
    }

    @Override
    public EnhancedType<ZonedDateTime> type() {
        return EnhancedType.of(ZonedDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(ZonedDateTime input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public ZonedDateTime transformTo(AttributeValue input) {
        try {
            if (input.s() != null) {
                return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
            }

            return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static final class Visitor extends TypeConvertingVisitor<ZonedDateTime> {
        private Visitor() {
            super(ZonedDateTime.class, InstantAsStringAttributeConverter.class);
        }

        @Override
        public ZonedDateTime convertString(String value) {
            return ZonedDateTime.parse(value);
        }
    }
}
