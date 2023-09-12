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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.testDataInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;

class EnhancedDocumentTest {

    private static Stream<Arguments> escapeDocumentStrings() {
        char c = 0x0a;
        return Stream.of(
            Arguments.of(String.valueOf(c), "{\"key\":\"\\n\"}")
            , Arguments.of("", "{\"key\":\"\"}")
            , Arguments.of("\"", "{\"key\":\"\\\"\"}")
            , Arguments.of("\\", "{\"key\":\"\\\\\"}")
            , Arguments.of(" ", "{\"key\":\" \"}")
            , Arguments.of("\t", "{\"key\":\"\\t\"}")
            , Arguments.of("\n", "{\"key\":\"\\n\"}")
            , Arguments.of("\r", "{\"key\":\"\\r\"}")
            , Arguments.of("\f", "{\"key\":\"\\f\"}")
        );
    }

    private static Stream<Arguments> unEscapeDocumentStrings() {
        return Stream.of(
            Arguments.of("'", "{\"key\":\"'\"}"),
            Arguments.of("'single quote'", "{\"key\":\"'single quote'\"}")
        );
    }

    @Test
    void enhancedDocumentGetters() {

        EnhancedDocument document = testDataInstance()
            .dataForScenario("complexDocWithSdkBytesAndMapArrays_And_PutOverWritten")
            .getEnhancedDocument();
        // Assert
        assertThat(document.getString("stringKey")).isEqualTo("stringValue");
        assertThat(document.getNumber("numberKey")).isEqualTo(SdkNumber.fromInteger(1));
        assertThat(document.getList("numberList", EnhancedType.of(BigDecimal.class)))
            .containsExactly(BigDecimal.valueOf(4), BigDecimal.valueOf(5), BigDecimal.valueOf(6));
        assertThat(document.getList("numberList", EnhancedType.of(SdkNumber.class)))
            .containsExactly(SdkNumber.fromInteger(4), SdkNumber.fromInteger(5), SdkNumber.fromInteger(6));
        assertThat(document.get("simpleDate", EnhancedType.of(LocalDate.class))).isEqualTo(LocalDate.MIN);
        assertThat(document.getStringSet("stringSet")).containsExactly("one", "two");
        assertThat(document.getBytes("sdkByteKey")).isEqualTo(SdkBytes.fromUtf8String("a"));
        assertThat(document.getBytesSet("sdkByteSet"))
            .containsExactlyInAnyOrder(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b"));
        assertThat(document.getNumberSet("numberSetSet")).containsExactlyInAnyOrder(SdkNumber.fromInteger(1),
                                                                                    SdkNumber.fromInteger(2));

        Map<String, BigDecimal> expectedBigDecimalMap = new LinkedHashMap<>();
        expectedBigDecimalMap.put("78b3522c-2ab3-4162-8c5d-f093fa76e68c", BigDecimal.valueOf(3));
        expectedBigDecimalMap.put("4ae1f694-52ce-4cf6-8211-232ccf780da8", BigDecimal.valueOf(9));
        assertThat(document.getMap("simpleMap", EnhancedType.of(String.class), EnhancedType.of(BigDecimal.class)))
            .containsExactlyEntriesOf(expectedBigDecimalMap);

        Map<UUID, BigDecimal> expectedUuidBigDecimalMap = new LinkedHashMap<>();
        expectedUuidBigDecimalMap.put(UUID.fromString("78b3522c-2ab3-4162-8c5d-f093fa76e68c"), BigDecimal.valueOf(3));
        expectedUuidBigDecimalMap.put(UUID.fromString("4ae1f694-52ce-4cf6-8211-232ccf780da8"), BigDecimal.valueOf(9));
        assertThat(document.getMap("simpleMap", EnhancedType.of(UUID.class), EnhancedType.of(BigDecimal.class)))
            .containsExactlyEntriesOf(expectedUuidBigDecimalMap);
    }

    @Test
    void enhancedDocWithNestedListAndMaps() {
        /**
         * No attributeConverters supplied, in this case it uses the {@link DefaultAttributeConverterProvider} and does not error
         */
        EnhancedDocument simpleDoc = EnhancedDocument.builder()
                                                     .attributeConverterProviders(defaultProvider())
                                                     .putString("HashKey", "abcdefg123")
                                                     .putNull("nullKey")
                                                     .putNumber("numberKey", 2.0)
                                                     .putBytes("sdkByte", SdkBytes.fromUtf8String("a"))
                                                     .putBoolean("booleanKey", true)
                                                     .putJson("jsonKey", "{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}")
                                                     .putStringSet("stingSet",
                                                                   Stream.of("a", "b", "c").collect(Collectors.toSet()))

                                                     .putNumberSet("numberSet", Stream.of(1, 2, 3, 4).collect(Collectors.toSet()))
                                                     .putBytesSet("sdkByteSet",
                                                                  Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet()))
                                                     .build();

        assertThat(simpleDoc.toJson()).isEqualTo("{\"HashKey\":\"abcdefg123\",\"nullKey\":null,\"numberKey\":2.0,"
                                                 + "\"sdkByte\":\"YQ==\",\"booleanKey\":true,\"jsonKey\":{\"1\":[\"a\",\"b\","
                                                 + "\"c\"],\"2\":1},\"stingSet\":[\"a\",\"b\",\"c\"],\"numberSet\":[1,2,3,4],"
                                                 + "\"sdkByteSet\":[\"YQ==\"]}");


        assertThat(simpleDoc.isPresent("HashKey")).isTrue();
        // No Null pointer or doesnot exist is thrown
        assertThat(simpleDoc.isPresent("HashKey2")).isFalse();
        assertThat(simpleDoc.getString("HashKey")).isEqualTo("abcdefg123");
        assertThat(simpleDoc.isNull("nullKey")).isTrue();

        assertThat(simpleDoc.getNumber("numberKey")).isEqualTo(SdkNumber.fromDouble(2.0));
        assertThat(simpleDoc.getNumber("numberKey").bigDecimalValue().compareTo(BigDecimal.valueOf(2.0))).isEqualTo(0);

        assertThat(simpleDoc.getBytes("sdkByte")).isEqualTo(SdkBytes.fromUtf8String("a"));
        assertThat(simpleDoc.getBoolean("booleanKey")).isTrue();
        assertThat(simpleDoc.getJson("jsonKey")).isEqualTo("{\"1\":[\"a\",\"b\",\"c\"],\"2\":1}");
        assertThat(simpleDoc.getStringSet("stingSet")).isEqualTo(Stream.of("a", "b", "c").collect(Collectors.toSet()));
        assertThat(simpleDoc.getList("stingSet", EnhancedType.of(String.class))).isEqualTo(Stream.of("a", "b", "c").collect(Collectors.toList()));

        assertThat(simpleDoc.getNumberSet("numberSet")
                            .stream().map(n -> n.intValue()).collect(Collectors.toSet()))
            .isEqualTo(Stream.of(1, 2, 3, 4).collect(Collectors.toSet()));


        assertThat(simpleDoc.getBytesSet("sdkByteSet")).isEqualTo(Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet()));


