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
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.VersionedRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
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
        versionedRecordTable = enhancedClient.table(TABLE_NAME, VERSIONED_RECORD_TABLE_SCHEMA);
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
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

    @Test
    public void transactWriteItems_recordWithoutVersion_andOptimisticLockingOnDelete_shouldSucceed() {
        Record originalItem = new Record().setId("123").setSort(10).setStringAttribute("Original Item");
        Key recordKey = Key.builder().partitionValue(originalItem.getId()).sortValue(originalItem.getSort()).build();

        // Put the item
        mappedTable.putItem(originalItem);

        // Retrieve the item, modify it separately and update it, which will increment the version
        Record savedItem = mappedTable.getItem(r -> r.key(recordKey));
        savedItem.setStringAttribute("Updated Item");
        mappedTable.updateItem(savedItem);

        // Get the updated item and try to delete it
        Record updatedItem = mappedTable.getItem(r -> r.key(recordKey));
        enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                           .addDeleteItem(mappedTable, updatedItem)
                                                                           .build());

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    @Test
    public void transactWriteItems_recordWithVersion_andOptimisticLockingOnDelete_ifVersionMatch_shouldSucceed() {
        VersionedRecord originalItem = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Original Item");
        Key recordKey = Key.builder().partitionValue(originalItem.getId()).sortValue(originalItem.getSort()).build();

        // Put the item
        versionedRecordTable.putItem(originalItem);

        // Retrieve the item, modify it separately and update it, which will increment the version
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);

        // Get the updated item and try to delete it
        VersionedRecord updatedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        enhancedClient.transactWriteItems(TransactWriteItemsEnhancedRequest.builder()
                                                                           .addDeleteItem(versionedRecordTable, updatedItem)
                                                                           .build());

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    @Test
    public void transactWriteItems_recordWithVersion_andOptimisticLockingOnDelete_ifVersionMismatch_shouldFail() {
        VersionedRecord originalItem = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Original Item");
        Key recordKey = Key.builder().partitionValue(originalItem.getId()).sortValue(originalItem.getSort()).build();

        // Put the item
        versionedRecordTable.putItem(originalItem);

        // Retrieve the item, modify it separately and update it, which will increment the version
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);

        // Get the updated item and try to delete it
        VersionedRecord updatedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        updatedItem.setVersion(3); // Intentionally set a version that does not match the current version

        TransactWriteItemsEnhancedRequest request =
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(versionedRecordTable, updatedItem)
                                             .build();

        TransactionCanceledException ex = assertThrows(TransactionCanceledException.class,
                                                       () -> enhancedClient.transactWriteItems(request));

        assertTrue(ex.hasCancellationReasons());
        assertEquals(1, ex.cancellationReasons().size());
        assertEquals("ConditionalCheckFailed", ex.cancellationReasons().get(0).code());
        assertEquals("The conditional request failed", ex.cancellationReasons().get(0).message());
    }

    @Test
    public void delete_recordWithoutVersion_andOptimisticLockingOnDelete_shouldSucceed() {
        Record originalItem = new Record().setId("123").setSort(10).setStringAttribute("Original Item");
        Key recordKey = Key.builder().partitionValue(originalItem.getId()).sortValue(originalItem.getSort()).build();

        // Put the item
        mappedTable.putItem(originalItem);

        // Retrieve the item, modify it separately and update it, which will increment the version
        Record savedItem = mappedTable.getItem(r -> r.key(recordKey));
        savedItem.setStringAttribute("Updated Item");
        mappedTable.updateItem(savedItem);

        // Get the updated item and try to delete it
        Record updatedItem = mappedTable.getItem(r -> r.key(recordKey));
        mappedTable.deleteItem(updatedItem);

        Record deletedItem = mappedTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    @Test
    public void delete_recordWithVersion_andOptimisticLockingOnDelete_ifVersionMatch_shouldSucceed() {
        VersionedRecord originalItem = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Original Item");
        Key recordKey = Key.builder().partitionValue(originalItem.getId()).sortValue(originalItem.getSort()).build();

        // Put the item
        versionedRecordTable.putItem(originalItem);

        // Retrieve the item, modify it separately and update it, which will increment the version
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);

        // Get the updated item and try to delete it
        VersionedRecord updatedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        versionedRecordTable.deleteItem(updatedItem);

        VersionedRecord deletedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        assertThat(deletedItem).isNull();
    }

    @Test
    public void delete_recordWithoutVersion_andOptimisticLockingOnDelete_ifVersionMismatch_shouldFail() {
        VersionedRecord originalItem = new VersionedRecord().setId("123").setSort(10).setStringAttribute("Original Item");
        Key recordKey = Key.builder().partitionValue(originalItem.getId()).sortValue(originalItem.getSort()).build();

        // Put the item
        versionedRecordTable.putItem(originalItem);

        // Retrieve the item, modify it separately and update it, which will increment the version
        VersionedRecord savedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        savedItem.setStringAttribute("Updated Item");
        versionedRecordTable.updateItem(savedItem);

        // Get the updated item and try to delete it
        VersionedRecord updatedItem = versionedRecordTable.getItem(r -> r.key(recordKey));
        updatedItem.setVersion(3); // Intentionally set a version that does not match the current version

        ConditionalCheckFailedException ex = assertThrows(
            ConditionalCheckFailedException.class,
            () -> versionedRecordTable.deleteItem(updatedItem));
        assertThat(ex.getMessage()).contains("The conditional request failed");
    }
}
