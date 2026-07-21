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
package software.amazon.awssdk.mapper.dynamodb;

import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.createSdkRangeTestTable;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getStringAttributeClass;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getUniqueRangeKeyObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import software.amazon.awssdk.mapper.dynamodb.pojos.RangeKeyClass;
import software.amazon.awssdk.mapper.dynamodb.pojos.StringAttributeClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransactionLoadTableMapperUnitTest extends TransactionsUnitTestBase {

    @BeforeClass
    public static void setUpTests() throws InterruptedException {
        setUp();
        dynamoMapper = new DynamoDBMapper(dynamoDB);
        createSdkRangeTestTable(dynamoDB);
    }

    @Test
    public void testSimpleTableMapperTransactionLoadSucceeds() {
        final DynamoDBTableMapper<RangeKeyClass, String, Double> tableMapper = dynamoMapper.newTableMapper(RangeKeyClass.class);
        final int numItems = 10;
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();

        for ( int i = 0; i < numItems; i++ ) {
            RangeKeyClass obj = getUniqueRangeKeyObject();
            tableMapper.save(obj);
            transactionLoadRequest.addLoad(obj);
        }

        List<Object> responseObjectList = tableMapper.transactionLoad(transactionLoadRequest);
        List<Object> requestObjectList = transactionLoadRequest.getObjectsToLoad();
        for(int i = 0; i < numItems; i++) {
            assertEquals(requestObjectList.get(i), responseObjectList.get(i));
        }
    }

    @Test
    public void testWrongInputObjectForTableMapperTransactionLoadFails() {
        final DynamoDBTableMapper<RangeKeyClass, String, Double> tableMapper = dynamoMapper.newTableMapper(RangeKeyClass.class);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(getStringAttributeClass());
        try {
            tableMapper.transactionLoad(transactionLoadRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException dme) {
            assertEquals(dme.getMessage(), "Input object is of the classType: " + StringAttributeClass.class + " but tableMapper is declared with classType: " + RangeKeyClass.class);
        }
    }

    @Test
    public void testMixedInputsObjectForTableMapperTransactionLoadFails() {
        final DynamoDBTableMapper<RangeKeyClass, String, Double> tableMapper = dynamoMapper.newTableMapper(RangeKeyClass.class);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        transactionLoadRequest.addLoad(obj);
        transactionLoadRequest.addLoad(getStringAttributeClass());
        try {
            tableMapper.transactionLoad(transactionLoadRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException dme) {
            assertEquals(dme.getMessage(), "Input object is of the classType: " + StringAttributeClass.class + " but tableMapper is declared with classType: " + RangeKeyClass.class);
        }
    }
}
