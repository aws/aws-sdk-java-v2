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
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.SdkNumberStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link SdkNumber} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a number.
 *
 * <p>
 * This supports reading the full range of integers supported by DynamoDB. For smaller numbers, consider using
 * {@link ShortAttributeConverter}, {@link IntegerAttributeConverter} or {@link LongAttributeConverter}.
 *
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class SdkNumberAttributeConverter implements AttributeConverter<SdkNumber> {
    private static final Visitor VISITOR = new Visitor();
    private static final SdkNumberStringConverter STRING_CONVERTER = SdkNumberStringConverter.create();

    private SdkNumberAttributeConverter() {
    }

    public static SdkNumberAttributeConverter create() {
        return new SdkNumberAttributeConverter();
    }

    @Override
    public EnhancedType<SdkNumber> type() {
        return EnhancedType.of(SdkNumber.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }

    @Override
    public AttributeValue transformFrom(SdkNumber input) {
        return AttributeValue.builder().n(STRING_CONVERTER.toString(input)).build();
    }

    @Override
    public SdkNumber transformTo(AttributeValue input) {
        if (input.n() != null) {
            return EnhancedAttributeValue.fromNumber(input.n()).convert(VISITOR);
        }
        return EnhancedAttributeValue.fromAttributeValue(input).convert(VISITOR);
    }

    private static final class Visitor extends TypeConvertingVisitor<SdkNumber> {
        private Visitor() {
            super(SdkNumber.class, SdkNumberAttributeConverter.class);
        }

        @Override
        public SdkNumber convertString(String value) {
            return STRING_CONVERTER.fromString(value);
        }

        @Override
        public SdkNumber convertNumber(String value) {
            return STRING_CONVERTER.fromString(value);
        }
    }
}
