/*
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.test.AWSTestBase;
import com.amazonaws.util.ImmutableMapParameter;
import com.amazonaws.util.ImmutableMapParameter.Builder;

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
    protected static AmazonDynamoDBClient client;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException,
            InterruptedException {
        setUpCredentials();
        client = new AmazonDynamoDBClient(credentials);
        mapper = new DynamoDBMapper(client);
        try {
            client.createTable(new CreateTableRequest()
                .withTableName(TABLENAME)
                .withKeySchema(new KeySchemaElement(HASH_KEY, KeyType.HASH),
                        new KeySchemaElement(RANGE_KEY, KeyType.RANGE))
                .withAttributeDefinitions(
                        new AttributeDefinition(HASH_KEY, ScalarAttributeType.N),
                        new AttributeDefinition(RANGE_KEY,
                                ScalarAttributeType.S))
                .withProvisionedThroughput(
                        new ProvisionedThroughput(READ_CAPACITY, WRITE_CAPACITY)));
        } catch(ResourceInUseException ex) {
            ex.printStackTrace();
        }
        waitForTableCreation();
        fillInData();
    }

    public static void fillInData() {
        final Builder<String, AttributeValue> record1 = ImmutableMapParameter
                .builder();
        record1.put(HASH_KEY, new AttributeValue().withN(FIRST_CUSTOMER_ID))
                .put(RANGE_KEY, new AttributeValue().withS(ADDRESS_TYPE_WORK))
                .put("AddressLine1",
                        new AttributeValue().withS("1918 8th Aven"))
                .put("city", new AttributeValue().withS("seattle"))
                .put("state", new AttributeValue().withS("WA"))
                .put("zipcode", new AttributeValue().withN("98104"));
        final Builder<String, AttributeValue> record2 = ImmutableMapParameter
                .builder();
        record2.put(HASH_KEY, new AttributeValue().withN(FIRST_CUSTOMER_ID))
                .put(RANGE_KEY, new AttributeValue().withS(ADDRESS_TYPE_HOME))
                .put("AddressLine1",
                        new AttributeValue().withS("15606 NE 40th ST"))
                .put("city", new AttributeValue().withS("redmond"))
                .put("state", new AttributeValue().withS("WA"))
                .put("zipcode", new AttributeValue().withN("98052"));

        client.putItem(new PutItemRequest(TABLENAME, record1.build()));
        client.putItem(new PutItemRequest(TABLENAME, record2.build()));
    }

    public static void waitForTableCreation() throws InterruptedException {
        while (true) {
            DescribeTableResult describeResult = client
                    .describeTable(TABLENAME);
            if (TABLE_STATUS_ACTIVE.equals(describeResult.getTable()
                    .getTableStatus())) {
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
            .withRangeKeyCondition(RANGE_KEY, new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue(ADDRESS_TYPE_HOME)))
            ;
        PaginatedQueryList<Customer> results = mapper.query(Customer.class,
                queryExpression);
        assertTrue(results.size() == 1);

        final Builder<String, AttributeValue> builder = ImmutableMapParameter
                .builder();
        builder.put(":zipcode", new AttributeValue().withN("98109"));

        queryExpression = queryExpression
            .withFilterExpression("zipcode = :zipcode")
            .withExpressionAttributeValues(builder.build());
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
        final Builder<String, AttributeValue> builder =
                ImmutableMapParameter.builder();
        builder.put(":customerId", new AttributeValue().withN(FIRST_CUSTOMER_ID))
               .put(":addressType", new AttributeValue(ADDRESS_TYPE_HOME))
               ;
        qxp.withExpressionAttributeValues(builder.build());

        PaginatedQueryList<Customer> results = mapper.query(Customer.class, qxp);
        assertTrue(results.size() == 1);

        builder.put(":zipcode", new AttributeValue().withN("98109"));
        qxp.withFilterExpression("zipcode = :zipcode")
           .withExpressionAttributeValues(builder.build())
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

        final Builder<String, AttributeValue> attributeValueMapBuilder = ImmutableMapParameter
                .builder();
        attributeValueMapBuilder
                .put(":state", new AttributeValue().withS("WA"));

        final Builder<String, String> attributeNameMapBuilder = ImmutableMapParameter
                .builder();
        attributeNameMapBuilder.put("#statename", "state");

        scanExpression = scanExpression
                .withFilterExpression("#statename = :state")
                .withExpressionAttributeValues(attributeValueMapBuilder.build())
                .withExpressionAttributeNames(attributeNameMapBuilder.build());
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

        Builder<String, ExpectedAttributeValue> expectedMapBuilder = ImmutableMapParameter
                .builder();
        expectedMapBuilder.put("zipcode", new ExpectedAttributeValue()
                .withAttributeValueList(new AttributeValue().withN("98052"))
                .withComparisonOperator(ComparisonOperator.EQ));

        DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression();
        deleteExpression.setConditionExpression("zipcode = :zipcode");

        final Builder<String, AttributeValue> attributeValueMapBuilder = ImmutableMapParameter
                .builder();
        attributeValueMapBuilder.put(":zipcode",
                new AttributeValue().withN("98052"));
        deleteExpression.setExpressionAttributeValues(attributeValueMapBuilder
                .build());
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
                client.deleteTable(TABLENAME);
            }
        } catch (Exception e) {
        } finally {
            if (client != null)
                client.shutdown();
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
