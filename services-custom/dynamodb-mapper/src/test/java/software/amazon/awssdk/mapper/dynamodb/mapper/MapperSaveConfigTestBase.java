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
import software.amazon.awssdk.mapper.dynamodb.DynamoDBAttribute;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBRangeKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBVersionAttribute;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.mapper.dynamodb.pojos.TestItem;
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
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT = ProvisionedThroughput.builder()
            .readCapacityUnits(READ_CAPACITY).writeCapacityUnits(
                    WRITE_CAPACITY).build();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpTestBase();
        dynamoMapper = new DynamoDBMapper(dynamo);

        createTestTable(DEFAULT_PROVISIONED_THROUGHPUT);
        dynamo.waiter().waitUntilTableExists(b -> b.tableName(tableName));
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
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
        assertEquals(KeyType.HASH.toString(), createdTableDescription
                .keySchema().get(0).keyTypeAsString());
        assertEquals(rangeKeyName, createdTableDescription
                .keySchema().get(1).attributeName());
        assertEquals(KeyType.RANGE.toString(), createdTableDescription
                .keySchema().get(1).keyTypeAsString());
    }
}
