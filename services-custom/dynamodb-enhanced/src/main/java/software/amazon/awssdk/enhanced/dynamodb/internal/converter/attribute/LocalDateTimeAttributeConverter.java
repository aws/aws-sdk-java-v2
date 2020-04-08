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

import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.padLeft;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.padLeft2;

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
 * This stores and reads values in DynamoDB as a number, so that they can be sorted numerically as part of a sort key.
 *
 * <p>
 * LocalDateTimes are stored in the format "[-]YYYYMMDDHHIISS[.NNNNNNNNN]", where:
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
 *
 * <p>
 * This is format-compatible with the {@link LocalDateAttributeConverter}, allowing values stored as {@link LocalDate} to be
 * retrieved as {@link LocalDateTime}s and vice-versa. The time associated with a value stored as a {@link LocalDate} is the
 * beginning of the day (midnight).
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0)} is stored as
 *     {@code ItemAttributeValueMapper.fromNumber("19880521000000")}</li>
 *     <li>{@code LocalDateTime.of(-1988, 5, 21, 0, 0, 0)} is stored as
 *     {@code ItemAttributeValueMapper.fromNumber("-19880521000000")}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).plusSeconds(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromNumber("19880521000001")}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).minusSeconds(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromNumber("19880520235959")}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).plusNanos(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromNumber("19880521000000.0000000001")}</li>
 *     <li>{@code LocalDateTime.of(1988, 5, 21, 0, 0, 0).minusNanos(1)} is stored as
 *     {@code ItemAttributeValueMapper.fromNumber("19880520235959.999999999")}</li>
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
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(LocalDateTime input) {
        String value = "" +
                       input.getYear() +
                       padLeft2(input.getMonthValue()) +
                       padLeft2(input.getDayOfMonth()) +
                       padLeft2(input.getHour()) +
                       padLeft2(input.getMinute()) +
                       padLeft2(input.getSecond()) +
                       (input.getNano() == 0 ? "" : "." + padLeft(9, input.getNano()));
        return AttributeValue.builder().n(value).build();
    }

    @Override
    public LocalDateTime transformTo(AttributeValue input) {
        if (input.n() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<LocalDateTime> {
        private Visitor() {
            super(LocalDateTime.class, InstantAsIntegerAttributeConverter.class);
        }

        @Override
        public LocalDateTime convertNumber(String value) {
            String[] splitOnDecimal = ConverterUtils.splitNumberOnDecimal(value);
            String[] chunkedDateTime = ConverterUtils.chunkWithLeftOverflow(splitOnDecimal[0], 2, 2, 2, 2, 2);

            int year = Integer.parseInt(chunkedDateTime[0]);
            return LocalDateTime.of(year,
                                    Integer.parseInt(chunkedDateTime[1]),
                                    Integer.parseInt(chunkedDateTime[2]),
                                    Integer.parseInt(chunkedDateTime[3]),
                                    Integer.parseInt(chunkedDateTime[4]),
                                    Integer.parseInt(chunkedDateTime[5]),
                                    Integer.parseInt(splitOnDecimal[1]));
        }
    }
}
