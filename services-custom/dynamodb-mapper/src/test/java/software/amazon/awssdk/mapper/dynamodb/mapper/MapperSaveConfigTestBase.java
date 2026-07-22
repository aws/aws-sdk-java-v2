package software.amazon.awssdk.mapper.dynamodb.mapper;

import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.ConsistentReads.CONSISTENT;
import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior.APPEND_SET;
import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior.CLOBBER;
import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior.PUT;
import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior.UPDATE;
import static software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import software.amazon.awssdk.mapper.dynamodb.test.util.DynamoDBIntegrationTestBase;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBAttribute;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBRangeKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBVersionAttribute;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import software.amazon.awssdk.mapper.dynamodb.pojos.TestItem;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class MapperSaveConfigTestBase extends DynamoDBIntegrationTestBase {

    protected static DynamoDBMapper dynamoMapper;

    protected static final DynamoDBMapperConfig defaultConfig =
            DynamoDBMapperConfig.builder()
                                .withSaveBehavior(UPDATE)
                                .withConsistentReads(CONSISTENT)
                                .build();
    protected static final DynamoDBMapperConfig updateSkipNullConfig =
            DynamoDBMapperConfig.builder()
                                .withSaveBehavior(UPDATE_SKIP_NULL_ATTRIBUTES)
                                .withConsistentReads(CONSISTENT)
                                .build();
    protected static final DynamoDBMapperConfig appendSetConfig =
            DynamoDBMapperConfig.builder()
                                .withSaveBehavior(APPEND_SET)
                                .withConsistentReads(CONSISTENT)
                                .build();
    protected static final DynamoDBMapperConfig clobberConfig =
            DynamoDBMapperConfig.builder()
                                .withSaveBehavior(CLOBBER)
                                .withConsistentReads(CONSISTENT)
                                .build();
    protected static final DynamoDBMapperConfig putConfig =
            DynamoDBMapperConfig.builder()
                                .withSaveBehavior(PUT)
                                .withConsistentReads(CONSISTENT)
                                .build();

    protected static final String tableName = "aws-java-sdk-dynamodb-mapper-save-config-test";

    protected static final String hashKeyName = "hashKey";

    protected static final String rangeKeyName = "rangeKey";

    protected static final String nonKeyAttributeName = "nonKeyAttribute";

    protected static final String stringSetAttributeName = "stringSetAttribute";

    protected static final String versionAttributeName = "version";

    /** Read capacity for the test table being created in Amazon DynamoDB. */
    protected static final Long READ_CAPACITY = 10L;

    /** Write capacity for the test table being created in Amazon DynamoDB. */
    protected static final Long WRITE_CAPACITY = 5L;

    /** Provisioned Throughput for the test table created in Amazon DynamoDB */
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT = new ProvisionedThroughput()
            .withReadCapacityUnits(READ_CAPACITY).withWriteCapacityUnits(
                    WRITE_CAPACITY);

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        dynamo = new AmazonDynamoDBClient(credentials);
        dynamoMapper = new DynamoDBMapper(dynamo);

        createTestTable(DEFAULT_PROVISIONED_THROUGHPUT);
        TableUtils.waitUntilActive(dynamo, tableName);
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(tableName);
    }

    @DynamoDBTable(tableName = tableName)
    public static class TestItemWithVersion extends TestItem {

        @DynamoDBVersionAttribute
        private Long version;

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }

    @DynamoDBTable(tableName = tableName)
    static public class TestAppendToScalarItem {

        private String hashKey;
        private Long rangeKey;
        private Set<String> fakeStringSetAttribute;

        @DynamoDBHashKey(attributeName = hashKeyName)
        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDBRangeKey(attributeName = rangeKeyName)
        public Long getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(Long rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDBAttribute(attributeName = nonKeyAttributeName)
        public Set<String> getFakeStringSetAttribute() {
            return fakeStringSetAttribute;
        }

        public void setFakeStringSetAttribute(Set<String> stringSetAttribute) {
            this.fakeStringSetAttribute = stringSetAttribute;
        }
    }

    /**
     * Helper method to create a table in Amazon DynamoDB
     */
    protected static void createTestTable(
            ProvisionedThroughput provisionedThroughput) {
        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(
                                hashKeyName).withKeyType(
                                KeyType.HASH))
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(
                                rangeKeyName).withKeyType(
                                KeyType.RANGE))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(
                                hashKeyName).withAttributeType(
                                ScalarAttributeType.S))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(
                                rangeKeyName).withAttributeType(
                                ScalarAttributeType.N));
        createTableRequest.setProvisionedThroughput(provisionedThroughput);

        TableDescription createdTableDescription = dynamo.createTable(
                createTableRequest).getTableDescription();
        System.out.println("Created Table: " + createdTableDescription);
        assertEquals(tableName, createdTableDescription.getTableName());
        assertNotNull(createdTableDescription.getTableStatus());
        assertEquals(hashKeyName, createdTableDescription
                .getKeySchema().get(0).getAttributeName());
        assertEquals(KeyType.HASH.toString(), createdTableDescription
                .getKeySchema().get(0).getKeyType());
        assertEquals(rangeKeyName, createdTableDescription
                .getKeySchema().get(1).getAttributeName());
        assertEquals(KeyType.RANGE.toString(), createdTableDescription
                .getKeySchema().get(1).getKeyType());
    }
}
