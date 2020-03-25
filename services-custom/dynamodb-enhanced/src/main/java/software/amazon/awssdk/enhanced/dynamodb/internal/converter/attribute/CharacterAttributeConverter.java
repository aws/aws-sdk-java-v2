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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.CharacterStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Character} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a single-character string.
 *
 * <p>
 * This only supports reading a single character from DynamoDB. Any string longer than 1 character will cause a RuntimeException
 * during conversion.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class CharacterAttributeConverter implements AttributeConverter<Character>, PrimitiveConverter<Character> {
    private static final Visitor VISITOR = new Visitor();
    private static final CharacterStringConverter STRING_CONVERTER = CharacterStringConverter.create();

    private CharacterAttributeConverter() {
    }

    public static CharacterAttributeConverter create() {
        return new CharacterAttributeConverter();
    }

    @Override
    public EnhancedType<Character> type() {
        return EnhancedType.of(Character.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(Character input) {
        return AttributeValue.builder().s(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public Character transformTo(AttributeValue input) {
        if (input.s() != null) {
            return EnhancedAttributeValue.fromString(input.s()).convert(VISITOR);
        }

        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    @Override
    public EnhancedType<Character> primitiveType() {
        return EnhancedType.of(char.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Character> {
        private Visitor() {
            super(Character.class, CharacterAttributeConverter.class);
        }

        @Override
        public Character convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
