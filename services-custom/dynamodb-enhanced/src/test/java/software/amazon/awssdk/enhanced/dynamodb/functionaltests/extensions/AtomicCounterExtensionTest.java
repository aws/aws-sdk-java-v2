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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.atomicCounter;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Objects;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

public class AtomicCounterExtensionTest extends LocalDynamoDbSyncTestBase {

    private static final StaticTableSchema<StaticCounterRecord> TABLE_SCHEMA =
        StaticTableSchema.builder(StaticCounterRecord.class)
                         .newItemSupplier(StaticCounterRecord::new)
                         .addAttribute(String.class,
                                       a -> a.name("id1")
                                             .getter(StaticCounterRecord::getId)
                                             .setter(StaticCounterRecord::setId)
                                             .addTag(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("data")
                                             .getter(StaticCounterRecord::getData)
                                             .setter(StaticCounterRecord::setData))
                         .addAttribute(Long.class,
                                       a -> a.name("defaultCounter")
                                             .getter(StaticCounterRecord::getDefaultCounter)
                                             .setter(StaticCounterRecord::setDefaultCounter)
                                             .addTag(atomicCounter()))
                         .addAttribute(Long.class,
                                       a -> a.name("customCounter")
                                             .getter(StaticCounterRecord::getCustomCounter)
                                             .setter(StaticCounterRecord::setCustomCounter)
                                             .addTag(atomicCounter(5, 10)))
                         .build();

    private final DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();

    private final DynamoDbTable<BeanCounterRecord> beanMappedTable = enhancedClient.table(
        getConcreteTableName("atomic-counter-table-bean"), BeanTableSchema.create(BeanCounterRecord.class));

    private final DynamoDbTable<StaticCounterRecord> staticMappedTable = enhancedClient.table(
        getConcreteTableName("atomic-counter-table-static"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        staticMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        beanMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(
            getConcreteTableName("atomic-counter-table-bean")));
        getDynamoDbClient().deleteTable(r -> r.tableName(
            getConcreteTableName("atomic-counter-table-static")));
    }

    @Test
    public void putItem_beanSchema_initializesCountersWithDefaultValues() {
        BeanCounterRecord beanRecord = new BeanCounterRecord();
        beanRecord.setId("id");

        beanMappedTable.putItem(beanRecord);

        BeanCounterRecord retrieved = beanMappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo("id");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(0L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(10L);
    }

    @Test
    public void updateItem_beanSchema_incrementsCounters() {
        BeanCounterRecord beanRecord = new BeanCounterRecord();
        beanRecord.setId("id1");
        beanRecord.setData("data1");
        beanMappedTable.putItem(beanRecord);

        BeanCounterRecord update = new BeanCounterRecord();
        update.setId("id1");
        update.setData("data2");
        beanMappedTable.updateItem(update);

        BeanCounterRecord retrieved = beanMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getData()).isEqualTo("data2");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(1L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(15L);
    }

    @Test
    public void updateItem_beanSchema_multipleUpdates_incrementsCountersCorrectly() {
        BeanCounterRecord record = new BeanCounterRecord();
        record.setId("id1");
        record.setData("data1");
        beanMappedTable.putItem(record);

        for (int i = 2; i <= 10; i++) {
            BeanCounterRecord update = new BeanCounterRecord();
            update.setId("id1");
            update.setData(String.format("data%d", i));
            beanMappedTable.updateItem(update);
        }

        BeanCounterRecord retrieved = beanMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getData()).isEqualTo("data10");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(9L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(55L);
    }

    @Test
    public void putItem_beanSchema_withExistingCounterValues_overwritesWithStartValues() {
        BeanCounterRecord record = new BeanCounterRecord();
        record.setId("id1");
        record.setDefaultCounter(100L);
        record.setCustomCounter(200L);

        beanMappedTable.putItem(record);

        BeanCounterRecord retrieved = beanMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getDefaultCounter()).isEqualTo(0L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(10L);
    }

    @Test
    public void putItem_staticSchema_initializesCountersWithDefaultValues() {
        StaticCounterRecord record = new StaticCounterRecord();
        record.setId("id1");

        staticMappedTable.putItem(record);

        StaticCounterRecord retrieved = staticMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo("id1");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(0L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(10L);
    }

    @Test
    public void updateItem_staticSchema_incrementsCounters() {
        StaticCounterRecord record = new StaticCounterRecord();
        record.setId("id1");
        record.setData("data1");
        staticMappedTable.putItem(record);

        StaticCounterRecord update = new StaticCounterRecord();
        update.setId("id1");
        update.setData("data2");
        staticMappedTable.updateItem(update);

        StaticCounterRecord retrieved = staticMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getData()).isEqualTo("data2");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(1L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(15L);
    }

    @Test
    public void updateItem_staticSchema_multipleUpdates_incrementsCountersCorrectly() {
        StaticCounterRecord record = new StaticCounterRecord();
        record.setId("id1");
        record.setData("data1");
        staticMappedTable.putItem(record);

        for (int i = 2; i <= 10; i++) {
            StaticCounterRecord update = new StaticCounterRecord();
            update.setId("id1");
            update.setData(String.format("data%d", i));
            staticMappedTable.updateItem(update);
        }

        StaticCounterRecord retrieved = staticMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getData()).isEqualTo("data10");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(9L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(55L);
    }

    @Test
    public void putItem_staticSchema_withExistingCounterValues_overwritesWithStartValues() {
        StaticCounterRecord record = new StaticCounterRecord();
        record.setId("id1");
        record.setDefaultCounter(100L);
        record.setCustomCounter(200L);

        staticMappedTable.putItem(record);

        StaticCounterRecord retrieved = staticMappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getDefaultCounter()).isEqualTo(0L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(10L);
    }

    @DynamoDbBean
    public static class BeanCounterRecord {
        private String id;
        private String data;
        private Long defaultCounter;
        private Long customCounter;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @DynamoDbAtomicCounter
        public Long getDefaultCounter() {
            return defaultCounter;
        }

        public void setDefaultCounter(Long defaultCounter) {
            this.defaultCounter = defaultCounter;
        }

        @DynamoDbAtomicCounter(delta = 5, startValue = 10)
        public Long getCustomCounter() {
            return customCounter;
        }

        public void setCustomCounter(Long customCounter) {
            this.customCounter = customCounter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StaticCounterRecord that = (StaticCounterRecord) o;
            return Objects.equals(id, that.id)
                   && Objects.equals(data, that.data)
                   && Objects.equals(defaultCounter, that.defaultCounter)
                   && Objects.equals(customCounter, that.customCounter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, defaultCounter, customCounter);
        }
    }

    public static class StaticCounterRecord {
        private String id;
        private String data;
        private Long defaultCounter;
        private Long customCounter;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StaticCounterRecord that = (StaticCounterRecord) o;
            return Objects.equals(id, that.id)
                   && Objects.equals(data, that.data)
                   && Objects.equals(defaultCounter, that.defaultCounter)
                   && Objects.equals(customCounter, that.customCounter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, defaultCounter, customCounter);
        }
    }
}
