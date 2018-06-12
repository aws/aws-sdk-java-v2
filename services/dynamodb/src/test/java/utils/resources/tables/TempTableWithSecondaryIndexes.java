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

package utils.resources.tables;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
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
import utils.test.resources.DynamoDBTableResource;
import utils.test.util.DynamoDBTestBase;

/**
 * The table used by SecondaryIndexesIntegrationTest
 */
public class TempTableWithSecondaryIndexes extends DynamoDBTableResource {

    public static final String TEMP_TABLE_NAME = "java-sdk-indexes-" + System.currentTimeMillis();
    public static final String HASH_KEY_NAME = "hash_key";
    public static final String RANGE_KEY_NAME = "range_key";
    public static final String LSI_NAME = "local_secondary_index";
    public static final String LSI_RANGE_KEY_NAME = "local_secondary_index_attribute";
    public static final String GSI_NAME = "global_secondary_index";
    public static final String GSI_HASH_KEY_NAME = "global_secondary_index_hash_attribute";
    public static final String GSI_RANGE_KEY_NAME = "global_secondary_index_range_attribute";
    public static final ProvisionedThroughput GSI_PROVISIONED_THROUGHPUT = ProvisionedThroughput.builder()
            .readCapacityUnits(5L)
            .writeCapacityUnits(5L)
            .build();

    @Override
    protected DynamoDbClient getClient() {
        return DynamoDBTestBase.getClient();
    }

    /**
     * Table schema:
     *      Hash Key : HASH_KEY_NAME (S)
     *      Range Key : RANGE_KEY_NAME (N)
     * LSI schema:
     *      Hash Key : HASH_KEY_NAME (S)
     *      Range Key : LSI_RANGE_KEY_NAME (N)
     * GSI schema:
     *      Hash Key : GSI_HASH_KEY_NAME (N)
     *      Range Key : GSI_RANGE_KEY_NAME (N)
     */
    @Override
    protected CreateTableRequest getCreateTableRequest() {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TEMP_TABLE_NAME)
                .keySchema(
                        KeySchemaElement.builder()
                            .attributeName(HASH_KEY_NAME)
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement.builder()
                                .attributeName(RANGE_KEY_NAME)
                                .keyType(KeyType.RANGE)
                                .build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(
                                HASH_KEY_NAME).attributeType(
                                ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(
                                RANGE_KEY_NAME).attributeType(
                                ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(
                                LSI_RANGE_KEY_NAME).attributeType(
                                ScalarAttributeType.N).build(),
                        AttributeDefinition.builder().attributeName(
                                GSI_HASH_KEY_NAME).attributeType(
                                ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(
                                GSI_RANGE_KEY_NAME).attributeType(
                                ScalarAttributeType.N).build())
                .provisionedThroughput(BasicTempTable.DEFAULT_PROVISIONED_THROUGHPUT)
                .localSecondaryIndexes(
                        LocalSecondaryIndex.builder()
                                .indexName(LSI_NAME)
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName(
                                                        HASH_KEY_NAME)
                                                .keyType(KeyType.HASH).build(),
                                        KeySchemaElement.builder()
                                                .attributeName(
                                                        LSI_RANGE_KEY_NAME)
                                                .keyType(KeyType.RANGE).build())
                                .projection(
                                        Projection.builder()
                                                .projectionType(ProjectionType.KEYS_ONLY).build()).build())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder().indexName(GSI_NAME)
                                                  .keySchema(
                                                          KeySchemaElement.builder()
                                                                  .attributeName(
                                                                          GSI_HASH_KEY_NAME)
                                                                  .keyType(KeyType.HASH).build(),
                                                          KeySchemaElement.builder()
                                                                  .attributeName(
                                                                          GSI_RANGE_KEY_NAME)
                                                                  .keyType(KeyType.RANGE).build())
                                                  .projection(
                                                          Projection.builder()
                                                                  .projectionType(ProjectionType.KEYS_ONLY).build())
                                                  .provisionedThroughput(
                                                          GSI_PROVISIONED_THROUGHPUT).build())
                .build();
        return createTableRequest;
    }

}
