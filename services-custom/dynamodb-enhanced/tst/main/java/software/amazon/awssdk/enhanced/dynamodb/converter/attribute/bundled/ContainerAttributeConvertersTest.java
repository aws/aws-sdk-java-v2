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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromListOfAttributeValues;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromMap;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromString;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ElementTypeAwareCollection;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.GenericConvertibleCollection;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.GenericConvertibleMap;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.KeyValueTypeAwareMap;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.StringStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.utils.ImmutableMap;

public class ContainerAttributeConvertersTest {
    @Test
    public void collectionAttributeConverterBehavesForCollections() {
        verifyCollection(CollectionAttributeConverter.collectionConverter(StringAttributeConverter.create()), ArrayList::new);
        verifyCollection(CollectionAttributeConverter.listConverter(StringAttributeConverter.create()), ArrayList::new);
        verifyCollection(CollectionAttributeConverter.setConverter(StringAttributeConverter.create()), HashSet::new);
        verifyCollection(CollectionAttributeConverter.sortedSetConverter(StringAttributeConverter.create()), TreeSet::new);
        verifyCollection(CollectionAttributeConverter.queueConverter(StringAttributeConverter.create()), ArrayDeque::new);
        verifyCollection(CollectionAttributeConverter.dequeConverter(StringAttributeConverter.create()), ArrayDeque::new);
        verifyCollection(CollectionAttributeConverter.navigableSetConverter(StringAttributeConverter.create()), TreeSet::new);
    }

    private <T extends Collection<String>> void verifyCollection(CollectionAttributeConverter<T> converter, Supplier<T> constructor) {
        T emptyCollection = constructor.get();
        T collectionWithStuff = constructor.get();
        collectionWithStuff.add("bar");
        collectionWithStuff.add("foo");

        assertThat(toAttributeValue(converter, emptyCollection).asListOfAttributeValues()).isEmpty();
        assertThat(toAttributeValue(converter, collectionWithStuff).asListOfAttributeValues())
                .containsExactly(fromString("bar"), fromString("foo"));
        assertThat(fromAttributeValue(converter, fromListOfAttributeValues())).isEmpty();
        assertThat(fromAttributeValue(converter, fromListOfAttributeValues(fromString("bar"), fromString("foo"))))
                .containsExactly("bar", "foo");
    }

    @Test
    public void mapAttributeConverterBehavesForCollections() {
        verifyMap(MapAttributeConverter.mapConverter(StringStringConverter.create(), StringAttributeConverter.create()),
                  HashMap::new);
        verifyMap(MapAttributeConverter.concurrentMapConverter(StringStringConverter.create(), StringAttributeConverter.create()),
                  ConcurrentHashMap::new);
        verifyMap(MapAttributeConverter.sortedMapConverter(StringStringConverter.create(), StringAttributeConverter.create()),
                  TreeMap::new);
        verifyMap(MapAttributeConverter.navigableMapConverter(StringStringConverter.create(), StringAttributeConverter.create()),
                  TreeMap::new);
    }

    private <T extends Map<String, String>> void verifyMap(MapAttributeConverter<T> converter, Supplier<T> constructor) {
        T emptyMap = constructor.get();
        T mapWithStuff = constructor.get();
        mapWithStuff.put("a", "b");
        mapWithStuff.put("c", "d");

        assertThat(toAttributeValue(converter, emptyMap).asMap()).isEmpty();
        assertThat(toAttributeValue(converter, mapWithStuff).asMap())
                .hasSize(2)
                .containsEntry("a", fromString("b"))
                .containsEntry("c", fromString("d"));

        assertThat(fromAttributeValue(converter, fromMap(emptyMap()))).isEmpty();
        assertThat(fromAttributeValue(converter, fromMap(ImmutableMap.of("a", fromString("b"), "c", fromString("d")))))
                .hasSize(2)
                .containsEntry("a", "b")
                .containsEntry("c", "d");
    }

