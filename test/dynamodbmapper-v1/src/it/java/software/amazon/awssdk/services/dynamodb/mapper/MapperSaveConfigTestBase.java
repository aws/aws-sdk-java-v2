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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.TableUtils;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.SaveBehavior;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbRangeKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import utils.test.util.DynamoDBIntegrationTestBase;

public class MapperSaveConfigTestBase extends DynamoDBIntegrationTestBase {

    protected static final DynamoDbMapperConfig defaultConfig = new DynamoDbMapperConfig(
            SaveBehavior.UPDATE);
    protected static final DynamoDbMapperConfig updateSkipNullConfig = new DynamoDbMapperConfig(
            SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES);
    protected static final DynamoDbMapperConfig appendSetConfig = new DynamoDbMapperConfig(
            SaveBehavior.APPEND_SET);
    protected static final DynamoDbMapperConfig clobberConfig = new DynamoDbMapperConfig(
            SaveBehavior.CLOBBER);
    protected static final String tableName = "aws-java-sdk-dynamodb-mapper-save-config-test";
    protected static final String hashKeyName = "hashKey";
    protected static final String rangeKeyName = "rangeKey";
    protected static final String nonKeyAttributeName = "nonKeyAttribute";
    protected static final String stringSetAttributeName = "stringSetAttribute";
    /**
     * Read capacity for the test table being created in Amazon DynamoDB.
     */
    protected static final Long READ_CAPACITY = 10L;
    /**
     * Write capacity for the test table being created in Amazon DynamoDB.
     */
    protected static final Long WRITE_CAPACITY = 5L;
    /**
     * Provisioned Throughput for the test table created in Amazon DynamoDB
     */
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT = ProvisionedThroughput.builder()
            .readCapacityUnits(READ_CAPACITY).writeCapacityUnits(
                    WRITE_CAPACITY).build();
    protected static DynamoDbMapper dynamoMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        dynamo = DynamoDbClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        dynamoMapper = new DynamoDbMapper(dynamo);

        createTestTable(DEFAULT_PROVISIONED_THROUGHPUT);
        TableUtils.waitUntilActive(dynamo, tableName);
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }

    /**
     * Helper method to create a table in Amazon DynamoDB
     */
    protected static void createTestTable(
            ProvisionedThroughput provisionedThroughput) {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder().attributeName(
                                hashKeyName).keyType(
                                KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(
                                rangeKeyName).keyType(
                                KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(
                                hashKeyName).attributeType(
                                ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(
                                rangeKeyName).attributeType(
                                ScalarAttributeType.N).build())
                .provisionedThroughput(provisionedThroughput)
                .build();

        TableDescription createdTableDescription = dynamo.createTable(
                createTableRequest).tableDescription();
        System.out.println("Created Table: " + createdTableDescription);
        assertEquals(tableName, createdTableDescription.tableName());
        assertNotNull(createdTableDescription.tableStatus());
        assertEquals(hashKeyName, createdTableDescription
                .keySchema().get(0).attributeName());
        assertEquals(KeyType.HASH, createdTableDescription
                .keySchema().get(0).keyType());
        assertEquals(rangeKeyName, createdTableDescription
                .keySchema().get(1).attributeName());
        assertEquals(KeyType.RANGE, createdTableDescription
                .keySchema().get(1).keyType());
    }

    @DynamoDbTable(tableName = tableName)
    public static class TestItem {

        private String hashKey;
        private Long rangeKey;
        private String nonKeyAttribute;
        private Set<String> stringSetAttribute;

        @DynamoDbHashKey(attributeName = hashKeyName)
        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDbRangeKey(attributeName = rangeKeyName)
        public Long getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(Long rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDbAttribute(attributeName = nonKeyAttributeName)
        public String getNonKeyAttribute() {
            return nonKeyAttribute;
        }

        public void setNonKeyAttribute(String nonKeyAttribute) {
            this.nonKeyAttribute = nonKeyAttribute;
        }

        @DynamoDbAttribute(attributeName = stringSetAttributeName)
        public Set<String> getStringSetAttribute() {
            return stringSetAttribute;
        }

        public void setStringSetAttribute(Set<String> stringSetAttribute) {
            this.stringSetAttribute = stringSetAttribute;
        }

    }

    @DynamoDbTable(tableName = tableName)
    public static class TestAppendToScalarItem {

        private String hashKey;
        private Long rangeKey;
        private Set<String> fakeStringSetAttribute;

        @DynamoDbHashKey(attributeName = hashKeyName)
        public String getHashKey() {
            return hashKey;
        }

        public void setHashKey(String hashKey) {
            this.hashKey = hashKey;
        }

        @DynamoDbRangeKey(attributeName = rangeKeyName)
        public Long getRangeKey() {
            return rangeKey;
        }

        public void setRangeKey(Long rangeKey) {
            this.rangeKey = rangeKey;
        }

        @DynamoDbAttribute(attributeName = nonKeyAttributeName)
        public Set<String> getFakeStringSetAttribute() {
            return fakeStringSetAttribute;
        }

        public void setFakeStringSetAttribute(Set<String> stringSetAttribute) {
            this.fakeStringSetAttribute = stringSetAttribute;
        }
    }
}
