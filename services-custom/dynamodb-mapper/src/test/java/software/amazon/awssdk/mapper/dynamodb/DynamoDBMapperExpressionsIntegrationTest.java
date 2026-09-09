/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.mapper.dynamodb.test.AWSTestBase;
import software.amazon.awssdk.mapper.dynamodb.test.util.DynamoDBTestBase;

public class DynamoDBMapperExpressionsIntegrationTest extends AWSTestBase {

    /** Table name to be used for this testing */
    static final String TABLENAME = "java-sdk-mapper-customer";

    /** Attribute name of the hash key */
    private static final String HASH_KEY = "customerId";

    /** Attribute name of the range key */
    private static final String RANGE_KEY = "addressType";

    /** Status of the table */
    private static final String TABLE_STATUS_ACTIVE = "ACTIVE";

    /** Sleep time in milli seconds for the table to become active. */
    private static final long SLEEP_TIME_IN_MILLIS = 5000;

    /** Provisioned Throughput read capacity for the table. */
    private static final long READ_CAPACITY = 10;

    /** Provisioned Throughput write capacity for the table. */
    private static final long WRITE_CAPACITY = 10;

    private static final String FIRST_CUSTOMER_ID = "1000";
    private static final String ADDRESS_TYPE_HOME = "home";
    private static final String ADDRESS_TYPE_WORK = "work";

    /** Reference to the mapper used for this testing */
    protected static DynamoDBMapper mapper;

    /** Reference to the client being used by the mapper. */
    protected static DynamoDbClient client;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException,
            InterruptedException {
        client = DynamoDBTestBase.getClient();
        mapper = new DynamoDBMapper(client);
        try {
            client.createTable(CreateTableRequest.builder()
                .tableName(TABLENAME)
                .keySchema(KeySchemaElement.builder().attributeName(HASH_KEY).keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(RANGE_KEY).keyType(KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(HASH_KEY).attributeType(ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(RANGE_KEY)
                                .attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(
                        ProvisionedThroughput.builder().readCapacityUnits(READ_CAPACITY).writeCapacityUnits(WRITE_CAPACITY).build())
                .build());
        } catch(ResourceInUseException ex) {
            ex.printStackTrace();
        }
        waitForTableCreation();
        fillInData();
    }

    public static void fillInData() {
        final Map<String, AttributeValue> record1 = new HashMap<String, AttributeValue>();
        record1.put(HASH_KEY, AttributeValue.builder().n(FIRST_CUSTOMER_ID).build());
        record1.put(RANGE_KEY, AttributeValue.builder().s(ADDRESS_TYPE_WORK).build());
        record1.put("AddressLine1",
                        AttributeValue.builder().s("1918 8th Aven").build());
        record1.put("city", AttributeValue.builder().s("seattle").build());
        record1.put("state", AttributeValue.builder().s("WA").build());
        record1.put("zipcode", AttributeValue.builder().n("98104").build());
        final Map<String, AttributeValue> record2 = new HashMap<String, AttributeValue>();
        record2.put(HASH_KEY, AttributeValue.builder().n(FIRST_CUSTOMER_ID).build());
        record2.put(RANGE_KEY, AttributeValue.builder().s(ADDRESS_TYPE_HOME).build());
        record2.put("AddressLine1",
                        AttributeValue.builder().s("15606 NE 40th ST").build());
        record2.put("city", AttributeValue.builder().s("redmond").build());
        record2.put("state", AttributeValue.builder().s("WA").build());
        record2.put("zipcode", AttributeValue.builder().n("98052").build());

        client.putItem(PutItemRequest.builder().tableName(TABLENAME).item(record1).build());
        client.putItem(PutItemRequest.builder().tableName(TABLENAME).item(record2).build());
    }

    public static void waitForTableCreation() throws InterruptedException {
        while (true) {
            DescribeTableResponse describeResult = client
                    .describeTable(b -> b.tableName(TABLENAME));
            if (TABLE_STATUS_ACTIVE.equals(describeResult.table()
                    .tableStatusAsString())) {
                break;
            }
            Thread.sleep(SLEEP_TIME_IN_MILLIS);
        }
    }

    /**
     * Queries for a record based on hash and range key. Provider a filter
     * expression that filters results.
     */
    @Test
    public void testQueryFilterExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));

        DynamoDBQueryExpression<Customer> queryExpression =
            new DynamoDBQueryExpression<Customer>()
            .withHashKeyValues(customer)
            .withRangeKeyCondition(RANGE_KEY, Condition.builder()
                .comparisonOperator(ComparisonOperator.EQ)
                .attributeValueList(AttributeValue.builder().s(ADDRESS_TYPE_HOME).build()).build())
            ;
        PaginatedQueryList<Customer> results = mapper.query(Customer.class,
                queryExpression);
        assertTrue(results.size() == 1);

        final Map<String, AttributeValue> builder = new HashMap<String, AttributeValue>();
        builder.put(":zipcode", AttributeValue.builder().n("98109").build());

        queryExpression = queryExpression
            .withFilterExpression("zipcode = :zipcode")
            .withExpressionAttributeValues(builder);
        results = mapper.query(Customer.class, queryExpression);
        assertTrue(results.size() == 0);
    }

