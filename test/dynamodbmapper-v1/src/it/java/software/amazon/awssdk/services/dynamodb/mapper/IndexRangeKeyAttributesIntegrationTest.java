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

/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.ConsistentRead;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbQueryExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Tests that index range keys are properly handled as common attribute
 * when items are loaded, saved/updated by using primary key.
 * Also tests using index range keys for queries.
 */
public class IndexRangeKeyAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String RANGE_KEY = "rangeKey";
    private static final String INDEX_FOO_RANGE_KEY = "indexFooRangeKey";
    private static final String INDEX_BAR_RANGE_KEY = "indexBarRangeKey";
    private static final String MULTIPLE_INDEX_RANGE_KEY = "multipleIndexRangeKey";
    private static final String FOO_ATTRIBUTE = "fooAttribute";
    private static final String BAR_ATTRIBUTE = "barAttribute";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();
    private static final List<Long> hashKeyValues = new LinkedList<Long>();
    private static final int totalHash = 5;
    private static final int rangePerHash = 64;
    private static final int indexFooRangeStep = 2;
    private static final int indexBarRangeStep = 4;
    private static final int multipleIndexRangeStep = 8;
    private static DynamoDbMapper mapper;
    // We don't start with the current system millis like other tests because
    // it's out of the range of some data types
    private static int start = 1;

    // Test data
    static {
        for (int i = 0; i < totalHash; i++) {
            long hashKeyValue = startKey++;
            hashKeyValues.add(hashKeyValue);
            for (int j = 0; j < rangePerHash; j++) {
                Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
                attr.put(KEY_NAME, AttributeValue.builder().n("" + hashKeyValue).build());
                attr.put(RANGE_KEY, AttributeValue.builder().n("" + j).build());
                if (j % indexFooRangeStep == 0) {
                    attr.put(INDEX_FOO_RANGE_KEY, AttributeValue.builder().n("" + j).build());
                }
                if (j % indexBarRangeStep == 0) {
                    attr.put(INDEX_BAR_RANGE_KEY, AttributeValue.builder().n("" + j).build());
                }
                if (j % multipleIndexRangeStep == 0) {
                    attr.put(MULTIPLE_INDEX_RANGE_KEY, AttributeValue.builder().n("" + j).build());
                }
                attr.put(FOO_ATTRIBUTE, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
                attr.put(BAR_ATTRIBUTE, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
                attr.put(VERSION_ATTRIBUTE, AttributeValue.builder().n("1").build());

                attrs.add(attr);
            }
        }
    }

    ;

    @BeforeClass
    public static void setUp() throws Exception {
        boolean recreateTable = false;
        setUpTableWithIndexRangeAttribute(recreateTable);

        // Insert the data
        for (Map<String, AttributeValue> attr : attrs) {
            dynamo.putItem(PutItemRequest.builder().tableName(TABLE_WITH_INDEX_RANGE_ATTRIBUTE).item(attr).build());
        }

        mapper = new DynamoDbMapper(dynamo,
                                    new DynamoDbMapperConfig(ConsistentRead.CONSISTENT));
    }

    /**
     * Tests that attribute annotated with @DynamoDBIndexRangeKey is properly set in the loaded object.
     */
    @Test
    public void testLoad() throws Exception {
        for (Map<String, AttributeValue> attr : attrs) {
            IndexRangeKeyClass x = mapper.load(newIndexRangeKey(Long.parseLong(attr.get(KEY_NAME).n()),
                                                                Double.parseDouble(attr.get(RANGE_KEY).n())));

            // Convert all numbers to the most inclusive type for easy
            // comparison
            assertEquals(new BigDecimal(x.getKey()), new BigDecimal(attr.get(KEY_NAME).n()));
            assertEquals(new BigDecimal(x.getRangeKey()), new BigDecimal(attr.get(RANGE_KEY).n()));
            if (null == attr.get(INDEX_FOO_RANGE_KEY)) {
                assertNull(x.getIndexFooRangeKeyWithFakeName());
            } else {
                assertEquals(new BigDecimal(x.getIndexFooRangeKeyWithFakeName()),
                             new BigDecimal(attr.get(INDEX_FOO_RANGE_KEY).n()));
            }
            if (null == attr.get(INDEX_BAR_RANGE_KEY)) {
                assertNull(x.getIndexBarRangeKey());
            } else {
                assertEquals(new BigDecimal(x.getIndexBarRangeKey()), new BigDecimal(attr.get(INDEX_BAR_RANGE_KEY).n()));
            }
            assertEquals(new BigDecimal(x.getVersion()), new BigDecimal(attr.get(VERSION_ATTRIBUTE).n()));
            assertEquals(x.getFooAttribute(), attr.get(FOO_ATTRIBUTE).s());
            assertEquals(x.getBarAttribute(), attr.get(BAR_ATTRIBUTE).s());

        }
    }

    private IndexRangeKeyClass newIndexRangeKey(long hashKey, double rangeKey) {
        IndexRangeKeyClass obj = new IndexRangeKeyClass();
        obj.setKey(hashKey);
        obj.setRangeKey(rangeKey);
        return obj;
    }

    /**
     * Tests that attribute annotated with @DynamoDBIndexRangeKey is properly saved.
     */
    @Test
    public void testSave() throws Exception {
        List<IndexRangeKeyClass> objs = new ArrayList<IndexRangeKeyClass>();
        for (int i = 0; i < 5; i++) {
            IndexRangeKeyClass obj = getUniqueObject();
            objs.add(obj);
        }

        for (IndexRangeKeyClass obj : objs) {
            mapper.save(obj);
        }

        for (IndexRangeKeyClass obj : objs) {
            IndexRangeKeyClass loaded = mapper.load(IndexRangeKeyClass.class, obj.getKey(), obj.getRangeKey());
            assertEquals(obj, loaded);
        }
    }

    /**
     * Tests that version attribute is still working as expected.
     */
    @Test
    public void testUpdate() throws Exception {
        List<IndexRangeKeyClass> objs = new ArrayList<IndexRangeKeyClass>();
        for (int i = 0; i < 5; i++) {
            IndexRangeKeyClass obj = getUniqueObject();
            objs.add(obj);
        }

        for (IndexRangeKeyClass obj : objs) {
            mapper.save(obj);
        }

        for (IndexRangeKeyClass obj : objs) {
            IndexRangeKeyClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            replacement.setRangeKey(obj.getRangeKey());
            replacement.setVersion(obj.getVersion());
            mapper.save(replacement);

            IndexRangeKeyClass loadedObject = mapper.load(IndexRangeKeyClass.class, obj.getKey(), obj.getRangeKey());
            assertEquals(replacement, loadedObject);

            // If we try to update the old version, we should get an error
            replacement.setVersion(replacement.getVersion() - 1);
            try {
                mapper.save(replacement);
                fail("Should have thrown an exception");
            } catch (Exception expected) {
                // Ignored or expected.
            }
        }
    }

    /**
     * Tests making queries on local secondary index
     */
    @Test
    public void testQueryWithIndexRangekey() {
        int indexFooRangePerHash = rangePerHash / indexFooRangeStep;
        int indexBarRangePerHash = rangePerHash / indexBarRangeStep;
        for (long hashKeyValue : hashKeyValues) {
            IndexRangeKeyClass hashKeyItem = new IndexRangeKeyClass();
            hashKeyItem.setKey(hashKeyValue);

            /**
             * Query items by primary range key
             */
            List<IndexRangeKeyClass> result = mapper.query(IndexRangeKeyClass.class,
                                                           new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                                                   .withHashKeyValues(hashKeyItem)
                                                                   .withRangeKeyCondition(RANGE_KEY,
                                                                                          Condition.builder()
                                                                                                  .attributeValueList(
                                                                                                          AttributeValue.builder()
                                                                                                                  .n("0").build())
                                                                                                  .comparisonOperator(
                                                                                                          ComparisonOperator.GE
                                                                                                                  .toString()).build()));
            assertTrue(rangePerHash == result.size());
            // check that all attributes are retrieved
            for (IndexRangeKeyClass itemInFooIndex : result) {
                assertNotNull(itemInFooIndex.getFooAttribute());
                assertNotNull(itemInFooIndex.getBarAttribute());
            }

            /**
             * Query items on index_foo
             */
            result = mapper.query(IndexRangeKeyClass.class,
                                  new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                          .withHashKeyValues(hashKeyItem)
                                          .withRangeKeyCondition(INDEX_FOO_RANGE_KEY,
                                                                 Condition.builder()
                                                                         .attributeValueList(AttributeValue.builder().n("0").build())
                                                                         .comparisonOperator(
                                                                                 ComparisonOperator.GE.toString()).build()));
            assertTrue(indexFooRangePerHash == result.size());
            // check that only the projected attributes are retrieved
            for (IndexRangeKeyClass itemInFooIndex : result) {
                assertNotNull(itemInFooIndex.getFooAttribute());
                assertNull(itemInFooIndex.getBarAttribute());
            }

            /**
             * Query items on index_bar
             */
            result = mapper.query(IndexRangeKeyClass.class,
                                  new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                          .withHashKeyValues(hashKeyItem)
                                          .withRangeKeyCondition(INDEX_BAR_RANGE_KEY,
                                                                 Condition.builder()
                                                                         .attributeValueList(AttributeValue.builder().n("0").build())
                                                                         .comparisonOperator(
                                                                                 ComparisonOperator.GE.toString()).build()));
            assertTrue(indexBarRangePerHash == result.size());
            // check that only the projected attributes are retrieved
            for (IndexRangeKeyClass itemInBarIndex : result) {
                assertNull(itemInBarIndex.getFooAttribute());
                assertNotNull(itemInBarIndex.getBarAttribute());
            }
        }
    }

    /**
     * Tests the exception when user specifies an invalid range key name in the query.
     */
    @Test
    public void testInvalidRangeKeyNameException() {
        IndexRangeKeyClass hashKeyItem = new IndexRangeKeyClass();
        hashKeyItem.setKey(0);
        try {
            mapper.query(IndexRangeKeyClass.class,
                         new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                 .withHashKeyValues(hashKeyItem)
                                 .withRangeKeyCondition("some_range_key",
                                                        Condition.builder()
                                                                .attributeValueList(AttributeValue.builder().n("0").build())
                                                                .comparisonOperator(ComparisonOperator.GE.toString()).build()));
            fail("some_range_key is not a valid range key name.");
        } catch (DynamoDbMappingException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            fail("Should trigger an DynamoDBMappingException.");
        }
    }

    /**
     * Tests the exception when user specifies an invalid index name in the query.
     */
    @Test
    public void testInvalidIndexNameException() {
        IndexRangeKeyClass hashKeyItem = new IndexRangeKeyClass();
        hashKeyItem.setKey(0);
        try {
            mapper.query(IndexRangeKeyClass.class,
                         new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                 .withHashKeyValues(hashKeyItem)
                                 .withRangeKeyCondition(INDEX_BAR_RANGE_KEY,
                                                        Condition.builder()
                                                                .attributeValueList(AttributeValue.builder().n("0").build())
                                                                .comparisonOperator(ComparisonOperator.GE.toString()).build())
                                 .withIndexName("some_index"));
            fail("some_index is not a valid index name.");
        } catch (IllegalArgumentException iae) {
            System.out.println(iae.getMessage());
        } catch (Exception e) {
            fail("Should trigger an IllegalArgumentException.");
        }
    }

    /**
     * Tests making queries by using range key that is shared by multiple indexes.
     */
    @Test
    public void testQueryWithRangeKeyForMultipleIndexes() {
        int multipleIndexRangePerHash = rangePerHash / multipleIndexRangeStep;
        for (long hashKeyValue : hashKeyValues) {
            IndexRangeKeyClass hashKeyItem = new IndexRangeKeyClass();
            hashKeyItem.setKey(hashKeyValue);

            /**
             * Query items by a range key that is shared by multiple indexes
             */
            List<IndexRangeKeyClass> result = mapper.query(IndexRangeKeyClass.class,
                                                           new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                                                   .withHashKeyValues(hashKeyItem)
                                                                   .withRangeKeyCondition(MULTIPLE_INDEX_RANGE_KEY,
                                                                                          Condition.builder()
                                                                                                  .attributeValueList(
                                                                                                          AttributeValue.builder()
                                                                                                                  .n("0").build())
                                                                                                  .comparisonOperator(
                                                                                                          ComparisonOperator.GE
                                                                                                                  .toString()).build())
                                                                   .withIndexName("index_foo_copy"));
            assertTrue(multipleIndexRangePerHash == result.size());
            // check that only the projected attributes are retrieved
            for (IndexRangeKeyClass itemInFooIndex : result) {
                assertNotNull(itemInFooIndex.getFooAttribute());
                assertNull(itemInFooIndex.getBarAttribute());
            }
            result = mapper.query(IndexRangeKeyClass.class,
                                  new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                          .withHashKeyValues(hashKeyItem)
                                          .withRangeKeyCondition(MULTIPLE_INDEX_RANGE_KEY,
                                                                 Condition.builder()
                                                                         .attributeValueList(AttributeValue.builder().n("0").build())
                                                                         .comparisonOperator(
                                                                                 ComparisonOperator.GE.toString()).build())
                                          .withIndexName("index_bar_copy"));
            assertTrue(multipleIndexRangePerHash == result.size());
            // check that only the projected attributes are retrieved
            for (IndexRangeKeyClass itemInFooIndex : result) {
                assertNull(itemInFooIndex.getFooAttribute());
                assertNotNull(itemInFooIndex.getBarAttribute());
            }

            /**
             * Exception when user doesn't specify which index to use
             */
            try {
                mapper.query(IndexRangeKeyClass.class,
                             new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                     .withHashKeyValues(hashKeyItem)
                                     .withRangeKeyCondition(MULTIPLE_INDEX_RANGE_KEY,
                                                            Condition.builder()
                                                                    .attributeValueList(AttributeValue.builder().n("0").build())
                                                                    .comparisonOperator(ComparisonOperator.GE.toString()).build()));
                fail("No index name is specified when query with a range key shared by multiple indexes");
            } catch (IllegalArgumentException iae) {
                System.out.println(iae.getMessage());
            } catch (Exception e) {
                fail("Should trigger an IllegalArgumentException.");
            }

            /**
             * Exception when user uses an invalid index name
             */
            try {
                mapper.query(IndexRangeKeyClass.class,
                             new DynamoDbQueryExpression<IndexRangeKeyClass>()
                                     .withHashKeyValues(hashKeyItem)
                                     .withRangeKeyCondition(MULTIPLE_INDEX_RANGE_KEY,
                                                            Condition.builder()
                                                                    .attributeValueList(AttributeValue.builder().n("0").build())
                                                                    .comparisonOperator(ComparisonOperator.GE.toString()).build())
                                     .withIndexName("index_foo"));
                fail("index_foo is not annotated as part of the localSecondaryIndexNames in " +
                     "the @DynamoDBIndexRangeKey annotation of multipleIndexRangeKey");
            } catch (IllegalArgumentException iae) {
                System.out.println(iae.getMessage());
            } catch (Exception e) {
                fail("Should trigger an IllegalArgumentException.");
            }
        }

    }


    private IndexRangeKeyClass getUniqueObject() {
        IndexRangeKeyClass obj = new IndexRangeKeyClass();
        obj.setKey(startKey++);
        obj.setRangeKey((double) start++);
        obj.setIndexFooRangeKeyWithFakeName((double) start++);
        obj.setIndexBarRangeKey((double) start++);
        obj.setFooAttribute("" + startKey++);
        obj.setBarAttribute("" + startKey++);
        return obj;
    }

}
