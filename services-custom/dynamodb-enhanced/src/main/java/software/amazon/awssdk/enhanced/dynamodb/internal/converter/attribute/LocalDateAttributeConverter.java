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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link LocalDate} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a String.
 *
 * <p>
 * LocalDates are stored in the official {@link LocalDate} format "[-]YYYY-MM-DD", where:
 * <ol>
 *     <li>Y is a year between {@link Year#MIN_VALUE} and {@link Year#MAX_VALUE} (prefixed with - if it is negative)</li>
 *     <li>M is a 2-character, zero-prefixed month between 01 and 12</li>
 *     <li>D is a 2-character, zero-prefixed day between 01 and 31</li>
 * </ol>
 * See {@link LocalDate} for more details on the serialization format.
 *
 * <p>
 * This is unidirectional format-compatible with the {@link LocalDateTimeAttributeConverter}, allowing values
 * stored as {@link LocalDate} to be retrieved as {@link LocalDateTime}s.
 *
 * <p>
 * This serialization is lexicographically orderable when the year is not negative.
 * <p>
 *
 * Examples:
 * <ul>
 *     <li>{@code LocalDate.of(1988, 5, 21)} is stored as as an AttributeValue with the String "1988-05-21"}</li>
 *     <li>{@code LocalDate.of(0, 1, 1)} is stored as as an AttributeValue with the String "0000-01-01"}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class LocalDateAttributeConverter implements AttributeConverter<LocalDate> {
    private static final Visitor VISITOR = new Visitor();

    private LocalDateAttributeConverter() {
    }

    public static LocalDateAttributeConverter create() {
        return new LocalDateAttributeConverter();
    }

    @Override
    public EnhancedType<LocalDate> type() {
        return EnhancedType.of(LocalDate.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(LocalDate input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public LocalDate transformTo(AttributeValue input) {
        try {
            if (input.s() != null) {
                return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
            }

            return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static final class Visitor extends TypeConvertingVisitor<LocalDate> {
        private Visitor() {
            super(LocalDate.class, InstantAsStringAttributeConverter.class);
        }

        @Override
        public LocalDate convertString(String value) {
            return LocalDate.parse(value);
        }
    }
}
