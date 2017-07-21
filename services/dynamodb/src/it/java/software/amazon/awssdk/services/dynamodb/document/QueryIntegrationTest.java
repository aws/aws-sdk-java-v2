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

package software.amazon.awssdk.services.dynamodb.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbQueryExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.pojos.RangeKeyClass;

/**
 * Integration tests for the query operation on DynamoDBMapper.
 */
public class QueryIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final long HASH_KEY = System.currentTimeMillis();
    private static final int TEST_ITEM_NUMBER = 500;
    private static RangeKeyClass hashKeyObject;
    private static DynamoDbMapper mapper;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTableWithRangeAttribute();

        DynamoDbMapperConfig mapperConfig = new DynamoDbMapperConfig(DynamoDbMapperConfig.ConsistentReads.CONSISTENT);
        mapper = new DynamoDbMapper(dynamo, mapperConfig);

        putTestData(mapper, TEST_ITEM_NUMBER);

        hashKeyObject = new RangeKeyClass();
        hashKeyObject.setKey(HASH_KEY);
    }

    /**
     * Use BatchSave to put some test data into the tested table. Each item is
     * hash-keyed by the same value, and range-keyed by numbers starting from 0.
     */
    private static void putTestData(DynamoDbMapper mapper, int itemNumber) {
        List<RangeKeyClass> objs = new ArrayList<RangeKeyClass>();
        for (int i = 0; i < itemNumber; i++) {
            RangeKeyClass obj = new RangeKeyClass();
            obj.setKey(HASH_KEY);
            obj.setRangeKey(i);
            obj.setBigDecimalAttribute(new BigDecimal(i));
            objs.add(obj);
        }
        mapper.batchSave(objs);
    }

    @Test
    public void testQueryWithPrimaryRangeKey() throws Exception {
        DynamoDbQueryExpression<RangeKeyClass> queryExpression =
                new DynamoDbQueryExpression<RangeKeyClass>()
                        .withHashKeyValues(hashKeyObject)
                        .withRangeKeyCondition(
                                "rangeKey",
                                Condition.builder()
                                        .comparisonOperator(ComparisonOperator.GT)
                                        .attributeValueList(AttributeValue.builder().n("1.0").build())
                                        .build())
                        .withLimit(11);
        List<RangeKeyClass> list = mapper.query(RangeKeyClass.class, queryExpression);

        int count = 0;
        Iterator<RangeKeyClass> iterator = list.iterator();
        while (iterator.hasNext()) {
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
        queryExpression = new DynamoDbQueryExpression<RangeKeyClass>().withHashKeyValues(hashKeyObject);
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
        Condition filterCondition = Condition.builder()
                .comparisonOperator(ComparisonOperator.LT)
                .attributeValueList(
                        AttributeValue.builder().n(Integer.toString(randomFilterValue)).build()).build();

        /*
         * (1) Apply the filter on the range key, in form of key condition
         */
        DynamoDbQueryExpression<RangeKeyClass> queryWithRangeKeyCondition =
                new DynamoDbQueryExpression<RangeKeyClass>()
                        .withHashKeyValues(hashKeyObject)
                        .withRangeKeyCondition("rangeKey", filterCondition);
        List<RangeKeyClass> rangeKeyConditionResult = mapper.query(RangeKeyClass.class, queryWithRangeKeyCondition);

        /*
         * (2) Apply the filter on the bigDecimalAttribute, in form of query filter
         */
        DynamoDbQueryExpression<RangeKeyClass> queryWithQueryFilterCondition =
                new DynamoDbQueryExpression<RangeKeyClass>()
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
        try {
            DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
            long hashKey = System.currentTimeMillis();
            RangeKeyClass keyObject = new RangeKeyClass();
            keyObject.setKey(hashKey);
            DynamoDbQueryExpression<RangeKeyClass> queryExpression = new DynamoDbQueryExpression<RangeKeyClass>()
                    .withHashKeyValues(keyObject);
            queryExpression.withRangeKeyCondition("rangeKey",
                    Condition.builder().comparisonOperator(ComparisonOperator.GT.toString())
                            .attributeValueList(
                                    AttributeValue.builder().n("1.0").build()).build()).withLimit(11)
                    .withIndexName("some_index");
            mapper.query(RangeKeyClass.class, queryExpression);
            fail("User should not provide index name when making query with the primary range key");
        } catch (IllegalArgumentException expected) {
            System.out.println(expected.getMessage());
        } catch (Exception e) {
            fail("Should trigger AmazonClientException.");
        }

    }
}
