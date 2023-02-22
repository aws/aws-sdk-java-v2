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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.ARRAY_AND_MAP_IN_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.NUMBER_STRING_ARRAY;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.STRINGS_ARRAY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;

public class EnhancedDocumentTest {

    public static final String EMPTY_OR_NULL_ERROR = "attributeName cannot empty or null";
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
                                   .map(o -> Integer.parseInt(o.toString()))
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
    void nullArgsInStaticConstructor() {
        assertThat(EnhancedDocument.fromMap(null)).isNull();
        assertThat(EnhancedDocument.fromJson(null)).isNull();
    }

    @Test
    void accessingStringSetFromBuilderMethods() {

        Set<String> stringSet = Stream.of(STRINGS_ARRAY).collect(Collectors.toSet());
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .addStringSet("stringSet", stringSet)
                                                            .build();

        assertThat(enhancedDocument.getStringSet("stringSet")).isEqualTo(stringSet);
        assertThat(enhancedDocument.getList("stringSet")).isNull();
    }

    @Test
    void toBuilderOverwritingOldJson() {
        EnhancedDocument document = EnhancedDocument.fromJson(ARRAY_AND_MAP_IN_JSON);
        assertThat(document.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);
        EnhancedDocument fromBuilder = document.toBuilder().json(INNER_JSON).build();
        assertThat(fromBuilder.toJson()).isEqualTo(INNER_JSON);
    }

    @Test
    void builder_with_NullKeys() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addString(null, "Sample"))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addNull(null))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addNumber(null, 3))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addList(null, Arrays.asList()))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addSdkBytes(null, SdkBytes.fromUtf8String("a")))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addMap(null, new HashMap<>()))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addStringSet(null, Stream.of("a").collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addNumberSet(null, Stream.of(1).collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addStringSet(null, Stream.of("a").collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().addSdkBytesSet(null, Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
    }

    @Test
    void errorWhen_NoAttributeConverter_IsProviderIsDefined() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one").aBoolean(false).build();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> EnhancedDocument.builder().add("customObject", customObject).build())
                                                              .withMessage("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI)");

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> EnhancedDocument.builder().addList("customObject", Arrays.asList(customObject)).build())
                                                              .withMessage("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI)");

        Map<String, CustomClassForDocumentAPI> customClassMap = new LinkedHashMap<>();
        customClassMap.put("one", customObject);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> EnhancedDocument.builder().addMap("customObject", customClassMap).build())
                                                              .withMessage("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI)");
    }

    @Test
    void attributeConverter_OrderInBuilder_Doesnot_Matter_forSimpleAdd() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one")
                                                                          .longNumber(26L)
                                                                          .aBoolean(false).build();
        EnhancedDocument afterCustomClass = EnhancedDocument.builder()
                                                            .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                            .addString("direct_attr", "sample_value")
                                                            .add("customObject", customObject).build();

        EnhancedDocument beforeCustomClass = EnhancedDocument.builder()
                                                             .addString("direct_attr", "sample_value")
                                                             .add("customObject", customObject)
                                                             .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                             .build();
        assertThat(afterCustomClass.toJson()).isEqualTo("{\"direct_attr\": \"sample_value\",\"customObject\": {\"foo\": "
                                                        + "\"str_one\"}}");
        assertThat(beforeCustomClass.toJson()).isEqualTo(afterCustomClass.toJson());
    }

    @Test
    void attributeConverter_OrderInBuilder_Doesnot_Matter_ForListAdd() {
        CustomClassForDocumentAPI customObjectOne = CustomClassForDocumentAPI.builder().string("str_one")
                                                                             .longNumber(26L)
                                                                             .aBoolean(false).build();

        CustomClassForDocumentAPI customObjectTwo = CustomClassForDocumentAPI.builder().string("str_two")
                                                                             .longNumber(27L)
                                                                             .aBoolean(true).build();
        EnhancedDocument afterCustomClass = EnhancedDocument.builder()
                                                            .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                            .addString("direct_attr", "sample_value")
                                                            .addList("customObject", Arrays.asList(customObjectOne,
                                                                                                   customObjectTwo)).build();
        EnhancedDocument beforeCustomClass = EnhancedDocument.builder()
                                                             .addString("direct_attr", "sample_value")
                                                             .addList("customObject", Arrays.asList(customObjectOne,
                                                                                                    customObjectTwo))
                                                             .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                             .build();
        assertThat(afterCustomClass.toJson()).isEqualTo("{\"direct_attr\": \"sample_value\",\"customObject\": [{\"foo\": "
                                                        + "\"str_one\"}, {\"foo\": \"str_two\"}]}");
        assertThat(beforeCustomClass.toJson()).isEqualTo(afterCustomClass.toJson());
    }

    @Test
    void attributeConverter_OrderInBuilder_Doesnot_Matter_forMapAdd() {
        CustomClassForDocumentAPI customObjectOne = CustomClassForDocumentAPI.builder().string("str_one")
                                                                             .longNumber(26L)
                                                                             .aBoolean(false).build();
        CustomClassForDocumentAPI customObjectTwo = CustomClassForDocumentAPI.builder().string("str_two")
                                                                             .longNumber(27L)
                                                                             .aBoolean(true)
                                                                             .build();
        Map<String, CustomClassForDocumentAPI> map = new LinkedHashMap<>();
        map.put("one", customObjectOne);
        map.put("two", customObjectTwo);

        EnhancedDocument afterCustomClass = EnhancedDocument.builder()
                                                            .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                            .addString("direct_attr", "sample_value")
                                                            .addMap("customObject", map)
                                                            .build();
        EnhancedDocument beforeCustomClass = EnhancedDocument.builder()
                                                             .addString("direct_attr", "sample_value")
                                                             .addMap("customObject", map)
                                                             .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                             .build();

    }
}
