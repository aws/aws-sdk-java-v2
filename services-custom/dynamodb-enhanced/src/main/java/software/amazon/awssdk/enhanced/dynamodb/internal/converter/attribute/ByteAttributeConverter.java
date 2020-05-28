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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.ByteStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Byte} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a single byte.
 *
 * <p>
 * This only supports reading a single byte from DynamoDB. Any binary data greater than 1 byte will cause a RuntimeException
 * during conversion.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class ByteAttributeConverter implements AttributeConverter<Byte>, PrimitiveConverter<Byte> {
    private static final ByteStringConverter STRING_CONVERTER = ByteStringConverter.create();
    private static final Visitor VISITOR = new Visitor();

    private ByteAttributeConverter() {
    }

    public static ByteAttributeConverter create() {
        return new ByteAttributeConverter();
    }

    @Override
    public AttributeValue transformFrom(Byte input) {
        return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public Byte transformTo(AttributeValue input) {
        if (input.b() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    @Override
    public EnhancedType<Byte> type() {
        return EnhancedType.of(Byte.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public EnhancedType<Byte> primitiveType() {
        return EnhancedType.of(byte.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Byte> {
        private Visitor() {
            super(Byte.class, ByteAttributeConverter.class);
        }

        @Override
        public Byte convertNumber(String number) {
            return Byte.parseByte(number);
        }
    }
}
