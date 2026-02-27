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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
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

    // 1. deleteItem(T item) - deprecated - on Non-versioned record
    // -> Optimistic Locking NOT applied -> unconditionally deletes the record
    @Test
    public void deprecatedDeleteItem_onNonVersionedRecord_skipsOptimisticLockingAndUnconditionallyDeletes() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item);
        Record savedItem = mappedTable.getItem(r -> r.key(recordKey));
        mappedTable.deleteItem(savedItem);

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 2. deleteItem(T item) - deprecated - on Versioned record
    // -> Optimistic Locking is not applied -> unconditionally deletes the record
    @Test
    public void deprecatedDeleteItem_onVersionedRecordAndMatchingVersions_skipsOptimisticLockingAndUnconditionallyDeletes() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(savedItem);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 3. deleteItem(T item) - deprecated - on Versioned record, with stale version
    // -> Optimistic Locking is not applied -> unconditionally deletes the record
    @Test
    public void deprecatedDeleteItem_onVersionedRecordAndStaleVersion_skipsOptimisticLockingAndUnconditionallyDeletes() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        // Simulate a stale version by changing the version number
        savedItem.setVersion(2);
        versionedRecordTable.deleteItem(savedItem);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        // the item is deleted even though the version was stale because the old method does not apply optimistic locking
        assertThat(deletedItem).isNull();
    }

    // 4. deleteItem(T item, false) on Versioned record
    // -> Optimistic Locking false -> Optimistic Locking is NOT applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_noOptimisticLocking_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
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

    // 5. deleteItem(T item, true) on Versioned record with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItem_onVersionedRecord_optimisticLockingAndVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(savedItem, true);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 6. deleteItem(T item, true) on Versioned record with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking applied -> does NOT delete the record
    @Test
    public void deleteItem_onVersionedRecord_optimisticLockingAndVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
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

    // 7. deleteItem(DeleteItemEnhancedRequest) on VersionedRecord with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItemWithRequest_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .optimisticLocking(matchVersion, versionAttributeName)
                                     .build();

        versionedRecordTable.deleteItem(requestWithLocking);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 8. deleteItem(DeleteItemEnhancedRequest) on VersionedRecord with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking is applied -> does NOT delete the record
    @Test
    public void deleteItemWithRequest_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .optimisticLocking(mismatchVersion, versionAttributeName)
                                     .build();

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 9. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions match + custom condition respected
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItemWithRequest_whenOptimisticLockingAndCustomConditionAreRespected_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String matchingDatabaseValue = "test";
        expressionValues.put(":value", AttributeValue.fromS(matchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .conditionExpression(conditionExpression)
                                     .optimisticLocking(matchVersion, versionAttributeName)
                                     .build();

        versionedRecordTable.deleteItem(requestWithLocking);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 10. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions match + custom condition not respected
    // -> Optimistic Locking is applied -> does not delete the record because of the failing custom condition
    @Test
    public void deleteItemWithRequest_whenOptimisticLockingConditionRespected_butCustomConditionFails_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String nonMatchingDatabaseValue = "nonMatchingValue";
        expressionValues.put(":value", AttributeValue.fromS(nonMatchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .conditionExpression(conditionExpression)
                                     .optimisticLocking(matchVersion, versionAttributeName)
                                     .build();

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 11. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions do not match + custom condition respected
    // -> does not delete the record
    @Test
    public void deleteItemWithRequest_whenCustomConditionRespected_butOptimisticConditionFails_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String matchingDatabaseValue = "test";
        expressionValues.put(":value", AttributeValue.fromS(matchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .conditionExpression(conditionExpression)
                                     .optimisticLocking(mismatchVersion, versionAttributeName)
                                     .build();

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 12. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions do not match + custom condition fails
    // -> does not delete the record
    @Test
    public void deleteItemWithRequest_whenOptimisticLockingAndCustomConditionNotRespected_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String nonMatchingDatabaseValue = "nonMatchingValue";
        expressionValues.put(":value", AttributeValue.fromS(nonMatchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        DeleteItemEnhancedRequest requestWithLocking =
            DeleteItemEnhancedRequest.builder()
                                     .key(recordKey)
                                     .conditionExpression(conditionExpression)
                                     .optimisticLocking(mismatchVersion, versionAttributeName)
                                     .build();

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 13. deleteItem(Consumer<>) on VersionedRecord with Optimistic Locking true and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItemWithBuilder_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        AttributeValue matchVersion = AttributeValue.builder().n("1").build();
        String versionAttributeName = "version";

        versionedRecordTable.putItem(item);

        versionedRecordTable.deleteItem(r -> r
            .key(recordKey)
            .optimisticLocking(matchVersion, versionAttributeName));

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 14. deleteItem(Consumer<>) on VersionedRecord with Optimistic Locking true and versions DO NOT match
    // -> Optimistic Locking is applied -> does NOT delete the record
    @Test
    public void deleteItemWithBuilder_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(r -> r
            .key(recordKey)
            .optimisticLocking(mismatchVersion, versionAttributeName))

        ).isInstanceOf(ConditionalCheckFailedException.class)
         .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 15. deleteItem(Consumer<>) with Optimistic Locking true, versions match + custom condition respected
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void deleteItemWithBuilder_whenOptimisticLockingAndCustomConditionAreRespected_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String matchingDatabaseValue = "test";
        expressionValues.put(":value", AttributeValue.fromS(matchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        versionedRecordTable.deleteItem(r -> r
            .key(recordKey)
            .optimisticLocking(matchVersion, versionAttributeName)
            .conditionExpression(conditionExpression));

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 16. deleteItem(Consumer<>) with Optimistic Locking true, versions match + custom condition not respected
    // -> Optimistic Locking is applied -> does not delete the record because of the failing custom condition
    @Test
    public void deleteItemWithBuilder_whenOptimisticLockingConditionRespected_butCustomConditionFails_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String nonMatchingDatabaseValue = "nonMatchingValue";
        expressionValues.put(":value", AttributeValue.fromS(nonMatchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(r -> r
            .key(recordKey)
            .conditionExpression(conditionExpression)
            .optimisticLocking(matchVersion, versionAttributeName))

        ).isInstanceOf(ConditionalCheckFailedException.class)
         .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 17. deleteItem(Consumer<>) with Optimistic Locking true, versions do not match + custom condition respected
    // -> does not delete the record
    @Test
    public void deleteItemWithBuilder_whenCustomConditionRespected_butOptimisticConditionFails_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String matchingDatabaseValue = "test";
        expressionValues.put(":value", AttributeValue.fromS(matchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(r -> r
            .key(recordKey)
            .conditionExpression(conditionExpression)
            .optimisticLocking(mismatchVersion, versionAttributeName))

        ).isInstanceOf(ConditionalCheckFailedException.class)
         .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 18. deleteItem(Consumer<>) with Optimistic Locking true, versions do not match + custom condition fails
    // -> does not delete the record
    @Test
    public void deleteItemWithBuilder_whenOptimisticLockingAndCustomConditionNotRespected_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String nonMatchingDatabaseValue = "nonMatchingValue";
        expressionValues.put(":value", AttributeValue.fromS(nonMatchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(r -> r
            .key(recordKey)
            .conditionExpression(conditionExpression)
            .optimisticLocking(mismatchVersion, versionAttributeName))

        ).isInstanceOf(ConditionalCheckFailedException.class)
         .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 19. TransactWriteItems.deleteItem(T item) - deprecated - on Non-versioned record
    // -> Optimistic Locking NOT applied -> deletes the record
    @Test
    public void transactDeleteItem_onNonVersionedRecord_deletesTheRecord() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item);

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(mappedTable, item)
                                             .build());

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 20. TransactWriteItems.deleteItem(T item) - deprecated - on Versioned record and versions match
    // -> Optimistic Locking is NOT applied (old deprecated method -> does NOT support Optimistic Locking) -> deletes the record
    @Test
    public void transactDeleteItem_onVersionedRecord_whenVersionsMatch_skipsOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
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

    // 21. TransactWriteItems.deleteItem(T item) - deprecated - on Versioned record and versions do NOT match
    // -> Optimistic Locking is NOT applied (old deprecated method -> does NOT support Optimistic Locking) -> deletes the record
    @Test
    public void transactDeleteItem_onVersionedRecord_whenVersionsMismatch_skipsOptimisticLockingAndDeletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
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

    // 22. TransactWriteItems with builder method on Versioned record and versions match
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void transactDeleteItemWithRequest_onVersionedRecord_whenVersionsMatch_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .optimisticLocking(matchVersion, versionAttributeName)
                                             .build();

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(versionedRecordTable, requestWithLocking)
                                             .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 23. TransactWriteItems with builder method on Versioned record and versions do NOT match
    // -> Optimistic Locking applied -> does NOT delete the record
    @Test
    public void transactDeleteItemWithRequest_onVersionedRecord_whenVersionsMismatch_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .optimisticLocking(mismatchVersion, versionAttributeName)
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

    // 24. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions match + custom condition respected
    // -> Optimistic Locking is applied -> deletes the record
    @Test
    public void transactDeleteItemWithRequest_whenOptimisticLockingAndCustomConditionAreRespected_deletesTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String matchingDatabaseValue = "test";
        expressionValues.put(":value", AttributeValue.fromS(matchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .conditionExpression(conditionExpression)
                                             .optimisticLocking(matchVersion, versionAttributeName)
                                             .build();

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(versionedRecordTable, requestWithLocking)
                                             .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 25. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions match + custom condition not respected
    // -> Optimistic Locking is applied -> does not delete the record because of the failing custom condition
    @Test
    public void transactDeleteItemWithRequest_whenOptimisticLockingConditionRespected_butCustomConditionFails_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String nonMatchingDatabaseValue = "nonMatchingValue";
        expressionValues.put(":value", AttributeValue.fromS(nonMatchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        AttributeValue matchVersion = AttributeValue.builder().n(savedItem.getVersion().toString()).build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .conditionExpression(conditionExpression)
                                             .optimisticLocking(matchVersion, versionAttributeName)
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

    // 26. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions do not match + custom condition respected
    // -> does not delete the record
    @Test
    public void transactDeleteItemWithRequest_whenCustomConditionRespected_butOptimisticConditionFails_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String matchingDatabaseValue = "test";
        expressionValues.put(":value", AttributeValue.fromS(matchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .conditionExpression(conditionExpression)
                                             .optimisticLocking(mismatchVersion, versionAttributeName)
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

    // 27. deleteItem(DeleteItemEnhancedRequest) with Optimistic Locking true, versions do not match + custom condition fails
    // -> does not delete the record
    @Test
    public void transactDeleteItemWithRequest_whenOptimisticLockingAndCustomConditionNotRespected_doesNotDeleteTheRecord() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("test");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#stringAttribute", "stringAttribute");

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        String nonMatchingDatabaseValue = "nonMatchingValue";
        expressionValues.put(":value", AttributeValue.fromS(nonMatchingDatabaseValue));

        Expression conditionExpression =
            Expression.builder()
                      .expression("#stringAttribute = :value")
                      .expressionNames(Collections.unmodifiableMap(expressionNames))
                      .expressionValues(Collections.unmodifiableMap(expressionValues))
                      .build();

        versionedRecordTable.putItem(item);

        AttributeValue mismatchVersion = AttributeValue.builder().n("2").build();
        String versionAttributeName = "version";

        TransactDeleteItemEnhancedRequest requestWithLocking =
            TransactDeleteItemEnhancedRequest.builder()
                                             .key(recordKey)
                                             .conditionExpression(conditionExpression)
                                             .optimisticLocking(mismatchVersion, versionAttributeName)
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