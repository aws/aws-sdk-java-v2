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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalDouble;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.OptionalDoubleStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link OptionalDouble} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports converting numbers stored in DynamoDB into a double-precision floating point number, within the range
 * {@link Double#MIN_VALUE}, {@link Double#MAX_VALUE}. Null values are converted to {@code OptionalDouble.empty()}. For less
 * precision or smaller values, consider using {@link OptionalAttributeConverter} along with a {@link Float} type.
 * For greater precision or larger values, consider using {@link OptionalAttributeConverter} along with a
 * {@link BigDecimal} type.
 *
 * <p>
 * If values are known to be whole numbers, it is recommended to use a perfect-precision whole number representation like those
 * provided by {@link OptionalIntAttributeConverter}, {@link OptionalLongAttributeConverter}, or a
 * {@link OptionalAttributeConverter} along with a {@link BigInteger} type.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class OptionalDoubleAttributeConverter implements AttributeConverter<OptionalDouble> {
    private static final Visitor VISITOR = new Visitor();
    private static final OptionalDoubleStringConverter STRING_CONVERTER = OptionalDoubleStringConverter.create();

    private OptionalDoubleAttributeConverter() {
    }

    public static OptionalDoubleAttributeConverter create() {
        return new OptionalDoubleAttributeConverter();
    }

    @Override
    public EnhancedType<OptionalDouble> type() {
        return EnhancedType.of(OptionalDouble.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(OptionalDouble input) {
        if (input.isPresent()) {
            ConverterUtils.validateDouble(input.getAsDouble());
            return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
        } else {
            return AttributeValues.nullAttributeValue();
        }
    }

    @Override
    public OptionalDouble transformTo(AttributeValue input) {
        OptionalDouble result;
        if (input.n() != null) {
            result = EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        } else {
            result = EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        }
        result.ifPresent(ConverterUtils::validateDouble);
        return result;
    }

    private static final class Visitor extends TypeConvertingVisitor<OptionalDouble> {
        private Visitor() {
            super(OptionalDouble.class, OptionalDoubleAttributeConverter.class);
        }

        @Override
        public OptionalDouble convertNull() {
            return OptionalDouble.empty();
        }

        @Override
        public OptionalDouble convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public OptionalDouble convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
