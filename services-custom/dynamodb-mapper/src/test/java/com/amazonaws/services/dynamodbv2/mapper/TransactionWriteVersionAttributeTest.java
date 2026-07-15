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

import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createMultipleObjectTestDataForTransactionWrite;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getMultiVersionRangeKeyObject;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getUniqueRangeKeyObject;
import static org.junit.Assert.assertEquals;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.TestObjectCreator;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTransactionWriteExpression;
import software.amazon.awssdk.mapper.dynamodb.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.amazonaws.services.dynamodbv2.pojos.MultiVersionRangeKeyClass;
import com.amazonaws.services.dynamodbv2.pojos.RangeKeyClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TransactionWriteVersionAttributeTest extends TransactionsTestBase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final String VERSION_AND_CONDITION_FAILURE_MESSAGE =
            "A transactional write operation may not also specify a condition " +
                    "expression if a versioned attribute is present on the " +
                    "model of the item.";

    @Test
    public void testTransactionWriteForVersionedAttributeWithConditionExpressionFailsOnPut() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");

        // Put
        transactionWriteRequest.addPut(obj, writeExpression);

        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);
        dynamoMapper.transactionWrite(transactionWriteRequest);
    }

    @Test
    public void testTransactionWriteForVersionedAttributeWithConditionExpressionFailsOnUpdate() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");
        // Update
        transactionWriteRequest.addUpdate(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);

        dynamoMapper.transactionWrite(transactionWriteRequest);
    }

    @Test
    public void testTransactionWriteForVersionedAttributeWithConditionExpressionFailsOnDelete() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");
        // Delete
        transactionWriteRequest.addDelete(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);

        dynamoMapper.transactionWrite(transactionWriteRequest);
    }

    @Test
    public void testTransactionWriteForVersionedAttributeWithConditionExpressionFailsOnConditionCheck() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");
        // ConditionCheck
        transactionWriteRequest.addConditionCheck(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);

        dynamoMapper.transactionWrite(transactionWriteRequest);
    }

    @Test
    public void testTransactionWriteForNullVersionAttributeSucceedsOnPut() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();

        // Put
        transactionWriteRequest.addPut(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        // Verify that item is updated with proper version
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testTransactionWriteForNullVersionAttributeSucceedsOnUpdate() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();

        // Update
        transactionWriteRequest.addUpdate(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        // Verify that item is updated with proper version
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testTransactionWriteForNonNullVersionAttributeSucceedsOnPut() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();

        // Put
        transactionWriteRequest.addPut(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        // Verify that item is updated with proper version
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testTransactionWriteForNonNullVersionAttributeSucceedsOnUpdate() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        // Update
        obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addUpdate(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        // Verify that item is updated with proper version
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testTransactionWriteForNonNullVersionAttributeSucceedsOnDelete() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();

        // Delete
        obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        // Verify that item is deleted
        assertEquals(null, dynamoMapper.load(obj));
    }

    @Test
    public void testTransactionWriteForHigherVersionAttributeFailsForPut() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        obj.setVersion(3L);

        // Put
        transactionWriteRequest.addPut(obj);
        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
        } catch (TransactionCanceledException tce) {
            // Expected
        }

    }

    @Test
    public void testTransactionWriteForHigherVersionAttributeFailsForUpdate() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        obj.setVersion(3L);
        // Update
        transactionWriteRequest.addUpdate(obj);
        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    @Test
    public void testTransactionWriteForHigherVersionAttributeFails() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        dynamoMapper.save(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        obj.setVersion(3L);

        // Delete
        transactionWriteRequest.addDelete(obj);
        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
        } catch (TransactionCanceledException tce) {
            // Expected
        }

    }

    @Test
    public void testMultipleOperationsWithConditionAndVersionAttributeOnPut() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");

        transactionWriteRequest.addPut(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);
        dynamoMapper.transactionWrite(transactionWriteRequest);
    }

    @Test
    public void testMultipleOperationsWithConditionAndVersionAttributeOnUpdate() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");

        transactionWriteRequest.addUpdate(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);
        dynamoMapper.transactionWrite(transactionWriteRequest);

    }

    @Test
    public void testMultipleOperationsWithConditionAndVersionAttributeOnConditionCheck() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");

        transactionWriteRequest.addConditionCheck(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);
        dynamoMapper.transactionWrite(transactionWriteRequest);

    }

    @Test
    public void testMultipleOperationsWithConditionAndVersionAttributeOnDelete() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        DynamoDBTransactionWriteExpression writeExpression = new DynamoDBTransactionWriteExpression();
        writeExpression.withConditionExpression("SomeConditionExpression");

        transactionWriteRequest.addDelete(obj, writeExpression);
        exception.expect(SdkClientException.class);
        exception.expectMessage(VERSION_AND_CONDITION_FAILURE_MESSAGE);
        dynamoMapper.transactionWrite(transactionWriteRequest);
    }

    @Test
    public void testMultipleOperationsWithNullValuedVersionOnPut() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        transactionWriteRequest.addPut(obj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, obj, obj, false /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion());
    }

    @Test
    public void testMultipleOperationsWithNullValuedVersionOnUpdate() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        transactionWriteRequest.addUpdate(obj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, obj, obj, false /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion());
    }

    @Test
    public void testMultipleOperationsWithNullValuedVersionOnPutAndUpdate() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass putObj = getUniqueRangeKeyObject();
        transactionWriteRequest.addPut(putObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, putObj, putObj, false /* saveObjectToTable*/);
        RangeKeyClass updateObj = getUniqueRangeKeyObject();
        transactionWriteRequest.addUpdate(updateObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, updateObj, updateObj, false /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        RangeKeyClass putObjectOnTable = dynamoMapper.load(putObj);
        assertEquals(putObj, putObjectOnTable);
        RangeKeyClass updateObjectOnTable = dynamoMapper.load(updateObj);
        assertEquals(updateObj, updateObjectOnTable);
    }

    @Test
    public void testMultipleOperationsWithNonNullValuedVersionOnPut() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        transactionWriteRequest.addPut(obj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, obj, obj, true /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testMultipleOperationsWithNonNullValuedVersionOnUpdate() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        transactionWriteRequest.addUpdate(obj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, obj, obj, true /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        RangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
    }

    @Test
    public void testMultipleOperationsWithNonNullValuedVersionOnDelete() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        transactionWriteRequest.addDelete(obj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, obj, null, true/* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithNonNullValuedVersionOnPutUpdateAndDelete() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        RangeKeyClass putObj = getUniqueRangeKeyObject();
        transactionWriteRequest.addPut(putObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, putObj, putObj, true /* saveObjectToTable*/);
        RangeKeyClass updateObj = getUniqueRangeKeyObject();
        transactionWriteRequest.addUpdate(updateObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, updateObj, updateObj, true /* saveObjectToTable*/);
        RangeKeyClass deleteObj = getUniqueRangeKeyObject();
        transactionWriteRequest.addDelete(deleteObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, deleteObj, null, true /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        RangeKeyClass putObjectOnTable = dynamoMapper.load(putObj);
        assertEquals(putObj, putObjectOnTable);
        RangeKeyClass updateObjectOnTable = dynamoMapper.load(updateObj);
        assertEquals(updateObj, updateObjectOnTable);
        RangeKeyClass objectOnTable = dynamoMapper.load(putObj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        objectOnTable = dynamoMapper.load(updateObj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
    }

    @Test
    public void testNullValuedMultiVersionAttributeOnPutSucceeds() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();

        // Put
        transactionWriteRequest.addPut(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        // Verify that item is updated with proper version
        MultiVersionRangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion());
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion2());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testNullValuedMultiVersionAttributeOnUpdateSucceeds() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();

        // Update
        transactionWriteRequest = new TransactionWriteRequest();
        obj = getMultiVersionRangeKeyObject();
        transactionWriteRequest.addUpdate(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        MultiVersionRangeKeyClass objectOnTable = dynamoMapper.load(obj);
        objectOnTable = dynamoMapper.load(obj);
        // Verify that item is updated with proper version
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion());
        assertEquals(Long.valueOf(1L), objectOnTable.getVersion2());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testNonNullValuedMultiVersionAttributeOnPutSucceeds() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(obj);
        dynamoMapper.save(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        // Verify that item is updated with proper version
        MultiVersionRangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion2());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testNonNullValuedMultiVersionAttributeOnUpdateSucceeds() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addUpdate(obj);
        dynamoMapper.save(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        MultiVersionRangeKeyClass objectOnTable = dynamoMapper.load(obj);
        // Verify that item is updated with proper version
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion2());
        assertEquals(obj, objectOnTable);
    }

    @Test
    public void testNonNullValuedMultiVersionAttributeOnDeleteSucceeds() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(obj);
        dynamoMapper.save(obj);
        dynamoMapper.transactionWrite(transactionWriteRequest);
        MultiVersionRangeKeyClass objectOnTable = dynamoMapper.load(obj);
        assertEquals(null, objectOnTable);
    }

    @Test
    public void testOneInvalidValuedMultiVersionAttributeOnPutFails() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(obj);
        dynamoMapper.save(obj);
        obj.setVersion(0L);

        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    @Test
    public void testOneInvalidValuedMultiVersionAttributeOnUpdateFails() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addUpdate(obj);
        dynamoMapper.save(obj);
        obj.setVersion(2L);
        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    @Test
    public void testOneInvalidValuedMultiVersionAttributeOnDeleteFails() {
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(obj);
        dynamoMapper.save(obj);
        obj.setVersion(0L);
        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    @Test
    public void testNonNullValuedMultiVersionAttributesOnPutUpdateAndDelete() {
        TestObjectCreator.TransactionWriteTestRequest transactionWriteTestRequest = new TestObjectCreator.TransactionWriteTestRequest()
                                                                                            .withPutConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withUpdateConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withDeleteConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE)
                                                                                            .withConditionCheckConditionExpressionMode(TestObjectCreator.ConditionExpressionMode.MULTIPLE);
        TestObjectCreator.TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        TransactionWriteRequest transactionWriteRequest = transactionWriteTestData.getTransactionWriteRequest();
        MultiVersionRangeKeyClass putObj = getMultiVersionRangeKeyObject();
        transactionWriteRequest.addPut(putObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, putObj, putObj, true /* saveObjectToTable*/);
        MultiVersionRangeKeyClass updateObj = getMultiVersionRangeKeyObject();
        transactionWriteRequest.addUpdate(updateObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, updateObj, updateObj, true /* saveObjectToTable*/);
        MultiVersionRangeKeyClass deleteObj = getMultiVersionRangeKeyObject();
        transactionWriteRequest.addDelete(deleteObj);
        addVersionAttributeObjectExpectations(transactionWriteTestData, deleteObj, null, true /* saveObjectToTable*/);
        executeAndValidateTransactionWrite(dynamoMapper,
                                           transactionWriteRequest,
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
        MultiVersionRangeKeyClass putObjectOnTable = dynamoMapper.load(putObj);
        assertEquals(putObj, putObjectOnTable);
        MultiVersionRangeKeyClass updateObjectOnTable = dynamoMapper.load(updateObj);
        assertEquals(updateObj, updateObjectOnTable);
        MultiVersionRangeKeyClass objectOnTable = dynamoMapper.load(putObj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion2());
        objectOnTable = dynamoMapper.load(updateObj);
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion());
        assertEquals(Long.valueOf(2L), objectOnTable.getVersion2());
    }

    private void addVersionAttributeObjectExpectations(TestObjectCreator.TransactionWriteTestData transactionWriteTestData,
                                                       Object obj,
                                                       Object expectedObj,
                                                       boolean saveObjectToTable) {
        transactionWriteTestData.getExpectedObjectKeys().add(obj);
        transactionWriteTestData.getExpectedObjects().add(expectedObj);
        if (saveObjectToTable) {
            dynamoMapper.save(obj);
        }
    }

}
