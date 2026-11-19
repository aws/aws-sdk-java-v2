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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Shared models, collection fixtures, and DynamoDB attribute maps for DefaultAttributeConverterProvider table tests.
 */
public final class DefaultAttributeConverterProviderTestModels {

    public static final String TABLE_NAME = "TABLE_NAME";
    public static final String ITEM_ID = "id-1";
    public static final String COUNTER_KEY = "count";
    public static final Integer COUNTER_VALUE = 1;
    public static final String LABEL_VALUE = "red";
    public static final String EVENT_VALUE = "created";
    public static final String DIRECT_VALUE = "plain";
    public static final String DETAILS_NOTE = "note-1";

    private DefaultAttributeConverterProviderTestModels() {
    }

    public static LinkedHashMap<String, Integer> oneCounter() {
        LinkedHashMap<String, Integer> counters = new LinkedHashMap<>();
        counters.put(COUNTER_KEY, COUNTER_VALUE);
        return counters;
    }

    public static LinkedHashSet<String> oneLabel() {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(LABEL_VALUE);
        return labels;
    }

    public static ArrayList<String> oneEvent() {
        ArrayList<String> events = new ArrayList<>();
        events.add(EVENT_VALUE);
        return events;
    }

    public static AttributeValue idAttribute() {
        return AttributeValue.builder().s(ITEM_ID).build();
    }

    public static AttributeValue countersAttribute() {
        return AttributeValue.builder()
                             .m(Collections.singletonMap(COUNTER_KEY,
                                                         AttributeValue.builder().n("1").build()))
                             .build();
    }

    public static AttributeValue labelsAttribute() {
        return AttributeValue.builder().ss(LABEL_VALUE).build();
    }

    public static AttributeValue eventsAttribute() {
        return AttributeValue.builder()
                             .l(AttributeValue.builder().s(EVENT_VALUE).build())
                             .build();
    }

    public static AttributeValue eventsWithNullElementAttribute() {
        List<AttributeValue> members = new ArrayList<>();
        members.add(AttributeValue.builder().s("a").build());
        members.add(AttributeValue.fromNul(true));
        members.add(AttributeValue.builder().s("b").build());
        return AttributeValue.builder().l(members).build();
    }

    public static Map<String, AttributeValue> completeItem() {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("id", idAttribute());
        item.put("counters", countersAttribute());
        item.put("labels", labelsAttribute());
        item.put("events", eventsAttribute());
        return item;
    }

    public static Map<String, AttributeValue> completeItemWithNullableNull() {
        Map<String, AttributeValue> item = new LinkedHashMap<>(completeItem());
        item.put("nullable", AttributeValue.fromNul(true));
        return item;
    }

    public static ConverterRecord populatedConverterRecord() {
        ConverterRecord record = new ConverterRecord();
        record.setId(ITEM_ID);
        record.setCounters(oneCounter());
        record.setLabels(oneLabel());
        record.setEvents(oneEvent());
        return record;
    }

    public static void assertWrittenItem(Map<String, AttributeValue> item) {
        assertThat(item.get("id").s()).isEqualTo(ITEM_ID);
        assertThat(item.get("counters")).isEqualTo(countersAttribute());
        assertThat(item.get("labels")).isEqualTo(labelsAttribute());
        assertThat(item.get("events")).isEqualTo(eventsAttribute());
    }

