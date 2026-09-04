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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ListAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tests converter resolution and item conversion for nested and flattened schema attributes.
 * <p>
 * The tests combine bean, immutable, and static schemas with nested collections and null child values. They verify
 * converter provider ownership, failure during child schema construction, flattened child conversion, and flattened
 * string maps that do not require converter provider lookup.
 */
public class NestedSchemaConverterTest {

    @BeforeEach
    void clearSchemaCaches() {
        invokeClearSchemaCache(BeanTableSchema.class);
        invokeClearSchemaCache(ImmutableTableSchema.class);
        NestedNullBeanOuter.childSetterCalls.set(0);
        NestedBeanOuterWithNullImmutable.childSetterCalls.set(0);
        NestedImmutableOuterWithNullBean.Builder.childSetterCalls.set(0);
        RecordingCustomProvider.reset();
        RecordingRequestedTypesProvider.reset();
    }

    @Test
    @DisplayName("Nested bean list of unsupported type reports the missing member converter")
    void fromBean_whenNestedBeanWithUnsupportedList_throwsConverterNotFoundForMemberType() {

        assertThatThrownBy(() -> TableSchema.fromBean(OuterBeanWithUnsupportedList.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @Test
    @DisplayName("Nested immutable list of Object fails converter lookup for its member type")
    void fromImmutableClass_whenNestedImmutableWithObjectList_throwsConverterNotFoundForMemberType() {
        assertThatThrownBy(() -> TableSchema.fromImmutableClass(OuterImmutableWithObjectList.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @Test
    @DisplayName("Bean containing a nonnull bean round-trips nested collections")
    void itemToMapThenMapToItem_whenBeanContainingNonnullBean_roundTripsNestedCollections() {
        NestedBeanChild child = populatedChildBean("child-1", 7, "nested", "clicked");
        NestedBeanOuter item = populatedOuterBean("id-1", 3, "root", "opened", child);

        TableSchema<NestedBeanOuter> schema = TableSchema.fromBean(NestedBeanOuter.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        NestedBeanOuter read = schema.mapToItem(map);

        assertThat(map.get("child").hasM()).isTrue();
        assertCollectionAttributeTypes(map);
        assertCollectionAttributeTypes(map.get("child").m());
        assertThat(read).isEqualTo(item);
        assertReconstructedCollections(read.getCounters(), read.getLabels(), read.getEvents(),
                                       item.getCounters(), item.getLabels(), item.getEvents());
        assertReconstructedCollections(read.getChild().getCounters(), read.getChild().getLabels(),
                                       read.getChild().getEvents(), child.getCounters(), child.getLabels(),
                                       child.getEvents());
    }

    @Test
    @DisplayName("Bean containing a nonnull immutable round-trips nested collections")
    void itemToMapThenMapToItem_whenBeanContainingNonnullImmutable_roundTripsNestedCollections() {
        NestedImmutableChild child = populatedImmutableChild("child-1", 7, "nested", "clicked");
        NestedBeanOuterWithImmutable item = new NestedBeanOuterWithImmutable();
        item.setId("id-1");
        item.setCounters(counters(3));
        item.setLabels(labels("root"));
        item.setEvents(events("opened"));
        item.setChild(child);

        TableSchema<NestedBeanOuterWithImmutable> schema = TableSchema.fromBean(NestedBeanOuterWithImmutable.class);
        TableSchema.fromImmutableClass(NestedImmutableChild.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        NestedBeanOuterWithImmutable read = schema.mapToItem(map);

        assertThat(map.get("child").hasM()).isTrue();
        assertCollectionAttributeTypes(map.get("child").m());
        assertReconstructedCollections(read.getChild().counters(), read.getChild().labels(),
                                       read.getChild().events(), child.counters(), child.labels(),
                                       child.events());
    }

    @Test
    @DisplayName("Immutable containing a nonnull bean round-trips nested collections")
    void itemToMapThenMapToItem_whenImmutableContainingNonnullBean_roundTripsNestedCollections() {
        NestedBeanChildForImmutable child = populatedChildForImmutable("child-1", 7, "nested", "clicked");
        NestedImmutableOuter item = NestedImmutableOuter.builder()
            .id("id-1")
            .counters(counters(3))
            .labels(labels("root"))
            .events(events("opened"))
            .child(child)
            .build();

        TableSchema<NestedImmutableOuter> schema = TableSchema.fromImmutableClass(NestedImmutableOuter.class);
        TableSchema.fromBean(NestedBeanChildForImmutable.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        NestedImmutableOuter read = schema.mapToItem(map);

        assertThat(map.get("child").hasM()).isTrue();
        assertCollectionAttributeTypes(map.get("child").m());
        assertReconstructedCollections(read.child().getCounters(), read.child().getLabels(),
                                       read.child().getEvents(), child.getCounters(), child.getLabels(),
                                       child.getEvents());
    }

    @Test
    @DisplayName("Bean containing a null bean writes NULL and skips the child setter")
    void itemToMapThenMapToItem_whenBeanContainingNullBean_writesNulAndSkipsChildSetter() {
        NestedNullBeanOuter item = new NestedNullBeanOuter();
        item.setId("id-1");
        item.setCounters(counters(3));
        item.setLabels(labels("root"));
        item.setEvents(events("opened"));
        NestedNullBeanOuter.childSetterCalls.set(0);

        TableSchema<NestedNullBeanOuter> schema = TableSchema.fromBean(NestedNullBeanOuter.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, false);
        NestedNullBeanOuter read = schema.mapToItem(map);

        assertThat(map.get("child")).isEqualTo(AttributeValue.fromNul(true));
        assertCollectionAttributeTypes(map);
        assertThat(NestedNullBeanOuter.childSetterCalls.get()).isZero();
        assertThat(read.getChild()).isNull();
        assertReconstructedCollections(read.getCounters(), read.getLabels(), read.getEvents(),
                                       item.getCounters(), item.getLabels(), item.getEvents());
    }

    @Test
    @DisplayName("Bean containing a null immutable writes NULL and leaves the child null")
    void itemToMapThenMapToItem_whenBeanContainingNullImmutable_writesNulAndLeavesChildNull() {
        NestedBeanOuterWithNullImmutable item = new NestedBeanOuterWithNullImmutable();
        item.setId("id-1");
        item.setCounters(counters(3));
        item.setLabels(labels("root"));
        item.setEvents(events("opened"));
        NestedBeanOuterWithNullImmutable.childSetterCalls.set(0);

        TableSchema<NestedBeanOuterWithNullImmutable> schema = TableSchema.fromBean(NestedBeanOuterWithNullImmutable.class);
        TableSchema.fromImmutableClass(NestedNullImmutableChild.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, false);
        NestedBeanOuterWithNullImmutable read = schema.mapToItem(map);

        assertThat(map.get("child")).isEqualTo(AttributeValue.fromNul(true));
        assertThat(NestedBeanOuterWithNullImmutable.childSetterCalls.get()).isZero();
        assertThat(read.getChild()).isNull();
        assertReconstructedCollections(read.getCounters(), read.getLabels(), read.getEvents(),
                                       item.getCounters(), item.getLabels(), item.getEvents());
    }

    @Test
    @DisplayName("Immutable containing a null bean writes NULL and skips the child builder method")
    void itemToMapThenMapToItem_whenImmutableContainingNullBean_writesNulAndSkipsChildBuilder() {
        NestedImmutableOuterWithNullBean item = NestedImmutableOuterWithNullBean.builder()
            .id("id-1")
            .counters(counters(3))
            .labels(labels("root"))
            .events(events("opened"))
            .build();
        NestedImmutableOuterWithNullBean.Builder.childSetterCalls.set(0);

        TableSchema<NestedImmutableOuterWithNullBean> schema =
            TableSchema.fromImmutableClass(NestedImmutableOuterWithNullBean.class);
        TableSchema.fromBean(NestedBeanChildForNullImmutable.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, false);
        NestedImmutableOuterWithNullBean read = schema.mapToItem(map);

        assertThat(map.get("child")).isEqualTo(AttributeValue.fromNul(true));
        assertThat(NestedImmutableOuterWithNullBean.Builder.childSetterCalls.get()).isZero();
        assertThat(read.child()).isNull();
        assertReconstructedCollections(read.counters(), read.labels(), read.events(),
                                       item.counters(), item.labels(), item.events());
    }

    @Test
    @DisplayName("Flattened bean child attributes use the child's default converters")
    void itemToMapThenMapToItem_whenFlattenedBeanChild_usesChildCollectionConverters() {
        FlattenedChildBean child = new FlattenedChildBean();
        child.setCounters(counters(3));
        child.setLabels(labels("root"));
        child.setEvents(events("opened"));
        OuterFlattenedBean item = new OuterFlattenedBean();
        item.setId("id-1");
        item.setChild(child);

        TableSchema<OuterFlattenedBean> schema = TableSchema.fromBean(OuterFlattenedBean.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        OuterFlattenedBean read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("child")).isNull();
        assertThat(schema.converterForAttribute("counters")).isInstanceOf(MapAttributeConverter.class);
        assertThat(schema.converterForAttribute("counters").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(schema.converterForAttribute("labels")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("labels").attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(schema.converterForAttribute("events")).isInstanceOf(ListAttributeConverter.class);
        assertThat(schema.converterForAttribute("events").attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(map.get("counters").hasM()).isTrue();
        assertThat(map.get("labels").hasSs()).isTrue();
        assertThat(map.get("events").hasL()).isTrue();
        assertThat(read.getChild().getCounters()).isEqualTo(child.getCounters()).isInstanceOf(LinkedHashMap.class);
        assertThat(read.getChild().getLabels()).isEqualTo(child.getLabels()).isInstanceOf(LinkedHashSet.class);
        assertThat(read.getChild().getEvents()).isEqualTo(child.getEvents()).isInstanceOf(ArrayList.class);
    }

    @Test
    @DisplayName("Flattened bean construction fails when the child converter lookup fails")
    void fromBean_whenFlattenedBeanChildWithUnsupportedType_throwsConverterNotFound() {
        EnhancedType<UnsupportedType> type = EnhancedType.of(UnsupportedType.class);

        assertThatThrownBy(() -> TableSchema.fromBean(OuterFlattenedUnsupportedBean.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @Test
    @DisplayName("Flattened bean child uses the child's custom provider")
    void itemToMapThenMapToItem_whenFlattenedBeanChildCustomProvider_selectsCustomTypeConverter() {
        EnhancedType<CustomType> type = EnhancedType.of(CustomType.class);
        FlattenedCustomChild child = new FlattenedCustomChild();
        child.setValue(new CustomType("x"));
        OuterFlattenedCustomBean item = new OuterFlattenedCustomBean();
        item.setId("id-1");
        item.setChild(child);

        TableSchema<OuterFlattenedCustomBean> schema = TableSchema.fromBean(OuterFlattenedCustomBean.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        OuterFlattenedCustomBean read = schema.mapToItem(map);

        assertThat(RecordingCustomProvider.current().requestedTypes()).contains(type);
        assertThat(schema.converterForAttribute("value")).isInstanceOf(CustomTypeConverter.class);
        assertThat(schema.converterForAttribute("value").attributeValueType()).isEqualTo(AttributeValueType.S);
        assertThat(map.get("value").s()).isEqualTo("custom:x");
        assertThat(read.getChild().getValue()).isEqualTo(child.getValue());
    }

    @Test
    @DisplayName("Flattened string map does not consult a converter provider")
    void itemToMapThenMapToItem_whenFlattenedStringMap_doesNotConsultProviderForMapType() {
        EnhancedType<Map<String, String>> type = EnhancedType.mapOf(String.class, String.class);
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("attr1", "value1");
        FlattenMapBean item = new FlattenMapBean();
        item.setId("id-1");
        item.setAttributes(attributes);

        TableSchema<FlattenMapBean> schema = TableSchema.fromBean(FlattenMapBean.class);
        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        FlattenMapBean read = schema.mapToItem(map);

        assertThat(RecordingRequestedTypesProvider.current().requestedTypes()).doesNotContain(type);
        assertThat(schema.converterForAttribute("attr1")).isNull();
        assertThat(map.get("attr1").s()).isEqualTo("value1");
        assertThat(map.get("attr1").hasM()).isFalse();
        assertThat(read.getAttributes()).isEqualTo(attributes);
    }

    @Test
    @DisplayName("Static flattened string map does not consult a converter provider")
    void itemToMapThenMapToItem_whenStaticFlattenedStringMap_doesNotConsultProviderForMapType() {
        EnhancedType<Map<String, String>> type = EnhancedType.mapOf(String.class, String.class);
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("attr1", "value1");
        FlattenMapItem item = new FlattenMapItem();
        item.setId("id-1");
        item.setAttributes(attributes);
        RecordingRequestedTypesProvider provider = new RecordingRequestedTypesProvider();

        StaticTableSchema<FlattenMapItem> schema = StaticTableSchema.builder(FlattenMapItem.class)
            .newItemSupplier(FlattenMapItem::new)
            .attributeConverterProviders(provider, DefaultAttributeConverterProvider.create())
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(FlattenMapItem::getId).setter(FlattenMapItem::setId).tags(primaryPartitionKey())
                .attributeConverter(new StringAttributeConverter()))
            .flatten("attributes", FlattenMapItem::getAttributes, FlattenMapItem::setAttributes)
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        FlattenMapItem read = schema.mapToItem(map);

        assertThat(provider.requestedTypes()).doesNotContain(type);
        assertThat(schema.converterForAttribute("attr1")).isNull();
        assertThat(map.get("attr1").s()).isEqualTo("value1");
        assertThat(read.getAttributes()).isEqualTo(attributes);
    }

    @Test
    @DisplayName("Static flattened child schema uses the supplied schema converters")
    void itemToMapThenMapToItem_whenStaticFlattenedChildSchema_usesChildCollectionConverters() {
        FlattenedChildItem child = new FlattenedChildItem();
        child.setCounters(counters(3));
        child.setLabels(labels("root"));
        child.setEvents(events("opened"));
        FlattenedOuterItem item = new FlattenedOuterItem();
        item.setId("id-1");
        item.setChild(child);

        StaticTableSchema<FlattenedChildItem> childSchema = StaticTableSchema.builder(FlattenedChildItem.class)
            .newItemSupplier(FlattenedChildItem::new)
            .addAttribute(EnhancedType.mapOf(String.class, Integer.class), a -> a.name("counters")
                .getter(FlattenedChildItem::getCounters).setter(FlattenedChildItem::setCounters))
            .addAttribute(EnhancedType.setOf(String.class), a -> a.name("labels")
                .getter(FlattenedChildItem::getLabels).setter(FlattenedChildItem::setLabels))
            .addAttribute(EnhancedType.listOf(String.class), a -> a.name("events")
                .getter(FlattenedChildItem::getEvents).setter(FlattenedChildItem::setEvents))
            .build();

        StaticTableSchema<FlattenedOuterItem> schema = StaticTableSchema.builder(FlattenedOuterItem.class)
            .newItemSupplier(FlattenedOuterItem::new)
            .addAttribute(EnhancedType.of(String.class), a -> a.name("id")
                .getter(FlattenedOuterItem::getId).setter(FlattenedOuterItem::setId).tags(primaryPartitionKey()))
            .flatten(childSchema, FlattenedOuterItem::getChild, FlattenedOuterItem::setChild)
            .build();

        Map<String, AttributeValue> map = schema.itemToMap(item, true);
        FlattenedOuterItem read = schema.mapToItem(map);

        assertThat(schema.converterForAttribute("counters")).isInstanceOf(MapAttributeConverter.class);
        assertThat(schema.converterForAttribute("counters").attributeValueType()).isEqualTo(AttributeValueType.M);
        assertThat(schema.converterForAttribute("labels")).isInstanceOf(SetAttributeConverter.class);
        assertThat(schema.converterForAttribute("labels").attributeValueType()).isEqualTo(AttributeValueType.SS);
        assertThat(schema.converterForAttribute("events")).isInstanceOf(ListAttributeConverter.class);
        assertThat(schema.converterForAttribute("events").attributeValueType()).isEqualTo(AttributeValueType.L);
        assertThat(map.get("counters").hasM()).isTrue();
        assertThat(map.get("labels").hasSs()).isTrue();
        assertThat(map.get("events").hasL()).isTrue();
        assertThat(read.getChild().getCounters()).isEqualTo(child.getCounters()).isInstanceOf(LinkedHashMap.class);
        assertThat(read.getChild().getLabels()).isEqualTo(child.getLabels()).isInstanceOf(LinkedHashSet.class);
        assertThat(read.getChild().getEvents()).isEqualTo(child.getEvents()).isInstanceOf(ArrayList.class);
    }

    private static void invokeClearSchemaCache(Class<?> schemaClass) {
        try {
            Method method = schemaClass.getDeclaredMethod("clearSchemaCache");
            method.setAccessible(true);
            method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Integer> counters(int value) {
        Map<String, Integer> counters = new LinkedHashMap<>();
        counters.put("count", value);
        return counters;
    }

    private static Set<String> labels(String label) {
        Set<String> labels = new LinkedHashSet<>();
        labels.add(label);
        return labels;
    }

    private static List<String> events(String event) {
        List<String> events = new ArrayList<>();
        events.add(event);
        return events;
    }

    private static void assertCollectionAttributeTypes(Map<String, AttributeValue> map) {
        assertThat(map.get("counters").hasM()).isTrue();
        assertThat(map.get("labels").hasSs()).isTrue();
        assertThat(map.get("events").hasL()).isTrue();
    }

    private static void assertReconstructedCollections(Map<String, Integer> counters, Set<String> labels,
                                                       List<String> events, Map<String, Integer> expectedCounters,
                                                       Set<String> expectedLabels, List<String> expectedEvents) {
        assertThat(counters).isEqualTo(expectedCounters).isInstanceOf(LinkedHashMap.class);
        assertThat(labels).isEqualTo(expectedLabels).isInstanceOf(LinkedHashSet.class);
        assertThat(events).isEqualTo(expectedEvents).isInstanceOf(ArrayList.class);
    }

    private static NestedBeanChild populatedChildBean(String id, int count, String label, String event) {
        NestedBeanChild child = new NestedBeanChild();
        child.setId(id);
        child.setCounters(counters(count));
        child.setLabels(labels(label));
        child.setEvents(events(event));
        return child;
    }

    private static NestedBeanOuter populatedOuterBean(String id, int count, String label, String event,
                                                  NestedBeanChild child) {
        NestedBeanOuter item = new NestedBeanOuter();
        item.setId(id);
        item.setCounters(counters(count));
        item.setLabels(labels(label));
        item.setEvents(events(event));
        item.setChild(child);
        return item;
    }

    private static NestedImmutableChild populatedImmutableChild(String id, int count, String label, String event) {
        return NestedImmutableChild.builder()
            .id(id)
            .counters(counters(count))
            .labels(labels(label))
            .events(events(event))
            .build();
    }

    private static NestedBeanChildForImmutable populatedChildForImmutable(String id, int count, String label,
                                                                      String event) {
        NestedBeanChildForImmutable child = new NestedBeanChildForImmutable();
        child.setId(id);
        child.setCounters(counters(count));
        child.setLabels(labels(label));
        child.setEvents(events(event));
        return child;
    }

    @DynamoDbBean
    public static class OuterBeanWithUnsupportedList {
        private NestedBeanWithUnsupportedList child;

        public NestedBeanWithUnsupportedList getChild() {
            return child;
        }

        public void setChild(NestedBeanWithUnsupportedList child) {
            this.child = child;
        }
    }

    @DynamoDbBean
    public static class NestedBeanWithUnsupportedList {
        private List<UnsupportedType> value;

        public List<UnsupportedType> getValue() {
            return value;
        }

        public void setValue(List<UnsupportedType> value) {
            this.value = value;
        }
    }

    @DynamoDbImmutable(builder = OuterImmutableWithObjectList.Builder.class)
    public static final class OuterImmutableWithObjectList {
        private final NestedImmutableWithObjectList child;

        private OuterImmutableWithObjectList(Builder b) {
            this.child = b.child;
        }

        public static Builder builder() {
            return new Builder();
        }

        public NestedImmutableWithObjectList child() {
            return child;
        }

        public static final class Builder {
            private NestedImmutableWithObjectList child;

            public Builder child(NestedImmutableWithObjectList child) {
                this.child = child;
                return this;
            }

            public OuterImmutableWithObjectList build() {
                return new OuterImmutableWithObjectList(this);
            }
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableWithObjectList.Builder.class)
    public static final class NestedImmutableWithObjectList {
        private final List<Object> value;

        private NestedImmutableWithObjectList(Builder b) {
            this.value = b.value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public List<Object> value() {
            return value;
        }

        public static final class Builder {
            private List<Object> value;

            public Builder value(List<Object> value) {
                this.value = value;
                return this;
            }

            public NestedImmutableWithObjectList build() {
                return new NestedImmutableWithObjectList(this);
            }
        }
    }

    @DynamoDbBean
    public static class NestedBeanChild {
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NestedBeanChild)) {
                return false;
            }
            NestedBeanChild that = (NestedBeanChild) o;
            return Objects.equals(id, that.id)
                   && Objects.equals(counters, that.counters)
                   && Objects.equals(labels, that.labels)
                   && Objects.equals(events, that.events);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, counters, labels, events);
        }
    }

    @DynamoDbBean
    public static class NestedBeanOuter {
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private NestedBeanChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }

        public NestedBeanChild getChild() {
            return child;
        }

        public void setChild(NestedBeanChild child) {
            this.child = child;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NestedBeanOuter)) {
                return false;
            }
            NestedBeanOuter that = (NestedBeanOuter) o;
            return Objects.equals(id, that.id)
                   && Objects.equals(counters, that.counters)
                   && Objects.equals(labels, that.labels)
                   && Objects.equals(events, that.events)
                   && Objects.equals(child, that.child);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, counters, labels, events, child);
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableChild.Builder.class)
    public static final class NestedImmutableChild {
        private final String id;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;

        private NestedImmutableChild(Builder b) {
            this.id = b.id;
            this.counters = b.counters;
            this.labels = b.labels;
            this.events = b.events;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public Map<String, Integer> counters() {
            return counters;
        }

        public Set<String> labels() {
            return labels;
        }

        public List<String> events() {
            return events;
        }

        public static final class Builder {
            private String id;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder counters(Map<String, Integer> counters) {
                this.counters = counters;
                return this;
            }

            public Builder labels(Set<String> labels) {
                this.labels = labels;
                return this;
            }

            public Builder events(List<String> events) {
                this.events = events;
                return this;
            }

            public NestedImmutableChild build() {
                return new NestedImmutableChild(this);
            }
        }
    }

    @DynamoDbBean
    public static class NestedBeanOuterWithImmutable {
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private NestedImmutableChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }

        public NestedImmutableChild getChild() {
            return child;
        }

        public void setChild(NestedImmutableChild child) {
            this.child = child;
        }
    }

    @DynamoDbBean
    public static class NestedBeanChildForImmutable {
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableOuter.Builder.class)
    public static final class NestedImmutableOuter {
        private final String id;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;
        private final NestedBeanChildForImmutable child;

        private NestedImmutableOuter(Builder b) {
            this.id = b.id;
            this.counters = b.counters;
            this.labels = b.labels;
            this.events = b.events;
            this.child = b.child;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public Map<String, Integer> counters() {
            return counters;
        }

        public Set<String> labels() {
            return labels;
        }

        public List<String> events() {
            return events;
        }

        public NestedBeanChildForImmutable child() {
            return child;
        }

        public static final class Builder {
            private String id;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;
            private NestedBeanChildForImmutable child;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder counters(Map<String, Integer> counters) {
                this.counters = counters;
                return this;
            }

            public Builder labels(Set<String> labels) {
                this.labels = labels;
                return this;
            }

            public Builder events(List<String> events) {
                this.events = events;
                return this;
            }

            public Builder child(NestedBeanChildForImmutable child) {
                this.child = child;
                return this;
            }

            public NestedImmutableOuter build() {
                return new NestedImmutableOuter(this);
            }
        }
    }

    @DynamoDbBean
    public static class NestedNullBeanChild {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @DynamoDbBean
    public static class NestedNullBeanOuter {
        static final AtomicInteger childSetterCalls = new AtomicInteger();
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private NestedNullBeanChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }

        public NestedNullBeanChild getChild() {
            return child;
        }

        public void setChild(NestedNullBeanChild child) {
            childSetterCalls.incrementAndGet();
            this.child = child;
        }
    }

    @DynamoDbImmutable(builder = NestedNullImmutableChild.Builder.class)
    public static final class NestedNullImmutableChild {
        private final String id;

        private NestedNullImmutableChild(Builder b) {
            this.id = b.id;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public static final class Builder {
            private String id;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public NestedNullImmutableChild build() {
                return new NestedNullImmutableChild(this);
            }
        }
    }

    @DynamoDbBean
    public static class NestedBeanOuterWithNullImmutable {
        static final AtomicInteger childSetterCalls = new AtomicInteger();
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private NestedNullImmutableChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }

        public NestedNullImmutableChild getChild() {
            return child;
        }

        public void setChild(NestedNullImmutableChild child) {
            childSetterCalls.incrementAndGet();
            this.child = child;
        }
    }

    @DynamoDbBean
    public static class NestedBeanChildForNullImmutable {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @DynamoDbImmutable(builder = NestedImmutableOuterWithNullBean.Builder.class)
    public static final class NestedImmutableOuterWithNullBean {
        private final String id;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;
        private final NestedBeanChildForNullImmutable child;

        private NestedImmutableOuterWithNullBean(Builder b) {
            this.id = b.id;
            this.counters = b.counters;
            this.labels = b.labels;
            this.events = b.events;
            this.child = b.child;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public Map<String, Integer> counters() {
            return counters;
        }

        public Set<String> labels() {
            return labels;
        }

        public List<String> events() {
            return events;
        }

        public NestedBeanChildForNullImmutable child() {
            return child;
        }

        public static final class Builder {
            static final AtomicInteger childSetterCalls = new AtomicInteger();
            private String id;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;
            private NestedBeanChildForNullImmutable child;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder counters(Map<String, Integer> counters) {
                this.counters = counters;
                return this;
            }

            public Builder labels(Set<String> labels) {
                this.labels = labels;
                return this;
            }

            public Builder events(List<String> events) {
                this.events = events;
                return this;
            }

            public Builder child(NestedBeanChildForNullImmutable child) {
                childSetterCalls.incrementAndGet();
                this.child = child;
                return this;
            }

            public NestedImmutableOuterWithNullBean build() {
                return new NestedImmutableOuterWithNullBean(this);
            }
        }
    }

    @DynamoDbBean
    public static class FlattenedChildBean {
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }
    }

    @DynamoDbBean
    public static class OuterFlattenedBean {
        private String id;
        private FlattenedChildBean child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbFlatten
        public FlattenedChildBean getChild() {
            return child;
        }

        public void setChild(FlattenedChildBean child) {
            this.child = child;
        }
    }

    @DynamoDbBean
    public static class FlattenedUnsupportedChild {
        private UnsupportedType value;

        public UnsupportedType getValue() {
            return value;
        }

        public void setValue(UnsupportedType value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class OuterFlattenedUnsupportedBean {
        private String id;
        private FlattenedUnsupportedChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbFlatten
        public FlattenedUnsupportedChild getChild() {
            return child;
        }

        public void setChild(FlattenedUnsupportedChild child) {
            this.child = child;
        }
    }

    @DynamoDbBean(converterProviders = {
        RecordingCustomProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class FlattenedCustomChild {
        private CustomType value;

        public CustomType getValue() {
            return value;
        }

        public void setValue(CustomType value) {
            this.value = value;
        }
    }

    @DynamoDbBean
    public static class OuterFlattenedCustomBean {
        private String id;
        private FlattenedCustomChild child;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbFlatten
        public FlattenedCustomChild getChild() {
            return child;
        }

        public void setChild(FlattenedCustomChild child) {
            this.child = child;
        }
    }

    @DynamoDbBean(converterProviders = {
        RecordingRequestedTypesProvider.class,
        DefaultAttributeConverterProvider.class
    })
    public static class FlattenMapBean {
        private String id;
        private Map<String, String> attributes;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbFlatten
        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
    }

    static final class FlattenMapItem {
        private String id;
        private Map<String, String> attributes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
    }

    static final class FlattenedChildItem {
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;

        public Map<String, Integer> getCounters() {
            return counters;
        }

        public void setCounters(Map<String, Integer> counters) {
            this.counters = counters;
        }

        public Set<String> getLabels() {
            return labels;
        }

        public void setLabels(Set<String> labels) {
            this.labels = labels;
        }

        public List<String> getEvents() {
            return events;
        }

        public void setEvents(List<String> events) {
            this.events = events;
        }
    }

    static final class FlattenedOuterItem {
        private String id;
        private FlattenedChildItem child;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public FlattenedChildItem getChild() {
            return child;
        }

        public void setChild(FlattenedChildItem child) {
            this.child = child;
        }
    }

    public static final class UnsupportedType {
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

    public static final class RecordingCustomProvider implements AttributeConverterProvider {
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

    public static final class RecordingRequestedTypesProvider implements AttributeConverterProvider {
        private static final ThreadLocal<RecordingRequestedTypesProvider> CURRENT =
            new ThreadLocal<>();
        private final List<EnhancedType<?>> requestedTypes = new ArrayList<>();

        public RecordingRequestedTypesProvider() {
            CURRENT.set(this);
        }

        public static RecordingRequestedTypesProvider current() {
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
}
