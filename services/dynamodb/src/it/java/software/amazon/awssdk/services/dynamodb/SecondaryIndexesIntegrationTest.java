/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.TempTableWithSecondaryIndexes;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests for LSI & GSI.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = TempTableWithSecondaryIndexes.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
                    })
public class SecondaryIndexesIntegrationTest extends DynamoDBTestBase {

    private static final int MAX_RETRIES = 5;
    private static final int SLEEP_TIME = 20000;
    private static final String tableName = TempTableWithSecondaryIndexes.TEMP_TABLE_NAME;
    private static final String HASH_KEY_NAME = TempTableWithSecondaryIndexes.HASH_KEY_NAME;
    private static final String RANGE_KEY_NAME = TempTableWithSecondaryIndexes.RANGE_KEY_NAME;
    private static final String LSI_NAME = TempTableWithSecondaryIndexes.LSI_NAME;
    private static final String LSI_RANGE_KEY_NAME = TempTableWithSecondaryIndexes.LSI_RANGE_KEY_NAME;
    private static final String GSI_NAME = TempTableWithSecondaryIndexes.GSI_NAME;
    private static final String GSI_HASH_KEY_NAME = TempTableWithSecondaryIndexes.GSI_HASH_KEY_NAME;
    private static final String GSI_RANGE_KEY_NAME = TempTableWithSecondaryIndexes.GSI_RANGE_KEY_NAME;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBTestBase.setUpTestBase();
    }

    /**
     * Assert the tableDescription is as expected
     */
    @Test
    public void testDescribeTempTableWithIndexes() {
        TableDescription tableDescription = dynamo.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table();
        assertEquals(tableName, tableDescription.tableName());
        assertNotNull(tableDescription.tableStatus());
        assertEquals(2, tableDescription.keySchema().size());
        assertEquals(HASH_KEY_NAME,
                     tableDescription.keySchema().get(0)
                                     .attributeName());
        assertEquals(KeyType.HASH, tableDescription
                .keySchema().get(0).keyType());
        assertEquals(RANGE_KEY_NAME, tableDescription.keySchema()
                                                     .get(1).attributeName());
        assertEquals(KeyType.RANGE, tableDescription
                .keySchema().get(1).keyType());

        assertEquals(1, tableDescription.localSecondaryIndexes().size());
        assertEquals(LSI_NAME, tableDescription
                .localSecondaryIndexes().get(0).indexName());
        assertEquals(2, tableDescription
                .localSecondaryIndexes().get(0).keySchema().size());
        assertEquals(HASH_KEY_NAME, tableDescription
                .localSecondaryIndexes().get(0).keySchema().get(0).attributeName());
        assertEquals(KeyType.HASH, tableDescription
                .localSecondaryIndexes().get(0).keySchema().get(0).keyType());
        assertEquals(LSI_RANGE_KEY_NAME, tableDescription
                .localSecondaryIndexes().get(0).keySchema().get(1).attributeName());
        assertEquals(KeyType.RANGE, tableDescription
                .localSecondaryIndexes().get(0).keySchema().get(1).keyType());
        assertEquals(ProjectionType.KEYS_ONLY,
                     tableDescription.localSecondaryIndexes().get(0)
                                     .projection().projectionType());
        assertTrue(tableDescription.localSecondaryIndexes().get(0)
                                           .projection().nonKeyAttributes() instanceof SdkAutoConstructList);

        assertEquals(1, tableDescription.globalSecondaryIndexes().size());
        assertEquals(GSI_NAME, tableDescription
                .globalSecondaryIndexes().get(0).indexName());
        assertEquals(2, tableDescription
                .globalSecondaryIndexes().get(0).keySchema().size());
        assertEquals(GSI_HASH_KEY_NAME, tableDescription
                .globalSecondaryIndexes().get(0).keySchema().get(0).attributeName());
        assertEquals(KeyType.HASH, tableDescription
                .globalSecondaryIndexes().get(0).keySchema().get(0).keyType());
        assertEquals(GSI_RANGE_KEY_NAME, tableDescription
                .globalSecondaryIndexes().get(0).keySchema().get(1).attributeName());
        assertEquals(KeyType.RANGE, tableDescription
                .globalSecondaryIndexes().get(0).keySchema().get(1).keyType());
        assertEquals(ProjectionType.KEYS_ONLY,
                     tableDescription.globalSecondaryIndexes().get(0)
                                     .projection().projectionType());
        assertTrue(tableDescription.globalSecondaryIndexes().get(0)
                                           .projection().nonKeyAttributes() instanceof SdkAutoConstructList);

    }

    /**
     * Tests making queries with global secondary index.
     */
    @Test
    public void testQueryWithGlobalSecondaryIndex() throws InterruptedException {
        // GSI attributes don't have to be unique
        // so items with the same GSI keys but different primary keys
        // could co-exist in the table.
        int totalDuplicateGSIKeys = 10;
        Random random = new Random();
        String duplicateGSIHashValue = UUID.randomUUID().toString();
        int duplicateGSIRangeValue = random.nextInt();
        for (int i = 0; i < totalDuplicateGSIKeys; i++) {
            Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
            item.put(HASH_KEY_NAME, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            item.put(RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put(GSI_HASH_KEY_NAME, AttributeValue.builder().s(duplicateGSIHashValue).build());
            item.put(GSI_RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(duplicateGSIRangeValue)).build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
        }

        // Query the duplicate GSI key values should return all the items
        Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put(
                GSI_HASH_KEY_NAME,
                Condition.builder().attributeValueList(
                        AttributeValue.builder().s((duplicateGSIHashValue)).build())
                               .comparisonOperator(ComparisonOperator.EQ).build());
        keyConditions.put(
                GSI_RANGE_KEY_NAME,
                Condition.builder().attributeValueList(
                        AttributeValue.builder().n(Integer
                                                           .toString(duplicateGSIRangeValue)).build())
                               .comparisonOperator(ComparisonOperator.EQ).build());

        // All the items with the GSI keys should be returned
        assertQueryResponseCount(totalDuplicateGSIKeys, QueryRequest.builder()
                .tableName(tableName)
                .indexName(GSI_NAME)
                .keyConditions(keyConditions).build());

        // Other than this, the behavior of GSI query should be the similar
        // as LSI query. So following code is similar to that used for
        // LSI query test.

        String randomPrimaryHashKeyValue = UUID.randomUUID().toString();
        String randomGSIHashKeyValue = UUID.randomUUID().toString();
        int totalItemsPerHash = 10;
        int totalIndexedItemsPerHash = 5;
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();

        item.put(HASH_KEY_NAME, AttributeValue.builder().s(randomPrimaryHashKeyValue).build());
        item.put(GSI_HASH_KEY_NAME, AttributeValue.builder().s(randomGSIHashKeyValue).build());
        // Items with GSI keys
        for (int i = 0; i < totalIndexedItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put(GSI_RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put("attribute_" + i, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            item.remove("attribute_" + i);
        }
        item.remove(GSI_RANGE_KEY_NAME);
        // Items with incomplete GSI keys (no GSI range key)
        for (int i = totalIndexedItemsPerHash; i < totalItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put("attribute_" + i, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            item.remove("attribute_" + i);
        }

        /**
         *  1) Query-with-GSI (only by GSI hash key)
         */
        QueryResponse result = dynamo.query(QueryRequest.builder()
                                                  .tableName(tableName)
                                                  .indexName(GSI_NAME)
                                                  .keyConditions(
                                                          Collections.singletonMap(
                                                                  GSI_HASH_KEY_NAME,
                                                                  Condition.builder()
                                                                          .attributeValueList(AttributeValue.builder()
                                                                                  .s(randomGSIHashKeyValue).build())
                                                                          .comparisonOperator(
                                                                                         ComparisonOperator.EQ).build())).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // By default, the result includes all the key attributes (2 primary + 2 GSI).
        assertEquals(4, result.items().get(0).size());

        /**
         * 2) Query-with-GSI (by GSI hash + range)
         */
        int rangeKeyConditionRange = 2;
        keyConditions = new HashMap<String, Condition>();
        keyConditions.put(
                GSI_HASH_KEY_NAME,
                Condition.builder()
                        .attributeValueList(AttributeValue.builder()
                                .s(randomGSIHashKeyValue)
                                .build())
                        .comparisonOperator(ComparisonOperator.EQ)
                        .build());
        keyConditions.put(
                GSI_RANGE_KEY_NAME,
                Condition.builder()
                        .attributeValueList(AttributeValue.builder()
                                .n(Integer.toString(rangeKeyConditionRange))
                                .build())
                        .comparisonOperator(ComparisonOperator.LT)
                        .build());
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(GSI_NAME)
                                      .keyConditions(keyConditions)
                                      .build());
        assertEquals((Object) rangeKeyConditionRange, (Object) result.count());

        /**
         * 3) Query-with-GSI does not support Select.ALL_ATTRIBUTES if the index
         * was not created with this projection type.
         */
        try {
            result = dynamo.query(QueryRequest.builder()
                                          .tableName(tableName)
                                          .indexName(GSI_NAME)
                                          .keyConditions(
                                                  Collections.singletonMap(
                                                          GSI_HASH_KEY_NAME,
                                                          Condition.builder()
                                                                  .attributeValueList(AttributeValue.builder()
                                                                          .s(randomGSIHashKeyValue)
                                                                          .build())
                                                                         .comparisonOperator(ComparisonOperator.EQ)
                                                                  .build()))
                                          .select(Select.ALL_ATTRIBUTES).build());
            fail("SdkServiceException is expected");
        } catch (SdkServiceException exception) {
            assertTrue(exception.getMessage().contains("Select type ALL_ATTRIBUTES is not supported for global secondary"));
        }

        /**
         * 4) Query-with-GSI on selected attributes (by AttributesToGet)
         */
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(GSI_NAME)
                                      .keyConditions(
                                              Collections.singletonMap(
                                                      GSI_HASH_KEY_NAME,
                                                      Condition.builder()
                                                              .attributeValueList(AttributeValue.builder()
                                                                      .s(randomGSIHashKeyValue).build())
                                                                     .comparisonOperator(ComparisonOperator.EQ).build()))
                                      .attributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // Two attributes as specified in AttributesToGet
        assertEquals(2, result.items().get(0).size());

        /**
         * 5) Exception when using both Selection and AttributeToGet
         */
        try {
            result = dynamo.query(QueryRequest.builder()
                                          .tableName(tableName)
                                          .indexName(GSI_NAME)
                                          .keyConditions(
                                                  Collections.singletonMap(
                                                          GSI_HASH_KEY_NAME,
                                                          Condition.builder().attributeValueList(
                                                                  AttributeValue.builder()
                                                                          .s(randomGSIHashKeyValue).build())
                                                                         .comparisonOperator(ComparisonOperator.EQ).build()))
                                          .attributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME, LSI_RANGE_KEY_NAME)
                                          .select(Select.ALL_PROJECTED_ATTRIBUTES).build());
            fail("Should trigger exception when using both Select and AttributeToGet.");
        } catch (SdkServiceException exception) {
            // Ignored or expected.
        }

        /**
         * 6) Query-with-GSI on selected attributes (by Select.SPECIFIC_ATTRIBUTES)
         */
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(GSI_NAME)
                                      .keyConditions(
                                              Collections.singletonMap(
                                                      GSI_HASH_KEY_NAME,
                                                      Condition.builder().attributeValueList(
                                                              AttributeValue.builder()
                                                                      .s(randomGSIHashKeyValue).build())
                                                                     .comparisonOperator(
                                                                             ComparisonOperator.EQ).build()))
                                      .attributesToGet(HASH_KEY_NAME)
                                      .select(Select.SPECIFIC_ATTRIBUTES).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // Only one attribute as specified in AttributesToGet
        assertEquals(1, result.items().get(0).size());
    }

    /**
     * Tests making queries with local secondary index.
     */
    @Test
    public void testQueryWithLocalSecondaryIndex() throws Exception {
        String randomHashKeyValue = UUID.randomUUID().toString();
        int totalItemsPerHash = 10;
        int totalIndexedItemsPerHash = 5;
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();

        item.put(HASH_KEY_NAME, AttributeValue.builder().s(randomHashKeyValue).build());
        // Items with LSI range key
        for (int i = 0; i < totalIndexedItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put(LSI_RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put("attribute_" + i, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            item.remove("attribute_" + i);
        }
        item.remove(LSI_RANGE_KEY_NAME);
        // Items without LSI range key
        for (int i = totalIndexedItemsPerHash; i < totalItemsPerHash; i++) {
            item.put(RANGE_KEY_NAME, AttributeValue.builder().n(Integer.toString(i)).build());
            item.put("attribute_" + i, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            item.remove("attribute_" + i);
        }

        /**
         *  1) Query-with-LSI (only by hash key)
         */
        QueryResponse result = dynamo.query(QueryRequest.builder()
                                                  .tableName(tableName)
                                                  .indexName(LSI_NAME)
                                                  .keyConditions(
                                                          Collections.singletonMap(
                                                                  HASH_KEY_NAME,
                                                                  Condition.builder().attributeValueList(
                                                                          AttributeValue.builder()
                                                                                  .s(randomHashKeyValue).build())
                                                                                 .comparisonOperator(
                                                                                         ComparisonOperator.EQ).build())).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // By default, the result includes all the projected attributes.
        assertEquals(3, result.items().get(0).size());

        /**
         * 2) Query-with-LSI (by hash + LSI range)
         */
        int rangeKeyConditionRange = 2;
        Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put(
                HASH_KEY_NAME,
                Condition.builder().attributeValueList(
                        AttributeValue.builder().s(randomHashKeyValue).build())
                               .comparisonOperator(ComparisonOperator.EQ).build());
        keyConditions.put(
                LSI_RANGE_KEY_NAME,
                Condition.builder().attributeValueList(AttributeValue.builder()
                                                               .n(Integer.toString(rangeKeyConditionRange)).build())
                               .comparisonOperator(ComparisonOperator.LT).build());
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(LSI_NAME)
                                      .keyConditions(keyConditions).build());
        assertEquals((Object) rangeKeyConditionRange, (Object) result.count());

        /**
         * 3) Query-with-LSI on selected attributes (by Select)
         */
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(LSI_NAME)
                                      .keyConditions(
                                              Collections.singletonMap(
                                                      HASH_KEY_NAME,
                                                      Condition.builder().attributeValueList(
                                                              AttributeValue.builder()
                                                                      .s(randomHashKeyValue).build())
                                                                     .comparisonOperator(ComparisonOperator.EQ).build()))
                                      .select(Select.ALL_ATTRIBUTES).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // By setting Select.ALL_ATTRIBUTES, all attributes in the item will be returned
        assertEquals(4, result.items().get(0).size());

        /**
         * 4) Query-with-LSI on selected attributes (by AttributesToGet)
         */
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(LSI_NAME)
                                      .keyConditions(
                                              Collections.singletonMap(
                                                      HASH_KEY_NAME,
                                                      Condition.builder().attributeValueList(
                                                              AttributeValue.builder()
                                                                      .s(randomHashKeyValue).build())
                                                                     .comparisonOperator(ComparisonOperator.EQ).build()))
                                      .attributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // Two attributes as specified in AttributesToGet
        assertEquals(2, result.items().get(0).size());

        /**
         * 5) Exception when using both Selection and AttributeToGet
         */
        try {
            result = dynamo.query(QueryRequest.builder()
                                          .tableName(tableName)
                                          .indexName(LSI_NAME)
                                          .keyConditions(
                                                  Collections.singletonMap(
                                                          HASH_KEY_NAME,
                                                          Condition.builder().attributeValueList(
                                                                  AttributeValue.builder()
                                                                          .s(randomHashKeyValue).build())
                                                                         .comparisonOperator(ComparisonOperator.EQ).build()))
                                          .attributesToGet(HASH_KEY_NAME, RANGE_KEY_NAME, LSI_RANGE_KEY_NAME)
                                          .select(Select.ALL_PROJECTED_ATTRIBUTES).build());
            fail("Should trigger exception when using both Select and AttributeToGet.");
        } catch (SdkServiceException exception) {
            // Ignored or expected.
        }

        /**
         * 6) Query-with-LSI on selected attributes (by Select.SPECIFIC_ATTRIBUTES)
         */
        result = dynamo.query(QueryRequest.builder()
                                      .tableName(tableName)
                                      .indexName(LSI_NAME)
                                      .keyConditions(
                                              Collections.singletonMap(
                                                      HASH_KEY_NAME,
                                                      Condition.builder().attributeValueList(
                                                              AttributeValue.builder()
                                                                      .s(randomHashKeyValue).build())
                                                                     .comparisonOperator(
                                                                             ComparisonOperator.EQ).build()))
                                      .attributesToGet(HASH_KEY_NAME)
                                      .select(Select.SPECIFIC_ATTRIBUTES).build());
        // Only the indexed items should be returned
        assertEquals((Object) totalIndexedItemsPerHash, (Object) result.count());
        // Only one attribute as specified in AttributesToGet
        assertEquals(1, result.items().get(0).size());
    }

    private void assertQueryResponseCount(Integer expected, QueryRequest request)
            throws InterruptedException {

        int retries = 0;
        QueryResponse result = null;
        do {
            result = dynamo.query(request);

            if (expected == result.count()) {
                return;
            }
            // Handling eventual consistency.
            Thread.sleep(SLEEP_TIME);
            retries++;
        } while (retries <= MAX_RETRIES);

        Assert.fail("Failed to assert query count. Expected : " + expected
                    + " actual : " + result.count());
    }
}
