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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.DefaultStringConverterProvider;

class ParameterizedDocumentTest {

    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_BuilderMethodsOfDefaultDocument(TestData testData) {
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */
        assertThat(testData.getEnhancedDocument().toMap()).isEqualTo(testData.getDdbItemMap());
    }

    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_validateJsonStringAreEqual(TestData testData) {
        System.out.println(testData.getScenario());
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */
        assertThat(testData.getEnhancedDocument().toJson()).isEqualTo(testData.getJson());
    }

    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_documentsCreated_fromJson(TestData testData) {
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */
        assertThat(EnhancedDocument.fromJson(testData.getJson()).toJson())
            .isEqualTo(testData.getEnhancedDocument().toJson());
    }

    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_documentsCreated_fromAttributeValueMap(TestData testData) {
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */

        assertThat(EnhancedDocument.fromAttributeValueMap(testData.getDdbItemMap()).toMap())
            .isEqualTo(testData.getDdbItemMap());
    }

        @ParameterizedTest
        @ArgumentsSource(EnhancedDocumentTestData.class)
        void validateGetterMethodsOfDefaultDocument(TestData testData) {
            EnhancedDocument enhancedDocument = testData.getEnhancedDocument();
            Map<String, List<EnhancedType>> enhancedTypeMap = testData.getTypeMap().enhancedTypeMap;
            AttributeConverterProvider chainConverterProvider = testData.getAttributeConverterProvider();

           assertThat(testData.getEnhancedDocument().toMap()).isEqualTo(testData.getDdbItemMap());

            testData.getDdbItemMap().forEach((key, value) -> {
                EnhancedAttributeValue enhancedAttributeValue = EnhancedAttributeValue.fromAttributeValue(value);

                switch (enhancedAttributeValue.type()) {
                    case NULL:
                        assertThat(enhancedDocument.isNull(key)).isTrue();
                        break;
                    case S:
                        assertThat(enhancedAttributeValue.asString()).isEqualTo(enhancedDocument.getString(key));
                        assertThat(enhancedAttributeValue.asString()).isEqualTo(enhancedDocument.get(key, String.class));
                        assertThat(enhancedAttributeValue.asString()).isEqualTo(enhancedDocument.get(key, EnhancedType.of(String.class)));
                        break;
                    case N:
                        assertThat(enhancedAttributeValue.asNumber()).isEqualTo(enhancedDocument.getNumber(key).stringValue());
                        assertThat(enhancedAttributeValue.asNumber()).isEqualTo(String.valueOf(enhancedDocument.get(key,
                                                                                                            SdkNumber.class)));
                        assertThat(enhancedAttributeValue.asNumber()).isEqualTo(enhancedDocument.get(key,
                                                                                                     EnhancedType.of(SdkNumber.class)).toString());
                        break;
                    case B:
                        assertThat(enhancedAttributeValue.asBytes()).isEqualTo(enhancedDocument.getBytes(key));
                        assertThat(enhancedAttributeValue.asBytes()).isEqualTo(enhancedDocument.get(key, SdkBytes.class));
                        assertThat(enhancedAttributeValue.asBytes()).isEqualTo(enhancedDocument.get(key, EnhancedType.of(SdkBytes.class)));
                        break;
                    case BOOL:
                        assertThat(enhancedAttributeValue.asBoolean()).isEqualTo(enhancedDocument.getBoolean(key));
                        assertThat(enhancedAttributeValue.asBoolean()).isEqualTo(enhancedDocument.get(key, Boolean.class));
                        assertThat(enhancedAttributeValue.asBoolean()).isEqualTo(enhancedDocument.get(key, EnhancedType.of(Boolean.class)));
                        break;
                    case NS:
                        Set<SdkNumber> expectedNumber = chainConverterProvider.converterFor(EnhancedType.setOf(SdkNumber.class)).transformTo(value);
                        assertThat(expectedNumber).isEqualTo(enhancedDocument.getNumberSet(key));
                        break;
                    case SS:
                        Set<String> stringSet = chainConverterProvider.converterFor(EnhancedType.setOf(String.class)).transformTo(value);
                        assertThat(stringSet).isEqualTo(enhancedDocument.getStringSet(key));
                        break;
                    case BS:
                        Set<SdkBytes> sdkBytesSet = chainConverterProvider.converterFor(EnhancedType.setOf(SdkBytes.class)).transformTo(value);
                        assertThat(sdkBytesSet).isEqualTo(enhancedDocument.getBytesSet(key));
                        break;
                    case L:
                        EnhancedType enhancedType = enhancedTypeMap.get(key).get(0);
                        ListAttributeConverter converter = ListAttributeConverter
                            .create(chainConverterProvider.converterFor(enhancedType));
                        if(converter == null){
                            throw new IllegalStateException("Converter not found for " + enhancedType);
                        }
                        assertThat(converter.transformTo(value)).isEqualTo(enhancedDocument.getList(key, enhancedType));
                        assertThat(enhancedDocument.getListOfUnknownType(key)).isEqualTo(value.l());
                        break;
                    case M:
                        EnhancedType keyType = enhancedTypeMap.get(key).get(0);
                        EnhancedType valueType = enhancedTypeMap.get(key).get(1);
                        MapAttributeConverter mapAttributeConverter = MapAttributeConverter.mapConverter(
                            DefaultStringConverterProvider.create().converterFor(keyType),
                            chainConverterProvider.converterFor(valueType)
                        );
                        assertThat(mapAttributeConverter.transformTo(value))
                            .isEqualTo(enhancedDocument.getMap(key, keyType, valueType));
                        assertThat(enhancedDocument.getMapOfUnknownType(key)).isEqualTo(value.m());
                        break;
                    default:
                        throw new IllegalStateException("EnhancedAttributeValue type not found: " + enhancedAttributeValue.type());
                }
            });
        }
}
