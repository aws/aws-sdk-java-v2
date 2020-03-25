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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.CharacterArrayStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@code char[]} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * This supports reading every string value supported by DynamoDB, making it fully compatible with custom converters as
 * well as internal converters (e.g. {@link StringAttributeConverter}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class CharacterArrayAttributeConverter implements AttributeConverter<char[]> {
    private static final CharacterArrayStringConverter CHAR_ARRAY_STRING_CONVERTER = CharacterArrayStringConverter.create();
    private static final StringAttributeConverter STRING_ATTRIBUTE_CONVERTER = StringAttributeConverter.create();

    private CharacterArrayAttributeConverter() {
    }

    public static CharacterArrayAttributeConverter create() {
        return new CharacterArrayAttributeConverter();
    }

    @Override
    public EnhancedType<char[]> type() {
        return EnhancedType.of(char[].class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(char[] input) {
        return AttributeValue.builder().s(CHAR_ARRAY_STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public char[] transformTo(AttributeValue input) {
        return CHAR_ARRAY_STRING_CONVERTER.fromString(STRING_ATTRIBUTE_CONVERTER.transformTo(input));
    }
}
