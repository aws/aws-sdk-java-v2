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

import java.time.DateTimeException;
import java.time.ZoneOffset;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.ZoneOffsetStringConverter;
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
@SdkInternalApi
@ThreadSafe
@Immutable
public final class ZoneOffsetAttributeConverter implements AttributeConverter<ZoneOffset> {
    public static final ZoneOffsetStringConverter STRING_CONVERTER = ZoneOffsetStringConverter.create();

    public static ZoneOffsetAttributeConverter create() {
        return new ZoneOffsetAttributeConverter();
    }

    @Override
    public EnhancedType<ZoneOffset> type() {
        return EnhancedType.of(ZoneOffset.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(ZoneOffset input) {
        return AttributeValue.builder().s(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public ZoneOffset transformTo(AttributeValue input) {
        try {
            return EnhancedAttributeValue.fromAttributeValue(input).convert(Visitor.INSTANCE);
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
