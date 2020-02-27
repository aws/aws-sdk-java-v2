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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.converter.internal.TimeConversion;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeValueType;
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
 *     {@code 1970-01-01T01:00:00+01:00[Europe/Paris]}</li>
 *     <li>{@code OffsetDateTime.MIN.toZonedDateTime()} is stored as
 *     {@code ItemAttributeValueMapper.fromString("-999999999-01-01T00:00:00+18:00")}</li>
 *     <li>{@code OffsetDateTime.MAX.toZonedDateTime()} is stored as
 *     {@code ItemAttributeValueMapper.fromString("+999999999-12-31T23:59:59.999999999-18:00")}</li>
 *     <li>{@code Instant.EPOCH.atZone(ZoneOffset.UTC)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00Z")}</li>
 * </ul>
 *
 * <p>
 * This converter can read any values written by itself, {@link InstantAsIntegerAttributeConverter},
 * {@link InstantAsStringAttributeConverter}, or {@link OffsetDateTimeAsStringAttributeConverter}. Values written by
 * {@code Instant} converters are treated as if they are in the UTC time zone.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ZonedDateTimeAsStringAttributeConverter implements AttributeConverter<ZonedDateTime> {
    public static ZonedDateTimeAsStringAttributeConverter create() {
        return new ZonedDateTimeAsStringAttributeConverter();
    }

    @Override
    public TypeToken<ZonedDateTime> type() {
        return TypeToken.of(ZonedDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(ZonedDateTime input) {
        return TimeConversion.toStringAttributeValue(input).toGeneratedAttributeValue();
    }

    @Override
    public ZonedDateTime transformTo(AttributeValue input) {
        return TimeConversion.zonedDateTimeFromAttributeValue(ItemAttributeValue.fromGeneratedAttributeValue(input));
    }
}
