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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.GenericObjectStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 *
 * A fallback {@link AttributeConverter} used when no specific converter is registered. Serializes to and from strings using
 * {@link GenericObjectStringConverter}, and stores as {@code SS}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public final class FallbackAttributeConverter<T> implements AttributeConverter<T> {

    private final GenericObjectStringConverter<T> stringConverter;

    private FallbackAttributeConverter(GenericObjectStringConverter<T> stringConverter) {
        this.stringConverter = stringConverter;
    }

    public static <T> FallbackAttributeConverter<T> create(EnhancedType<T> type) {
        return new FallbackAttributeConverter<>(GenericObjectStringConverter.create(type));
    }

    @Override
    public EnhancedType<T> type() {
        return stringConverter.type();
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(T input) {
        return AttributeValue.builder().s(stringConverter.toString(input)).build();
    }

    @Override
    public T transformTo(AttributeValue input) {
        return stringConverter.fromString(input.s());
    }
}
