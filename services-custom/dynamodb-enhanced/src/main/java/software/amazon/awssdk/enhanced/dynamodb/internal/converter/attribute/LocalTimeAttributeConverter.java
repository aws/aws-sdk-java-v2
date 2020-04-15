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

import java.time.DateTimeException;
import java.time.LocalTime;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link LocalTime} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a String.
 *
 * <p>
 * LocalTimes are stored in the official {@link LocalTime} format "HH:II:SS[.NNNNNNNNN]", where:
 * <ol>
 *     <li>H is a 2-character, zero-prefixed hour between 00 and 23</li>
 *     <li>I is a 2-character, zero-prefixed minute between 00 and 59</li>
 *     <li>S is a 2-character, zero-prefixed second between 00 and 59</li>
 *     <li>N is a 9-character, zero-prefixed nanosecond between 000,000,000 and 999,999,999.
 *     The . and N may be excluded if N is 0.</li>
 * </ol>
 * See {@link LocalTime} for more details on the serialization format.
 *
 * <p>
 * This serialization is lexicographically orderable.
 * <p>
 *
 * Examples:
 * <ul>
 *     <li>{@code LocalTime.of(5, 30, 0)} is stored as an AttributeValue with the String "05:30"}</li>
 *     <li>{@code LocalTime.of(5, 30, 0, 1)} is stored as an AttributeValue with the String "05:30:00.000000001"}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class LocalTimeAttributeConverter implements AttributeConverter<LocalTime> {
    private static final Visitor VISITOR = new Visitor();

    private LocalTimeAttributeConverter() {
    }

    public static LocalTimeAttributeConverter create() {
        return new LocalTimeAttributeConverter();
    }

    @Override
    public EnhancedType<LocalTime> type() {
        return EnhancedType.of(LocalTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(LocalTime input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public LocalTime transformTo(AttributeValue input) {
        if (input.s() != null) {
            return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<LocalTime> {
        private Visitor() {
            super(LocalTime.class, InstantAsStringAttributeConverter.class);
        }

        @Override
        public LocalTime convertString(String value) {
            try {
                return LocalTime.parse(value);
            } catch (DateTimeException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
