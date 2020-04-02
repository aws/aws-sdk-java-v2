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
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


/**
 * A converter between {@link StringBuffer} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * This supports reading any DynamoDB attribute type into a string builder.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class StringBuilderAttributeConverter implements AttributeConverter<StringBuilder> {
    public static final StringAttributeConverter STRING_CONVERTER = StringAttributeConverter.create();

    public static StringBuilderAttributeConverter create() {
        return new StringBuilderAttributeConverter();
    }

    @Override
    public EnhancedType<StringBuilder> type() {
        return EnhancedType.of(StringBuilder.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(StringBuilder input) {
        return STRING_CONVERTER.transformFrom(input.toString());
    }

    @Override
    public StringBuilder transformTo(AttributeValue input) {
        return new StringBuilder(STRING_CONVERTER.transformTo(input));
    }
}