    @Test
    public void collectionSubtypeAttributeConverterBehavesForCollections() {
        CollectionSubtypeAttributeConverter converter = CollectionSubtypeAttributeConverter.create();

        assertThat(toAttributeValue(converter, emptyList())).isEqualTo(ItemAttributeValue.fromListOfAttributeValues());
        assertThat(toAttributeValue(converter, asList(1, 2)))
                .isEqualTo(ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromNumber("1"),
                                                                        ItemAttributeValue.fromNumber("2")));
        assertThat(toAttributeValue(converter, asList("foo", 1)))
                .isEqualTo(ItemAttributeValue.fromListOfAttributeValues(fromString("foo"),
                                                                        ItemAttributeValue.fromNumber("1")));
        assertThat(toAttributeValue(converter, new CustomLongCollection())).isEqualTo(ItemAttributeValue.fromListOfAttributeValues());

        // No elements
        assertThat(fromAttributeValue(converter,
                                      TypeToken.collectionOf(String.class),
                                      ItemAttributeValue.fromListOfAttributeValues()))
                .isEmpty();

        // Different element types
        assertThat(fromAttributeValue(converter,
                                      TypeToken.collectionOf(String.class),
                                      ItemAttributeValue.fromListOfAttributeValues(fromString("foo"))))
                .containsExactly("foo");
        assertThat(fromAttributeValue(converter,
                                      TypeToken.collectionOf(Long.class),
                                      ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromNumber("1"))))
                .containsExactly(1L);
    }

    @Test
    public void collectionSubtypeAttributeConverterBehavesForCollectionSubtypes() {
        CollectionSubtypeAttributeConverter converter = CollectionSubtypeAttributeConverter.create();

        // Built-in collection types
        ItemAttributeValue stringAttribute = ItemAttributeValue.fromListOfAttributeValues(fromString("foo"));
        assertThat(fromAttributeValue(converter, TypeToken.setOf(String.class), stringAttribute)).containsExactly("foo");
        assertThat(fromAttributeValue(converter, TypeToken.listOf(String.class), stringAttribute)).containsExactly("foo");
        assertThat(fromAttributeValue(converter, TypeToken.sortedSetOf(String.class), stringAttribute)).containsExactly("foo");
        assertThat(fromAttributeValue(converter, TypeToken.dequeOf(String.class), stringAttribute)).containsExactly("foo");
        assertThat(fromAttributeValue(converter, TypeToken.queueOf(String.class), stringAttribute)).containsExactly("foo");

        // Custom collection types
        assertThat(fromAttributeValue(converter,
                                      TypeToken.of(CustomLongCollection.class),
                                      ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromNumber("1"))))
                .containsExactly(1L);
        assertThat(fromAttributeValue(converter,
                                      new TypeToken<CustomCollection<Long>>(){},
                                      ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromNumber("1"))))
                .containsExactly(1L);
        assertThat(fromAttributeValue(converter,
                                      new TypeToken<CustomGenericCollection<Void, Long>>(){},
                                      ItemAttributeValue.fromListOfAttributeValues(ItemAttributeValue.fromNumber("1"))))
                .containsExactly(1L);
    }

    @Test
    public void customCollectionConstructorWorks() {
        ArrayList<Object> list = new ArrayList<>();
        CollectionSubtypeAttributeConverter converter =
                CollectionSubtypeAttributeConverter.builder()
                                                   .putCollectionConstructor(List.class, () -> list)
                                                   .build();

        ItemAttributeValue stringAttribute = ItemAttributeValue.fromListOfAttributeValues(fromString("foo"));
        assertThat(fromAttributeValue(converter, TypeToken.listOf(String.class), stringAttribute))
                .isSameAs(list).containsExactly("foo");
    }

    @Test
    public void mapSubtypeAttributeConverterBehavesForMaps() {
        MapSubtypeAttributeConverter converter = MapSubtypeAttributeConverter.create();

        assertThat(toAttributeValue(converter, emptyMap()))
                .isEqualTo(ItemAttributeValue.fromMap(emptyMap()));
        assertThat(toAttributeValue(converter, ImmutableMap.of("a", 2)))
                .isEqualTo(ItemAttributeValue.fromMap(ImmutableMap.of("a", ItemAttributeValue.fromNumber("2"))));
        assertThat(toAttributeValue(converter, ImmutableMap.of((Object) "a", 2, 3, "b")))
                .isEqualTo(ItemAttributeValue.fromMap(ImmutableMap.of("a", ItemAttributeValue.fromNumber("2"),
                                                                      "3", fromString("b"))));

        assertThat(toAttributeValue(converter, new CustomLongStringMap()))
                .isEqualTo(ItemAttributeValue.fromMap(emptyMap()));

        // No elements
        assertThat(fromAttributeValue(converter,
                                      TypeToken.mapOf(Long.class, String.class),
                                      ItemAttributeValue.fromMap(emptyMap())))
                .isEmpty();

        // Different element types
        assertThat(fromAttributeValue(converter,
                                      TypeToken.mapOf(Long.class, String.class),
                                      ItemAttributeValue.fromMap(ImmutableMap.of("2", fromString("a")))))
                .hasSize(1)
                .containsEntry(2L, "a");
        assertThat(fromAttributeValue(converter,
                                      TypeToken.mapOf(String.class, Long.class),
                                      ItemAttributeValue.fromMap(ImmutableMap.of("a", ItemAttributeValue.fromNumber("1")))))
                .hasSize(1)
                .containsEntry("a", 1L);
    }

    @Test
    public void mapSubtypeAttributeConverterBehavesForMapSubtypes() {
        MapSubtypeAttributeConverter converter = MapSubtypeAttributeConverter.create();

        // Built-in map types
        ItemAttributeValue mapAttributeValue = ItemAttributeValue.fromMap(ImmutableMap.of("2", fromString("a")));
        assertThat(fromAttributeValue(converter, TypeToken.mapOf(Long.class, String.class), mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");
        assertThat(fromAttributeValue(converter, TypeToken.concurrentMapOf(Long.class, String.class), mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");
        assertThat(fromAttributeValue(converter, TypeToken.sortedMapOf(Long.class, String.class), mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");
        assertThat(fromAttributeValue(converter, TypeToken.navigableMapOf(Long.class, String.class), mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");

        // Custom map types
        assertThat(fromAttributeValue(converter, TypeToken.of(CustomLongStringMap.class), mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");
        assertThat(fromAttributeValue(converter, new TypeToken<CustomMap<Long, String>>(){}, mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");
        assertThat(fromAttributeValue(converter, new TypeToken<CustomGenericMap<Void, Long, String>>(){}, mapAttributeValue))
                .hasSize(1).containsEntry(2L, "a");
    }

    @Test
    public void customMapConstructorWorks() {
        Map<Object, Object> map = new HashMap<>();
        MapSubtypeAttributeConverter converter =
                MapSubtypeAttributeConverter.builder()
                                            .putMapConstructor(Map.class, () -> map)
                                            .build();

        ItemAttributeValue mapAttribute = ItemAttributeValue.fromMap(emptyMap());
        assertThat(fromAttributeValue(converter, TypeToken.mapOf(String.class, String.class), mapAttribute))
                .isSameAs(map);
    }

    public static final class CustomLongCollection extends ArrayList<Long> implements ElementTypeAwareCollection {
        @Override
        public TypeToken<Long> elementConversionType() {
            return TypeToken.of(Long.class);
        }
    }

    public static final class CustomCollection<T> extends ArrayList<T> {}

    public static final class CustomGenericCollection<U, T> extends ArrayList<T> implements GenericConvertibleCollection {
        @Override
        public int elementConversionTypeIndex() {
            return 1;
        }
    }

    public static final class CustomLongStringMap extends HashMap<Long, String> implements KeyValueTypeAwareMap {
        @Override
        public TypeToken<?> keyConversionType() {
            return TypeToken.of(Long.class);
        }

        @Override
        public TypeToken<?> valueConversionType() {
            return TypeToken.of(String.class);
        }
    }

    public static final class CustomMap<K, V> extends HashMap<K, V> {}

    public static final class CustomGenericMap<T, K, V> extends HashMap<Long, String> implements GenericConvertibleMap {
        @Override
        public int keyConversionTypeIndex() {
            return 1;
        }

        @Override
        public int valueConversionTypeIndex() {
            return 2;
        }
    }
}