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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.net.URL;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.UrlStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

/**
 * A converter between {@link URL} and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a string, according to the format of {@link URL#URL(String)} and
 * {@link URL#toString()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class UrlAttributeConverter implements AttributeConverter<URL> {
    public static final UrlStringConverter STRING_CONVERTER = UrlStringConverter.create();

    public static UrlAttributeConverter create() {
        return new UrlAttributeConverter();
    }

    @Override
    public TypeToken<URL> type() {
        return TypeToken.of(URL.class);
    }

    @Override
    public ItemAttributeValue toAttributeValue(URL input, ConversionContext context) {
        return ItemAttributeValue.fromString(STRING_CONVERTER.toString(input));
    }

    @Override
    public URL fromAttributeValue(ItemAttributeValue input,
                                  ConversionContext context) {
        return input.convert(Visitor.INSTANCE);
    }

    private static final class Visitor extends TypeConvertingVisitor<URL> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(URL.class, UrlAttributeConverter.class);
        }

        @Override
        public URL convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
