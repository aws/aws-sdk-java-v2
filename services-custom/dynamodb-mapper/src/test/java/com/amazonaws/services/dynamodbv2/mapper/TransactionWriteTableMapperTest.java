/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
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
package com.amazonaws.services.dynamodbv2.mapper;

import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.TableNameResolver;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.ConditionExpressionMode;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createMultipleObjectTestDataForTransactionWrite;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createStringHashKeyTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.deleteTestTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionWriteTestData;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionWriteTestRequest;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.generateStringMatcherTransactWriteExpression;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.generateStringContainsTransactWriteExpression;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getHashKeyRangeKeyObject;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getStringAttributeClass;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getTestItemObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMappingException;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTableMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTransactionWriteExpression;
import software.amazon.awssdk.mapper.dynamodb.TransactionLoadRequest;
import software.amazon.awssdk.mapper.dynamodb.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CancellationReason;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;
import com.amazonaws.services.dynamodbv2.pojos.TestItem;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class TransactionWriteTableMapperTest extends TransactionsTestBase {
    private static final String stringAttributeClassTableName1 = "sdk-util-1e5658e0-034e-4b90-90cb-13b00cfc866a";
    private static final String stringAttributeClassTableName2 = "sdk-util-21e0905c-0b85-4675-9c0f-b0e5f6d408c9";

    private static DynamoDBTableMapper<TestItem, String, Long> tableMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        TransactionsTestBase.setUp();
        tableMapper = dynamoMapper.newTableMapper(TestItem.class);
    }

    @Test
    public void testAllOperationsWithSingleItem() {
        TestItem obj = getTestItemObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(obj);

        // Put one object
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), Arrays.asList((Object) obj));

        // Update the object
        String updatedStringAttribute = "updatedStringAttribute";
        obj.setStringAttribute(updatedStringAttribute);
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addUpdate(obj);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), Arrays.asList((Object) obj));

        // ConditionCheck the object
        DynamoDBTransactionWriteExpression writeExpression = generateStringMatcherTransactWriteExpression(updatedStringAttribute,
                                                                                                          "not" + updatedStringAttribute);
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addConditionCheck(obj, writeExpression);
        // Succeeds
        tableMapper.transactionWrite(transactionWriteRequest);

        // Delete
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(obj);
        List<Object> expectedObjects = new ArrayList<Object>();
        expectedObjects.add(null);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), expectedObjects);
    }

    @Test
    public void testAllOperationsWithSingleItemAndUnusualExistence() {
        TestItem existingObject = getTestItemObject();
        tableMapper.save(existingObject);
        TestItem objectToPut = getTestItemObject();
        objectToPut.setHashKey(existingObject.getHashKey());
        objectToPut.setRangeKey(existingObject.getRangeKey());
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(objectToPut);

        // Put one object which overwrites existing object
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) existingObject), Arrays.asList((Object) objectToPut));

        // Update a non-existing object
        TestItem objectToUpdate = getTestItemObject();
        String updatedStringAttribute = "updatedStringAttribute";
        objectToUpdate.setStringAttribute(updatedStringAttribute);
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addUpdate(objectToUpdate);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) objectToUpdate), Arrays.asList((Object) objectToUpdate));

        // ConditionCheck on a non-existing object
        TestItem objectToConditionCheck = getTestItemObject();
        DynamoDBTransactionWriteExpression writeExpression = generateStringMatcherTransactWriteExpression("someString",
                                                                                                          "someOtherString");
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addConditionCheck(objectToConditionCheck, writeExpression);
        try {
            tableMapper.transactionWrite(transactionWriteRequest);
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException ex) {
            // Expected
        }

        // Delete a non-existing object
        TestItem objectToDelete = getTestItemObject();
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(objectToDelete);
        List<Object> expectedObjects = new ArrayList<Object>();
        expectedObjects.add(null);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) objectToDelete), expectedObjects);
    }

    @Test
    public void testWithSingleInvalidObjectTypeItem() {
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(getStringAttributeClass());
        try {
            tableMapper.transactionWrite(transactionWriteRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException ex) {
            String expectedMessage = "Input object is of the classType: " + StringAttributeClass.class + " but tableMapper is declared with classType: "
                                        + TestItem.class;
            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Test
    public void testMultipleMixedOperationsOnMultipleTables() throws InterruptedException {
        final int numObjects = 6;
        TableNameResolver tableNameResolver = new TableNameResolver() {
            private int counter;
            private boolean savingForTestSetup = true;
            private boolean readingForTestVerification = false;

            /**
             * This method gets called in three flows:
             *      1. From DynamoDBMapper.save for every object except first 3 objects (because we use ConditionExpressionMode.NONE for puts so corresponding
             *         objects are not saved as part of test data creation)
             *      2. From DynamoDBMapper.transactionWrite for every object (because we add every object as input to transactionWrite)
             *      3. From DynamoDBMapper.transactionLoad for every object except objects at index 6 and 7 (because we add every object as input to
             *         transactionLoad for verification except conditionCheck objects)
             *
             * So, to keep resulting table name same across save/transactionWrite/transactionLoad operations for same object, we do the following:
             *      1. If savingForTestSetup is true and counter = 0, set counter = 3 (this effectively results in getTableName calls being avoided for
             *         first 3 objects while saving)
             *      2. If counter >= numObjects and savingForTestSetup is true (meaning we've completed save flow), we reset counter and set savingForTestSetup to false
             *      3. If counter >= numObjects and savingForTestSetup is false (meaning we've completed transactionWrite flow), we reset counter and set readingForTestVerification to true
             *      4. If readingForTestVerification is true and counter = 5, set counter = 8 (this effectively results in getTableName calls being avoided for
             *         objects at index 6 and 7 during transactionLoad)
             */
            @Override
            public String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
                if (!StringAttributeClass.class.equals(clazz)) {
                    throw new IllegalArgumentException("Input object must be of type: " + StringAttributeClass.class);
                }

                if (savingForTestSetup && counter == 0) {
                    counter = 3;
                }

                String tableName;
                if (counter % 3 == 0) {
                    tableName = sdkUtilTableName;
                } else if (counter % 3 == 1) {
                    tableName = stringAttributeClassTableName1;
                } else {
                    tableName = stringAttributeClassTableName2;
                }

                if (readingForTestVerification && counter == 5) {
                    counter = 8;
                } else {
                    counter++;
                }

                if (counter >= numObjects) {
                    counter = 0;
                    if (savingForTestSetup) {
                        savingForTestSetup = false;
                    } else {
                        readingForTestVerification = true;
                    }
                }

                return tableName;
            }
        };
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder().withTableNameResolver(tableNameResolver).build();
        DynamoDBMapper mapperWithTableNameResolver = new DynamoDBMapper(dynamo, config);
        DynamoDBTableMapper<StringAttributeClass, String, ?> tableMapperWithTableNameResolver = mapperWithTableNameResolver.newTableMapper(StringAttributeClass.class);

        try {
            createStringHashKeyTable(dynamo, stringAttributeClassTableName1);
            createStringHashKeyTable(dynamo, stringAttributeClassTableName2);
            TransactionWriteTestRequest testRequest = new TransactionWriteTestRequest().withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                                       .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                                       .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                                       .withIsMultiTable(false);
            TransactionWriteTestData testData = createMultipleObjectTestDataForTransactionWrite(mapperWithTableNameResolver, testRequest);

            executeAndValidateTransactionWrite(tableMapperWithTableNameResolver, testData.getTransactionWriteRequest(), testData.getExpectedObjectKeys(), testData.getExpectedObjects());
        } finally {
            deleteTestTable(dynamo, stringAttributeClassTableName1);
            deleteTestTable(dynamo, stringAttributeClassTableName2);
        }
    }

    @Test
    public void testWithMultipleMixedOperationsOfWhichSomeHaveInvalidObjectTypeItems() {
        // Add operations with valid object types
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(getTestItemObject());
        transactionWriteRequest.addUpdate(getTestItemObject());
        TestItem objectToConditionCheck = getTestItemObject();
        DynamoDBTransactionWriteExpression conditionCheckWriteExpression = generateStringContainsTransactWriteExpression(objectToConditionCheck.getStringAttribute());
        transactionWriteRequest.addConditionCheck(objectToConditionCheck, conditionCheckWriteExpression);
        transactionWriteRequest.addDelete(getTestItemObject());

        // Add operations with mix of valid and invalid object types
        transactionWriteRequest.addPut(getStringAttributeClass());
        transactionWriteRequest.addUpdate(getTestItemObject());
        transactionWriteRequest.addUpdate(getHashKeyRangeKeyObject());

        try {
            tableMapper.transactionWrite(transactionWriteRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException ex) {
            String expectedMessage = "Input object is of the classType: " + StringAttributeClass.class + " but tableMapper is declared with classType: "
                                        + TestItem.class;
            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Test
    public void testMultipleMixedOperationsOfWhichSomeCauseTransactionCancelled() {
        DynamoDBTableMapper<StringAttributeClass, String, ?> tableMapper = dynamoMapper.newTableMapper(StringAttributeClass.class);
        TransactionWriteTestRequest testRequest = new TransactionWriteTestRequest().withPutConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                                   .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                                   .withConditionCheckConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                                   .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                                   .withIsMultiTable(false);
        TransactionWriteTestData testData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, testRequest);

        try {
            executeAndValidateTransactionWrite(tableMapper, testData.getTransactionWriteRequest(), testData.getExpectedObjectKeys(), testData.getExpectedObjects());
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    private void validateItem(Map<String, AttributeValue> item, Object expectedObject) {
        if (!StringAttributeClass.class.equals(expectedObject.getClass())) {
            throw new IllegalArgumentException("Unsupported expectedObject type: " + expectedObject.getClass());
        }
        StringAttributeClass expectedStringAttributeObject = (StringAttributeClass) expectedObject;
        assertEquals(expectedStringAttributeObject.getKey(), item.get("key").getS());
        assertEquals(expectedStringAttributeObject.getStringAttribute(), item.get("stringAttribute").getS());
        assertEquals(expectedStringAttributeObject.getRenamedAttribute(), item.get("originalName").getS());
    }

    @Override
    protected void executeAndValidateTransactionWrite(TransactionWriteRequest transactionWriteRequest,
                                                      List<Object> expectedResponseObjectKeys,
                                                      List<Object> expectedResponseObjects) {
        executeAndValidateTransactionWrite(tableMapper, transactionWriteRequest, expectedResponseObjectKeys, expectedResponseObjects);
    }

    protected void executeAndValidateTransactionWrite(DynamoDBTableMapper tableMapper,
                                                      TransactionWriteRequest transactionWriteRequest,
                                                      List<Object> expectedResponseObjectKeys,
                                                      List<Object> expectedResponseObjects) {
        tableMapper.transactionWrite(transactionWriteRequest);
        int numItems = expectedResponseObjectKeys.size();
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        for (Object objKey: expectedResponseObjectKeys) {
            transactionLoadRequest.addLoad(objKey);
        }

        // transactionLoad can sometimes fail due to conflict with above transactionWrite which is not yet complete, so we re-try for some time when
        // that type of failure is encountered
        long endTime = System.currentTimeMillis() + 30 * 1000;
        List<Object> actualResponseObjects = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                actualResponseObjects = tableMapper.transactionLoad(transactionLoadRequest);
                break;
            } catch (TransactionCanceledException tce) {
                List<CancellationReason> cancellationReasons = tce.getCancellationReasons();
                Set<String> uniqueCancellationReasonCodes = new HashSet<String>();
                for (CancellationReason cancellationReason: cancellationReasons) {
                    uniqueCancellationReasonCodes.add(cancellationReason.getCode());
                }
                if (uniqueCancellationReasonCodes.size() != 1 || !"TransactionConflict".equals(cancellationReasons.get(0).getCode())) {
                    fail("transactionLoad failed with TransactionCanceledException having non-TransactionConflict cancellation reason(s): " + tce);
                }
                // Sleep for some time before re-trying transactionLoad
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    fail("interrupted while waiting to re-try transactionLoad: " + ie);
                }
            }
        }
        if (actualResponseObjects == null) {
            fail("Timed out while executing transactionLoad due to conflict with ongoing transactionWrite");
        }

        assertEquals(numItems, actualResponseObjects.size());
        for (int i = 0; i < numItems; i++) {
            assertEquals(expectedResponseObjects.get(i), actualResponseObjects.get(i));
        }
    }
}
