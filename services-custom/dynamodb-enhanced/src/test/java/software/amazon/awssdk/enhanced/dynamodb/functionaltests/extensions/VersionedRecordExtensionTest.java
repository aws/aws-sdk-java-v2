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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

public class VersionedRecordExtensionTest extends LocalDynamoDbSyncTestBase {

    private static final TableSchema<VersionedRecord> TABLE_SCHEMA =
        TableSchema.fromClass(VersionedRecord.class);

    private final DynamoDbEnhancedClient enhancedClient =
        DynamoDbEnhancedClient.builder()
                              .dynamoDbClient(getDynamoDbClient())
                              .build();

    private final DynamoDbTable<VersionedRecord> mappedTable =
        enhancedClient.table(getConcreteTableName("versioned-table"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(getConcreteTableName("versioned-table")));
    }

    @Test
    public void putItem_setsInitialVersion() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        record.setData("data");

        mappedTable.putItem(record);

        VersionedRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getVersion()).isEqualTo(1L);
        assertThat(retrieved.getData()).isEqualTo("data");
    }

    @Test
    public void updateItem_incrementsVersion() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        record.setData("data");
        mappedTable.putItem(record);

        VersionedRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        retrieved.setData("data_update");
        mappedTable.updateItem(retrieved);

        VersionedRecord afterUpdate = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(afterUpdate.getVersion()).isEqualTo(2L);
        assertThat(afterUpdate.getData()).isEqualTo("data_update");
    }

    @Test
    public void updateItem_withIncorrectVersion_throwsConditionalCheckFailedException() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        record.setData("data");
        mappedTable.putItem(record);

        VersionedRecord retrieved1 = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        VersionedRecord retrieved2 = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));

        retrieved1.setData("data_update1");
        mappedTable.updateItem(retrieved1);

        retrieved2.setData("data_update2");
        assertThatThrownBy(() -> mappedTable.updateItem(retrieved2))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void putItem_withNullVersion_onExistingItem_throwsConditionalCheckFailedException() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        record.setData("data");
        mappedTable.putItem(record);
        VersionedRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(retrieved.getVersion()).isEqualTo(1L);

        retrieved.setData("data_update");
        mappedTable.updateItem(retrieved);
        VersionedRecord afterUpdate = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(afterUpdate.getVersion()).isEqualTo(2L);

        // Attempting to put an item with an existing version in the DB
        VersionedRecord newRecord = new VersionedRecord();
        newRecord.setId("id");
        newRecord.setData("new-data");
        assertThatThrownBy(() -> mappedTable.putItem(newRecord))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void multipleUpdates_incrementsVersionCorrectly() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        mappedTable.putItem(record);

        for (int i = 1; i <= 5; i++) {
            VersionedRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
            assertThat(retrieved.getVersion()).isEqualTo(i);
            retrieved.setData("data-update-" + i);
            mappedTable.updateItem(retrieved);
        }

        VersionedRecord finalRecord = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(finalRecord.getVersion()).isEqualTo(6L);
        assertThat(finalRecord.getData()).isEqualTo("data-update-5");
    }

    @Test
    public void putItem_withExplicitVersion_throwsConditionalCheckFailedException() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        record.setData("data");
        record.setVersion(100L);

        assertThatThrownBy(() -> mappedTable.putItem(record))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void deleteItem_withCorrectVersion_succeeds() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        mappedTable.putItem(record);

        VersionedRecord retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        mappedTable.deleteItem(retrieved);

        VersionedRecord afterDelete = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(afterDelete).isNull();
    }

    @Test
    public void deleteItem_withIncorrectVersion_succeeds() {
        VersionedRecord record = new VersionedRecord();
        record.setId("id");
        mappedTable.putItem(record);

        VersionedRecord retrieved1 = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        VersionedRecord retrieved2 = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));

        retrieved1.setData("data_update");
        mappedTable.updateItem(retrieved1);

        // This operation succeeds even if the two versions are incompatible because Optimistic Locking is not yet implemented
        // for delete operations.
        mappedTable.deleteItem(retrieved2);
        VersionedRecord afterDelete = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        assertThat(afterDelete).isNull();
    }

    @DynamoDbBean
    public static class VersionedRecord {
        private String id;
        private String data;
        private Long version;

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

        @DynamoDbVersionAttribute
        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VersionedRecord that = (VersionedRecord) o;
            return Objects.equals(id, that.id) &&
                   Objects.equals(data, that.data) &&
                   Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, data, version);
        }
    }
}