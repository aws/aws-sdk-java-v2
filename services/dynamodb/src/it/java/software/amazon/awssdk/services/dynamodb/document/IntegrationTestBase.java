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

package software.amazon.awssdk.services.dynamodb.document;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

/**
 * hashkey only            gsi hashkey only
 * gsi (hashkey + range key)
 *
 * hashkey + rangekey      lsi (same hash key + range key)
 * gsi hashkey only
 * gsi (hashkey + range key)
 */
public class IntegrationTestBase {
    // Table names
    protected static final String HASH_ONLY_TABLE_NAME = "DynamoDBTest_PrimaryHashKeyOnly";
    protected static final String RANGE_TABLE_NAME = "DynamoDBTest_PrimaryHashAndRangeKey";
    // Index names
    protected static final String HASH_ONLY_GSI_NAME = "HashOnlyGSI";
    protected static final String RANGE_GSI_NAME = "RangeGSI";
    protected static final String LSI_NAME = "LSI"; // LSI must involve a range key
    // attribute names for the primary keys
    protected static final String HASH_KEY_NAME = "hashkeyAttr";
    protected static final String RANGE_KEY_NAME = "rangekeyAttr";
    // attribute names for the GSI keys
    protected static final String GSI_HASH_KEY_NAME = "gsiHashkeyAttr";
    protected static final String GSI_RANGE_KEY_NAME = "gsiRangekeyAttr";
    // attribute names for the LSI keys (Note an LSI must share the same hash key as the table.)
    protected static final String LSI_RANGE_KEY_NAME = "lsiRangekeyAttr";
    private static final ProvisionedThroughput THRUPUT = ProvisionedThroughput.builder()
            .readCapacityUnits(1L).writeCapacityUnits(2L).build();
    private static final Projection PROJECTION = Projection.builder().projectionType(ProjectionType.ALL).build();
    protected static DynamoDb dynamo;
    protected static DynamoDb dynamoOld;

    //    private static final boolean IS_SERVICE_BUILDER_USED = false;
    @BeforeClass
    public static void setup() throws InterruptedException {
        dynamoOld = new DynamoDb(DynamoDBClient.builder().credentialsProvider(new StaticCredentialsProvider(awsTestCredentials())).build());
        DynamoDBClient client = DynamoDBClient.builder().credentialsProvider(new StaticCredentialsProvider(awsTestCredentials())).build();
        dynamo = new DynamoDb(client);
        createTable_hashKeyOnly();
        createTable_rangeKey();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        dynamo.shutdown();
        dynamoOld.shutdown();
    }

