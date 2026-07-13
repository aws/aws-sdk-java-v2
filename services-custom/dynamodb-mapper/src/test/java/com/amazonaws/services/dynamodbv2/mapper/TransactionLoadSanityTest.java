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

import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createMultipleObjectTestDataForTransactionLoad;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.Existence;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getStringAttributeClass;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionLoadTestData;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionLoadTestDataRequest;

import com.amazonaws.services.dynamodbv2.datamodeling.TransactionLoadRequest;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransactionLoadSanityTest extends TransactionsTestBase {

    @Test
    public void testWithSingleExistingItem() {
        StringAttributeClass obj = getStringAttributeClass();
        dynamoMapper.save(obj);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);

        executeAndValidateTransactionLoad(transactionLoadRequest, Arrays.asList((Object) obj));
    }

    @Test
    public void testWithSingleNonExistingItem() {
        StringAttributeClass obj = getStringAttributeClass();
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);
        List<Object> expectedObjects = new ArrayList<Object>();
        expectedObjects.add(null);

        executeAndValidateTransactionLoad(transactionLoadRequest, expectedObjects);
    }

    @Test
    public void testWithMultipleAllExistingItemsFromSameTable() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(5)
                                                                                             .withExistence(Existence.ALL)
                                                                                             .withUseSameObjectType(true);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleSomeExistingItemsFromSameTable() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(5)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(true);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleNoneExistingItemsFromSameTable() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(5)
                                                                                             .withExistence(Existence.NONE)
                                                                                             .withUseSameObjectType(true);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }


    @Test
    public void testWithMultipleAllExistingItemsFromDifferentTables() {
        // Setting numObjects = 10 helps test with maximum items per transaction supported by low level SDK API
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(10)
                                                                                             .withExistence(Existence.ALL)
                                                                                             .withUseSameObjectType(false);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleSomeExistingItemsFromDifferentTables() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.SOME)
                                                                                             .withUseSameObjectType(false);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }

    @Test
    public void testWithMultipleNoneExistingItemsFromDifferentTables() {
        TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(6)
                                                                                             .withExistence(Existence.NONE)
                                                                                             .withUseSameObjectType(false);
        TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(dynamoMapper, testDataRequest);

        executeAndValidateTransactionLoad(testData.getTransactionLoadRequest(), testData.getExpectedObjects());
    }
}
