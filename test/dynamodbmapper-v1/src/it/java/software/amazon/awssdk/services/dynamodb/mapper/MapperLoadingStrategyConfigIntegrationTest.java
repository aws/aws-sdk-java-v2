/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.ConsistentReads;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.PaginationLoadingStrategy;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbQueryExpression;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbScanExpression;
import software.amazon.awssdk.services.dynamodb.datamodeling.PaginatedList;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.pojos.RangeKeyClass;

/**
 * Integration tests for PaginationLoadingStrategy configuration
 */
public class MapperLoadingStrategyConfigIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static long hashKey = System.currentTimeMillis();
    private static int PAGE_SIZE = 5;
    private static int PARALLEL_SEGMENT = 3;
    private static int OBJECTS_NUM = 50;
    private static int RESULTS_NUM = OBJECTS_NUM - 2; // condition: rangeKey > 1.0

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTableWithRangeAttribute();
        createTestData();
    }

    private static void createTestData() {
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);

        List<RangeKeyClass> objs = new ArrayList<RangeKeyClass>();
        for (int i = 0; i < OBJECTS_NUM; i++) {
            RangeKeyClass obj = new RangeKeyClass();
            obj.setKey(hashKey);
            obj.setRangeKey(i);
            objs.add(obj);
        }

        mapper.batchSave(objs);
    }

    private static PaginatedList<RangeKeyClass> getTestPaginatedQueryList(PaginationLoadingStrategy paginationLoadingStrategy) {
        DynamoDbMapperConfig mapperConfig = new DynamoDbMapperConfig(ConsistentReads.CONSISTENT);
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo, mapperConfig);

        // Construct the query expression for the tested hash-key value and any range-key value greater that 1.0
        RangeKeyClass keyObject = new RangeKeyClass();
        keyObject.setKey(hashKey);
        DynamoDbQueryExpression<RangeKeyClass> queryExpression = new DynamoDbQueryExpression<RangeKeyClass>()
                .withHashKeyValues(keyObject);
        queryExpression.withRangeKeyCondition("rangeKey",
                                              Condition.builder().comparisonOperator(ComparisonOperator.GT.toString())
                                                             .attributeValueList(
                                                                     AttributeValue.builder().n("1.0").build()).build()).withLimit(PAGE_SIZE);

        return mapper.query(RangeKeyClass.class, queryExpression, new DynamoDbMapperConfig(paginationLoadingStrategy));
    }

    private static PaginatedList<RangeKeyClass> getTestPaginatedScanList(PaginationLoadingStrategy paginationLoadingStrategy) {
        DynamoDbMapperConfig mapperConfig = new DynamoDbMapperConfig(ConsistentReads.CONSISTENT);
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo, mapperConfig);

        // Construct the scan expression with the exact same conditions
        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression();
        scanExpression.addFilterCondition("key",
                                          Condition.builder().comparisonOperator(ComparisonOperator.EQ).attributeValueList(
                                                  AttributeValue.builder().n(Long.toString(hashKey)).build()).build());
        scanExpression.addFilterCondition("rangeKey",
                                          Condition.builder().comparisonOperator(ComparisonOperator.GT).attributeValueList(
                                                  AttributeValue.builder().n("1.0").build()).build());
        scanExpression.setLimit(PAGE_SIZE);

        return mapper.scan(RangeKeyClass.class, scanExpression, new DynamoDbMapperConfig(paginationLoadingStrategy));
    }

    private static PaginatedList<RangeKeyClass> getTestPaginatedParallelScanList(
            PaginationLoadingStrategy paginationLoadingStrategy) {
        DynamoDbMapperConfig mapperConfig = new DynamoDbMapperConfig(ConsistentReads.CONSISTENT);
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo, mapperConfig);

        // Construct the scan expression with the exact same conditions
        DynamoDbScanExpression scanExpression = new DynamoDbScanExpression();
        scanExpression.addFilterCondition("key",
                                          Condition.builder().comparisonOperator(ComparisonOperator.EQ).attributeValueList(
                                                  AttributeValue.builder().n(Long.toString(hashKey)).build()).build());
        scanExpression.addFilterCondition("rangeKey",
                                          Condition.builder().comparisonOperator(ComparisonOperator.GT).attributeValueList(
                                                  AttributeValue.builder().n("1.0").build()).build());
        scanExpression.setLimit(PAGE_SIZE);

        return mapper.parallelScan(RangeKeyClass.class, scanExpression, PARALLEL_SEGMENT,
                                   new DynamoDbMapperConfig(paginationLoadingStrategy));
    }

    private static void testAllPaginatedListOperations(PaginatedList<RangeKeyClass> list) {

        // (1) isEmpty()
        assertFalse(list.isEmpty());

        // (2) get(int n)
        assertNotNull(list.get(RESULTS_NUM / 2));

        // (3) contains(Object org0)
        RangeKeyClass obj = new RangeKeyClass();
        obj.setKey(hashKey);
        obj.setRangeKey(0);
        assertFalse(list.contains(obj));
        obj.setRangeKey(2);
        assertTrue(list.contains(obj));

        // (4) subList(int org0, int arg1)
        List<RangeKeyClass> subList = list.subList(0, RESULTS_NUM);
        assertEquals(RESULTS_NUM, subList.size());
        try {
            list.subList(0, RESULTS_NUM + 1);
            fail("IndexOutOfBoundsException is IndexOutOfBoundsException but not thrown");
        } catch (IndexOutOfBoundsException e) {
            // Ignored or expected.
        }

        // (5) indexOf(Object org0)
        assertTrue(list.indexOf(obj) < RESULTS_NUM);

        // (6) loadAllResults()
        list.loadAllResults();

        // (7) size()
        assertEquals(RESULTS_NUM, list.size());

    }

    private static void testPaginatedListIterator(PaginatedList<RangeKeyClass> list) {
        for (RangeKeyClass item : list) {
            assertEquals(hashKey, item.getKey());
            assertTrue(item.getRangeKey() < OBJECTS_NUM);
        }

        // make sure the list could be iterated again
        for (RangeKeyClass item : list) {
            assertEquals(hashKey, item.getKey());
            assertTrue(item.getRangeKey() < OBJECTS_NUM);
        }
    }

    private static void testIterationOnlyPaginatedListOperations(PaginatedList<RangeKeyClass> list) {

        // Unsupported operations

        // (1) isEmpty()
        try {
            list.isEmpty();
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

        // (2) get(int n)
        try {
            list.get(RESULTS_NUM / 2);
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

        // (3) contains(Object org0)
        try {
            list.contains(new RangeKeyClass());
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

        // (4) subList(int org0, int arg1)
        try {
            list.subList(0, RESULTS_NUM);
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

        // (5) indexOf(Object org0)
        try {
            list.indexOf(new RangeKeyClass());
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

        // (6) loadAllResults()
        try {
            list.loadAllResults();
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

        // (7) size()
        try {
            list.size();
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }
        ;

        // Could be iterated once
        for (RangeKeyClass item : list) {
            assertEquals(hashKey, item.getKey());
            assertTrue(item.getRangeKey() < OBJECTS_NUM);
            // At most one page of results in memeory
            assertTrue(loadedResultsNumber(list) <= PAGE_SIZE);
        }

        // not twice
        try {
            for (@SuppressWarnings("unused") RangeKeyClass item : list) {
                fail("UnsupportedOperationException expected but is not thrown");
            }
        } catch (UnsupportedOperationException e) {
            // Ignored or expected.
        }

    }

    /**
     * Use reflection to get the size of the private allResults field
     **/
    @SuppressWarnings("unchecked")
    private static int loadedResultsNumber(PaginatedList<RangeKeyClass> list) {
        Field privateAllResults = null;
        try {
            privateAllResults = list.getClass().getSuperclass().getDeclaredField("allResults");
        } catch (SecurityException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        privateAllResults.setAccessible(true);
        List<RangeKeyClass> allResults = null;
        try {
            allResults = (List<RangeKeyClass>) privateAllResults.get(list);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        }
        return allResults.size();
    }

    @Test
    public void testLazyLoading() {
        // Get all the paginated lists using the tested loading strategy
        PaginatedList<RangeKeyClass> queryList = getTestPaginatedQueryList(PaginationLoadingStrategy.LAZY_LOADING);
        PaginatedList<RangeKeyClass> scanList = getTestPaginatedScanList(PaginationLoadingStrategy.LAZY_LOADING);
        PaginatedList<RangeKeyClass> parallelScanList = getTestPaginatedParallelScanList(PaginationLoadingStrategy.LAZY_LOADING);

        // check that only at most one page of results are loaded up to this point
        assertTrue(loadedResultsNumber(queryList) <= PAGE_SIZE);
        assertTrue(loadedResultsNumber(scanList) <= PAGE_SIZE);
        assertTrue(loadedResultsNumber(parallelScanList) <= PAGE_SIZE * PARALLEL_SEGMENT);

        testAllPaginatedListOperations(queryList);
        testAllPaginatedListOperations(scanList);
        testAllPaginatedListOperations(parallelScanList);

        // Re-construct the paginated lists and test the iterator behavior
        queryList = getTestPaginatedQueryList(PaginationLoadingStrategy.LAZY_LOADING);
        scanList = getTestPaginatedScanList(PaginationLoadingStrategy.LAZY_LOADING);
        parallelScanList = getTestPaginatedParallelScanList(PaginationLoadingStrategy.LAZY_LOADING);

        testPaginatedListIterator(queryList);
        testPaginatedListIterator(scanList);
        testPaginatedListIterator(parallelScanList);

    }

    @Test
    public void testEagerLoading() {
        // Get all the paginated lists using the tested loading strategy
        PaginatedList<RangeKeyClass> queryList = getTestPaginatedQueryList(PaginationLoadingStrategy.EAGER_LOADING);
        PaginatedList<RangeKeyClass> scanList = getTestPaginatedScanList(PaginationLoadingStrategy.EAGER_LOADING);
        PaginatedList<RangeKeyClass> parallelScanList = getTestPaginatedParallelScanList(PaginationLoadingStrategy.EAGER_LOADING);

        // check that all results have been loaded
        assertEquals(RESULTS_NUM, loadedResultsNumber(queryList));
        assertEquals(RESULTS_NUM, loadedResultsNumber(scanList));
        assertEquals(RESULTS_NUM, loadedResultsNumber(parallelScanList));

        testAllPaginatedListOperations(queryList);
        testAllPaginatedListOperations(scanList);
        testAllPaginatedListOperations(parallelScanList);

        // Re-construct the paginated lists and test the iterator behavior
        queryList = getTestPaginatedQueryList(PaginationLoadingStrategy.LAZY_LOADING);
        scanList = getTestPaginatedScanList(PaginationLoadingStrategy.LAZY_LOADING);
        parallelScanList = getTestPaginatedParallelScanList(PaginationLoadingStrategy.LAZY_LOADING);

        testPaginatedListIterator(queryList);
        testPaginatedListIterator(scanList);
        testPaginatedListIterator(parallelScanList);
    }

    @Test
    public void testIterationOnly() {
        // Get all the paginated lists using the tested loading strategy
        PaginatedList<RangeKeyClass> queryList = getTestPaginatedQueryList(PaginationLoadingStrategy.ITERATION_ONLY);
        PaginatedList<RangeKeyClass> scanList = getTestPaginatedScanList(PaginationLoadingStrategy.ITERATION_ONLY);
        PaginatedList<RangeKeyClass> parallelScanList = getTestPaginatedParallelScanList(
                PaginationLoadingStrategy.ITERATION_ONLY);

        // check that only at most one page of results are loaded up to this point
        assertTrue(loadedResultsNumber(queryList) <= PAGE_SIZE);
        assertTrue(loadedResultsNumber(scanList) <= PAGE_SIZE);
        assertTrue(loadedResultsNumber(parallelScanList) <= PAGE_SIZE * PARALLEL_SEGMENT);

        testIterationOnlyPaginatedListOperations(queryList);
        testIterationOnlyPaginatedListOperations(scanList);
        testIterationOnlyPaginatedListOperations(parallelScanList);
    }
}
