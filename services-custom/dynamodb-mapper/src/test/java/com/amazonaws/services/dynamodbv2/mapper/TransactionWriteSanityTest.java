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

import static com.amazonaws.services.dynamodbv2.TestObjectCreator.ConditionExpressionMode;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionWriteTestRequest;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionWriteTestData;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createMultipleObjectTestDataForTransactionWrite;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.generateStringMatcherTransactWriteExpression;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getStringAttributeClass;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getTestItemObject;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getHashKeyRangeKeyObject;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTransactionWriteExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.amazonaws.services.dynamodbv2.pojos.HashKeyRangeKeyClass;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;
import com.amazonaws.services.dynamodbv2.pojos.TestItem;
import org.junit.Test;

/**
 * Sanity integration tests for TransactionWrite operations.
 */
public class TransactionWriteSanityTest extends TransactionsTestBase {

    @Test
    public void testAllOperationsWithSingleItem() {
        StringAttributeClass obj = getStringAttributeClass();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(obj);

        // Put one object
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), Arrays.asList((Object) obj));

        // Update the object
        String updatedStringAttribute = "updatedStringAttribute";
        transactionWriteRequest = new TransactionWriteRequest();
        obj.setStringAttribute(updatedStringAttribute);
        transactionWriteRequest.addUpdate(obj);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), Arrays.asList((Object) obj));

        // ConditionCheck the object
        transactionWriteRequest = new TransactionWriteRequest();
        DynamoDBTransactionWriteExpression writeExpression = generateStringMatcherTransactWriteExpression(updatedStringAttribute,
                                                                                                          "not" + updatedStringAttribute);
        transactionWriteRequest.addConditionCheck(obj, writeExpression);
        //   Succeeds
        dynamoMapper.transactionWrite(transactionWriteRequest);

        // Delete
        transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addDelete(obj);
        List<Object> expectedObjects = new ArrayList<Object>();
        expectedObjects.add(null);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), expectedObjects);
    }

    @Test
    public void testAllOperationsWithSingleItemAndUnusualExistence() {
        StringAttributeClass obj = getStringAttributeClass();
        StringAttributeClass objToPut = getStringAttributeClass();
        // To put on existing object
        objToPut.setKey(obj.getKey());
        saveObject(obj);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(objToPut);

        // Put one object on existing object
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) obj), Arrays.asList((Object) objToPut));

        // Update non existing object
        transactionWriteRequest = new TransactionWriteRequest();
        StringAttributeClass objToUpdate = getStringAttributeClass();
        transactionWriteRequest.addUpdate(objToUpdate);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) objToUpdate), Arrays.asList((Object) objToUpdate));

        // ConditionCheck non existing object
        transactionWriteRequest = new TransactionWriteRequest();
        StringAttributeClass objToConditionCheck = getStringAttributeClass();
        DynamoDBTransactionWriteExpression writeExpression = generateStringMatcherTransactWriteExpression("someString",
                                                                                                          "someOtherString");
        transactionWriteRequest.addConditionCheck(objToConditionCheck, writeExpression);
        try {
            dynamoMapper.transactionWrite(transactionWriteRequest);
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException tce) {
            // Expected
        }

        // Delete non existing object
        transactionWriteRequest = new TransactionWriteRequest();
        StringAttributeClass objToDelete = getStringAttributeClass();
        transactionWriteRequest.addDelete(objToDelete);
        List<Object> expectedObjects = new ArrayList<Object>();
        expectedObjects.add(null);
        executeAndValidateTransactionWrite(transactionWriteRequest, Arrays.asList((Object) objToDelete), expectedObjects);
    }

    @Test
    public void testSingleMixedOperationsOnSingleTable() {

        StringAttributeClass objectToBeUpdated = getStringAttributeClass();
        StringAttributeClass objectToBeConditionChecked = getStringAttributeClass();
        StringAttributeClass objectToBeDeleted = getStringAttributeClass();

        // Save it so that we can update it inside transaction
        saveObject(objectToBeUpdated);
        // Save it so that we can conditionCheck it inside transaction
        saveObject(objectToBeConditionChecked);
        // Save it so that we can delete it inside transaction
        saveObject(objectToBeDeleted);

        // Save the object
        StringAttributeClass objectToBePut = getStringAttributeClass();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(objectToBePut);
        // Update the object
        String updatedStringAttribute = "updatedStringAttribute";
        objectToBeUpdated.setStringAttribute(updatedStringAttribute);
        transactionWriteRequest.addUpdate(objectToBeUpdated);
        // ConditionCheck the object
        DynamoDBTransactionWriteExpression conditionCheckWriteExpression =
                generateStringMatcherTransactWriteExpression(objectToBeConditionChecked.getStringAttribute(),
                                                             "not" + updatedStringAttribute);
        transactionWriteRequest.addConditionCheck(objectToBeConditionChecked, conditionCheckWriteExpression);
        // Delete the object
        transactionWriteRequest.addDelete(objectToBeDeleted);
        List<Object> responseObjectKeys = Arrays.<Object>asList(objectToBePut,
                                                               objectToBeUpdated,
                                                               objectToBeDeleted);
        List<Object> responseObjects = Arrays.<Object>asList(objectToBePut,
                                                             objectToBeUpdated,
                                                             null);
        executeAndValidateTransactionWrite(transactionWriteRequest, responseObjectKeys, responseObjects);
    }

    @Test
    public void testSingleMixedOperationsOnMultipleTables() {
        StringAttributeClass objectToBeUpdated = getStringAttributeClass();
        TestItem objectToBeConditionChecked = getTestItemObject();
        HashKeyRangeKeyClass objectToBeDeleted = getHashKeyRangeKeyObject();

        // Save it so that we can update it inside transaction
        saveObject(objectToBeUpdated);
        // Save it so that we can conditionCheck it inside transaction
        saveObject(objectToBeConditionChecked);
        // Save it so that we can delete it inside transaction
        saveObject(objectToBeDeleted);

        // Save the object
        StringAttributeClass objectToBePut = getStringAttributeClass();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(objectToBePut);
        // Update the object
        String updatedStringAttribute = "updatedStringAttribute";
        objectToBeUpdated.setStringAttribute(updatedStringAttribute);
        transactionWriteRequest.addUpdate(objectToBeUpdated);
        // ConditionCheck the object
        DynamoDBTransactionWriteExpression conditionCheckWriteExpression =
                generateStringMatcherTransactWriteExpression(objectToBeConditionChecked.getStringAttribute(),
                                                             "not" + updatedStringAttribute);
        transactionWriteRequest.addConditionCheck(objectToBeConditionChecked, conditionCheckWriteExpression);

        // Delete the object
        transactionWriteRequest.addDelete(objectToBeDeleted);
        List<Object> responseObjectKeys = Arrays.<Object>asList(objectToBePut,
                                                                objectToBeUpdated,
                                                                objectToBeDeleted);
        List<Object> responseObjects = Arrays.<Object>asList(objectToBePut,
                                                             objectToBeUpdated,
                                                             null);
        executeAndValidateTransactionWrite(transactionWriteRequest, responseObjectKeys, responseObjects);
    }

    @Test
    public void testMultipleMixedOperationsOnSingleTable() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                           .withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                           .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                           .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                           .withIsMultiTable(false);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleMixedOperationsOnMultipleTables() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                           .withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                           .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                           .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                           .withIsMultiTable(true);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }
}
