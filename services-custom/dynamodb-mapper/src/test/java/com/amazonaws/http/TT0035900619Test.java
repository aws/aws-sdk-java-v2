package com.amazonaws.http;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import software.amazon.awssdk.mapper.dynamodb.LocalDynamoDBTestBase;
import software.amazon.awssdk.mapper.dynamodb.test.retry.RetryRule;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class TT0035900619Test extends LocalDynamoDBTestBase {
    private static final int RETRY_COUNT = 1;
    private static final long SLEEP_TIME_MILLIS = 5000;

    private static AmazonDynamoDB client;
    protected static String TABLE_NAME = "TT0035900619IntegrationTest-" + UUID.randomUUID();

    @Rule
    public RetryRule retry = new RetryRule(3);

    @BeforeClass
    public static void setup() throws InterruptedException {
        client = client(new ClientConfiguration().withMaxErrorRetry(RETRY_COUNT));
        List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition("hashKey", ScalarAttributeType.S));
        List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement("hashKey", KeyType.HASH));

        client.createTable(attributeDefinitions, 
            TABLE_NAME,
            keySchema, new ProvisionedThroughput(1L,
            1L));
        waitForActiveTable(TABLE_NAME);
    }

    public static TableDescription waitForActiveTable(String tableName)
            throws InterruptedException {
        DescribeTableResult result = client.describeTable(tableName);
        TableDescription desc = result.getTable();
        String status = desc.getTableStatus();
        for (;; status = desc.getTableStatus()) {
            if ("ACTIVE".equals(status)) {
                return desc;
            } else if ("CREATING".equals(status) || "UPDATING".equals(status)) {
                Thread.sleep(SLEEP_TIME_MILLIS);
                result = client.describeTable(tableName);
                desc = result.getTable();
            } else {
                throw new IllegalArgumentException("Table " + tableName
                        + " is not being created (with status=" + status + ")");
            }
        }
    }
    
    @AfterClass
    public static void bye() {
        // Disable error injection or else the deletion would fail!
        AmazonHttpClient.configUnreliableTestConditions(null);
        client.deleteTable(TABLE_NAME);
        client.shutdown();
    }

    @Test
    public void testFakeRuntimeException_Once() {
        try {
            AmazonHttpClient.configUnreliableTestConditions(
                    new UnreliableTestConfig()
                    .withMaxNumErrors(1)
                    .withBytesReadBeforeException(10)
                    .withFakeIOException(false)
                    .withResetIntervalBeforeException(2)
            );
            System.out.println(client .describeTable(TABLE_NAME));
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
                .withFakeIOException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(TABLE_NAME));
    }

    @Test
    public void testFakeIOException_MaxRetries() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(RETRY_COUNT)
                .withBytesReadBeforeException(10)
                .withFakeIOException(true)
                .withResetIntervalBeforeException(2)
        );
        System.out.println(client.describeTable(TABLE_NAME));
    }

    @Test
    public void testFakeIOException_OneTooMany() {
        AmazonHttpClient.configUnreliableTestConditions(
                new UnreliableTestConfig()
                .withMaxNumErrors(RETRY_COUNT + 1)
                .withBytesReadBeforeException(10)
                .withFakeIOException(true)
                .withResetIntervalBeforeException(2)
        );
        try {
            System.out.println(client.describeTable(TABLE_NAME));
            Assert.fail();
        } catch(AmazonClientException expected) {
            expected.printStackTrace();
        }
    }

    protected static AWSCredentials awsTestCredentials() {
        try {
            return new PropertiesCredentials(new File(
                    System.getProperty("user.home")
                            + "/.aws/awsTestAccount.properties"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
