/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.tests;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.EnumType;

/**
 * Verifies that the behavior when retrieving enums from a model behaves as expected.
 */
public class EnumTest {
    @Test
    public void knownEnumFieldsBehaveCorrectly() {
        assertThat(simulateUnmarshallingEnum("EnumValue1")).isEqualTo(EnumType.ENUM_VALUE1);
        assertThat(simulateUnmarshallingEnum((String) null)).isEqualTo(null);
        assertThat(simulateUnmarshallingEnum(EnumType.ENUM_VALUE1)).isEqualTo(EnumType.ENUM_VALUE1);
    }

    @Test
    public void unknownEnumFieldsBehaveCorrectly() {
        assertThat(simulateUnmarshallingEnum("Foo")).isEqualTo(EnumType.UNKNOWN);
    }

    @Test
    public void knownEnumListFieldsBehaveCorrectly() {
        assertThat(simulateUnmarshallingEnumList("EnumValue1", "EnumValue2"))
                .containsExactly(EnumType.ENUM_VALUE1, EnumType.ENUM_VALUE2);
        assertThat(simulateUnmarshallingEnumList(new String[] { null })).containsExactly(new EnumType[] { null });
        assertThat(simulateUnmarshallingEnumList()).isEmpty();
    }

    @Test
    public void unknownEnumListFieldsBehaveCorrectly() {
        assertThat(simulateUnmarshallingEnumList("Foo", "EnumValue2")).containsExactly(EnumType.UNKNOWN, EnumType.ENUM_VALUE2);
    }

    @Test
    public void knownEnumMapFieldsBehaveCorrectly() {
        assertThat(simulateUnmarshallingEnumMap(new SimpleImmutableEntry<>("EnumValue1", "EnumValue2")))
                .containsExactly(new SimpleImmutableEntry<>(EnumType.ENUM_VALUE1, EnumType.ENUM_VALUE2));
    }

    @Test
    public void unknownEnumMapFieldsBehaveCorrectly() {
        assertThat(simulateUnmarshallingEnumMap(new SimpleImmutableEntry<>("Foo", "EnumValue2")))
                .isEmpty();
        assertThat(simulateUnmarshallingEnumMap(new SimpleImmutableEntry<>("Foo", "Foo")))
                .isEmpty();
        assertThat(simulateUnmarshallingEnumMap(new SimpleImmutableEntry<>("Foo", "")))
                .isEmpty();
        assertThat(simulateUnmarshallingEnumMap(new SimpleImmutableEntry<>("EnumValue1", "Foo")))
                .containsExactly(new SimpleImmutableEntry<>(EnumType.ENUM_VALUE1, EnumType.UNKNOWN));
        assertThat(simulateUnmarshallingEnumMap(new SimpleImmutableEntry<>("EnumValue1", "")))
                .containsExactly(new SimpleImmutableEntry<>(EnumType.ENUM_VALUE1, EnumType.UNKNOWN));
    }

    private EnumType simulateUnmarshallingEnum(String value) {
        return AllTypesResponse.builder().enumMember(value).build().enumMember();
    }

    private EnumType simulateUnmarshallingEnum(EnumType value) {
        return AllTypesResponse.builder().enumMember(value).build().enumMember();
    }

    private List<EnumType> simulateUnmarshallingEnumList(String... values) {
        return AllTypesResponse.builder().listOfEnums(values).build().listOfEnums();
    }

    @SafeVarargs
    private final Map<EnumType, EnumType> simulateUnmarshallingEnumMap(Entry<String, String>... values) {
        Map<String, String> enumMap = Stream.of(values).collect(toMap(Entry::getKey, Entry::getValue));
        return AllTypesResponse.builder().mapOfEnumToEnum(enumMap).build().mapOfEnumToEnum();
    }
}
