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

import java.time.OffsetDateTime;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TimeConversion;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link OffsetDateTime} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * Values are stored in ISO-8601 format, with nanosecond precision. If the offset has seconds then they will also be included,
 * even though this is not part of the ISO-8601 standard. For full ISO-8601 compliance, ensure your {@code OffsetDateTime}s do
 * not have offsets at the precision level of seconds.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code OffsetDateTime.MIN} is stored as
 *     {@code ItemAttributeValueMapper.fromString("-999999999-01-01T00:00:00+18:00")}</li>
 *     <li>{@code OffsetDateTime.MAX} is stored as
 *     {@code ItemAttributeValueMapper.fromString("+999999999-12-31T23:59:59.999999999-18:00")}</li>
 *     <li>{@code Instant.EPOCH.atOffset(ZoneOffset.UTC).plusSeconds(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:01Z")}</li>
 *     <li>{@code Instant.EPOCH.atOffset(ZoneOffset.UTC).minusSeconds(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1969-12-31T23:59:59Z")}</li>
 *     <li>{@code Instant.EPOCH.atOffset(ZoneOffset.UTC).plusMillis(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00.001Z")}</li>
 *     <li>{@code Instant.EPOCH.atOffset(ZoneOffset.UTC).minusMillis(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1969-12-31T23:59:59.999Z")}</li>
 *     <li>{@code Instant.EPOCH.atOffset(ZoneOffset.UTC).plusNanos(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00.000000001Z")}</li>
 *     <li>{@code Instant.EPOCH.atOffset(ZoneOffset.UTC).minusNanos(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1969-12-31T23:59:59.999999999Z")}</li>
 * </ul>
 *
 * <p>
 * This converter can read any values written by itself, {@link InstantAsIntegerAttributeConverter},
 * {@link InstantAsStringAttributeConverter}, or {@link ZonedDateTimeAsStringAttributeConverter}. Values written by
 * {@code Instant} converters are treated as if they are in the UTC time zone (and an offset of 0 seconds will be returned).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class OffsetDateTimeAsStringAttributeConverter implements AttributeConverter<OffsetDateTime> {
    public static OffsetDateTimeAsStringAttributeConverter create() {
        return new OffsetDateTimeAsStringAttributeConverter();
    }

    @Override
    public EnhancedType<OffsetDateTime> type() {
        return EnhancedType.of(OffsetDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(OffsetDateTime input) {
        return TimeConversion.toStringAttributeValue(input).toAttributeValue();
    }

    @Override
    public OffsetDateTime transformTo(AttributeValue input) {
        return TimeConversion.offsetDateTimeFromAttributeValue(EnhancedAttributeValue.fromAttributeValue(input));
    }
}
