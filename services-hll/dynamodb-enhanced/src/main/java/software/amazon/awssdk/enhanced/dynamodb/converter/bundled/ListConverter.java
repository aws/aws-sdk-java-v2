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

package software.amazon.awssdk.enhanced.dynamodb.converter.bundled;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.InstanceOfConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link List} subtypes and {@link ItemAttributeValue}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class ListConverter extends InstanceOfConverter<List<?>> {
    public ListConverter() {
        super(List.class);
    }

    @Override
    protected ItemAttributeValue doToAttributeValue(List<?> input, ConversionContext context) {
        List<ItemAttributeValue> attributeValues = new ArrayList<>();
        for (Object object : input) {
            attributeValues.add(context.converter().toAttributeValue(object, context));
        }
        return ItemAttributeValue.fromListOfAttributeValues(attributeValues);
    }

    @Override
    protected List<?> doFromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context) {
        Class<?> listType = desiredType.rawClass();
        List<TypeToken<?>> listTypeParameters = desiredType.rawClassParameters();

        Validate.isTrue(listTypeParameters.size() == 1,
                        "The desired List type appears to be parameterized with more than 1 type: %s", desiredType);
        TypeToken<?> parameterType = listTypeParameters.get(0);

        return input.convert(new TypeConvertingVisitor<List<?>>(List.class, ListConverter.class) {
            @Override
            public List<?> convertSetOfStrings(List<String> value) {
                return convertCollection(value, ItemAttributeValue::fromString);
            }

            @Override
            public List<?> convertSetOfNumbers(List<String> value) {
                return convertCollection(value, ItemAttributeValue::fromNumber);
            }

            @Override
            public List<?> convertSetOfBytes(List<SdkBytes> value) {
                return convertCollection(value, ItemAttributeValue::fromBytes);
            }

            @Override
            public List<?> convertListOfAttributeValues(Collection<ItemAttributeValue> value) {
                return convertCollection(value, Function.identity());
            }

            private <T> List<?> convertCollection(Collection<T> collection,
                                                  Function<T, ItemAttributeValue> toAttributeValueFunction) {
                return collection.stream()
                                 .map(toAttributeValueFunction)
                                 .map(v -> context.converter().fromAttributeValue(v, parameterType, context))
                                 .collect(Collectors.toCollection(() -> createList(listType)));
            }
        });
    }

    private List<Object> createList(Class<?> listType) {
        if (listType.isInterface()) {
            Validate.isTrue(listType.equals(List.class), "Requested interface type %s is not supported.", listType);
            return new ArrayList<>();
        }

        try {
            return (List<Object>) listType.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to instantiate the requested type " + listType.getTypeName() + ".", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Requested type " + listType.getTypeName() + " is not supported, because it " +
                                            "does not have a zero-arg constructor.", e);
        }
    }
}
