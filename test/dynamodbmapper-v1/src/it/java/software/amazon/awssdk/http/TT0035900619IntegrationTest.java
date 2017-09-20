package software.amazon.awssdk.http;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class TT0035900619IntegrationTest {
    private static DynamoDBClient client;
    private static final long SLEEP_TIME_MILLIS = 5000;
    protected static String TABLE_NAME = "TT0035900619IntegrationTest-" + UUID.randomUUID();

    @BeforeClass
    public static void setup() throws InterruptedException {
        client = DynamoDBClient.builder().credentialsProvider(new StaticCredentialsProvider(awsTestCredentials())).build();
        List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(AttributeDefinition.builder().attributeName("hashKey").attributeType(ScalarAttributeType.S).build());
        List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(KeySchemaElement.builder().attributeName("hashKey").keyType(KeyType.HASH).build());

        client.createTable(CreateTableRequest.builder().attributeDefinitions(attributeDefinitions)
                .tableName(TABLE_NAME)
                .keySchema(keySchema)
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build()).build());
        waitForActiveTable(TABLE_NAME);
    }

    public static TableDescription waitForActiveTable(String tableName)
            throws InterruptedException {
        DescribeTableResponse result = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
        TableDescription desc = result.table();
        String status = desc.tableStatus();
        for (;; status = desc.tableStatus()) {
            if ("ACTIVE".equals(status)) {
                return desc;
            } else if ("CREATING".equals(status) || "UPDATING".equals(status)) {
                Thread.sleep(SLEEP_TIME_MILLIS);
                result = client.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
                desc = result.table();
            } else {
                throw new IllegalArgumentException("Table " + tableName
                        + " is not being created (with status=" + status + ")");
            }
        }
    }

    @AfterClass
    public static void bye() throws Exception {
        // Disable error injection or else the deletion would fail!
        AmazonHttpClient.configUnreliableTestConditions(null);
        client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
        client.close();
    }

    @Test
    public void testFakeRuntimeException_Once() {
        try {
            AmazonHttpClient.configUnreliableTestConditions(
                    new UnreliableTestConfig()
                    .withMaxNumErrors(1)
                    .withBytesReadBeforeException(10)
                    .withFakeIoException(false)
                    .withResetIntervalBeforeException(2)
            );
            System.out.println(client .describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build()));
            Assert.fail();
        } catch (RuntimeException expected) {
            expected.printStackTrace();
        }
    }

    @Test
    public void testFakeIOException_Once() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(1)
                .withBytesReadBeforeException(10)
                .withFakeIoException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build()));
    }

    @Test
    @ReviewBeforeRelease("Custom retry policy not yet implemented in code generator")
    @Ignore // TODO
    public void testFakeIOException_MaxRetries() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(PredefinedRetryPolicies.DYNAMODB_DEFAULT_MAX_ERROR_RETRY)
                .withBytesReadBeforeException(10)
                .withFakeIoException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build()));
    }

    @Test
    public void testFakeIOException_OneTooMany() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(PredefinedRetryPolicies.DYNAMODB_DEFAULT_MAX_ERROR_RETRY+1)
                .withBytesReadBeforeException(10)
                .withFakeIoException(true)
                .withResetIntervalBeforeException(2)
        );
        try {
            System.out.println(client.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build()));
            Assert.fail();
        } catch(AmazonClientException expected) {
            expected.printStackTrace();
        }
    }

    protected static AwsCredentials awsTestCredentials() {
        try {
            return AwsIntegrationTestBase.CREDENTIALS_PROVIDER_CHAIN.getCredentials();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
