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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled;

import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.IntegerStringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeConvertingVisitor;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Integer} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports reading numbers between {@link Integer#MIN_VALUE} and {@link Integer#MAX_VALUE} from DynamoDB. For smaller
 * numbers, consider using {@link ShortAttributeConverter}. For larger numbers, consider using {@link LongAttributeConverter}
 * or {@link BigIntegerAttributeConverter}. Numbers outside of the supported range will cause a {@link NumberFormatException}
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
public final class IntegerAttributeConverter implements AttributeConverter<Integer> {
    public static final IntegerStringConverter INTEGER_STRING_CONVERTER = IntegerStringConverter.create();

    private IntegerAttributeConverter() {
    }

    public static IntegerAttributeConverter create() {
        return new IntegerAttributeConverter();
    }

    @Override
    public TypeToken<Integer> type() {
        return TypeToken.of(Integer.class);
    }

    @Override
    public AttributeValue transformFrom(Integer input) {
        return ItemAttributeValue.fromNumber(INTEGER_STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public Integer transformTo(AttributeValue input) {
        if (input.n() != null) {
            return ItemAttributeValue.fromNumber(input.n()).convert(Visitor.INSTANCE);
        }

        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(Visitor.INSTANCE);
    }

    private static final class Visitor extends TypeConvertingVisitor<Integer> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(Instant.class, IntegerAttributeConverter.class);
        }

        @Override
        public Integer convertString(String value) {
            return INTEGER_STRING_CONVERTER.fromString(value);
        }

        @Override
        public Integer convertNumber(String value) {
            return INTEGER_STRING_CONVERTER.fromString(value);
        }
    }
}
