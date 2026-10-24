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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UuidAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests converter selection and item conversion for static table schemas.
 * <p>
 * The tests cover scalar, enumeration, collection, map, and schema backed document attributes. They also verify null
 * handling, configured provider precedence, attribute converters, empty provider lists, recursive member lookup, and
 * unsupported attribute declarations during schema construction.
 */
public class StaticSchemaConverterTest {

    @Test
    @DisplayName("UUID attribute selects UuidAttributeConverter and round-trips the input")
    void converterForAttribute_whenUuidValue_selectsUuidConverterAndReadsEqualValue() {
        UUID input = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UuidItem item = new UuidItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<UUID> type = EnhancedType.of(UUID.class);

        StaticTableSchema<UuidItem> schema = StaticTableSchema.builder(UuidItem.class)
            .newItemSupplier(UuidItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(UuidItem::getId).setter(UuidItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(UuidItem::getValue).setter(UuidItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        UuidItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(UuidAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.getValue()).isEqualTo(input);
    }

    @Test
    @DisplayName("Map of String to Integer selects MapAttributeConverter and reads a LinkedHashMap")
    void converterForAttribute_whenStringIntegerMap_selectsMapConverterAndReadsLinkedHashMap() {
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", 2);
        MapItem item = new MapItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);

        StaticTableSchema<MapItem> schema = StaticTableSchema.builder(MapItem.class)
            .newItemSupplier(MapItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(MapItem::getId).setter(MapItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(MapItem::getValue).setter(MapItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        MapItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(MapAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(read.getValue()).isEqualTo(input).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    @DisplayName("Schema-bearing document attribute selects DocumentAttributeConverter and reconstructs")
    void converterForAttribute_whenDocumentType_selectsDocumentConverterAndReconstructs() {
        TableSchema<DocumentType> documentSchema = documentSchema();
        DocumentType input = new DocumentType();
        input.setName("doc");
        DocumentItem item = new DocumentItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, documentSchema);

        StaticTableSchema<DocumentItem> schema = StaticTableSchema.builder(DocumentItem.class)
            .newItemSupplier(DocumentItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(DocumentItem::getId).setter(DocumentItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(DocumentItem::getValue).setter(DocumentItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        DocumentItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(map.get("value").m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read.getValue().getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("Null string is stored as DynamoDB NULL when ignore-nulls is false")
    void itemToMap_whenNullStringIgnoreNullsFalse_containsNulValue() {
        StringItem item = new StringItem();
        item.setId("id-1");
        StaticTableSchema<StringItem> schema = stringSchema();

        Map<String, AttributeValue> map = schema.itemToMap(item, false);

        assertThat(map).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("Null string is omitted when ignore-nulls is true")
    void itemToMap_whenNullStringIgnoreNullsTrue_omitsValue() {
        StringItem item = new StringItem();
        item.setId("id-1");
        StaticTableSchema<StringItem> schema = stringSchema();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);

        assertThat(map).doesNotContainKey("value");
    }

    @Test
    @DisplayName("DynamoDB NULL is skipped on read and leaves the setter uncalled")
    void mapToItem_whenNulValue_doesNotCallSetterAndLeavesValueNull() {
        AtomicInteger setterCalls = new AtomicInteger();
        StaticTableSchema<StringItem> schema = StaticTableSchema.builder(StringItem.class)
            .newItemSupplier(StringItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(StringItem::getId).setter(StringItem::setId).tags(primaryPartitionKey()))
            .addAttribute(EnhancedType.of(String.class), a -> a.name("value")
                .getter(StringItem::getValue)
                .setter((item, value) -> {
                    setterCalls.incrementAndGet();
                    item.setValue(value);
                }))
            .build();
        Map<String, AttributeValue> map = new LinkedHashMap<>();
        map.put("id", AttributeValue.fromS("id-1"));
        map.put("value", AttributeValue.fromNul(true));

        StringItem read = schema.mapToItem(map);

        assertThat(read).isNotNull();
        assertThat(setterCalls.get()).isZero();
        assertThat(read.getValue()).isNull();
    }

    @Test
    @DisplayName("Unconverted Object attribute fails converter lookup")
    void build_whenUnconvertedObject_throwsConverterNotFound() {
        EnhancedType<Object> type = EnhancedType.of(Object.class);

        assertThatThrownBy(() -> StaticTableSchema.builder(ObjectItem.class)
            .newItemSupplier(ObjectItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(ObjectItem::getId).setter(ObjectItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(ObjectItem::getValue).setter(ObjectItem::setValue))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Unsupported attribute fails lookup with the enclosing type in the message")
    void build_whenUnsupportedType_throwsConverterNotFound() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() -> StaticTableSchema.builder(UnsupportedItem.class)
            .newItemSupplier(UnsupportedItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(UnsupportedItem::getId).setter(UnsupportedItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(UnsupportedItem::getValue).setter(UnsupportedItem::setValue))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Custom provider before default selects CustomTypeConverter")
    void build_whenRecordingCustomBeforeDefault_selectsCustomTypeConverter() {
        CustomType input = new CustomType("x");
        CustomTypeItem item = new CustomTypeItem();
        item.setValue(input);
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingCustomProvider custom = new RecordingCustomProvider();

        StaticTableSchema<CustomTypeItem> schema = StaticTableSchema.builder(CustomTypeItem.class)
            .newItemSupplier(CustomTypeItem::new)
            .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
            .addAttribute(type, a -> a.name("value")
                .getter(CustomTypeItem::getValue).setter(CustomTypeItem::setValue).tags(primaryPartitionKey()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        CustomTypeItem read = schema.mapToItem(map);

        assertThat(custom.requestedTypes()).hasSize(2);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(CustomTypeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.getValue()).isEqualTo(input);
    }

    @Test
    @DisplayName("Null-returning provider falls back to the default string converter")
    void build_whenReturningNullBeforeDefault_selectsStringAttributeConverter() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        ReturningNullProvider custom = new ReturningNullProvider();

        StaticTableSchema<StringItem> schema = StaticTableSchema.builder(StringItem.class)
            .newItemSupplier(StringItem::new)
            .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
            .addAttribute(type, a -> a.name("value")
                .getter(StringItem::getValue).setter(StringItem::setValue).tags(primaryPartitionKey()))
            .build();

        assertThat(custom.requestedTypes()).hasSize(1);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(StringAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
    }

    @Test
    @DisplayName("Default provider before custom blocks later custom fallback")
    void build_whenDefaultBeforeRecordingCustom_throwsAndDoesNotInvokeCustom() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingCustomProvider custom = new RecordingCustomProvider();

        assertThatThrownBy(() -> StaticTableSchema.builder(CustomTypeItem.class)
            .newItemSupplier(CustomTypeItem::new)
            .attributeConverterProviders(DefaultAttributeConverterProvider.create(), custom)
            .addAttribute(type, a -> a.name("value")
                .getter(CustomTypeItem::getValue).setter(CustomTypeItem::setValue).tags(primaryPartitionKey()))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
        assertThat(custom.requestedTypes()).isEmpty();
    }

    @Test
    @DisplayName("Throwing provider exception is propagated without consulting the default")
    void build_whenThrowingProviderBeforeDefault_propagatesProviderFailure() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingDefaultProvider defaultProvider = new RecordingDefaultProvider();

        assertThatThrownBy(() -> StaticTableSchema.builder(CustomTypeItem.class)
            .newItemSupplier(CustomTypeItem::new)
            .attributeConverterProviders(new ThrowingProvider(), defaultProvider)
            .addAttribute(type, a -> a.name("value")
                .getter(CustomTypeItem::getValue).setter(CustomTypeItem::setValue).tags(primaryPartitionKey()))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + type);
        assertThat(defaultProvider.requestedTypes()).isEmpty();
    }

    @Test
    @DisplayName("Static mutable ObjectProvider before default selects ObjectStringConverter")
    void build_whenObjectProviderBeforeDefault_selectsObjectStringConverter() {
        ObjectItem item = new ObjectItem();
        item.setValue(new Object());
        EnhancedType<Object> type = EnhancedType.of(Object.class);
        ObjectProvider provider = new ObjectProvider();

        StaticTableSchema<ObjectItem> schema = StaticTableSchema.builder(ObjectItem.class)
            .newItemSupplier(ObjectItem::new)
            .attributeConverterProviders(provider, DefaultAttributeConverterProvider.create())
            .addAttribute(type, a -> a.name("value")
                .getter(ObjectItem::getValue).setter(ObjectItem::setValue).tags(primaryPartitionKey()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ObjectItem read = schema.mapToItem(map);

        int objectRequestCount = 0;
        for (EnhancedType<?> requestedType : provider.requestedTypes()) {
            if (EnhancedType.of(Object.class).equals(requestedType)) {
                objectRequestCount++;
            }
        }
        assertThat(objectRequestCount).isEqualTo(2);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(ObjectStringConverter.class);
        assertThat(read.getValue()).isEqualTo("custom");
    }

    @Test
    @DisplayName("Custom provider intercepts a captured HashMap token before the default")
    void build_whenHashMapProviderBeforeDefault_selectsHashMapConverter() {
        HashMap<String, Integer> input = new HashMap<>();
        input.put("a", 1);
        HashMapItem item = new HashMapItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };
        ExactTypeProvider custom = new ExactTypeProvider(type, new HashMapConverter());
        AtomicReference<Object> assigned = new AtomicReference<>();

        StaticTableSchema<HashMapItem> schema = StaticTableSchema.builder(HashMapItem.class)
            .newItemSupplier(HashMapItem::new)
            .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(HashMapItem::getId).setter(HashMapItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(HashMapItem::getValue)
                .setter((model, value) -> {
                    assigned.set(value);
                    model.setValue(value);
                }))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        schema.mapToItem(map);

        assertThat(custom.requestCountFor(type)).isEqualTo(2);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(HashMapConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(assigned.get()).isInstanceOf(HashMap.class);
    }

    @Test
    @DisplayName("Attribute-level converter intercepts a schema-bearing Object token")
    void build_whenDocumentObjectWithAttributeConverter_skipsProvider() {
        ObjectItem item = new ObjectItem();
        item.setId("id-1");
        item.setValue(new Object());
        TableSchema<Object> objectSchema =
            StaticTableSchema.builder(Object.class).newItemSupplier(Object::new).build();
        EnhancedType<Object> type = EnhancedType.documentOf(Object.class, objectSchema);
        ReturningNullProvider provider = new ReturningNullProvider();
        AtomicReference<Object> assigned = new AtomicReference<>();

        StaticTableSchema<ObjectItem> schema = StaticTableSchema.builder(ObjectItem.class)
            .newItemSupplier(ObjectItem::new)
            .attributeConverterProviders(provider, DefaultAttributeConverterProvider.create())
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(ObjectItem::getId).setter(ObjectItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(ObjectItem::getValue)
                .setter((model, value) -> {
                    assigned.set(value);
                    model.setValue(value);
                })
                .attributeConverter(new ObjectStringConverter()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        schema.mapToItem(map);

        assertThat(provider.requestedTypes()).doesNotContain(type);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(ObjectStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(assigned.get()).isEqualTo("custom");
    }

    @Test
    @DisplayName("Attribute-level ObjectStringConverter intercepts Object without consulting the provider")
    void converterForAttribute_whenObjectStringConverter_skipsProviderAndReadsConverterValue() {
        ObjectItem item = new ObjectItem();
        item.setValue(new Object());
        EnhancedType<Object> type = EnhancedType.of(Object.class);
        ObjectProvider provider = new ObjectProvider();

        StaticTableSchema<ObjectItem> schema = StaticTableSchema.builder(ObjectItem.class)
            .newItemSupplier(ObjectItem::new)
            .attributeConverterProviders(provider, DefaultAttributeConverterProvider.create())
            .addAttribute(type, a -> a.name("value")
                .getter(ObjectItem::getValue).setter(ObjectItem::setValue).tags(primaryPartitionKey())
                .attributeConverter(new ObjectStringConverter()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ObjectItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(ObjectStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(provider.requestedTypes()).doesNotContain(type);
        assertThat(read.getValue()).isEqualTo("custom");
    }

    @Test
    @DisplayName("Attribute-level UnsupportedStringConverter intercepts an unsupported class")
    void converterForAttribute_whenUnsupportedStringConverter_readsEqualValue() {
        UnsupportedType input = new UnsupportedType();
        UnsupportedItem item = new UnsupportedItem();
        item.setValue(input);
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        StaticTableSchema<UnsupportedItem> schema = StaticTableSchema.builder(UnsupportedItem.class)
            .newItemSupplier(UnsupportedItem::new)
            .addAttribute(type, a -> a.name("value")
                .getter(UnsupportedItem::getValue).setter(UnsupportedItem::setValue)
                .tags(primaryPartitionKey())
                .attributeConverter(new UnsupportedStringConverter()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        UnsupportedItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(UnsupportedStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.getValue()).isEqualTo(input);
    }

    @Test
    @DisplayName("Attribute-level HashMapConverter intercepts a captured HashMap type")
    void converterForAttribute_whenHashMapConverter_setterReceivesHashMap() {
        HashMap<String, Integer> input = new HashMap<>();
        input.put("a", 1);
        HashMapItem item = new HashMapItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };
        AtomicReference<Object> assigned = new AtomicReference<>();

        StaticTableSchema<HashMapItem> schema = StaticTableSchema.builder(HashMapItem.class)
            .newItemSupplier(HashMapItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(HashMapItem::getId).setter(HashMapItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(HashMapItem::getValue)
                .setter((model, value) -> {
                    assigned.set(value);
                    model.setValue(value);
                })
                .attributeConverter(new HashMapConverter()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(HashMapConverter.class);
        assertThat(schema.converterForAttribute("value").type()).isEqualTo(type);
        assertThat(assigned.get()).isInstanceOf(HashMap.class);
    }

    @Test
    @DisplayName("Empty providers fail without an attribute converter")
    void build_whenEmptyProvidersWithoutAttributeConverter_throwsNullPointerException() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        assertThatThrownBy(() -> StaticTableSchema.builder(StringItem.class)
            .newItemSupplier(StringItem::new)
            .attributeConverterProviders(Collections.emptyList())
            .addAttribute(type, a -> a.name("value")
                .getter(StringItem::getValue).setter(StringItem::setValue).tags(primaryPartitionKey()))
            .build())
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Attribute-level CustomStringConverter survives empty providers")
    void itemToMap_whenEmptyProvidersWithCustomStringConverter_writesCustomText() {
        StringItem item = new StringItem();
        item.setValue("text");
        EnhancedType<String> type = EnhancedType.of(String.class);

        StaticTableSchema<StringItem> schema = StaticTableSchema.builder(StringItem.class)
            .newItemSupplier(StringItem::new)
            .attributeConverterProviders(Collections.emptyList())
            .addAttribute(type, a -> a.name("value")
                .getter(StringItem::getValue).setter(StringItem::setValue).tags(primaryPartitionKey())
                .attributeConverter(new CustomStringConverter()))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        StringItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(CustomStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("custom:text");
        assertThat(read.getValue()).isEqualTo("text");
    }

    @Test
    @DisplayName("Unconverted captured HashMap fails lookup before assignment")
    void build_whenUnconvertedHashMap_throwsConverterNotFound() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };

        assertThatThrownBy(() -> StaticTableSchema.builder(HashMapItem.class)
            .newItemSupplier(HashMapItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(HashMapItem::getId).setter(HashMapItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(HashMapItem::getValue).setter(HashMapItem::setValue))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Custom member converter is not consulted for an enclosing map token")
    void build_whenMapOfUnsupportedWithMemberProvider_throwsConverterNotFoundForMapType() {
        EnhancedType<Map<String, UnsupportedType>> type =
            EnhancedType.mapOf(String.class, UnsupportedType.class);
        UnsupportedMemberProvider custom = new UnsupportedMemberProvider();

        assertThatThrownBy(() -> StaticTableSchema.builder(UnsupportedMapItem.class)
            .newItemSupplier(UnsupportedMapItem::new)
            .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
            .addAttribute(type, a -> a.name("value")
                .getter(UnsupportedMapItem::getValue).setter(UnsupportedMapItem::setValue))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
        assertThat(custom.requestedTypes()).containsExactly(type);
    }

    @Test
    @DisplayName("Collection of String selects SetAttributeConverter and reads a LinkedHashSet")
    void converterForAttribute_whenStringCollection_selectsSetConverterAndReadsLinkedHashSet() {
        Collection<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        CollectionItem item = new CollectionItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);

        StaticTableSchema<CollectionItem> schema = StaticTableSchema.builder(CollectionItem.class)
            .newItemSupplier(CollectionItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(CollectionItem::getId).setter(CollectionItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(CollectionItem::getValue).setter(CollectionItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        CollectionItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.getValue()).isInstanceOf(LinkedHashSet.class)
                                   .containsExactly("a", "b");
    }

    @Test
    @DisplayName("List of String selects ListAttributeConverter and reads an ArrayList")
    void converterForAttribute_whenStringList_selectsListConverterAndReadsArrayList() {
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("a");
        ListItem item = new ListItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);

        StaticTableSchema<ListItem> schema = StaticTableSchema.builder(ListItem.class)
            .newItemSupplier(ListItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(ListItem::getId).setter(ListItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(ListItem::getValue).setter(ListItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ListItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(ListAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(read.getValue()).isInstanceOf(ArrayList.class)
                                   .containsExactly("a", "b", "a");
    }

    @Test
    @DisplayName("Set of String selects SetAttributeConverter and reads a LinkedHashSet")
    void converterForAttribute_whenStringSet_selectsSetConverterAndReadsLinkedHashSet() {
        Set<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        SetItem item = new SetItem();
        item.setId("id-1");
        item.setValue(input);
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);

        StaticTableSchema<SetItem> schema = StaticTableSchema.builder(SetItem.class)
            .newItemSupplier(SetItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(SetItem::getId).setter(SetItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(SetItem::getValue).setter(SetItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        SetItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.getValue()).isInstanceOf(LinkedHashSet.class)
                                   .containsExactly("a", "b");
    }

    @Test
    @DisplayName("Enum attribute selects EnumAttributeConverter and round-trips")
    void converterForAttribute_whenEnumValue_selectsEnumConverterAndReadsEqualValue() {
        EnumItem item = new EnumItem();
        item.setId("id-1");
        item.setValue(TestEnum.OPEN);
        EnhancedType<TestEnum> type = EnhancedType.of(TestEnum.class);

        StaticTableSchema<EnumItem> schema = StaticTableSchema.builder(EnumItem.class)
            .newItemSupplier(EnumItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(EnumItem::getId).setter(EnumItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(EnumItem::getValue).setter(EnumItem::setValue))
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        EnumItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(EnumAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("OPEN");
        assertThat(read.getValue()).isEqualTo(TestEnum.OPEN);
    }

    @Test
    @DisplayName("Unconverted captured HashSet fails lookup before assignment")
    void build_whenUnconvertedHashSet_throwsConverterNotFound() {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() {
        };

        assertThatThrownBy(() -> StaticTableSchema.builder(HashSetItem.class)
            .newItemSupplier(HashSetItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(HashSetItem::getId).setter(HashSetItem::setId).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(HashSetItem::getValue).setter(HashSetItem::setValue))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    private static TableSchema<DocumentType> documentSchema() {
        return StaticTableSchema.builder(DocumentType.class)
                                .newItemSupplier(DocumentType::new)
                                .addAttribute(String.class, a -> a.name("name")
                                                                  .getter(DocumentType::getName)
                                                                  .setter(DocumentType::setName))
                                .build();
    }

    private static StaticTableSchema<StringItem> stringSchema() {
        return StaticTableSchema.builder(StringItem.class)
            .newItemSupplier(StringItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(StringItem::getId).setter(StringItem::setId).tags(primaryPartitionKey()))
            .addAttribute(EnhancedType.of(String.class), a -> a.name("value")
                .getter(StringItem::getValue).setter(StringItem::setValue))
            .build();
    }

    static final class UuidItem {
        private String id;
        private UUID value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public UUID getValue() {
            return value;
        }

        public void setValue(UUID value) {
            this.value = value;
        }
    }

    static final class MapItem {
        private String id;
        private Map<String, Integer> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getValue() {
            return value;
        }

        public void setValue(Map<String, Integer> value) {
            this.value = value;
        }
    }

    static final class DocumentItem {
        private String id;
        private DocumentType value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public DocumentType getValue() {
            return value;
        }

        public void setValue(DocumentType value) {
            this.value = value;
        }
    }

    static final class StringItem {
        private String id;
        private String value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static final class ObjectItem {
        private String id;
        private Object value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    static final class UnsupportedItem {
        private String id;
        private UnsupportedType value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public UnsupportedType getValue() {
            return value;
        }

        public void setValue(UnsupportedType value) {
            this.value = value;
        }
    }

    static final class CustomTypeItem {
        private CustomType value;

        public CustomType getValue() {
            return value;
        }

        public void setValue(CustomType value) {
            this.value = value;
        }
    }

    static final class HashMapItem {
        private String id;
        private HashMap<String, Integer> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public HashMap<String, Integer> getValue() {
            return value;
        }

        public void setValue(HashMap<String, Integer> value) {
            this.value = value;
        }
    }

    static final class UnsupportedMapItem {
        private String id;
        private Map<String, UnsupportedType> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, UnsupportedType> getValue() {
            return value;
        }

        public void setValue(Map<String, UnsupportedType> value) {
            this.value = value;
        }
    }

    static final class CollectionItem {
        private String id;
        private Collection<String> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Collection<String> getValue() {
            return value;
        }

        public void setValue(Collection<String> value) {
            this.value = value;
        }
    }

    static final class ListItem {
        private String id;
        private List<String> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getValue() {
            return value;
        }

        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    static final class SetItem {
        private String id;
        private Set<String> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Set<String> getValue() {
            return value;
        }

        public void setValue(Set<String> value) {
            this.value = value;
        }
    }

    static final class EnumItem {
        private String id;
        private TestEnum value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public TestEnum getValue() {
            return value;
        }

        public void setValue(TestEnum value) {
            this.value = value;
        }
    }

    static final class HashSetItem {
        private String id;
        private HashSet<String> value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public HashSet<String> getValue() {
            return value;
        }

        public void setValue(HashSet<String> value) {
            this.value = value;
        }
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CustomType)) {
                return false;
            }
            CustomType that = (CustomType) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    static final class UnsupportedType {
        @Override
        public boolean equals(Object o) {
            return o instanceof UnsupportedType;
        }

        @Override
        public int hashCode() {
            return 1;
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

    public static final class RecordingCustomProvider implements AttributeConverterProvider {
        private static final ThreadLocal<RecordingCustomProvider> CURRENT = new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public RecordingCustomProvider() {
            CURRENT.set(this);
        }

        public static RecordingCustomProvider current() {
            return CURRENT.get();
        }

        public List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(CustomType.class).equals(enhancedType)) {
                return (AttributeConverter<T>) new CustomTypeConverter();
            }
            return null;
        }
    }

    public static final class ReturningNullProvider implements AttributeConverterProvider {
        private static final ThreadLocal<ReturningNullProvider> CURRENT = new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public ReturningNullProvider() {
            CURRENT.set(this);
        }

        public static ReturningNullProvider current() {
            return CURRENT.get();
        }

        public List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            return null;
        }
    }

    public static final class ThrowingProvider implements AttributeConverterProvider {
        public ThrowingProvider() {
        }

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            throw new IllegalArgumentException("Attribute converter provider failed while looking up " + enhancedType);
        }
    }

    public static final class ObjectProvider implements AttributeConverterProvider {
        private static final ThreadLocal<ObjectProvider> CURRENT = new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public ObjectProvider() {
            CURRENT.set(this);
        }

        public static ObjectProvider current() {
            return CURRENT.get();
        }

        public List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(Object.class).equals(enhancedType)) {
                return (AttributeConverter<T>) new ObjectStringConverter();
            }
            return null;
        }
    }

    static final class ExactTypeProvider implements AttributeConverterProvider {
        private final EnhancedType<?> target;
        private final AttributeConverter<?> converter;
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        ExactTypeProvider(EnhancedType<?> target, AttributeConverter<?> converter) {
            this.target = target;
            this.converter = converter;
        }

        int requestCountFor(EnhancedType<?> type) {
            int count = 0;
            for (EnhancedType<?> requestedType : requestedTypes) {
                if (type.equals(requestedType)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (target.equals(enhancedType)) {
                return (AttributeConverter<T>) converter;
            }
            return null;
        }
    }

    static final class RecordingDefaultProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();
        private final AttributeConverterProvider delegate = DefaultAttributeConverterProvider.create();

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            return delegate.converterFor(enhancedType);
        }
    }

    static final class UnsupportedMemberProvider implements AttributeConverterProvider {
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        List<EnhancedType<?>> requestedTypes() {
            return requestedTypes;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            requestedTypes.add(enhancedType);
            if (EnhancedType.of(UnsupportedType.class).equals(enhancedType)) {
                return (AttributeConverter<T>) new UnsupportedStringConverter();
            }
            return null;
        }
    }

    public static final class CustomTypeConverter implements AttributeConverter<CustomType> {

        @Override
        public AttributeValue transformFrom(CustomType input) {
            return AttributeValue.fromS("custom:" + input.value());
        }

        @Override
        public CustomType transformTo(AttributeValue input) {
            return new CustomType(input.s().substring("custom:".length()));
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

    public static final class CustomStringConverter implements AttributeConverter<String> {

        @Override
        public AttributeValue transformFrom(String input) {
            return AttributeValue.fromS("custom:" + input);
        }

        @Override
        public String transformTo(AttributeValue input) {
            String stored = input.s();
            return stored.startsWith("custom:") ? stored.substring("custom:".length()) : stored;
        }

        @Override
        public EnhancedType<String> type() {
            return EnhancedType.of(String.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    public static final class ObjectStringConverter implements AttributeConverter<Object> {
        public ObjectStringConverter() {
        }

        @Override
        public AttributeValue transformFrom(Object input) {
            return AttributeValue.fromS("custom");
        }

        @Override
        public Object transformTo(AttributeValue input) {
            return input.s();
        }

        @Override
        public EnhancedType<Object> type() {
            return EnhancedType.of(Object.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }

    public static final class UnsupportedStringConverter implements AttributeConverter<UnsupportedType> {

        @Override
        public AttributeValue transformFrom(UnsupportedType input) {
            return AttributeValue.fromS("custom");
        }

        @Override
        public UnsupportedType transformTo(AttributeValue input) {
            return new UnsupportedType();
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

    public static final class HashMapConverter implements AttributeConverter<HashMap<String, Integer>> {
        private static final EnhancedType<HashMap<String, Integer>> TYPE =
            new EnhancedType<HashMap<String, Integer>>() {
            };

        public HashMapConverter() {
        }

        @Override
        public AttributeValue transformFrom(HashMap<String, Integer> input) {
            Map<String, AttributeValue> values = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : input.entrySet()) {
                values.put(entry.getKey(), AttributeValue.fromN(Integer.toString(entry.getValue())));
            }
            return AttributeValue.fromM(values);
        }

        @Override
        public HashMap<String, Integer> transformTo(AttributeValue input) {
            HashMap<String, Integer> result = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : input.m().entrySet()) {
                result.put(entry.getKey(), Integer.valueOf(entry.getValue().n()));
            }
            return result;
        }

        @Override
        public EnhancedType<HashMap<String, Integer>> type() {
            return TYPE;
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.M;
        }
    }

}
