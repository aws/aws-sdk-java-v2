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

package software.amazon.awssdk.services.dynamodb.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.spec.GetItemSpec;
import software.amazon.awssdk.services.dynamodb.document.utils.NameMap;
import software.amazon.awssdk.services.dynamodb.document.utils.ValueMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class UpdateItemIntegrationTest {

    private static final long READ_CAPACITY = 1;
    private static final long WRITE_CAPACITY = 1;
    private static final Long FIRST_CUSTOMER_ID = 1000L;
    private static final String ADDRESS_TYPE_HOME = "home";
    private static final String ADDRESS_TYPE_WORK = "work";
    private static DynamoDb dynamoDb;
    private static String TABLE_NAME = "UpdateItemIntegrationTest";
    private static String HASH_KEY = "customer_id";
    private static String RANGE_KEY = "address_type";

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBClient client = DynamoDBClient.builder()
                                              .credentialsProvider(AwsIntegrationTestBase.CREDENTIALS_PROVIDER_CHAIN)
                                              .build();
        dynamoDb = new DynamoDb(client);

        createTable();
        fillInData();
    }

    private static void createTable() throws Exception {
        Table table = dynamoDb.getTable(TABLE_NAME);
        TableDescription desc = table.waitForActiveOrDelete();
        if (desc == null) {
            // table doesn't exist; let's create it
            KeySchemaElement hashKey =
                    KeySchemaElement.builder().attributeName(HASH_KEY).keyType(KeyType.HASH).build();
            KeySchemaElement rangeKey =
                    KeySchemaElement.builder().attributeName(RANGE_KEY).keyType(KeyType.RANGE).build();
            CreateTableRequest createTableRequest = CreateTableRequest.builder().
                    tableName(TABLE_NAME)
                    .keySchema(Arrays.asList(hashKey, rangeKey))
                    .attributeDefinitions(
                                    AttributeDefinition.builder().attributeName(HASH_KEY).attributeType(ScalarAttributeType.N).build(),
                                    AttributeDefinition.builder().attributeName(RANGE_KEY).attributeType(ScalarAttributeType.S).build())
                    .provisionedThroughput(
                                    ProvisionedThroughput.builder().readCapacityUnits(READ_CAPACITY).writeCapacityUnits(WRITE_CAPACITY).build())
                    .build();
            table = dynamoDb.createTable(createTableRequest);
            table.waitForActive();
        }
    }

    private static void fillInData() {
        Table table = dynamoDb.getTable(TABLE_NAME);
        table.putItem(new Item().with(HASH_KEY, FIRST_CUSTOMER_ID)
                                .with(RANGE_KEY, ADDRESS_TYPE_WORK)
                                .with("AddressLine1", "1918 8th Aven")
                                .with("city", "seattle")
                                .with("state", "WA")
                                .with("zipcode", 98104));
        table.putItem(new Item().with(HASH_KEY, FIRST_CUSTOMER_ID)
                                .with(RANGE_KEY, ADDRESS_TYPE_HOME)
                                .with("AddressLine1", "15606 NE 40th ST")
                                .with("city", "redmond")
                                .with("state", "WA")
                                .with("zipcode", 98052));
    }

    @AfterClass
    public static void shutDown() throws Exception {
        //        Table table = dynamoDB.getTable(TABLE_NAME);
        //        table.delete();
        dynamoDb.shutdown();
    }

    /**
     * This test case tests the various methods in AttributeUpdate class. At
     * each phase, retrieves the items and compares its values.
     */
    @Test
    public void testAddingNewAttributeToExistingRow() {
        final String phoneNumber1 = "123-456-7890";
        final Set<String> phoneNumbers = new HashSet<String>();
        phoneNumbers.add(phoneNumber1);

        // Adds a new attribute to the row.
        Table table = dynamoDb.getTable(TABLE_NAME);
        table.updateItem(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK,
                         new AttributeUpdate("phone").put(phoneNumbers));
        Item item = table.getItem(new GetItemSpec()
                                          .withPrimaryKey(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK)
                                          .withConsistentRead(true)
                                 );
        Set<String> phoneNumbersRetrieved = item.getStringSet("phone");
        assertEquals(phoneNumbers, phoneNumbersRetrieved);
        assertTrue(phoneNumbersRetrieved.contains(phoneNumber1));
        assertTrue(phoneNumbersRetrieved.size() == 1);

        // Adds a new element to the attribute
        final String phoneNumber2 = "987-654-3210";
        table.updateItem(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK,
                         new AttributeUpdate("phone").addElements(phoneNumber2));
        item = table.getItem(new GetItemSpec()
                                     .withPrimaryKey(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK)
                                     .withConsistentRead(true));
        phoneNumbersRetrieved = item.getStringSet("phone");
        assertTrue(phoneNumbersRetrieved.contains(phoneNumber2));
        assertTrue(phoneNumbersRetrieved.contains(phoneNumber1));
        assertTrue(phoneNumbersRetrieved.size() == 2);

        // removes an element from the attribute
        table.updateItem(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK,
                         new AttributeUpdate("phone").removeElements(phoneNumber2));
        item = table.getItem(new GetItemSpec()
                                     .withPrimaryKey(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK)
                                     .withConsistentRead(true));
        phoneNumbersRetrieved = item.getStringSet("phone");
        assertFalse(phoneNumbersRetrieved.contains(phoneNumber2));
        assertTrue(phoneNumbersRetrieved.contains(phoneNumber1));
        assertTrue(phoneNumbersRetrieved.size() == 1);

        // deletes the attribute
        table.updateItem(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK, new AttributeUpdate("phone").delete());
        item = table.getItem(new GetItemSpec()
                                     .withPrimaryKey(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK)
                                     .withConsistentRead(true));
        phoneNumbersRetrieved = item.getStringSet("phone");
        assertNull(phoneNumbersRetrieved);

        final Number oldValue = item.getNumber("zipcode");

        // Increments the zip code attribute
        table.updateItem(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK,
                         new AttributeUpdate("zipcode").addNumeric(1));
        item = table.getItem(new GetItemSpec()
                                     .withPrimaryKey(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK)
                                     .withConsistentRead(true));
        Number newValue = item.getNumber("zipcode");
        assertEquals(oldValue.longValue() + 1, newValue.longValue());

        // Decrements the zip code attribute
        table.updateItem(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK,
                         new AttributeUpdate("zipcode").addNumeric(-1));
        item = table.getItem(new GetItemSpec()
                                     .withPrimaryKey(HASH_KEY, FIRST_CUSTOMER_ID, RANGE_KEY, ADDRESS_TYPE_WORK)
                                     .withConsistentRead(true));
        newValue = item.getNumber("zipcode");
        assertEquals(oldValue.longValue(), newValue.longValue());
    }

    /**
     * This test cases performs an update item with expected set. The update
     * must fail as the expected condition is not met.
     */
    @Test
    public void testUpdateItemWithExpectedSet() {
        final String phoneNumber1 = "123-456-7890";
        final String phoneNumber2 = "987-654-3210";
        final Set<String> phoneNumbers = new HashSet<String>();
        phoneNumbers.add(phoneNumber1);
        Table table = dynamoDb.getTable(TABLE_NAME);
        try {
            table.updateItem(
                    HASH_KEY, FIRST_CUSTOMER_ID,
                    RANGE_KEY, ADDRESS_TYPE_WORK,
                    Arrays.asList(new Expected("phone").eq(phoneNumbers)),
                    new AttributeUpdate("phone").addElements(phoneNumber2));
            fail("Update Should fail as the phone number attribute is not present in the row");
        } catch (Exception e) {
            assertTrue(e instanceof SdkServiceException);
        }
    }

    /**
     * Performs an update using the update expression. Asserts by retrieving the
     * item and checking if the update values are present in the record.
     */
    @Test
    public void testUpdateItemWithUpdateExpression() {
        final String phoneNumber1 = "123-456-7890";
        final String phoneNumber2 = "987-654-3210";
        final Set<String> phoneNumbers = new HashSet<String>();
        phoneNumbers.add(phoneNumber1);
        phoneNumbers.add(phoneNumber2);
        final String updateExpression = "set #phoneAttributeName = :phoneAtributeValue";

        final Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#phoneAttributeName", "phone");
        final Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":phoneAtributeValue", phoneNumbers);

        Table table = dynamoDb.getTable(TABLE_NAME);
        table.updateItem(
                HASH_KEY, FIRST_CUSTOMER_ID,
                RANGE_KEY, ADDRESS_TYPE_WORK,
                updateExpression, nameMap, valueMap);
        Item item = table.getItem(new GetItemSpec()
                                          .withPrimaryKey(
                                                  HASH_KEY, FIRST_CUSTOMER_ID,
                                                  RANGE_KEY, ADDRESS_TYPE_WORK)
                                          .withConsistentRead(true));
        Set<String> phoneNumbersRetrieved = item.getStringSet("phone");
        assertNotNull(phoneNumbersRetrieved);
        assertTrue(phoneNumbersRetrieved.size() == 2);
        assertTrue(phoneNumbersRetrieved.contains(phoneNumber1));
        assertTrue(phoneNumbersRetrieved.contains(phoneNumber2));
    }

    /**
     * Performs an update using the update and conditional expression. The
     * update should fail as the conditional expression fails to true.
     */
    @Test
    public void testUpdateItemWithConditionExpression() {
        Table table = dynamoDb.getTable(TABLE_NAME);
        try {
            table.updateItem(
                    HASH_KEY, FIRST_CUSTOMER_ID,
                    RANGE_KEY, ADDRESS_TYPE_WORK,
                    "set #mno = list_append(:phoneNumber, :phoneNumber)",
                    "zipcode = :zipcode",
                    new NameMap().with("#mno", "phone"),
                    new ValueMap()
                            .withList(":phoneNumber", "987-654-3210")
                            // compare zipecode, which is of type int, to string
                            // leading to an intentional failure in the update condition
                            .withString(":zipcode", "98104")
                            );
            fail("Update Should fail as the zip code mentioned in the conditon expression doesn't match");
        } catch (SdkServiceException e) {
            assertTrue(e.getMessage().contains("conditional request failed"));
        }
    }

}
