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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbScanExpression;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.PaginatedParallelScanList;
import software.amazon.awssdk.services.dynamodb.datamodeling.PaginatedScanList;
import software.amazon.awssdk.services.dynamodb.datamodeling.ScanResultPage;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import software.amazon.awssdk.util.ImmutableMapParameter;

/**
 * Integration tests for the scan operation on DynamoDBMapper.
 */
public class ScanIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String TABLE_NAME = "aws-java-sdk-util-scan";
    /**
     * We set a small limit in order to test the behavior of PaginatedList
     * when it could not load all the scan result in one batch.
     */
    private static final int SCAN_LIMIT = 10;
    private static final int PARALLEL_SCAN_SEGMENTS = 5;

    private static void createTestData() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (int i = 0; i < 500; i++) {
            util.save(new SimpleClass(Integer.toString(i), Integer.toString(i)));
        }
    }

    @BeforeClass
    public static void setUpTestData() throws Exception {
        String keyName = "id";
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName(keyName).keyType(KeyType.HASH).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(keyName).attributeType(
                                ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(5L).build())
                .build();

        TableUtils.createTableIfNotExists(dynamo, createTableRequest);
        TableUtils.waitUntilActive(dynamo, TABLE_NAME);

        createTestData();
    }


    @Test
    public void testScan() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression().withLimit(SCAN_LIMIT);
        scanExpression
                .addFilterCondition("value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());
        scanExpression
                .addFilterCondition("extraData", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());
        List<SimpleClass> list = util.scan(SimpleClass.class, scanExpression);

        int count = 0;
        Iterator<SimpleClass> iterator = list.iterator();
        while (iterator.hasNext()) {
            count++;
            SimpleClass next = iterator.next();
            assertNotNull(next.getExtraData());
            assertNotNull(next.getValue());
        }

        int totalCount = util.count(SimpleClass.class, scanExpression);

        assertNotNull(list.get(totalCount / 2));
        assertEquals(totalCount, count);
        assertEquals(totalCount, list.size());

        assertTrue(list.contains(list.get(list.size() / 2)));
        assertEquals(count, list.toArray().length);
    }

    /**
     * Tests scanning the table with AND/OR logic operator.
     */
    @Test
    public void testScanWithConditionalOperator() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);

        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression()
                .withLimit(SCAN_LIMIT)
                .withScanFilter(ImmutableMapParameter.of(
                        "value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build(),
                        "non-existent-field", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build()
                                                        ))
                .withConditionalOperator(ConditionalOperator.AND);

        List<SimpleClass> andConditionResult = mapper.scan(SimpleClass.class, scanExpression);
        assertTrue(andConditionResult.isEmpty());

        List<SimpleClass> orConditionResult = mapper.scan(SimpleClass.class,
                                                          scanExpression.withConditionalOperator(ConditionalOperator.OR));
        assertFalse(orConditionResult.isEmpty());
    }

    @Test
    public void testParallelScan() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression().withLimit(SCAN_LIMIT);
        scanExpression
                .addFilterCondition("value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());
        scanExpression
                .addFilterCondition("extraData", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());

        PaginatedParallelScanList<SimpleClass> parallelScanList = util
                .parallelScan(SimpleClass.class, scanExpression, PARALLEL_SCAN_SEGMENTS);
        int count = 0;
        Iterator<SimpleClass> iterator = parallelScanList.iterator();
        HashMap<String, Boolean> allDataAppearance = new HashMap<String, Boolean>();
        for (int i = 0; i < 500; i++) {
            allDataAppearance.put("" + i, false);
        }
        while (iterator.hasNext()) {
            count++;
            SimpleClass next = iterator.next();
            assertNotNull(next.getExtraData());
            assertNotNull(next.getValue());
            allDataAppearance.put(next.getId(), true);
        }
        assertFalse(allDataAppearance.values().contains(false));

        int totalCount = util.count(SimpleClass.class, scanExpression);

        assertNotNull(parallelScanList.get(totalCount / 2));
        assertEquals(totalCount, count);
        assertEquals(totalCount, parallelScanList.size());

        assertTrue(parallelScanList.contains(parallelScanList.get(parallelScanList.size() / 2)));
        assertEquals(count, parallelScanList.toArray().length);

    }

    @Test
    public void testParallelScanPerformance() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression().withLimit(SCAN_LIMIT);
        scanExpression
                .addFilterCondition("value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());
        scanExpression
                .addFilterCondition("extraData", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());

        long startTime = System.currentTimeMillis();
        PaginatedScanList<SimpleClass> scanList = util.scan(SimpleClass.class, scanExpression);
        scanList.loadAllResults();
        long fullTableScanTime = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        PaginatedParallelScanList<SimpleClass> parallelScanList = util
                .parallelScan(SimpleClass.class, scanExpression, PARALLEL_SCAN_SEGMENTS);
        parallelScanList.loadAllResults();
        long parallelScanTime = System.currentTimeMillis() - startTime;
        assertEquals(scanList.size(), parallelScanList.size());
        assertTrue(fullTableScanTime > parallelScanTime);
        System.out.println("fullTableScanTime : " + fullTableScanTime + "(ms), parallelScanTime : " + parallelScanTime + "(ms).");
    }

    @Test
    public void testParallelScanExceptionHandling() {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        int INVALID_LIMIT = 0;
        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression().withLimit(INVALID_LIMIT);
        try {
            PaginatedParallelScanList<SimpleClass> parallelScanList = util
                    .parallelScan(SimpleClass.class, scanExpression, PARALLEL_SCAN_SEGMENTS);
            fail("Should have seen the AmazonServiceException");
        } catch (AmazonServiceException ase) {
            assertNotNull(ase.getErrorCode());
            assertNotNull(ase.getErrorType());
            assertNotNull(ase.getMessage());
        } catch (Exception e) {
            fail("Should have seen the AmazonServiceException");
        }

    }

    @Test
    public void testScanPage() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression();
        scanExpression.addFilterCondition("value",
                                          Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());
        scanExpression.addFilterCondition("extraData",
                                          Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL.toString()).build());
        int limit = 3;
        scanExpression.setLimit(limit);
        ScanResultPage<SimpleClass> result = util.scanPage(SimpleClass.class, scanExpression);

        int count = 0;
        Iterator<SimpleClass> iterator = result.getResults().iterator();
        Set<SimpleClass> seen = new HashSet<SimpleClass>();
        while (iterator.hasNext()) {
            count++;
            SimpleClass next = iterator.next();
            assertNotNull(next.getExtraData());
            assertNotNull(next.getValue());
            assertTrue(seen.add(next));
        }

        assertEquals(limit, count);
        assertEquals(count, result.getResults().toArray().length);

        scanExpression.setExclusiveStartKey(result.lastEvaluatedKey());
        result = util.scanPage(SimpleClass.class, scanExpression);

        iterator = result.getResults().iterator();
        count = 0;
        while (iterator.hasNext()) {
            count++;
            SimpleClass next = iterator.next();
            assertNotNull(next.getExtraData());
            assertNotNull(next.getValue());
            assertTrue(seen.add(next));
        }

        assertEquals(limit, count);
        assertEquals(count, result.getResults().toArray().length);

    }

    @DynamoDbTable(tableName = "aws-java-sdk-util-scan")
    public static final class SimpleClass {
        private String id;
        private String value;
        private String extraData;


        public SimpleClass() {
        }

        public SimpleClass(String id, String value) {
            this.id = id;
            this.value = value;
            this.extraData = UUID.randomUUID().toString();
        }

        @DynamoDbHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getExtraData() {
            return extraData;
        }

        public void setExtraData(String extraData) {
            this.extraData = extraData;
        }
    }
}
