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
import java.util.OptionalLong;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.OptionalLongStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link OptionalLong} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports reading numbers between {@link Long#MIN_VALUE} and {@link Long#MAX_VALUE} from DynamoDB. Null values are
 * converted to {@code OptionalLong.empty()}. For larger numbers, consider using the {@link OptionalAttributeConverter}
 * along with a {@link BigInteger}. For shorter numbers, consider using the {@link OptionalIntAttributeConverter} or
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
public final class OptionalLongAttributeConverter implements AttributeConverter<OptionalLong> {
    private static final Visitor VISITOR = new Visitor();
    private static final OptionalLongStringConverter STRING_CONVERTER = OptionalLongStringConverter.create();

    private OptionalLongAttributeConverter() {
    }

    @Override
    public EnhancedType<OptionalLong> type() {
        return EnhancedType.of(OptionalLong.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    public static OptionalLongAttributeConverter create() {
        return new OptionalLongAttributeConverter();
    }

    @Override
    public AttributeValue transformFrom(OptionalLong input) {
        if (input.isPresent()) {
            return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
        } else {
            return AttributeValues.nullAttributeValue();
        }
    }

    @Override
    public OptionalLong transformTo(AttributeValue input) {
        if (input.n() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<OptionalLong> {
        private Visitor() {
            super(OptionalLong.class, OptionalLongAttributeConverter.class);
        }

        @Override
        public OptionalLong convertNull() {
            return OptionalLong.empty();
        }

        @Override
        public OptionalLong convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public OptionalLong convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
