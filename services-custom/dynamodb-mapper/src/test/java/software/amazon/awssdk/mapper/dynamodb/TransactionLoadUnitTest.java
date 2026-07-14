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

import static com.amazonaws.services.dynamodbv2.TestObjectCreator.DEFAULT_PROVISIONED_THROUGHPUT;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createSdkMapperSaveConfigTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createSdkRangeTestTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createStringHashKeyTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createTestTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getMultiTableObjects;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getSchemaViolatingTestItemObject;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getStringAttributeClass;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getUniqueRangeKeyObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.mapper.NoSuchTableClass;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import com.amazonaws.services.dynamodbv2.pojos.RangeKeyClass;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;
import com.amazonaws.services.dynamodbv2.pojos.TestItem;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransactionLoadUnitTest extends TransactionsUnitTestBase {

    @BeforeClass
    public static void setUpTests() throws InterruptedException {
        setUp();
        dynamoMapper = new DynamoDBMapper(dynamoDB);
        createSdkMapperSaveConfigTable(dynamoDB);
        createSdkRangeTestTable(dynamoDB);
        createStringHashKeyTable(dynamoDB);
    }

    @Test
    public void testTransactionLoadCanLoadSingleItems() {
        StringAttributeClass obj = getStringAttributeClass();
        dynamoMapper.save(obj);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);
        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
        assertEquals(1, responseObjectList.size());
        assertEquals(obj, responseObjectList.get(0));
    }

    @Test
    public void testTranasctionLoadForNonExistingItemsReturnsNull() {
        StringAttributeClass obj = getStringAttributeClass();
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);
        StringAttributeClass obj2 = getStringAttributeClass();
        transactionLoadRequest.addLoad(obj2);
        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
        assertEquals(2, responseObjectList.size());
        for (Object respObj : responseObjectList) {
            assertEquals(null, respObj);
        }
    }

    @Test
    public void testTransactionLoadForOverriddenConfigReturnsOverriddenTableValues() throws InterruptedException {
        String overriddenTableName = "overriddenTableName";
        String stringInOverriddenTable = "StringAttributeOnOverriddenTable";
        createTestTable(dynamoDB, DEFAULT_PROVISIONED_THROUGHPUT,
                        overriddenTableName,
                        "key" /* hashKeyName */,
                        ScalarAttributeType.S,
                        null /* rangeKeyName */,
                        null /* rangeKeyAttributeType */);
        StringAttributeClass obj = getStringAttributeClass();
        DynamoDBMapperConfig.TableNameOverride tableNameOverride = new DynamoDBMapperConfig.TableNameOverride(overriddenTableName);

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                                                          .withTableNameOverride(tableNameOverride)
                                                          .build();
        dynamoMapper.save(obj);
        obj.setStringAttribute(stringInOverriddenTable);
        // Saving a value with overridden tableName
        dynamoMapper.save(obj, config);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);
        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, config);
        assertEquals(1, responseObjectList.size());
        StringAttributeClass stringAttributeClassObject = (StringAttributeClass) responseObjectList.get(0);
        assertEquals(stringInOverriddenTable, stringAttributeClassObject.getStringAttribute());
    }

    @Test
    public void testTransactionLoadOnMultipleTablesSucceeds() {

        final int numItems = 10;
        List<Object> objs = getMultiTableObjects(numItems);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        for (Object obj : objs) {
            dynamoMapper.save(obj);
            transactionLoadRequest.addLoad(obj);
        }
        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);

        for (int i = 0; i < numItems; i++) {
            // Ensure that request and response objects are in the same order as this is a transactionLoad request
            assertEquals(objs.get(i), responseObjectList.get(i));
        }
    }

    @Test
    public void testTransactionLoadOnMultipleTablesWithProjectionsSucceeds() throws InterruptedException {

        final int numItems = 10;
        List<Object> objs = getMultiTableObjects(numItems);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        // Read only integerAttributes from the item
        Map<String, String> expressionAttributeNames = new HashMap<String, String>();
        expressionAttributeNames.put("#sa", "stringAttribute");
        String projectIntegerAttribute = "#sa";
        DynamoDBTransactionLoadExpression dynamoDBTransactionLoadExpression = new DynamoDBTransactionLoadExpression()
                                                                                      .withProjectionExpression(projectIntegerAttribute)
                                                                                      .withExpressionAttributeNames(expressionAttributeNames);
        for (int i = 0; i < numItems; i++) {
            dynamoMapper.save(objs.get(i));
            if (i % 2 == 0) {
                transactionLoadRequest.addLoad(objs.get(i), dynamoDBTransactionLoadExpression);
            } else {
                transactionLoadRequest.addLoad(objs.get(i));
            }
        }

        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);

        for (int i = 0; i < numItems/2; i++) {
            StringAttributeClass stringAttributeClassObject = (StringAttributeClass) responseObjectList.get(i);
            if (i % 2 == 0) {
                assertEquals(stringAttributeClassObject.getKey(), null);
                assertNotEquals(stringAttributeClassObject.getStringAttribute(), null);
            } else {
                assertEquals(objs.get(i), stringAttributeClassObject);
            }
        }

        for (int i = numItems/2 ; i < numItems; i++) {
            TestItem testItemClassObject = (TestItem) responseObjectList.get(i);
            if (i % 2 == 0) {
                assertEquals(testItemClassObject.getHashKey(), null);
                assertNotEquals(testItemClassObject.getStringAttribute(), null);
            } else {
                assertEquals(objs.get(i), (testItemClassObject));
            }
        }
    }

    @Test
    public void testTransactionLoadOnNonExistingAttributeProjectionTestSucceeds() {
        StringAttributeClass obj = getStringAttributeClass();
        dynamoMapper.save(obj);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        Map<String, String> expressionAttributeNames = new HashMap<String, String>();
        expressionAttributeNames.put("#nsa", "nonStringAttribute");
        String projectIntegerAttribute = "#nsa";
        DynamoDBTransactionLoadExpression dynamoDBTransactionLoadExpression = new DynamoDBTransactionLoadExpression()
                                                                                      .withProjectionExpression(projectIntegerAttribute)
                                                                                      .withExpressionAttributeNames(expressionAttributeNames);
        transactionLoadRequest.addLoad(obj, dynamoDBTransactionLoadExpression);
        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
        assertEquals(1, responseObjectList.size());
        StringAttributeClass stringAttributeClassObject = (StringAttributeClass)responseObjectList.get(0);
        assertEquals(null, stringAttributeClassObject.getKey());
        assertEquals(null, stringAttributeClassObject.getRenamedAttribute());
        assertEquals(null, stringAttributeClassObject.getStringAttribute());
    }

    @Test
    public void testTransactionLoadOnVersionedAttributeSucceeds() {

        final int numItems = 10;
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();

        for ( int i = 0; i < numItems; i++ ) {
            RangeKeyClass obj = getUniqueRangeKeyObject();
            DynamoDBMapperTableModel<RangeKeyClass> model = getTable(obj);
            DynamoDBMapperFieldModel<RangeKeyClass,Object> val = model.field("version");
            assertEquals(true, val.versioned());
            dynamoMapper.save(obj);
            transactionLoadRequest.addLoad(obj);
        }

        List<Object> responseObjectList = dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
        for(int i = 0; i < numItems; i++) {
            assertEquals(transactionLoadRequest.getObjectsToLoad().get(i), responseObjectList.get(i));
        }
    }

    @Test
    public void testTransactionLoadOnNonExistingTableReturnsResourceNotFoundException() {
        final int numItems = 10;
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        for ( int i = 0; i < numItems; i++ ) {
            NoSuchTableClass obj = new NoSuchTableClass();
            obj.setKey("someKey");
            transactionLoadRequest.addLoad(obj);
        }

        try {
            dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
            fail("Expected ResourceNotFoundException but no exception thrown");
        } catch (ResourceNotFoundException e) {
            // Pass
        }
    }

    @Test
    public void testTransactionLoadOnSchemaViolatingAttributeReturnsTransactionCanceledException() {
        StringAttributeClass obj = getStringAttributeClass();
        dynamoMapper.save(obj);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(getSchemaViolatingTestItemObject());
        try {
            dynamoMapper.transactionLoad(transactionLoadRequest, DynamoDBMapperConfig.DEFAULT);
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException tce) {
            // Pass
        }
    }
}