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
        assertThat(convertToEnumWithBuilder("EnumValue1")).isEqualTo(EnumType.ENUM_VALUE1);
        assertThat(convertToEnumWithBuilder((String) null)).isEqualTo(null);
        assertThat(convertToEnumWithBuilder(EnumType.ENUM_VALUE1)).isEqualTo(EnumType.ENUM_VALUE1);
    }

    @Test
    public void unknownEnumFieldsBehaveCorrectly() {
        assertThat(convertToEnumWithBuilder("Foo")).isEqualTo(EnumType.UNKNOWN_TO_SDK_VERSION);
    }

    @Test
    public void knownEnumListFieldsBehaveCorrectly() {
        assertThat(convertToListEnumWithBuilder("EnumValue1", "EnumValue2"))
                .containsExactly(EnumType.ENUM_VALUE1, EnumType.ENUM_VALUE2);
        assertThat(convertToListEnumWithBuilder(new String[] {null })).containsExactly(new EnumType[] {null });
        assertThat(convertToMapEnumWithBuilder()).isEmpty();
    }

    @Test
    public void unknownEnumListFieldsBehaveCorrectly() {
        assertThat(convertToListEnumWithBuilder("Foo", "EnumValue2")).containsExactly(EnumType.UNKNOWN_TO_SDK_VERSION, EnumType.ENUM_VALUE2);
    }

    @Test
    public void knownEnumMapFieldsBehaveCorrectly() {
        assertThat(convertToMapEnumWithBuilder(new SimpleImmutableEntry<>("EnumValue1", "EnumValue2")))
                .containsExactly(new SimpleImmutableEntry<>(EnumType.ENUM_VALUE1, EnumType.ENUM_VALUE2));
    }

    @Test
    public void unknownEnumMapFieldsBehaveCorrectly() {
        assertThat(convertToMapEnumWithBuilder(new SimpleImmutableEntry<>("Foo", "EnumValue2")))
                .isEmpty();
        assertThat(convertToMapEnumWithBuilder(new SimpleImmutableEntry<>("Foo", "Foo")))
                .isEmpty();
        assertThat(convertToMapEnumWithBuilder(new SimpleImmutableEntry<>("Foo", "")))
                .isEmpty();
        assertThat(convertToMapEnumWithBuilder(new SimpleImmutableEntry<>("EnumValue1", "Foo")))
                .containsExactly(new SimpleImmutableEntry<>(EnumType.ENUM_VALUE1, EnumType.UNKNOWN_TO_SDK_VERSION));
        assertThat(convertToMapEnumWithBuilder(new SimpleImmutableEntry<>("EnumValue1", "")))
                .containsExactly(new SimpleImmutableEntry<>(EnumType.ENUM_VALUE1, EnumType.UNKNOWN_TO_SDK_VERSION));
    }

    private EnumType convertToEnumWithBuilder(String value) {
        return AllTypesResponse.builder().enumMember(value).build().enumMember();
    }

    private EnumType convertToEnumWithBuilder(EnumType value) {
        return AllTypesResponse.builder().enumMember(value).build().enumMember();
    }

    private List<EnumType> convertToListEnumWithBuilder(String... values) {
        return AllTypesResponse.builder().listOfEnums(values).build().listOfEnums();
    }

    @SafeVarargs
    private final Map<EnumType, EnumType> convertToMapEnumWithBuilder(Entry<String, String>... values) {
        Map<String, String> enumMap = Stream.of(values).collect(toMap(Entry::getKey, Entry::getValue));
        return AllTypesResponse.builder().mapOfEnumToEnum(enumMap).build().mapOfEnumToEnum();
    }
}
