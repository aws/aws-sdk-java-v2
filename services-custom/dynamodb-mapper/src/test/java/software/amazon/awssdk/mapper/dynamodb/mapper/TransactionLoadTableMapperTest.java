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
package software.amazon.awssdk.mapper.dynamodb.mapper;

import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.TableNameOverride;
import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.TableNameResolver;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.createMultipleObjectTestDataForTransactionLoad;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.createSdkRangeTestTable;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.deleteTestTable;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.Existence;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.TransactionLoadTestData;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.TransactionLoadTestDataRequest;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getStringAttributeClass;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getUniqueRangeKeyObject;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getSchemaViolatingTestItemObject;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getTestItemObject;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.ObjectSupplier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMappingException;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTableMapper;
import software.amazon.awssdk.mapper.dynamodb.TransactionLoadRequest;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import software.amazon.awssdk.mapper.dynamodb.pojos.RangeKeyClass;
import software.amazon.awssdk.mapper.dynamodb.pojos.SchemaViolatingTestItem;
import software.amazon.awssdk.mapper.dynamodb.pojos.StringAttributeClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransactionLoadTableMapperTest extends TransactionsTestBase {
    private static final String rangeKeySubClass1TableName = "sdk-range-d696fab0-566d-426d-b788-617f7397a9eb";
    private static final String rangeKeySubClass2TableName = "sdk-range-3789cdd0-d257-49e4-a7c2-84b4baad7a5a";

    private static DynamoDBTableMapper<RangeKeyClass, String, Double> tableMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        TransactionsTestBase.setUp();
        tableMapper = dynamoMapper.newTableMapper(RangeKeyClass.class);
    }

    @Test
    public void testWithSingleExistingItem() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        tableMapper.save(obj);
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);

        executeAndValidateTransactionLoad(transactionLoadRequest, Arrays.asList((Object) obj));
    }

    @Test
    public void testWithSingleNonExistingItem() {
        RangeKeyClass obj = getUniqueRangeKeyObject();
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(obj);
        List<Object> expectedObjects = new ArrayList<Object>();
        expectedObjects.add(null);

        executeAndValidateTransactionLoad(transactionLoadRequest, expectedObjects);
    }

    @Test
    public void testWithSingleInvalidObjectTypeItem() {
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        transactionLoadRequest.addLoad(getStringAttributeClass());
        try {
            tableMapper.transactionLoad(transactionLoadRequest);
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException ex) {
            String expectedMessage = "Input object is of the classType: " + StringAttributeClass.class + " but tableMapper is declared with classType: "
                                        + RangeKeyClass.class;
            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Test
    public void testWithMultipleSomeExistingItemsFromDifferentTables() throws InterruptedException {
        final int numObjects = 10;
        TableNameResolver tableNameResolver = new TableNameResolver() {
            private int counter;
            private boolean stillSaving = true;

            @Override
            public String getTableName(Class<?> clazz, DynamoDBMapperConfig config) {
                if (!RangeKeyClass.class.equals(clazz)) {
                    throw new IllegalArgumentException("Input object must be of type: " + RangeKeyClass.class);
                }
                String tableName;
                if (counter % 3 == 0) {
                    tableName = sdkRangeTableName;
                } else if (counter % 3 == 1) {
                    tableName = rangeKeySubClass1TableName;
                } else {
                    tableName = rangeKeySubClass2TableName;
                }
                counter++;

                // This method gets called in two flows:
                //  1. From DynamoDBMapper.save for every alternate object (because we only save half of the total objects we add as input to transactionLoad)
                //  2. From DynamoDBMapper.transactionLoad for every object (because we add every object as input to transactionLoad)
                // So, to keep resulting table name same across save/transactionLoad for same object, we increment counter twice per call to this method while
                // saving objects.
                if (stillSaving) {
                    counter++;
                }
                // This means all items have been saved, so we reset counter so that names match for same object while doing transactionLoad
                if (counter >= numObjects - 1) {
                    counter = 0;
                    stillSaving = false;
                }

                return tableName;
            }
        };
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder().withTableNameResolver(tableNameResolver).build();
        DynamoDBMapper mapperWithTableNameResolver = new DynamoDBMapper(dynamo, config);
        DynamoDBTableMapper<RangeKeyClass, String, Double> tableMapperWithTableNameResolver = mapperWithTableNameResolver.newTableMapper(RangeKeyClass.class);

        ObjectSupplier objectSupplier = new ObjectSupplier() {
                @Override
                public Object get() {
                    return getUniqueRangeKeyObject();
                }
            };

        try {
            createSdkRangeTestTable(dynamo, rangeKeySubClass1TableName);
            createSdkRangeTestTable(dynamo, rangeKeySubClass2TableName);
            TransactionLoadTestDataRequest testDataRequest = new TransactionLoadTestDataRequest().withNumObjects(numObjects)
                                                                                                 .withExistence(Existence.SOME)
                                                                                                 .withUseSameObjectType(true)
                                                                                                 .withObjectSupplier(objectSupplier);
            TransactionLoadTestData testData = createMultipleObjectTestDataForTransactionLoad(mapperWithTableNameResolver, testDataRequest);

            executeAndValidateTransactionLoad(tableMapperWithTableNameResolver, testData.getTransactionLoadRequest(), testData.getExpectedObjects());
        } finally {
            deleteTestTable(dynamo, rangeKeySubClass1TableName);
            deleteTestTable(dynamo, rangeKeySubClass2TableName);
        }
    }

    @Test
    public void testWithMultipleSomeInvalidObjectTypeItems() {
        try {
            TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
            for (int i = 0; i < 3; i++) {
                transactionLoadRequest.addLoad(getUniqueRangeKeyObject());
            }
            transactionLoadRequest.addLoad(getStringAttributeClass());
            transactionLoadRequest.addLoad(getUniqueRangeKeyObject());
            transactionLoadRequest.addLoad(getTestItemObject());

            executeAndValidateTransactionLoad(transactionLoadRequest, Collections.emptyList());
            fail("Expected DynamoDBMappingException but no exception thrown");
        } catch (DynamoDBMappingException ex) {
            String expectedMessage = "Input object is of the classType: " + StringAttributeClass.class + " but tableMapper is declared with classType: "
                                        + RangeKeyClass.class;
            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Test
    public void testWithMultipleItemsWhenSomeItemsCauseTransactionCancelled() {
        int numObjects = 3;
        TransactionLoadRequest transactionLoadRequest = new TransactionLoadRequest();
        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                                                          .withTableNameOverride(TableNameOverride.withTableNameReplacement(mapperSaveConfigTableName))
                                                          .build();
        DynamoDBMapper mapperWithTableNameOverride = new DynamoDBMapper(dynamo, config);
        DynamoDBTableMapper<SchemaViolatingTestItem, String, Double> tableMapperWithTableNameOverride =
                mapperWithTableNameOverride.newTableMapper(SchemaViolatingTestItem.class);
        for (int i = 0; i < numObjects; i++) {
            transactionLoadRequest.addLoad(getSchemaViolatingTestItemObject());
        }

        try {
            executeAndValidateTransactionLoad(tableMapperWithTableNameOverride, transactionLoadRequest, Collections.emptyList());
            fail("Expected TransactionCanceledException but no exception thrown");
        } catch (TransactionCanceledException ex) {
            // Expected
        }
    }

    @Override
    protected void executeAndValidateTransactionLoad(TransactionLoadRequest transactionLoadRequest, List<Object> expectedObjects) {
        executeAndValidateTransactionLoad(tableMapper, transactionLoadRequest, expectedObjects);
    }

    private void executeAndValidateTransactionLoad(DynamoDBTableMapper tableMapper,
                                                   TransactionLoadRequest transactionLoadRequest,
                                                   List<Object> expectedObjects) {
        List<Object> responseObjects = tableMapper.transactionLoad(transactionLoadRequest);
        int numItems = expectedObjects.size();
        assertEquals(numItems, responseObjects.size());
        for (int i = 0; i < numItems; i++) {
            assertEquals(expectedObjects.get(i), responseObjects.get(i));
        }
    }
}
