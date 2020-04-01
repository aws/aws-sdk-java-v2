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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.DoubleStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Double} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports converting numbers stored in DynamoDB into a double-precision floating point number, within the range
 * {@link Double#MIN_VALUE}, {@link Double#MAX_VALUE}. For less precision or smaller values, consider using
 * {@link FloatAttributeConverter}. For greater precision or larger values, consider using {@link BigDecimalAttributeConverter}.
 *
 * <p>
 * If values are known to be whole numbers, it is recommended to use a perfect-precision whole number representation like those
 * provided by {@link ShortAttributeConverter}, {@link IntegerAttributeConverter} or {@link BigIntegerAttributeConverter}.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class DoubleAttributeConverter implements AttributeConverter<Double>, PrimitiveConverter<Double> {
    private static final Visitor VISITOR = new Visitor();
    private static final DoubleStringConverter STRING_CONVERTER = DoubleStringConverter.create();

    private DoubleAttributeConverter() {
    }

    public static DoubleAttributeConverter create() {
        return new DoubleAttributeConverter();
    }

    @Override
    public EnhancedType<Double> type() {
        return EnhancedType.of(Double.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(Double input) {
        ConverterUtils.validateDouble(input);
        return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public Double transformTo(AttributeValue input) {
        Double result;
        if (input.n() != null) {
            result = EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        } else {
            result = EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
        }

        ConverterUtils.validateDouble(result);
        return result;
    }

    @Override
    public EnhancedType<Double> primitiveType() {
        return EnhancedType.of(double.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Double> {
        private Visitor() {
            super(Double.class, DoubleAttributeConverter.class);
        }

        @Override
        public Double convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public Double convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
