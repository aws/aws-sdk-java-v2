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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.testDataInstance;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData;
import software.amazon.awssdk.enhanced.dynamodb.document.TestData;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ChainConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class DocumentTableSchemaTest {

    String NO_PRIMARY_KEYS_IN_METADATA = "Attempt to execute an operation that requires a primary index without defining "
                                         + "any primary key attributes in the table metadata.";

    @Test
    void converterForAttribute_APIIsNotSupported() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> documentTableSchema.converterForAttribute("someKey"));
    }

    @Test
    void defaultBuilderWith_NoElement_CreateEmptyMetaData() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        assertThat(documentTableSchema.tableMetadata()).isNotNull();
        assertThat(documentTableSchema.isAbstract()).isFalse();
        //Accessing attribute for documentTableSchema when TableMetaData not supplied in the builder.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
            () -> documentTableSchema.tableMetadata().primaryKeys()).withMessage(NO_PRIMARY_KEYS_IN_METADATA);
        assertThat(documentTableSchema.attributeValue(EnhancedDocument
                                                          .builder()
                                                          .addAttributeConverterProvider(defaultProvider())
                                                          .build(), "key")).isNull();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> documentTableSchema.tableMetadata().primaryKeys());

        assertThat(documentTableSchema.attributeNames()).isEqualTo(Collections.emptyList());


    }

    @Test
    void tableMetaData_With_BothSortAndHashKey_InTheBuilder() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema
            .builder()
            .addIndexPartitionKey(TableMetadata.primaryIndexName(), "sampleHashKey", AttributeValueType.S)
            .addIndexSortKey("sort-index", "sampleSortKey", AttributeValueType.S)
            .build();
        assertThat(documentTableSchema.attributeNames()).isEqualTo(Arrays.asList("sampleHashKey", "sampleSortKey"));
        assertThat(documentTableSchema.tableMetadata().keyAttributes().stream().collect(Collectors.toList())).isEqualTo(
            Arrays.asList(StaticKeyAttributeMetadata.create("sampleHashKey", AttributeValueType.S),
                          StaticKeyAttributeMetadata.create("sampleSortKey", AttributeValueType.S)));
    }

    @Test
    void tableMetaData_WithOnly_HashKeyInTheBuilder() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema
            .builder()
            .addIndexPartitionKey(
                TableMetadata.primaryIndexName(), "sampleHashKey", AttributeValueType.S)
            .build();
        assertThat(documentTableSchema.attributeNames()).isEqualTo(Collections.singletonList("sampleHashKey"));
        assertThat(documentTableSchema.tableMetadata().keyAttributes().stream().collect(Collectors.toList())).isEqualTo(
            Collections.singletonList(StaticKeyAttributeMetadata.create("sampleHashKey", AttributeValueType.S)));
    }

    @Test
    void defaultConverter_IsNotCreated_When_NoConverter_IsPassedInBuilder_IgnoreNullAsFalse() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .putNull("nullKey")
                                                            .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                            .putString("stringKey", "stringValue")
                                                            .build();


        assertThatIllegalStateException()
            .isThrownBy(() -> documentTableSchema.mapToItem(enhancedDocument.toMap(), false))
            .withMessageContaining("AttributeConverter not found for class EnhancedType(java.lang.String). "
                                   + "Please add an AttributeConverterProvider for this type. If it is a default type, add the "
                                   + "DefaultAttributeConverterProvider to the builder.");
    }

    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_DocumentTableSchemaItemToMap(TestData testData) {
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();

        Assertions.assertThat(
            documentTableSchema.itemToMap(testData.getEnhancedDocument(), false)).isEqualTo(testData.getDdbItemMap());
    }

    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_DocumentTableSchema_mapToItem(TestData testData) {
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        assertThat(documentTableSchema.mapToItem(null)).isNull();
        Assertions.assertThat(
            documentTableSchema.mapToItem(testData.getDdbItemMap()).toMap()).isEqualTo(testData.getEnhancedDocument()
                                                                                               .toMap());
        // TODO : order mismatch ??
        //
        // Assertions.assertThat(
        //     documentTableSchema.mapToItem(testData.getDdbItemMap()).toJson()).isEqualTo(testData.getJson());
    }


    @Test
    void enhanceTypeOf_TableSchema() {
        assertThat(DocumentTableSchema.builder().build().itemType()).isEqualTo(EnhancedType.of(EnhancedDocument.class));
    }

    @Test
    void error_When_attributeConvertersIsOverwrittenToIncorrectConverter() {

        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().attributeConverterProviders(defaultProvider())
                                                                     .attributeConverterProviders(ChainConverterProvider.create()).build();
        TestData simpleStringData = testDataInstance().dataForScenario("simpleString");
        // Lazy loading is done , thus it does not fail until we try to access some doc from enhancedDocument
        EnhancedDocument enhancedDocument = documentTableSchema.mapToItem(simpleStringData.getDdbItemMap(), false);
        assertThatIllegalStateException().isThrownBy(
            () -> {
                enhancedDocument.getString("stringKey");
            }).withMessage(
            "AttributeConverter not found for class EnhancedType(java.lang.String). Please add an AttributeConverterProvider "
            + "for this type. "
            + "If it is a default type, add the DefaultAttributeConverterProvider to the builder.");
    }

    @Test
    void default_attributeConverters_isUsedFromTableSchema() {

        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        TestData simpleStringData = testDataInstance().dataForScenario("simpleString");
        EnhancedDocument enhancedDocument = documentTableSchema.mapToItem(simpleStringData.getDdbItemMap(), false);
        assertThat(enhancedDocument.getString("stringKey")).isEqualTo("stringValue");
    }

    @Test
    void custom_attributeConverters_isUsedFromTableSchema() {

        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder()
                                                                     .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(), defaultProvider())
                                                                     .build();
        TestData simpleStringData = testDataInstance().dataForScenario("customList");
        EnhancedDocument enhancedDocument = documentTableSchema.mapToItem(simpleStringData.getDdbItemMap(), false);
        assertThat(enhancedDocument.getList("customClassForDocumentAPI", EnhancedType.of(CustomClassForDocumentAPI.class)).size()).isEqualTo(2);
    }


    @ParameterizedTest
    @ArgumentsSource(EnhancedDocumentTestData.class)
    void validate_DocumentTableSchemaItemToMapWithFilter(TestData testData) {
        EnhancedDocument filterDocument = testData.getEnhancedDocument().toBuilder()
                                         .putString("filterOne", "str")
                                         .putBoolean("filterTwo", false)
                                         .putNumber("filterThree", 3L)
                                         .putNumber("noFilter", 10)
                                         .putNull("filterNull")
                                         .build();

        Map<String, AttributeValue> filteredAttributeValueMap = new LinkedHashMap<>();
        filteredAttributeValueMap.put("filterOne", AttributeValue.fromS("str"));
        filteredAttributeValueMap.put("filterTwo", AttributeValue.fromBool(false));
        filteredAttributeValueMap.put("filterThree", AttributeValue.fromN("3"));
        filteredAttributeValueMap.put("filterNull", AttributeValue.fromNul(true));

        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();

        Assertions.assertThat(
            documentTableSchema.itemToMap(filterDocument,
                                          Arrays.asList("filterOne", "filterTwo", "filterThree","filterNull")
                                          )).isEqualTo(filteredAttributeValueMap);
    }

    @Test
    void validate_DocumentTableSchema_WithCustomIntegerAttributeProvider() {
        EnhancedDocument numberDocument = EnhancedDocument.builder()
                                                          .putNumber("integerOne", 1)
                                                          .putNumber("integerTen", 10)
                                                          .putNull("null")
                                                          .build();

        Map<String, AttributeValue> resultMap = new LinkedHashMap<>();
        resultMap.put("integerOne", AttributeValue.fromN("11"));
        resultMap.put("integerTen", AttributeValue.fromN("20"));
        resultMap.put("null", AttributeValue.fromNul(true));

        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder()
                                                                     .attributeConverterProviders(
                                                                         Collections.singletonList(
                                                                             CustomAttributeForDocumentConverterProvider.create()))
                                                                     .build();
        Assertions.assertThat(
            documentTableSchema.itemToMap(numberDocument, true)).isEqualTo(resultMap);
    }
}
