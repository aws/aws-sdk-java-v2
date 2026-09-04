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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.DocumentAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.IntegerAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests converter selection and item conversion for immutable table schemas.
 * <p>
 * The tests cover scalar, enumeration, collection, map, and nested immutable attributes. They also verify null
 * handling, schema caching, declared converter providers, attribute converters, provider precedence, and unsupported
 * attribute types during schema creation and item conversion.
 */
public class ImmutableSchemaConverterTest {

    @BeforeEach
    void setUp() {
        clearSchemaCache(BeanTableSchema.class);
        clearSchemaCache(ImmutableTableSchema.class);
        RecordingCustomProvider.reset();
        ReturningNullProvider.reset();
        ThrowingProvider.reset();
        ObjectProvider.reset();
        UnsupportedTypeOnlyProvider.reset();
    }

    private static void clearSchemaCache(Class<?> schemaClass) {
        try {
            Method method = schemaClass.getDeclaredMethod("clearSchemaCache");
            method.setAccessible(true);
            method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("A nonnull integer property selects IntegerAttributeConverter and round-trips")
    void fromImmutableClass_whenNonnullInteger_selectsIntegerConverterAndRoundTrips() {
        TableSchema<IntegerModel> schema = TableSchema.fromImmutableClass(IntegerModel.class);
        IntegerModel model = IntegerModel.builder().id("id-1").value(42).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        IntegerModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(IntegerAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.N);
        assertThat(map.get("value").n()).isEqualTo("42");
        assertThat(read.value()).isEqualTo(42);
    }

    @Test
    @DisplayName("A string set is stored as SS and rebuilt as a LinkedHashSet")
    void fromImmutableClass_whenStringSet_selectsSetConverterAndRebuildsLinkedHashSet() {
        TableSchema<StringSetModel> schema = TableSchema.fromImmutableClass(StringSetModel.class);
        Set<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        StringSetModel model = StringSetModel.builder().id("id-1").value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringSetModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.value()).isInstanceOf(LinkedHashSet.class)
                                .containsExactly("a", "b");
    }

    @Test
    @DisplayName("A nested immutable property selects DocumentAttributeConverter and is rebuilt")
    void fromImmutableClass_whenNestedImmutable_selectsDocumentConverterAndRebuildsNestedValue() {
        TableSchema<NestedOuterModel> schema = TableSchema.fromImmutableClass(NestedOuterModel.class);
        NestedInnerModel nested = NestedInnerModel.builder().nestedValue("inner").build();
        NestedOuterModel model = NestedOuterModel.builder().id("id-1").value(nested).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        NestedOuterModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(read.value().nestedValue()).isEqualTo("inner");
    }

    @Test
    @DisplayName("itemToMap with ignoreNulls false stores a null string as DynamoDB NULL")
    void itemToMap_whenNullStringIgnoreNullsFalse_includesNulValue() {
        TableSchema<StringModel> schema = TableSchema.fromImmutableClass(StringModel.class);
        StringModel model = StringModel.builder().id("id-1").build();

        Map<String, AttributeValue> map = schema.itemToMap(model, false);

        assertThat(map).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("itemToMap with ignoreNulls true omits a null string property")
    void itemToMap_whenNullStringIgnoreNullsTrue_omitsValue() {
        TableSchema<StringModel> schema = TableSchema.fromImmutableClass(StringModel.class);
        StringModel model = StringModel.builder().id("id-1").build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);

        assertThat(map).doesNotContainKey("value");
    }

    @Test
    @DisplayName("mapToItem skips the builder method for a DynamoDB NULL string property")
    void mapToItem_whenNulString_doesNotCallBuilderValueAndLeavesValueNull() {
        TableSchema<NullReadModel> schema = TableSchema.fromImmutableClass(NullReadModel.class);
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("id", AttributeValue.fromS("id-1"));
        map.put("value", AttributeValue.fromNul(true));
        NullReadModel.Builder.resetValueCalls();

        NullReadModel read = schema.mapToItem(map);

        assertThat(read).isNotNull();
        assertThat(NullReadModel.Builder.valueCalls()).isZero();
        assertThat(read.value()).isNull();
    }

    @Test
    @DisplayName("An unconverted Object property fails converter lookup")
    void fromImmutableClass_whenUnconvertedObject_throwsConverterNotFound() {
        assertThatThrownBy(() -> TableSchema.fromImmutableClass(ObjectModel.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("An unconverted UnsupportedType property fails converter lookup")
    void fromImmutableClass_whenUnconvertedUnsupportedType_throwsIllegalStateException() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() -> TableSchema.fromImmutableClass(UnsupportedModel.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A custom provider listed first is consulted twice and supplies CustomTypeConverter")
    void fromImmutableClass_whenCustomProviderFirst_invokesCustomTwiceAndRoundTrips() {
        TableSchema<CustomFirstModel> schema = TableSchema.fromImmutableClass(CustomFirstModel.class);
        CustomType input = new CustomType("x");
        CustomFirstModel model = CustomFirstModel.builder().value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        CustomFirstModel read = schema.mapToItem(map);

        assertThat(RecordingCustomProvider.current().requestedTypes())
            .hasSize(2)
            .containsExactly(EnhancedType.of(CustomType.class), EnhancedType.of(CustomType.class));
        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(CustomTypeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.value()).isEqualTo(input);
    }

    @Test
    @DisplayName("A null-returning provider is consulted once then the default string converter is used")
    void fromImmutableClass_whenNullReturningProviderThenDefault_invokesProviderOnceAndSelectsStringConverter() {
        TableSchema<NullThenDefaultModel> schema =
            TableSchema.fromImmutableClass(NullThenDefaultModel.class);

        assertThat(ReturningNullProvider.current().requestedTypes())
            .containsExactly(EnhancedType.of(String.class));
        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(StringAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
    }

    @Test
    @DisplayName("Default-first ordering throws before a later custom provider is consulted")
    void fromImmutableClass_whenDefaultProviderFirst_throwsAndDoesNotInvokeCustomProvider() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);

        assertThatThrownBy(() -> TableSchema.fromImmutableClass(DefaultThenCustomModel.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
        RecordingCustomProvider custom = RecordingCustomProvider.current();
        assertThat(custom == null || custom.requestedTypes().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("A throwing provider failure is propagated without consulting the default provider")
    void fromImmutableClass_whenThrowingProviderThenDefault_propagatesProviderFailure() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);

        assertThatThrownBy(() -> TableSchema.fromImmutableClass(ThrowingThenDefaultModel.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + type);
    }

    @Test
    @DisplayName("Immutable schema factory caches by model class")
    void fromImmutableClass_whenSameClassTwice_returnsSameImmutableTableSchemaReference() {
        ImmutableTableSchema<StringModel> first = TableSchema.fromImmutableClass(StringModel.class);
        ImmutableTableSchema<StringModel> second = TableSchema.fromImmutableClass(StringModel.class);

        assertThat(first).isSameAs(second);
        assertThat(first).isInstanceOf(ImmutableTableSchema.class);
    }

    @Test
    @DisplayName("Generic schema factory dispatches an immutable class")
    void fromClass_whenImmutableModel_returnsImmutableTableSchemaWithStringConverter() {
        StringModel item = StringModel.builder().id("id-1").value("text").build();

        TableSchema<StringModel> schema = TableSchema.fromClass(StringModel.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);

        assertThat(schema).isInstanceOf(ImmutableTableSchema.class);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(StringAttributeConverter.class);
        assertThat(map.get("value").s()).isEqualTo("text");
    }

    @Test
    @DisplayName("Immutable ObjectProvider before default selects ObjectStringConverter")
    void fromImmutableClass_whenObjectProviderBeforeDefault_selectsObjectStringConverter() {
        ObjectProviderModel item = ObjectProviderModel.builder().value(new Object()).build();

        TableSchema<ObjectProviderModel> schema = TableSchema.fromImmutableClass(ObjectProviderModel.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ObjectProviderModel read = schema.mapToItem(map);

        int objectRequestCount = 0;
        for (EnhancedType<?> requestedType : ObjectProvider.current().requestedTypes()) {
            if (EnhancedType.of(Object.class).equals(requestedType)) {
                objectRequestCount++;
            }
        }
        assertThat(objectRequestCount).isEqualTo(2);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(ObjectStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.value()).isEqualTo("custom");
    }

    @Test
    @DisplayName("ConvertedBy intercepts Object and does not consult the schema provider for that type")
    void fromImmutableClass_whenObjectConvertedBy_selectsObjectStringConverterAndSkipsProvider() {
        TableSchema<ConvertedObjectModel> schema =
            TableSchema.fromImmutableClass(ConvertedObjectModel.class);
        ConvertedObjectModel model = ConvertedObjectModel.builder().value(new Object()).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        ConvertedObjectModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(ObjectStringConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(ObjectProvider.current().requestedTypes()).doesNotContain(EnhancedType.of(Object.class));
        assertThat(read.value()).isEqualTo("custom");
    }

    @Test
    @DisplayName("ConvertedBy intercepts UnsupportedType with UnsupportedStringConverter")
    void fromImmutableClass_whenUnsupportedConvertedBy_selectsUnsupportedStringConverterAndRoundTrips() {
        TableSchema<ConvertedUnsupportedModel> schema =
            TableSchema.fromImmutableClass(ConvertedUnsupportedModel.class);
        UnsupportedType input = new UnsupportedType();
        ConvertedUnsupportedModel model = ConvertedUnsupportedModel.builder().value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        ConvertedUnsupportedModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(UnsupportedStringConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.value()).isEqualTo(input);
    }

    @Test
    @DisplayName("ConvertedBy intercepts HashSet and the builder receives a HashSet")
    void fromImmutableClass_whenHashSetConvertedBy_selectsHashSetConverterAndBuilderReceivesHashSet() {
        TableSchema<ConvertedHashSetModel> schema =
            TableSchema.fromImmutableClass(ConvertedHashSetModel.class);
        HashSet<String> input = new HashSet<>();
        input.add("a");
        input.add("b");
        ConvertedHashSetModel model = ConvertedHashSetModel.builder().value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        ConvertedHashSetModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(HashSetConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.value()).isInstanceOf(HashSet.class)
                                .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    @DisplayName("An empty provider list fails with NPE when no attribute converter is present")
    void fromImmutableClass_whenEmptyProvidersWithoutConvertedBy_throwsNullPointerException() {
        assertThatThrownBy(() -> TableSchema.fromImmutableClass(EmptyProvidersModel.class))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("ConvertedBy still works when the immutable type declares an empty provider list")
    void fromImmutableClass_whenEmptyProvidersWithConvertedBy_usesCustomStringConverter() {
        TableSchema<EmptyProvidersConvertedModel> schema =
            TableSchema.fromImmutableClass(EmptyProvidersConvertedModel.class);
        EmptyProvidersConvertedModel model = EmptyProvidersConvertedModel.builder().value("text").build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        EmptyProvidersConvertedModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(CustomStringConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("custom:text");
        assertThat(read.value()).isEqualTo("text");
    }

    @Test
    @DisplayName("An unconverted HashSet property fails lookup before builder assignment")
    void fromImmutableClass_whenUnconvertedHashSet_throwsIllegalStateException() {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() {
        };

        assertThatThrownBy(() -> TableSchema.fromImmutableClass(HashSetModel.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A custom provider is not consulted for set members during default fallback")
    void fromImmutableClass_whenUnsupportedSetWithCustomMemberProvider_throwsForMemberType() {

        assertThatThrownBy(() -> TableSchema.fromImmutableClass(UnsupportedSetModel.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
        assertThat(UnsupportedTypeOnlyProvider.current().requestedTypes()).hasSize(1);
        EnhancedType<?> requestedType = UnsupportedTypeOnlyProvider.current().requestedTypes().get(0);
        assertThat(requestedType.rawClass()).isEqualTo(Set.class);
        assertThat(requestedType.rawClassParameters()).containsExactly(EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("A Collection of String is stored as SS and rebuilt as a LinkedHashSet")
    void fromImmutableClass_whenStringCollection_selectsSetConverterAndRebuildsLinkedHashSet() {
        TableSchema<StringCollectionModel> schema = TableSchema.fromImmutableClass(StringCollectionModel.class);
        Collection<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        StringCollectionModel model = StringCollectionModel.builder().value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringCollectionModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.value()).isInstanceOf(LinkedHashSet.class)
                                .containsExactly("a", "b");
    }

    @Test
    @DisplayName("A string list with a duplicate is stored as L and rebuilt as an ArrayList")
    void fromImmutableClass_whenStringListWithDuplicate_preservesOrderAndDuplicateAsArrayList() {
        TableSchema<StringListModel> schema = TableSchema.fromImmutableClass(StringListModel.class);
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("a");
        StringListModel model = StringListModel.builder().id("id-1").value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringListModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(read.value()).isInstanceOf(ArrayList.class)
                                .containsExactly("a", "b", "a");
    }

    @Test
    @DisplayName("A string-to-integer map is stored as M and rebuilt as a LinkedHashMap")
    void fromImmutableClass_whenStringIntegerMap_selectsMapConverterAndRebuildsLinkedHashMap() {
        TableSchema<StringIntegerMapModel> schema =
            TableSchema.fromImmutableClass(StringIntegerMapModel.class);
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", 2);
        StringIntegerMapModel model = StringIntegerMapModel.builder().id("id-1").value(input).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringIntegerMapModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(read.value()).isEqualTo(input).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    @DisplayName("An enum property selects EnumAttributeConverter and round-trips")
    void fromImmutableClass_whenEnumProperty_selectsEnumConverterAndRoundTrips() {
        TableSchema<EnumModel> schema = TableSchema.fromImmutableClass(EnumModel.class);
        EnumModel model = EnumModel.builder().id("id-1").value(TestEnum.OPEN).build();

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        EnumModel read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(EnumAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("OPEN");
        assertThat(read.value()).isEqualTo(TestEnum.OPEN);
    }

    @Test
    @DisplayName("An unconverted HashMap property fails lookup before builder assignment")
    void fromImmutableClass_whenUnconvertedHashMap_throwsIllegalStateException() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };

        assertThatThrownBy(() -> TableSchema.fromImmutableClass(HashMapModel.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @DynamoDbImmutable(builder = IntegerModel.Builder.class)
    public static final class IntegerModel {
        private final String id;
        private final Integer value;

        private IntegerModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public Integer value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Integer value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Integer value) {
                this.value = value;
                return this;
            }

            public IntegerModel build() {
                return new IntegerModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = StringSetModel.Builder.class)
    public static final class StringSetModel {
        private final String id;
        private final Set<String> value;

        private StringSetModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public Set<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Set<String> value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Set<String> value) {
                this.value = value;
                return this;
            }

            public StringSetModel build() {
                return new StringSetModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NestedInnerModel.Builder.class)
    public static final class NestedInnerModel {
        private final String nestedValue;

        private NestedInnerModel(Builder b) {
            this.nestedValue = b.nestedValue;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String nestedValue() {
            return nestedValue;
        }

        public static final class Builder {
            private String nestedValue;

            public Builder nestedValue(String nestedValue) {
                this.nestedValue = nestedValue;
                return this;
            }

            public NestedInnerModel build() {
                return new NestedInnerModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NestedOuterModel.Builder.class)
    public static final class NestedOuterModel {
        private final String id;
        private final NestedInnerModel value;

        private NestedOuterModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public NestedInnerModel value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private NestedInnerModel value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(NestedInnerModel value) {
                this.value = value;
                return this;
            }

            public NestedOuterModel build() {
                return new NestedOuterModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = StringModel.Builder.class)
    public static final class StringModel {
        private final String id;
        private final String value;

        private StringModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public String value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private String value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public StringModel build() {
                return new StringModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NullReadModel.Builder.class)
    public static final class NullReadModel {
        private final String id;
        private final String value;

        private NullReadModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public String value() {
            return value;
        }

        public static final class Builder {
            private static final ThreadLocal<Integer> VALUE_CALLS = ThreadLocal.withInitial(() -> 0);
            private String id;
            private String value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(String value) {
                VALUE_CALLS.set(VALUE_CALLS.get() + 1);
                this.value = value;
                return this;
            }

            static int valueCalls() {
                return VALUE_CALLS.get();
            }

            static void resetValueCalls() {
                VALUE_CALLS.remove();
            }

            public NullReadModel build() {
                return new NullReadModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = ObjectModel.Builder.class)
    public static final class ObjectModel {
        private final Object value;

        private ObjectModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Object value() {
            return value;
        }

        public static final class Builder {
            private Object value;

            public Builder value(Object value) {
                this.value = value;
                return this;
            }

            public ObjectModel build() {
                return new ObjectModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = UnsupportedModel.Builder.class)
    public static final class UnsupportedModel {
        private final UnsupportedType value;

        private UnsupportedModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public UnsupportedType value() {
            return value;
        }

        public static final class Builder {
            private UnsupportedType value;

            public Builder value(UnsupportedType value) {
                this.value = value;
                return this;
            }

            public UnsupportedModel build() {
                return new UnsupportedModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = CustomFirstModel.Builder.class, converterProviders = {
        RecordingCustomProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static final class CustomFirstModel {
        private final CustomType value;

        private CustomFirstModel(Builder b) {
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

            public Builder value(CustomType value) {
                this.value = value;
                return this;
            }

            public CustomFirstModel build() {
                return new CustomFirstModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NullThenDefaultModel.Builder.class, converterProviders = {
        ReturningNullProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static final class NullThenDefaultModel {
        private final String value;

        private NullThenDefaultModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String value() {
            return value;
        }

        public static final class Builder {
            private String value;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public NullThenDefaultModel build() {
                return new NullThenDefaultModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = DefaultThenCustomModel.Builder.class, converterProviders = {
        DefaultAttributeConverterProvider.class,
        RecordingCustomProvider.class
    })
    public static final class DefaultThenCustomModel {
        private final CustomType value;

        private DefaultThenCustomModel(Builder b) {
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

            public Builder value(CustomType value) {
                this.value = value;
                return this;
            }

            public DefaultThenCustomModel build() {
                return new DefaultThenCustomModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = ThrowingThenDefaultModel.Builder.class, converterProviders = {
        ThrowingProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static final class ThrowingThenDefaultModel {
        private final CustomType value;

        private ThrowingThenDefaultModel(Builder b) {
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

            public Builder value(CustomType value) {
                this.value = value;
                return this;
            }

            public ThrowingThenDefaultModel build() {
                return new ThrowingThenDefaultModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = ObjectProviderModel.Builder.class, converterProviders = {
        ObjectProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static final class ObjectProviderModel {
        private final Object value;

        private ObjectProviderModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public Object value() {
            return value;
        }

        public static final class Builder {
            private Object value;

            public Builder value(Object value) {
                this.value = value;
                return this;
            }

            public ObjectProviderModel build() {
                return new ObjectProviderModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = ConvertedObjectModel.Builder.class, converterProviders = {
        ObjectProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static final class ConvertedObjectModel {
        private final Object value;

        private ConvertedObjectModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbConvertedBy(ObjectStringConverter.class)
        public Object value() {
            return value;
        }

        public static final class Builder {
            private Object value;

            public Builder value(Object value) {
                this.value = value;
                return this;
            }

            public ConvertedObjectModel build() {
                return new ConvertedObjectModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = ConvertedUnsupportedModel.Builder.class)
    public static final class ConvertedUnsupportedModel {
        private final UnsupportedType value;

        private ConvertedUnsupportedModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbConvertedBy(UnsupportedStringConverter.class)
        public UnsupportedType value() {
            return value;
        }

        public static final class Builder {
            private UnsupportedType value;

            public Builder value(UnsupportedType value) {
                this.value = value;
                return this;
            }

            public ConvertedUnsupportedModel build() {
                return new ConvertedUnsupportedModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = ConvertedHashSetModel.Builder.class)
    public static final class ConvertedHashSetModel {
        private final HashSet<String> value;

        private ConvertedHashSetModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbConvertedBy(HashSetConverter.class)
        public HashSet<String> value() {
            return value;
        }

        public static final class Builder {
            private HashSet<String> value;

            public Builder value(HashSet<String> value) {
                this.value = value;
                return this;
            }

            public ConvertedHashSetModel build() {
                return new ConvertedHashSetModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = EmptyProvidersModel.Builder.class, converterProviders = {})
    public static final class EmptyProvidersModel {
        private final String value;

        private EmptyProvidersModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String value() {
            return value;
        }

        public static final class Builder {
            private String value;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public EmptyProvidersModel build() {
                return new EmptyProvidersModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = EmptyProvidersConvertedModel.Builder.class, converterProviders = {})
    public static final class EmptyProvidersConvertedModel {
        private final String value;

        private EmptyProvidersConvertedModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbConvertedBy(CustomStringConverter.class)
        public String value() {
            return value;
        }

        public static final class Builder {
            private String value;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public EmptyProvidersConvertedModel build() {
                return new EmptyProvidersConvertedModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = HashSetModel.Builder.class)
    public static final class HashSetModel {
        private final HashSet<String> value;

        private HashSetModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public HashSet<String> value() {
            return value;
        }

        public static final class Builder {
            private HashSet<String> value;

            public Builder value(HashSet<String> value) {
                this.value = value;
                return this;
            }

            public HashSetModel build() {
                return new HashSetModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = UnsupportedSetModel.Builder.class, converterProviders = {
        UnsupportedTypeOnlyProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static final class UnsupportedSetModel {
        private final Set<UnsupportedType> value;

        private UnsupportedSetModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Set<UnsupportedType> value() {
            return value;
        }

        public static final class Builder {
            private Set<UnsupportedType> value;

            public Builder value(Set<UnsupportedType> value) {
                this.value = value;
                return this;
            }

            public UnsupportedSetModel build() {
                return new UnsupportedSetModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = StringCollectionModel.Builder.class)
    public static final class StringCollectionModel {
        private final Collection<String> value;

        private StringCollectionModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Collection<String> value() {
            return value;
        }

        public static final class Builder {
            private Collection<String> value;

            public Builder value(Collection<String> value) {
                this.value = value;
                return this;
            }

            public StringCollectionModel build() {
                return new StringCollectionModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = StringListModel.Builder.class)
    public static final class StringListModel {
        private final String id;
        private final List<String> value;

        private StringListModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public List<String> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private List<String> value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(List<String> value) {
                this.value = value;
                return this;
            }

            public StringListModel build() {
                return new StringListModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = StringIntegerMapModel.Builder.class)
    public static final class StringIntegerMapModel {
        private final String id;
        private final Map<String, Integer> value;

        private StringIntegerMapModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public Map<String, Integer> value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private Map<String, Integer> value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(Map<String, Integer> value) {
                this.value = value;
                return this;
            }

            public StringIntegerMapModel build() {
                return new StringIntegerMapModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = EnumModel.Builder.class)
    public static final class EnumModel {
        private final String id;
        private final TestEnum value;

        private EnumModel(Builder b) {
            this.id = b.id;
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public TestEnum value() {
            return value;
        }

        public static final class Builder {
            private String id;
            private TestEnum value;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder value(TestEnum value) {
                this.value = value;
                return this;
            }

            public EnumModel build() {
                return new EnumModel(this);
            }
        }
    }

    @DynamoDbImmutable(builder = HashMapModel.Builder.class)
    public static final class HashMapModel {
        private final HashMap<String, Integer> value;

        private HashMapModel(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public HashMap<String, Integer> value() {
            return value;
        }

        public static final class Builder {
            private HashMap<String, Integer> value;

            public Builder value(HashMap<String, Integer> value) {
                this.value = value;
                return this;
            }

            public HashMapModel build() {
                return new HashMapModel(this);
            }
        }
    }

    public enum TestEnum {
        OPEN,
        CLOSED
    }

    public static final class CustomType {
        private final String value;

        public CustomType(String value) {
            this.value = value;
        }

        public String value() {
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

    public static final class UnsupportedType {
        @Override
        public boolean equals(Object o) {
            return o instanceof UnsupportedType;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    public static class CustomTypeConverter implements AttributeConverter<CustomType> {
        public CustomTypeConverter() {
        }

        @Override
        public AttributeValue transformFrom(CustomType input) {
            return AttributeValue.fromS("custom:" + input.value());
        }

        @Override
        public CustomType transformTo(AttributeValue input) {
            String stored = input.s();
            return new CustomType(stored.substring("custom:".length()));
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

    public static class CustomStringConverter implements AttributeConverter<String> {
        public CustomStringConverter() {
        }

        @Override
        public AttributeValue transformFrom(String input) {
            return AttributeValue.fromS("custom:" + input);
        }

        @Override
        public String transformTo(AttributeValue input) {
            String stored = input.s();
            if (stored != null && stored.startsWith("custom:")) {
                return stored.substring("custom:".length());
            }
            return stored;
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

    public static class ObjectStringConverter implements AttributeConverter<Object> {
        public ObjectStringConverter() {
        }

        @Override
        public AttributeValue transformFrom(Object input) {
            return AttributeValue.fromS("custom");
        }

        @Override
        public Object transformTo(AttributeValue input) {
            return "custom";
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

    public static class UnsupportedStringConverter implements AttributeConverter<UnsupportedType> {
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

    public static class HashSetConverter implements AttributeConverter<HashSet<String>> {
        public HashSetConverter() {
        }

        @Override
        public AttributeValue transformFrom(HashSet<String> input) {
            return AttributeValue.fromSs(new ArrayList<>(input));
        }

        @Override
        public HashSet<String> transformTo(AttributeValue input) {
            return new HashSet<>(input.ss());
        }

        @Override
        public EnhancedType<HashSet<String>> type() {
            return new EnhancedType<HashSet<String>>() {
            };
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.SS;
        }
    }

    public static class RecordingCustomProvider implements AttributeConverterProvider {
        private static final ThreadLocal<RecordingCustomProvider> CURRENT =
            new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public RecordingCustomProvider() {
            CURRENT.set(this);
        }

        public static RecordingCustomProvider current() {
            return CURRENT.get();
        }

        public static void reset() {
            CURRENT.remove();
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

    public static class ReturningNullProvider implements AttributeConverterProvider {
        private static final ThreadLocal<ReturningNullProvider> CURRENT =
            new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public ReturningNullProvider() {
            CURRENT.set(this);
        }

        public static ReturningNullProvider current() {
            return CURRENT.get();
        }

        public static void reset() {
            CURRENT.remove();
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

    public static class ThrowingProvider implements AttributeConverterProvider {
        private static final ThreadLocal<ThrowingProvider> CURRENT = new ThreadLocal<>();

        public ThrowingProvider() {
            CURRENT.set(this);
        }

        public static ThrowingProvider current() {
            return CURRENT.get();
        }

        public static void reset() {
            CURRENT.remove();
        }

        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
            throw new IllegalArgumentException("Attribute converter provider failed while looking up " + enhancedType);
        }
    }

    public static class ObjectProvider implements AttributeConverterProvider {
        private static final ThreadLocal<ObjectProvider> CURRENT = new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public ObjectProvider() {
            CURRENT.set(this);
        }

        public static ObjectProvider current() {
            return CURRENT.get();
        }

        public static void reset() {
            CURRENT.remove();
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

    public static class UnsupportedTypeOnlyProvider implements AttributeConverterProvider {
        private static final ThreadLocal<UnsupportedTypeOnlyProvider> CURRENT =
            new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public UnsupportedTypeOnlyProvider() {
            CURRENT.set(this);
        }

        public static UnsupportedTypeOnlyProvider current() {
            return CURRENT.get();
        }

        public static void reset() {
            CURRENT.remove();
        }

        public List<EnhancedType<?>> requestedTypes() {
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
}
