/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalDouble;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.OptionalDoubleStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ConverterUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * A converter between {@link OptionalDouble} and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports converting numbers stored in DynamoDB into a double-precision floating point number, within the range
 * {@link Double#MIN_VALUE}, {@link Double#MAX_VALUE}. Null values are converted to {@code OptionalDouble.empty()}. For less
 * precision or smaller values, consider using {@link OptionalSubtypeAttributeConverter} along with a {@link Float} type.
 * For greater precision or larger values, consider using {@link OptionalSubtypeAttributeConverter} along with a
 * {@link BigDecimal} type.
 *
 * <p>
 * If values are known to be whole numbers, it is recommended to use a perfect-precision whole number representation like those
 * provided by {@link OptionalIntAttributeConverter}, {@link OptionalLongAttributeConverter}, or a
 * {@link OptionalSubtypeAttributeConverter} along with a {@link BigInteger} type.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class OptionalDoubleAttributeConverter implements AttributeConverter<OptionalDouble> {
    private static final Visitor VISITOR = new Visitor();
    private static final OptionalDoubleStringConverter STRING_CONVERTER = OptionalDoubleStringConverter.create();

    private OptionalDoubleAttributeConverter() {}

    public static OptionalDoubleAttributeConverter create() {
        return new OptionalDoubleAttributeConverter();
    }

    @Override
    public TypeToken<OptionalDouble> type() {
        return TypeToken.of(OptionalDouble.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(OptionalDouble input, ConversionContext context) {
        if (input.isPresent()) {
            ConverterUtils.validateDouble(input.getAsDouble());
            return ItemAttributeValue.fromNumber(STRING_CONVERTER.toString(input));
        } else {
            return ItemAttributeValue.nullValue();
        }
    }

    @Override
    public OptionalDouble fromAttributeValue(ItemAttributeValue input,
                                             ConversionContext context) {
        OptionalDouble result = input.convert(VISITOR);
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
