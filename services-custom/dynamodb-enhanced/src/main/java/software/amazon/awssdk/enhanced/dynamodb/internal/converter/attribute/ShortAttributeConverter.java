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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.ShortStringConverter;
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
@SdkInternalApi
@ThreadSafe
@Immutable
public final class ShortAttributeConverter implements AttributeConverter<Short>, PrimitiveConverter<Short> {
    public static final ShortStringConverter STRING_CONVERTER = ShortStringConverter.create();

    public static ShortAttributeConverter create() {
        return new ShortAttributeConverter();
    }

    @Override
    public EnhancedType<Short> type() {
        return EnhancedType.of(Short.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(Short input) {
        return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public Short transformTo(AttributeValue input) {
        if (input.n() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(Visitor.INSTANCE);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(Visitor.INSTANCE);
    }

    @Override
    public EnhancedType<Short> primitiveType() {
        return EnhancedType.of(short.class);
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
