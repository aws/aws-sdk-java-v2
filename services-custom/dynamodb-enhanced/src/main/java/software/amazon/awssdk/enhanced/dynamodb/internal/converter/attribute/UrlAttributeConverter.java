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

import java.net.URL;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.UrlStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link URL} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a string, according to the format of {@link URL#URL(String)} and
 * {@link URL#toString()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class UrlAttributeConverter implements AttributeConverter<URL> {
    public static final UrlStringConverter STRING_CONVERTER = UrlStringConverter.create();

    public static UrlAttributeConverter create() {
        return new UrlAttributeConverter();
    }

    @Override
    public EnhancedType<URL> type() {
        return EnhancedType.of(URL.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(URL input) {
        return AttributeValue.builder().s(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public URL transformTo(AttributeValue input) {
        return EnhancedAttributeValue.fromAttributeValue(input).convert(Visitor.INSTANCE);
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
