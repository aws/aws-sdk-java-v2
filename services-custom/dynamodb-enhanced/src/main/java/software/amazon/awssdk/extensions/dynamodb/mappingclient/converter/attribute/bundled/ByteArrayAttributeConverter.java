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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeConvertingVisitor;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@code byte[]} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a binary blob.
 *
 * <p>
 * This supports reading every byte value supported by DynamoDB, making it fully compatible with custom converters as
 * well as internal converters (e.g. {@link SdkBytesAttributeConverter}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ByteArrayAttributeConverter implements AttributeConverter<byte[]> {
    private static final Visitor VISITOR = new Visitor();

    private ByteArrayAttributeConverter() {
    }

    public static ByteArrayAttributeConverter create() {
        return new ByteArrayAttributeConverter();
    }

    @Override
    public TypeToken<byte[]> type() {
        return TypeToken.of(byte[].class);
    }

    @Override
    public AttributeValue transformFrom(byte[] input) {
        return ItemAttributeValue.fromBytes(SdkBytes.fromByteArray(input)).toGeneratedAttributeValue();
    }

    @Override
    public byte[] transformTo(AttributeValue input) {
        if (input.b() != null) {
            return ItemAttributeValue.fromBytes(input.b()).convert(VISITOR);
        }

        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<byte[]> {
        private Visitor() {
            super(byte[].class, ByteArrayAttributeConverter.class);
        }

        @Override
        public byte[] convertBytes(SdkBytes value) {
            return value.asByteArray();
        }
    }
}
