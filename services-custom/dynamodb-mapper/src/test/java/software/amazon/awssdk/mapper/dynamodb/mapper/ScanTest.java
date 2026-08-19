package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import software.amazon.awssdk.mapper.dynamodb.LocalDynamoDBTestBase;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBScanExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.PaginatedParallelScanList;
import software.amazon.awssdk.mapper.dynamodb.PaginatedScanList;
import software.amazon.awssdk.mapper.dynamodb.ScanResultPage;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalOperator;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
/**
 * Integration tests for the scan operation on DynamoDBMapper.
 */
public class ScanTest extends LocalDynamoDBTestBase {

    private static final String TABLE_NAME = "aws-java-sdk-util-scan";
    /**
     * We set a small limit in order to test the behavior of PaginatedList
     * when it could not load all the scan result in one batch.
     */
    private static final int SCAN_LIMIT = 10;
    private static final int PARALLEL_SCAN_SEGMENTS = 3;
    private static DynamoDbClient dynamo;

    private static void createTestData() throws Exception {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);
        for (int i = 0; i < 500; i++) {
            util.save(new SimpleClass(Integer.toString(i), Integer.toString(i)));
        }
    }

    @BeforeClass
    public static void setUpTestData() throws Exception {
        dynamo = client();
        String keyName = "id";
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName(keyName).keyType(KeyType.HASH).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(keyName)
                                .attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(10L)
                        .writeCapacityUnits(5L).build())
                .build();

        if (createTableIfNotExists(createTableRequest)) {
            dynamo.waiter().waitUntilTableExists(b -> b.tableName(TABLE_NAME));
        }

        createTestData();
    }

    private static boolean createTableIfNotExists(CreateTableRequest createTableRequest) {
        try {
            dynamo.createTable(createTableRequest);
            return true;
        } catch (ResourceInUseException e) {
            // Table already exists.
            return false;
        }
    }


    @Test
    public void testScan() throws Exception {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression().withLimit(SCAN_LIMIT);
        scanExpression.addFilterCondition("value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
        scanExpression.addFilterCondition("extraData", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
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
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);

        Map<String, Condition> scanFilter = new HashMap<>();
        scanFilter.put("value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
        scanFilter.put("non-existent-field", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withLimit(SCAN_LIMIT)
            .withScanFilter(scanFilter)
            .withConditionalOperator(ConditionalOperator.AND);

        List<SimpleClass> andConditionResult = mapper.scan(SimpleClass.class, scanExpression);
        assertTrue(andConditionResult.isEmpty());

        List<SimpleClass> orConditionResult = mapper.scan(SimpleClass.class,
                scanExpression.withConditionalOperator(ConditionalOperator.OR));
        assertFalse(orConditionResult.isEmpty());
    }

    @Test
    public void testParallelScan() throws Exception {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression().withLimit(SCAN_LIMIT);
        scanExpression.addFilterCondition("value", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
        scanExpression.addFilterCondition("extraData", Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());

        PaginatedParallelScanList<SimpleClass> parallelScanList = util.parallelScan(SimpleClass.class, scanExpression, PARALLEL_SCAN_SEGMENTS);
        int count = 0;
        Iterator<SimpleClass> iterator = parallelScanList.iterator();
        HashMap<String, Boolean> allDataAppearance = new HashMap<String, Boolean>();
        for (int i=0;i<500;i++) {
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
    public void testScanPage() throws Exception {
        DynamoDBMapper util = new DynamoDBMapper(dynamo);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.addFilterCondition("value",
                Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
        scanExpression.addFilterCondition("extraData",
                Condition.builder().comparisonOperator(ComparisonOperator.NOT_NULL).build());
        int limit = 3;
        scanExpression.setLimit(limit);
        ScanResultPage<SimpleClass> result = util.scanPage(SimpleClass.class, scanExpression);

        int count = 0;
        Iterator<SimpleClass> iterator = result.getResults().iterator();
        Set<SimpleClass> seen = new HashSet<ScanTest.SimpleClass>();
        while ( iterator.hasNext() ) {
            count++;
            SimpleClass next = iterator.next();
            assertNotNull(next.getExtraData());
            assertNotNull(next.getValue());
            assertTrue(seen.add(next));
        }

        assertEquals(limit, count);
        assertEquals(count, result.getResults().toArray().length);

        scanExpression.setExclusiveStartKey(result.getLastEvaluatedKey());
        result = util.scanPage(SimpleClass.class, scanExpression);

        iterator = result.getResults().iterator();
        count = 0;
        while ( iterator.hasNext() ) {
            count++;
            SimpleClass next = iterator.next();
            assertNotNull(next.getExtraData());
            assertNotNull(next.getValue());
            assertTrue(seen.add(next));
        }

        assertEquals(limit, count);
        assertEquals(count, result.getResults().toArray().length);

    }
    @DynamoDBTable(tableName = "aws-java-sdk-util-scan")
    public static final class SimpleClass {
        private String id;
        private String value;
        private String extraData;


        public SimpleClass() {}

        public SimpleClass(String id, String value) {
            this.id = id;
            this.value = value;
            this.extraData = UUID.randomUUID().toString();
        }

        @DynamoDBHashKey
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
