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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.CharSequenceStringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link CharSequence} and {@link AttributeValue}.
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
@SdkPublicApi
@ThreadSafe
@Immutable
public final class CharSequenceAttributeConverter implements AttributeConverter<CharSequence> {
    private static final CharSequenceStringConverter CHAR_SEQUENCE_STRING_CONVERTER = CharSequenceStringConverter.create();
    private static final StringAttributeConverter STRING_ATTRIBUTE_CONVERTER = StringAttributeConverter.create();

    private CharSequenceAttributeConverter() {
    }

    public static CharSequenceAttributeConverter create() {
        return new CharSequenceAttributeConverter();
    }

    @Override
    public TypeToken<CharSequence> type() {
        return TypeToken.of(CharSequence.class);
    }

    @Override
    public AttributeValue transformFrom(CharSequence input) {
        return ItemAttributeValue.fromString(CHAR_SEQUENCE_STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public CharSequence transformTo(AttributeValue input) {
        String string = STRING_ATTRIBUTE_CONVERTER.transformTo(input);
        return CHAR_SEQUENCE_STRING_CONVERTER.fromString(string);
    }
}
