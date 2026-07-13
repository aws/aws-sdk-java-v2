/*
 * Copyright 2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.services.dynamodbv2;

import static com.amazonaws.test.util.SdkAsserts.assertNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.dynamodbv2.test.resources.tables.BasicTempTable;
import com.amazonaws.dynamodbv2.test.resources.tables.TempTableWithBinaryKey;
import com.amazonaws.dynamodbv2.test.util.DynamoDBTestBase;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.test.resources.RequiredResources;
import com.amazonaws.test.resources.RequiredResources.RequiredResource;
import com.amazonaws.test.resources.RequiredResources.ResourceCreationPolicy;
import com.amazonaws.test.resources.RequiredResources.ResourceRetentionPolicy;
import com.amazonaws.test.resources.ResourceCentricBlockJUnit4ClassRunner;

public class ServiceTest extends LocalDynamoDBTestBase {

    private static final String HASH_KEY_NAME = BasicTempTable.HASH_KEY_NAME;
    private static final String tableName = BasicTempTable.TEMP_TABLE_NAME;
    private static final String binaryKeyTableName = TempTableWithBinaryKey.TEMP_BINARY_TABLE_NAME;
    private static final Long READ_CAPACITY = BasicTempTable.READ_CAPACITY;
    private static final Long WRITE_CAPACITY = BasicTempTable.WRITE_CAPACITY;
    private static AmazonDynamoDB dynamo;

    /**
     * The only @BeforeClass method.
     */
    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();
        dynamo = client();
        dynamo.createTable(BasicTempTable.getCreateTableRequest());
        dynamo.createTable(TempTableWithBinaryKey.getCreateTableRequest());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullQueryKeyErrorHandling() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        // Put a valid item first
        item.put(HASH_KEY_NAME, new AttributeValue("bar"));
        item.put("age", new AttributeValue("30"));
        PutItemRequest putItemRequest = new PutItemRequest(tableName, item).withReturnValues(ReturnValue.ALL_OLD
                .toString());
        dynamo.putItem(putItemRequest);
        Map<String, KeysAndAttributes> items = new HashMap<String, KeysAndAttributes>();
        // Put a valid key and a null one
        items.put(tableName,
                new KeysAndAttributes().withKeys(getMapKey(HASH_KEY_NAME, new AttributeValue().withS("bar")), null));

        BatchGetItemRequest request = new BatchGetItemRequest();
        request.setRequestItems(items);

        try {
            dynamo.batchGetItem(request);
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        Map<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
        writeAttributes.put(HASH_KEY_NAME, new AttributeValue().withS("" + System.currentTimeMillis()));
        writeAttributes.put("bar", new AttributeValue().withS("" + System.currentTimeMillis()));
        writeRequests.add(new WriteRequest().withPutRequest(new PutRequest().withItem(writeAttributes)));
        writeRequests.add(new WriteRequest().withPutRequest(new PutRequest().withItem(null)));
        requestItems.put(tableName, writeRequests);
        try {
            dynamo.batchWriteItem(new BatchWriteItemRequest().withRequestItems(requestItems));
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
        }

    }

    /**
     * Tests that we correctly parse JSON error responses into AmazonServiceExceptions.
     */
    @Test
    public void testErrorHandling() throws Exception {

        DeleteTableRequest request = new DeleteTableRequest("non-existant-table");
        try {
            dynamo.deleteTable(request);
            fail("Expected an exception to be thrown");
        } catch (AmazonServiceException ase) {
            assertNotEmpty(ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertNotEmpty(ase.getMessage());
            assertNotEmpty(ase.getRequestId());
            assertNotEmpty(ase.getServiceName());
            assertTrue(ase.getStatusCode() >= 400);
            assertTrue(ase.getStatusCode() < 600);
        }
    }

    /**
     * Tests that we properly handle error responses for request entities that
     * are too large.
     */
    // DISABLED because DynamoDB apparently upped their max request size; we
    // should be hitting this with a unit test that simulates an appropriate
    // AmazonServiceException.
    // @Test
    public void testRequestEntityTooLargeErrorHandling() throws Exception {
        BatchGetItemRequest request = new BatchGetItemRequest();

        Map<String, KeysAndAttributes> items = new HashMap<String, KeysAndAttributes>();
        for (int i = 0; i < 1024; i++) {
            KeysAndAttributes kaa = new KeysAndAttributes();
            StringBuilder bigString = new StringBuilder();
            for (int j = 0; j < 1024; j++) {
                bigString.append("a");
            }
            bigString.append(i);
            items.put(bigString.toString(), kaa);
        }
        request.setRequestItems(items);

        try {
            dynamo.batchGetItem(request);
        } catch (AmazonServiceException ase) {
            assertNotNull(ase.getMessage());
            assertEquals("Request entity too large", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertEquals(413, ase.getStatusCode());
        }
    }

    @Test
    public void testBatchWriteTooManyItemsErrorHandling() throws Exception {
        int itemNumber = 26;
        HashMap<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        for (int i = 0; i < itemNumber; i++) {
            HashMap<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
            writeAttributes.put(HASH_KEY_NAME, new AttributeValue().withS("" + System.currentTimeMillis()));
            writeAttributes.put("bar", new AttributeValue().withS("" + System.currentTimeMillis()));
            writeRequests.add(new WriteRequest().withPutRequest(new PutRequest().withItem(writeAttributes)));
        }
        requestItems.put(tableName, writeRequests);
        try {
            dynamo.batchWriteItem(new BatchWriteItemRequest().withRequestItems(requestItems));
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
            assertEquals(ErrorType.Client, ase.getErrorType());
            assertNotEmpty(ase.getMessage());
            assertNotEmpty(ase.getRequestId());
            assertNotEmpty(ase.getServiceName());
            assertEquals(400, ase.getStatusCode());
        }
    }

    /**
     * Tests that we can call each service operation to create and describe
     * tables, put, update and delete data, and query.
     */
    @Test
    public void testServiceOperations() throws Exception {
        // Describe all tables
        ListTablesResult describeTablesResult = dynamo.listTables();

        // Describe our new table
        DescribeTableRequest describeTablesRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamo.describeTable(describeTablesRequest).getTable();
        assertEquals(tableName, tableDescription.getTableName());
        assertNotNull(tableDescription.getTableStatus());
        assertEquals(HASH_KEY_NAME, tableDescription.getKeySchema().get(0).getAttributeName());
        assertEquals(KeyType.HASH.toString(), tableDescription.getKeySchema().get(0).getKeyType());
        assertNotNull(tableDescription.getProvisionedThroughput().getNumberOfDecreasesToday());
        assertEquals(READ_CAPACITY, tableDescription.getProvisionedThroughput().getReadCapacityUnits());
        assertEquals(WRITE_CAPACITY, tableDescription.getProvisionedThroughput().getWriteCapacityUnits());

        // Add some data
        int contentLength = 1 * 1024;
        Set<ByteBuffer> byteBufferSet = new HashSet<ByteBuffer>();
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength)));
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength + 1)));

        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(HASH_KEY_NAME, new AttributeValue("bar"));
        item.put("age", new AttributeValue().withN("30"));
        item.put("bar", new AttributeValue("" + System.currentTimeMillis()));
        item.put("foos", new AttributeValue().withSS("bleh", "blah"));
        item.put("S", new AttributeValue().withSS("ONE", "TWO"));
        item.put("blob", new AttributeValue().withB(ByteBuffer.wrap(generateByteArray(contentLength))));
        item.put("blobs", new AttributeValue().withBS(ByteBuffer.wrap(generateByteArray(contentLength)), ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        item.put("BS", new AttributeValue().withBS(byteBufferSet));

        PutItemRequest putItemRequest = new PutItemRequest(tableName, item).withReturnValues(ReturnValue.ALL_OLD.toString());

        PutItemResult putItemResult = dynamo.putItem(putItemRequest);

        // Get our new item
        GetItemResult getItemResult = dynamo.getItem(new GetItemRequest(tableName, getMapKey(HASH_KEY_NAME,
                new AttributeValue("bar"))).withConsistentRead(true));
        assertNotNull(getItemResult.getItem().get("S").getSS());
        assertEquals(2, getItemResult.getItem().get("S").getSS().size());
        assertTrue(getItemResult.getItem().get("S").getSS().contains("ONE"));
        assertTrue(getItemResult.getItem().get("S").getSS().contains("TWO"));
        assertEquals("30", getItemResult.getItem().get("age").getN());
        assertNotNull(getItemResult.getItem().get("bar").getS());
        assertNotNull(getItemResult.getItem().get("blob").getB());
        assertEquals(0, getItemResult.getItem().get("blob").getB().compareTo(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertNotNull(getItemResult.getItem().get("blobs").getBS());
        assertEquals(2, getItemResult.getItem().get("blobs").getBS().size());
        assertTrue(getItemResult.getItem().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(getItemResult.getItem().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertNotNull(getItemResult.getItem().get("BS").getBS());
        assertEquals(2, getItemResult.getItem().get("BS").getBS().size());
        assertTrue(getItemResult.getItem().get("BS").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(getItemResult.getItem().get("BS").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Add some data into the table with binary hash key
        ByteBuffer byteBuffer = ByteBuffer.allocate(contentLength * 2);
        byteBuffer.put(generateByteArray(contentLength));
        byteBuffer.flip();
        item = new HashMap<String, AttributeValue>();
        item.put(HASH_KEY_NAME, new AttributeValue().withB(byteBuffer));
        // Reuse the byteBuffer
        item.put("blob", new AttributeValue().withB(byteBuffer));
        item.put("blobs", new AttributeValue().withBS(ByteBuffer.wrap(generateByteArray(contentLength)), ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        // Reuse the byteBufferSet
        item.put("BS", new AttributeValue().withBS(byteBufferSet));

        putItemRequest = new PutItemRequest(binaryKeyTableName, item).withReturnValues(ReturnValue.ALL_OLD.toString());
        dynamo.putItem(putItemRequest);

        // Get our new item
        getItemResult = dynamo.getItem(new GetItemRequest(binaryKeyTableName, getMapKey(HASH_KEY_NAME,
                new AttributeValue().withB(byteBuffer))).withConsistentRead(true));
        assertNotNull(getItemResult.getItem().get("blob").getB());
        assertEquals(0, getItemResult.getItem().get("blob").getB().compareTo(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertNotNull(getItemResult.getItem().get("blobs").getBS());
        assertEquals(2, getItemResult.getItem().get("blobs").getBS().size());
        assertTrue(getItemResult.getItem().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(getItemResult.getItem().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertNotNull(getItemResult.getItem().get("BS").getBS());
        assertEquals(2, getItemResult.getItem().get("BS").getBS().size());
        assertTrue(getItemResult.getItem().get("BS").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(getItemResult.getItem().get("BS").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Load some random data
        System.out.println("Loading data...");
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            item = new HashMap<String, AttributeValue>();
            item.put(HASH_KEY_NAME, new AttributeValue("bar-" + System.currentTimeMillis()));
            item.put("age", new AttributeValue().withN(Integer.toString(random.nextInt(100) + 30)));
            item.put("bar", new AttributeValue("" + System.currentTimeMillis()));
            item.put("foos", new AttributeValue().withSS("bleh", "blah"));
            dynamo.putItem(new PutItemRequest(tableName, item).withReturnValues(ReturnValue.ALL_OLD.toString()));
        }

        // Update an item
        Map<String, AttributeValueUpdate> itemUpdates = new HashMap<String, AttributeValueUpdate>();
        itemUpdates.put("1", new AttributeValueUpdate(new AttributeValue("\u00A2"), AttributeAction.PUT.toString()));
        itemUpdates.put("foos", new AttributeValueUpdate(new AttributeValue().withSS("foo"), AttributeAction.PUT.toString()));
        itemUpdates.put("S", new AttributeValueUpdate(new AttributeValue().withSS("THREE"), AttributeAction.ADD.toString()));
        itemUpdates.put("age", new AttributeValueUpdate(new AttributeValue().withN("10"), AttributeAction.ADD.toString()));
        itemUpdates.put("blob", new AttributeValueUpdate(new AttributeValue().withB(ByteBuffer.wrap(generateByteArray(contentLength + 1))), AttributeAction.PUT.toString()));
        itemUpdates.put("blobs", new AttributeValueUpdate(new AttributeValue().withBS(ByteBuffer.wrap(generateByteArray(contentLength))), AttributeAction.PUT.toString()));
        UpdateItemRequest updateItemRequest = new UpdateItemRequest(tableName, getMapKey(HASH_KEY_NAME, new AttributeValue("bar")), itemUpdates).withReturnValues("ALL_NEW");

        UpdateItemResult updateItemResult = dynamo.updateItem(updateItemRequest);

        assertEquals("\u00A2", updateItemResult.getAttributes().get("1").getS());
        assertEquals(1, updateItemResult.getAttributes().get("foos").getSS().size());
        assertTrue(updateItemResult.getAttributes().get("foos").getSS().contains("foo"));
        assertEquals(3, updateItemResult.getAttributes().get("S").getSS().size());
        assertTrue(updateItemResult.getAttributes().get("S").getSS().contains("ONE"));
        assertTrue(updateItemResult.getAttributes().get("S").getSS().contains("TWO"));
        assertTrue(updateItemResult.getAttributes().get("S").getSS().contains("THREE"));
        assertEquals(Integer.toString(30 + 10), updateItemResult.getAttributes().get("age").getN());
        assertEquals(0, updateItemResult.getAttributes().get("blob").getB().compareTo(ByteBuffer.wrap(generateByteArray(contentLength + 1))));
        assertEquals(1, updateItemResult.getAttributes().get("blobs").getBS().size());
        assertTrue(updateItemResult.getAttributes().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength))));

        itemUpdates.clear();
        itemUpdates.put("age", new AttributeValueUpdate(new AttributeValue().withN("30"), AttributeAction.PUT.toString()));
        itemUpdates.put("blobs", new AttributeValueUpdate(new AttributeValue().withBS(ByteBuffer.wrap(generateByteArray(contentLength + 1))), AttributeAction.ADD.toString()));
        updateItemRequest = new UpdateItemRequest(tableName, getMapKey(HASH_KEY_NAME, new AttributeValue("bar")),
                itemUpdates).withReturnValues("ALL_NEW");

        updateItemResult = dynamo.updateItem(updateItemRequest);

        assertEquals("30", updateItemResult.getAttributes().get("age").getN());
        assertEquals(2, updateItemResult.getAttributes().get("blobs").getBS().size());
        assertTrue(updateItemResult.getAttributes().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength))));
        assertTrue(updateItemResult.getAttributes().get("blobs").getBS().contains(ByteBuffer.wrap(generateByteArray(contentLength + 1))));

        // Get an item that doesn't exist.
        GetItemRequest getItemsRequest = new GetItemRequest(tableName, getMapKey(HASH_KEY_NAME, new AttributeValue("3"))).withConsistentRead(true);
        GetItemResult getItemsResult = dynamo.getItem(getItemsRequest);
        assertNull(getItemsResult.getItem());

        // Get an item that doesn't have any attributes,
        getItemsRequest = new GetItemRequest(tableName, getMapKey(HASH_KEY_NAME, new AttributeValue("bar"))).withConsistentRead(true).withAttributesToGet("non-existent-attribute");
        getItemsResult = dynamo.getItem(getItemsRequest);
        assertEquals(0, getItemsResult.getItem().size());


        // Scan data
        ScanRequest scanRequest = new ScanRequest(tableName).withAttributesToGet(HASH_KEY_NAME);
        ScanResult scanResult = dynamo.scan(scanRequest);
        assertTrue(scanResult.getCount() > 0);
        assertTrue(scanResult.getScannedCount() > 0);


        // Try a more advanced Scan query and run it a few times for performance metrics
        System.out.println("Testing Scan...");
        for (int i = 0; i < 10; i++) {
            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
            scanFilter.put("age", new Condition().withAttributeValueList(new AttributeValue().withN("40")).withComparisonOperator(ComparisonOperator.GT.toString()));
            scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
            scanResult = dynamo.scan(scanRequest);
        }

        // Batch write
        HashMap<String, List<WriteRequest>> requestItems = new HashMap<String, List<WriteRequest>>();
        List<WriteRequest> writeRequests = new ArrayList<WriteRequest>();
        HashMap<String, AttributeValue> writeAttributes = new HashMap<String, AttributeValue>();
        writeAttributes.put(HASH_KEY_NAME, new AttributeValue().withS("" + System.currentTimeMillis()));
        writeAttributes.put("bar", new AttributeValue().withS("" + System.currentTimeMillis()));
        writeRequests.add(new WriteRequest().withPutRequest(new PutRequest().withItem(writeAttributes)));
        writeRequests.add(new WriteRequest().withDeleteRequest(new DeleteRequest().withKey(getMapKey(HASH_KEY_NAME, new AttributeValue().withS("toDelete")))));
        requestItems.put(tableName, writeRequests);
        BatchWriteItemResult batchWriteItem = dynamo.batchWriteItem(new BatchWriteItemRequest().withRequestItems(requestItems));
//        assertNotNull(batchWriteItem.getItemCollectionMetrics());
//        assertEquals(1, batchWriteItem.getItemCollectionMetrics().size());
//        assertEquals(tableName, batchWriteItem.getItemCollectionMetrics().entrySet().iterator().next().get);
//        assertNotNull(tableName, batchWriteItem.getResponses().iterator().next().getCapacityUnits());
        assertNotNull(batchWriteItem.getUnprocessedItems());
        assertTrue(batchWriteItem.getUnprocessedItems().isEmpty());

        // Delete some data
        DeleteItemRequest deleteItemRequest = new DeleteItemRequest(tableName, getMapKey(HASH_KEY_NAME,
                new AttributeValue("jeep"))).withReturnValues(ReturnValue.ALL_OLD.toString());
        DeleteItemResult deleteItemResult = dynamo.deleteItem(deleteItemRequest);

        // Delete our table
        DeleteTableResult deleteTable = dynamo.deleteTable(new DeleteTableRequest().withTableName(tableName));

    }

}