    public static void assertReconstructedCollections(ConverterRecord record) {
        assertThat(record.getId()).isEqualTo(ITEM_ID);
        assertThat(record.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
        assertThat(record.getLabels()).isInstanceOf(LinkedHashSet.class).isEqualTo(oneLabel());
        assertThat(record.getEvents()).isInstanceOf(ArrayList.class).isEqualTo(oneEvent());
    }

    public static ConverterImmutable populatedConverterImmutable() {
        return ConverterImmutable.builder()
                                 .id(ITEM_ID)
                                 .counters(oneCounter())
                                 .labels(oneLabel())
                                 .events(oneEvent())
                                 .build();
    }

    public static StaticRecord populatedStaticRecord() {
        StaticRecord record = new StaticRecord();
        record.setId(ITEM_ID);
        record.setCounters(oneCounter());
        record.setLabels(oneLabel());
        record.setEvents(oneEvent());
        return record;
    }

    public static StaticImmutableRecord populatedStaticImmutableRecord() {
        return StaticImmutableRecord.builder()
                                    .id(ITEM_ID)
                                    .counters(oneCounter())
                                    .labels(oneLabel())
                                    .events(oneEvent())
                                    .build();
    }

    public static DefaultProviderBean populatedDefaultProviderBean() {
        DefaultProviderBean bean = new DefaultProviderBean();
        bean.setId(ITEM_ID);
        bean.setCounters(oneCounter());
        bean.setLabels(oneLabel());
        bean.setEvents(oneEvent());
        return bean;
    }

    public static DefaultProviderImmutable populatedDefaultProviderImmutable() {
        return DefaultProviderImmutable.builder()
                                       .id(ITEM_ID)
                                       .counters(oneCounter())
                                       .labels(oneLabel())
                                       .events(oneEvent())
                                       .build();
    }

    public static AllBranchesDetails populatedDetails() {
        AllBranchesDetails details = new AllBranchesDetails();
        details.setNote(DETAILS_NOTE);
        return details;
    }

    public static TableSchema<AllBranchesDetails> detailsSchema() {
        return TableSchema.fromBean(AllBranchesDetails.class);
    }

    public static AllBranchesBean populatedAllBranchesBean() {
        AllBranchesBean bean = new AllBranchesBean();
        bean.setId(ITEM_ID);
        bean.setDirect(DIRECT_VALUE);
        bean.setCounters(oneCounter());
        bean.setLabels(oneLabel());
        bean.setEvents(oneEvent());
        bean.setStatus(TestEnum.OPEN);
        bean.setDetails(populatedDetails());
        return bean;
    }

    public static AllBranchesImmutable populatedAllBranchesImmutable() {
        return AllBranchesImmutable.builder()
                                   .id(ITEM_ID)
                                   .direct(DIRECT_VALUE)
                                   .counters(oneCounter())
                                   .labels(oneLabel())
                                   .events(oneEvent())
                                   .status(TestEnum.OPEN)
                                   .details(populatedDetails())
                                   .build();
    }

    public static AllBranchesStaticRecord populatedAllBranchesStaticRecord() {
        AllBranchesStaticRecord record = new AllBranchesStaticRecord();
        record.setId(ITEM_ID);
        record.setDirect(DIRECT_VALUE);
        record.setCounters(oneCounter());
        record.setLabels(oneLabel());
        record.setEvents(oneEvent());
        record.setStatus(TestEnum.OPEN);
        record.setDetails(populatedDetails());
        return record;
    }

    public static AllBranchesStaticImmutable populatedAllBranchesStaticImmutable() {
        return AllBranchesStaticImmutable.builder()
                                         .id(ITEM_ID)
                                         .direct(DIRECT_VALUE)
                                         .counters(oneCounter())
                                         .labels(oneLabel())
                                         .events(oneEvent())
                                         .status(TestEnum.OPEN)
                                         .details(populatedDetails())
                                         .build();
    }

    public static Map<String, AttributeValue> allBranchesItem() {
        Map<String, AttributeValue> details = new LinkedHashMap<>();
        details.put("note", AttributeValue.builder().s(DETAILS_NOTE).build());
        Map<String, AttributeValue> item = new LinkedHashMap<>(completeItem());
        item.put("direct", AttributeValue.builder().s(DIRECT_VALUE).build());
        item.put("status", AttributeValue.builder().s(TestEnum.OPEN.toString()).build());
        item.put("details", AttributeValue.builder().m(details).build());
        return item;
    }

    public static EnhancedDocument typedCollectionsDocument() {
        return EnhancedDocument.builder()
                               .putString("id", ITEM_ID)
                               .putMap("counters", oneCounter(),
                                       EnhancedType.of(String.class), EnhancedType.of(Integer.class))
                               .put("labels", oneLabel(), EnhancedType.setOf(String.class))
                               .putList("events", oneEvent(), EnhancedType.of(String.class))
                               .build();
    }

    public static EnhancedDocument typedCollectionsDocumentWithNull() {
        return typedCollectionsDocument().toBuilder()
                                         .putNull("nullable")
                                         .build();
    }

    public static EnhancedDocument allBranchesDocument(TableSchema<AllBranchesDetails> schema) {
        return EnhancedDocument.builder()
                               .putString("id", ITEM_ID)
                               .putString("direct", DIRECT_VALUE)
                               .putMap("counters", oneCounter(),
                                       EnhancedType.of(String.class), EnhancedType.of(Integer.class))
                               .put("labels", oneLabel(), EnhancedType.setOf(String.class))
                               .putList("events", oneEvent(), EnhancedType.of(String.class))
                               .put("status", TestEnum.OPEN, TestEnum.class)
                               .put("details", populatedDetails(),
                                    EnhancedType.documentOf(AllBranchesDetails.class, schema))
                               .build();
    }

    public static StaticTableSchema<StaticRecord> staticRecordSchema() {
        return StaticTableSchema.builder(StaticRecord.class)
                                .newItemSupplier(StaticRecord::new)
                                .addAttribute(String.class, a -> a.name("id")
                                                                  .getter(StaticRecord::getId)
                                                                  .setter(StaticRecord::setId)
                                                                  .tags(primaryPartitionKey()))
                                .addAttribute(EnhancedType.mapOf(String.class, Integer.class),
                                              a -> a.name("counters")
                                                    .getter(StaticRecord::getCounters)
                                                    .setter(StaticRecord::setCounters))
                                .addAttribute(EnhancedType.setOf(String.class),
                                              a -> a.name("labels")
                                                    .getter(StaticRecord::getLabels)
                                                    .setter(StaticRecord::setLabels))
                                .addAttribute(EnhancedType.listOf(String.class),
                                              a -> a.name("events")
                                                    .getter(StaticRecord::getEvents)
                                                    .setter(StaticRecord::setEvents))
                                .build();
    }

    public static StaticImmutableTableSchema<StaticImmutableRecord, StaticImmutableRecord.Builder>
        staticImmutableRecordSchema() {
        return StaticImmutableTableSchema.builder(StaticImmutableRecord.class, StaticImmutableRecord.Builder.class)
                                         .newItemBuilder(StaticImmutableRecord::builder,
                                                         StaticImmutableRecord.Builder::build)
                                         .addAttribute(String.class, a -> a.name("id")
                                                                           .getter(StaticImmutableRecord::id)
                                                                           .setter(StaticImmutableRecord.Builder::id)
                                                                           .tags(primaryPartitionKey()))
                                         .addAttribute(EnhancedType.mapOf(String.class, Integer.class),
                                                       a -> a.name("counters")
                                                             .getter(StaticImmutableRecord::counters)
                                                             .setter(StaticImmutableRecord.Builder::counters))
                                         .addAttribute(EnhancedType.setOf(String.class),
                                                       a -> a.name("labels")
                                                             .getter(StaticImmutableRecord::labels)
                                                             .setter(StaticImmutableRecord.Builder::labels))
                                         .addAttribute(EnhancedType.listOf(String.class),
                                                       a -> a.name("events")
                                                             .getter(StaticImmutableRecord::events)
                                                             .setter(StaticImmutableRecord.Builder::events))
                                         .build();
    }

    public static StaticTableSchema<AllBranchesStaticRecord> allBranchesStaticSchema() {
        TableSchema<AllBranchesDetails> details = detailsSchema();
        return StaticTableSchema.builder(AllBranchesStaticRecord.class)
                                .newItemSupplier(AllBranchesStaticRecord::new)
                                .addAttribute(EnhancedType.of(String.class),
                                              a -> a.name("id")
                                                    .getter(AllBranchesStaticRecord::getId)
                                                    .setter(AllBranchesStaticRecord::setId)
                                                    .tags(primaryPartitionKey()))
                                .addAttribute(EnhancedType.of(String.class),
                                              a -> a.name("direct")
                                                    .getter(AllBranchesStaticRecord::getDirect)
                                                    .setter(AllBranchesStaticRecord::setDirect))
                                .addAttribute(EnhancedType.mapOf(String.class, Integer.class),
                                              a -> a.name("counters")
                                                    .getter(AllBranchesStaticRecord::getCounters)
                                                    .setter(AllBranchesStaticRecord::setCounters))
                                .addAttribute(EnhancedType.setOf(String.class),
                                              a -> a.name("labels")
                                                    .getter(AllBranchesStaticRecord::getLabels)
                                                    .setter(AllBranchesStaticRecord::setLabels))
                                .addAttribute(EnhancedType.listOf(String.class),
                                              a -> a.name("events")
                                                    .getter(AllBranchesStaticRecord::getEvents)
                                                    .setter(AllBranchesStaticRecord::setEvents))
                                .addAttribute(EnhancedType.of(TestEnum.class),
                                              a -> a.name("status")
                                                    .getter(AllBranchesStaticRecord::getStatus)
                                                    .setter(AllBranchesStaticRecord::setStatus))
                                .addAttribute(EnhancedType.documentOf(AllBranchesDetails.class, details),
                                              a -> a.name("details")
                                                    .getter(AllBranchesStaticRecord::getDetails)
                                                    .setter(AllBranchesStaticRecord::setDetails))
                                .build();
    }

    public static StaticImmutableTableSchema<AllBranchesStaticImmutable, AllBranchesStaticImmutable.Builder>
        allBranchesStaticImmutableSchema() {
        TableSchema<AllBranchesDetails> details = detailsSchema();
        return StaticImmutableTableSchema.builder(AllBranchesStaticImmutable.class,
                                                  AllBranchesStaticImmutable.Builder.class)
                                         .newItemBuilder(AllBranchesStaticImmutable::builder,
                                                         AllBranchesStaticImmutable.Builder::build)
                                         .addAttribute(EnhancedType.of(String.class),
                                                       a -> a.name("id")
                                                             .getter(AllBranchesStaticImmutable::id)
                                                             .setter(AllBranchesStaticImmutable.Builder::id)
                                                             .tags(primaryPartitionKey()))
                                         .addAttribute(EnhancedType.of(String.class),
                                                       a -> a.name("direct")
                                                             .getter(AllBranchesStaticImmutable::direct)
                                                             .setter(AllBranchesStaticImmutable.Builder::direct))
                                         .addAttribute(EnhancedType.mapOf(String.class, Integer.class),
                                                       a -> a.name("counters")
                                                             .getter(AllBranchesStaticImmutable::counters)
                                                             .setter(AllBranchesStaticImmutable.Builder::counters))
                                         .addAttribute(EnhancedType.setOf(String.class),
                                                       a -> a.name("labels")
                                                             .getter(AllBranchesStaticImmutable::labels)
                                                             .setter(AllBranchesStaticImmutable.Builder::labels))
                                         .addAttribute(EnhancedType.listOf(String.class),
                                                       a -> a.name("events")
                                                             .getter(AllBranchesStaticImmutable::events)
                                                             .setter(AllBranchesStaticImmutable.Builder::events))
                                         .addAttribute(EnhancedType.of(TestEnum.class),
                                                       a -> a.name("status")
                                                             .getter(AllBranchesStaticImmutable::status)
                                                             .setter(AllBranchesStaticImmutable.Builder::status))
                                         .addAttribute(EnhancedType.documentOf(AllBranchesDetails.class, details),
                                                       a -> a.name("details")
                                                             .getter(AllBranchesStaticImmutable::details)
                                                             .setter(AllBranchesStaticImmutable.Builder::details))
                                         .build();
    }

    public enum TestEnum {
        OPEN,
        CLOSED
    }

    public static class UnsupportedType {
    }

    @DynamoDbBean
    public static class ConverterRecord {
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private String nullable;

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

        public String getNullable() {
            return nullable;
        }

        public void setNullable(String nullable) {
            this.nullable = nullable;
        }
    }

    @DynamoDbImmutable(builder = ConverterImmutable.Builder.class)
    public static final class ConverterImmutable {
        private final String id;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;

        private ConverterImmutable(Builder builder) {
            this.id = builder.id;
            this.counters = builder.counters;
            this.labels = builder.labels;
            this.events = builder.events;
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

        public static final class Builder {
            private String id;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;

            private Builder() {
            }

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

            public ConverterImmutable build() {
                return new ConverterImmutable(this);
            }
        }
    }

    public static class StaticRecord {
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

    public static final class StaticImmutableRecord {
        private final String id;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;

        private StaticImmutableRecord(Builder builder) {
            this.id = builder.id;
            this.counters = builder.counters;
            this.labels = builder.labels;
            this.events = builder.events;
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

            private Builder() {
            }

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

            public StaticImmutableRecord build() {
                return new StaticImmutableRecord(this);
            }
        }
    }

    @DynamoDbBean
    public static class DefaultProviderBean {
        private String id;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;

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
    }

    @DynamoDbImmutable(builder = DefaultProviderImmutable.Builder.class)
    public static final class DefaultProviderImmutable {
        private final String id;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;

        private DefaultProviderImmutable(Builder builder) {
            this.id = builder.id;
            this.counters = builder.counters;
            this.labels = builder.labels;
            this.events = builder.events;
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

        public static final class Builder {
            private String id;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;

            private Builder() {
            }

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

            public DefaultProviderImmutable build() {
                return new DefaultProviderImmutable(this);
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
    public static class SingleGsiPartitionBean {
        private String id;
        private String gsiKey;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbSecondaryPartitionKey(indexNames = "gsi")
        public String getGsiKey() {
            return gsiKey;
        }

        public void setGsiKey(String gsiKey) {
            this.gsiKey = gsiKey;
        }
    }

    @DynamoDbBean
    public static class SingleGsiSortBean {
        private String id;
        private String gsiKey;
        private Integer gsiSort;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbSecondaryPartitionKey(indexNames = "gsi")
        public String getGsiKey() {
            return gsiKey;
        }

        public void setGsiKey(String gsiKey) {
            this.gsiKey = gsiKey;
        }

        @DynamoDbSecondarySortKey(indexNames = "gsi")
        public Integer getGsiSort() {
            return gsiSort;
        }

        public void setGsiSort(Integer gsiSort) {
            this.gsiSort = gsiSort;
        }
    }

    @DynamoDbBean
    public static class AllBranchesDetails {
        private String note;

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    @DynamoDbBean
    public static class AllBranchesBean {
        private String id;
        private String direct;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private TestEnum status;
        private AllBranchesDetails details;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDirect() {
            return direct;
        }

        public void setDirect(String direct) {
            this.direct = direct;
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

        public TestEnum getStatus() {
            return status;
        }

        public void setStatus(TestEnum status) {
            this.status = status;
        }

        public AllBranchesDetails getDetails() {
            return details;
        }

        public void setDetails(AllBranchesDetails details) {
            this.details = details;
        }
    }

    @DynamoDbImmutable(builder = AllBranchesImmutable.Builder.class)
    public static final class AllBranchesImmutable {
        private final String id;
        private final String direct;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;
        private final TestEnum status;
        private final AllBranchesDetails details;

        private AllBranchesImmutable(Builder builder) {
            this.id = builder.id;
            this.direct = builder.direct;
            this.counters = builder.counters;
            this.labels = builder.labels;
            this.events = builder.events;
            this.status = builder.status;
            this.details = builder.details;
        }

        public static Builder builder() {
            return new Builder();
        }

        @DynamoDbPartitionKey
        public String id() {
            return id;
        }

        public String direct() {
            return direct;
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

        public TestEnum status() {
            return status;
        }

        public AllBranchesDetails details() {
            return details;
        }

        public static final class Builder {
            private String id;
            private String direct;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;
            private TestEnum status;
            private AllBranchesDetails details;

            private Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder direct(String direct) {
                this.direct = direct;
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

            public Builder status(TestEnum status) {
                this.status = status;
                return this;
            }

            public Builder details(AllBranchesDetails details) {
                this.details = details;
                return this;
            }

            public AllBranchesImmutable build() {
                return new AllBranchesImmutable(this);
            }
        }
    }

    public static class AllBranchesStaticRecord {
        private String id;
        private String direct;
        private Map<String, Integer> counters;
        private Set<String> labels;
        private List<String> events;
        private TestEnum status;
        private AllBranchesDetails details;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDirect() {
            return direct;
        }

        public void setDirect(String direct) {
            this.direct = direct;
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

        public TestEnum getStatus() {
            return status;
        }

        public void setStatus(TestEnum status) {
            this.status = status;
        }

        public AllBranchesDetails getDetails() {
            return details;
        }

        public void setDetails(AllBranchesDetails details) {
            this.details = details;
        }
    }

    public static final class AllBranchesStaticImmutable {
        private final String id;
        private final String direct;
        private final Map<String, Integer> counters;
        private final Set<String> labels;
        private final List<String> events;
        private final TestEnum status;
        private final AllBranchesDetails details;

        private AllBranchesStaticImmutable(Builder builder) {
            this.id = builder.id;
            this.direct = builder.direct;
            this.counters = builder.counters;
            this.labels = builder.labels;
            this.events = builder.events;
            this.status = builder.status;
            this.details = builder.details;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String id() {
            return id;
        }

        public String direct() {
            return direct;
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

        public TestEnum status() {
            return status;
        }

        public AllBranchesDetails details() {
            return details;
        }

        public static final class Builder {
            private String id;
            private String direct;
            private Map<String, Integer> counters;
            private Set<String> labels;
            private List<String> events;
            private TestEnum status;
            private AllBranchesDetails details;

            private Builder() {
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder direct(String direct) {
                this.direct = direct;
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

            public Builder status(TestEnum status) {
                this.status = status;
                return this;
            }

            public Builder details(AllBranchesDetails details) {
                this.details = details;
                return this;
            }

            public AllBranchesStaticImmutable build() {
                return new AllBranchesStaticImmutable(this);
            }
        }
    }

    @DynamoDbBean
    public static class BeanWithObject {
        private String id;
        private Object payload;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }
    }

    @DynamoDbBean
    public static class BeanWithUnsupported {
        private String id;
        private UnsupportedType payload;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public UnsupportedType getPayload() {
            return payload;
        }

        public void setPayload(UnsupportedType payload) {
            this.payload = payload;
        }
    }

    @DynamoDbBean
    public static class BeanWithArrayList {
        private String id;
        private ArrayList<String> values;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ArrayList<String> getValues() {
            return values;
        }

        public void setValues(ArrayList<String> values) {
            this.values = values;
        }
    }

    @DynamoDbBean
    public static class BeanWithHashSet {
        private String id;
        private HashSet<String> values;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public HashSet<String> getValues() {
            return values;
        }

        public void setValues(HashSet<String> values) {
            this.values = values;
        }
    }

    @DynamoDbBean
    public static class BeanWithHashMap {
        private String id;
        private HashMap<String, Integer> values;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public HashMap<String, Integer> getValues() {
            return values;
        }

        public void setValues(HashMap<String, Integer> values) {
            this.values = values;
        }
    }
}
