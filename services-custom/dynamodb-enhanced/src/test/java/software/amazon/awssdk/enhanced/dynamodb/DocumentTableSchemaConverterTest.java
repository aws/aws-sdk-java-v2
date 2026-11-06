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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests typed item conversion through document table schemas.
 * <p>
 * The tests verify default and configured converter providers, provider ordering, fallback, and propagated failures.
 * They also cover typed scalar, list, and nested document values, explicit document nulls, null items, preconverted
 * attribute values, schema metadata, and unsupported attribute converter lookup.
 */
public class DocumentTableSchemaConverterTest {

    @Test
    @DisplayName("The default schema provider supports typed reads")
    void mapToItem_whenDefaultSchema_installsDefaultProviderAndReadsString() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        Map<String, AttributeValue> map = Collections.singletonMap("value", AttributeValue.fromS("text"));

        EnhancedDocument document = schema.mapToItem(map);
        String read = document.get("value", type);

        assertThat(document.attributeConverterProviders())
            .contains(AttributeConverterProvider.defaultProvider());
        assertThat(read).isEqualTo("text");
    }

    @Test
    @DisplayName("The default schema provider supports typed writes")
    void itemToMap_whenTypedIntegerDocument_storesN() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", 7, EnhancedType.of(Integer.class))
                                                    .build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);

        assertThat(map).containsEntry("value", AttributeValue.fromN("7"));
    }

    @Test
    @DisplayName("A document provider precedes the schema provider and is invoked twice by the merged chain")
    void itemToMap_whenDocumentProviderBeforeSchemaProvider_selectsDocumentConverterAndCallsItTwice() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingPrefixProvider documentProvider = new RecordingPrefixProvider("document");
        RecordingPrefixProvider schemaProvider = new RecordingPrefixProvider("schema");
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(documentProvider)
                                                    .put("value", new CustomType("x"), type)
                                                    .build();
        DocumentTableSchema schema = DocumentTableSchema.builder()
                                                        .attributeConverterProviders(schemaProvider)
                                                        .build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);

        assertThat(documentProvider.requestedTypes()).containsExactly(type, type);
        assertThat(map.get("value").s()).isEqualTo("document:x");
        assertThat(schemaProvider.requestedTypes()).isEmpty();
    }

    @Test
    @DisplayName("A null-returning document provider falls back to the schema default")
    void itemToMap_whenNullReturningDocumentProvider_fallsBackToSchemaDefault() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        ReturningNullProvider returningNull = new ReturningNullProvider();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(returningNull)
                                                    .put("value", "text", type)
                                                    .build();
        DocumentTableSchema schema = DocumentTableSchema.builder().build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);

        assertThat(returningNull.requestedTypes()).containsExactly(type);
        assertThat(map).containsEntry("value", AttributeValue.fromS("text"));
    }

    @Test
    @DisplayName("A default document provider can block a later schema custom provider")
    void itemToMap_whenDefaultDocumentProviderBeforeSchemaCustom_throwsConverterNotFoundAndSkipsSchemaCustom() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingPrefixProvider schemaCustom = new RecordingPrefixProvider("schema");
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(
                                                        AttributeConverterProvider.defaultProvider())
                                                    .put("value", new CustomType("x"), type)
                                                    .build();
        DocumentTableSchema schema = DocumentTableSchema.builder()
                                                        .attributeConverterProviders(schemaCustom)
                                                        .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
        assertThat(schemaCustom.requestedTypes()).isEmpty();
    }

    @Test
    @DisplayName("Empty schema providers fail lazily for typed data")
    void itemToMap_whenEmptySchemaProvidersAndTypedString_throwsDocumentConverterNotFound() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        DocumentTableSchema schema = DocumentTableSchema.builder()
                                                        .attributeConverterProviders(Collections.emptyList())
                                                        .build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", "text", type)
                                                    .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(documentConverterNotFoundMessage(type));
    }

    @Test
    @DisplayName("Empty schema providers preserve preconverted AttributeValue data")
    void itemToMap_whenEmptySchemaProvidersAndAttributeValue_preservesSWithoutConverterLookup() {
        DocumentTableSchema schema = DocumentTableSchema.builder()
                                                        .attributeConverterProviders(Collections.emptyList())
                                                        .build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeValueMap(Collections.singletonMap(
                                                        "value", AttributeValue.fromS("text")))
                                                    .build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);

        assertThat(map).containsEntry("value", AttributeValue.fromS("text"));
    }

    @Test
    @DisplayName("A null item write is explicitly supported")
    void itemToMap_whenNullItem_returnsNull() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();

        assertThat(schema.itemToMap(null, false)).isNull();
    }

    @Test
    @DisplayName("A null map read is explicitly supported")
    void mapToItem_whenNullMap_returnsNull() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();

        assertThat(schema.mapToItem(null)).isNull();
    }

    @Test
    @DisplayName("A false ignore-null flag retains explicit document null")
    void itemToMap_whenExplicitNullWithIgnoreNullsFalse_retainsNul() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder().putNull("value").build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);

        assertThat(map).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("A true ignore-null flag also retains explicit document null")
    void itemToMap_whenExplicitNullWithIgnoreNullsTrue_retainsNul() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder().putNull("value").build();

        Map<String, AttributeValue> map = schema.itemToMap(document, true);

        assertThat(map).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("Attribute converter lookup is unsupported")
    void converterForAttribute_whenAnyName_throwsUnsupportedOperationExceptionWithNullMessage() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();

        assertThatThrownBy(() -> schema.converterForAttribute("value"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage(null);
    }

    @Test
    @DisplayName("The document schema item type is fixed as EnhancedDocument")
    void itemType_whenBuiltSchema_returnsEnhancedDocumentType() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();

        assertThat(schema.itemType()).isEqualTo(EnhancedType.of(EnhancedDocument.class));
    }

    @Test
    @DisplayName("A typed Object write fails converter lookup")
    void itemToMap_whenTypedObject_throwsConverterNotFound() {
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", new Object(), EnhancedType.of(Object.class))
                                                    .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("An unsupported typed write propagates the schema default provider failure")
    void itemToMap_whenUnsupportedType_throwsConverterNotFound() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", new UnsupportedType("x"), type)
                                                    .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("The default schema provider converts a typed list")
    void itemToMapThenMapToItem_whenTypedStringList_roundTripsAsLAndArrayList() {
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);
        List<String> input = new ArrayList<>();
        input.add("a");
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", input, type)
                                                    .build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);
        EnhancedDocument readDocument = schema.mapToItem(map);
        List<String> read = readDocument.get("value", type);

        assertThat(map.get("value").hasL()).isTrue();
        assertThat(map.get("value").l()).containsExactly(AttributeValue.fromS("a"));
        assertThat(read).isInstanceOf(ArrayList.class).isEqualTo(input);
    }

    @Test
    @DisplayName("The default schema provider converts a typed nested document")
    void itemToMapThenMapToItem_whenSchemaBearingDocument_storesMAndReconstructsThroughSchema() {
        TableSchema<DocumentType> documentSchema = documentSchema();
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, documentSchema);
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        DocumentType input = new DocumentType();
        input.setName("doc");
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", input, type)
                                                    .build();

        Map<String, AttributeValue> map = schema.itemToMap(document, false);
        EnhancedDocument readDocument = schema.mapToItem(map);
        DocumentType read = readDocument.get("value", type);

        assertThat(map.get("value").hasM()).isTrue();
        assertThat(map.get("value").m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read.getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("The default schema provider rejects a typed concrete list")
    void itemToMap_whenConcreteArrayList_throwsConverterNotFound() {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() { };
        ArrayList<String> input = new ArrayList<>();
        input.add("a");
        DocumentTableSchema schema = DocumentTableSchema.builder().build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", input, type)
                                                    .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A schema provider exception propagates through document conversion")
    void itemToMap_whenThrowingSchemaProvider_propagatesExceptionAndSkipsLaterProvider() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        AttributeConverterProvider throwing = new ThrowingProvider();
        AttributeConverterProvider later = mock(AttributeConverterProvider.class);
        DocumentTableSchema schema = DocumentTableSchema.builder()
                                                        .attributeConverterProviders(throwing, later)
                                                        .build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", new CustomType("x"), type)
                                                    .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + type);
        verifyNoInteractions(later);
    }

    @Test
    @DisplayName("Object lookup consults the merged schema provider before reporting a missing converter")
    void itemToMap_whenObjectTypeWithObjectProviderOnSchema_throwsConverterNotFoundAfterInvokingProvider() {
        AttributeConverterProvider objectProvider = mock(AttributeConverterProvider.class);
        DocumentTableSchema schema = DocumentTableSchema.builder()
                                                        .attributeConverterProviders(
                                                            objectProvider,
                                                            AttributeConverterProvider.defaultProvider())
                                                        .build();
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", new Object(), EnhancedType.of(Object.class))
                                                    .build();

        assertThatThrownBy(() -> schema.itemToMap(document, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
        verify(objectProvider).converterFor(EnhancedType.of(Object.class));
    }

    private static TableSchema<DocumentType> documentSchema() {
        return StaticTableSchema.builder(DocumentType.class)
                                .newItemSupplier(DocumentType::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(DocumentType::getName)
                                                                  .setter(DocumentType::setName))
                                .build();
    }

    private static String documentConverterNotFoundMessage(EnhancedType<?> type) {
        return "AttributeConverter not found for class " + type
               + ". Please add an AttributeConverterProvider for this type. If it is a default type, add the "
               + "DefaultAttributeConverterProvider to the builder.";
    }

    static final class CustomType {
        private final String value;

        CustomType(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    static final class UnsupportedType {
        private final String value;

        UnsupportedType(String value) {
            this.value = value;
        }
    }

    static final class DocumentType {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static final class ReturningNullProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            return null;
        }

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }
    }

    static final class CustomTypeConverter implements AttributeConverter<CustomType> {
        private final String prefix;

        CustomTypeConverter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public AttributeValue transformFrom(CustomType input) {
            return AttributeValue.fromS(prefix + ":" + input.value());
        }

        @Override
        public CustomType transformTo(AttributeValue input) {
            String text = input.s();
            return new CustomType(text.substring(prefix.length() + 1));
        }

        @Override
        public EnhancedType<CustomType> type() {
            return EnhancedType.of(CustomType.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    static final class RecordingPrefixProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();
        private final CustomTypeConverter converter;

        RecordingPrefixProvider(String prefix) {
            this.converter = new CustomTypeConverter(prefix);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(CustomType.class).equals(enhancedType)) {
                return (AttributeConverter<T>) converter;
            }
            return null;
        }

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }
    }

    static final class ThrowingProvider implements AttributeConverterProvider {
        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            throw new IllegalArgumentException("Attribute converter provider failed while looking up " + enhancedType);
        }
    }

}
