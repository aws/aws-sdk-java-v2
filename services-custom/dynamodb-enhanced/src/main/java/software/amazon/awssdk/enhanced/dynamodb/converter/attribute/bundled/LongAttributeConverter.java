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
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.LongStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Long} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports reading numbers between {@link Long#MIN_VALUE} and {@link Long#MAX_VALUE} from DynamoDB. For smaller
 * numbers, consider using {@link ShortAttributeConverter} or {@link IntegerAttributeConverter}. For larger numbers, consider
 * using {@link BigIntegerAttributeConverter}. Numbers outside of the supported range will cause a {@link NumberFormatException}
 * on conversion.
 *
 * <p>
 * This does not support reading decimal numbers. For decimal numbers, consider using {@link FloatAttributeConverter},
 * {@link DoubleAttributeConverter} or {@link BigDecimalAttributeConverter}. Decimal numbers will cause a
 * {@link NumberFormatException} on conversion.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class LongAttributeConverter implements AttributeConverter<Long>, PrimitiveConverter<Long> {
    private static final Visitor VISITOR = new Visitor();
    private static final LongStringConverter STRING_CONVERTER = LongStringConverter.create();

    private LongAttributeConverter() {
    }

    @Override
    public TypeToken<Long> type() {
        return TypeToken.of(Long.class);
    }

    public static LongAttributeConverter create() {
        return new LongAttributeConverter();
    }

    @Override
    public AttributeValue transformFrom(Long input) {
        return ItemAttributeValue.fromNumber(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public Long transformTo(AttributeValue input) {
        if (input.n() != null) {
            return ItemAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }

        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(VISITOR);
    }

    @Override
    public TypeToken<Long> primitiveType() {
        return TypeToken.of(long.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Long> {
        private Visitor() {
            super(Long.class, LongAttributeConverter.class);
        }

        @Override
        public Long convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public Long convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
