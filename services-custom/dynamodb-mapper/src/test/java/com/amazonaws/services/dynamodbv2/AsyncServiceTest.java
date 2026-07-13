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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsyncServiceTest extends LocalDynamoDBTestBase {

    /** The DynamoDB asynchronous client to be used in this test. */
    private static AmazonDynamoDBAsync dynamoAsync;

    /** Hashset to record all of the tables created in this test. */
    private static final HashSet<String> createdTableNames = new HashSet<String>();

    /** Name prefix of all the tables to be created in this test. */
    private static final String ASYNC_TEST_TABLE_NAME_PREFIX = "async-java-sdk-" + System.currentTimeMillis() + "-";

    private static final String HASH_KEY_NAME = "hash";

    @BeforeClass
    public static void setUp() throws Exception {
        dynamoAsync = asyncClient();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.out.println("**********************************************************");
        System.out.println("*****************  AfterClass Procedure  *****************");
        System.out.println("**********************************************************");
        dynamoAsync.shutdown();
        for (String tableName : createdTableNames ) {
            try {
                TableUtils.waitUntilActive(dynamoAsync, tableName);
                dynamoAsync.deleteTable(new DeleteTableRequest(tableName));
            } catch ( Exception e ) {
                System.out.println("Error when trying to delect table [" + tableName + "]");
                System.out.println("Error detail: " + e.toString());
            }
        }
    }

    /**
     * Tests getting the CreateTableResult by polling Future object.
     */
    @Test(timeout=60*1000)
    public void testAsyncCreateTableByPollingFuture() throws Exception {
        String TABLE_POLLINGFUTURE_SINGLETEST = ASYNC_TEST_TABLE_NAME_PREFIX + "testTimeForPollingFuture";

        // Create a table
        recordCreatedTestTable(TABLE_POLLINGFUTURE_SINGLETEST);
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(TABLE_POLLINGFUTURE_SINGLETEST)
                .withKeySchema(new KeySchemaElement().withAttributeName(HASH_KEY_NAME).withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(HASH_KEY_NAME).withAttributeType(
                                ScalarAttributeType.S));
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(20L).withWriteCapacityUnits(10L));

        // Call the async method to create the table
        Future<CreateTableResult> futureCreateTableResult  = dynamoAsync.createTableAsync(createTableRequest);
        long startPollingTime = System.currentTimeMillis();
        while (!futureCreateTableResult.isDone()) {
            // POLLING....
            // We need to check whether the task is somehow canceled
            if (futureCreateTableResult.isCancelled()) {
                fail("[" + TABLE_POLLINGFUTURE_SINGLETEST + "] table is unexpectly canceled.");
            }
        }
        long finishPollingTime = System.currentTimeMillis();
        long timeForPolling = finishPollingTime - startPollingTime;
        System.out.println("We wasted " + timeForPolling + "ms in polling the Future!");

        TableDescription createdTableDescription = futureCreateTableResult.get().getTableDescription();
        System.out.println("Created Table: " + createdTableDescription);

        // Check whether the table is correctly created
        assertEquals(TABLE_POLLINGFUTURE_SINGLETEST, createdTableDescription.getTableName());
        assertNotNull(createdTableDescription.getTableStatus());
        assertEquals(HASH_KEY_NAME, createdTableDescription.getKeySchema().get(0).getAttributeName());
        assertEquals(KeyType.HASH.toString(), createdTableDescription.getKeySchema().get(0).getKeyType());
        assertEquals(HASH_KEY_NAME, createdTableDescription.getAttributeDefinitions().get(0).getAttributeName());
        assertEquals(ScalarAttributeType.S.toString(), createdTableDescription.getAttributeDefinitions().get(0).getAttributeType());

    }

    /**
     * Tests asynchronously processing the CreateTableResult by passing the
     * callback handler.
     */
    @Test(timeout=60*1000)
    public void testAsyncCreateTableByCallback() throws Exception {
        final String TABLE_CALlBACK_SINGLETEST = ASYNC_TEST_TABLE_NAME_PREFIX + "testTimeForCallback";

        // Create a table
        recordCreatedTestTable(TABLE_CALlBACK_SINGLETEST);
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(TABLE_CALlBACK_SINGLETEST)
                .withKeySchema(new KeySchemaElement().withAttributeName(HASH_KEY_NAME).withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(HASH_KEY_NAME).withAttributeType(
                                ScalarAttributeType.S));
        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(20L).withWriteCapacityUnits(20L));

        final long startTime = System.currentTimeMillis();
        // Call the async method to create the table
        dynamoAsync.createTableAsync(createTableRequest,
                new AsyncHandler<CreateTableRequest, CreateTableResult> () {

            public void onError(Exception exception) {
                System.out.println("MysteriousException during creating table: [" + TABLE_CALlBACK_SINGLETEST + "]");
                fail("Error detail: " + exception.toString());
            }

            public void onSuccess(CreateTableRequest request, CreateTableResult result) {
                long endTime = System.currentTimeMillis();
                long timeForPolling = endTime - startTime;
                System.out.println("The callback function is called after " + timeForPolling + "ms.");

                // Check whether the table is correctly created
                TableDescription createdTableDescription = result.getTableDescription();
                System.out.println("Created Table: " + createdTableDescription);
                assertEquals(TABLE_CALlBACK_SINGLETEST, createdTableDescription.getTableName());
                assertNotNull(createdTableDescription.getTableStatus());
                assertEquals(HASH_KEY_NAME, createdTableDescription.getKeySchema().get(0).getAttributeName());
                assertEquals(KeyType.HASH.toString(), createdTableDescription.getKeySchema().get(0).getKeyType());
                assertEquals(HASH_KEY_NAME, createdTableDescription.getAttributeDefinitions().get(0).getAttributeName());
                assertEquals(ScalarAttributeType.S.toString(), createdTableDescription.getAttributeDefinitions().get(0).getAttributeType());
            }
        });

    }

    /**
     * Tests async handler for AmazonServiceException.
     */
    @Test(timeout=60*1000)
    public void testServiceExceptionCallback() throws Exception {
        DeleteTableRequest deleteNonexistentTableRequest = new DeleteTableRequest("NONEXISTENT_TABLE");
        try {
            Future<DeleteTableResult> futureDeleteTableResult  = dynamoAsync.deleteTableAsync(deleteNonexistentTableRequest,
                        new AsyncHandler<DeleteTableRequest, DeleteTableResult> () {

                public void onError(Exception exception) {
                    assertTrue(exception instanceof AmazonServiceException);

                    AmazonServiceException ase = (AmazonServiceException)exception;
                    assertTrue(ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException"));
                }

                public void onSuccess(DeleteTableRequest request, DeleteTableResult result) {
                    fail("We are not supposed to be in onCompleted handler!");
                }
            });
            // For backward compatibility, we need to make sure that the ASE is rethrown to the calling thread
            // So... we wait for it!!
            System.out.println("Waiting for the ServiceExeption rethrown into the main thread!");
            futureDeleteTableResult.get();
            fail("Expected AmazonServiceException, but wasn't thrown");
        } catch (AmazonServiceException ex) {
            fail("The exception should be wrapped in ExcecutionException!");
        } catch (ExecutionException ex) {
            System.out.println("Successully catch the ExcecutionException in the calling thread!");
            assertTrue(ex.getCause() instanceof AmazonServiceException);
        } catch (Exception ex) {
            fail ("The exception should be wrapped in ExcecutionException!");
        }
    }

    /**
     * Tests async handler for other runtime exceptions.
     */
    @Test(timeout=60*1000)
    public void testClientExceptionCallback() throws Exception {
        // Passing a null request would trigger an AmazonClientExeption by
        CreateTableRequest createTableRequest = null;
        try {
            Future<CreateTableResult> futureDeleteTableResult  = dynamoAsync.createTableAsync(createTableRequest,
                        new AsyncHandler<CreateTableRequest, CreateTableResult> () {
                public void onError(Exception exception) {
                    assertTrue(exception instanceof NullPointerException);
                }

                public void onSuccess(CreateTableRequest request, CreateTableResult result) {
                    fail("We are not supposed to be in onCompleted handler!");
                }
            });
            // For backward compatibility, we need to make sure that the ACE is rethrown to the calling thread
            // So... we wait for it!!
            System.out.println("Waiting for the ClientExeption rethrown into the main thread!");
            futureDeleteTableResult.get();
            fail("Expected AmazonServiceException, but wasn't thrown");
        } catch (AmazonServiceException ex) {
            fail("The exception should be wrapped in ExcecutionException!");
        } catch (ExecutionException ex) {
            System.out.println("Successully catch the ExcecutionException in the calling thread!");
            assertTrue(ex.getCause() instanceof NullPointerException);
        } catch (Exception ex) {
            fail ("The exception should be wrapped in ExcecutionException!");
        }
    }

    /**
     * Record the created test table, so that tearDown() will clean up all
     * these temporary tables.
     */
    private static void recordCreatedTestTable(String tableName) {
        createdTableNames.add(tableName);
    }
}
