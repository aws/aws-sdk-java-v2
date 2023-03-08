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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.testDataInstance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;

public class EnhancedDocumentTest{

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

            assertThat(enhancedDocument.toJson()).isEqualTo("{\"stringKey\": \"stringValue\", \"simpleKeyNew\": "
                                                            + "\"simpleValueNew\"}");
            assertThat(enhancedDocument.getString("simpleKeyOriginal")).isNull();

        }

        @Test
        void builder_with_NullKeys() {
            String EMPTY_OR_NULL_ERROR = "attributeName must not be null.";
            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putString(null, "Sample"))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putNull(null))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putNumber(null, 3))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putList(null, Arrays.asList(), EnhancedType.of(String.class)))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putBytes(null, SdkBytes.fromUtf8String("a")))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putMapOfType(null, new HashMap<>(), null, null))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putStringSet(null, Stream.of("a").collect(Collectors.toSet())))
                .withMessage(EMPTY_OR_NULL_ERROR);

            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putNumberSet(null, Stream.of(1).collect(Collectors.toSet())))
                .withMessage(EMPTY_OR_NULL_ERROR);
            assertThatNullPointerException()
                .isThrownBy(() -> EnhancedDocument.builder().putStringSet(null, Stream.of("a").collect(Collectors.toSet())))
                .withMessage(EMPTY_OR_NULL_ERROR);
            assertThatNullPointerException()
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

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> enhancedDocument.get(
            "stringKey",
            EnhancedType.of(
                EnhancedDocumentTestData.class))).withMessage(
            "AttributeConverter not found for class EnhancedType(java.lang.String). Please add an AttributeConverterProvider for this type. "
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
                "software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkNumberAttributeConverter cannot convert"
                + " an attribute of type M into the requested type class software.amazon.awssdk.core.SdkNumber");
        }

    @Test
    void access_CustomType_without_AttributeConverterProvider() {
        EnhancedDocument enhancedDocument = EnhancedDocument.fromJson(testDataInstance()
                                                                          .dataForScenario("ElementsOfCustomType")
                                                                          .getJson());

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> enhancedDocument.get(
                "customMapValue",
                EnhancedType.of(
                    CustomClassForDocumentAPI.class))).withMessage("Converter not found for "
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
                                                                .putWithType("customObject",customObject,
                                                                             EnhancedType.of(CustomClassForDocumentAPI.class))
                                                                .build();

            assertThat(afterCustomClass.toJson()).isEqualTo("{\"direct_attr\": \"sample_value\", \"customObject\": "
                                                            + "{\"longNumber\": 26,\"string\": \"str_one\"}}");

            EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                     .putString("direct_attr", "sample_value")
                                                     .putWithType("customObject", customObject,
                                                                  EnhancedType.of(CustomClassForDocumentAPI.class)).attributeConverterProviders
                                                         (defaultProvider(), CustomAttributeForDocumentConverterProvider.create())
                                                     .build();

            assertThatIllegalStateException().isThrownBy(
                () -> enhancedDocument.toJson()
            ).withMessage("Converter not found for "
                          + "EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI)");
        }
}
