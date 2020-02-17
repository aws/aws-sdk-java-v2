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

import java.time.DateTimeException;
import java.time.ZoneOffset;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.ZoneOffsetStringConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeConvertingVisitor;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link ZoneOffset} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a string using {@link ZoneOffset#toString()} and {@link ZoneOffset#of(String)}.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ZoneOffsetAttributeConverter implements AttributeConverter<ZoneOffset> {
    public static final ZoneOffsetStringConverter STRING_CONVERTER = ZoneOffsetStringConverter.create();

    public static ZoneOffsetAttributeConverter create() {
        return new ZoneOffsetAttributeConverter();
    }

    @Override
    public TypeToken<ZoneOffset> type() {
        return TypeToken.of(ZoneOffset.class);
    }

    @Override
    public AttributeValue transformFrom(ZoneOffset input) {
        return ItemAttributeValue.fromString(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public ZoneOffset transformTo(AttributeValue input) {
        try {
            return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(Visitor.INSTANCE);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class Visitor extends TypeConvertingVisitor<ZoneOffset> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(ZoneOffset.class, ZoneOffsetAttributeConverter.class);
        }

        @Override
        public ZoneOffset convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
