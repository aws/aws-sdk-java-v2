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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link AtomicBoolean} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a boolean.
 *
 * <p>
 * This supports reading every boolean value supported by DynamoDB, making it fully compatible with custom converters as
 * well as internal converters (e.g. {@link BooleanAttributeConverter}).
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class AtomicBooleanAttributeConverter implements AttributeConverter<AtomicBoolean> {
    private static final BooleanAttributeConverter BOOLEAN_CONVERTER = BooleanAttributeConverter.create();

    private AtomicBooleanAttributeConverter() {
    }

    @Override
    public EnhancedType<AtomicBoolean> type() {
        return EnhancedType.of(AtomicBoolean.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.BOOL;
    }

    public static AtomicBooleanAttributeConverter create() {
        return new AtomicBooleanAttributeConverter();
    }

    @Override
    public AttributeValue transformFrom(AtomicBoolean input) {
        return AttributeValue.builder().bool(input.get()).build();
    }

    @Override
    public AtomicBoolean transformTo(AttributeValue input) {
        return new AtomicBoolean(BOOLEAN_CONVERTER.transformTo(input));
    }
}
