package com.amazonaws.services.dynamodbv2.mapper;

import static org.junit.Assert.*;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.LocalDynamoDBTestBase;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.DynamoDBMapperIntegrationTestBase;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.PaginationLoadingStrategy;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.pojos.RangeKeyClass;

/**
 * Integration tests for PaginationLoadingStrategy configuration
 */
public class MapperLoadingStrategyConfigTest extends LocalDynamoDBTestBase {

    private static long hashKey = System.currentTimeMillis();
    private static int PAGE_SIZE = 5;
    private static int PARALLEL_SEGMENT = 3;
    private static int OBJECTS_NUM = 50;
    private static int RESULTS_NUM = OBJECTS_NUM - 2; // condition: rangeKey > 1.0
    private static AmazonDynamoDB dynamo;

    @BeforeClass
    public static void setUp() throws Exception {
        dynamo = client();
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        dynamo.createTable(mapper.generateCreateTableRequest(RangeKeyClass.class)
                                 .withProvisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT));
        createTestData();
    }

    @Test
    public void testLazyLoading() {
        // Get all the paginated lists using the tested loading strategy
        PaginatedList<RangeKeyClass> queryList = getTestPaginatedQueryList(PaginationLoadingStrategy.LAZY_LOADING);
        PaginatedList<RangeKeyClass> scanList = getTestPaginatedScanList(PaginationLoadingStrategy.LAZY_LOADING);
        PaginatedList<RangeKeyClass> parallelScanList = getTestPaginatedParallelScanList(PaginationLoadingStrategy.LAZY_LOADING);
        
        // check that only at most one page of results are loaded up to this point
        assertTrue(getLoadedResultsNumber(queryList) <= PAGE_SIZE);
        assertTrue(getLoadedResultsNumber(scanList) <= PAGE_SIZE);
        assertTrue(getLoadedResultsNumber(parallelScanList) <= PAGE_SIZE * PARALLEL_SEGMENT);
        
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
        assertEquals(RESULTS_NUM, getLoadedResultsNumber(queryList));
        assertEquals(RESULTS_NUM, getLoadedResultsNumber(scanList));
        assertEquals(RESULTS_NUM, getLoadedResultsNumber(parallelScanList));
        
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
        PaginatedList<RangeKeyClass> parallelScanList = getTestPaginatedParallelScanList(PaginationLoadingStrategy.ITERATION_ONLY);
        
        // check that only at most one page of results are loaded up to this point
        assertTrue(getLoadedResultsNumber(queryList) <= PAGE_SIZE);
        assertTrue(getLoadedResultsNumber(scanList) <= PAGE_SIZE);
        assertTrue(getLoadedResultsNumber(parallelScanList) <= PAGE_SIZE * PARALLEL_SEGMENT);
        
        testIterationOnlyPaginatedListOperations(queryList);
        testIterationOnlyPaginatedListOperations(scanList);
        testIterationOnlyPaginatedListOperations(parallelScanList);
    }
    
    private static void createTestData() {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);

        List<RangeKeyClass> objs = new ArrayList<RangeKeyClass>();
        for ( int i = 0; i < OBJECTS_NUM; i++ ) {
            RangeKeyClass obj = new RangeKeyClass();
            obj.setKey(hashKey);
            obj.setRangeKey(i);
            objs.add(obj);
        }
        
        mapper.batchSave(objs);
    }
    
    private static PaginatedList<RangeKeyClass> getTestPaginatedQueryList(PaginationLoadingStrategy paginationLoadingStrategy) {
        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(ConsistentReads.CONSISTENT);
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo, mapperConfig);
        
        // Construct the query expression for the tested hash-key value and any range-key value greater that 1.0
        RangeKeyClass keyObject = new RangeKeyClass();
        keyObject.setKey(hashKey);
        DynamoDBQueryExpression<RangeKeyClass> queryExpression = new DynamoDBQueryExpression<RangeKeyClass>().withHashKeyValues(keyObject);
        queryExpression.withRangeKeyCondition("rangeKey",
                new Condition().withComparisonOperator(ComparisonOperator.GT.toString()).withAttributeValueList(
                        new AttributeValue().withN("1.0"))).withLimit(PAGE_SIZE);
        
        return mapper.query(RangeKeyClass.class, queryExpression, new DynamoDBMapperConfig(paginationLoadingStrategy));
    }
    
    private static PaginatedList<RangeKeyClass> getTestPaginatedScanList(PaginationLoadingStrategy paginationLoadingStrategy) {
        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(ConsistentReads.CONSISTENT);
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo, mapperConfig);
        
        // Construct the scan expression with the exact same conditions
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addFilterCondition("key", 
                new Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(
                        new AttributeValue().withN(Long.toString(hashKey))));
        scanExpression.addFilterCondition("rangeKey", 
                new Condition().withComparisonOperator(ComparisonOperator.GT).withAttributeValueList(
                        new AttributeValue().withN("1.0")));
        scanExpression.setLimit(PAGE_SIZE);
        
        return mapper.scan(RangeKeyClass.class, scanExpression, new DynamoDBMapperConfig(paginationLoadingStrategy));
    }
    
    private static PaginatedList<RangeKeyClass> getTestPaginatedParallelScanList(PaginationLoadingStrategy paginationLoadingStrategy) {
        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(ConsistentReads.CONSISTENT);
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo, mapperConfig);
        
        // Construct the scan expression with the exact same conditions
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addFilterCondition("key", 
                new Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(
                        new AttributeValue().withN(Long.toString(hashKey))));
        scanExpression.addFilterCondition("rangeKey", 
                new Condition().withComparisonOperator(ComparisonOperator.GT).withAttributeValueList(
                        new AttributeValue().withN("1.0")));
        scanExpression.setLimit(PAGE_SIZE);
        
        return mapper.parallelScan(RangeKeyClass.class, scanExpression, PARALLEL_SEGMENT, new DynamoDBMapperConfig(paginationLoadingStrategy));
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
        } catch (IndexOutOfBoundsException e) {}
        
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
        } catch (UnsupportedOperationException e) {}
        
        // (2) get(int n)
        try {
            list.get(RESULTS_NUM / 2);
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {}
        
        // (3) contains(Object org0)
        try {
            list.contains(new RangeKeyClass());
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {}
        
        // (4) subList(int org0, int arg1)
        try {
            list.subList(0, RESULTS_NUM);
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {}
        
        // (5) indexOf(Object org0)
        try {
            list.indexOf(new RangeKeyClass());
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {}
        
        // (6) loadAllResults()
        try {
            list.loadAllResults();
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {}
        
        // (7) size()
        try {
            list.size();
            fail("UnsupportedOperationException expected but is not thrown");
        } catch (UnsupportedOperationException e) {};
        
        // Could be iterated once
        for (RangeKeyClass item : list) {
            assertEquals(hashKey, item.getKey());
            assertTrue(item.getRangeKey() < OBJECTS_NUM);
            // At most one page of results in memeory
            assertTrue(getLoadedResultsNumber(list) <= PAGE_SIZE);
        }
        
        // not twice
        try {
            for (@SuppressWarnings("unused") RangeKeyClass item : list) {
                fail("UnsupportedOperationException expected but is not thrown");
            }
        } catch (UnsupportedOperationException e) {}
        
    }
    
    /** Use reflection to get the size of the private allResults field **/
    @SuppressWarnings("unchecked")
    private static int getLoadedResultsNumber(PaginatedList<RangeKeyClass> list) {
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
}
