/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.SdkAsserts.assertNotEmpty;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.core.exception.ErrorType;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.BasicTempTable;
import utils.resources.tables.TempTableWithBinaryKey;
import utils.test.util.DynamoDBTestBase;


@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = BasicTempTable.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS),
                            @RequiredResource(resource = TempTableWithBinaryKey.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
                    })
public class DynamoServiceIntegrationTest extends DynamoDBTestBase {

    private static final String HASH_KEY_NAME = BasicTempTable.HASH_KEY_NAME;
    private static final String tableName = BasicTempTable.TEMP_TABLE_NAME;
    private static final String binaryKeyTableName = TempTableWithBinaryKey.TEMP_BINARY_TABLE_NAME;
    private static final Long READ_CAPACITY = BasicTempTable.READ_CAPACITY;
    private static final Long WRITE_CAPACITY = BasicTempTable.WRITE_CAPACITY;

    /**
     * The only @BeforeClass method.
     */
    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullQueryKeyErrorHandling() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        // Put a valid item first
        item.put(HASH_KEY_NAME, AttributeValue.builder().s("bar").build());
        item.put("age", AttributeValue.builder().s("30").build());
        PutItemRequest putItemRequest = PutItemRequest.builder().tableName(tableName).item(item).returnValues(ReturnValue.ALL_OLD
                                                                                                     .toString()).build();
        dynamo.putItem(putItemRequest);
        Map<String, KeysAndAttributes> items = new HashMap<String, KeysAndAttributes>();
        // Put a valid key and a null one
        items.put(tableName,
                  KeysAndAttributes.builder().keys(mapKey(HASH_KEY_NAME, AttributeValue.builder().s("bar").build()), null).build());

        BatchGetItemRequest request =BatchGetItemRequest.builder()
                .requestItems(items)
                .build();

        try {
            dynamo.batchGetItem(request);
        } catch (SdkServiceException exception) {
            assertEquals("ValidationException", exception.errorCode());
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        Map<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
        writeAttributes.put(HASH_KEY_NAME, AttributeValue.builder().s("" + System.currentTimeMillis()).build());
        writeAttributes.put("bar", AttributeValue.builder().s("" + System.currentTimeMillis()).build());
        writeRequests.add(WriteRequest.builder().putRequest(PutRequest.builder().item(writeAttributes).build()).build());
        writeRequests.add(WriteRequest.builder().putRequest(PutRequest.builder().item(null).build()).build());
        requestItems.put(tableName, writeRequests);
        try {
            dynamo.batchWriteItem(BatchWriteItemRequest.builder().requestItems(requestItems).build());
        } catch (SdkServiceException exception) {
            assertEquals("ValidationException", exception.errorCode());
        }

    }

    /**
     * Tests that we correctly parse JSON error responses into SdkServiceException.
     */
    @Test
    public void testErrorHandling() throws Exception {

        DeleteTableRequest request = DeleteTableRequest.builder().tableName("non-existant-table").build();
        try {
            dynamo.deleteTable(request);
            fail("Expected an exception to be thrown");
        } catch (SdkServiceException exception) {
            assertNotEmpty(exception.errorCode());
            assertEquals(ErrorType.CLIENT, exception.errorType());
            assertNotEmpty(exception.getMessage());
            assertNotEmpty(exception.requestId());
            assertNotEmpty(exception.serviceName());
            assertTrue(exception.statusCode() >= 400);
            assertTrue(exception.statusCode() < 600);
        }
    }

    /**
     * Tests that we properly handle error responses for request entities that
     * are too large.
     */
    // DISABLED because DynamoDB apparently upped their max request size; we
    // should be hitting this with a unit test that simulates an appropriate
    // SdkServiceException.
    // @Test
    public void testRequestEntityTooLargeErrorHandling() throws Exception {

        Map<String, KeysAndAttributes> items = new HashMap<String, KeysAndAttributes>();
        for (int i = 0; i < 1024; i++) {
            KeysAndAttributes kaa = KeysAndAttributes.builder().build();
            StringBuilder bigString = new StringBuilder();
            for (int j = 0; j < 1024; j++) {
                bigString.append("a");
            }
            bigString.append(i);
            items.put(bigString.toString(), kaa);
        }
        BatchGetItemRequest request = BatchGetItemRequest.builder().requestItems(items).build();

        try {
            dynamo.batchGetItem(request);
        } catch (SdkServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals("Request entity too large", exception.errorCode());
            assertEquals(ErrorType.CLIENT, exception.errorType());
            assertEquals(413, exception.statusCode());
        }
    }

