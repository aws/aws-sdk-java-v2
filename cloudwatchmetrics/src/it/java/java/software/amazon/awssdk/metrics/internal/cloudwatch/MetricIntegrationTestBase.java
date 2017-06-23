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
package software.amazon.awssdk.metrics.internal.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.Dimensions;
import software.amazon.awssdk.services.cloudwatch.AmazonCloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResult;
import software.amazon.awssdk.services.dynamodb.AmazonDynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResult;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResult;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import software.amazon.awssdk.test.AWSTestBase;
import software.amazon.awssdk.util.AWSRequestMetrics.Field;

/**
 * This class provides helper methods and performs the initial setup for SDK
 * Metrics integration testing.
 *
 */
public class MetricIntegrationTestBase extends AWSTestBase {

    private static final String CLOUDWATCH_ENDPOINT = "monitoring.us-west-2.amazonaws.com";

    /** Client Object Reference for Amazon DynamoDB. */
    protected static AmazonDynamoDBClient dynamo;

    /** Client Object Reference for Amazon CloudWatch. */
    protected static AmazonCloudWatchClient cloudWatch;

    /** Object Reference for Metrics configuration used in testing. */
    protected static CloudWatchMetricConfig metricConfig;

    protected static MetricCollector metricCollection;

    /** Read capacity for the test table being created in Amazon DynamoDB. */
    protected static final Long READ_CAPACITY = 10L;

    /** Write capacity for the test table being created in Amazon DynamoDB. */
    protected static final Long WRITE_CAPACITY = 5L;

    /** End point where the test table is being created. */
    protected static final String ENDPOINT = "http://dynamodb.sa-east-1.amazonaws.com";

    /** Provisioned Throughput for the test table created in Amazon DynamoDB */
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT = ProvisionedThroughput.builder()
            .readCapacityUnits(READ_CAPACITY)
            .writeCapacityUnits(WRITE_CAPACITY)
            .build();

    /** Name of the table created in Amazon DynamoDB for testing. */
    protected static final String tableName = "integ-test-table-metric-"
            + new Date().getTime();

    /** Name of the Field in the table created in Amazon DynamoDB for testing. */
    protected static final String dynamoDBFieldName = "integ-test-field-metric-"
            + new Date().getTime();

    private static final int ONE_HOUR_IN_MILLISECONDS = 1000 * 60 * 60;

    /**
     * Namespace used in Amazon CloudWatch under which all metric data is
     * uploaded.
     */
    protected static final String METRIC_NAME = "AmazonDynamoDBv2";

    private static final boolean TEST_DEFAULT_MC = false;

    /**
     * Initializes the objects that are required for testing. Reads the
     * credentials required to connect to AWS from the file. Initializes the
     * client objects used for connecting to Amazon DynamoDB and Amazon
     * CloudWatch. Creates the tables in Amazon DynamoDB required for testing.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();

        if (AwsSdkMetrics.isDefaultMetricsEnabled()) {
            // -Dcom.amazonaws.sdk.enableDefaultMetrics specified during JVM start up
            System.out.println("Default MetricCollector enabled via system properties");
            MetricCollector c = AwsSdkMetrics.getMetricCollector();
            MetricCollectorSupport col = (MetricCollectorSupport)c;
            metricConfig = col.getConfig();
            cloudWatch = col.getCloudwatchClient();
        } else if (TEST_DEFAULT_MC) {
            System.out.println("Default MetricCollector enabled programmatically");
            assertTrue(AwsSdkMetrics.enableDefaultMetrics());
            MetricCollector c = AwsSdkMetrics.getMetricCollector();
            MetricCollectorSupport col = (MetricCollectorSupport)c;
            metricConfig = col.getConfig();
            cloudWatch = col.getCloudwatchClient();
        } else {
            System.out.println("Explicit Custom MetricCollector specified programmatically");
            MetricCollector defaultMC =  AwsSdkMetrics.getMetricCollector();
            assertFalse(defaultMC.isEnabled());
            // explicitly specified a metric collector
            metricConfig = new CloudWatchMetricConfig()
                .withCredentialsProvider(new StaticCredentialsProvider(credentials))
                .withCloudWatchEndPoint(CLOUDWATCH_ENDPOINT)
                ;
            MetricCollector mc = new CustomMetricCollector(metricConfig);
            mc.start();
            AwsSdkMetrics.setMetricCollector(mc);
            assertTrue(mc.isEnabled());
            assertSame(mc, AwsSdkMetrics.getMetricCollector());
            cloudWatch = new AmazonCloudWatchClient(credentials);
            cloudWatch.setEndpoint(CLOUDWATCH_ENDPOINT);
        }

        dynamo = new AmazonDynamoDBClient(credentials);

        deleteAllTables();

        if (doesTableExist(tableName) == false) {
            createTestTable(DEFAULT_PROVISIONED_THROUGHPUT);
        }

        TableUtils.waitUntilActive(dynamo, tableName);
    }

    /**
     * Helper method to check if the table exists in Amazon DynamoDB
     */
    protected static boolean doesTableExist(String tableName) {
        try {
            TableDescription table = dynamo.describeTable(DescribeTableRequest.builder().tableName(tableName).build())
                    .getTable();
            return "ACTIVE".equals(table.getTableStatus());
        } catch (AmazonServiceException ase) {
            if (ase.getErrorCode().equals("ResourceNotFoundException"))
                return false;
            throw ase;
        }
    }

