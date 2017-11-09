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

package utils.test.util;

import org.junit.BeforeClass;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class DynamoDBIntegrationTestBase extends DynamoDBTestBase {

    protected static final String KEY_NAME = "key";
    protected static final String TABLE_NAME = "aws-java-sdk-util";
    protected static final String TABLE_WITH_RANGE_ATTRIBUTE = "aws-java-sdk-range-test";
    protected static final String TABLE_WITH_INDEX_RANGE_ATTRIBUTE = "aws-java-sdk-index-range-test";
    protected static long startKey = System.currentTimeMillis();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        dynamo = DynamoDBClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        // Create a table
        String keyName = KEY_NAME;
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .attributeName(keyName)
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(keyName)
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(5L).build())
                .build();

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_NAME);
        }
    }

    /**
     * Quick utility method to delete all tables when we have too much capacity
     * reserved for the region.
     */
    public static void deleteAllTables() {
        ListTablesResponse listTables = dynamo.listTables(ListTablesRequest.builder().build());
        for (String name : listTables.tableNames()) {
            dynamo.deleteTable(DeleteTableRequest.builder().tableName(name).build());
        }
    }

    protected static void setUpTableWithRangeAttribute() throws Exception {
        setUp();

        String keyName = DynamoDBIntegrationTestBase.KEY_NAME;
        String rangeKeyAttributeName = "rangeKey";
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_WITH_RANGE_ATTRIBUTE)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(keyName)
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName(rangeKeyAttributeName)
                                .keyType(KeyType.RANGE)
                                .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(keyName)
                                .attributeType(ScalarAttributeType.N)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName(rangeKeyAttributeName)
                                .attributeType(ScalarAttributeType.N)
                                .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(5L).build())
                .build();

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_WITH_RANGE_ATTRIBUTE);
        }
    }

    protected static void setUpTableWithIndexRangeAttribute(boolean recreateTable) throws Exception {
        setUp();
        if (recreateTable) {
            dynamo.deleteTable(DeleteTableRequest.builder().tableName(TABLE_WITH_INDEX_RANGE_ATTRIBUTE).build());
            waitForTableToBecomeDeleted(TABLE_WITH_INDEX_RANGE_ATTRIBUTE);
        }

        String keyName = DynamoDBIntegrationTestBase.KEY_NAME;
        String rangeKeyAttributeName = "rangeKey";
        String indexFooRangeKeyAttributeName = "indexFooRangeKey";
        String indexBarRangeKeyAttributeName = "indexBarRangeKey";
        String multipleIndexRangeKeyAttributeName = "multipleIndexRangeKey";
        String fooAttributeName = "fooAttribute";
        String barAttributeName = "barAttribute";
        String indexFooName = "index_foo";
        String indexBarName = "index_bar";
        String indexFooCopyName = "index_foo_copy";
        String indexBarCopyName = "index_bar_copy";

        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_WITH_INDEX_RANGE_ATTRIBUTE)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(keyName)
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName(rangeKeyAttributeName)
                                .keyType(KeyType.RANGE)
                                .build())
                .localSecondaryIndexes(
                        LocalSecondaryIndex.builder()
                                .indexName(indexFooName)
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName(keyName)
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName(indexFooRangeKeyAttributeName)
                                                .keyType(KeyType.RANGE)
                                                .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.INCLUDE)
                                        .nonKeyAttributes(fooAttributeName)
                                        .build())
                                .build(),
                        LocalSecondaryIndex.builder()
                                .indexName(indexBarName)
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName(keyName)
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName(indexBarRangeKeyAttributeName)
                                                .keyType(KeyType.RANGE)
                                                .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.INCLUDE)
                                        .nonKeyAttributes(barAttributeName)
                                        .build())
                                .build(),
                        LocalSecondaryIndex.builder()
                                .indexName(indexFooCopyName)
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName(keyName)
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName(multipleIndexRangeKeyAttributeName)
                                                .keyType(KeyType.RANGE)
                                                .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.INCLUDE)
                                        .nonKeyAttributes(fooAttributeName)
                                        .build())
                                .build(),
                        LocalSecondaryIndex.builder()
                                .indexName(indexBarCopyName)
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName(keyName)
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName(multipleIndexRangeKeyAttributeName)
                                                .keyType(KeyType.RANGE)
                                                .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.INCLUDE)
                                        .nonKeyAttributes(barAttributeName)
                                        .build())
                                .build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(keyName).attributeType(ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(rangeKeyAttributeName)
                                                 .attributeType(ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(indexFooRangeKeyAttributeName)
                                                 .attributeType(ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(indexBarRangeKeyAttributeName)
                                                 .attributeType(ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(multipleIndexRangeKeyAttributeName)
                                                 .attributeType(ScalarAttributeType.N).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(5L).build())
                .build();

        if (TableUtils.createTableIfNotExists(dynamo, createTableRequest)) {
            TableUtils.waitUntilActive(dynamo, TABLE_WITH_INDEX_RANGE_ATTRIBUTE);
        }
    }
}
