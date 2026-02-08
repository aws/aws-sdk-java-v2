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
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class AtomicCounterExtensionTest extends LocalDynamoDbSyncTestBase {

    private static final StaticTableSchema<CounterRecord> TABLE_SCHEMA =
        StaticTableSchema.builder(CounterRecord.class)
                         .newItemSupplier(CounterRecord::new)
                         .addAttribute(String.class,
                                       a -> a.name("id1")
                                             .getter(CounterRecord::getId)
                                             .setter(CounterRecord::setId)
                                             .addTag(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("data")
                                             .getter(CounterRecord::getData)
                                             .setter(CounterRecord::setData))
                         .addAttribute(Long.class,
                                       a -> a.name("defaultCounter")
                                             .getter(CounterRecord::getDefaultCounter)
                                             .setter(CounterRecord::setDefaultCounter)
                                             .addTag(atomicCounter()))
                         .addAttribute(Long.class,
                                       a -> a.name("customCounter")
                                             .getter(CounterRecord::getCustomCounter)
                                             .setter(CounterRecord::setCustomCounter)
                                             .addTag(atomicCounter(5, 10)))
                         .build();

    private final DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();

    private final DynamoDbTable<CounterRecord> mappedTable = enhancedClient.table(
        getConcreteTableName("atomic-counter-table"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(
            getConcreteTableName("atomic-counter-table")));
    }

    @Test
    public void putItem_initializesCountersWithDefaultValues() {
        CounterRecord record = new CounterRecord();
        record.setId("id1");

        mappedTable.putItem(record);

        CounterRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo("id1");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(0L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(10L);
    }

    @Test
    public void updateItem_incrementsCounters() {
        CounterRecord record = new CounterRecord();
        record.setId("id1");
        record.setData("data1");
        mappedTable.putItem(record);

        CounterRecord update = new CounterRecord();
        update.setId("id1");
        update.setData("data2");
        mappedTable.updateItem(update);

        CounterRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getData()).isEqualTo("data2");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(1L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(15L);
    }

    @Test
    public void updateItem_multipleUpdates_incrementsCountersCorrectly() {
        CounterRecord record = new CounterRecord();
        record.setId("id1");
        record.setData("data1");
        mappedTable.putItem(record);

        for (int i = 2; i <= 10; i++) {
            CounterRecord update = new CounterRecord();
            update.setId("id1");
            update.setData(String.format("data%d", i));
            mappedTable.updateItem(update);
        }

        CounterRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getData()).isEqualTo("data10");
        assertThat(retrieved.getDefaultCounter()).isEqualTo(9L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(55L);
    }

    @Test
    public void putItem_withExistingCounterValues_overwritesWithStartValues() {
        CounterRecord record = new CounterRecord();
        record.setId("id1");
        record.setDefaultCounter(100L);
        record.setCustomCounter(200L);

        mappedTable.putItem(record);

        CounterRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getDefaultCounter()).isEqualTo(0L);
        assertThat(retrieved.getCustomCounter()).isEqualTo(10L);
    }

    public static class CounterRecord {
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
            CounterRecord that = (CounterRecord) o;
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
