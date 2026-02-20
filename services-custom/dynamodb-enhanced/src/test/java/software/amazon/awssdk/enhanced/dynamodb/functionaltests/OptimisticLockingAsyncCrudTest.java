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
import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.versionAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import java.util.concurrent.CompletionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
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

public class OptimisticLockingAsyncCrudTest extends LocalDynamoDbAsyncTestBase {

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


    private final DynamoDbEnhancedAsyncClient enhancedClient =
        DynamoDbEnhancedAsyncClient.builder()
                                   .dynamoDbClient(getDynamoDbAsyncClient())
                                   .build();

    private final DynamoDbAsyncTable<Record> mappedTable =
        enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private final DynamoDbAsyncTable<VersionedRecord> versionedRecordTable =
        enhancedClient.table(getConcreteTableName("versioned-table-name"), VERSIONED_RECORD_TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
        versionedRecordTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(
            DeleteTableRequest.builder()
                              .tableName(getConcreteTableName("table-name"))
                              .build()).join();

        getDynamoDbAsyncClient().deleteTable(
            DeleteTableRequest.builder()
                              .tableName(getConcreteTableName("versioned-table-name"))
                              .build()).join();
    }

    // 1. deleteItem(T item) on Non-versioned record
    // -> Optimistic Locking NOT applied -> deletes the record
    @Test
    public void deleteItem_onNonVersionedRecord_doesNotApplyOptimisticLockingAndDeletesTheRecord() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item).join();
        mappedTable.deleteItem(item).join();

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 2. deleteItem(T item) on Versioned record and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_whenVersionsMatch_appliesOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        versionedRecordTable.deleteItem(savedItem).join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 3. deleteItem(T item, false) on Versioned record
    // -> Optimistic Locking false -> Optimistic Locking is NOT applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_noOptimisticLocking_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();

        // Update the item to change its version (new version = 2)
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem).join();

        // Delete with old version (version = 1) but flag = false - should succeed (no optimistic locking)
        VersionedRecord oldVersionItem = new VersionedRecord().setId("123").setSort(10).setVersion(1);
        versionedRecordTable.deleteItem(oldVersionItem, false).join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 4. deleteItem(T item, true) on Versioned record with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_optimisticLockingAndVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        versionedRecordTable.deleteItem(savedItem, true).join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 5. deleteItem(T item, true) on Versioned record with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking applied -> does NOT delete the record
    @Test
    public void deleteItem_onVersionedRecord_optimisticLockingAndVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();

        // Update the item to change its version (new version = 2)
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem).join();

        // Try to delete with old version (version = 1) and flag=true - should fail
        VersionedRecord oldVersionItem = new VersionedRecord().setId("123").setSort(10).setVersion(1);

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(oldVersionItem, true).join())
            .isInstanceOf(CompletionException.class)
            .satisfies(e -> assertThat(e.getCause()).isInstanceOf(ConditionalCheckFailedException.class))
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 6. deleteItem(DeleteItemEnhancedRequest) on VersionedRecord with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItemWithBuilder_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .withOptimisticLocking(matchVersion, "version")
                                     .build();

        versionedRecordTable.deleteItem(requestWithLocking).join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 7. deleteItem(DeleteItemEnhancedRequest) on VersionedRecord with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking is applied -> does NOT delete the record
    @Test
    public void deleteItemWithBuilder_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .withOptimisticLocking(mismatchVersion, "version")
                                     .build();

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking).join())
            .isInstanceOf(CompletionException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }


    // 8. TransactWriteItems.addDeleteItem(T item) on Non-versioned record
    // -> Optimistic Locking NOT applied -> deletes the record
    @Test
    public void transactDeleteItem_onNonVersionedRecord_deletesTheRecord() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item).join();

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(mappedTable, item)
                                             .build()).join();

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 9. TransactWriteItems.addDeleteItem(T item) on Versioned record and versions match
    // -> Optimistic Locking is NOT applied (old deprecated method -> does NOT support Optimistic Locking) -> deletes the record
    @Test
    public void transactDeleteItem_versionedRecord_versionsMatch_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();

        enhancedClient.transactWriteItems(
                          TransactWriteItemsEnhancedRequest.builder()
                                                           .addDeleteItem(versionedRecordTable, savedItem)
                                                           .build())
                      .join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 10. TransactWriteItems.addDeleteItem(T item) on Versioned record and versions do NOT match
    // -> Optimistic Locking is NOT applied (old deprecated method -> does NOT support Optimistic Locking) -> deletes the record
    @Test
    public void transactDeleteItem_onVersionedRecord_whenVersionsMismatch_doesNotApplyOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        Integer mismatchedVersion = 2;
        savedItem.setVersion(mismatchedVersion);

        enhancedClient.transactWriteItems(
                          TransactWriteItemsEnhancedRequest.builder()
                                                           .addDeleteItem(versionedRecordTable, savedItem)
                                                           .build())
                      .join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 11. TransactWriteItems with builder method on Versioned record and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void transactDeleteItemWithBuilder_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .withOptimisticLocking(matchVersion, "version")
                                             .build();

        enhancedClient.transactWriteItems(
                          TransactWriteItemsEnhancedRequest.builder()
                                                           .addDeleteItem(versionedRecordTable, requestWithLocking)
                                                           .build())
                      .join();

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey)).join();
        assertThat(deletedItem).isNull();
    }

    // 12. TransactWriteItems with builder method on Versioned record and versions do NOT match
    // -> Optimistic Locking applied -> does NOT delete the record
    @Test
    public void transactDeleteItemWithBuilder_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item).join();

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .withOptimisticLocking(mismatchVersion, "version")
                                             .build();

        assertThatThrownBy(() -> enhancedClient.transactWriteItems(
                                                   TransactWriteItemsEnhancedRequest.builder()
                                                                                    .addDeleteItem(versionedRecordTable,
                                                                                                   requestWithLocking)
                                                                                    .build())
                                               .join())
            .isInstanceOf(CompletionException.class)
            .satisfies(e -> assertThat(((TransactionCanceledException) e.getCause())
                                           .cancellationReasons()
                                           .stream()
                                           .anyMatch(reason -> "ConditionalCheckFailed".equals(reason.code())))
                .isTrue());
    }
}
