package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.ConsistentReads;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBQueryExpression;
import software.amazon.awssdk.mapper.dynamodb.QueryResultPage;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.Select;
import software.amazon.awssdk.mapper.dynamodb.pojos.RangeKeyClass;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the query operation on DynamoDBMapper.
 */
public class QueryIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final long HASH_KEY = System.currentTimeMillis();
    private static RangeKeyClass hashKeyObject;
    private static final int TEST_ITEM_NUMBER = 500;
    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTableWithRangeAttribute();

        DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig(ConsistentReads.CONSISTENT);
        mapper = new DynamoDBMapper(dynamo, mapperConfig);

        putTestData(mapper, TEST_ITEM_NUMBER);

        hashKeyObject = new RangeKeyClass();
        hashKeyObject.setKey(HASH_KEY);
    }

    @Test
    public void testQueryWithSelectCount() throws Exception {
        DynamoDBQueryExpression<RangeKeyClass> countExpression =
                new DynamoDBQueryExpression<RangeKeyClass>().withHashKeyValues(hashKeyObject)
                                                            .withSelect(Select.COUNT);
        QueryResultPage<RangeKeyClass> list = mapper.queryPage(RangeKeyClass.class, countExpression);
        assertEquals((Integer) TEST_ITEM_NUMBER, list.getCount());
    }

    @Test
    public void testQueryWithPrimaryRangeKey() throws Exception {
        DynamoDBQueryExpression<RangeKeyClass> queryExpression =
                new DynamoDBQueryExpression<RangeKeyClass>()
                    .withHashKeyValues(hashKeyObject)
                    .withRangeKeyCondition(
                            "rangeKey",
                            new Condition()
                                .withComparisonOperator(ComparisonOperator.GT)
                                .withAttributeValueList(new AttributeValue().withN("1.0")))
                    .withLimit(11);
        List<RangeKeyClass> list = mapper.query(RangeKeyClass.class, queryExpression);

        int count = 0;
        Iterator<RangeKeyClass> iterator = list.iterator();
        while ( iterator.hasNext() ) {
            count++;
            RangeKeyClass next = iterator.next();
            assertTrue(next.getRangeKey() > 1.00);
        }

        int numMatchingObjects = TEST_ITEM_NUMBER - 2;
        assertEquals(count, numMatchingObjects);
        assertEquals(numMatchingObjects, list.size());

        assertNotNull(list.get(list.size() / 2));
        assertTrue(list.contains(list.get(list.size() / 2)));
        assertEquals(numMatchingObjects, list.toArray().length);

        Thread.sleep(250);
        int totalCount = mapper.count(RangeKeyClass.class, queryExpression);
        assertEquals(numMatchingObjects, totalCount);

        /**
         * Tests query with only hash key
         */
        queryExpression = new DynamoDBQueryExpression<RangeKeyClass>().withHashKeyValues(hashKeyObject);
        list = mapper.query(RangeKeyClass.class, queryExpression);
        assertEquals(TEST_ITEM_NUMBER, list.size());
    }

    /**
     * Tests making queries using query filter on non-key attributes.
     */
    @Test
    public void testQueryFilter() {
        // A random filter condition to be applied to the query.
        Random random = new Random();
        int randomFilterValue = random.nextInt(TEST_ITEM_NUMBER);
        Condition filterCondition = new Condition()
            .withComparisonOperator(ComparisonOperator.LT)
            .withAttributeValueList(
                new AttributeValue().withN(Integer.toString(randomFilterValue)));

        /*
         * (1) Apply the filter on the range key, in form of key condition
         */
        DynamoDBQueryExpression<RangeKeyClass> queryWithRangeKeyCondition =
                new DynamoDBQueryExpression<RangeKeyClass>()
                    .withHashKeyValues(hashKeyObject)
                    .withRangeKeyCondition("rangeKey", filterCondition);
        List<RangeKeyClass> rangeKeyConditionResult = mapper.query(RangeKeyClass.class, queryWithRangeKeyCondition);

        /*
         * (2) Apply the filter on the bigDecimalAttribute, in form of query filter
         */
        DynamoDBQueryExpression<RangeKeyClass> queryWithQueryFilterCondition =
                new DynamoDBQueryExpression<RangeKeyClass>()
                    .withHashKeyValues(hashKeyObject)
                    .withQueryFilter(Collections.singletonMap("bigDecimalAttribute", filterCondition));
        List<RangeKeyClass> queryFilterResult = mapper.query(RangeKeyClass.class, queryWithQueryFilterCondition);

        assertEquals(rangeKeyConditionResult.size(), queryFilterResult.size());
        for (int i = 0; i < rangeKeyConditionResult.size(); i++) {
            assertEquals(rangeKeyConditionResult.get(i), queryFilterResult.get(i));
        }
    }

    /**
     * Tests that exception should be raised when user provides an index name
     * when making query with the primary range key.
     */
    @Test
    public void testUnnecessaryIndexNameException() {
    	try{
    		DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        	long hashKey = System.currentTimeMillis();
        	RangeKeyClass keyObject = new RangeKeyClass();
            keyObject.setKey(hashKey);
        	DynamoDBQueryExpression<RangeKeyClass> queryExpression = new DynamoDBQueryExpression<RangeKeyClass>().withHashKeyValues(keyObject);
            queryExpression.withRangeKeyCondition("rangeKey",
    				                new Condition().withComparisonOperator(ComparisonOperator.GT.toString()).withAttributeValueList(
    				                        new AttributeValue().withN("1.0"))).withLimit(11)
                            .withIndexName("some_index");
        	mapper.query(RangeKeyClass.class, queryExpression);
        	fail("User should not provide index name when making query with the primary range key");
    	} catch (IllegalArgumentException expected) {
    		System.out.println(expected.getMessage());
    	} catch (Exception e) {
    		fail("Should trigger AmazonClientException.");
    	}

    }

    /**
     * Use BatchSave to put some test data into the tested table. Each item is
     * hash-keyed by the same value, and range-keyed by numbers starting from 0.
     */
    private static void putTestData(DynamoDBMapper mapper, int itemNumber) {
        List<RangeKeyClass> objs = new ArrayList<RangeKeyClass>();
        for ( int i = 0; i < itemNumber; i++ ) {
            RangeKeyClass obj = new RangeKeyClass();
            obj.setKey(HASH_KEY);
            obj.setRangeKey(i);
            obj.setBigDecimalAttribute(new BigDecimal(i));
            objs.add(obj);
        }
        mapper.batchSave(objs);
    }
}
