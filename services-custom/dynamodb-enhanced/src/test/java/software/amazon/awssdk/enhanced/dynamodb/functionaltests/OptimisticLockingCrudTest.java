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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.versionAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.VersionedRecord;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class OptimisticLockingCrudTest extends LocalDynamoDbSyncTestBase {

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class,
                                       a -> a.name("id")
                                             .getter(Record::getId)
                                             .setter(Record::setId)
                                             .tags(primaryPartitionKey(), secondaryPartitionKey("index1")))
                         .addAttribute(Integer.class,
                                       a -> a.name("sort")
                                             .getter(Record::getSort)
                                             .setter(Record::setSort)
                                             .tags(primarySortKey(), secondarySortKey("index1")))
                         .addAttribute(Integer.class,
                                       a -> a.name("value")
                                             .getter(Record::getValue)
                                             .setter(Record::setValue))
                         .addAttribute(String.class,
                                       a -> a.name("gsi_id")
                                             .getter(Record::getGsiId)
                                             .setter(Record::setGsiId)
                                             .tags(secondaryPartitionKey("gsi_keys_only")))
                         .addAttribute(Integer.class,
                                       a -> a.name("gsi_sort")
                                             .getter(Record::getGsiSort)
                                             .setter(Record::setGsiSort)
                                             .tags(secondarySortKey("gsi_keys_only")))
                         .addAttribute(String.class,
                                       a -> a.name("stringAttribute")
                                             .getter(Record::getStringAttribute)
                                             .setter(Record::setStringAttribute))
                         .build();

    private static final TableSchema<VersionedRecord> VERSIONED_RECORD_TABLE_SCHEMA =
        StaticTableSchema.builder(VersionedRecord.class)
                         .newItemSupplier(VersionedRecord::new)
                         .addAttribute(String.class,
                                       a -> a.name("id")
                                             .getter(VersionedRecord::getId)
                                             .setter(VersionedRecord::setId)
                                             .tags(primaryPartitionKey(), secondaryPartitionKey("index1")))
                         .addAttribute(Integer.class,
                                       a -> a.name("sort")
                                             .getter(VersionedRecord::getSort)
                                             .setter(VersionedRecord::setSort)
                                             .tags(primarySortKey(), secondarySortKey("index1")))
                         .addAttribute(Integer.class,
                                       a -> a.name("value")
                                             .getter(VersionedRecord::getValue)
                                             .setter(VersionedRecord::setValue))
                         .addAttribute(String.class,
                                       a -> a.name("gsi_id")
                                             .getter(VersionedRecord::getGsiId)
                                             .setter(VersionedRecord::setGsiId)
                                             .tags(secondaryPartitionKey("gsi_keys_only")))
                         .addAttribute(Integer.class,
                                       a -> a.name("gsi_sort")
                                             .getter(VersionedRecord::getGsiSort)
                                             .setter(VersionedRecord::setGsiSort)
                                             .tags(secondarySortKey("gsi_keys_only")))
                         .addAttribute(String.class,
                                       a -> a.name("stringAttribute")
                                             .getter(VersionedRecord::getStringAttribute)
                                             .setter(VersionedRecord::setStringAttribute))
                         .addAttribute(Integer.class,
                                       a -> a.name("version")
                                             .getter(VersionedRecord::getVersion)
                                             .setter(VersionedRecord::setVersion)
                                             .tags(versionAttribute()))
                         .build();


    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private final DynamoDbTable<Record> mappedTable =
        enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private final DynamoDbTable<VersionedRecord> versionedRecordTable =
        enhancedClient.table(getConcreteTableName("versioned-table-name"), VERSIONED_RECORD_TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        versionedRecordTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(
            DeleteTableRequest.builder()
                              .tableName(getConcreteTableName("table-name"))
                              .build());

        getDynamoDbClient().deleteTable(
            DeleteTableRequest.builder()
                              .tableName(getConcreteTableName("versioned-table-name"))
                              .build());
    }

    // 1. deleteItem(T item) on Non-versioned record
    // -> Optimistic Locking NOT applied -> deletes the record
    @Test
    public void deleteItem_onNonVersionedRecord_doesNotApplyOptimisticLockingAndDeletesTheRecord() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item);
        mappedTable.deleteItem(item);

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 2. deleteItem(T item) on Versioned record and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_whenVersionsMatch_appliesOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(savedItem);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 3. deleteItem(T item, false) on Versioned record
    // -> Optimistic Locking false -> Optimistic Locking is NOT applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_noOptimisticLocking_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        // Update the item to change its version (new version = 2)
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);

        // Delete with old version (version = 1) but flag = false - should succeed (no optimistic locking)
        VersionedRecord oldVersionItem = new VersionedRecord().setId("123").setSort(10).setVersion(1);
        versionedRecordTable.deleteItem(oldVersionItem, false);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 4. deleteItem(T item, true) on Versioned record with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_optimisticLockingAndVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(savedItem, true);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 5. deleteItem(T item, true) on Versioned record with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking applied -> does NOT delete the record
    @Test
    public void deleteItem_onVersionedRecord_optimisticLockingAndVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        // Update the item to change its version (new version = 2)
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);

        // Try to delete with old version (version = 1) and flag = true - should fail
        VersionedRecord oldVersionItem = new VersionedRecord().setId("123").setSort(10).setVersion(1);

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(oldVersionItem, true))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 6. deleteItem(DeleteItemEnhancedRequest) on VersionedRecord with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItemWithBuilder_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .withOptimisticLocking(matchVersion, "version")
                                     .build();

        versionedRecordTable.deleteItem(requestWithLocking);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 7. deleteItem(DeleteItemEnhancedRequest) on VersionedRecord with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking is applied -> does NOT delete the record
    @Test
    public void deleteItemWithBuilder_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .withOptimisticLocking(mismatchVersion, "version")
                                     .build();

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }


    // 8. TransactWriteItems.addDeleteItem(T item) on Non-versioned record
    // -> Optimistic Locking NOT applied -> deletes the record
    @Test
    public void transactDeleteItem_onNonVersionedRecord_deletesTheRecord() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item);

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(mappedTable, item)
                                             .build());

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 9. TransactWriteItems.addDeleteItem(T item) on Versioned record and versions match
    // -> Optimistic Locking is NOT applied (old deprecated method -> does NOT support Optimistic Locking) -> deletes the record
    @Test
    public void transactDeleteItem_onVersionedRecord_whenVersionsMatch_doesNotApplyOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(versionedRecordTable, savedItem)
                                             .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 10. TransactWriteItems.addDeleteItem(T item) on Versioned record and versions do NOT match
    // -> Optimistic Locking is NOT applied (old deprecated method -> does NOT support Optimistic Locking) -> deletes the record
    @Test
    public void transactDeleteItem_onVersionedRecord_whenVersionsMismatch_doesNotApplyOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        Integer mismatchedVersion = 2;
        savedItem.setVersion(mismatchedVersion);

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(versionedRecordTable, savedItem)
                                             .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 11. TransactWriteItems with builder method on Versioned record and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void transactDeleteItemWithBuilder_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .withOptimisticLocking(matchVersion, "version")
                                             .build();

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(versionedRecordTable, requestWithLocking)
                                             .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 12. TransactWriteItems with builder method on Versioned record and versions do NOT match
    // -> Optimistic Locking applied -> does NOT delete the record
    @Test
    public void transactDeleteItemWithBuilder_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .withOptimisticLocking(mismatchVersion, "version")
                                             .build();

        TransactionCanceledException ex =
            assertThrows(TransactionCanceledException.class,
                         () -> enhancedClient.transactWriteItems(
                             TransactWriteItemsEnhancedRequest.builder()
                                                              .addDeleteItem(versionedRecordTable, requestWithLocking)
                                                              .build()));

        assertTrue(ex.hasCancellationReasons());
        assertEquals(1, ex.cancellationReasons().size());
        assertEquals("ConditionalCheckFailed", ex.cancellationReasons().get(0).code());
        assertEquals("The conditional request failed", ex.cancellationReasons().get(0).message());
    }
}