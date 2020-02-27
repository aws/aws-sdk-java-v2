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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.StringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between a specific {@link Map} type and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a map from string to attribute value. This uses a configured {@link StringAttributeConverter}
 * to convert the map keys to a string, and a configured {@link AttributeConverter} to convert the map values to an attribute
 * value.
 *
 * <p>
 * This supports reading maps from DynamoDB. This uses a configured {@link StringAttributeConverter} to convert the map keys, and
 * a configured {@link AttributeConverter} to convert the map values.
 *
 * <p>
 * A builder is exposed to allow defining how the map, key and value types are created and converted:
 * <code>
 * AttributeConverter<Map<MonthDay, String>> mapConverter =
 * MapAttributeConverter.builder(TypeToken.mapOf(Integer.class, String.class))
 * .mapConstructor(HashMap::new)
 * .keyConverter(MonthDayStringConverter.create())
 * .valueConverter(StringAttributeConverter.create())
 * .build();
 * </code>
 *
 * <p>
 * For frequently-used types, static methods are exposed to reduce the amount of boilerplate involved in creation:
 * <code>
 * AttributeConverter<Map<MonthDay, String>> mapConverter =
 * MapAttributeConverter.mapConverter(MonthDayStringConverter.create(),
 * StringAttributeConverter.create());
 * <p>
 * AttributeConverter<SortedMap<MonthDay, String>> sortedMapConverter =
 * MapAttributeConverter.sortedMapConverter(MonthDayStringConverter.create(),
 * StringAttributeConverter.create());
 * </code>
 *
 * @see MapAttributeConverter
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public class MapAttributeConverter<T extends Map<?, ?>> implements AttributeConverter<T> {
    private final Delegate<T, ?, ?> delegate;

    private static final Map<Class<?>, Supplier<?>> TYPE_CONSTRUCTORS;

    static {
        Map<Class<?>, Supplier<?>> constructors = new HashMap<>();
        constructors.put(Map.class, LinkedHashMap::new);
        constructors.put(ConcurrentMap.class, ConcurrentHashMap::new);
        constructors.put(SortedMap.class, TreeMap::new);
        constructors.put(NavigableMap.class, TreeMap::new);
        constructors.put(ConcurrentNavigableMap.class, ConcurrentSkipListMap::new);
        TYPE_CONSTRUCTORS = Collections.unmodifiableMap(constructors);
    }
    private MapAttributeConverter(Delegate<T, ?, ?> delegate) {
        this.delegate = delegate;
    }

    public static <T extends Map<K, V>, K, V> MapAttributeConverter<T> mapConverter(TypeToken<T> mapType,
                                                                                    StringConverter<K> keyConverter,
                                                                                    AttributeConverter<V> valueConverter) {
        return builder(mapType)
            .keyConverter(keyConverter)
            .valueConverter(valueConverter)
            .mapConstructor(TYPE_CONSTRUCTORS.get(mapType.rawClass()))
            .build();
    }

    public static <T extends Map<K, V>, K, V> Builder<T, K, V> builder(TypeToken<T> mapType) {
        return new Builder<>(mapType);
    }

    @Override
    public TypeToken<T> type() {
        return delegate.type();
    }

    @Override
    public AttributeValue transformFrom(T input) {
        return delegate.toAttributeValue(input).toGeneratedAttributeValue();
    }

    @Override
    public T transformTo(AttributeValue input) {
        return delegate.fromAttributeValue(input);
    }

    private static final class Delegate<T extends Map<K, V>, K, V> {
        private final TypeToken<T> type;
        private final Supplier<? extends T> mapConstructor;
        private final StringConverter<K> keyConverter;
        private final AttributeConverter<V> valueConverter;

        private Delegate(Builder<T, K, V> builder) {
            this.type = builder.mapType;
            this.mapConstructor = builder.mapConstructor;
            this.keyConverter = builder.keyConverter;
            this.valueConverter = builder.valueConverter;
        }

        public TypeToken<T> type() {
            return type;
        }

        public ItemAttributeValue toAttributeValue(T input) {
            Map<String, ItemAttributeValue> result = new LinkedHashMap<>();
            input.forEach((k, v) -> result.put(keyConverter.toString(k),
                                               ItemAttributeValue.fromGeneratedAttributeValue(valueConverter.transformFrom(v))));
            return ItemAttributeValue.fromMap(result);
        }

        public T fromAttributeValue(AttributeValue input) {
            return ItemAttributeValue.fromGeneratedAttributeValue(input).convert(new TypeConvertingVisitor<T>(Map.class, MapAttributeConverter.class) {
                @Override
                public T convertMap(Map<String, ItemAttributeValue> value) {
                    T result = mapConstructor.get();
                    value.forEach((k, v) -> result.put(keyConverter.fromString(k),
                                                       valueConverter.transformTo(v.toGeneratedAttributeValue())));
                    return result;
                }
            });
        }
    }

    public static final class Builder<T extends Map<K, V>, K, V> {
        private final TypeToken<T> mapType;

        private StringConverter<K> keyConverter;
        private AttributeConverter<V> valueConverter;
        private Supplier<? extends T> mapConstructor;

        private Builder(TypeToken<T> mapType) {
            this.mapType = mapType;
        }

        public Builder<T, K, V> mapConstructor(Supplier<?> mapConstructor) {
            this.mapConstructor = (Supplier<? extends T>) mapConstructor;
            return this;
        }

        public Builder<T, K, V> keyConverter(StringConverter<K> keyConverter) {
            this.keyConverter = keyConverter;
            return this;
        }

        public Builder<T, K, V> valueConverter(AttributeConverter<V> valueConverter) {
            this.valueConverter = valueConverter;
            return this;
        }

        public MapAttributeConverter<T> build() {
            return new MapAttributeConverter<>(new Delegate<>(this));
        }
    }
}
