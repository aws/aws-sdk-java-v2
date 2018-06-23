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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import software.amazon.awssdk.services.dynamodb.util.TableUtils.TableNeverTransitionedToStateException;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class TableUtilsIntegrationTest extends AwsIntegrationTestBase {

    private static final int CUSTOM_TIMEOUT = 5 * 1000;

    /**
     * Wait a generous amount of time after the custom timeout to account for
     * variance due to polling interval. This is only used in tests that use
     * {@link TableUtilsIntegrationTest#CUSTOM_TIMEOUT}
     */
    private static final int TEST_TIMEOUT = CUSTOM_TIMEOUT * 2;

    private static final int CUSTOM_POLLING_INTERVAL = 1 * 1000;
    private static final long READ_CAPACITY = 5L;
    private static final long WRITE_CAPACITY = 5L;
    private static final String HASH_KEY_NAME = "someHash";

    private static DynamoDbClient ddb;
    private String tableName;

    @BeforeClass
    public static void setupFixture() {
        ddb = DynamoDbClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    private CreateTableRequest createTableRequest() {
        return CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(KeySchemaElement.builder()
                        .keyType(KeyType.HASH)
                        .attributeName(HASH_KEY_NAME).build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(HASH_KEY_NAME)
                        .attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(READ_CAPACITY)
                        .writeCapacityUnits(WRITE_CAPACITY).build())
                .build();
    }

    private DeleteTableRequest deleteTableRequest() {
        return DeleteTableRequest.builder().tableName(tableName).build();
    }

    private void createTable() {
        ddb.createTable(createTableRequest());
    }

    @Before
    public void setup() {
        tableName = "TableUtilsTest-" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws InterruptedException {
        if (tableStatus() != null) {
            if (!tableStatus().equals(TableStatus.DELETING)) {
                TableUtils.waitUntilActive(ddb, tableName);
                ddb.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
            }
            waitUntilTableDeleted();
        }
    }

    /**
     * @return Table status or null if it doesn't exist.
     */
    private String tableStatus() {
        try {
            return ddb.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table().tableStatusAsString();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    // TODO replace with waiters when available.
    private void waitUntilTableDeleted() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        // Wait up to five minutes for a table to be deleted.
        long endTime = startTime + 5 * 60 * 1000;
        while (System.currentTimeMillis() < endTime) {
            try {
                ddb.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
                Thread.sleep(1000);
            } catch (ResourceNotFoundException e) {
                return;
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitUntilActive_InvalidTimeout_ThrowsException() throws Exception {
        TableUtils.waitUntilActive(ddb, tableName, -1, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitUntilActive_InvalidInterval_ThrowsException() throws Exception {
        TableUtils.waitUntilActive(ddb, tableName, 10, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitUntilActive_IntervalGreaterThanTimeout_ThrowsException() throws Exception {
        TableUtils.waitUntilActive(ddb, tableName, 10, 100);
    }

    @Test
    public void waitUntilActive_MethodBlocksUntilTableIsActive() throws Exception {
        createTable();
        TableUtils.waitUntilActive(ddb, tableName);
        assertEquals(TableStatus.ACTIVE,
                     ddb.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table().tableStatus());
    }

    @Test(expected = TableNeverTransitionedToStateException.class, timeout = TEST_TIMEOUT)
    public void waitUntilActive_TableNeverTransitionsToActive_ThrowsException() throws Exception {
        createTable();
        // We wait long enough for DescribeTable to return something but not
        // long enough for the table to transition to active
        TableUtils.waitUntilActive(ddb, tableName, 1 * 1000, 500);
    }

    @Test(expected = TableNeverTransitionedToStateException.class, timeout = TEST_TIMEOUT)
    public void waitUntilActive_NoSuchTable_BlocksUntilTimeoutThenThrowsException() throws
                                                                                    InterruptedException {
        TableUtils.waitUntilActive(ddb, tableName, CUSTOM_TIMEOUT, CUSTOM_POLLING_INTERVAL);
    }

    @Test
    public void waitUntilExists_MethodBlocksUntilTableExists() throws InterruptedException {
        createTable();
        TableUtils.waitUntilExists(ddb, tableName);
        assertNotNull(ddb.describeTable(DescribeTableRequest.builder().tableName(tableName).build()));
    }

    @Test(expected = SdkClientException.class, timeout = TEST_TIMEOUT)
    public void waitUntilExists_NoSuchTable_BlocksUntilTimeoutThenThrowsException() throws
                                                                                    InterruptedException {
        TableUtils.waitUntilExists(ddb, tableName, CUSTOM_TIMEOUT, CUSTOM_POLLING_INTERVAL);
    }

    @Test
    public void testCreateTableIfNotExists() throws InterruptedException {
        assertTrue(TableUtils.createTableIfNotExists(ddb, createTableRequest()));
        TableUtils.waitUntilExists(ddb, tableName);
        assertFalse(TableUtils.createTableIfNotExists(ddb, createTableRequest()));
    }

    @Test
    public void testDeleteTableIfExists() throws InterruptedException {
        assertFalse(TableUtils.deleteTableIfExists(ddb, deleteTableRequest()));
        createTable();
        TableUtils.waitUntilActive(ddb, tableName);
        assertTrue(TableUtils.deleteTableIfExists(ddb, deleteTableRequest()));
        waitUntilTableDeleted();
    }

}
