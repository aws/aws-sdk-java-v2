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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.atomicCounter;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
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
    public void beforeWrite_updateItemOperation_hasCounters_createsUpdateExpression() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);

        Map<String, AttributeValue> items = ITEM_MAPPER.itemToMap(atomicCounterItem, true);
        assertThat(items).hasSize(1);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(ITEM_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.UPDATE_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());

        Map<String, AttributeValue> transformedItem = result.transformedItem();
        assertThat(transformedItem).isNotNull().hasSize(1);
        assertThat(transformedItem).containsEntry("id", AttributeValue.fromS(RECORD_ID));

        assertThat(result.updateExpression()).isNotNull();

        List<SetAction> setActions = result.updateExpression().setActions();
        assertThat(setActions).hasSize(2);

        verifyAction(setActions, "customCounter", "5", "5");
        verifyAction(setActions, "defaultCounter", "-1", "1");
    }

    @Test
    public void beforeWrite_updateItemOperation_noCounters_noChanges() {
        SimpleItem item = new SimpleItem();
        item.setId(RECORD_ID);
        item.setNumberAttribute(4L);

        Map<String, AttributeValue> items = SIMPLE_ITEM_MAPPER.itemToMap(item, true);
        assertThat(items).hasSize(2);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(SIMPLE_ITEM_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.UPDATE_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());
        assertThat(result.transformedItem()).isNull();
        assertThat(result.updateExpression()).isNull();
    }

    @Test
    public void beforeWrite_updateItemOperation_hasCountersInItem_createsUpdateExpressionAndFilters() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);
        atomicCounterItem.setCustomCounter(255L);

        Map<String, AttributeValue> items = ITEM_MAPPER.itemToMap(atomicCounterItem, true);
        assertThat(items).hasSize(2);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(ITEM_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.UPDATE_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());

        Map<String, AttributeValue> transformedItem = result.transformedItem();
        assertThat(transformedItem).isNotNull().hasSize(1);
        assertThat(transformedItem).containsEntry("id", AttributeValue.fromS(RECORD_ID));

        assertThat(result.updateExpression()).isNotNull();

        List<SetAction> setActions = result.updateExpression().setActions();
        assertThat(setActions).hasSize(2);

        verifyAction(setActions, "customCounter", "5", "5");
        verifyAction(setActions, "defaultCounter", "-1", "1");
    }

    @Test
    public void beforeWrite_putItemOperation_hasCounters_createsItemTransform() {
        AtomicCounterItem atomicCounterItem = new AtomicCounterItem();
        atomicCounterItem.setId(RECORD_ID);

        Map<String, AttributeValue> items = ITEM_MAPPER.itemToMap(atomicCounterItem, true);
        assertThat(items).hasSize(1);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(ITEM_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.PUT_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem()).isNotNull();
        assertThat(result.transformedItem()).hasSize(3);
        assertThat(result.updateExpression()).isNull();
    }

    @Test
    public void beforeWrite_putItemOperation_noCounters_noChanges() {
        SimpleItem item = new SimpleItem();
        item.setId(RECORD_ID);
        item.setNumberAttribute(4L);

        Map<String, AttributeValue> items = SIMPLE_ITEM_MAPPER.itemToMap(item, true);
        assertThat(items).hasSize(2);

        WriteModification result =
            atomicCounterExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                              .items(items)
                                                                              .tableMetadata(SIMPLE_ITEM_MAPPER.tableMetadata())
                                                                              .operationName(OperationName.PUT_ITEM)
                                                                              .operationContext(PRIMARY_CONTEXT).build());

        assertThat(result.transformedItem()).isNull();
        assertThat(result.updateExpression()).isNull();
    }

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

        assertThat(result).isEqualTo(ReadModification.builder().build());
    }

    private void verifyAction(List<SetAction> actions, String attributeName, String expectedStart, String expectedDelta) {
        String expectedPath = String.format("#AMZN_MAPPED_%s", attributeName);
        SetAction action = actions.stream()
                                  .filter(a -> a.path().equals(expectedPath))
                                  .findFirst()
                                  .orElseThrow(() -> new IllegalStateException("Failed to find expected action"));

        assertThat(action.value()).isEqualTo(String.format("if_not_exists(#AMZN_MAPPED_%1$s, :AMZN_MAPPED_%1$s_Start) + "
                                                           + ":AMZN_MAPPED_%1$s_Delta", attributeName));

        Map<String, String> expressionNames =
            Collections.singletonMap(String.format("#AMZN_MAPPED_%s", attributeName), attributeName);
        assertThat(action.expressionNames()).isEqualTo(expressionNames);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(String.format(":AMZN_MAPPED_%s_Start", attributeName), AttributeValue.builder().n(expectedStart).build());
        expressionValues.put(String.format(":AMZN_MAPPED_%s_Delta", attributeName), AttributeValue.builder().n(expectedDelta).build());
        assertThat(action.expressionValues()).isEqualTo(expressionValues);
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
