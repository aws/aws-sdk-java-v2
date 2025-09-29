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

import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.VersionedRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class CrudWithResponseIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {

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

    @Test
    public void putItem_set_requestedMetadataNull() {
        Record record = new Record().setId("1").setSort(10);
        PutItemEnhancedResponse<Record> response = mappedTable.putItemWithResponse(r -> r.item(record));

        assertThat(response.itemCollectionMetrics()).isNull();
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void putItem_set_requestedMetadataNotNull() {
        Record record = new Record().setId("1").setSort(10);
        PutItemEnhancedRequest<Record> request = PutItemEnhancedRequest.builder(Record.class)
                                                                       .item(record)
                                                                       .returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                                                                       .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                       .build();

        PutItemEnhancedResponse<Record> response = mappedTable.putItemWithResponse(request);

        assertThat(response.itemCollectionMetrics()).isNotNull();
        assertThat(response.consumedCapacity()).isNotNull();
        assertThat(response.consumedCapacity().capacityUnits()).isNotNull();
    }

    @Test
    public void updateItem_set_requestedMetadataNull() {
        Record record = new Record().setId("1").setSort(10);
        UpdateItemEnhancedResponse<Record> response = mappedTable.updateItemWithResponse(r -> r.item(record));

        assertThat(response.itemCollectionMetrics()).isNull();
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void updateItem_set_itemCollectionMetricsNotNull() {
        Record record = new Record().setId("1").setSort(10);
        UpdateItemEnhancedRequest<Record> request = UpdateItemEnhancedRequest.builder(Record.class)
                                                                             .item(record)
                                                                             .returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                                                                             .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                             .build();

        UpdateItemEnhancedResponse<Record> response = mappedTable.updateItemWithResponse(request);

        assertThat(response.itemCollectionMetrics()).isNotNull();
        assertThat(response.consumedCapacity()).isNotNull();
        assertThat(response.consumedCapacity().capacityUnits()).isNotNull();
    }

    @Test
    public void putItem_returnValuesOnConditionCheckFailure_set_returnValuesOnConditionCheckFailureNull() {
        Record record = new Record().setId("1").setSort(10);
        mappedTable.putItem(record);

        Expression itemDoesNotExist = Expression.builder().expression("attribute_not_exists(id)").build();
        PutItemEnhancedRequest<Record> request = PutItemEnhancedRequest.builder(Record.class)
                                                                       .item(record)
                                                                       .conditionExpression(itemDoesNotExist)
                                                                       .build();

        assertThatThrownBy(() -> mappedTable.putItem(request))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(((ConditionalCheckFailedException) e).hasItem()).isFalse());
    }

    @Test
    public void putItem_returnValuesOnConditionCheckFailure_set_returnValuesOnConditionCheckFailureNotNull() {
        Record record = new Record().setId("1").setSort(10);
        mappedTable.putItem(record);

        Expression itemDoesNotExist = Expression.builder().expression("attribute_not_exists(id)").build();
        PutItemEnhancedRequest<Record> request = PutItemEnhancedRequest.builder(Record.class)
                                                                       .item(record)
                                                                       .conditionExpression(itemDoesNotExist)
                                                                       .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                                                                       .build();

        assertThatThrownBy(() -> mappedTable.putItem(request))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(((ConditionalCheckFailedException) e).hasItem()).isTrue());
    }

    @Test
    public void updateItem_returnValuesOnConditionCheckFailure_set_returnValuesOnConditionCheckFailureNull() {
        Record record = new Record().setId("1").setSort(10);
        mappedTable.putItem(record);

        Expression itemDoesNotExist = Expression.builder().expression("attribute_not_exists(id)").build();
        UpdateItemEnhancedRequest<Record> request = UpdateItemEnhancedRequest.builder(Record.class)
                                                                             .item(record)
                                                                             .conditionExpression(itemDoesNotExist)
                                                                             .build();

        assertThatThrownBy(() -> mappedTable.updateItem(request))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(((ConditionalCheckFailedException) e).hasItem()).isFalse());
    }

    @Test
    public void updateItem_returnValuesOnConditionCheckFailure_set_returnValuesOnConditionCheckFailureNotNull() {
        Record record = new Record().setId("1").setSort(10);
        mappedTable.putItem(record);

        Expression itemDoesNotExist = Expression.builder().expression("attribute_not_exists(id)").build();
        UpdateItemEnhancedRequest<Record> request = UpdateItemEnhancedRequest.builder(Record.class)
                                                                             .item(record)
                                                                             .conditionExpression(itemDoesNotExist)
                                                                             .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                                                                             .build();

        assertThatThrownBy(() -> mappedTable.updateItem(request))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(((ConditionalCheckFailedException) e).hasItem()).isTrue());
    }

    @Test
    public void deleteItem_returnValuesOnConditionCheckFailure_set_returnValuesOnConditionCheckFailureNull() {
        Record record = new Record().setId("1").setSort(10);
        Key recordKey = Key.builder()
                           .partitionValue(record.getId())
                           .sortValue(record.getSort())
                           .build();
        mappedTable.putItem(record);

        Expression itemDoesNotExist = Expression.builder().expression("attribute_not_exists(id)").build();
        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder()
                                                                     .key(recordKey)
                                                                     .conditionExpression(itemDoesNotExist)
                                                                     .build();

        assertThatThrownBy(() -> mappedTable.deleteItem(request))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(((ConditionalCheckFailedException) e).hasItem()).isFalse());
    }

    @Test
    public void deleteItem_returnValuesOnConditionCheckFailure_set_returnValuesOnConditionCheckFailureNotNull() {
        Record record = new Record().setId("1").setSort(10);
        Key recordKey = Key.builder()
                           .partitionValue(record.getId())
                           .sortValue(record.getSort())
                           .build();
        mappedTable.putItem(record);

        Expression itemDoesNotExist = Expression.builder().expression("attribute_not_exists(id)").build();
        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder()
                                                                     .key(recordKey)
                                                                     .conditionExpression(itemDoesNotExist)
                                                                     .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                                                                     .build();

        assertThatThrownBy(() -> mappedTable.deleteItem(request))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(((ConditionalCheckFailedException) e).hasItem()).isTrue());
    }

    @Test
    public void deleteItem__unset_consumedCapacityNull() {
        Key key = Key.builder().partitionValue("1").sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response = mappedTable.deleteItemWithResponse(r -> r.key(key));

        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void deleteItem__set_consumedCapacityNotNull() {
        Key key = Key.builder().partitionValue("1").sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response =
            mappedTable.deleteItemWithResponse(r -> r.key(key)
                                                     .returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                                                     .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL));

        assertThat(response.consumedCapacity()).isNotNull();
    }

    @Test
    public void getItem_set_requestedMetadataNull() {
        Record record = new Record().setId("101").setSort(102).setStringAttribute(getStringAttrValue(80_000));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getSort())
                     .build();

        mappedTable.putItem(record);
        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(req -> req.key(key));

        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void getItem_set_eventualConsistent() throws InterruptedException {
        Record record = new Record().setId("101").setSort(102).setStringAttribute(getStringAttrValue(80 * 1024));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getSort())
                     .build();
        mappedTable.putItem(record);

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(
            req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        );

        // Handle eventual consistency: DynamoDB's eventually consistent reads may not immediately
        // reflect recent writes. Retry once with a delay to ensure item propagation.
        if(response.attributes() == null){
            Thread.sleep(200);
            response = mappedTable.getItemWithResponse(
                req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL));
        }
        assertThat(response.attributes())
            .withFailMessage("Item not propagated to DynamoDB after retry - eventual consistency delay exceeded")
            .isNotNull();

        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        // An eventually consistent read request of an item up to 4 KB requires one-half read request unit.
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(10.0, Offset.offset(1.0));
    }

    @Test
    public void getItem_set_stronglyConsistent() {
        Record record = new Record().setId("101").setSort(102).setStringAttribute(getStringAttrValue(80 * 1024));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getSort())
                     .build();
        mappedTable.putItem(record);

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(
            req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).consistentRead(true)
        );
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        // A strongly consistent read request of an item up to 4 KB requires one read request unit.
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(20.0, Offset.offset(1.0));
    }

    // ========== OPTIMISTIC LOCKING TESTS ==========

    // 1. deleteItem(T item) - Non-versioned record
    @Test
    public void deleteItem_nonVersionedRecord_shouldSucceed() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item);
        mappedTable.deleteItem(item);

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 2. deleteItem(T item) - Versioned record, versions match
    @Test
    public void deleteItem_versionedRecord_versionMatch_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(savedItem);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 3. deleteItem(T item, false) - Versioned record, should not use optimistic locking
    @Test
    public void deleteItem_versionedRecord_flagFalse_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        
        // Update the item to change its version
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);
        
        // Delete with old version but flag=false - should succeed (no optimistic locking)
        VersionedRecord oldVersionItem = new VersionedRecord().setId("123").setSort(10).setVersion(1);
        versionedRecordTable.deleteItem(oldVersionItem, false);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 4. deleteItem(T item, true) - Versioned record, versions match
    @Test
    public void deleteItem_versionedRecord_flagTrue_versionMatch_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(savedItem, true);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 5. deleteItem(T item, true) - Versioned record, versions mismatch
    @Test
    public void deleteItem_versionedRecord_flagTrue_versionMismatch_shouldFail() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        
        // Update the item to change its version
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);
        
        // Try to delete with old version and flag=true - should fail
        VersionedRecord oldVersionItem = new VersionedRecord().setId("123").setSort(10).setVersion(1);
        
        assertThatThrownBy(() -> versionedRecordTable.deleteItem(oldVersionItem, true))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }




    // 6. deleteItem(DeleteItemEnhancedRequest) with helper - versions match
    @Test
    public void deleteItemWithHelper_versionMatch_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        DeleteItemEnhancedRequest baseRequest = DeleteItemEnhancedRequest.builder().key(recordKey).build();
        DeleteItemEnhancedRequest requestWithLocking = DeleteItemEnhancedRequest.withOptimisticLocking(
            baseRequest, AttributeValue.builder().n(savedItem.getVersion().toString()).build(), "version");

        versionedRecordTable.deleteItem(requestWithLocking);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 7. deleteItem(DeleteItemEnhancedRequest) with helper - versions mismatch
    @Test
    public void deleteItemWithHelper_versionMismatch_shouldFail() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        DeleteItemEnhancedRequest baseRequest = DeleteItemEnhancedRequest.builder().key(recordKey).build();
        DeleteItemEnhancedRequest requestWithLocking = DeleteItemEnhancedRequest.withOptimisticLocking(
            baseRequest, AttributeValue.builder().n("999").build(), "version");

        assertThatThrownBy(() -> versionedRecordTable.deleteItem(requestWithLocking))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .satisfies(e -> assertThat(e.getMessage()).contains("The conditional request failed"));
    }

    // 8. TransactWriteItems.addDeleteItem(T item) - Non-versioned record
    @Test
    public void transactDeleteItem_nonVersionedRecord_shouldSucceed() {
        Record item = new Record().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        mappedTable.putItem(item);

        enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                           .addDeleteItem(mappedTable, item)
                                                                           .build());

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 9. TransactWriteItems.addDeleteItem(T item) - Versioned record, versions match
    @Test
    public void transactDeleteItem_versionedRecord_versionMatch_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                           .addDeleteItem(versionedRecordTable, savedItem)
                                                                           .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }




    // 10. TransactWriteItems with helper - versions match
    @Test
    public void transactDeleteItemWithHelper_versionMatch_shouldSucceed() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));

        TransactDeleteItemEnhancedRequest baseRequest = TransactDeleteItemEnhancedRequest.builder().key(recordKey).build();
        TransactDeleteItemEnhancedRequest requestWithLocking = TransactDeleteItemEnhancedRequest.withOptimisticLocking(
            baseRequest, AttributeValue.builder().n(savedItem.getVersion().toString()).build(), "version");

        enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                           .addDeleteItem(versionedRecordTable,
                                                                                          requestWithLocking)
                                                                           .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    // 11. TransactWriteItems with helper - versions mismatch
    @Test
    public void transactDeleteItemWithHelper_versionMismatch_shouldFail() {
        VersionedRecord item = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Test Item");
        Key recordKey = Key.builder().partitionValue(item.getId()).sortValue(item.getSort()).build();

        versionedRecordTable.putItem(item);

        TransactDeleteItemEnhancedRequest baseRequest = TransactDeleteItemEnhancedRequest.builder().key(recordKey).build();
        TransactDeleteItemEnhancedRequest requestWithLocking = TransactDeleteItemEnhancedRequest.withOptimisticLocking(
            baseRequest, AttributeValue.builder().n("999").build(), "version");

        TransactionCanceledException ex = assertThrows(TransactionCanceledException.class,
                                                       () -> enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                                                                                .addDeleteItem(versionedRecordTable, requestWithLocking)
                                                                                                                                .build()));

        assertTrue(ex.hasCancellationReasons());
        assertEquals(1, ex.cancellationReasons().size());
        assertEquals("ConditionalCheckFailed", ex.cancellationReasons().get(0).code());
        assertEquals("The conditional request failed", ex.cancellationReasons().get(0).message());
    }
}