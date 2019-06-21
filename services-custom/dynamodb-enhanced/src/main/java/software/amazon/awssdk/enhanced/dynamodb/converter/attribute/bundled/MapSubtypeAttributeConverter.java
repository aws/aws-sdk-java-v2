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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.MappedTable;
import software.amazon.awssdk.enhanced.dynamodb.Table;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.GenericConvertibleMap;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.KeyValueTypeAwareMap;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.DefaultStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ConvertibleItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between {@link Map} subtypes and {@link ItemAttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as maps from string to attribute values.
 *
 * <p>
 * <b>Key Conversion:</b> This uses the {@link DefaultStringConverter} to convert the map keys to and from strings.
 * Because of this, the client or item must be configured with a converter for the key type, or an exception will be raised. If
 * you wish to statically configure the key type converter, use {@link MapAttributeConverter}.
 *
 * <p>
 * <b>Value Conversion:</b> This uses the {@link ConversionContext#attributeConverter()} to convert the map keys to and from
 * strings. Because of this, the client or item must be configured with a converter for the value type, or an exception will be
 * raised. If you wish to statically configure the value converter, use {@link MapAttributeConverter}.
 *
 * <p>
 * <b>Key and Value Type:</b> To delegate element conversion to the {@link DefaultStringConverter} and
 * {@link ConversionContext#attributeConverter()}, the key and value types must be determined. For converting from Java maps
 * to DynamoDB maps, the key and value's {@link Class} is used. For converting from DynamoDB map to Java map,
 * the output type is selected by the requested attribute type. For {@link MappedTable}s, this is the type associated with the
 * attribute. For {@link Table}s, this is the type passed to the {@link ConvertibleItemAttributeValue#as(TypeToken)} method. If
 * the type has two type parameters (e.g. {@code Map<String, String>}) the key type is assumed to be the first parameter, and the
 * value type is assumed to be the second parameter. If the type has additional type parameters, the
 * {@link GenericConvertibleMap} interface can be implemented to specify which type parameters are the key and values. If the
 * type definition does not include the key and value tyeps, the {@link KeyValueTypeAwareMap} interface can be implemented to
 * specify which key and value type should be used.
 *
 * <p>
 * <b>Map Type:</b> When converting DynamoDB maps to Java maps, this converter must automatically create
 * an instance of the map. This is done via the following process:
 * <ol>
 *     <li>Check for a constructor that was specified when the converter was created via
 *     {@link Builder#putMapConstructor(Class, Supplier)}.</li>
 *     <li>Check for a built-in constructor. A constructor is currently built-in for the following types: {@link Map},
 *     {@link ConcurrentMap}, {@link SortedMap}, {@link NavigableMap}, and {@link ConcurrentNavigableMap}.</li>
 *     <li>Check for a zero-argument method on the requested map type.</li>
 * </ol>
 *
 * <p>
 * This can be created via {@link #create()}.
 *
 * @see CollectionSubtypeAttributeConverter
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class MapSubtypeAttributeConverter implements SubtypeAttributeConverter<Map<?, ?>> {
    private static final DefaultStringConverter DEFAULT_STRING_CONVERTER = DefaultStringConverter.create();
    private static final TypeToken<Map<?, ?>> TYPE = new TypeToken<Map<?, ?>>(){};
    private static final Map<Class<?>, Supplier<Map<Object, Object>>> TYPE_CONSTRUCTORS;
    private final Map<Class<?>, Supplier<Map<Object, Object>>> additionalMapConstructors;

    static {
        Map<Class<?>, Supplier<Map<Object, Object>>> constructors = new HashMap<>();
        constructors.put(Map.class, LinkedHashMap::new);
        constructors.put(ConcurrentMap.class, ConcurrentHashMap::new);
        constructors.put(SortedMap.class, TreeMap::new);
        constructors.put(NavigableMap.class, TreeMap::new);
        constructors.put(ConcurrentNavigableMap.class, ConcurrentSkipListMap::new);
        TYPE_CONSTRUCTORS = Collections.unmodifiableMap(constructors);
    }

    private MapSubtypeAttributeConverter(MapSubtypeAttributeConverter.Builder builder) {
        this.additionalMapConstructors = Collections.unmodifiableMap(new HashMap<>(builder.additionalMapConstructors));
    }

    /**
     * Create a {@link MapSubtypeAttributeConverter} with default options set. {@link #builder()} can be used to configure
     * the way in which collection types are created.
     */
    public static MapSubtypeAttributeConverter create() {
        return builder().build();
    }

    /**
     * Create a {@link Builder} for defining a {@link MapSubtypeAttributeConverter} with custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TypeToken<Map<?, ?>> type() {
        return TYPE;
    }

    @Override
    public ItemAttributeValue toAttributeValue(Map<?, ?> input, ConversionContext context) {
        Map<String, ItemAttributeValue> result = new LinkedHashMap<>();
        input.forEach((key, value) -> result.put(DEFAULT_STRING_CONVERTER.toString(key),
                                                 context.attributeConverter().toAttributeValue(value, context)));
        return ItemAttributeValue.fromMap(result);
    }

    @Override
    public <T extends Map<?, ?>> T fromAttributeValue(ItemAttributeValue input,
                                                      TypeToken<T> desiredType,
                                                      ConversionContext context) {
        Class<?> mapType = desiredType.rawClass();

        return input.convert(new TypeConvertingVisitor<T>(Map.class, MapSubtypeAttributeConverter.class) {
            @Override
            public T convertMap(Map<String, ItemAttributeValue> value) {
                Map<Object, Object> result = createMap(mapType);
                Pair<TypeToken<?>, TypeToken<?>> typeParameters = getMapTypeParameters(result, desiredType);
                value.forEach((k, v) -> {
                    result.put(DEFAULT_STRING_CONVERTER.fromString(typeParameters.left(), k),
                               context.attributeConverter().fromAttributeValue(v, typeParameters.right(), context));
                });
                // This is a safe cast - We know the values we added to the map match the type that the customer requested.
                return (T) result;
            }
        });
    }

    private Pair<TypeToken<?>, TypeToken<?>> getMapTypeParameters(Map<Object, Object> result, TypeToken<?> desiredType) {
        if (result instanceof KeyValueTypeAwareMap) {
            KeyValueTypeAwareMap convertibleMap = (KeyValueTypeAwareMap) result;

            TypeToken<?> keyConversionType =
                    Validate.notNull(convertibleMap.keyConversionType(),
                                     "Invalid key conversion type (null) for map (%s).", desiredType);

            TypeToken<?> valueConversionType =
                    Validate.notNull(convertibleMap.valueConversionType(),
                                     "Invalid value conversion type (null) for map (%s).", desiredType);

            return Pair.of(keyConversionType, valueConversionType);
        }

        List<TypeToken<?>> mapTypeParameters = desiredType.rawClassParameters();
        int keyIndex;
        int valueIndex;
        if (result instanceof GenericConvertibleMap) {
            GenericConvertibleMap convertibleMap = (GenericConvertibleMap) result;
            keyIndex = convertibleMap.keyConversionTypeIndex();
            valueIndex = convertibleMap.valueConversionTypeIndex();

            Validate.isTrue(0 <= keyIndex && keyIndex < mapTypeParameters.size(),
                            "Invalid key conversion type index (%s) for map (%s). Must be between 0 and %s.",
                            keyIndex, desiredType, mapTypeParameters.size());
            Validate.isTrue(0 <= valueIndex && valueIndex < mapTypeParameters.size(),
                            "Invalid value conversion type index (%s) for map (%s). Must be between 0 and %s.",
                            valueIndex, desiredType, mapTypeParameters.size());
        } else {
            keyIndex = 0;
            valueIndex = 1;
            Validate.isTrue(mapTypeParameters.size() == 2,
                            "Cannot determine key and value types for the requested map type: %s. If you're specifying a type " +
                            "token, make sure you are specifying an element type (e.g. TypeToken.mapOf(String.class, " +
                            "String.class) instead of TypeToken.of(Map.class). If you're using a custom map type " +
                            "without the key and value types as parameters, it will need to implement " +
                            "KeyValueTypeAwareMap to specify the element type. If you're using a custom map " +
                            "type with the key and value types as parameters, but there is more than two parameters, it will " +
                            "need to implement GenericConvertibleMap to specify which type " +
                            "parameters are encoding the key and element types.", desiredType);
        }

        return Pair.of(mapTypeParameters.get(keyIndex), mapTypeParameters.get(valueIndex));
    }

    private Map<Object, Object> createMap(Class<?> mapType) {
        Supplier<Map<Object, Object>> constructor = additionalMapConstructors.get(mapType);
        if (constructor != null) {
            return constructor.get();
        }

        constructor = TYPE_CONSTRUCTORS.get(mapType);
        if (constructor != null) {
            return constructor.get();
        }

        try {
            return (Map<Object, Object>) mapType.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to instantiate the requested type " + mapType.getTypeName() + ".", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Requested type " + mapType.getTypeName() + " is not supported, because it " +
                                            "does not have a public, callable zero-arg constructor. Either add such a " +
                                            "constructor to the object or configure the MapSubtypeAttributeConverter " +
                                            "with a construction method to use instead.", e);
        }
    }

    /**
     * A builder for configuring and creating {@link MapSubtypeAttributeConverter}s.
     */
    public static final class Builder {
        Map<Class<?>, Supplier<Map<Object, Object>>> additionalMapConstructors = new HashMap<>();

        private Builder() {}

        /**
         * Add an additional constructor to be used for creating an instance of the specified type. When the specified type is
         * requested in the {@link #fromAttributeValue} methods, this constructor will be used instead of the built-in behavior.
         */
        public <T extends Map<?, ?>> Builder putMapConstructor(Class<T> type, Supplier<? extends T> constructor) {
            this.additionalMapConstructors.put(type, (Supplier<Map<Object, Object>>) constructor);
            return this;
        }

        public MapSubtypeAttributeConverter build() {
            return new MapSubtypeAttributeConverter(this);
        }
    }
}
