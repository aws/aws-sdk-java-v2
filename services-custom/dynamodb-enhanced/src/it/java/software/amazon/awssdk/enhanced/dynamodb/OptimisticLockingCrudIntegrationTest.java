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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.VersionedRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class OptimisticLockingCrudIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {

    private static final String TABLE_NAME = createTestTableName();
    private static final String VERSIONED_TABLE_NAME = createTestTableName();

    private static final EnhancedLocalSecondaryIndex LOCAL_SECONDARY_INDEX =
        EnhancedLocalSecondaryIndex.builder()
                                   .indexName("index1")
                                   .projection(Projection.builder()
                                                         .projectionType(ProjectionType.ALL)
                                                         .build())
                                   .build();

    private static DynamoDbClient dynamoDbClient;
    private static DynamoDbEnhancedClient enhancedClient;
    private static DynamoDbTable<Record> mappedTable;
    private static DynamoDbTable<VersionedRecord> versionedRecordTable;

    @BeforeClass
    public static void beforeClass() {
        dynamoDbClient = createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(dynamoDbClient)
                                               .extensions(VersionedRecordExtension.builder().build())
                                               .build();
        mappedTable = enhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
        mappedTable.createTable(r -> r.localSecondaryIndices(LOCAL_SECONDARY_INDEX));
        versionedRecordTable = enhancedClient.table(VERSIONED_TABLE_NAME, VERSIONED_RECORD_TABLE_SCHEMA);
        versionedRecordTable.createTable();
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(VERSIONED_TABLE_NAME));
    }

    @After
    public void tearDown() {
        mappedTable.scan()
                   .items()
                   .forEach(record -> mappedTable.deleteItem(record));

        versionedRecordTable.scan()
                            .items()
                            .forEach(versionedRecord -> versionedRecordTable.deleteItem(versionedRecord));
    }

    @AfterClass
    public static void afterClass() {
        try {
            dynamoDbClient.deleteTable(r -> r.tableName(TABLE_NAME));
            dynamoDbClient.deleteTable(r -> r.tableName(VERSIONED_TABLE_NAME));
        } finally {
            dynamoDbClient.close();
        }
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