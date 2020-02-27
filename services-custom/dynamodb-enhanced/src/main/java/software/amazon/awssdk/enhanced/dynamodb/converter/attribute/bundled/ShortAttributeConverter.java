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
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ShortStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeValueType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Short} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports reading numbers between {@link Short#MIN_VALUE} and {@link Short#MAX_VALUE} from DynamoDB. For larger numbers,
 * consider using {@link IntegerAttributeConverter}, {@link LongAttributeConverter} or {@link BigIntegerAttributeConverter}.
 * Numbers outside of the supported range will cause a {@link NumberFormatException} on conversion.
 *
 * <p>
 * This does not support reading decimal numbers. For decimal numbers, consider using {@link FloatAttributeConverter},
 * {@link DoubleAttributeConverter} or {@link BigDecimalAttributeConverter}. Decimal numbers will cause a
 * {@link NumberFormatException} on conversion.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ShortAttributeConverter implements AttributeConverter<Short>, PrimitiveConverter<Short> {
    public static final ShortStringConverter STRING_CONVERTER = ShortStringConverter.create();

    public static ShortAttributeConverter create() {
        return new ShortAttributeConverter();
    }

    @Override
    public TypeToken<Short> type() {
        return TypeToken.of(Short.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(Short input) {
        return ItemAttributeValue.fromNumber(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public Short transformTo(AttributeValue input) {
        if (input.n() != null) {
            return ItemAttributeValue.fromNumber(input.n()).convert(Visitor.INSTANCE);
        }

        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(Visitor.INSTANCE);
    }

    @Override
    public TypeToken<Short> primitiveType() {
        return TypeToken.of(short.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Short> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(Short.class, ShortAttributeConverter.class);
        }

        @Override
        public Short convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public Short convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
