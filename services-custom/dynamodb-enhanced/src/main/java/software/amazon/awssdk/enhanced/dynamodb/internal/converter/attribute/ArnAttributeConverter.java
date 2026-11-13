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
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.ArnStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Arn} and {@link AttributeValue}.
 *
 * <p>
 * This supports storing and reading values in DynamoDB as a string.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class ArnAttributeConverter implements AttributeConverter<Arn> {
    public static final ArnStringConverter STRING_CONVERTER = ArnStringConverter.create();

    public static ArnAttributeConverter create() {
        return new ArnAttributeConverter();
    }

    @Override
    public EnhancedType<Arn> type() {
        return EnhancedType.of(Arn.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(Arn input) {
        return AttributeValue.builder().s(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public Arn transformTo(AttributeValue input) {
        return EnhancedAttributeValue.fromAttributeValue(input).convert(Visitor.INSTANCE);
    }

    private static final class Visitor extends TypeConvertingVisitor<Arn> {
        private static final Visitor INSTANCE = new Visitor();

        private Visitor() {
            super(Arn.class, ArnAttributeConverter.class);
        }

        @Override
        public Arn convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
