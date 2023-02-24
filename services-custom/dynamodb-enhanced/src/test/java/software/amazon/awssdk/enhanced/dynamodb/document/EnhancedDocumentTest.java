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
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
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
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;

public class EnhancedDocumentTest {

    public static final String EMPTY_OR_NULL_ERROR = "attributeName cannot empty or null";
    static String INNER_JSON = "{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}";

    private static Stream<Arguments> documentsCreatedFromStaticMethods() {
        Map<String, Object> map = getStringObjectMap();
        return Stream.of(
            Arguments.of(EnhancedDocument.fromJson(ARRAY_AND_MAP_IN_JSON,Arrays.asList(defaultProvider()))),
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

        assertThat(enhancedDocument.getNumber("numberKey").intValue()).isEqualTo(1);

        assertThat(enhancedDocument.getList("numberList")
                                   .stream()
                                   .map(o -> Integer.parseInt(o.toString()))
                                   .collect(Collectors.toList()))
            .isEqualTo(Arrays.stream(NUMBER_STRING_ARRAY)
                             .map(s -> Integer.parseInt(s))
                             .collect(Collectors.toList()));

        assertThat(enhancedDocument.getList("numberList", EnhancedType.of(String.class)))
            .isEqualTo(Arrays.asList(NUMBER_STRING_ARRAY));


        assertThat(enhancedDocument.getEnhancedDocument("mapKey").toJson())
            .isEqualTo(EnhancedDocument.fromJson(INNER_JSON, Arrays.asList(defaultProvider())).toJson());

        // This is same as V1, where the Json List of String is identified as List of Strings rather than set of string
        assertThat(enhancedDocument.getEnhancedDocument("mapKey").getList("1")).isEqualTo(Arrays.asList(STRINGS_ARRAY));
        assertThat(enhancedDocument.getEnhancedDocument("mapKey").getStringSet("1")).isNull();
    }

    @Test
    void nullArgsInStaticConstructor() {

        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> EnhancedDocument.fromMap(null))
            .withMessage("attributeMap must not be null.");

        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> EnhancedDocument.fromJson(null, Arrays.asList(defaultProvider())))
            .withMessage("json must not be null.");

        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> EnhancedDocument.fromAttributeValueMap(null))
            .withMessage("attributeValueMap must not be null.");
    }

    @Test
    void accessingStringSetFromBuilderMethods() {

        Set<String> stringSet = Stream.of(STRINGS_ARRAY).collect(Collectors.toSet());
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
            .addAttributeConverterProvider(defaultProvider())
                                                            .putStringSet("stringSet", stringSet)
                                                            .build();

        assertThat(enhancedDocument.getStringSet("stringSet")).isEqualTo(stringSet);
        assertThat(enhancedDocument.getList("stringSet")).isNull();
    }

    @Test
    void toBuilderOverwritingOldJson() {
        EnhancedDocument document = EnhancedDocument.fromJson(ARRAY_AND_MAP_IN_JSON, Arrays.asList(defaultProvider()));
        assertThat(document.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);
        EnhancedDocument fromBuilder = document.toBuilder().json(INNER_JSON).build();
        assertThat(fromBuilder.toJson()).isEqualTo(INNER_JSON);
    }

    @Test
    void builder_with_NullKeys() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putString(null, "Sample"))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putNull(null))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putNumber(null, 3))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putObjectList(null, Arrays.asList()))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putBytes(null, SdkBytes.fromUtf8String("a")))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putMap(null, new HashMap<>()))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putStringSet(null, Stream.of("a").collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putNumberSet(null, Stream.of(1).collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putStringSet(null, Stream.of("a").collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> EnhancedDocument.builder().putBytesSet(null, Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
    }

    @Test
    void errorWhen_NoAttributeConverter_IsProviderIsDefined() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one").aBoolean(false).build();

        EnhancedDocument enhancedDocument =
            EnhancedDocument.builder().addAttributeConverterProvider(defaultProvider()).putObject("customObject", customObject).build();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> enhancedDocument.get("customObject",
                                                                                                     EnhancedType.of(CustomClassForDocumentAPI.class)))
            .withMessage("AttributeConverter not found for type EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI). "
                         + "Please add an AttributeConverterProvider for this type. If it is a default type, add the DefaultAttributeConverterProvider to the Document builder.");
    }

    @Test
    void attributeConverter_OrderInBuilder_Doesnot_Matter_forSimpleAdd() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one")
                                                                          .longNumber(26L)
                                                                          .aBoolean(false).build();
        EnhancedDocument afterCustomClass = EnhancedDocument.builder()
                                                            .attributeConverterProviders(
                                                                defaultProvider(),
                                                                CustomAttributeForDocumentConverterProvider.create())
                                                            .putString("direct_attr", "sample_value")
                                                            .putObject("customObject", customObject).build();

        EnhancedDocument beforeCustomClass = EnhancedDocument.builder()
                                                             .putString("direct_attr", "sample_value")
                                                             .putObject("customObject", customObject)
                                                             .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(), defaultProvider())
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
                                                            .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(), defaultProvider())
                                                            .putString("direct_attr", "sample_value")
                                                            .putObjectList("customObject", Arrays.asList(customObjectOne,
                                                                                                         customObjectTwo)).build();
        EnhancedDocument beforeCustomClass = EnhancedDocument.builder()
                                                             .putString("direct_attr", "sample_value")
                                                             .putObjectList("customObject", Arrays.asList(customObjectOne,
                                                                                                          customObjectTwo))
                                                             .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(), defaultProvider())
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
                                                            .putString("direct_attr", "sample_value")
                                                            .putMap("customObject", map)
                                                            .build();
        EnhancedDocument beforeCustomClass = EnhancedDocument.builder()
                                                             .putString("direct_attr", "sample_value")
                                                             .putMap("customObject", map)
                                                             .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                             .build();

    }
}
