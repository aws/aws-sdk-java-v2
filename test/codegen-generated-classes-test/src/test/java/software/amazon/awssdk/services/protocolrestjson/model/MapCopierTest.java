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

package software.amazon.awssdk.services.protocolrestjson.model;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for generated map member copiers.
 */
public class MapCopierTest {
    @Test
    public void nullParamsAreCopiedAsAutoConstructedMap() {
        assertThat(MapOfStringToStringCopier.copy(null)).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void preservesAutoConstructedMapInput() {
        assertThat(MapOfStringToStringCopier.copy(DefaultSdkAutoConstructMap.getInstance())).isInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void explicitlyEmptyMapsAreNotCopiedAsAutoConstructed() {
        assertThat(MapOfStringToStringCopier.copy(new HashMap<>())).isNotInstanceOf(SdkAutoConstructMap.class);
    }

    @Test
    public void nullValuesInMapsAreCopied() {
        Map<String, String> map = new HashMap<>();
        map.put("test", null);
        assertThat(MapOfStringToStringCopier.copy(map)).isEqualTo(map);
    }

    @Test
    public void modificationsInOriginalMapDoNotReflectInCopiedMap() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> copiedMap = MapOfStringToStringCopier.copy(map);
        map.put("test", null);
        assertThat(copiedMap).isEmpty();
    }

    @Test
    public void copiedMapIsImmutable() {
        assertThatThrownBy(() -> MapOfStringToStringCopier.copy(new HashMap<>()).put("test", "a")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void unknownEnumKeyNotAddedToCopiedMap() {
        Map<String, String> mapOfEnumToEnum = new HashMap<>();
        mapOfEnumToEnum.put("foo", "bar");
        Map<EnumType, EnumType> copy = MapOfEnumToEnumCopier.copyStringToEnum(mapOfEnumToEnum);
        Assertions.assertThat(copy).isEmpty();
    }

    @Test
    public void knownEnumKeyAddedToCopiedMap() {
        Map<String, String> mapOfEnumToEnum = new HashMap<>();
        mapOfEnumToEnum.put(EnumType.ENUM_VALUE1.toString(), "bar");
        Map<EnumType, EnumType> copy = MapOfEnumToEnumCopier.copyStringToEnum(mapOfEnumToEnum);
        Assertions.assertThat(copy).hasSize(1);
    }
}
