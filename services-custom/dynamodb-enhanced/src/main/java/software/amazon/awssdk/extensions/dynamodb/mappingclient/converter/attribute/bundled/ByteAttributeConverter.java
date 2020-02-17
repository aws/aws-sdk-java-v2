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
import software.amazon.awssdk.utils.Validate;

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
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ByteAttributeConverter implements AttributeConverter<Byte> {
    private static final Visitor VISITOR = new Visitor();

    private ByteAttributeConverter() {
    }

    public static ByteAttributeConverter create() {
        return new ByteAttributeConverter();
    }

    @Override
    public TypeToken<Byte> type() {
        return TypeToken.of(Byte.class);
    }

    @Override
    public AttributeValue transformFrom(Byte input) {
        return ItemAttributeValue.fromBytes(SdkBytes.fromByteArray(new byte[] {input})).toGeneratedAttributeValue();
    }

    @Override
    public Byte transformTo(AttributeValue input) {
        if (input.b() != null) {
            return ItemAttributeValue.fromBytes(input.b()).convert(VISITOR);
        }

        return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<Byte> {
        private Visitor() {
            super(Byte.class, ByteAttributeConverter.class);
        }

        @Override
        public Byte convertBytes(SdkBytes value) {
            byte[] bytes = value.asByteArray();
            Validate.isTrue(bytes.length == 1, "Cannot convert byte array of length %s to a byte.", bytes.length);
            return bytes[0];
        }
    }
}
