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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link SdkBytes} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a binary blob.
 *
 * <p>
 * This supports reading every byte value supported by DynamoDB, making it fully compatible with custom converters as
 * well as internal converters (e.g. {@link ByteArrayAttributeConverter}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class SdkBytesAttributeConverter implements AttributeConverter<SdkBytes> {
    private static final Visitor VISITOR = new Visitor();

    private SdkBytesAttributeConverter() {
    }

    @Override
    public EnhancedType<SdkBytes> type() {
        return EnhancedType.of(SdkBytes.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.B;
    }

    public static SdkBytesAttributeConverter create() {
        return new SdkBytesAttributeConverter();
    }

    @Override
    public AttributeValue transformFrom(SdkBytes input) {
        return AttributeValue.builder().b(input).build();
    }

    @Override
    public SdkBytes transformTo(AttributeValue input) {
        return EnhancedAttributeValue.fromBytes(input.b()).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<SdkBytes> {
        private Visitor() {
            super(SdkBytes.class, SdkBytesAttributeConverter.class);
        }

        @Override
        public SdkBytes convertBytes(SdkBytes value) {
            return value;
        }
    }
}
