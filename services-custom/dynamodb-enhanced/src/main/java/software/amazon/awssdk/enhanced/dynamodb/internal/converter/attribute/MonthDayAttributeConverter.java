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

import java.time.MonthDay;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link MonthDay} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a String.
 *
 * <p>
 * MonthDays are stored in the official {@link MonthDay} format "--MM-DD", where:
 * <ol>
 *     <li>M is a 2-character, zero-prefixed month between 01 and 12</li>
 *     <li>D is a 2-character, zero-prefixed day between 01 and 31</li>
 * </ol>
 * See {@link MonthDay} for more details on the serialization format.
 *
 * <p>
 * This serialization is lexicographically orderable.
 * <p>
 *
 * Examples:
 * <ul>
 *     <li>{@code MonthDay.of(5, 21)} is stored as as an AttributeValue with the String "--05-21"}</li>
 *     <li>{@code MonthDay.of(12, 1)} is stored as as an AttributeValue with the String "--12-01"}</li>
 * </ul>
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class MonthDayAttributeConverter implements AttributeConverter<MonthDay> {
    private static final Visitor VISITOR = new Visitor();

    private MonthDayAttributeConverter() {
    }

    public static MonthDayAttributeConverter create() {
        return new MonthDayAttributeConverter();
    }

    @Override
    public EnhancedType<MonthDay> type() {
        return EnhancedType.of(MonthDay.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(MonthDay input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public MonthDay transformTo(AttributeValue input) {
        try {
            if (input.s() != null) {
                return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
            }

            return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class Visitor extends TypeConvertingVisitor<MonthDay> {
        private Visitor() {
            super(MonthDay.class, MonthDayAttributeConverter.class);
        }

        @Override
        public MonthDay convertString(String value) {
            return MonthDay.parse(value);
        }
    }
}
