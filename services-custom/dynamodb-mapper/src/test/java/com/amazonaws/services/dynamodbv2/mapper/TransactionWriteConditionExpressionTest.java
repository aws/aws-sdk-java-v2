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
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionWriteTestData;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.TransactionWriteTestRequest;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createMultipleObjectTestDataForTransactionWrite;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CancellationReason;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.amazonaws.services.dynamodbv2.pojos.HashKeyRangeKeyClass;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;
import com.amazonaws.services.dynamodbv2.pojos.TestItem;
import org.junit.Test;

/**
 * All the tests unless specified run on multiple tables with multiple operations
 */
public class TransactionWriteConditionExpressionTest extends TransactionsTestBase {

    @Test
    public void testOnePutWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testOneUpdateWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testOneDeleteWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultiplePutsWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.MULTIPLE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());

    }

    @Test
    public void testMultipleUpdatesWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.MULTIPLE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.NONE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleDeletesWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.NONE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.MULTIPLE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithCondition() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.MULTIPLE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.MULTIPLE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.MULTIPLE)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.MULTIPLE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithOneEachHavingExpressionAttributeNames() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_ONLY)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_ONLY)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_ONLY)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_ONLY);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithMultipleOfEachHavingExpressionAttributeNames() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_ONLY)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_ONLY)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_ONLY)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_ONLY);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithOneOfEachHavingExpressionAttributeNamesValues() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_VALUES)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_VALUES)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_VALUES)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_NAMES_VALUES);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithOneEachHavingExpressionAttributeValues() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_VALUES_ONLY)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_VALUES_ONLY)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_VALUES_ONLY)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.ONE_ATTRIBUTE_VALUES_ONLY);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithMultipleOfEachHavingExpressionAttributeValues() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_VALUES_ONLY)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_VALUES_ONLY)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_VALUES_ONLY)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_VALUES_ONLY);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithOneEachHavingSimpleConditionExpression() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithMultipleOfEachHavingExpressionAttributeNamesAndValues() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_VALUES)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_VALUES)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_VALUES)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.MULTIPLE_ATTRIBUTE_NAMES_VALUES);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                           transactionWriteTestData.getExpectedObjectKeys(),
                                           transactionWriteTestData.getExpectedObjects());
    }

    @Test
    public void testMultipleOperationsWithOneOfEachHavingInvalidExpression() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.ONE_INVALID_RETURN_VALUES_SET)
                                                                          .withIsMultiTable(true);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        try {
            executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                               transactionWriteTestData.getExpectedObjectKeys(),
                                               transactionWriteTestData.getExpectedObjects());
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    @Test
    public void testMultipleOperationsWithMultipleOfEachHavingInvalidExpression() {
        /**
         * TODO: This is currently failing on multi-table mode due to an issue with low level API.
         *       Please flip this to multi-table mode once this issue is resolved: https://tt.amazon.com/0189188611
         */
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.MULTIPLE_INVALID_RETURN_VALUES_SET)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.MULTIPLE_INVALID_RETURN_VALUES_SET)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.MULTIPLE_INVALID_RETURN_VALUES_SET)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.MULTIPLE_INVALID_RETURN_VALUES_SET)
                                                                          .withIsMultiTable(false);

        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        try {
            executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                               transactionWriteTestData.getExpectedObjectKeys(),
                                               transactionWriteTestData.getExpectedObjects());
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException tce) {
            // Expected
        }
    }

    @Test
    public void testMultipleOperationsWithOneOfEachHavingReservedWordExpression() {
        TransactionWriteTestRequest transactionWriteTestRequest = new TransactionWriteTestRequest()
                                                                          .withPutConditionExpressionMode(ConditionExpressionMode.ONE_RESERVED_WORD)
                                                                          .withUpdateConditionExpressionMode(ConditionExpressionMode.ONE_RESERVED_WORD)
                                                                          .withDeleteConditionExpressionMode(ConditionExpressionMode.ONE_RESERVED_WORD)
                                                                          .withConditionCheckConditionExpressionMode(ConditionExpressionMode.ONE_RESERVED_WORD);
        TransactionWriteTestData transactionWriteTestData = createMultipleObjectTestDataForTransactionWrite(dynamoMapper, transactionWriteTestRequest);
        try {
            executeAndValidateTransactionWrite(transactionWriteTestData.getTransactionWriteRequest(),
                                               transactionWriteTestData.getExpectedObjectKeys(),
                                               transactionWriteTestData.getExpectedObjects());
            fail("Expected AmazonServiceException but no exception thrown");
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
        }
    }

    private static Map<String, AttributeValue> generateAttributeItemFromObject(Object obj) {
        Map<String, AttributeValue> expectedResponseValueMap = new HashMap<String, AttributeValue>();
        if (obj.getClass().equals(StringAttributeClass.class)) {
            StringAttributeClass stringAttributeClassObject = (StringAttributeClass) obj;
            expectedResponseValueMap.put("originalName", new AttributeValue(stringAttributeClassObject.getRenamedAttribute()));
            expectedResponseValueMap.put("stringAttribute", new AttributeValue(stringAttributeClassObject.getStringAttribute()));
            expectedResponseValueMap.put("key", new AttributeValue(stringAttributeClassObject.getKey()));
        } else if (obj.getClass().equals(TestItem.class)) {
            TestItem testItemObject = (TestItem) obj;
            expectedResponseValueMap.put("hashKey", new AttributeValue(testItemObject.getHashKey()));
            expectedResponseValueMap.put("rangeKey", new AttributeValue().withN(testItemObject.getRangeKey().toString()));
            expectedResponseValueMap.put("stringAttribute", new AttributeValue(testItemObject.getStringAttribute()));
            expectedResponseValueMap.put("nonKeyAttribute", new AttributeValue(testItemObject.getNonKeyAttribute()));
            expectedResponseValueMap.put("stringSetAttribute", new AttributeValue().withSS(testItemObject.getStringSetAttribute()));
        } else if (obj.getClass().equals(HashKeyRangeKeyClass.class)) {
            HashKeyRangeKeyClass hashKeyRangeKeyClassObject = (HashKeyRangeKeyClass) obj;
            expectedResponseValueMap.put("hashKey", new AttributeValue().withN(Long.toString(hashKeyRangeKeyClassObject.getHashKey())));
            expectedResponseValueMap.put("rangeKey", new AttributeValue().withN(Double.toString(hashKeyRangeKeyClassObject.getRangeKey())));
            expectedResponseValueMap.put("stringAttribute", new AttributeValue(hashKeyRangeKeyClassObject.getStringAttribute()));
        } else {
            throw new IllegalArgumentException("Unsupported class passed in: " + obj.getClass());
        }
        return expectedResponseValueMap;
    }

}