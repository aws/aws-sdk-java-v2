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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TimeConversion;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Instant} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number, so that they can be sorted numerically as part of a sort key.
 *
 * <p>
 * Instants are stored in the format "[-]X[.Y]", where X is the number of seconds past the epoch of 1970-01-01T00:00:00Z
 * in this instant, and Y is the fraction of seconds, up to the nanosecond precision (Y is at most 9 characters long).
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code Instant.EPOCH.plusSeconds(1)} is stored as {@code ItemAttributeValueMapper.fromNumber("1")}</li>
 *     <li>{@code Instant.EPOCH.minusSeconds(1)} is stored as {@code ItemAttributeValueMapper.fromNumber("-1")}</li>
 *     <li>{@code Instant.EPOCH.plusMillis(1)} is stored as {@code ItemAttributeValueMapper.fromNumber("0.001")}</li>
 *     <li>{@code Instant.EPOCH.minusMillis(1)} is stored as {@code ItemAttributeValueMapper.fromNumber("-0.001")}</li>
 *     <li>{@code Instant.EPOCH.plusNanos(1)} is stored as {@code ItemAttributeValueMapper.fromNumber("0.000000001")}</li>
 *     <li>{@code Instant.EPOCH.minusNanos(1)} is stored as {@code ItemAttributeValueMapper.fromNumber("-0.000000001")}</li>
 * </ul>
 *
 * <p>
 * This converter can read any values written by itself, {@link InstantAsStringAttributeConverter},
 * {@link OffsetDateTimeAsStringAttributeConverter} or {@link ZonedDateTimeAsStringAttributeConverter}. Offset and zoned times
 * will be automatically converted to the equivalent {@code Instant} based on the time zone information in the record (e.g.
 * {@code ItemAttributeValueMapper.fromString("1970-01-01T00:00:00+01:00")} will be converted to
 * {@code Instant.EPOCH.minus(1, ChronoUnit.HOURS)}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class InstantAsIntegerAttributeConverter implements AttributeConverter<Instant> {
    private InstantAsIntegerAttributeConverter() {
    }

    public static InstantAsIntegerAttributeConverter create() {
        return new InstantAsIntegerAttributeConverter();
    }

    @Override
    public EnhancedType<Instant> type() {
        return EnhancedType.of(Instant.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(Instant input) {
        return TimeConversion.toIntegerAttributeValue(input).toAttributeValue();
    }

    @Override
    public Instant transformTo(AttributeValue input) {
        if (input.n() != null) {
            return TimeConversion.instantFromAttributeValue(EnhancedAttributeValue.fromNumber(input.n()));
        }
        return TimeConversion.instantFromAttributeValue(EnhancedAttributeValue.fromAttributeValue(input));
    }
}
