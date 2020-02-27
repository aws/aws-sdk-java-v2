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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.converter.internal.ConverterUtils;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.FloatStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Float} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports converting numbers stored in DynamoDB into a single-precision floating point number, within the range
 * {@link Float#MIN_VALUE}, {@link Float#MAX_VALUE}. For more precision or larger values, consider using
 * {@link DoubleAttributeConverter} or {@link BigDecimalAttributeConverter}.
 *
 * <p>
 * If values are known to be whole numbers, it is recommended to use a perfect-precision whole number representation like those
 * provided by {@link ShortAttributeConverter}, {@link IntegerAttributeConverter} or {@link BigIntegerAttributeConverter}.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class FloatAttributeConverter implements AttributeConverter<Float>, PrimitiveConverter<Float> {
    private static final Visitor VISITOR = new Visitor();
    private static final FloatStringConverter STRING_CONVERTER = FloatStringConverter.create();

    private FloatAttributeConverter() {
    }

    public static FloatAttributeConverter create() {
        return new FloatAttributeConverter();
    }

    @Override
    public TypeToken<Float> type() {
        return TypeToken.of(Float.class);
    }

    @Override
    public AttributeValue transformFrom(Float input) {
        ConverterUtils.validateFloat(input);
        return ItemAttributeValue.fromNumber(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public Float transformTo(AttributeValue input) {
        Float result;
        if (input.n() != null) {
            result = ItemAttributeValue.fromNumber(input.n()).convert(VISITOR);
        } else {
            result = ItemAttributeValue.fromGeneratedAttributeValue(input).convert(VISITOR);
        }

        ConverterUtils.validateFloat(result);
        return result;
    }

    @Override
    public TypeToken<Float> primitiveType() {
        return TypeToken.of(float.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Float> {
        private Visitor() {
            super(Float.class, FloatAttributeConverter.class);
        }

        @Override
        public Float convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public Float convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