    /**
     * Helper method to create a table in Amazon DynamoDB
     */
    protected static void createTestTable(
            ProvisionedThroughput provisionedThroughput) {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(KeySchemaElement.builder().attributeName(
                        dynamoDBFieldName).keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName(
                        dynamoDBFieldName).attributeType(
                        ScalarAttributeType.S).build())
                .provisionedThroughput(provisionedThroughput)
                .build();

        TableDescription createdTableDescription = dynamo.createTable(
                createTableRequest).getTableDescription();
        System.out.println("Created Table: " + createdTableDescription);
        assertEquals(tableName, createdTableDescription.tableName());
        assertNotNull(createdTableDescription.tableStatus());
        assertEquals(dynamoDBFieldName, createdTableDescription.keySchema()
                .get(0).attributeName());
        assertEquals(KeyType.HASH.toString(), createdTableDescription
                .keySchema().get(0).keyType());
    }

    protected static void deleteAllTables() {
        if (true)
            return;
        ListTablesResult res = dynamo.listTables();
        for (String name: res.tableNames()) {
            DeleteTableRequest req = new DeleteTableRequest(name);
            System.err.println("Deleting table " + name);
            DeleteTableResult dr = dynamo.deleteTable(req);
            System.err.println(dr);
        }
    }

    /**
     * Helper method that helps the thread to be in wait until the data points
     * is available in Amazon CloudWatch.
     */
    protected static List<Datapoint> waitForDatapointsToBecomeAvailable() {
        System.out.println("Waiting for datapoints to become available...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        List<String> statistics = new ArrayList<String>();
        statistics.add("Sum");

        Collection<Dimension> dims = new ArrayList<Dimension>();
        // Looking specifically for the HttpRequestTime for a PutItemRequest
        dims.add(Dimension.builder()
                .name(Dimensions.MetricType.name())
                .value(Field.HttpRequestTime.name()).build());
        dims.add(Dimension.builder()
                .name(Dimensions.RequestType.name())
                .value(PutItemRequest.class.getSimpleName()).build());

        GetMetricStatisticsRequest getMetricStatisticsRequest = GetMetricStatisticsRequest.builder()
                .namespace(AwsSdkMetrics.getMetricNameSpace())
                .statistics(statistics)
                .metricName(METRIC_NAME)
                .dimensions(dims)
                .period(60)
                .build();

        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(1000 * 20);
            } catch (Exception e) {
            }
            try {

                getMetricStatisticsRequest = getMetricStatisticsRequest.toBuilder()
                        .startTime(new Date(new Date().getTime() - ONE_HOUR_IN_MILLISECONDS))
                        .endTime(new Date()).build();
                GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(getMetricStatisticsRequest);

                List<Datapoint> datapoints = result.datapoints();
                System.err.println("datapoints.size(): " + datapoints.size());
                if (datapoints.size() > 0) {
                    return datapoints;
                }

            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase(
                        "ResourceNotFoundException") == false)
                    throw ase;
            }
        }

        throw new RuntimeException(
                "Datapoints not available in Amazon CloudWatch.");
    }

}