    @Test
    public void testBatchWriteTooManyItemsErrorHandling() throws Exception {
        int itemNumber = 26;
        HashMap<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        for (int i = 0; i < itemNumber; i++) {
            HashMap<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
            writeAttributes.put(HASH_KEY_NAME, AttributeValue.builder().s("" + System.currentTimeMillis()).build());
            writeAttributes.put("bar", AttributeValue.builder().s("" + System.currentTimeMillis()).build());
            writeRequests.add(WriteRequest.builder().putRequest(PutRequest.builder().item(writeAttributes).build()).build());
        }
        requestItems.put(tableName, writeRequests);
        try {
            dynamo.batchWriteItem(BatchWriteItemRequest.builder().requestItems(requestItems).build());
        } catch (SdkServiceException exception) {
            assertEquals("ValidationException", exception.errorCode());
            assertEquals(ErrorType.CLIENT, exception.errorType());
            assertNotEmpty(exception.getMessage());
            assertNotEmpty(exception.requestId());
            assertNotEmpty(exception.serviceName());
            assertEquals(400, exception.statusCode());
        }
    }

    /**
     * Tests that we can call each service operation to create and describe
     * tables, put, update and delete data, and query.
     */
    @Test
    public void testServiceOperations() throws Exception {
        // Describe all tables
        ListTablesResponse describeTablesResult = dynamo.listTables(ListTablesRequest.builder().build());

        // Describe our new table
        DescribeTableRequest describeTablesRequest = DescribeTableRequest.builder().tableName(tableName).build();
        TableDescription tableDescription = dynamo.describeTable(describeTablesRequest).table();
        assertEquals(tableName, tableDescription.tableName());
        assertNotNull(tableDescription.tableStatus());
        assertEquals(HASH_KEY_NAME, tableDescription.keySchema().get(0).attributeName());
        assertEquals(KeyType.HASH, tableDescription.keySchema().get(0).keyType());
        assertNotNull(tableDescription.provisionedThroughput().numberOfDecreasesToday());
        assertEquals(READ_CAPACITY, tableDescription.provisionedThroughput().readCapacityUnits());
        assertEquals(WRITE_CAPACITY, tableDescription.provisionedThroughput().writeCapacityUnits());

        // Add some data
        int contentLength = 1 * 1024;
        Set<ByteBuffer> byteBufferSet = new HashSet<ByteBuffer>();
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength)));
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength + 1)));

        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(HASH_KEY_NAME, AttributeValue.builder().s("bar").build());
        item.put("age", AttributeValue.builder().n("30").build());
        item.put("bar", AttributeValue.builder().s("" + System.currentTimeMillis()).build());
        item.put("foos", AttributeValue.builder().ss("bleh", "blah").build());
        item.put("S", AttributeValue.builder().ss("ONE", "TWO").build());
        item.put("blob", AttributeValue.builder().b(ByteBuffer.wrap(generateByteArray(contentLength))).build());
        item.put("blobs", AttributeValue.builder().bs(ByteBuffer.wrap(generateByteArray(contentLength)),
                                                      ByteBuffer.wrap(generateByteArray(contentLength + 1))).build());
        item.put("BS", AttributeValue.builder().bs(byteBufferSet).build());

        PutItemRequest putItemRequest = PutItemRequest.builder().tableName(tableName).item(item).returnValues(ReturnValue.ALL_OLD.toString()).build();

        PutItemResponse putItemResult = dynamo.putItem(putItemRequest);

        // Get our new item
        GetItemResponse itemResult = dynamo.getItem(GetItemRequest.builder().tableName(tableName).key(mapKey(HASH_KEY_NAME,
                                                                                             AttributeValue.builder().s("bar").build()))
                                                             .consistentRead(true).build());
        assertNotNull(itemResult.item().get("S").ss());
        assertEquals(2, itemResult.item().get("S").ss().size());
        assertTrue(itemResult.item().get("S").ss().contains("ONE"));
        assertTrue(itemResult.item().get("S").ss().contains("TWO"));
        assertEquals("30", itemResult.item().get("age").n());
        assertNotNull(itemResult.item().get("bar").s());
        assertNotNull(itemResult.item().get("blob").b());
        assertEquals(0, itemResult.item().get("blob").b().compareTo(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertNotNull(itemResult.item().get("blobs").bs());
        assertEquals(2, itemResult.item().get("blobs").bs().size());
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertNotNull(itemResult.item().get("BS").bs());
        assertEquals(2, itemResult.item().get("BS").bs().size());
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Pause to try and deal with ProvisionedThroughputExceededExceptions
        Thread.sleep(1000 * 5);

        // Add some data into the table with binary hash key
        ByteBuffer byteBuffer = ByteBuffer.allocate(contentLength * 2);
        byteBuffer.put(generateByteArray(contentLength));
        byteBuffer.flip();
        item = new HashMap<String, AttributeValue>();
        item.put(HASH_KEY_NAME, AttributeValue.builder().b(byteBuffer).build());
        // Reuse the byteBuffer
        item.put("blob", AttributeValue.builder().b(byteBuffer).build());
        item.put("blobs", AttributeValue.builder().bs(ByteBuffer.wrap(generateByteArray(contentLength)),
                                                      ByteBuffer.wrap(generateByteArray(contentLength + 1))).build());
        // Reuse the byteBufferSet
        item.put("BS", AttributeValue.builder().bs(byteBufferSet).build());

        putItemRequest = PutItemRequest.builder().tableName(binaryKeyTableName).item(item).returnValues(ReturnValue.ALL_OLD.toString()).build();
        dynamo.putItem(putItemRequest);

        // Get our new item
        itemResult = dynamo.getItem(GetItemRequest.builder().tableName(binaryKeyTableName).key(mapKey(HASH_KEY_NAME,
                                                                                        AttributeValue.builder().b(byteBuffer).build()))
                                               .consistentRead(true).build());
        assertNotNull(itemResult.item().get("blob").b());
        assertEquals(0, itemResult.item().get("blob").b().compareTo(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertNotNull(itemResult.item().get("blobs").bs());
        assertEquals(2, itemResult.item().get("blobs").bs().size());
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("blobs").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertNotNull(itemResult.item().get("BS").bs());
        assertEquals(2, itemResult.item().get("BS").bs().size());
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(itemResult.item().get("BS").bs().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Pause to try and deal with ProvisionedThroughputExceededExceptions
        Thread.sleep(1000 * 5);

        // Load some random data
        System.out.println("Loading data...");
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            item = new HashMap<String, AttributeValue>();
            item.put(HASH_KEY_NAME, AttributeValue.builder().s("bar-" + System.currentTimeMillis()).build());
            item.put("age", AttributeValue.builder().n(Integer.toString(random.nextInt(100) + 30)).build());
            item.put("bar", AttributeValue.builder().s("" + System.currentTimeMillis()).build());
            item.put("foos", AttributeValue.builder().ss("bleh", "blah").build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).returnValues(ReturnValue.ALL_OLD.toString()).build());
        }

        // Update an item
        Map<String, AttributeValueUpdate> itemUpdates = new HashMap<String, AttributeValueUpdate>();
        itemUpdates.put("1", AttributeValueUpdate.builder().value(AttributeValue.builder().s("¢").build()).action(AttributeAction.PUT.toString()).build());
        itemUpdates.put("foos", AttributeValueUpdate.builder().value(AttributeValue.builder().ss("foo").build()).action(AttributeAction.PUT.toString()).build());
        itemUpdates.put("S", AttributeValueUpdate.builder().value(AttributeValue.builder().ss("THREE").build()).action(AttributeAction.ADD.toString()).build());
        itemUpdates.put("age", AttributeValueUpdate.builder().value(AttributeValue.builder().n("10").build()).action(AttributeAction.ADD.toString()).build());
        itemUpdates.put("blob", AttributeValueUpdate.builder().value(
                AttributeValue.builder().b(ByteBuffer.wrap(generateByteArray(contentLength + 1))).build()).action(
                AttributeAction.PUT.toString()).build());
        itemUpdates.put("blobs",
                        AttributeValueUpdate.builder().value(AttributeValue.builder().bs(ByteBuffer.wrap(generateByteArray(contentLength))).build()).action(
                                                 AttributeAction.PUT.toString()).build());
        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder().tableName(tableName).key(
                mapKey(HASH_KEY_NAME, AttributeValue.builder().s("bar").build())).attributeUpdates(
                itemUpdates).returnValues("ALL_NEW").build();

        UpdateItemResponse updateItemResult = dynamo.updateItem(updateItemRequest);

        assertEquals("¢", updateItemResult.attributes().get("1").s());
        assertEquals(1, updateItemResult.attributes().get("foos").ss().size());
        assertTrue(updateItemResult.attributes().get("foos").ss().contains("foo"));
        assertEquals(3, updateItemResult.attributes().get("S").ss().size());
        assertTrue(updateItemResult.attributes().get("S").ss().contains("ONE"));
        assertTrue(updateItemResult.attributes().get("S").ss().contains("TWO"));
        assertTrue(updateItemResult.attributes().get("S").ss().contains("THREE"));
        assertEquals(Integer.toString(30 + 10), updateItemResult.attributes().get("age").n());
        assertEquals(0, updateItemResult.attributes().get("blob").b()
                                        .compareTo(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertEquals(1, updateItemResult.attributes().get("blobs").bs().size());
        assertTrue(updateItemResult.attributes().get("blobs").bs()
                                   .contains(ByteBuffer.wrap(generateByteArray(contentLength))));

        itemUpdates.clear();
        itemUpdates.put("age", AttributeValueUpdate.builder().value(AttributeValue.builder().n("30").build()).action(AttributeAction.PUT.toString()).build());
        itemUpdates.put("blobs", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().bs(ByteBuffer.wrap(generateByteArray(contentLength + 1))).build())
                .action(AttributeAction.ADD.toString())
                .build());
        updateItemRequest = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(mapKey(HASH_KEY_NAME, AttributeValue.builder().s("bar").build()))
                .attributeUpdates(itemUpdates)
                .returnValues("ALL_NEW")
                .build();

        updateItemResult = dynamo.updateItem(updateItemRequest);

        assertEquals("30", updateItemResult.attributes().get("age").n());
        assertEquals(2, updateItemResult.attributes().get("blobs").bs().size());
        assertTrue(updateItemResult.attributes().get("blobs").bs()
                                   .contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(updateItemResult.attributes().get("blobs").bs()
                                   .contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Get an item that doesn't exist.
        GetItemRequest itemsRequest = GetItemRequest.builder().tableName(tableName).key(mapKey(HASH_KEY_NAME, AttributeValue.builder().s("3").build()))
                .consistentRead(true).build();
        GetItemResponse itemsResult = dynamo.getItem(itemsRequest);
        assertNull(itemsResult.item());

        // Get an item that doesn't have any attributes,
        itemsRequest = GetItemRequest.builder().tableName(tableName).key(mapKey(HASH_KEY_NAME, AttributeValue.builder().s("bar").build()))
                .consistentRead(true).attributesToGet("non-existent-attribute").build();
        itemsResult = dynamo.getItem(itemsRequest);
        assertEquals(0, itemsResult.item().size());


        // Scan data
        ScanRequest scanRequest = ScanRequest.builder().tableName(tableName).attributesToGet(HASH_KEY_NAME).build();
        ScanResponse scanResult = dynamo.scan(scanRequest);
        assertTrue(scanResult.count() > 0);
        assertTrue(scanResult.scannedCount() > 0);


        // Try a more advanced Scan query and run it a few times for performance metrics
        System.out.println("Testing Scan...");
        for (int i = 0; i < 10; i++) {
            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
            scanFilter.put("age", Condition.builder()
                    .attributeValueList(AttributeValue.builder().n("40").build())
                    .comparisonOperator(ComparisonOperator.GT.toString())
                    .build());
            scanRequest = ScanRequest.builder().tableName(tableName).scanFilter(scanFilter).build();
            scanResult = dynamo.scan(scanRequest);
        }

        // Batch write
        HashMap<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        HashMap<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
        writeAttributes.put(HASH_KEY_NAME, AttributeValue.builder().s("" + System.currentTimeMillis()).build());
        writeAttributes.put("bar", AttributeValue.builder().s("" + System.currentTimeMillis()).build());
        writeRequests.add(WriteRequest.builder().putRequest(PutRequest.builder().item(writeAttributes).build()).build());
        writeRequests.add(WriteRequest.builder()
                .deleteRequest(DeleteRequest.builder()
                        .key(mapKey(HASH_KEY_NAME, AttributeValue.builder().s("toDelete").build()))
                        .build())
                .build());
        requestItems.put(tableName, writeRequests);
        BatchWriteItemResponse batchWriteItem = dynamo.batchWriteItem(BatchWriteItemRequest.builder().requestItems(requestItems).build());
        //        assertNotNull(batchWriteItem.itemCollectionMetrics());
        //        assertEquals(1, batchWriteItem.itemCollectionMetrics().size());
        //        assertEquals(tableName, batchWriteItem.itemCollectionMetrics().entrySet().iterator().next().get);
        //        assertNotNull(tableName, batchWriteItem.getResponses().iterator().next().getCapacityUnits());
        assertNotNull(batchWriteItem.unprocessedItems());
        assertTrue(batchWriteItem.unprocessedItems().isEmpty());

        // Delete some data
        DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(mapKey(HASH_KEY_NAME, AttributeValue.builder().s("jeep").build()))
                .returnValues(ReturnValue.ALL_OLD.toString())
                .build();
        DeleteItemResponse deleteItemResult = dynamo.deleteItem(deleteItemRequest);

        // Delete our table
        DeleteTableResponse deleteTable = dynamo.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());

    }

}
