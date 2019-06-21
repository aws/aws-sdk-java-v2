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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link Optional} and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores {@code Optional.empty()} values in DynamoDB as a null. For present values, his uses the
 * {@link ConversionContext#attributeConverter()} to convert the collection contents to an attribute value. This means
 * that the client or item must be configured with a converter for the type contained in the optional.
 *
 * <p>
 * This reads null types as {@code Optional.empty()}, and present values using the
 * {@link ConversionContext#attributeConverter()}. This means that the client or item must be configured with a converter
 * for the requested type in the optional.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class OptionalSubtypeAttributeConverter implements SubtypeAttributeConverter<Optional<?>> {
    private static final TypeToken<Optional<?>> TYPE = new TypeToken<Optional<?>>() {};

    private OptionalSubtypeAttributeConverter() {}

    public static OptionalSubtypeAttributeConverter create() {
        return new OptionalSubtypeAttributeConverter();
    }

    @Override
    public TypeToken<Optional<?>> type() {
        return TYPE;
    }

    @Override
    public ItemAttributeValue toAttributeValue(Optional<?> input, ConversionContext context) {
        if (!input.isPresent()) {
            return context.attributeConverter().toAttributeValue(null, context);
        }

        return context.attributeConverter().toAttributeValue(input.get(), context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Optional<?>> T fromAttributeValue(ItemAttributeValue input,
                                                        TypeToken<T> targetType,
                                                        ConversionContext context) {
        T result;
        if (input.isNull()) {
            // This is safe - An Optional.empty() can be used for any Optional<?> subtype.
            result = (T) Optional.empty();
        } else {
            List<TypeToken<?>> optionalTypeParameters = targetType.rawClassParameters();

            Validate.isTrue(optionalTypeParameters.size() == 1,
                            "The desired Optional type was not parameterized with 1 parameter, somehow: %s", targetType);

            TypeToken<?> desiredType = optionalTypeParameters.get(0);

            // This is safe - Optional is final (so it's not an upcast), and we're populating it with the requested type.
            result = (T) Optional.ofNullable(context.attributeConverter().fromAttributeValue(input, desiredType, context));
        }

        return result;
    }
}
