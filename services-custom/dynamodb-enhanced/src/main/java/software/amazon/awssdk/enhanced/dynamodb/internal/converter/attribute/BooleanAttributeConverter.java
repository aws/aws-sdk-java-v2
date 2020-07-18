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

import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.PrimitiveConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.BooleanStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link AtomicBoolean} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a boolean.
 *
 * <p>
 * This supports reading every boolean value supported by DynamoDB, making it fully compatible with custom converters as well
 * as internal converters (e.g. {@link AtomicBooleanAttributeConverter}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class BooleanAttributeConverter implements AttributeConverter<Boolean>, PrimitiveConverter<Boolean> {
    private static final Visitor VISITOR = new Visitor();
    private static final BooleanStringConverter STRING_CONVERTER = BooleanStringConverter.create();

    private BooleanAttributeConverter() {
    }

    public static BooleanAttributeConverter create() {
        return new BooleanAttributeConverter();
    }

    @Override
    public EnhancedType<Boolean> type() {
        return EnhancedType.of(Boolean.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.BOOL;
    }

    @Override
    public AttributeValue transformFrom(Boolean input) {
        return AttributeValue.builder().bool(input).build();
    }

    @Override
    public Boolean transformTo(AttributeValue input) {
        if (input.bool() != null) {
            return EnhancedAttributeValue.fromBoolean(input.bool()).convert(VISITOR);
        }
        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    @Override
    public EnhancedType<Boolean> primitiveType() {
        return EnhancedType.of(boolean.class);
    }

    private static final class Visitor extends TypeConvertingVisitor<Boolean> {
        private Visitor() {
            super(Boolean.class, BooleanAttributeConverter.class);
        }

        @Override
        public Boolean convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public Boolean convertNumber(String value) {
            switch (value) {
                case "0": return false;
                case "1": return true;
                default: throw new IllegalArgumentException("Number could not be converted to boolean: " + value);
            }
        }

        @Override
        public Boolean convertBoolean(Boolean value) {
            return value;
        }
    }
}
