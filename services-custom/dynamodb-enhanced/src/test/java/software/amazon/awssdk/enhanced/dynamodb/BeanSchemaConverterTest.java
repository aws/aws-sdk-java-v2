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
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests converter selection and item conversion for bean based table schemas.
 * <p>
 * The tests cover scalar, enumeration, collection, map, and nested bean attributes. They also verify null handling,
 * schema caching, declared converter providers, attribute converters, provider precedence, and unsupported attribute
 * types during schema creation and item conversion.
 */
public class BeanSchemaConverterTest {

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
    @DisplayName("A nonnull string property selects StringAttributeConverter and round-trips")
    void fromBean_whenNonnullString_selectsStringConverterAndRoundTrips() {
        TableSchema<StringBean> schema = TableSchema.fromBean(StringBean.class);
        StringBean model = new StringBean();
        model.setId("id-1");
        model.setValue("hello");

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(StringAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("hello");
        assertThat(read.getValue()).isEqualTo("hello");
    }

    @Test
    @DisplayName("A string list with a duplicate is stored as L and read as an ArrayList")
    void fromBean_whenStringListWithDuplicate_preservesOrderAndDuplicateAsArrayList() {
        TableSchema<StringListBean> schema = TableSchema.fromBean(StringListBean.class);
        StringListBean model = new StringListBean();
        model.setId("id-1");
        List<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        input.add("a");
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringListBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(ListAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(read.getValue()).isInstanceOf(ArrayList.class)
                                   .containsExactly("a", "b", "a");
    }

    @Test
    @DisplayName("A nested bean property selects DocumentAttributeConverter and is reconstructed")
    void fromBean_whenNestedBean_selectsDocumentConverterAndReconstructsNestedValue() {
        TableSchema<NestedOuterBean> schema = TableSchema.fromBean(NestedOuterBean.class);
        NestedInnerBean nested = new NestedInnerBean();
        nested.setNestedValue("inner");
        NestedOuterBean model = new NestedOuterBean();
        model.setId("id-1");
        model.setValue(nested);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        NestedOuterBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(DocumentAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(read.getValue().getNestedValue()).isEqualTo("inner");
    }

    @Test
    @DisplayName("itemToMap with ignoreNulls false stores a null string as DynamoDB NULL")
    void itemToMap_whenNullStringIgnoreNullsFalse_includesNulValue() {
        TableSchema<StringBean> schema = TableSchema.fromBean(StringBean.class);
        StringBean model = new StringBean();
        model.setId("id-1");

        Map<String, AttributeValue> map = schema.itemToMap(model, false);

        assertThat(map).containsEntry("value", AttributeValue.fromNul(true));
    }

    @Test
    @DisplayName("itemToMap with ignoreNulls true omits a null string property")
    void itemToMap_whenNullStringIgnoreNullsTrue_omitsValue() {
        TableSchema<StringBean> schema = TableSchema.fromBean(StringBean.class);
        StringBean model = new StringBean();
        model.setId("id-1");

        Map<String, AttributeValue> map = schema.itemToMap(model, true);

        assertThat(map).doesNotContainKey("value");
    }

    @Test
    @DisplayName("mapToItem skips the setter for a DynamoDB NULL string property")
    void mapToItem_whenNulString_doesNotCallSetterAndLeavesValueNull() {
        TableSchema<NullReadBean> schema = TableSchema.fromBean(NullReadBean.class);
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("id", AttributeValue.fromS("id-1"));
        map.put("value", AttributeValue.fromNul(true));

        NullReadBean read = schema.mapToItem(map);

        assertThat(read).isNotNull();
        assertThat(read.valueSetterCalls).isZero();
        assertThat(read.getValue()).isNull();
    }

    @Test
    @DisplayName("An unconverted Object property fails converter lookup")
    void fromBean_whenUnconvertedObject_throwsConverterNotFound() {
        assertThatThrownBy(() -> TableSchema.fromBean(ObjectBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("An unconverted UnsupportedType property fails converter lookup")
    void fromBean_whenUnconvertedUnsupportedType_throwsIllegalStateException() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() -> TableSchema.fromBean(UnsupportedBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A custom provider listed first is consulted twice and supplies CustomTypeConverter")
    void fromBean_whenCustomProviderFirst_invokesCustomTwiceAndRoundTrips() {
        TableSchema<CustomFirstBean> schema = TableSchema.fromBean(CustomFirstBean.class);
        CustomFirstBean model = new CustomFirstBean();
        CustomType input = new CustomType("x");
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        CustomFirstBean read = schema.mapToItem(map);

        assertThat(RecordingCustomProvider.current().requestedTypes())
            .hasSize(2)
            .containsExactly(EnhancedType.of(CustomType.class), EnhancedType.of(CustomType.class));
        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(CustomTypeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.getValue()).isEqualTo(input);
    }

    @Test
    @DisplayName("A null-returning provider is consulted once then the default string converter is used")
    void fromBean_whenNullReturningProviderThenDefault_invokesProviderOnceAndSelectsStringConverter() {
        TableSchema<NullThenDefaultBean> schema = TableSchema.fromBean(NullThenDefaultBean.class);

        assertThat(ReturningNullProvider.current().requestedTypes())
            .containsExactly(EnhancedType.of(String.class));
        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(StringAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
    }

    @Test
    @DisplayName("Default-first ordering throws before a later custom provider is consulted")
    void fromBean_whenDefaultProviderFirst_throwsAndDoesNotInvokeCustomProvider() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);

        assertThatThrownBy(() -> TableSchema.fromBean(DefaultThenCustomBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
        RecordingCustomProvider custom = RecordingCustomProvider.current();
        assertThat(custom == null || custom.requestedTypes().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("A throwing provider failure is propagated without consulting the default provider")
    void fromBean_whenThrowingProviderThenDefault_propagatesProviderFailure() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);

        assertThatThrownBy(() -> TableSchema.fromBean(ThrowingThenDefaultBean.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attribute converter provider failed while looking up " + type);
    }

    @Test
    @DisplayName("Bean schema factory caches by model class")
    void fromBean_whenSameClassTwice_returnsSameBeanTableSchemaReference() {
        BeanTableSchema<StringBean> first = TableSchema.fromBean(StringBean.class);
        BeanTableSchema<StringBean> second = TableSchema.fromBean(StringBean.class);

        assertThat(first).isSameAs(second);
        assertThat(first).isInstanceOf(BeanTableSchema.class);
    }

    @Test
    @DisplayName("Generic schema factory dispatches a bean class")
    void fromClass_whenBeanModel_returnsBeanTableSchemaWithStringConverter() {
        StringBean item = new StringBean();
        item.setId("id-1");
        item.setValue("text");

        TableSchema<StringBean> schema = TableSchema.fromClass(StringBean.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);

        assertThat(schema).isInstanceOf(BeanTableSchema.class);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(StringAttributeConverter.class);
        assertThat(map.get("value").s()).isEqualTo("text");
    }

    @Test
    @DisplayName("Bean ObjectProvider before default selects ObjectStringConverter")
    void fromBean_whenObjectProviderBeforeDefault_selectsObjectStringConverter() {
        ObjectProviderBean item = new ObjectProviderBean();
        item.setValue(new Object());

        TableSchema<ObjectProviderBean> schema = TableSchema.fromBean(ObjectProviderBean.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        ObjectProviderBean read = schema.mapToItem(map);

        int objectRequestCount = 0;
        for (EnhancedType<?> requestedType : ObjectProvider.current().requestedTypes()) {
            if (EnhancedType.of(Object.class).equals(requestedType)) {
                objectRequestCount++;
            }
        }
        assertThat(objectRequestCount).isEqualTo(2);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(ObjectStringConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.getValue()).isEqualTo("custom");
    }

    @Test
    @DisplayName("ConvertedBy intercepts Object and does not consult the schema provider for that type")
    void fromBean_whenObjectConvertedBy_selectsObjectStringConverterAndSkipsProvider() {
        TableSchema<ConvertedObjectBean> schema = TableSchema.fromBean(ConvertedObjectBean.class);
        ConvertedObjectBean model = new ConvertedObjectBean();
        model.setValue(new Object());

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        ConvertedObjectBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(ObjectStringConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(ObjectProvider.current().requestedTypes()).doesNotContain(EnhancedType.of(Object.class));
        assertThat(read.getValue()).isEqualTo("custom");
    }

    @Test
    @DisplayName("ConvertedBy intercepts UnsupportedType with UnsupportedStringConverter")
    void fromBean_whenUnsupportedConvertedBy_selectsUnsupportedStringConverterAndRoundTrips() {
        TableSchema<ConvertedUnsupportedBean> schema = TableSchema.fromBean(ConvertedUnsupportedBean.class);
        ConvertedUnsupportedBean model = new ConvertedUnsupportedBean();
        UnsupportedType input = new UnsupportedType();
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        ConvertedUnsupportedBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(UnsupportedStringConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(read.getValue()).isEqualTo(input);
    }

    @Test
    @DisplayName("ConvertedBy intercepts ArrayList and the setter receives an ArrayList")
    void fromBean_whenArrayListConvertedBy_selectsArrayListConverterAndSetterReceivesArrayList() {
        TableSchema<ConvertedArrayListBean> schema = TableSchema.fromBean(ConvertedArrayListBean.class);
        ConvertedArrayListBean model = new ConvertedArrayListBean();
        ArrayList<String> input = new ArrayList<>();
        input.add("a");
        input.add("b");
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        ConvertedArrayListBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(ArrayListConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(map.get("value").hasL()).isTrue();
        assertThat(read.getValue()).isInstanceOf(ArrayList.class)
                                   .containsExactly("a", "b");
    }

    @Test
    @DisplayName("An empty provider list fails with NPE when no attribute converter is present")
    void fromBean_whenEmptyProvidersWithoutConvertedBy_throwsNullPointerException() {
        assertThatThrownBy(() -> TableSchema.fromBean(EmptyProvidersBean.class))
            .isInstanceOf(NullPointerException.class)
            .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
    }

    @Test
    @DisplayName("ConvertedBy still works when the bean declares an empty provider list")
    void fromBean_whenEmptyProvidersWithConvertedBy_usesCustomStringConverter() {
        TableSchema<EmptyProvidersConvertedBean> schema =
            TableSchema.fromBean(EmptyProvidersConvertedBean.class);
        EmptyProvidersConvertedBean model = new EmptyProvidersConvertedBean();
        model.setValue("text");

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        EmptyProvidersConvertedBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(CustomStringConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("custom:text");
        assertThat(read.getValue()).isEqualTo("text");
    }

    @Test
    @DisplayName("An unconverted ArrayList property fails lookup before mapToItem")
    void fromBean_whenUnconvertedArrayList_throwsIllegalStateException() {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() {
        };

        assertThatThrownBy(() -> TableSchema.fromBean(ArrayListBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("A custom provider is not consulted for list members during default fallback")
    void fromBean_whenUnsupportedListWithCustomMemberProvider_throwsForMemberType() {

        assertThatThrownBy(() -> TableSchema.fromBean(UnsupportedListBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
        assertThat(UnsupportedTypeOnlyProvider.current().requestedTypes()).hasSize(1);
        EnhancedType<?> requestedType = UnsupportedTypeOnlyProvider.current().requestedTypes().get(0);
        assertThat(requestedType.rawClass()).isEqualTo(List.class);
        assertThat(requestedType.rawClassParameters()).containsExactly(EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("A Collection of String is stored as SS and read as a LinkedHashSet")
    void fromBean_whenStringCollection_selectsSetConverterAndReadsLinkedHashSet() {
        TableSchema<StringCollectionBean> schema = TableSchema.fromBean(StringCollectionBean.class);
        StringCollectionBean model = new StringCollectionBean();
        Collection<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringCollectionBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.getValue()).isInstanceOf(LinkedHashSet.class)
                                   .containsExactly("a", "b");
    }

    @Test
    @DisplayName("A string set is stored as SS and read as a LinkedHashSet")
    void fromBean_whenStringSet_selectsSetConverterAndReadsLinkedHashSet() {
        TableSchema<StringSetBean> schema = TableSchema.fromBean(StringSetBean.class);
        StringSetBean model = new StringSetBean();
        model.setId("id-1");
        Set<String> input = new LinkedHashSet<>();
        input.add("a");
        input.add("b");
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringSetBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(SetAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(read.getValue()).isInstanceOf(LinkedHashSet.class)
                                   .containsExactly("a", "b");
    }

    @Test
    @DisplayName("A string-to-integer map is stored as M and read as a LinkedHashMap")
    void fromBean_whenStringIntegerMap_selectsMapConverterAndReadsLinkedHashMap() {
        TableSchema<StringIntegerMapBean> schema = TableSchema.fromBean(StringIntegerMapBean.class);
        StringIntegerMapBean model = new StringIntegerMapBean();
        model.setId("id-1");
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", 2);
        model.setValue(input);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        StringIntegerMapBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(MapAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(read.getValue()).isEqualTo(input).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    @DisplayName("An enum property selects EnumAttributeConverter and round-trips")
    void fromBean_whenEnumProperty_selectsEnumConverterAndRoundTrips() {
        TableSchema<EnumBean> schema = TableSchema.fromBean(EnumBean.class);
        EnumBean model = new EnumBean();
        model.setId("id-1");
        model.setValue(TestEnum.OPEN);

        Map<String, AttributeValue> map = schema.itemToMap(model, true);
        EnumBean read = schema.mapToItem(map);

        AttributeConverter<?> converter = schema.converterForAttribute("value");
        assertThat(converter).isInstanceOf(EnumAttributeConverter.class);
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("OPEN");
        assertThat(read.getValue()).isEqualTo(TestEnum.OPEN);
    }

    @Test
    @DisplayName("An unconverted HashSet property fails lookup before mapToItem")
    void fromBean_whenUnconvertedHashSet_throwsIllegalStateException() {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() {
        };

        assertThatThrownBy(() -> TableSchema.fromBean(HashSetBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("An unconverted HashMap property fails lookup before mapToItem")
    void fromBean_whenUnconvertedHashMap_throwsIllegalStateException() {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() {
        };

        assertThatThrownBy(() -> TableSchema.fromBean(HashMapBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @DynamoDbBean
    public static class StringBean {
        private String id;
        private String value;

        @DynamoDbPartitionKey
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

    @DynamoDbBean
    public static class StringListBean {
        private String id;
        private List<String> value;

        @DynamoDbPartitionKey
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

    @DynamoDbBean
    public static class NestedInnerBean {
        private String nestedValue;

        public String getNestedValue() {
            return nestedValue;
        }

        public void setNestedValue(String nestedValue) {
            this.nestedValue = nestedValue;
        }
    }

    @DynamoDbBean
    public static class NestedOuterBean {
        private String id;
        private NestedInnerBean value;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NestedInnerBean getValue() {
            return value;
        }

        public void setValue(NestedInnerBean value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class NullReadBean {
        private String id;
        private String value;
        public int valueSetterCalls;

        @DynamoDbPartitionKey
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
            valueSetterCalls++;
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class ObjectBean {
        private Object value;

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class UnsupportedBean {
        private UnsupportedType value;

        public UnsupportedType getValue() {
            return value;
        }

        public void setValue(UnsupportedType value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        RecordingCustomProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class CustomFirstBean {
        private CustomType value;

        public CustomType getValue() {
            return value;
        }

        public void setValue(CustomType value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        ReturningNullProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class NullThenDefaultBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        DefaultAttributeConverterProvider.class,
        RecordingCustomProvider.class
    })
    public static class DefaultThenCustomBean {
        private CustomType value;

        public CustomType getValue() {
            return value;
        }

        public void setValue(CustomType value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        ThrowingProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class ThrowingThenDefaultBean {
        private CustomType value;

        public CustomType getValue() {
            return value;
        }

        public void setValue(CustomType value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        ObjectProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class ObjectProviderBean {
        private Object value;

        @DynamoDbPartitionKey
        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        ObjectProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class ConvertedObjectBean {
        private Object value;

        @DynamoDbConvertedBy(ObjectStringConverter.class)
        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class ConvertedUnsupportedBean {
        private UnsupportedType value;

        @DynamoDbConvertedBy(UnsupportedStringConverter.class)
        public UnsupportedType getValue() {
            return value;
        }

        public void setValue(UnsupportedType value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class ConvertedArrayListBean {
        private ArrayList<String> value;

        @DynamoDbConvertedBy(ArrayListConverter.class)
        public ArrayList<String> getValue() {
            return value;
        }

        public void setValue(ArrayList<String> value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {})
    public static class EmptyProvidersBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {})
    public static class EmptyProvidersConvertedBean {
        private String value;

        @DynamoDbConvertedBy(CustomStringConverter.class)
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class ArrayListBean {
        private ArrayList<String> value;

        public ArrayList<String> getValue() {
            return value;
        }

        public void setValue(ArrayList<String> value) {
            this.value = value;
        }
    }

    @DynamoDbBean(converterProviders = {
        UnsupportedTypeOnlyProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class UnsupportedListBean {
        private List<UnsupportedType> value;

        public List<UnsupportedType> getValue() {
            return value;
        }

        public void setValue(List<UnsupportedType> value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class StringCollectionBean {
        private Collection<String> value;

        public Collection<String> getValue() {
            return value;
        }

        public void setValue(Collection<String> value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class StringSetBean {
        private String id;
        private Set<String> value;

        @DynamoDbPartitionKey
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

    @DynamoDbBean
    public static class StringIntegerMapBean {
        private String id;
        private Map<String, Integer> value;

        @DynamoDbPartitionKey
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

    @DynamoDbBean
    public static class EnumBean {
        private String id;
        private TestEnum value;

        @DynamoDbPartitionKey
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

    @DynamoDbBean
    public static class HashSetBean {
        private HashSet<String> value;

        public HashSet<String> getValue() {
            return value;
        }

        public void setValue(HashSet<String> value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class HashMapBean {
        private HashMap<String, Integer> value;

        public HashMap<String, Integer> getValue() {
            return value;
        }

        public void setValue(HashMap<String, Integer> value) {
            this.value = value;
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

    public static class ArrayListConverter implements AttributeConverter<ArrayList<String>> {
        public ArrayListConverter() {
        }

        @Override
        public AttributeValue transformFrom(ArrayList<String> input) {
            List<AttributeValue> values = new ArrayList<>();
            for (String member : input) {
                values.add(AttributeValue.fromS(member));
            }
            return AttributeValue.fromL(values);
        }

        @Override
        public ArrayList<String> transformTo(AttributeValue input) {
            ArrayList<String> result = new ArrayList<>();
            for (AttributeValue member : input.l()) {
                result.add(member.s());
            }
            return result;
        }

        @Override
        public EnhancedType<ArrayList<String>> type() {
            return new EnhancedType<ArrayList<String>>() {
            };
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.L;
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
