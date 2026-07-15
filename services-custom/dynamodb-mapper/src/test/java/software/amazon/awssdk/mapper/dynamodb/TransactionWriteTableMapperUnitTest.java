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

import static com.amazonaws.services.dynamodbv2.TestObjectCreator.createStringHashKeyTable;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getStringAttributeClass;
import static com.amazonaws.services.dynamodbv2.TestObjectCreator.getTestItemObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.pojos.StringAttributeClass;
import com.amazonaws.services.dynamodbv2.pojos.TestItem;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransactionWriteTableMapperUnitTest extends TransactionsUnitTestBase {

    @BeforeClass
    public static void setUpTests() throws InterruptedException {
        setUp();
        dynamoMapper = new DynamoDBMapper(dynamoDB);
        createStringHashKeyTable(dynamoDB);
    }

    @Test
    public void testSimpleTableMapperTransactionWriteSucceeds() {
        final DynamoDBTableMapper<StringAttributeClass, String, Double> tableMapper = dynamoMapper.newTableMapper(StringAttributeClass.class);
        final int numItems = 10;
        List<Object> requestObjectList = new ArrayList<Object>();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        for ( int i = 0; i < numItems; i++ ) {
            StringAttributeClass obj = getStringAttributeClass();
            requestObjectList.add(obj);
            transactionWriteRequest.addPut(obj);
            transactionLoadRequest.addLoad(obj);

        }
        tableMapper.transactionWrite(transactionWriteRequest);
        List<Object> responseObjectList = transactionLoadObjects(transactionLoadRequest);
        for(int i = 0; i < numItems; i++) {
            assertEquals(requestObjectList.get(i), responseObjectList.get(i));
        }
    }

    @Test
    public void testMultipleItemTableMapperTransactionWriteSucceeds() {
        final DynamoDBTableMapper<StringAttributeClass, String, Double> tableMapper = dynamoMapper.newTableMapper(StringAttributeClass.class);
        StringAttributeClass objectToBeUpdated = getStringAttributeClass();
        StringAttributeClass objectToBeConditionChecked = getStringAttributeClass();
        StringAttributeClass objectToBeDeleted = getStringAttributeClass();

        // Save it so that we can update it inside transaction
        tableMapper.save(objectToBeUpdated);
        // Save it so that we can conditionCheck it inside transaction
        tableMapper.save(objectToBeConditionChecked);
        // Save it so that we can delete it inside transaction
        tableMapper.save(objectToBeDeleted);

        // Save the object
        StringAttributeClass objectToBePut = getStringAttributeClass();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(objectToBePut);
        // Update the object
        String updatedStringAttribute = "updatedStringAttribute";
        objectToBeUpdated.setStringAttribute(updatedStringAttribute);
        transactionWriteRequest.addUpdate(objectToBeUpdated);
        // ConditionCheck the object
        DynamoDBTransactionWriteExpression conditionCheckWriteExpression = new DynamoDBTransactionWriteExpression();
        conditionCheckWriteExpression.withConditionExpression("(#sA IN (:attr1, :attr2))");
        Map<String, String> attributeNameMap = new HashMap<String, String>();
        attributeNameMap.put("#sA", "stringAttribute");
        Map<String, AttributeValue> attributeValueMap = new HashMap<String, AttributeValue>();
        attributeValueMap.put(":attr1", new AttributeValue(objectToBeConditionChecked.getStringAttribute()));
        attributeValueMap.put(":attr2", new AttributeValue("not" + updatedStringAttribute));
        conditionCheckWriteExpression.withExpressionAttributeValues(attributeValueMap);
        conditionCheckWriteExpression.withExpressionAttributeNames(attributeNameMap);
        transactionWriteRequest.addConditionCheck(objectToBeConditionChecked, conditionCheckWriteExpression);
        // Delete the object
        StringAttributeClass objectToBeDeletedKey = new StringAttributeClass();
        objectToBeDeletedKey.setKey(objectToBeDeleted.getKey());

        transactionWriteRequest.addDelete(objectToBeDeletedKey);

        // Ensure transactWrite succeeds implying conditionCheck succeeds
        tableMapper.transactionWrite(transactionWriteRequest);

        // Ensure toPut object is put
        assertEquals(objectToBePut, dynamoMapper.load(objectToBePut));
        // Ensure toUpdate object is updated
        assertEquals(updatedStringAttribute, dynamoMapper.load(objectToBeUpdated).getStringAttribute());
        // Ensure toDelete object is deleted
        assertEquals(null, dynamoMapper.load(objectToBeDeleted));
    }

    @Test
    public void testWrongInputObjectForTableMapperTransactionWriteFails() {
        final DynamoDBTableMapper<StringAttributeClass, String, Double> tableMapper = dynamoMapper.newTableMapper(StringAttributeClass.class);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(getTestItemObject());
        try {
            tableMapper.transactionWrite(transactionWriteRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException dme) {
            assertEquals(dme.getMessage(), "Input object is of the classType: " + TestItem.class + " but tableMapper is declared with classType: " + StringAttributeClass.class);
        }
    }

    @Test
    public void testMixedInputsObjectForTableMapperTransactionLoadFails() {
        final DynamoDBTableMapper<StringAttributeClass, String, Double> tableMapper = dynamoMapper.newTableMapper(StringAttributeClass.class);
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        transactionWriteRequest.addPut(getStringAttributeClass());
        transactionWriteRequest.addDelete(getTestItemObject());
        try {
            tableMapper.transactionWrite(transactionWriteRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException dme) {
            assertEquals(dme.getMessage(), "Input object is of the classType: " + TestItem.class + " but tableMapper is declared with classType: " + StringAttributeClass.class);
        }
    }
}
