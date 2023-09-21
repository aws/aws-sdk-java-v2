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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import org.assertj.core.data.Offset;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;

public class AsyncCrudWithResponseIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {


    private static final String TABLE_NAME = createTestTableName();


    private static final EnhancedLocalSecondaryIndex LOCAL_SECONDARY_INDEX = EnhancedLocalSecondaryIndex.builder()
                                                                                                        .indexName("index1")
                                                                                                        .projection(Projection.builder()
                                                                                                                              .projectionType(ProjectionType.ALL)
                                                                                                                              .build())
                                                                                                        .build();

    private static DynamoDbAsyncClient dynamoDbClient;
    private static DynamoDbEnhancedAsyncClient enhancedClient;
    private static DynamoDbAsyncTable<Record> mappedTable;

    @BeforeClass
    public static void setup() {
        dynamoDbClient = createAsyncDynamoDbClient();
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
        mappedTable.createTable(r -> r.localSecondaryIndices(LOCAL_SECONDARY_INDEX)).join();
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME)).join();
    }

    @AfterClass
    public static void teardown() {
        try {
            dynamoDbClient.deleteTable(r -> r.tableName(TABLE_NAME)).join();
        } finally {
            dynamoDbClient.close();
        }
    }


    @Test
    public void putItem_returnItemCollectionMetrics_set_itemCollectionMetricsNull() {
        Record record = new Record().setId("1").setSort(10);
        PutItemEnhancedRequest<Record> request = PutItemEnhancedRequest.builder(Record.class)
                                                                       .item(record)
                                                                       .build();

        PutItemEnhancedResponse<Record> response = mappedTable.putItemWithResponse(request).join();

        assertThat(response.itemCollectionMetrics()).isNull();
    }

    @Test
    public void putItem_returnItemCollectionMetrics_set_itemCollectionMetricsNotNull() {
        Record record = new Record().setId("1").setSort(10);
        PutItemEnhancedRequest<Record> request = PutItemEnhancedRequest.builder(Record.class)
                                                                       .item(record)
                                                                       .returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                                                                       .build();

        PutItemEnhancedResponse<Record> response = mappedTable.putItemWithResponse(request).join();

        assertThat(response.itemCollectionMetrics()).isNotNull();
    }

    @Test
    public void updateItem_returnItemCollectionMetrics_set_itemCollectionMetricsNull() {
        Record record = new Record().setId("1").setSort(10);
        UpdateItemEnhancedRequest<Record> request = UpdateItemEnhancedRequest.builder(Record.class)
                                                                          .item(record)
                                                                          .build();

        UpdateItemEnhancedResponse<Record> response = mappedTable.updateItemWithResponse(request).join();

        assertThat(response.itemCollectionMetrics()).isNull();
    }

    @Test
    public void updateItem_returnItemCollectionMetrics_set_itemCollectionMetricsNotNull() {
        Record record = new Record().setId("1").setSort(10);
        UpdateItemEnhancedRequest<Record> request = UpdateItemEnhancedRequest.builder(Record.class)
                                                                             .item(record)
                                                                             .returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                                                                             .build();

        UpdateItemEnhancedResponse<Record> response = mappedTable.updateItemWithResponse(request).join();

        assertThat(response.itemCollectionMetrics()).isNotNull();
    }

    @Test
    public void deleteItem_returnConsumedCapacity_unset_consumedCapacityNull() {
        Key key = Key.builder().partitionValue("1").sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response = mappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void deleteItem_returnConsumedCapacity_set_consumedCapacityNotNull() {
        Key key = Key.builder().partitionValue("1").sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response =
            mappedTable.deleteItemWithResponse(r -> r.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)).join();

        assertThat(response.consumedCapacity()).isNotNull();
    }

    @Test
    public void delete_returnItemCollectionMetrics_set_itemCollectionMetricsNotNull() {
        Key key = Key.builder().partitionValue("1").sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response =
            mappedTable.deleteItemWithResponse(r -> r.key(key).returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE))
                       .join();

        assertThat(response.itemCollectionMetrics()).isNotNull();
    }

    @Test
    public void getItem_withoutReturnConsumedCapacity() {
        Record record = new Record().setId("101").setSort(102).setStringAttribute(getStringAttrValue(80_000));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getSort())
                     .build();

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(req -> req.key(key)).join();
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void getItem_withReturnConsumedCapacity_eventualConsistent() {
        Record record = new Record().setId("101").setSort(102).setStringAttribute(getStringAttrValue(80 * 1024));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getSort())
                     .build();
        mappedTable.putItem(record).join();

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(
            req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        ).join();
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        // An eventually consistent read request of an item up to 4 KB requires one-half read request unit.
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(10.0, Offset.offset(1.0));
    }

    @Test
    public void getItem_withReturnConsumedCapacity_stronglyConsistent() {
        Record record = new Record().setId("201").setSort(202).setStringAttribute(getStringAttrValue(80 * 1024));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getSort())
                     .build();
        mappedTable.putItem(record).join();

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(
            req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).consistentRead(true)
        ).join();
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        // A strongly consistent read request of an item up to 4 KB requires one read request unit.
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(20.0, Offset.offset(1.0));
    }
}
