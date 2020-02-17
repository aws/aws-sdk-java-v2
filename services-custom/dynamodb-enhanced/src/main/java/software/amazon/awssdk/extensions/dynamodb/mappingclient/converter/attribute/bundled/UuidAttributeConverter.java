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

import java.util.UUID;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.UuidStringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeConvertingVisitor;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link UUID} and {@link AttributeValue}.
 *
 * <p>
 * This supports storing and reading values in DynamoDB as a string.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class UuidAttributeConverter implements AttributeConverter<UUID> {
    public static final UuidStringConverter STRING_CONVERTER = UuidStringConverter.create();

    public static UuidAttributeConverter create() {
        return new UuidAttributeConverter();
    }

    @Override
    public TypeToken<UUID> type() {
        return TypeToken.of(UUID.class);
    }

    @Override
    public AttributeValue transformFrom(UUID input) {
        return ItemAttributeValue.fromString(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public UUID transformTo(AttributeValue input) {
        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(Visitor.INSTANCE);
    }

    private static final class Visitor extends TypeConvertingVisitor<UUID> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(UUID.class, UuidAttributeConverter.class);
        }

        @Override
        public UUID convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
