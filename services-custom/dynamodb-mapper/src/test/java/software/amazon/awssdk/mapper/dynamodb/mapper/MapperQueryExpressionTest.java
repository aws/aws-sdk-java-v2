package software.amazon.awssdk.mapper.dynamodb.mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBRangeKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBIndexHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBIndexRangeKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBQueryExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;


/**
 * Unit test for the private method DynamoDBMapper#createQueryRequestFromExpression
 */
public class MapperQueryExpressionTest {

    private static final String TABLE_NAME = "table_name";
    private static final Condition RANGE_KEY_CONDITION = Condition.builder()
            .attributeValueList(AttributeValue.builder().s("some value").build())
            .comparisonOperator(ComparisonOperator.EQ)
            .build();

    private static DynamoDbClient mockDynamoDb;
    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setUp() {
        mockDynamoDb = mock(DynamoDbClient.class);
        when(mockDynamoDb.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(Collections.emptyList()).build());
        mapper = new DynamoDBMapper(mockDynamoDb);
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public final class HashOnlyClass {

        @DynamoDBHashKey
        @DynamoDBIndexHashKey (
                globalSecondaryIndexNames = "GSI-primary-hash"
        )
        private String primaryHashKey;

        @DynamoDBIndexHashKey (
                globalSecondaryIndexNames = {"GSI-index-hash-1", "GSI-index-hash-2"}
        )
        private String indexHashKey;

        @DynamoDBIndexHashKey (
                globalSecondaryIndexNames = {"GSI-another-index-hash"}
        )
        private String anotherIndexHashKey;

        public HashOnlyClass(String primaryHashKey, String indexHashKey, String anotherIndexHashKey) {
            this.primaryHashKey = primaryHashKey;
            this.indexHashKey = indexHashKey;
            this.anotherIndexHashKey = anotherIndexHashKey;
        }

        public String getPrimaryHashKey() {
            return primaryHashKey;
        }

        public void setPrimaryHashKey(String primaryHashKey) {
            this.primaryHashKey = primaryHashKey;
        }

        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        public String getAnotherIndexHashKey() {
            return anotherIndexHashKey;
        }

        public void setAnotherIndexHashKey(String anotherIndexHashKey) {
            this.anotherIndexHashKey = anotherIndexHashKey;
        }
    }

    /** Tests different scenarios of hash-only query **/
    @Test
    public void testHashConditionOnly() {
        // Primary hash only
        QueryRequest queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", null, null)));
        assertTrue(queryRequest.keyConditions().size() == 1);
        assertEquals("primaryHashKey", queryRequest.keyConditions().keySet().iterator().next());
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertNull(queryRequest.indexName());

        // Primary hash used for a GSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", null, null))
                        .withIndexName("GSI-primary-hash"));
        assertTrue(queryRequest.keyConditions().size() == 1);
        assertEquals("primaryHashKey", queryRequest.keyConditions().keySet().iterator().next());
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertEquals("GSI-primary-hash", queryRequest.indexName());

        // Primary hash query takes higher priority then index hash query
        queryRequest = testCreateQueryRequestFromExpression(
                    HashOnlyClass.class,
                    new DynamoDBQueryExpression<HashOnlyClass>()
                            .withHashKeyValues(new HashOnlyClass("foo", "bar", null)));
        assertTrue(queryRequest.keyConditions().size() == 1);
        assertEquals("primaryHashKey", queryRequest.keyConditions().keySet().iterator().next());
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertNull(queryRequest.indexName());

        // Ambiguous query on multiple index hash keys
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass(null, "bar", "charlie")),
                "Ambiguous query expression: More than one index hash key EQ conditions");

        // Ambiguous query when not specifying index name
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass(null, "bar", null)),
                "Ambiguous query expression: More than one GSIs");

        // Explicitly specify a GSI.
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", "bar", null))
                        .withIndexName("GSI-index-hash-1"));
        assertTrue(queryRequest.keyConditions().size() == 1);
        assertEquals("indexHashKey", queryRequest.keyConditions().keySet().iterator().next());
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("bar").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("indexHashKey"));
        assertEquals("GSI-index-hash-1", queryRequest.indexName());

        // Non-existent GSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass("foo", "bar", null))
                        .withIndexName("some fake gsi"),
                        "No hash key condition is applicable to the specified index");

        // No hash key condition specified
        queryRequest = testCreateQueryRequestFromExpression(
                HashOnlyClass.class,
                new DynamoDBQueryExpression<HashOnlyClass>()
                        .withHashKeyValues(new HashOnlyClass(null, null, null)),
                "Illegal query expression: No hash key condition is found in the query");
    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public final class HashRangeClass {
        private String primaryHashKey;
        private String indexHashKey;
        private String primaryRangeKey;
        private String indexRangeKey;
        private String anotherIndexRangeKey;

        public HashRangeClass(String primaryHashKey, String indexHashKey) {
            this.primaryHashKey = primaryHashKey;
            this.indexHashKey = indexHashKey;
        }

        @DynamoDBHashKey
        @DynamoDBIndexHashKey (
                globalSecondaryIndexNames = {
                        "GSI-primary-hash-index-range-1",
                        "GSI-primary-hash-index-range-2"}
        )
        public String getPrimaryHashKey() {
            return primaryHashKey;
        }

        public void setPrimaryHashKey(String primaryHashKey) {
            this.primaryHashKey = primaryHashKey;
        }

        @DynamoDBIndexHashKey (
                globalSecondaryIndexNames = {
                        "GSI-index-hash-primary-range",
                        "GSI-index-hash-index-range-1",
                        "GSI-index-hash-index-range-2"}
        )
        public String getIndexHashKey() {
            return indexHashKey;
        }

        public void setIndexHashKey(String indexHashKey) {
            this.indexHashKey = indexHashKey;
        }

        @DynamoDBRangeKey
        @DynamoDBIndexRangeKey (
                globalSecondaryIndexNames = {"GSI-index-hash-primary-range"},
                localSecondaryIndexName = "LSI-primary-range"
        )
        public String getPrimaryRangeKey() {
            return primaryRangeKey;
        }

        public void setPrimaryRangeKey(String primaryRangeKey) {
            this.primaryRangeKey = primaryRangeKey;
        }

        @DynamoDBIndexRangeKey (
                globalSecondaryIndexNames = {
                        "GSI-primary-hash-index-range-1",
                        "GSI-index-hash-index-range-1",
                        "GSI-index-hash-index-range-2"},
                localSecondaryIndexNames = {"LSI-index-range-1", "LSI-index-range-2"}
        )
        public String getIndexRangeKey() {
            return indexRangeKey;
        }

        public void setIndexRangeKey(String indexRangeKey) {
            this.indexRangeKey = indexRangeKey;
        }

        @DynamoDBIndexRangeKey (
                localSecondaryIndexName = "LSI-index-range-3",
                globalSecondaryIndexName = "GSI-primary-hash-index-range-2"
        )
        public String getAnotherIndexRangeKey() {
            return anotherIndexRangeKey;
        }

        public void setAnotherIndexRangeKey(String anotherIndexRangeKey) {
            this.anotherIndexRangeKey = anotherIndexRangeKey;
        }
    }

    /** Tests hash + range query **/
    @Test
    public void testHashAndRangeCondition() {
        // Primary hash + primary range
        QueryRequest queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("primaryRangeKey", RANGE_KEY_CONDITION));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("primaryRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.keyConditions().get("primaryRangeKey"));
        assertNull(queryRequest.indexName());

        // Primary hash + primary range on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("primaryRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("LSI-primary-range"));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("primaryRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.keyConditions().get("primaryRangeKey"));
        assertEquals("LSI-primary-range", queryRequest.indexName());

        // Primary hash + index range used by multiple LSI. But also a GSI hash + range
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("indexRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.keyConditions().get("indexRangeKey"));
        assertEquals("GSI-primary-hash-index-range-1", queryRequest.indexName());


        // Primary hash + index range on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("LSI-index-range-1"));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("primaryHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("indexRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.keyConditions().get("indexRangeKey"));
        assertEquals("LSI-index-range-1", queryRequest.indexName());

        // Non-existent LSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("some fake lsi"),
                "No range key condition is applicable to the specified index");

        // Illegal query: Primary hash + primary range on a GSI
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("GSI-index-hash-index-range-1"),
                "Illegal query expression: No hash key condition is applicable to the specified index");

        // GSI hash + GSI range
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass(null, "foo"))
                        .withRangeKeyCondition("primaryRangeKey", RANGE_KEY_CONDITION));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("indexHashKey"));
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("indexHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("primaryRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.keyConditions().get("primaryRangeKey"));
        assertEquals("GSI-index-hash-primary-range", queryRequest.indexName());

        // Ambiguous query: GSI hash + index range used by multiple GSIs
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass(null, "foo"))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION),
                "Illegal query expression: Cannot infer the index name from the query expression.");

        // Explicitly specify the GSI name
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass(null, "foo"))
                        .withRangeKeyCondition("indexRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("GSI-index-hash-index-range-2"));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("indexHashKey"));
        assertEquals(
                Condition.builder().attributeValueList(AttributeValue.builder().s("foo").build()).comparisonOperator(ComparisonOperator.EQ).build(),
                queryRequest.keyConditions().get("indexHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("indexRangeKey"));
        assertEquals(RANGE_KEY_CONDITION, queryRequest.keyConditions().get("indexRangeKey"));
        assertEquals("GSI-index-hash-index-range-2", queryRequest.indexName());

        // Ambiguous query: (1) primary hash + LSI range OR (2) GSI hash + range
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("anotherIndexRangeKey", RANGE_KEY_CONDITION),
                "Ambiguous query expression: Found multiple valid queries:");

        // Multiple range key conditions specified
        Map<String, Condition> multipleRangeKeyConditions = new HashMap<>();
        multipleRangeKeyConditions.put("primaryRangeKey", RANGE_KEY_CONDITION);
        multipleRangeKeyConditions.put("indexRangeKey", RANGE_KEY_CONDITION);
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyConditions(multipleRangeKeyConditions),
                "Illegal query expression: Conditions on multiple range keys");

        // Using an un-annotated range key
        queryRequest = testCreateQueryRequestFromExpression(
                HashRangeClass.class,
                new DynamoDBQueryExpression<HashRangeClass>()
                        .withHashKeyValues(new HashRangeClass("foo", null))
                        .withRangeKeyCondition("indexHashKey", RANGE_KEY_CONDITION),
                "not annotated with either @DynamoDBRangeKey or @DynamoDBIndexRangeKey.");
    }


    @DynamoDBTable(tableName = TABLE_NAME)
    public final class LSIRangeKeyClass {
        private String primaryHashKey;
        private String primaryRangeKey;
        private String lsiRangeKey;
        
        public LSIRangeKeyClass(String primaryHashKey, String primaryRangeKey) {
            this.primaryHashKey = primaryHashKey;
            this.primaryRangeKey = primaryRangeKey;
        }
        
        @DynamoDBHashKey
        public String getPrimaryHashKey() {
            return primaryHashKey;
        }
        public void setPrimaryHashKey(String primaryHashKey) {
            this.primaryHashKey = primaryHashKey;
        }
        
        @DynamoDBRangeKey
        public String getPrimaryRangeKey() {
            return primaryRangeKey;
        }
        public void setPrimaryRangeKey(String primaryRangeKey) {
            this.primaryRangeKey = primaryRangeKey;
        }
        
        @DynamoDBIndexRangeKey(localSecondaryIndexName = "LSI")
        public String getLsiRangeKey() {
            return lsiRangeKey;
        }
        public void setLsiRangeKey(String lsiRangeKey) {
            this.lsiRangeKey = lsiRangeKey;
        }
    }
    
    @Test
    public void testHashOnlyQueryOnHashRangeTable() {
        // Primary hash only query on a Hash+Range table
        QueryRequest queryRequest = testCreateQueryRequestFromExpression(
                LSIRangeKeyClass.class,
                new DynamoDBQueryExpression<LSIRangeKeyClass>()
                        .withHashKeyValues(new LSIRangeKeyClass("foo", null)));
        assertTrue(queryRequest.keyConditions().size() == 1);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertNull(queryRequest.indexName());
        
        // Hash+Range query on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                LSIRangeKeyClass.class,
                new DynamoDBQueryExpression<LSIRangeKeyClass>()
                        .withHashKeyValues(new LSIRangeKeyClass("foo", null))
                        .withRangeKeyCondition("lsiRangeKey", RANGE_KEY_CONDITION)
                        .withIndexName("LSI"));
        assertTrue(queryRequest.keyConditions().size() == 2);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertTrue(queryRequest.keyConditions().containsKey("lsiRangeKey"));
        assertEquals("LSI", queryRequest.indexName());
        
        // Hash-only query on a LSI
        queryRequest = testCreateQueryRequestFromExpression(
                LSIRangeKeyClass.class,
                new DynamoDBQueryExpression<LSIRangeKeyClass>()
                        .withHashKeyValues(new LSIRangeKeyClass("foo", null))
                        .withIndexName("LSI"));
        assertTrue(queryRequest.keyConditions().size() == 1);
        assertTrue(queryRequest.keyConditions().containsKey("primaryHashKey"));
        assertEquals("LSI", queryRequest.indexName());
    }

    private static <T> QueryRequest testCreateQueryRequestFromExpression(
            Class<T> clazz, DynamoDBQueryExpression<T> queryExpression) {
        return testCreateQueryRequestFromExpression(clazz, queryExpression, null);
    }

    private static <T> QueryRequest testCreateQueryRequestFromExpression(
            Class<T> clazz, DynamoDBQueryExpression<T> queryExpression,
            String expectedErrorMessage) {
        try {
            mapper.queryPage(clazz, queryExpression, DynamoDBMapperConfig.DEFAULT);
            if (expectedErrorMessage != null) {
                fail("Exception containing messsage ("
                        + expectedErrorMessage + ") is expected.");
            }
            ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
            verify(mockDynamoDb, atLeastOnce()).query(captor.capture());
            return captor.getValue();
        } catch (RuntimeException e) {
            if (expectedErrorMessage != null && e.getMessage() != null) {
                assertTrue("Exception message [" + e.getMessage() + "] does not contain " +
                        "the expected message [" + expectedErrorMessage + "].",
                        e.getMessage().contains(expectedErrorMessage));
            } else {
                e.printStackTrace();
                fail("Internal error when calling createQueryRequestFromExpressio method");
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }

}
