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
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests typed value conversion performed by enhanced documents.
 * <p>
 * The tests cover scalar, enumeration, collection, map, iterable, and schema backed document values. They verify
 * provider ordering and fallback, recursive member lookup, JSON provider configuration, explicit null values,
 * unsupported type errors, and propagation of converter failures.
 */
public class EnhancedDocumentConverterTest {

    @Test
    @DisplayName("Typed scalar conversion stores S text and reads the original string")
    void putThenGet_whenStringType_storesSTextAndReadsOriginalString() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        EnhancedDocument document = defaultBuilder().put("value", "text", type).build();
        Map<String, AttributeValue> map = document.toMap();
        String read = document.get("value", type);

        assertThat(map.get("value")).isEqualTo(AttributeValue.fromS("text"));
        assertThat(read).isEqualTo("text");
    }

    @Test
    @DisplayName("Typed list conversion stores L and reads an ArrayList that retains order and duplicates")
    void putThenGet_whenStringList_storesLAndReadsArrayListRetainingOrderAndDuplicates() {
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("a");

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        List<String> read = document.get("value", type);

        assertThat(map.get("value").hasL()).isTrue();
        assertThat(read).isInstanceOf(ArrayList.class)
                        .containsExactly("a", "b", "a")
                        .isEqualTo(input);
    }

    @Test
    @DisplayName("Typed set conversion stores SS and reads an equal LinkedHashSet")
    void putThenGet_whenStringSet_storesSsAndReadsEqualLinkedHashSet() {
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);
        Set<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        Set<String> read = document.get("value", type);

        assertThat(map.get("value").hasSs()).isTrue();
        assertThat(read).isInstanceOf(LinkedHashSet.class).isEqualTo(input);
    }

    @Test
    @DisplayName("Typed map conversion stores M with an N member and reads an equal LinkedHashMap")
    void putThenGet_whenStringToIntegerMap_storesMWithNMemberAndReadsEqualLinkedHashMap() {
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("count", 7);

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        Map<String, Integer> read = document.get("value", type);

        assertThat(map.get("value").hasM()).isTrue();
        assertThat(map.get("value").m().get("count")).isEqualTo(AttributeValue.fromN("7"));
        assertThat(read).isInstanceOf(LinkedHashMap.class).isEqualTo(input);
    }

    @Test
    @DisplayName("Typed enum conversion stores toString text and reads the same constant")
    void putThenGet_whenEnumType_storesToStringTextAndReadsSameConstant() {
        EnhancedType<TestEnum> type = EnhancedType.of(TestEnum.class);
        TestEnum input = TestEnum.OPEN;

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        TestEnum read = document.get("value", type);

        assertThat(map.get("value")).isEqualTo(AttributeValue.fromS(input.toString()));
        assertThat(read).isSameAs(input);
    }

    @Test
    @DisplayName("Typed document conversion stores M and reconstructs through the table schema")
    void putThenGet_whenSchemaBearingDocument_storesMAndReconstructsThroughTableSchema() {
        TableSchema<DocumentType> documentSchema = documentSchema();
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, documentSchema);
        DocumentType input = new DocumentType();
        input.setName("doc");

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        DocumentType read = document.get("value", type);

        assertThat(map.get("value").hasM()).isTrue();
        assertThat(map.get("value").m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read.getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("Deep typed document conversion nests L then M then M and reconstructs the document")
    void putThenGet_whenListOfMapOfDocument_nestsLThenMThenMAndReconstructsDocument() {
        TableSchema<DocumentType> documentSchema = documentSchema();
        EnhancedType<List<Map<String, DocumentType>>> type =
            EnhancedType.listOf(EnhancedType.mapOf(EnhancedType.of(String.class),
                                                   EnhancedType.documentOf(DocumentType.class, documentSchema)));
        DocumentType nested = new DocumentType();
        nested.setName("doc");
        Map<String, DocumentType> inner = new LinkedHashMap<>();
        inner.put("doc", nested);
        List<Map<String, DocumentType>> input = new ArrayList<>();
        input.add(inner);

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        List<Map<String, DocumentType>> read = document.get("value", type);

        assertThat(map.get("value").hasL()).isTrue();
        assertThat(map.get("value").l().get(0).hasM()).isTrue();
        assertThat(map.get("value").l().get(0).m().get("doc").hasM()).isTrue();
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat(read.get(0)).isInstanceOf(LinkedHashMap.class);
        assertThat(read.get(0).get("doc").getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("A builder with no provider succeeds on build then fails on toMap with the document error")
    void toMap_whenBuilderWithNoProvider_throwsDocumentConverterNotFound() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        EnhancedDocument document = EnhancedDocument.builder().put("value", "text", type).build();

        assertThat(document).isNotNull();
        assertThatThrownBy(document::toMap)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(documentConverterNotFoundMessage(type));
    }

    @Test
    @DisplayName("An all-null custom chain reports the document error and calls each provider once")
    void toMap_whenAllNullCustomChain_throwsDocumentConverterNotFoundAndCallsEachProviderOnce() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        ReturningNullProvider first = new ReturningNullProvider();
        ReturningNullProvider second = new ReturningNullProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(first, second)
                                                 .put("value", "text", type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(documentConverterNotFoundMessage(type));
        assertThat(first.requestedTypes()).containsExactly(type);
        assertThat(second.requestedTypes()).containsExactly(type);
    }

    @Test
    @DisplayName("A null-returning custom provider falls back to the default provider")
    void toMap_whenNullReturningCustomThenDefault_fallsBackToDefaultStringConverter() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        ReturningNullProvider returningNull = new ReturningNullProvider();

        Map<String, AttributeValue> map = EnhancedDocument.builder()
                                                          .attributeConverterProviders(
                                                              returningNull,
                                                              AttributeConverterProvider.defaultProvider())
                                                          .put("value", "text", type)
                                                          .build()
                                                          .toMap();

        assertThat(returningNull.requestedTypes()).containsExactly(type);
        assertThat(map.get("value")).isEqualTo(AttributeValue.fromS("text"));
    }

    @Test
    @DisplayName("A custom provider before the default is selected twice by the chain")
    void toMap_whenCustomProviderBeforeDefault_selectsCustomConverterAndCallsProviderTwice() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingCustomProvider customProvider = new RecordingCustomProvider();

        Map<String, AttributeValue> map = EnhancedDocument.builder()
                                                          .attributeConverterProviders(
                                                              customProvider,
                                                              AttributeConverterProvider.defaultProvider())
                                                          .put("value", new CustomType("x"), type)
                                                          .build()
                                                          .toMap();

        assertThat(customProvider.requestedTypes()).containsExactly(type, type);
        assertThat(customProvider.converter().attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("custom:x");
    }

    @Test
    @DisplayName("A throwing custom provider exception propagates and skips the default provider")
    void toMap_whenThrowingCustomProviderBeforeDefault_propagatesExceptionAndSkipsDefault() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        AttributeConverterProvider throwing = new ThrowingProvider();
        AttributeConverterProvider defaultProvider = mock(AttributeConverterProvider.class);

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(throwing, defaultProvider)
                                                 .put("value", new CustomType("x"), type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + type);
        verifyNoInteractions(defaultProvider);
    }

    @Test
    @DisplayName("Default-first order blocks a later custom provider")
    void toMap_whenDefaultProviderBeforeCustom_throwsConverterNotFoundAndSkipsCustom() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingCustomProvider customProvider = new RecordingCustomProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(
                                                     AttributeConverterProvider.defaultProvider(),
                                                     customProvider)
                                                 .put("value", new CustomType("x"), type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
        assertThat(customProvider.requestedTypes()).isEmpty();
    }

    @Test
    @DisplayName("A configured default provider propagates its direct failure for an unsupported type")
    void toMap_whenUnsupportedTypeWithDefaultProvider_throwsConverterNotFound() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() -> defaultBuilder().put("value", new UnsupportedType("x"), type).build().toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("EnhancedType Object fails converter lookup")
    void toMap_whenObjectEnhancedType_throwsConverterNotFound() {
        EnhancedType<Object> type = EnhancedType.of(Object.class);

        assertThatThrownBy(() -> defaultBuilder().put("value", new Object(), type).build().toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("Class-based get rejects Object before provider lookup")
    void get_whenObjectClass_throwsIllegalArgumentExceptionToUseGetList() {
        EnhancedDocument document = defaultBuilder()
            .put("value", "text", EnhancedType.of(String.class))
            .build();

        assertThatThrownBy(() -> document.get("value", Object.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Values of type List are not supported by this API, please use the getList API instead");
    }

    @Test
    @DisplayName("Class-based put rejects Object before lazy conversion")
    void put_whenObjectClass_throwsIllegalArgumentExceptionToUsePutList() {
        assertThatThrownBy(() -> EnhancedDocument.builder().put("value", new Object(), Object.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Values of type List are not supported by this API, please use the putList API instead");
    }

    @Test
    @DisplayName("EnhancedDocument sends Collection to its list branch and reads an ArrayList")
    void putThenGet_whenStringCollection_storesLAndReadsArrayList() {
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);
        List<String> input = new ArrayList<>();
        input.add("a");

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        Collection<String> read = document.get("value", type);

        assertThat(map.get("value").hasL()).isTrue();
        assertThat(read).isInstanceOf(ArrayList.class).containsExactly("a");
    }

    @Test
    @DisplayName("EnhancedDocument sends Iterable to its list branch and reads an ArrayList")
    void putThenGet_whenStringIterable_storesLAndReadsArrayList() {
        EnhancedType<Iterable<String>> type = new EnhancedType<Iterable<String>>() { };
        Iterable<String> input = new ArrayList<>(Collections.singletonList("a"));

        EnhancedDocument document = defaultBuilder().put("value", input, type).build();
        Map<String, AttributeValue> map = document.toMap();
        Iterable<String> read = document.get("value", type);

        assertThat(map.get("value").hasL()).isTrue();
        assertThat(read).isInstanceOf(ArrayList.class);
        assertThat((List<String>) read).containsExactly("a");
    }

    @Test
    @DisplayName("A non-Collection Iterable fails in the document list converter")
    void toMap_whenNonCollectionIterable_throwsClassCastException() {
        EnhancedType<Iterable<String>> type = new EnhancedType<Iterable<String>>() { };
        Iterable<String> input = new NonCollectionIterable();

        assertThatThrownBy(() -> defaultBuilder().put("value", input, type).build().toMap())
            .isInstanceOf(ClassCastException.class)
            .hasMessageContaining(NonCollectionIterable.class.getName())
            .hasMessageContaining("cannot be cast");
    }

    @Test
    @DisplayName("Deque delegates to the default provider and remains unsupported")
    void toMap_whenStringDeque_throwsConverterNotFound() {
        EnhancedType<Deque<String>> type = EnhancedType.dequeOf(String.class);
        Deque<String> input = new ArrayDeque<>();
        input.add("a");

        assertThatThrownBy(() -> defaultBuilder().put("value", input, type).build().toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A concrete ArrayList token delegates to the default provider and remains unsupported")
    void toMap_whenConcreteArrayList_throwsConverterNotFound() {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() { };
        ArrayList<String> input = new ArrayList<>();
        input.add("a");

        assertThatThrownBy(() -> defaultBuilder().put("value", input, type).build().toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Document list recursion can use a custom member provider")
    void putThenGet_whenListOfUnsupportedTypeWithMemberProvider_storesLWithSMembersAndReadsArrayList() {
        EnhancedType<List<UnsupportedType>> type = EnhancedType.listOf(UnsupportedType.class);
        List<UnsupportedType> input = new ArrayList<>();
        input.add(new UnsupportedType("a"));
        input.add(new UnsupportedType("b"));
        AttributeConverterProvider memberProvider = new UnsupportedMemberProvider();

        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(memberProvider)
                                                    .put("value", input, type)
                                                    .build();
        Map<String, AttributeValue> map = document.toMap();
        List<UnsupportedType> read = document.get("value", type);

        assertThat(map.get("value").hasL()).isTrue();
        assertThat(map.get("value").l()).containsExactly(AttributeValue.fromS("a"), AttributeValue.fromS("b"));
        assertThat(read).isInstanceOf(ArrayList.class).isEqualTo(input);
    }

    @Test
    @DisplayName("Document map recursion can use a custom value provider")
    void putThenGet_whenMapOfUnsupportedTypeWithValueProvider_storesMWithSMemberAndReadsLinkedHashMap() {
        EnhancedType<Map<String, UnsupportedType>> type =
            EnhancedType.mapOf(String.class, UnsupportedType.class);
        Map<String, UnsupportedType> input = new LinkedHashMap<>();
        input.put("key", new UnsupportedType("a"));
        AttributeConverterProvider memberProvider = new UnsupportedMemberProvider();

        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeConverterProviders(memberProvider)
                                                    .put("value", input, type)
                                                    .build();
        Map<String, AttributeValue> map = document.toMap();
        Map<String, UnsupportedType> read = document.get("value", type);

        assertThat(map.get("value").hasM()).isTrue();
        assertThat(map.get("value").m().get("key")).isEqualTo(AttributeValue.fromS("a"));
        assertThat(read).isInstanceOf(LinkedHashMap.class).isEqualTo(input);
    }

    @Test
    @DisplayName("A document list of Object fails converter lookup for its member type")
    void toMap_whenListOfObject_throwsConverterNotFoundForMemberType() {
        EnhancedType<List<Object>> type = EnhancedType.listOf(Object.class);
        List<Object> input = new ArrayList<>();
        input.add("a");

        assertThatThrownBy(() -> defaultBuilder().put("value", input, type).build().toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("An explicit document null is present without converter lookup")
    void putNull_whenWithoutProvider_isPresentAndStoredAsNul() {
        EnhancedDocument document = EnhancedDocument.builder().putNull("value").build();

        assertThat(document.isPresent("value")).isTrue();
        assertThat(document.isNull("value")).isTrue();
        assertThat(document.toMap()).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("An absent document attribute differs from explicit null")
    void get_whenAbsentAttribute_isNotPresentAndReturnsNull() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        EnhancedDocument document = EnhancedDocument.builder().build();

        assertThat(document.isPresent("value")).isFalse();
        assertThat(document.isNull("value")).isFalse();
        assertThat(document.get("value", type)).isNull();
    }

    @Test
    @DisplayName("Typed put rejects a null Java value")
    void put_whenNullJavaValue_throwsNullPointerExceptionWithPutNullGuidance() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        assertThatThrownBy(() -> EnhancedDocument.builder().put("value", null, type))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Value for value must not be null. Use putNull API to insert a Null value");
    }

    @Test
    @DisplayName("Typed get maps explicit NULL to null while presence flags remain true")
    void get_whenExplicitNulAttribute_returnsNullWhilePresentAndNullFlagsRemainTrue() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        EnhancedDocument document = defaultBuilder()
            .put("value", AttributeValue.fromNul(true), EnhancedType.of(AttributeValue.class))
            .build();

        assertThat(document.get("value", type)).isNull();
        assertThat(document.isPresent("value")).isTrue();
        assertThat(document.isNull("value")).isTrue();
    }

    @Test
    @DisplayName("fromJson installs the shared default provider")
    void fromJson_whenJsonObject_installsDefaultProviderAndReadsString() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        EnhancedDocument document = EnhancedDocument.fromJson("{\"value\":\"text\"}");

        assertThat(document.attributeConverterProviders())
            .contains(AttributeConverterProvider.defaultProvider());
        assertThat(document.get("value", type)).isEqualTo("text");
    }

    @Test
    @DisplayName("fromAttributeValueMap installs the shared default provider")
    void fromAttributeValueMap_whenNumericAttribute_installsDefaultProviderAndReadsInteger() {
        EnhancedType<Integer> type = EnhancedType.of(Integer.class);
        Map<String, AttributeValue> input = Collections.singletonMap("value", AttributeValue.fromN("7"));

        EnhancedDocument document = EnhancedDocument.fromAttributeValueMap(input);

        assertThat(document.attributeConverterProviders())
            .contains(AttributeConverterProvider.defaultProvider());
        assertThat(document.get("value", type)).isEqualTo(7);
    }

    @Test
    @DisplayName("An AttributeValue escape path stores and returns the exact value without a provider")
    void put_whenAttributeValueMap_storesAndReturnsExactValueWithoutProvider() {
        AttributeValue input = AttributeValue.fromM(
            Collections.singletonMap("inner", AttributeValue.fromS("text")));

        EnhancedDocument document = EnhancedDocument.builder()
                                                    .attributeValueMap(Collections.singletonMap("value", input))
                                                    .build();

        assertThat(document.toMap().get("value")).isEqualTo(input);
        assertThat(document.get("value", EnhancedType.of(AttributeValue.class))).isEqualTo(input);
    }

    @Test
    @DisplayName("A null EnhancedType fails only when lazy conversion runs")
    void toMap_whenNullEnhancedType_buildSucceedsThenThrowsNullPointerException() {
        EnhancedDocument document = EnhancedDocument.builder()
                                                    .put("value", "text", (EnhancedType<String>) null)
                                                    .build();

        assertThat(document).isNotNull();
        assertThatThrownBy(document::toMap)
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("A typed concrete HashMap differs from the base-map helper and remains unsupported")
    void toMap_whenConcreteHashMap_throwsConverterNotFound() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() { };
        HashMap<String, Integer> input = new HashMap<>();
        input.put("count", 7);

        assertThatThrownBy(() -> defaultBuilder().put("value", input, type).build().toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Object lookup consults a custom provider before reporting a missing converter")
    void toMap_whenObjectTypeWithObjectProvider_throwsConverterNotFoundAfterInvokingProvider() {
        EnhancedType<Object> type = EnhancedType.of(Object.class);
        AttributeConverterProvider objectProvider = mock(AttributeConverterProvider.class);

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(
                                                     objectProvider,
                                                     AttributeConverterProvider.defaultProvider())
                                                 .put("value", new Object(), type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
        verify(objectProvider).converterFor(type);
    }

    @Test
    @DisplayName("A custom provider intercepts a schema-bearing Object token")
    void toMap_whenSchemaBearingObjectWithMatchingProvider_usesCustomConverter() {
        TableSchema<Object> objectSchema = mock(TableSchema.class);
        EnhancedType<Object> type = EnhancedType.documentOf(Object.class, objectSchema);
        AttributeConverterProvider objectProvider = mock(AttributeConverterProvider.class);
        AttributeConverter<Object> objectConverter = mock(AttributeConverter.class);
        Object value = new Object();

        when(objectProvider.converterFor(type)).thenReturn(objectConverter);
        when(objectConverter.transformFrom(value)).thenReturn(AttributeValue.fromS("custom"));

        Map<String, AttributeValue> result = EnhancedDocument.builder()
                                                             .attributeConverterProviders(
                                                                 objectProvider,
                                                                 AttributeConverterProvider.defaultProvider())
                                                             .put("value", value, type)
                                                             .build()
                                                             .toMap();

        assertThat(result).containsEntry("value", AttributeValue.fromS("custom"));
        verify(objectConverter).transformFrom(value);
        verifyNoInteractions(objectSchema);
    }

    @Test
    @DisplayName("A throwing list-member provider exception propagates")
    void toMap_whenListOfUnsupportedTypeWithThrowingMemberProvider_propagatesNestedProviderFailure() {
        EnhancedType<List<UnsupportedType>> type = EnhancedType.listOf(UnsupportedType.class);
        List<UnsupportedType> input = new ArrayList<>();
        input.add(new UnsupportedType("a"));
        AttributeConverterProvider provider = new NestedThrowingProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(provider)
                                                 .put("value", input, type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("A throwing map-value provider exception propagates")
    void toMap_whenMapOfUnsupportedTypeWithThrowingValueProvider_propagatesNestedProviderFailure() {
        EnhancedType<Map<String, UnsupportedType>> type =
            EnhancedType.mapOf(String.class, UnsupportedType.class);
        Map<String, UnsupportedType> input = Collections.singletonMap("key", new UnsupportedType("a"));
        AttributeConverterProvider provider = new NestedThrowingProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(provider)
                                                 .put("value", input, type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("A throwing set-member provider is not reached because default recursive lookup fails")
    void toMap_whenSetOfUnsupportedTypeWithThrowingMemberProvider_recordsOnlyOuterSetAndNamesMemberType() {
        EnhancedType<Set<UnsupportedType>> type = EnhancedType.setOf(UnsupportedType.class);
        Set<UnsupportedType> input = new LinkedHashSet<>();
        input.add(new UnsupportedType("a"));
        NestedThrowingProvider provider = new NestedThrowingProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(
                                                     provider,
                                                     AttributeConverterProvider.defaultProvider())
                                                 .put("value", input, type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
        assertThat(provider.requestedTypes()).containsExactly(type);
    }

    @Test
    @DisplayName("A null list-member provider result names the member type in the document error")
    void toMap_whenListOfUnsupportedTypeWithNullMemberProvider_throwsDocumentConverterNotFoundForMember() {
        EnhancedType<List<UnsupportedType>> type = EnhancedType.listOf(UnsupportedType.class);
        EnhancedType<UnsupportedType> memberType = EnhancedType.of(UnsupportedType.class);
        List<UnsupportedType> input = Collections.singletonList(new UnsupportedType("a"));
        AttributeConverterProvider provider = new ReturningNullProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(provider)
                                                 .put("value", input, type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(documentConverterNotFoundMessage(memberType));
    }

    @Test
    @DisplayName("A null map-value provider result names the value type in the document error")
    void toMap_whenMapOfUnsupportedTypeWithNullValueProvider_throwsDocumentConverterNotFoundForValue() {
        EnhancedType<Map<String, UnsupportedType>> type =
            EnhancedType.mapOf(String.class, UnsupportedType.class);
        EnhancedType<UnsupportedType> valueType = EnhancedType.of(UnsupportedType.class);
        Map<String, UnsupportedType> input = Collections.singletonMap("key", new UnsupportedType("a"));
        AttributeConverterProvider provider = new ReturningNullProvider();

        assertThatThrownBy(() -> EnhancedDocument.builder()
                                                 .attributeConverterProviders(provider)
                                                 .put("value", input, type)
                                                 .build()
                                                 .toMap())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(documentConverterNotFoundMessage(valueType));
    }

    private static EnhancedDocument.Builder defaultBuilder() {
        return EnhancedDocument.builder()
                               .attributeConverterProviders(AttributeConverterProvider.defaultProvider());
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

    enum TestEnum {
        OPEN,
        CLOSED
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

        String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UnsupportedType)) {
                return false;
            }
            UnsupportedType that = (UnsupportedType) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
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

    static final class NonCollectionIterable implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return Collections.singletonList("a").iterator();
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

    static final class UnsupportedStringConverter implements AttributeConverter<UnsupportedType> {
        @Override
        public AttributeValue transformFrom(UnsupportedType input) {
            return AttributeValue.fromS(input.value());
        }

        @Override
        public UnsupportedType transformTo(AttributeValue input) {
            return new UnsupportedType(input.s());
        }

        @Override
        public EnhancedType<UnsupportedType> type() {
            return EnhancedType.of(UnsupportedType.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    static final class RecordingCustomProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();
        private final CustomTypeConverter converter = new CustomTypeConverter("custom");

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

        CustomTypeConverter converter() {
            return converter;
        }
    }

    static final class UnsupportedMemberProvider implements AttributeConverterProvider {
        private final UnsupportedStringConverter converter = new UnsupportedStringConverter();

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            if (EnhancedType.of(UnsupportedType.class).equals(enhancedType)) {
                return (AttributeConverter<T>) converter;
            }
            return null;
        }
    }

    static final class ThrowingProvider implements AttributeConverterProvider {
        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            throw new IllegalArgumentException("Attribute converter provider failed while looking up " + enhancedType);
        }
    }

    static final class NestedThrowingProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(UnsupportedType.class).equals(enhancedType)) {
                throw new IllegalArgumentException("Attribute converter provider failed while looking up " + enhancedType);
            }
            return null;
        }

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }
    }

}
