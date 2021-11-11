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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.atomicCounter;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AtomicCounterExtensionTest {
    private static final String RECORD_ID = "id123";

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private final AtomicCounterExtension atomicCounterExtension = AtomicCounterExtension.builder().build();

    private static final StaticTableSchema<AtomicCounterItem> ITEM_MAPPER =
        StaticTableSchema.builder(AtomicCounterItem.class)
                         .newItemSupplier(AtomicCounterItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(AtomicCounterItem::getId)
                                                           .setter(AtomicCounterItem::setId)
                                                           .addTag(primaryPartitionKey()))
                         .addAttribute(Long.class, a -> a.name("defaultCounter")
                                                            .getter(AtomicCounterItem::getDefaultCounter)
                                                            .setter(AtomicCounterItem::setDefaultCounter)
                                                            .addTag(atomicCounter()))
                         .addAttribute(Long.class, a -> a.name("customCounter")
                                                         .getter(AtomicCounterItem::getCustomCounter)
                                                         .setter(AtomicCounterItem::setCustomCounter)
                                                         .addTag(atomicCounter(5, 10)))
                         .build();

    private static final StaticTableSchema<SimpleItem> SIMPLE_ITEM_MAPPER =
        StaticTableSchema.builder(SimpleItem.class)
                         .newItemSupplier(SimpleItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(SimpleItem::getId)
                                                           .setter(SimpleItem::setId)
                                                           .addTag(primaryPartitionKey()))
                         .addAttribute(Long.class, a -> a.name("numberAttribute")
                                                         .getter(SimpleItem::getNumberAttribute)
                                                         .setter(SimpleItem::setNumberAttribute))
                         .build();

    @Test
    public void beforeRead_doesNotTransformObject() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);
        Map<String, AttributeValue> fakeItemMap = ITEM_MAPPER.itemToMap(atomicCounterItem, true);

        ReadModification result =
            atomicCounterExtension.afterRead(DefaultDynamoDbExtensionContext
                                               .builder()
                                               .items(fakeItemMap)
                                               .tableMetadata(ITEM_MAPPER.tableMetadata())
                                               .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result, is(ReadModification.builder().build()));
    }

    @Test
    public void beforeWrite_counterAttributesNotSet_transformedItemHasCounterStartValues() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);

        Map<String, AttributeValue> items = ITEM_MAPPER.itemToMap(atomicCounterItem, true);
        assertThat(items.size(), is(1));

        WriteModification result =
            atomicCounterExtension.beforeWrite(
                DefaultDynamoDbExtensionContext
                    .builder()
                    .items(items)
                    .tableMetadata(ITEM_MAPPER.tableMetadata())
                    .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem().size(), is(3));
        assertThat(result.transformedItem().get("id"), is(items.get("id")));
        assertThat(result.transformedItem().get("defaultCounter").n(), is("0"));
        assertThat(result.transformedItem().get("customCounter").n(), is("10"));
    }

    @Test
    public void beforeWrite_counterAttributesSet_transformedItemHasCounterStartValues() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);
        atomicCounterItem.setDefaultCounter(4L);
        atomicCounterItem.setCustomCounter(40L);

        Map<String, AttributeValue> items = ITEM_MAPPER.itemToMap(atomicCounterItem, true);
        assertThat(items.size(), is(3));

        WriteModification result =
            atomicCounterExtension.beforeWrite(
                DefaultDynamoDbExtensionContext
                    .builder()
                    .items(items)
                    .tableMetadata(ITEM_MAPPER.tableMetadata())
                    .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem().size(), is(3));
        assertThat(result.transformedItem().get("id"), is(items.get("id")));
        assertThat(result.transformedItem().get("defaultCounter").n(), is("0"));
        assertThat(result.transformedItem().get("customCounter").n(), is("10"));
    }

    @Test
    public void beforeWrite_counterAttributesNul_transformedItemHasCounterStartValues() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);

        Map<String, AttributeValue> items = new HashMap<>(ITEM_MAPPER.itemToMap(atomicCounterItem, true));
        items.put("defaultCounter", AttributeValue.builder().nul(true).build());

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                                     .builder()
                                                     .items(items)
                                                     .tableMetadata(ITEM_MAPPER.tableMetadata())
                                                     .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem().size(), is(3));
        assertThat(result.transformedItem().get("id"), is(items.get("id")));
        assertThat(result.transformedItem().get("defaultCounter").n(), is("0"));
        assertThat(result.transformedItem().get("customCounter").n(), is("10"));
    }

    @Test
    public void beforeWrite_noCounters_noTransformHappens() {
        SimpleItem item = new SimpleItem();
        item.setId(RECORD_ID);
        item.setNumberAttribute(4L);

        Map<String, AttributeValue> items = SIMPLE_ITEM_MAPPER.itemToMap(item, true);
        assertThat(items.size(), is(2));

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext
                                                   .builder()
                                                   .items(items)
                                                   .tableMetadata(SIMPLE_ITEM_MAPPER.tableMetadata())
                                                   .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem(), nullValue());
    }

    private static class AtomicCounterItem {

        private String id;
        private Long defaultCounter;
        private Long customCounter;

        public AtomicCounterItem() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getDefaultCounter() {
            return defaultCounter;
        }

        public void setDefaultCounter(Long defaultCounter) {
            this.defaultCounter = defaultCounter;
        }

        public Long getCustomCounter() {
            return customCounter;
        }

        public void setCustomCounter(Long customCounter) {
            this.customCounter = customCounter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AtomicCounterItem item = (AtomicCounterItem) o;
            return Objects.equals(id, item.id) &&
                   Objects.equals(defaultCounter, item.defaultCounter) &&
                   Objects.equals(customCounter, item.customCounter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), id, defaultCounter, customCounter);
        }
    }

    private static class SimpleItem {

        private String id;
        private Long numberAttribute;

        public SimpleItem() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getNumberAttribute() {
            return numberAttribute;
        }

        public void setNumberAttribute(Long numberAttribute) {
            this.numberAttribute = numberAttribute;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            SimpleItem item = (SimpleItem) o;
            return Objects.equals(id, item.id) &&
                   Objects.equals(numberAttribute, item.numberAttribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), id, numberAttribute);
        }
    }
}
