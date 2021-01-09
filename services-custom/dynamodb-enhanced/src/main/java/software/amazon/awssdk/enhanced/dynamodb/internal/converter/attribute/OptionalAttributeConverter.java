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

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link Optional} and {@link EnhancedAttributeValue}.
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class OptionalAttributeConverter<T> implements AttributeConverter<Optional<T>> {
    private final AttributeConverter delegate;

    private OptionalAttributeConverter(AttributeConverter delegate) {
        this.delegate = delegate;

    }

    public static OptionalAttributeConverter create(AttributeConverter delegate) {
        return new OptionalAttributeConverter(delegate);
    }

    @Override
    public EnhancedType<Optional<T>> type() {
        return EnhancedType.optionalOf(delegate.type().rawClass());
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(Optional<T> input) {
        if (!input.isPresent()) {
            return AttributeValues.nullAttributeValue();
        }

        return delegate.transformFrom(input.get());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> transformTo(AttributeValue input) {
        Optional<T> result;
        if (Boolean.TRUE.equals(input.nul())) {
            // This is safe - An Optional.empty() can be used for any Optional<?> subtype.
            result = Optional.empty();
        } else {
            result = (Optional<T>) Optional.ofNullable(delegate.transformTo(input));
        }

        return result;
    }
}
