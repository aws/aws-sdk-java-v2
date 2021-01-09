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
import java.util.OptionalInt;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.OptionalIntStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link OptionalInt} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports reading numbers between {@link Integer#MIN_VALUE} and {@link Integer#MAX_VALUE} from DynamoDB. Null values are
 * converted to {@code OptionalInt.empty()}. For larger numbers, consider using the {@link OptionalLongAttributeConverter} or
 * the {@link OptionalAttributeConverter} along with a {@link BigInteger}. For shorter numbers, consider using the
 * {@link OptionalAttributeConverter} along with a {@link Short} type.
 *
 * <p>
 * This does not support reading decimal numbers. For decimal numbers, consider using {@link OptionalDoubleAttributeConverter},
 * or the {@link OptionalAttributeConverter} with a {@link Float} or {@link BigDecimal}. Decimal numbers will cause a
 * {@link NumberFormatException} on conversion.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class OptionalIntAttributeConverter implements AttributeConverter<OptionalInt> {
    private static final Visitor VISITOR = new Visitor();
    private static final OptionalIntStringConverter STRING_CONVERTER = OptionalIntStringConverter.create();

    private OptionalIntAttributeConverter() {
    }

    @Override
    public EnhancedType<OptionalInt> type() {
        return EnhancedType.of(OptionalInt.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    public static OptionalIntAttributeConverter create() {
        return new OptionalIntAttributeConverter();
    }

    @Override
    public AttributeValue transformFrom(OptionalInt input) {
        if (input.isPresent()) {
            return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
        } else {
            return AttributeValues.nullAttributeValue();
        }
    }

    @Override
    public OptionalInt transformTo(AttributeValue input) {
        if (input.n() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<OptionalInt> {
        private Visitor() {
            super(OptionalInt.class, OptionalIntAttributeConverter.class);
        }

        @Override
        public OptionalInt convertNull() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public OptionalInt convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