    // Creates a hashkey only table with 2 GSI,
    // one hashkey only, the other hashkey and rangekey
    private static void createTable_hashKeyOnly() throws InterruptedException {
        DynamoDb[] ddbs = {dynamo, dynamoOld};
        for (DynamoDb ddb : ddbs) {
            Table table = ddb.getTable(HASH_ONLY_TABLE_NAME);
            TableDescription desc = table.waitForActiveOrDelete();
            if (desc == null) {
                // table doesn't exist; let's create it
                ddb.createTable(CreateTableRequest.builder()
                        .tableName(HASH_ONLY_TABLE_NAME)
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName(HASH_KEY_NAME)
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName(GSI_HASH_KEY_NAME)
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName(GSI_RANGE_KEY_NAME)
                                        .attributeType(ScalarAttributeType.N)
                                        .build())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName(HASH_KEY_NAME)
                                .keyType(KeyType.HASH)
                                .build())
                        .globalSecondaryIndexes(
                                GlobalSecondaryIndex.builder()
                                        .indexName(HASH_ONLY_GSI_NAME)
                                        .keySchema(KeySchemaElement.builder()
                                                .attributeName(GSI_HASH_KEY_NAME)
                                                .keyType(KeyType.HASH)
                                                .build())
                                        .projection(PROJECTION)
                                        .provisionedThroughput(THRUPUT).build(),
                                GlobalSecondaryIndex.builder()
                                        .indexName(RANGE_GSI_NAME)
                                        .keySchema(KeySchemaElement.builder()
                                                        .attributeName(GSI_HASH_KEY_NAME)
                                                        .keyType(KeyType.HASH)
                                                        .build(),
                                                KeySchemaElement.builder()
                                                        .attributeName(GSI_RANGE_KEY_NAME)
                                                        .keyType(KeyType.RANGE)
                                                        .build())
                                        .projection(PROJECTION)
                                        .provisionedThroughput(THRUPUT)
                                        .build())
                        .provisionedThroughput(THRUPUT)
                        .build());
                // waits until table becomes active
                table.waitForActive();
            }
        }
    }

    // Creates a (hashkey + rangekey) table with 2 GSI,
    // (one hashkey only, the other hashkey and rangekey)
    // and an LSI (which must have a rangekey and share the hashkey with the table)
    private static void createTable_rangeKey() throws InterruptedException {
        DynamoDb[] ddbs = {dynamo, dynamoOld};
        for (DynamoDb ddb : ddbs) {
            Table table = ddb.getTable(RANGE_TABLE_NAME);
            TableDescription desc = table.waitForActiveOrDelete();
            if (desc == null) {
                // table doesn't exist; let's create it
                CreateTableRequest req = CreateTableRequest.builder()
                        .tableName(RANGE_TABLE_NAME)
                        .attributeDefinitions(
                                AttributeDefinition.builder().attributeName(HASH_KEY_NAME).attributeType(ScalarAttributeType.S).build(),
                                AttributeDefinition.builder().attributeName(RANGE_KEY_NAME).attributeType(ScalarAttributeType.N).build(),
                                AttributeDefinition.builder().attributeName(LSI_RANGE_KEY_NAME).attributeType(ScalarAttributeType.N).build(),
                                AttributeDefinition.builder().attributeName(GSI_HASH_KEY_NAME).attributeType(ScalarAttributeType.S).build(),
                                AttributeDefinition.builder().attributeName(GSI_RANGE_KEY_NAME).attributeType(ScalarAttributeType.N).build()
                        )
                        .keySchema(
                                KeySchemaElement.builder().attributeName(HASH_KEY_NAME).keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName(RANGE_KEY_NAME).keyType(KeyType.RANGE).build())
                        .provisionedThroughput(THRUPUT)
                        .globalSecondaryIndexes(
                                GlobalSecondaryIndex.builder()
                                        .indexName(HASH_ONLY_GSI_NAME)
                                        .keySchema(KeySchemaElement.builder().attributeName(GSI_HASH_KEY_NAME).keyType(KeyType.HASH).build())
                                        .projection(PROJECTION)
                                        .provisionedThroughput(THRUPUT)
                                        .build(),
                                GlobalSecondaryIndex.builder()
                                        .indexName(RANGE_GSI_NAME)
                                        .keySchema(
                                                KeySchemaElement.builder().attributeName(GSI_HASH_KEY_NAME).keyType(KeyType.HASH).build(),
                                                KeySchemaElement.builder().attributeName(GSI_RANGE_KEY_NAME).keyType(KeyType.RANGE).build())
                                        .projection(PROJECTION)
                                        .provisionedThroughput(THRUPUT)
                                        .build())
                        .localSecondaryIndexes(
                                LocalSecondaryIndex.builder()
                                        .indexName(LSI_NAME)
                                        .keySchema(
                                                KeySchemaElement.builder().attributeName(HASH_KEY_NAME).keyType(KeyType.HASH).build(),
                                                KeySchemaElement.builder().attributeName(LSI_RANGE_KEY_NAME).keyType(KeyType.RANGE).build())
                                        .projection(PROJECTION).build()
                                                  ).build();
                ddb.createTable(req);
                // waits until table becomes active
                table.waitForActive();
            }
        }
    }

    protected static AwsCredentials awsTestCredentials() {
        try {
            return AwsIntegrationTestBase.CREDENTIALS_PROVIDER_CHAIN.getCredentials();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertTrue(int expected, int actual) {
        Assert.assertTrue("expected: " + expected + " but actual: " + actual,
                          expected == actual);
    }

    public static void assertGT(int greater, int less) {
        Assert.assertTrue("Expect " + greater + " > " + less, greater > less);
    }

    protected void putDataToRangeTable(DynamoDb dynamo, String hashKeyValue,
                                       int rangeKeyValueFrom, int rangeKeyValueTo) {
        Table table = dynamo.getTable(RANGE_TABLE_NAME);
        Item item = new Item()
                .withPrimaryKey(HASH_KEY_NAME, hashKeyValue, RANGE_KEY_NAME, 0)
                .withBinary("binary", new byte[] {1, 2, 3, 4})
                .withBinarySet("binarySet", new byte[] {5, 6}, new byte[] {7, 8})
                .withInt("intAttr", 1234)
                .withNumber("numberAttr", 999.1234)
                .withString("stringAttr", "bla")
                .withStringSet("stringSetAttr", "da", "di", "foo", "bar", "bazz");
        for (int i = rangeKeyValueFrom; i <= rangeKeyValueTo; i++) {
            item.withKeyComponent(RANGE_KEY_NAME, i);
            table.putItem(item);
        }
    }

    protected String[] getRangeTableAttributes() {
        String[] attrNames = {HASH_KEY_NAME, RANGE_KEY_NAME, "binary", "binarySet", "intAttr", "numberAttr", "stringAttr",
                              "stringSetAttr"};
        return attrNames;
    }

    protected String getRangeTableProjectionExpression() {
        return HASH_KEY_NAME + ", " + RANGE_KEY_NAME + ", #binary, binarySet, intAttr, numberAttr, stringAttr, stringSetAttr";
    }
}
