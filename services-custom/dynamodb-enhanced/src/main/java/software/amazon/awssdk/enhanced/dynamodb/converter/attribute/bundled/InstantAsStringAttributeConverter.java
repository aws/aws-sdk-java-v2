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
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.converter.internal.TimeConversion;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Instant} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string. If a number is desired (for sorting purposes), use
 * {@link InstantAsIntegerAttributeConverter} instead.
 *
 * <p>
 * Values are stored in ISO-8601 format, with nanosecond precision and a time zone of UTC.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code Instant.EPOCH.plusSeconds(1)} is stored as {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:01Z")}</li>
 *     <li>{@code Instant.EPOCH.minusSeconds(1)} is stored as {@code ItemAttributeValueMapper.fromString("1969-12-31T23:59:59Z")}</li>
 *     <li>{@code Instant.EPOCH.plusMillis(1)} is stored as {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00.001Z")}</li>
 *     <li>{@code Instant.EPOCH.minusMillis(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1969-12-31T23:59:59.999Z")}</li>
 *     <li>{@code Instant.EPOCH.plusNanos(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00.000000001Z")}</li>
 *     <li>{@code Instant.EPOCH.minusNanos(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromString("1969-12-31T23:59:59.999999999Z")}</li>
 * </ul>
 *
 * <p>
 * This converter can read any values written by itself, {@link InstantAsIntegerAttributeConverter},
 * {@link OffsetDateTimeAsStringAttributeConverter} or {@link ZonedDateTimeAsStringAttributeConverter}. Offset and zoned times
 * will be automatically converted to the equivalent {@code Instant} based on the time zone information in the record (e.g.
 * {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00+01:00")} will be converted to
 * {@code Instant.EPOCH.minus(1, ChronoUnit.HOURS)}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class InstantAsStringAttributeConverter implements AttributeConverter<Instant> {
    private InstantAsStringAttributeConverter() {
    }

    public static InstantAsStringAttributeConverter create() {
        return new InstantAsStringAttributeConverter();
    }

    @Override
    public TypeToken<Instant> type() {
        return TypeToken.of(Instant.class);
    }

    @Override
    public AttributeValue transformFrom(Instant input) {
        return TimeConversion.toStringAttributeValue(input).toGeneratedAttributeValue();
    }

    @Override
    public Instant transformTo(AttributeValue input) {
        return TimeConversion.instantFromAttributeValue(ItemAttributeValue.fromGeneratedAttributeValue(input));
    }
}