        // Trying to access some other Types
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> simpleDoc.getBoolean("sdkByteSet"))
                                                              .withMessageContaining("BooleanAttributeConverter cannot convert "
                                                                                     + "an attribute of type BS into the requested type class java.lang.Boolean");


    }

    @Test
    void testNullArgsInStaticConstructor() {
        assertThatNullPointerException()
            .isThrownBy(() -> EnhancedDocument.fromAttributeValueMap(null))
            .withMessage("attributeValueMap must not be null.");

        assertThatNullPointerException()
            .isThrownBy(() -> EnhancedDocument.fromJson(null))
            .withMessage("json must not be null.");
    }

    @Test
    void accessingSetFromBuilderMethodsAsListsInDocuments() {
        Set<String> stringSet = Stream.of("a", "b", "c").collect(Collectors.toSet());

        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .addAttributeConverterProvider(defaultProvider())
                                                            .putStringSet("stringSet", stringSet)
                                                            .build();

        Set<String> retrievedStringSet = enhancedDocument.getStringSet("stringSet");
        assertThat(retrievedStringSet).isEqualTo(stringSet);
        // Note that this behaviour is different in V1 , in order to remain consistent with EnhancedDDB converters
        List<String> retrievedStringList = enhancedDocument.getList("stringSet", EnhancedType.of(String.class));
        assertThat(retrievedStringList).containsExactlyInAnyOrderElementsOf(stringSet);
    }

    @Test
    void builder_ResetsTheOldValues_beforeJsonSetterIsCalled() {

        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .attributeConverterProviders(defaultProvider())
                                                            .putString("simpleKeyOriginal", "simpleValueOld")
                                                            .json("{\"stringKey\": \"stringValue\"}")
                                                            .putString("simpleKeyNew", "simpleValueNew")
                                                            .build();

        assertThat(enhancedDocument.toJson()).isEqualTo("{\"stringKey\":\"stringValue\",\"simpleKeyNew\":\"simpleValueNew\"}");
        assertThat(enhancedDocument.getString("simpleKeyOriginal")).isNull();

    }

    @Test
    void builder_with_NullKeys() {
        String EMPTY_OR_NULL_ERROR = "Attribute name must not be null or empty.";
        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putString(null, "Sample"))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putNull(null))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putNumber(null, 3))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putList(null, Arrays.asList(), EnhancedType.of(String.class)))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putBytes(null, SdkBytes.fromUtf8String("a")))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putMap(null, new HashMap<>(), null, null))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putStringSet(null, Stream.of("a").collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putNumberSet(null, Stream.of(1).collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putStringSet(null, Stream.of("a").collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> EnhancedDocument.builder().putBytesSet(null, Stream.of(SdkBytes.fromUtf8String("a"))
                                                                                 .collect(Collectors.toSet())))
            .withMessage(EMPTY_OR_NULL_ERROR);
    }

    @Test
    void errorWhen_NoAttributeConverter_IsProviderIsDefined() {
        EnhancedDocument enhancedDocument = testDataInstance().dataForScenario("simpleString")
                                                              .getEnhancedDocument()
                                                              .toBuilder()
                                                              .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                              .build();

        EnhancedType getType = EnhancedType.of(EnhancedDocumentTestData.class);
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> enhancedDocument.get(
            "stringKey", getType
        )).withMessage(
            "AttributeConverter not found for class EnhancedType(java.lang.String). Please add an AttributeConverterProvider "
            + "for this type. "
            + "If it is a default type, add the DefaultAttributeConverterProvider to the builder.");
    }

    @Test
    void access_NumberAttributeFromMap() {
        EnhancedDocument enhancedDocument = EnhancedDocument.fromJson(testDataInstance()
                                                                          .dataForScenario("ElementsOfCustomType")
                                                                          .getJson());

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
                                                                              enhancedDocument.getNumber("customMapValue"))
                                                              .withMessage(
                                                                  "software.amazon.awssdk.enhanced.dynamodb.internal.converter"
                                                                  + ".attribute.SdkNumberAttributeConverter cannot convert"
                                                                  + " an attribute of type M into the requested type class "
                                                                  + "software.amazon.awssdk.core.SdkNumber");
    }

    @Test
    void access_CustomType_without_AttributeConverterProvider() {
        EnhancedDocument enhancedDocument = EnhancedDocument.fromJson(testDataInstance()
                                                                          .dataForScenario("ElementsOfCustomType")
                                                                          .getJson());

        EnhancedType enhancedType = EnhancedType.of(
            CustomClassForDocumentAPI.class);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> enhancedDocument.get(
                "customMapValue", enhancedType)).withMessage("Converter not found for "
                                                             + "EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters"
                                                             + ".document.CustomClassForDocumentAPI)");
        EnhancedDocument docWithCustomProvider =
            enhancedDocument.toBuilder().attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(),
                                                                     defaultProvider()).build();
        assertThat(docWithCustomProvider.get("customMapValue", EnhancedType.of(CustomClassForDocumentAPI.class))).isNotNull();
    }

    @Test
    void error_When_DefaultProviderIsPlacedCustomProvider() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one")
                                                                          .longNumber(26L)
                                                                          .aBoolean(false).build();
        EnhancedDocument afterCustomClass = EnhancedDocument.builder()
                                                            .attributeConverterProviders(

                                                                CustomAttributeForDocumentConverterProvider.create(),
                                                                defaultProvider())
                                                            .putString("direct_attr", "sample_value")
                                                            .put("customObject", customObject,
                                                                 EnhancedType.of(CustomClassForDocumentAPI.class))
                                                            .build();

        assertThat(afterCustomClass.toJson()).isEqualTo("{\"direct_attr\":\"sample_value\",\"customObject\":{\"longNumber\":26,"
                                                        + "\"string\":\"str_one\"}}");

        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .putString("direct_attr", "sample_value")
                                                            .put("customObject", customObject,
                                                                 EnhancedType.of(CustomClassForDocumentAPI.class)).attributeConverterProviders
                                                                (defaultProvider(),
                                                                 CustomAttributeForDocumentConverterProvider.create())
                                                            .build();

        assertThatIllegalStateException().isThrownBy(
            () -> enhancedDocument.toJson()
        ).withMessage("Converter not found for "
                      + "EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "  ", "\n", "\r", "\f"})
    void invalidKeyNames(String escapingString) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                                                            EnhancedDocument.builder()
                                                                            .attributeConverterProviders(defaultProvider())
                                                                            .putString(escapingString, "sample")
                                                                            .build())
                                            .withMessageContaining("Attribute name must not be null or empty.");
    }

    @ParameterizedTest
    @MethodSource("escapeDocumentStrings")
    void escapingTheValues(String escapingString, String expectedJson) throws JsonProcessingException {

        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(defaultProvider())
                                                    .putString("key", escapingString)
                                                    .build();
        assertThat(document.toJson()).isEqualTo(expectedJson);
        assertThat(new ObjectMapper().readTree(document.toJson())).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("unEscapeDocumentStrings")
    void unEscapingTheValues(String escapingString, String expectedJson) throws JsonProcessingException {

        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(defaultProvider())
                                                    .putString("key", escapingString)
                                                    .build();
        assertThat(document.toJson()).isEqualTo(expectedJson);
        assertThat(new ObjectMapper().readTree(document.toJson())).isNotNull();

    }

    @Test
    void removeParameterFromDocument() {
        EnhancedDocument allSimpleTypes = testDataInstance().dataForScenario("allSimpleTypes").getEnhancedDocument();
        assertThat(allSimpleTypes.isPresent("nullKey")).isTrue();
        assertThat(allSimpleTypes.isNull("nullKey")).isTrue();
        assertThat(allSimpleTypes.getNumber("numberKey").intValue()).isEqualTo(10);
        assertThat(allSimpleTypes.getString("stringKey")).isEqualTo("stringValue");

        EnhancedDocument removedAttributesDoc = allSimpleTypes.toBuilder()
                                                              .remove("nullKey")
                                                              .remove("numberKey")
                                                              .build();

        assertThat(removedAttributesDoc.isPresent("nullKey")).isFalse();
        assertThat(removedAttributesDoc.isNull("nullKey")).isFalse();
        assertThat(removedAttributesDoc.isPresent("numberKey")).isFalse();
        assertThat(removedAttributesDoc.getString("stringKey")).isEqualTo("stringValue");

        assertThatIllegalArgumentException().isThrownBy(
                                                () -> removedAttributesDoc.toBuilder().remove(""))
                                            .withMessage("Attribute name must not be null or empty");


        assertThatIllegalArgumentException().isThrownBy(
                                                () -> removedAttributesDoc.toBuilder().remove(null))
                                            .withMessage("Attribute name must not be null or empty");
    }

    @Test
    void nullValueInsertion() {

        final String SAMPLE_KEY = "sampleKey";

        String expectedNullMessage = "Value for sampleKey must not be null. Use putNull API to insert a Null value";

        EnhancedDocument.Builder builder = EnhancedDocument.builder();
        assertThatNullPointerException().isThrownBy(() -> builder.putString(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.put(SAMPLE_KEY, null,
                                                                      EnhancedType.of(String.class))).withMessageContaining(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putNumber(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putBytes(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putStringSet(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putBytesSet(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putJson(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putNumberSet(SAMPLE_KEY, null)).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putMap(SAMPLE_KEY, null, EnhancedType.of(String.class),
                                                                         EnhancedType.of(String.class))).withMessage(expectedNullMessage);
        assertThatNullPointerException().isThrownBy(() -> builder.putList(SAMPLE_KEY, null, EnhancedType.of(String.class))).withMessage(expectedNullMessage);
    }

    @Test
    void accessingNulAttributeValue() {
        String NULL_KEY = "nullKey";
        EnhancedDocument enhancedDocument =
            EnhancedDocument.builder().attributeConverterProviders(defaultProvider()).putNull(NULL_KEY).build();

        Assertions.assertNull(enhancedDocument.getString(NULL_KEY));
        Assertions.assertNull(enhancedDocument.getList(NULL_KEY, EnhancedType.of(String.class)));
        assertThat(enhancedDocument.getBoolean(NULL_KEY)).isNull();
    }

    @Test
    void booleanValueRepresentation() {
        EnhancedDocument.Builder builder = EnhancedDocument.builder()
                                                           .attributeConverterProviders(defaultProvider());
        assertThat(builder.putString("boolean", "true").build().getBoolean("boolean")).isTrue();
        assertThat(builder.putNumber("boolean", 1).build().getBoolean("boolean")).isTrue();
    }

    @Test
    void putAndGetOfCustomTypes_with_EnhancedTypeApi() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one")
                                                                          .longNumber(26L)
                                                                          .aBoolean(false).build();
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .attributeConverterProviders(
                                                                CustomAttributeForDocumentConverterProvider.create(),
                                                                defaultProvider())
                                                            .putString("direct_attr", "sample_value")
                                                            .put("customObject", customObject,
                                                                 EnhancedType.of(CustomClassForDocumentAPI.class))
                                                            .build();

        assertThat(enhancedDocument.get("customObject", EnhancedType.of(CustomClassForDocumentAPI.class)))
            .isEqualTo(customObject);
    }

    @Test
    void putAndGetOfCustomTypes_with_ClassTypes() {
        CustomClassForDocumentAPI customObject = CustomClassForDocumentAPI.builder().string("str_one")
                                                                          .longNumber(26L)
                                                                          .aBoolean(false).build();
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .attributeConverterProviders(
                                                                CustomAttributeForDocumentConverterProvider.create(),
                                                                defaultProvider())
                                                            .putString("direct_attr", "sample_value")
                                                            .put("customObject", customObject,
                                                                 CustomClassForDocumentAPI.class)
                                                            .build();

        assertThat(enhancedDocument.get("customObject", CustomClassForDocumentAPI.class)).isEqualTo(customObject);
    }

    @Test
    void error_when_usingClassGetPut_for_CollectionValues(){

        assertThatIllegalArgumentException().isThrownBy(
            () -> EnhancedDocument.builder().put("mapKey", new HashMap(), Map.class))
                                            .withMessage("Values of type Map are not supported by this API, please use the putMap API instead");

        assertThatIllegalArgumentException().isThrownBy(
            () -> EnhancedDocument.builder().put("listKey", new ArrayList<>() , List.class))
                                            .withMessage("Values of type List are not supported by this API, please use the putList API instead");


        assertThatIllegalArgumentException().isThrownBy(
                                                () -> EnhancedDocument.builder().build().get("mapKey", Map.class))
                                            .withMessage("Values of type Map are not supported by this API, please use the getMap API instead");

        assertThatIllegalArgumentException().isThrownBy(
                                                () -> EnhancedDocument.builder().build().get("listKey" , List.class))
                                            .withMessage("Values of type List are not supported by this API, please use the getList API instead");
    }
}