    /**
     * Queries using key condition expression.
     */
    @Test
    public void testKeyConditionExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));

        DynamoDBQueryExpression<Customer> qxp =
            new DynamoDBQueryExpression<Customer>()
                .withKeyConditionExpression(
                    "customerId = :customerId AND addressType = :addressType");
        final Map<String, AttributeValue> builder =
                new HashMap<String, AttributeValue>();
        builder.put(":customerId", AttributeValue.builder().n(FIRST_CUSTOMER_ID).build());
        builder.put(":addressType", AttributeValue.builder().s(ADDRESS_TYPE_HOME).build());
        qxp.withExpressionAttributeValues(builder);

        PaginatedQueryList<Customer> results = mapper.query(Customer.class, qxp);
        assertTrue(results.size() == 1);

        builder.put(":zipcode", AttributeValue.builder().n("98109").build());
        qxp.withFilterExpression("zipcode = :zipcode")
           .withExpressionAttributeValues(builder)
           ;

        results = mapper.query(Customer.class, qxp);
        assertTrue(results.size() == 0);
    }

    /**
     * Scan the table and filters the results based on the filter expression
     * provided.
     */
    @Test
    public void testScanFilterExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

        PaginatedScanList<Customer> results = mapper.scan(Customer.class,
                scanExpression);
        assertTrue(results.size() == 2);

        final Map<String, AttributeValue> attributeValueMapBuilder = new HashMap<String, AttributeValue>();
        attributeValueMapBuilder
                .put(":state", AttributeValue.builder().s("WA").build());

        final Map<String, String> attributeNameMapBuilder = new HashMap<String, String>();
        attributeNameMapBuilder.put("#statename", "state");

        scanExpression = scanExpression
                .withFilterExpression("#statename = :state")
                .withExpressionAttributeValues(attributeValueMapBuilder)
                .withExpressionAttributeNames(attributeNameMapBuilder);
        results = mapper.scan(Customer.class, scanExpression);
        assertTrue(results.size() == 2);
    }

    /**
     * Performs delete operation with a condition expression specified. Delete
     * should fail as the condition in the conditional expression evaluates to
     * false.
     */
    @Test
    public void testDeleteConditionalExpression() {
        Customer customer = new Customer();
        customer.setCustomerId(Long.valueOf(FIRST_CUSTOMER_ID));
        customer.setAddressType(ADDRESS_TYPE_WORK);

        Map<String, ExpectedAttributeValue> expectedMapBuilder = new HashMap<String, ExpectedAttributeValue>();
        expectedMapBuilder.put("zipcode", ExpectedAttributeValue.builder()
                .attributeValueList(AttributeValue.builder().n("98052").build())
                .comparisonOperator(ComparisonOperator.EQ).build());

        DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
        deleteExpression.setConditionExpression("zipcode = :zipcode");

        final Map<String, AttributeValue> attributeValueMapBuilder = new HashMap<String, AttributeValue>();
        attributeValueMapBuilder.put(":zipcode",
                AttributeValue.builder().n("98052").build());
        deleteExpression.setExpressionAttributeValues(attributeValueMapBuilder);
        try {
            mapper.delete(customer, deleteExpression);
        } catch (Exception e) {
            assertTrue(e instanceof ConditionalCheckFailedException);
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            if (client != null) {
                client.deleteTable(b -> b.tableName(TABLENAME));
            }
        } catch (Exception e) {
        } finally {
            if (client != null)
                client.close();
        }
    }

    // Note don't move Customer to top level, or else it would break the release
    // pipeline, as the integration test will not be copied over causing
    // compilation failure
    @DynamoDBTable(tableName = DynamoDBMapperExpressionsIntegrationTest.TABLENAME)
    public static class Customer {

        private long customerId;

        private String addressType;

        private String addressLine1;

        private String city;

        private String state;

        private int zipcode;

        @DynamoDBAttribute(attributeName = "customerId")
        @DynamoDBHashKey(attributeName = "customerId")
        public long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(long customerId) {
            this.customerId = customerId;
        }

        @DynamoDBAttribute(attributeName = "addressType")
        @DynamoDBRangeKey(attributeName = "addressType")
        public String getAddressType() {
            return addressType;
        }

        public void setAddressType(String addressType) {
            this.addressType = addressType;
        }

        @DynamoDBAttribute(attributeName = "AddressLine1")
        public String getAddressLine1() {
            return addressLine1;
        }

        public void setAddressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
        }

        @DynamoDBAttribute(attributeName = "city")
        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        @DynamoDBAttribute(attributeName = "state")
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @DynamoDBAttribute(attributeName = "zipcode")
        public int getZipcode() {
            return zipcode;
        }

        public void setZipcode(int zipcode) {
            this.zipcode = zipcode;
        }
    }
}
