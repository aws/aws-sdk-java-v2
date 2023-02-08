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

package software.amazon.awssdk.enhanced.dynamodb.document;

import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.ARRAY_AND_MAP_IN_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.NUMBER_STRING_ARRAY;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.STRINGS_ARRAY;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class EnhancedDocumentTest {

    static String INNER_JSON = "{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}";

    private static Stream<Arguments> documentsCreatedFromStaticMethods() {
        Map<String, Object> map = getStringObjectMap();
        return Stream.of(
            Arguments.of(EnhancedDocument.fromJson(ARRAY_AND_MAP_IN_JSON)),
            Arguments.of(EnhancedDocument.fromMap(map)));
    }

    private static Map<String, Object> getStringObjectMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("numberKey", 1);
        map.put("numberList", Arrays.asList(1, 2, 3));
        Map<String, Object> innerMap = new LinkedHashMap<>();
        map.put("mapKey", innerMap);

        innerMap.put("1", Arrays.asList(STRINGS_ARRAY));
        innerMap.put("2", 1);
        return map;
    }

    @ParameterizedTest
    @MethodSource("documentsCreatedFromStaticMethods")
    void createFromJson(EnhancedDocument enhancedDocument) {
        assertThat(enhancedDocument.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);

        enhancedDocument.getJson("mapKey").equals(INNER_JSON);

        assertThat(enhancedDocument.getSdkNumber("numberKey").intValue()).isEqualTo(1);

        assertThat(enhancedDocument.getList("numberList")
                                   .stream()
                                   .map( o ->Integer.parseInt(o.toString()) )
                                   .collect(Collectors.toList()))
            .isEqualTo(Arrays.stream(NUMBER_STRING_ARRAY)
                             .map(s -> Integer.parseInt(s))
                             .collect(Collectors.toList()));

        assertThat(enhancedDocument.getList("numberList", EnhancedType.of(String.class)))
            .isEqualTo(Arrays.asList(NUMBER_STRING_ARRAY));


        assertThat(enhancedDocument.getMapAsDocument("mapKey").toJson())
            .isEqualTo(EnhancedDocument.fromJson(INNER_JSON).toJson());

        // This is same as V1, where the Json List of String is identified as List of Strings rather than set of string
        assertThat(enhancedDocument.getMapAsDocument("mapKey").getList("1")).isEqualTo(Arrays.asList(STRINGS_ARRAY));
        assertThat(enhancedDocument.getMapAsDocument("mapKey").getStringSet("1")).isNull();
    }

    @Test
    public void nullArgsInStaticConstructor(){
        assertThat(EnhancedDocument.fromMap(null)).isNull();
        assertThat(EnhancedDocument.fromJson(null)).isNull();
    }

    @Test
    void accessingStringSetFromBuilderMethods(){

        Set<String> stringSet = Stream.of(STRINGS_ARRAY).collect(Collectors.toSet());
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .addStringSet("stringSet", stringSet)
                                                            .build();

        assertThat(enhancedDocument.getStringSet("stringSet")).isEqualTo(stringSet);
        assertThat(enhancedDocument.getList("stringSet")).isNull();
    }

    @Test
    void toBuilderOverwritingOldJson(){
        EnhancedDocument document = EnhancedDocument.fromJson(ARRAY_AND_MAP_IN_JSON);
        assertThat(document.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);
        EnhancedDocument fromBuilder = document.toBuilder().json(INNER_JSON).build();
        assertThat(fromBuilder.toJson()).isEqualTo(INNER_JSON);
    }
}
