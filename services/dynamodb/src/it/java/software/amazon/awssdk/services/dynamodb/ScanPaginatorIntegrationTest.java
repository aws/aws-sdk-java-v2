/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.pagination.SdkIterable;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanPaginator;
import utils.resources.tables.BasicTempTable;
import utils.test.util.DynamoDBTestBase;
import utils.test.util.TableUtils;

public class ScanPaginatorIntegrationTest extends DynamoDBTestBase {

    private static final String TABLE_NAME = BasicTempTable.TEMP_TABLE_NAME;
    private static final String HASH_KEY_NAME = BasicTempTable.HASH_KEY_NAME;
    private static final String ATTRIBUTE_FOO = "attribute_foo";
    private static final int ITEM_COUNT = 19;

    @BeforeClass
    public static void setUpFixture() throws Exception {
        DynamoDBTestBase.setUpTestBase();

        dynamo.createTable(CreateTableRequest.builder().tableName(TABLE_NAME)
                                             .keySchema(KeySchemaElement.builder().keyType(KeyType.HASH)
                                                                        .attributeName(HASH_KEY_NAME)
                                                                        .build())
                                             .attributeDefinitions(AttributeDefinition.builder()
                                                                                      .attributeType(ScalarAttributeType.N)
                                                                                      .attributeName(HASH_KEY_NAME)
                                                                                      .build())
                                             .provisionedThroughput(ProvisionedThroughput.builder()
                                                                                         .readCapacityUnits(5L)
                                                                                         .writeCapacityUnits(5L)
                                                                                         .build())
                                             .build());

        TableUtils.waitUntilActive(dynamo, TABLE_NAME);

        putTestData();
    }

    @AfterClass
    public static void cleanUpFixture() {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
    }

    @Test
    public void test_MultipleIteration_On_Responses_Iterable() {
        ScanRequest request = ScanRequest.builder().tableName(TABLE_NAME).limit(2).build();
        ScanPaginator scanResponses = dynamo.scanIterable(request);

        int count = 0;

        // Iterate once
        for (ScanResponse response : scanResponses) {
            count += response.count();
        }
        Assert.assertEquals(ITEM_COUNT, count);

        // Iterate second time
        count = 0;
        for (ScanResponse response : scanResponses) {
            count += response.count();
        }
        Assert.assertEquals(ITEM_COUNT, count);
    }

    @Test
    public void test_MultipleIteration_On_PaginatedMember_Iterable() {
        ScanRequest request = ScanRequest.builder().tableName(TABLE_NAME).limit(2).build();
        SdkIterable<Map<String, AttributeValue>> items = dynamo.scanIterable(request).items();

        int count = 0;

        // Iterate once
        for (Map<String, AttributeValue> item : items) {
            count++;
        }
        Assert.assertEquals(ITEM_COUNT, count);

        // Iterate second time
        count = 0;
        for (Map<String, AttributeValue> item : items) {
            count++;
        }
        Assert.assertEquals(ITEM_COUNT, count);
    }

    @Test
    public void test_MultipleIteration_On_Responses_Stream() {
        int results_per_page = 2;
        ScanRequest request = ScanRequest.builder().tableName(TABLE_NAME).limit(results_per_page).build();
        ScanPaginator scanResponses = dynamo.scanIterable(request);

        // Iterate once
        Assert.assertEquals(ITEM_COUNT, scanResponses.stream()
                                                     .mapToInt(response -> response.count())
                                                     .sum());

        // Iterate second time
        Assert.assertEquals(ITEM_COUNT, scanResponses.stream()
                                                     .mapToInt(response -> response.count())
                                                     .sum());

        // Number of pages
        Assert.assertEquals(Math.ceil((double) ITEM_COUNT/results_per_page), scanResponses.stream().count(), 0);
    }

    @Test
    public void test_MultipleIteration_On_PaginatedMember_Stream() {
        int results_per_page = 2;
        ScanRequest request = ScanRequest.builder().tableName(TABLE_NAME).limit(results_per_page).build();
        SdkIterable<Map<String, AttributeValue>> items = dynamo.scanIterable(request).items();

        // Iterate once
        Assert.assertEquals(ITEM_COUNT, items.stream().distinct().count());

        // Iterate second time
        Assert.assertEquals(ITEM_COUNT, items.stream().distinct().count());
    }

    @Test (expected = IllegalStateException.class)
    public void iteration_On_SameStream_ThrowsError() {
        int results_per_page = 2;
        ScanRequest request = ScanRequest.builder().tableName(TABLE_NAME).limit(results_per_page).build();
        Stream<Map<String, AttributeValue>> itemsStream = dynamo.scanIterable(request).items().stream();

        // Iterate once
        Assert.assertEquals(ITEM_COUNT, itemsStream.distinct().count());

        // Iterate second time
        Assert.assertEquals(ITEM_COUNT, itemsStream.distinct().count());
    }

    @Test
    public void mix_Iterator_And_Stream_Calls() {
        ScanRequest request = ScanRequest.builder().tableName(TABLE_NAME).limit(2).build();
        ScanPaginator scanResponses = dynamo.scanIterable(request);

        Assert.assertEquals(ITEM_COUNT, scanResponses.stream().flatMap(r -> r.items().stream())
                                                     .distinct()
                                                     .count());


        Assert.assertEquals(ITEM_COUNT, scanResponses.stream().mapToInt(response -> response.count()).sum());


        int count = 0;
        for (ScanResponse response : scanResponses) {
            count += response.count();
        }
        Assert.assertEquals(ITEM_COUNT, count);
    }

    private static void putTestData() {
        Map<String, AttributeValue> item = new HashMap();
        Random random = new Random();

        for (int hashKeyValue = 0; hashKeyValue < ITEM_COUNT; hashKeyValue++) {
            item.put(HASH_KEY_NAME, AttributeValue.builder().n(Integer.toString(hashKeyValue)).build());
            item.put(ATTRIBUTE_FOO, AttributeValue.builder().n(Integer.toString(random.nextInt(ITEM_COUNT))).build());

            dynamo.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
            item.clear();
        }
    }

}