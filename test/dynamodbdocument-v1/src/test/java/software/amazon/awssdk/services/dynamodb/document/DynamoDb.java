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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.api.BatchGetItemApi;
import software.amazon.awssdk.services.dynamodb.document.api.BatchWriteItemApi;
import software.amazon.awssdk.services.dynamodb.document.api.ListTablesApi;
import software.amazon.awssdk.services.dynamodb.document.internal.BatchGetItemImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.BatchWriteItemImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.ListTablesImpl;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchGetItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.BatchWriteItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.ListTablesSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * DynamoDB Document API. This class is the entry point to make use of this
 * library.
 */
@ThreadSafe
public class DynamoDb implements ListTablesApi, BatchGetItemApi,
                                 BatchWriteItemApi {
    private final DynamoDBClient client;

    private final ListTablesImpl listTablesDelegate;
    private final BatchGetItemImpl batchGetItemDelegate;
    private final BatchWriteItemImpl batchWriteItemDelegate;

    public DynamoDb(DynamoDBClient client) {
        if (client == null) {
            throw new IllegalArgumentException();
        }
        this.client = client;
        this.listTablesDelegate = new ListTablesImpl(client);
        this.batchGetItemDelegate = new BatchGetItemImpl(client);
        this.batchWriteItemDelegate = new BatchWriteItemImpl(client);
    }

    /**
     * Create a DynamoDB object that talks to the specified AWS region. The
     * underlying service client will use all the default client configurations,
     * including the default credentials provider chain. See
     * {@link DynamoDBClient#DynamoDBClient()} for more information.
     * <p>BatchWriteRetryStrategyTest
     * If you need more control over the client configuration, use
     * {@link DynamoDb#DynamoDb(DynamoDBClient)} instead.
     *
     * @param regionEnum
     *            the AWS region enum
     * @see DynamoDBClient#DynamoDBClient()
     */
    public DynamoDb(Region regionEnum) {
        this(DynamoDBClient.builder().region(regionEnum).build());
    }

    /**
     * Returns the specified DynamoDB table.  No network call is involved.
     */
    public Table getTable(String tableName) {
        return new Table(client, tableName);
    }

    /**
     * Creates the specified table in DynamoDB.
     */
    public Table createTable(CreateTableRequest req) {
        CreateTableResponse result = client.createTable(req);
        return new Table(client, req.tableName(),
                         result.tableDescription());
    }

    /**
     * Creates the specified table in DynamoDB.
     */
    public Table createTable(String tableName,
                             List<KeySchemaElement> keySchema,
                             List<AttributeDefinition> attributeDefinitions,
                             ProvisionedThroughput provisionedThroughput) {
        return createTable(CreateTableRequest.builder()
                        .tableName(tableName)
                        .keySchema(keySchema)
                        .attributeDefinitions(attributeDefinitions)
                        .provisionedThroughput(provisionedThroughput)
                        .build());
    }

    @Override
    public TableCollection<ListTablesResponse> listTables() {
        return listTablesDelegate.listTables();
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(String exclusiveStartTableName) {
        return listTablesDelegate.listTables(exclusiveStartTableName);
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(String exclusiveStartTableName,
                                                        int maxResultSize) {
        return listTablesDelegate.listTables(exclusiveStartTableName,
                                             maxResultSize);
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(int maxResultSize) {
        return listTablesDelegate.listTables(maxResultSize);
    }

    @Override
    public TableCollection<ListTablesResponse> listTables(ListTablesSpec spec) {
        return listTablesDelegate.listTables(spec);
    }

    @Override
    public BatchGetItemOutcome batchGetItem(
            ReturnConsumedCapacity returnConsumedCapacity,
            TableKeysAndAttributes... tableKeysAndAttributes) {
        return batchGetItemDelegate.batchGetItem(returnConsumedCapacity,
                                                 tableKeysAndAttributes);
    }

    @Override
    public BatchGetItemOutcome batchGetItem(
            TableKeysAndAttributes... tableKeysAndAttributes) {
        return batchGetItemDelegate.batchGetItem(tableKeysAndAttributes);
    }

    @Override
    public BatchGetItemOutcome batchGetItem(BatchGetItemSpec spec) {
        return batchGetItemDelegate.batchGetItem(spec);
    }

    @Override
    public BatchGetItemOutcome batchGetItemUnprocessed(
            ReturnConsumedCapacity returnConsumedCapacity,
            Map<String, KeysAndAttributes> unprocessedKeys) {
        return batchGetItemDelegate.batchGetItemUnprocessed(
                returnConsumedCapacity, unprocessedKeys);
    }

    @Override
    public BatchGetItemOutcome batchGetItemUnprocessed(
            Map<String, KeysAndAttributes> unprocessedKeys) {
        return batchGetItemDelegate.batchGetItemUnprocessed(unprocessedKeys);
    }

    @Override
    public BatchWriteItemOutcome batchWriteItem(
            TableWriteItems... tableWriteItems) {
        return batchWriteItemDelegate.batchWriteItem(tableWriteItems);
    }

    @Override
    public BatchWriteItemOutcome batchWriteItem(BatchWriteItemSpec spec) {
        return batchWriteItemDelegate.batchWriteItem(spec);
    }

    @Override
    public BatchWriteItemOutcome batchWriteItemUnprocessed(
            Map<String, List<WriteRequest>> unprocessedItems) {
        return batchWriteItemDelegate.batchWriteItemUnprocessed(unprocessedItems);
    }

    /**
     * Shuts down and release all resources.
     */
    public void shutdown() throws Exception {
        client.close();
    }
}
