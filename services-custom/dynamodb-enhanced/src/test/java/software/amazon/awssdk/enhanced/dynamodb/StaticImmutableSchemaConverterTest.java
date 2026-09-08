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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests converter selection and item conversion for static immutable table schemas.
 * <p>
 * The tests cover scalar, enumeration, collection, map, and schema backed document attributes. They also verify null
 * handling, configured provider precedence, attribute converters, empty provider lists, recursive member lookup, and
 * unsupported attribute declarations during schema construction.
 */
public class StaticImmutableSchemaConverterTest {

    @Test
    @DisplayName("Instant attribute selects InstantAsStringAttributeConverter and round-trips the input")
    void converterForAttribute_whenInstantValue_selectsInstantConverterAndRebuildsEqualValue() {
        Instant input = Instant.parse("2020-01-02T03:04:05Z");
        InstantItem item = InstantItem.builder().id("id-1").value(input).build();
        EnhancedType<Instant> type = EnhancedType.of(Instant.class);

        StaticImmutableTableSchema<InstantItem, InstantItem.Builder> schema =
            StaticImmutableTableSchema.builder(InstantItem.class, InstantItem.Builder.class)
                .newItemBuilder(InstantItem.Builder::new, InstantItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(InstantItem::id).setter(InstantItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(InstantItem::value).setter(InstantItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        InstantItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(InstantAsStringAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.value()).isEqualTo(input);
    }

    @Test
    @DisplayName("List of String selects ListAttributeConverter and rebuilds an equal ArrayList")
    void converterForAttribute_whenStringList_selectsListConverterAndRebuildsArrayList() {
        List<String> input = new ArrayList<>(Arrays.asList("a", "a", "b"));
        ListItem item = ListItem.builder().id("id-1").value(input).build();
        EnhancedType<List<String>> type = EnhancedType.listOf(String.class);

        StaticImmutableTableSchema<ListItem, ListItem.Builder> schema =
            StaticImmutableTableSchema.builder(ListItem.class, ListItem.Builder.class)
                .newItemBuilder(ListItem.Builder::new, ListItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(ListItem::id).setter(ListItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(ListItem::value).setter(ListItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ListItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(ListAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(read.value()).isEqualTo(input).isInstanceOf(ArrayList.class);
    }

    @Test
    @DisplayName("Schema-bearing document attribute selects DocumentAttributeConverter and reconstructs")
    void converterForAttribute_whenDocumentType_selectsDocumentConverterAndReconstructs() {
        TableSchema<DocumentType> documentSchema = documentSchema();
        DocumentType input = new DocumentType();
        input.setName("doc");
        DocumentItem item = DocumentItem.builder().id("id-1").value(input).build();
        EnhancedType<DocumentType> type = EnhancedType.documentOf(DocumentType.class, documentSchema);

        StaticImmutableTableSchema<DocumentItem, DocumentItem.Builder> schema =
            StaticImmutableTableSchema.builder(DocumentItem.class, DocumentItem.Builder.class)
                .newItemBuilder(DocumentItem.Builder::new, DocumentItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(DocumentItem::id).setter(DocumentItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(DocumentItem::value).setter(DocumentItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        DocumentItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(map.get("value").m()).containsEntry("name", AttributeValue.fromS("doc"));
        assertThat(read.value().getName()).isEqualTo("doc");
    }

    @Test
    @DisplayName("Null string is stored as DynamoDB NULL when ignore-nulls is false")
    void itemToMap_whenNullStringIgnoreNullsFalse_containsNulValue() {
        StringItem item = StringItem.builder().id("id-1").build();

        Map<String, AttributeValue> map = stringSchema().itemToMap(item, false);

        assertThat(map).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("Null string is omitted when ignore-nulls is true")
    void itemToMap_whenNullStringIgnoreNullsTrue_omitsValue() {
        StringItem item = StringItem.builder().id("id-1").build();

        Map<String, AttributeValue> map = stringSchema().itemToMap(item, true);

        assertThat(map).doesNotContainKey("value");
    }

    @Test
    @DisplayName("DynamoDB NULL is skipped on read and leaves the builder method uncalled")
    void mapToItem_whenNulValue_doesNotCallBuilderMethodAndLeavesValueNull() {
        AtomicInteger valueCalls = new AtomicInteger();
        StaticImmutableTableSchema<StringItem, StringItem.Builder> schema =
            StaticImmutableTableSchema.builder(StringItem.class, StringItem.Builder.class)
                .newItemBuilder(StringItem.Builder::new, StringItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(StringItem::id).setter(StringItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(EnhancedType.of(String.class), a -> a.name("value")
                    .getter(StringItem::value)
                    .setter((builder, value) -> {
                        valueCalls.incrementAndGet();
                        builder.value(value);
                    }))
                .build();
        Map<String, AttributeValue> map = new LinkedHashMap<>();
        map.put("id", AttributeValue.fromS("id-1"));
        map.put("value", AttributeValue.fromNul(true));

        StringItem read = schema.mapToItem(map);

        assertThat(read).isNotNull();
        assertThat(valueCalls.get()).isZero();
        assertThat(read.value()).isNull();
    }

    @Test
    @DisplayName("Unconverted Object attribute fails converter lookup")
    void build_whenUnconvertedObject_throwsConverterNotFound() {
        EnhancedType<Object> type = EnhancedType.of(Object.class);

        assertThatThrownBy(() -> StaticImmutableTableSchema.builder(ObjectItem.class, ObjectItem.Builder.class)
            .newItemBuilder(ObjectItem.Builder::new, ObjectItem.Builder::build)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(ObjectItem::id).setter(ObjectItem.Builder::id).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(ObjectItem::value).setter(ObjectItem.Builder::value))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Unsupported attribute fails lookup with the enclosing type in the message")
    void build_whenUnsupportedType_throwsConverterNotFound() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() ->
            StaticImmutableTableSchema.builder(UnsupportedItem.class, UnsupportedItem.Builder.class)
                .newItemBuilder(UnsupportedItem.Builder::new, UnsupportedItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(UnsupportedItem::id).setter(UnsupportedItem.Builder::id)
                    .tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(UnsupportedItem::value).setter(UnsupportedItem.Builder::value))
                .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Custom provider before default selects CustomTypeConverter")
    void build_whenRecordingCustomBeforeDefault_selectsCustomTypeConverter() {
        CustomType input = new CustomType("x");
        CustomTypeItem item = CustomTypeItem.builder().value(input).build();
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        RecordingCustomProvider custom = new RecordingCustomProvider();

        StaticImmutableTableSchema<CustomTypeItem, CustomTypeItem.Builder> schema =
            StaticImmutableTableSchema.builder(CustomTypeItem.class, CustomTypeItem.Builder.class)
                .newItemBuilder(CustomTypeItem.Builder::new, CustomTypeItem.Builder::build)
                .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
                .addAttribute(type, a -> a.name("value")
                    .getter(CustomTypeItem::value).setter(CustomTypeItem.Builder::value)
                    .tags(primaryPartitionKey()))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        CustomTypeItem read = schema.mapToItem(map);

        assertThat(custom.requestedTypes()).hasSize(2);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(CustomTypeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.value()).isEqualTo(input);
    }

    @Test
    @DisplayName("Null-returning provider falls back to the default string converter")
    void build_whenReturningNullBeforeDefault_selectsStringAttributeConverter() {
        EnhancedType<String> type = EnhancedType.of(String.class);
        ReturningNullProvider custom = new ReturningNullProvider();

        StaticImmutableTableSchema<StringItem, StringItem.Builder> schema =
            StaticImmutableTableSchema.builder(StringItem.class, StringItem.Builder.class)
                .newItemBuilder(StringItem.Builder::new, StringItem.Builder::build)
                .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
                .addAttribute(type, a -> a.name("value")
                    .getter(StringItem::value).setter(StringItem.Builder::value).tags(primaryPartitionKey()))
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

        assertThatThrownBy(() ->
            StaticImmutableTableSchema.builder(CustomTypeItem.class, CustomTypeItem.Builder.class)
                .newItemBuilder(CustomTypeItem.Builder::new, CustomTypeItem.Builder::build)
                .attributeConverterProviders(DefaultAttributeConverterProvider.create(), custom)
                .addAttribute(type, a -> a.name("value")
                    .getter(CustomTypeItem::value).setter(CustomTypeItem.Builder::value)
                    .tags(primaryPartitionKey()))
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

        assertThatThrownBy(() ->
            StaticImmutableTableSchema.builder(CustomTypeItem.class, CustomTypeItem.Builder.class)
                .newItemBuilder(CustomTypeItem.Builder::new, CustomTypeItem.Builder::build)
                .attributeConverterProviders(new ThrowingProvider(), defaultProvider)
                .addAttribute(type, a -> a.name("value")
                    .getter(CustomTypeItem::value).setter(CustomTypeItem.Builder::value)
                    .tags(primaryPartitionKey()))
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + type);
        assertThat(defaultProvider.requestedTypes()).isEmpty();
    }

    @Test
    @DisplayName("Static immutable ObjectProvider before default selects ObjectStringConverter")
    void build_whenObjectProviderBeforeDefault_selectsObjectStringConverter() {
        ObjectItem item = ObjectItem.builder().value(new Object()).build();
        EnhancedType<Object> type = EnhancedType.of(Object.class);
        ObjectProvider provider = new ObjectProvider();

        StaticImmutableTableSchema<ObjectItem, ObjectItem.Builder> schema =
            StaticImmutableTableSchema.builder(ObjectItem.class, ObjectItem.Builder.class)
                .newItemBuilder(ObjectItem.Builder::new, ObjectItem.Builder::build)
                .attributeConverterProviders(provider, DefaultAttributeConverterProvider.create())
                .addAttribute(type, a -> a.name("value")
                    .getter(ObjectItem::value).setter(ObjectItem.Builder::value)
                    .tags(primaryPartitionKey()))
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
        assertThat(read.value()).isEqualTo("custom");
    }

    @Test
    @DisplayName("Attribute-level ObjectStringConverter intercepts Object without consulting the provider")
    void converterForAttribute_whenObjectStringConverter_skipsProviderAndBuilderReceivesConverterValue() {
        ObjectItem item = ObjectItem.builder().value(new Object()).build();
        EnhancedType<Object> type = EnhancedType.of(Object.class);
        ObjectProvider provider = new ObjectProvider();

        StaticImmutableTableSchema<ObjectItem, ObjectItem.Builder> schema =
            StaticImmutableTableSchema.builder(ObjectItem.class, ObjectItem.Builder.class)
                .newItemBuilder(ObjectItem.Builder::new, ObjectItem.Builder::build)
                .attributeConverterProviders(provider, DefaultAttributeConverterProvider.create())
                .addAttribute(type, a -> a.name("value")
                    .getter(ObjectItem::value).setter(ObjectItem.Builder::value).tags(primaryPartitionKey())
                    .attributeConverter(new ObjectStringConverter()))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ObjectItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(ObjectStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(provider.requestedTypes()).doesNotContain(type);
        assertThat(read.value()).isEqualTo("custom");
    }

    @Test
    @DisplayName("Attribute-level UnsupportedStringConverter intercepts an unsupported class")
    void converterForAttribute_whenUnsupportedStringConverter_rebuildsEqualValue() {
        UnsupportedType input = new UnsupportedType();
        UnsupportedItem item = UnsupportedItem.builder().value(input).build();
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        StaticImmutableTableSchema<UnsupportedItem, UnsupportedItem.Builder> schema =
            StaticImmutableTableSchema.builder(UnsupportedItem.class, UnsupportedItem.Builder.class)
                .newItemBuilder(UnsupportedItem.Builder::new, UnsupportedItem.Builder::build)
                .addAttribute(type, a -> a.name("value")
                    .getter(UnsupportedItem::value).setter(UnsupportedItem.Builder::value)
                    .tags(primaryPartitionKey())
                    .attributeConverter(new UnsupportedStringConverter()))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        UnsupportedItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(UnsupportedStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.value()).isEqualTo(input);
    }

    @Test
    @DisplayName("Attribute-level LinkedListConverter intercepts a captured LinkedList type")
    void converterForAttribute_whenLinkedListConverter_builderReceivesLinkedList() {
        LinkedList<String> input = new LinkedList<>(Arrays.asList("a", "b"));
        LinkedListItem item = LinkedListItem.builder().id("id-1").value(input).build();
        EnhancedType<LinkedList<String>> type = new EnhancedType<LinkedList<String>>() {
        };
        AtomicReference<Object> assigned = new AtomicReference<>();

        StaticImmutableTableSchema<LinkedListItem, LinkedListItem.Builder> schema =
            StaticImmutableTableSchema.builder(LinkedListItem.class, LinkedListItem.Builder.class)
                .newItemBuilder(LinkedListItem.Builder::new, LinkedListItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(LinkedListItem::id).setter(LinkedListItem.Builder::id)
                    .tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(LinkedListItem::value)
                    .setter((builder, value) -> {
                        assigned.set(value);
                        builder.value(value);
                    })
                    .attributeConverter(new LinkedListConverter()))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(LinkedListConverter.class);
        assertThat(schema.converterForAttribute("value").type()).isEqualTo(type);
        assertThat(assigned.get()).isInstanceOf(LinkedList.class);
    }

    @Test
    @DisplayName("Empty providers fail without an attribute converter")
    void build_whenEmptyProvidersWithoutAttributeConverter_throwsNullPointerException() {
        EnhancedType<String> type = EnhancedType.of(String.class);

        assertThatThrownBy(() -> StaticImmutableTableSchema.builder(StringItem.class, StringItem.Builder.class)
            .newItemBuilder(StringItem.Builder::new, StringItem.Builder::build)
            .attributeConverterProviders(Collections.emptyList())
            .addAttribute(type, a -> a.name("value")
                .getter(StringItem::value).setter(StringItem.Builder::value).tags(primaryPartitionKey()))
            .build())
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("Attribute-level CustomStringConverter survives empty providers")
    void itemToMap_whenEmptyProvidersWithCustomStringConverter_writesCustomText() {
        StringItem item = StringItem.builder().value("text").build();
        EnhancedType<String> type = EnhancedType.of(String.class);

        StaticImmutableTableSchema<StringItem, StringItem.Builder> schema =
            StaticImmutableTableSchema.builder(StringItem.class, StringItem.Builder.class)
                .newItemBuilder(StringItem.Builder::new, StringItem.Builder::build)
                .attributeConverterProviders(Collections.emptyList())
                .addAttribute(type, a -> a.name("value")
                    .getter(StringItem::value).setter(StringItem.Builder::value).tags(primaryPartitionKey())
                    .attributeConverter(new CustomStringConverter()))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        StringItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(CustomStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("custom:text");
        assertThat(read.value()).isEqualTo("text");
    }

    @Test
    @DisplayName("Unconverted captured LinkedList fails lookup before assignment")
    void build_whenUnconvertedLinkedList_throwsConverterNotFound() {
        EnhancedType<LinkedList<String>> type = new EnhancedType<LinkedList<String>>() {
        };

        assertThatThrownBy(() ->
            StaticImmutableTableSchema.builder(LinkedListItem.class, LinkedListItem.Builder.class)
                .newItemBuilder(LinkedListItem.Builder::new, LinkedListItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(LinkedListItem::id).setter(LinkedListItem.Builder::id)
                    .tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(LinkedListItem::value).setter(LinkedListItem.Builder::value))
                .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Custom member converter is not consulted for an enclosing list token")
    void build_whenListOfUnsupportedWithMemberProvider_throwsConverterNotFoundForMemberType() {
        EnhancedType<List<UnsupportedType>> type = EnhancedType.listOf(UnsupportedType.class);
        UnsupportedMemberProvider custom = new UnsupportedMemberProvider();

        assertThatThrownBy(() ->
            StaticImmutableTableSchema.builder(UnsupportedListItem.class, UnsupportedListItem.Builder.class)
                .newItemBuilder(UnsupportedListItem.Builder::new, UnsupportedListItem.Builder::build)
                .attributeConverterProviders(custom, DefaultAttributeConverterProvider.create())
                .addAttribute(type, a -> a.name("value")
                    .getter(UnsupportedListItem::value).setter(UnsupportedListItem.Builder::value))
                .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
        assertThat(custom.requestedTypes()).containsExactly(type);
    }

    @Test
    @DisplayName("Collection of String selects SetAttributeConverter and rebuilds a LinkedHashSet")
    void converterForAttribute_whenStringCollection_selectsSetConverterAndRebuildsLinkedHashSet() {
        Collection<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        CollectionItem item = CollectionItem.builder().id("id-1").value(input).build();
        EnhancedType<Collection<String>> type = EnhancedType.collectionOf(String.class);

        StaticImmutableTableSchema<CollectionItem, CollectionItem.Builder> schema =
            StaticImmutableTableSchema.builder(CollectionItem.class, CollectionItem.Builder.class)
                .newItemBuilder(CollectionItem.Builder::new, CollectionItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(CollectionItem::id).setter(CollectionItem.Builder::id)
                    .tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(CollectionItem::value).setter(CollectionItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        CollectionItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.value()).isInstanceOf(LinkedHashSet.class)
                                .containsExactly("a", "b");
    }

    @Test
    @DisplayName("Set of String selects SetAttributeConverter and rebuilds a LinkedHashSet")
    void converterForAttribute_whenStringSet_selectsSetConverterAndRebuildsLinkedHashSet() {
        Set<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        SetItem item = SetItem.builder().id("id-1").value(input).build();
        EnhancedType<Set<String>> type = EnhancedType.setOf(String.class);

        StaticImmutableTableSchema<SetItem, SetItem.Builder> schema =
            StaticImmutableTableSchema.builder(SetItem.class, SetItem.Builder.class)
                .newItemBuilder(SetItem.Builder::new, SetItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(SetItem::id).setter(SetItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(SetItem::value).setter(SetItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        SetItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.value()).isInstanceOf(LinkedHashSet.class)
                                .containsExactly("a", "b");
    }

    @Test
    @DisplayName("Map of String to Integer selects MapAttributeConverter and rebuilds a LinkedHashMap")
    void converterForAttribute_whenStringIntegerMap_selectsMapConverterAndRebuildsLinkedHashMap() {
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", 2);
        MapItem item = MapItem.builder().id("id-1").value(input).build();
        EnhancedType<Map<String, Integer>> type = EnhancedType.mapOf(String.class, Integer.class);

        StaticImmutableTableSchema<MapItem, MapItem.Builder> schema =
            StaticImmutableTableSchema.builder(MapItem.class, MapItem.Builder.class)
                .newItemBuilder(MapItem.Builder::new, MapItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(MapItem::id).setter(MapItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(MapItem::value).setter(MapItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        MapItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(MapAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(read.value()).isEqualTo(input).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    @DisplayName("Enum attribute selects EnumAttributeConverter and round-trips")
    void converterForAttribute_whenEnumValue_selectsEnumConverterAndReadsEqualValue() {
        EnumItem item = EnumItem.builder().id("id-1").value(TestEnum.OPEN).build();
        EnhancedType<TestEnum> type = EnhancedType.of(TestEnum.class);

        StaticImmutableTableSchema<EnumItem, EnumItem.Builder> schema =
            StaticImmutableTableSchema.builder(EnumItem.class, EnumItem.Builder.class)
                .newItemBuilder(EnumItem.Builder::new, EnumItem.Builder::build)
                .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                    .getter(EnumItem::id).setter(EnumItem.Builder::id).tags(primaryPartitionKey()))
                .addAttribute(type, a -> a.name("value")
                    .getter(EnumItem::value).setter(EnumItem.Builder::value))
                .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        EnumItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("value")).isInstanceOf(EnumAttributeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("OPEN");
        assertThat(read.value()).isEqualTo(TestEnum.OPEN);
    }

    @Test
    @DisplayName("Unconverted captured HashSet fails lookup before assignment")
    void build_whenUnconvertedHashSet_throwsConverterNotFound() {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() {
        };

        assertThatThrownBy(() -> StaticImmutableTableSchema.builder(HashSetItem.class, HashSetItem.Builder.class)
            .newItemBuilder(HashSetItem.Builder::new, HashSetItem.Builder::build)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(HashSetItem::id).setter(HashSetItem.Builder::id).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(HashSetItem::value).setter(HashSetItem.Builder::value))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Unconverted captured HashMap fails lookup before assignment")
    void build_whenUnconvertedHashMap_throwsConverterNotFound() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };

        assertThatThrownBy(() -> StaticImmutableTableSchema.builder(HashMapItem.class, HashMapItem.Builder.class)
            .newItemBuilder(HashMapItem.Builder::new, HashMapItem.Builder::build)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(HashMapItem::id).setter(HashMapItem.Builder::id).tags(primaryPartitionKey()))
            .addAttribute(type, a -> a.name("value")
                .getter(HashMapItem::value).setter(HashMapItem.Builder::value))
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

    private static StaticImmutableTableSchema<StringItem, StringItem.Builder> stringSchema() {
        return StaticImmutableTableSchema.builder(StringItem.class, StringItem.Builder.class)
            .newItemBuilder(StringItem.Builder::new, StringItem.Builder::build)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(StringItem::id).setter(StringItem.Builder::id).tags(primaryPartitionKey()))
            .addAttribute(EnhancedType.of(String.class), a -> a.name("value")
                .getter(StringItem::value).setter(StringItem.Builder::value))
            .build();
    }

    static final class InstantItem {
        private final String id;
        private final Instant value;

        private InstantItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public Instant value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Instant value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Instant value) {
                this.value = value;
                return this;
            }

            public InstantItem build() {
                return new InstantItem(this);
            }
        }
    }

    static final class ListItem {
        private final String id;
        private final List<String> value;

        private ListItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public List<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private List<String> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(List<String> value) {
                this.value = value;
                return this;
            }

            public ListItem build() {
                return new ListItem(this);
            }
        }
    }

    static final class DocumentItem {
        private final String id;
        private final DocumentType value;

        private DocumentItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public DocumentType value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private DocumentType value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(DocumentType value) {
                this.value = value;
                return this;
            }

            public DocumentItem build() {
                return new DocumentItem(this);
            }
        }
    }

    static final class StringItem {
        private final String id;
        private final String value;

        private StringItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public String value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private String value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public StringItem build() {
                return new StringItem(this);
            }
        }
    }

    static final class ObjectItem {
        private final String id;
        private final Object value;

        private ObjectItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public Object value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Object value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Object value) {
                this.value = value;
                return this;
            }

            public ObjectItem build() {
                return new ObjectItem(this);
            }
        }
    }

    static final class UnsupportedItem {
        private final String id;
        private final UnsupportedType value;

        private UnsupportedItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public UnsupportedType value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private UnsupportedType value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(UnsupportedType value) {
                this.value = value;
                return this;
            }

            public UnsupportedItem build() {
                return new UnsupportedItem(this);
            }
        }
    }

    static final class CustomTypeItem {
        private final CustomType value;

        private CustomTypeItem(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public CustomType value() {
            return value;
        }

        public static final class Builder {
            private CustomType value;

            public Builder() {
            }

            public Builder value(CustomType value) {
                this.value = value;
                return this;
            }

            public CustomTypeItem build() {
                return new CustomTypeItem(this);
            }
        }
    }

    static final class LinkedListItem {
        private final String id;
        private final LinkedList<String> value;

        private LinkedListItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public LinkedList<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private LinkedList<String> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(LinkedList<String> value) {
                this.value = value;
                return this;
            }

            public LinkedListItem build() {
                return new LinkedListItem(this);
            }
        }
    }

    static final class UnsupportedListItem {
        private final String id;
        private final List<UnsupportedType> value;

        private UnsupportedListItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public List<UnsupportedType> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private List<UnsupportedType> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(List<UnsupportedType> value) {
                this.value = value;
                return this;
            }

            public UnsupportedListItem build() {
                return new UnsupportedListItem(this);
            }
        }
    }

    static final class CollectionItem {
        private final String id;
        private final Collection<String> value;

        private CollectionItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public Collection<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Collection<String> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Collection<String> value) {
                this.value = value;
                return this;
            }

            public CollectionItem build() {
                return new CollectionItem(this);
            }
        }
    }

    static final class SetItem {
        private final String id;
        private final Set<String> value;

        private SetItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public Set<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Set<String> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Set<String> value) {
                this.value = value;
                return this;
            }

            public SetItem build() {
                return new SetItem(this);
            }
        }
    }

    static final class MapItem {
        private final String id;
        private final Map<String, Integer> value;

        private MapItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public Map<String, Integer> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Map<String, Integer> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Map<String, Integer> value) {
                this.value = value;
                return this;
            }

            public MapItem build() {
                return new MapItem(this);
            }
        }
    }

    static final class EnumItem {
        private final String id;
        private final TestEnum value;

        private EnumItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public TestEnum value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private TestEnum value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(TestEnum value) {
                this.value = value;
                return this;
            }

            public EnumItem build() {
                return new EnumItem(this);
            }
        }
    }

    static final class HashSetItem {
        private final String id;
        private final HashSet<String> value;

        private HashSetItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public HashSet<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private HashSet<String> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(HashSet<String> value) {
                this.value = value;
                return this;
            }

            public HashSetItem build() {
                return new HashSetItem(this);
            }
        }
    }

    static final class HashMapItem {
        private final String id;
        private final HashMap<String, Integer> value;

        private HashMapItem(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public HashMap<String, Integer> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private HashMap<String, Integer> value;

            public Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(HashMap<String, Integer> value) {
                this.value = value;
                return this;
            }

            public HashMapItem build() {
                return new HashMapItem(this);
            }
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
        public CustomTypeConverter() {
        }

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
        public CustomStringConverter() {
        }

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
        public UnsupportedStringConverter() {
        }

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

    public static final class LinkedListConverter implements AttributeConverter<LinkedList<String>> {
        private static final EnhancedType<LinkedList<String>> TYPE = new EnhancedType<LinkedList<String>>() {
        };

        public LinkedListConverter() {
        }

        @Override
        public AttributeValue transformFrom(LinkedList<String> input) {
            List<AttributeValue> values = new ArrayList<>();
            for (String member : input) {
                values.add(AttributeValue.fromS(member));
            }
            return AttributeValue.fromL(values);
        }

        @Override
        public LinkedList<String> transformTo(AttributeValue input) {
            LinkedList<String> result = new LinkedList<>();
            for (AttributeValue member : input.l()) {
                result.add(member.s());
            }
            return result;
        }

        @Override
        public EnhancedType<LinkedList<String>> type() {
            return TYPE;
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.L;
        }
    }

}
