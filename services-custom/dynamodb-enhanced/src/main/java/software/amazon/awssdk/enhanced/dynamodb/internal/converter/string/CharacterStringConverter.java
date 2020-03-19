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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link Character} and {@link String}.
 *
 * <p>
 * This converts values using {@link Character#toString()} and {@link String#charAt(int)}. If the string value is longer
 * than 1 character, an exception will be raised.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class CharacterStringConverter implements StringConverter<Character>, PrimitiveConverter<Character> {
    private CharacterStringConverter() {
    }

    public static CharacterStringConverter create() {
        return new CharacterStringConverter();
    }

    @Override
    public EnhancedType<Character> type() {
        return EnhancedType.of(Character.class);
    }

    @Override
    public EnhancedType<Character> primitiveType() {
        return EnhancedType.of(char.class);
    }

    @Override
    public Character fromString(String string) {
        Validate.isTrue(string.length() == 1, "Character string was not of length 1: %s", string);
        return string.charAt(0);
    }
}
