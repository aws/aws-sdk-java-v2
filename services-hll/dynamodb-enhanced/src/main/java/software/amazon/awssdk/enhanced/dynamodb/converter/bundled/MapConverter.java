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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.InstanceOfConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link Map} subtypes and {@link ItemAttributeValue}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class MapConverter extends InstanceOfConverter<Map<?, ?>> {
    private final Function<Object, String> keyToStringConverter;

    public MapConverter() {
        this(Object::toString);
    }

    public MapConverter(Function<Object, String> keyToStringConverter) {
        super(Map.class);
        this.keyToStringConverter = keyToStringConverter;
    }

    @Override
    protected ItemAttributeValue doToAttributeValue(Map<?, ?> input, ConversionContext context) {
        Map<String, ItemAttributeValue> result = new LinkedHashMap<>();
        input.forEach((key, value) -> result.put(keyToStringConverter.apply(key),
                                                 context.converter().toAttributeValue(value, context)));
        return ItemAttributeValue.fromMap(result);
    }

    @Override
    protected Map<?, ?> doFromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context) {
        Class<?> mapType = desiredType.rawClass();
        List<TypeToken<?>> mapTypeParameters = desiredType.rawClassParameters();

        Validate.isTrue(mapTypeParameters.size() == 2,
                        "The desired Map type appears to be parameterized with more than 2 types: %s", desiredType);
        TypeToken<?> keyType = mapTypeParameters.get(0);
        TypeToken<?> valueType = mapTypeParameters.get(1);

        return input.convert(new TypeConvertingVisitor<Map<?, ?>>(Map.class, MapConverter.class) {
            @Override
            public Map<?, ?> convertMap(Map<String, ItemAttributeValue> value) {
                Map<Object, Object> result = createMap(mapType);
                value.forEach((k, v) -> {
                    result.put(context.converter().fromAttributeValue(ItemAttributeValue.fromString(k), keyType, context),
                               context.converter().fromAttributeValue(v, valueType, context));
                });
                return result;
            }
        });
    }

    private Map<Object, Object> createMap(Class<?> mapType) {
        if (mapType.isInterface()) {
            Validate.isTrue(mapType.equals(Map.class), "Requested interface type %s is not supported.", mapType);
            return new LinkedHashMap<>();
        }

        try {
            return (Map<Object, Object>) mapType.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to instantiate the requested type " + mapType.getTypeName() + ".", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Requested type " + mapType.getTypeName() + " is not supported, because it " +
                                            "does not have a zero-arg constructor.", e);
        }
    }
}
