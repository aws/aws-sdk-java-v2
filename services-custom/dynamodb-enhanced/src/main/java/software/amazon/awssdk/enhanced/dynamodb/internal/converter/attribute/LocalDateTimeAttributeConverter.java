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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link LocalDateTime} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a string.
 *
 * <p>
 * Values are stored with nanosecond precision.
 *
 * <p>
 * LocalDateTimes are stored in the official {@link LocalDateTime} format "[-]YYYY-MM-DDTHH:II:SS[.NNNNNNNNN]", where:
 * <ol>
 *     <li>Y is a year between {@link Year#MIN_VALUE} and {@link Year#MAX_VALUE} (prefixed with - if it is negative)</li>
 *     <li>M is a 2-character, zero-prefixed month between 01 and 12</li>
 *     <li>D is a 2-character, zero-prefixed day between 01 and 31</li>
 *     <li>H is a 2-character, zero-prefixed hour between 00 and 23</li>
 *     <li>I is a 2-character, zero-prefixed minute between 00 and 59</li>
 *     <li>S is a 2-character, zero-prefixed second between 00 and 59</li>
 *     <li>N is a 9-character, zero-prefixed nanosecond between 000,000,000 and 999,999,999.
 *     The . and N may be excluded if N is 0.</li>
 * </ol>
 * See {@link LocalDateTime} for more details on the serialization format.
 * <p>
 * This is format-compatible with the {@link LocalDateAttributeConverter}, allowing values stored as {@link LocalDate} to be
 * retrieved as {@link LocalDateTime}s. The time associated with a value stored as a {@link LocalDate} is the
 * beginning of the day (midnight).
 *
 * <p>
 * This serialization is lexicographically orderable when the year is not negative.
 * </p>
 *
 * Examples:
 * <ul>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0)} is stored as
 *     an AttributeValue with the String "1988-05-21T00:00"}</li>
 *     <li>{@code LocalDateTime.of(-1988, 5, 21, 0, 0, 0)} is stored as
 *     an AttributeValue with the String "-1988-05-21T00:00"}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).plusSeconds(1)} is stored as
 *     an AttributeValue with the String "1988-05-21T00:00:01"}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).minusSeconds(1)} is stored as
 *     an AttributeValue with the String "1988-05-20T23:59:59"}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).plusNanos(1)} is stored as
 *     an AttributeValue with the String "1988-05-21T00:00:00.0000000001"}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).minusNanos(1)} is stored as
 *     an AttributeValue with the String "1988-05-20T23:59:59.999999999"}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime> {
    private static final Visitor VISITOR = new Visitor();

    public static LocalDateTimeAttributeConverter create() {
        return new LocalDateTimeAttributeConverter();
    }

    @Override
    public EnhancedType<LocalDateTime> type() {
        return EnhancedType.of(LocalDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(LocalDateTime input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public LocalDateTime transformTo(AttributeValue input) {
        try {
            if (input.s() != null) {
                return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
            }

            return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static final class Visitor extends TypeConvertingVisitor<LocalDateTime> {
        private Visitor() {
            super(LocalDateTime.class, InstantAsStringAttributeConverter.class);
        }

        @Override
        public LocalDateTime convertString(String value) {
            if (value.contains("T")) { // AttributeValue.S in LocalDateTime format
                return LocalDateTime.parse(value);
            } else { // AttributeValue.S in LocalDate format
                return ConverterUtils.convertFromLocalDate(LocalDate.parse(value));
            }
        }
    }
}
