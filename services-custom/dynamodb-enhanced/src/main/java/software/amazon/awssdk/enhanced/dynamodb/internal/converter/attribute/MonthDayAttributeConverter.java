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

import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils.padLeft2;

import java.time.DateTimeException;
import java.time.MonthDay;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link MonthDay} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a number, so that they can be sorted numerically as part of a sort key.
 *
 * <p>
 * LocalTimes are stored in the format "MMDD", where:
 * <ol>
 *     <li>M is a 2-character, zero-prefixed month between 01 and 12</li>
 *     <li>D is a 2-character, zero-prefixed day between 01 and 31</li>
 * </ol>
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>{@code MonthDay.of(5, 21)} is stored as {@code ItemAttributeValueMapper.fromNumber("0521")}</li>
 *     <li>{@code MonthDay.of(12, 1)} is stored as {@code ItemAttributeValueMapper.fromNumber("1201")}</li>
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
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(MonthDay input) {
        String value = "" +
                       padLeft2(input.getMonthValue()) +
                       padLeft2(input.getDayOfMonth());
        return AttributeValue.builder().n(value).build();
    }

    @Override
    public MonthDay transformTo(AttributeValue input) {
        if (input.n() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<MonthDay> {
        private Visitor() {
            super(MonthDay.class, MonthDayAttributeConverter.class);
        }

        @Override
        public MonthDay convertNumber(String value) {
            Validate.isTrue(value.length() == 4, "Invalid Month/Day length: %s, expected 4 (MMDD)", value.length());
            String[] chunkedMonthDay = ConverterUtils.chunk(value, 2, 2);
            try {
                return MonthDay.of(Integer.parseInt(chunkedMonthDay[0]),
                                   Integer.parseInt(chunkedMonthDay[1]));
            } catch (DateTimeException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
