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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue.fromListOfAttributeValues;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue.fromMap;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue.fromString;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.transformTo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.junit.Test;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.CollectionAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.MapAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.StringAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.string.bundled.StringStringConverter;
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

        assertThat(transformFrom(converter, emptyCollection).l()).isEmpty();
        assertThat(transformFrom(converter, collectionWithStuff).l())
                .containsExactly(fromString("bar").toGeneratedAttributeValue(), fromString("foo").toGeneratedAttributeValue());
        assertThat(transformTo(converter, fromListOfAttributeValues().toGeneratedAttributeValue())).isEmpty();
        assertThat(transformTo(converter, fromListOfAttributeValues(fromString("bar"), fromString("foo")).toGeneratedAttributeValue()))
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

        assertThat(transformFrom(converter, emptyMap).m()).isEmpty();
        assertThat(transformFrom(converter, mapWithStuff).m())
                .hasSize(2)
                .containsEntry("a", fromString("b").toGeneratedAttributeValue())
                .containsEntry("c", fromString("d").toGeneratedAttributeValue());

        assertThat(transformTo(converter, fromMap(emptyMap()).toGeneratedAttributeValue())).isEmpty();
        assertThat(transformTo(converter, fromMap(ImmutableMap.of("a", fromString("b"), "c", fromString("d"))).toGeneratedAttributeValue()))
                .hasSize(2)
                .containsEntry("a", "b")
                .containsEntry("c", "d");
    }
}