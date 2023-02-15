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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.ARRAY_AND_MAP_IN_JSON;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.ARRAY_MAP_ATTRIBUTE_VALUE;
import static software.amazon.awssdk.enhanced.dynamodb.document.DefaultEnhancedDocumentTest.STRING_ARRAY_ATTRIBUTES_LISTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ChainConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticKeyAttributeMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DocumentTableSchemaTest {

    String NO_PRIMARY_KEYS_IN_METADATA = "Attempt to execute an operation that requires a primary index without defining "
                                         + "any primary key attributes in the table metadata.";

    @Test
    void converterForAttribute_APIIsNotSupported(){
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
            () -> documentTableSchema.attributeNames()).withMessage(NO_PRIMARY_KEYS_IN_METADATA);
        assertThat(documentTableSchema.attributeValue(EnhancedDocument.builder().build(), "key")).isNull();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> documentTableSchema.tableMetadata().primaryKeys());

    }

    @Test
    void tableMetaData_With_BothSortAndHashKey_InTheBuilder() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema
            .builder()
            .primaryKey("sampleHashKey", AttributeValueType.S)
            .sortKey("sampleSortKey", AttributeValueType.S)
            .build();
        assertThat(documentTableSchema.attributeNames()).isEqualTo(Arrays.asList("sampleHashKey", "sampleSortKey"));
        assertThat(documentTableSchema.tableMetadata().keyAttributes().stream().collect(Collectors.toList())).isEqualTo(
            Arrays.asList(StaticKeyAttributeMetadata.create("sampleHashKey", AttributeValueType.S),
                          StaticKeyAttributeMetadata.create("sampleSortKey", AttributeValueType.S)));
    }

    @Test
    void tableMetaData_WithOnly_HashKeyInTheBuilder(){
        DocumentTableSchema documentTableSchema = DocumentTableSchema
            .builder()
            .primaryKey("sampleHashKey", AttributeValueType.S)
            .build();
        assertThat(documentTableSchema.attributeNames()).isEqualTo(Arrays.asList("sampleHashKey"));
        assertThat(documentTableSchema.tableMetadata().keyAttributes().stream().collect(Collectors.toList())).isEqualTo(
            Arrays.asList(StaticKeyAttributeMetadata.create("sampleHashKey", AttributeValueType.S)));
    }

    @Test
    void defaultConverter_IsCreated_When_NoConverter_IsPassedInBuilder_IgnoreNullAsFalse(){
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .addNull("nullKey")
                                                            .addString("stringKey", "stringValue")
                                                            .build();
        Map<String, AttributeValue> ignoreNullAsFalseMap = documentTableSchema.itemToMap(enhancedDocument,false);
        Map<String, AttributeValue> expectedMap = new LinkedHashMap<>();
        expectedMap.put("nullKey", AttributeValue.fromNul(true));
        expectedMap.put("stringKey", AttributeValue.fromS("stringValue"));
        assertThat(ignoreNullAsFalseMap).isEqualTo(expectedMap);
    }

    @Test
    void documentTableSchema_Errors_withEmptyDocument(){
        EnhancedDocument document = getAnonymousEnhancedDocument();
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        assertThat(documentTableSchema.itemToMap(document,true)).isNull();
        assertThat(documentTableSchema.itemToMap(document,new ArrayList<>())).isEqualTo(new LinkedHashMap<>());
        assertThat(documentTableSchema.attributeValue(document, "someItem")).isNull();
    }

    @Test
    void document_itemToMap_with_ComplexArrayMap(){
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.fromJson(ARRAY_AND_MAP_IN_JSON);
        Map<String, AttributeValue> stringAttributeValueMap = documentTableSchema.itemToMap(document, false);
        assertThat(stringAttributeValueMap).isEqualTo(ARRAY_MAP_ATTRIBUTE_VALUE.getAttributeValueMap());
        Map<String, AttributeValue> listOfAttributes = documentTableSchema.itemToMap(document, Arrays.asList("numKey","mapKey"));
        assertThat(listOfAttributes.size()).isEqualTo(2);
        assertThat(listOfAttributes.keySet()).isEqualTo(Stream.of("numKey", "mapKey").collect(Collectors.toSet()));
        AttributeValue attributeValue = documentTableSchema.attributeValue(document, "mapKey");
        assertThat(attributeValue.hasM()).isTrue();
        assertThat(attributeValue.m().get("1")).isEqualTo(STRING_ARRAY_ATTRIBUTES_LISTS);
        assertThat(listOfAttributes.size()).isEqualTo(2);
        assertThat(listOfAttributes.keySet()).isEqualTo(Stream.of("numKey", "mapKey").collect(Collectors.toSet()));
    }

    @Test
    void mapToItem_converts_DocumentItem() {
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder().build();
        EnhancedDocument document = documentTableSchema.mapToItem(ARRAY_MAP_ATTRIBUTE_VALUE.getAttributeValueMap());
        assertThat(document.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);
        assertThat(documentTableSchema.mapToItem(null)).isNull();
    }

    @Test
    void enhanceTypeOf_TableSchema(){
        assertThat(DocumentTableSchema.builder().build().itemType()).isEqualTo(EnhancedType.of(EnhancedDocument.class));
    }

    @Test
    void attributeConverters_ForAllAttributes_NotPassed_Uses_DefaultConverters(){
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder()
            .attributeConverterProviders(ChainConverterProvider.create()).build();
        EnhancedDocument document = documentTableSchema.mapToItem(ARRAY_MAP_ATTRIBUTE_VALUE.getAttributeValueMap());
        assertThat(document.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);
        Map<String, AttributeValue> stringAttributeValueMap = documentTableSchema.itemToMap(document, false);
        assertThat(stringAttributeValueMap).isEqualTo(ARRAY_MAP_ATTRIBUTE_VALUE.getAttributeValueMap());
    }

    @Test
    void emptyAttributeConvertersListPassed_UsesDefaultConverters(){
        DocumentTableSchema documentTableSchema = DocumentTableSchema.builder()
                                                                     .attributeConverterProviders(new ArrayList<>()).build();
        EnhancedDocument document = documentTableSchema.mapToItem(ARRAY_MAP_ATTRIBUTE_VALUE.getAttributeValueMap());
        assertThat(document.toJson()).isEqualTo(ARRAY_AND_MAP_IN_JSON);

        Map<String, AttributeValue> stringAttributeValueMap = documentTableSchema.itemToMap(document, false);
        assertThat(stringAttributeValueMap).isEqualTo(ARRAY_MAP_ATTRIBUTE_VALUE.getAttributeValueMap());
    }

    private static EnhancedDocument getAnonymousEnhancedDocument() {
        EnhancedDocument document = new EnhancedDocument() {
            @Override
            public Builder toBuilder() { return null; }
            @Override
            public boolean isNull(String attributeName) { return false; }
            @Override
            public boolean isPresent(String attributeName) { return false; }
            @Override
            public <T> T get(String attributeName, EnhancedType<T> type) { return null; }
            @Override
            public String getString(String attributeName) { return null; }
            @Override
            public SdkNumber getSdkNumber(String attributeName) {return null;}
            @Override
            public SdkBytes getSdkBytes(String attributeName) {return null;}
            @Override
            public Set<String> getStringSet(String attributeName) { return null;}
            @Override
            public Set<SdkNumber> getNumberSet(String attributeName) {return null;}
            @Override
            public Set<SdkBytes> getSdkBytesSet(String attributeName) {return null;}
            @Override
            public <T> List<T> getList(String attributeName, EnhancedType<T> type) {return null;}
            @Override
            public List<?> getList(String attributeName) {return null;}
            @Override
            public <T> Map<String, T> getMap(String attributeName, EnhancedType<T> type) {return null;}
            @Override
            public <T extends Number> Map<String, T> getMapOfNumbers(String attributeName, Class<T> valueType) {return null;}
            @Override
            public Map<String, Object> getRawMap(String attributeName) {return null;}
            @Override
            public EnhancedDocument getMapAsDocument(String attributeName) {return null;}
            @Override
            public String getJson(String attributeName) {return null;}
            @Override
            public String getJsonPretty(String attributeName) {return null;}
            @Override
            public Boolean getBoolean(String attributeName) {return null;}
            @Override
            public Object get(String attributeName) {return null;}
            @Override
            public EnhancedType<?> getTypeOf(String attributeName) {return null;}
            @Override
            public Map<String, Object> asMap() {return null;}
            @Override
            public String toJson() {return null;}
            @Override
            public String toJsonPretty() {return null;}

            @Override
            public Map<String, AttributeValue> toAttributeValueMap() {
                return null;
            }
        };
        return document;
    }


}
