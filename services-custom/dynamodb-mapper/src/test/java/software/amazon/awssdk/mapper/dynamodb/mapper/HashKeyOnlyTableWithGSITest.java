package software.amazon.awssdk.mapper.dynamodb.mapper;

import static org.junit.Assert.assertEquals;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.LocalDynamoDBTestBase;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBIndexHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBIndexRangeKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBQueryExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.PaginatedQueryList;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for GSI support with a table that has no primary range key (only a primary hash key).
 */
public class HashKeyOnlyTableWithGSITest extends LocalDynamoDBTestBase {

    public static final String HASH_KEY_ONLY_TABLE_NAME = "no-primary-range-key-gsi-test";
    private static DynamoDbClient dynamo;


    @BeforeClass
    public static void setUp() throws Exception {
        dynamo = client();
        CreateTableRequest req = CreateTableRequest.builder()
                .tableName(HASH_KEY_ONLY_TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                                                            .readCapacityUnits(10L).writeCapacityUnits(10L).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("status").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("ts").attributeType(ScalarAttributeType.S).build())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                                                            .readCapacityUnits(10L).writeCapacityUnits(10L).build())
                                .indexName("statusAndCreation")
                                .keySchema(
                                        KeySchemaElement.builder().attributeName("status").keyType(KeyType.HASH).build(),
                                        KeySchemaElement.builder().attributeName("ts").keyType(KeyType.RANGE).build())
                                .projection(
                                        Projection.builder().projectionType(ProjectionType.ALL).build())
                                .build())
                .build();

        dynamo.createTable(req);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(HASH_KEY_ONLY_TABLE_NAME).build());
    }

    @DynamoDBTable(tableName = HASH_KEY_ONLY_TABLE_NAME)
    public static class User {
        private String id;
        private String status;
        private String ts;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBIndexHashKey(globalSecondaryIndexName = "statusAndCreation")
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @DynamoDBIndexRangeKey(globalSecondaryIndexName = "statusAndCreation")
        public String getTs() {
            return ts;
        }

        public void setTs(String ts) {
            this.ts = ts;
        }
    }


    /** Tests that we can query using the hash/range GSI on our hash-key only table. */
    @Test
    public void testGSIQuery() throws Exception {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        String status = "foo-status";

        User user = new User();
        user.setId("123");
        user.setStatus(status);
        user.setTs("321");
        mapper.save(user);

        PaginatedQueryList<User> queryResult;
        long endTime = System.currentTimeMillis() + 1000 * 60;
        do {
            DynamoDBQueryExpression<User> expr = new DynamoDBQueryExpression<User>()
                    .withIndexName("statusAndCreation")
                    .withLimit(100)
                    .withConsistentRead(false)
                    .withHashKeyValues(user)
                    .withRangeKeyCondition("ts",
                                        Condition.builder()
                                                .comparisonOperator(ComparisonOperator.GT)
                                                .attributeValueList(AttributeValue.builder().s("100").build())
                                                .build());

            queryResult = mapper.query(User.class, expr);
        } while (queryResult.size() == 0 && System.currentTimeMillis() < endTime);

        assertEquals(1, queryResult.size());
        assertEquals(status, queryResult.get(0).getStatus());
    }

}
