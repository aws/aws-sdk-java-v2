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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.TestTableForParallelScan;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests on the low-level parallel scan operation.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = TestTableForParallelScan.class,
                                              creationPolicy = ResourceCreationPolicy.REUSE_EXISTING,
                                              retentionPolicy = ResourceRetentionPolicy.KEEP)
                    })
public class ParallelScanIntegrationTest extends DynamoDBTestBase {

    private static final String tableName = TestTableForParallelScan.TABLE_NAME;
    private static final String HASH_KEY_NAME = TestTableForParallelScan.HASH_KEY_NAME;
    private static final String ATTRIBUTE_FOO = "attribute_foo";
    private static final String ATTRIBUTE_BAR = "attribute_bar";
    private static final String ATTRIBUTE_RANDOM = "attribute_random";
    private static final int itemNumber = 200;

    /**
     * Creates a test table with a local secondary index
     */
    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();
    }

    private static void putTestData() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        Random random = new Random();
        for (int hashKeyValue = 0; hashKeyValue < itemNumber; hashKeyValue++) {
            item.put(HASH_KEY_NAME, AttributeValue.builder().n(Integer.toString(hashKeyValue)).build());
            item.put(ATTRIBUTE_RANDOM, AttributeValue.builder().n(Integer.toString(random.nextInt(itemNumber))).build());
            if (hashKeyValue < itemNumber / 2) {
                item.put(ATTRIBUTE_FOO, AttributeValue.builder().n(Integer.toString(hashKeyValue)).build());
            } else {
                item.put(ATTRIBUTE_BAR, AttributeValue.builder().n(Integer.toString(hashKeyValue)).build());
            }

            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            item.clear();
        }
    }

    /**
     * Tests making parallel scan on DynamoDB table.
     */
    @Test
    public void testParallelScan() {
        putTestData();

        /**
         * Only one segment.
         */
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .scanFilter(Collections.singletonMap(
                        ATTRIBUTE_RANDOM,
                        Condition.builder()
                                .attributeValueList(
                                        AttributeValue.builder().n("" + itemNumber / 2).build())
                                .comparisonOperator(
                                        ComparisonOperator.LT.toString()).build()))
                .totalSegments(1).segment(0).build();
        ScanResponse scanResult = dynamo.scan(scanRequest);
        assertEquals((Object) itemNumber, (Object) scanResult.scannedCount());
        int filteredItems = scanResult.count();

        /**
         * Multiple segments.
         */
        int totalSegments = 10;
        int filteredItemsInsegments = 0;
        for (int segment = 0; segment < totalSegments; segment++) {
            scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .scanFilter(
                            Collections.singletonMap(
                                    ATTRIBUTE_RANDOM,
                                    Condition.builder().attributeValueList(
                                            AttributeValue.builder().n(""
                                                                       + itemNumber / 2).build())
                                                   .comparisonOperator(
                                                           ComparisonOperator.LT
                                                                   .toString()).build()))
                    .totalSegments(totalSegments).segment(segment).build();
            scanResult = dynamo.scan(scanRequest);
            filteredItemsInsegments += scanResult.count();
        }
        assertEquals(filteredItems, filteredItemsInsegments);
    }
}
