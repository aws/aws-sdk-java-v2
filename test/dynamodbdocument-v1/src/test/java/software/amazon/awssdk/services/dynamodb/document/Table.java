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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.document.api.DeleteItemApi;
import software.amazon.awssdk.services.dynamodb.document.api.GetItemApi;
import software.amazon.awssdk.services.dynamodb.document.api.PutItemApi;
import software.amazon.awssdk.services.dynamodb.document.api.QueryApi;
import software.amazon.awssdk.services.dynamodb.document.api.ScanApi;
import software.amazon.awssdk.services.dynamodb.document.api.UpdateItemApi;
import software.amazon.awssdk.services.dynamodb.document.internal.DeleteItemImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.GetItemImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;
import software.amazon.awssdk.services.dynamodb.document.internal.PutItemImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.QueryImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.ScanImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.UpdateItemImpl;
import software.amazon.awssdk.services.dynamodb.document.spec.DeleteItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.GetItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.PutItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.QuerySpec;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.UpdateItemSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.UpdateTableSpec;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalSecondaryIndexAction;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexUpdate;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse;

/**
 * A DynamoDB table. Instance of this class is typically obtained via
 * {@link DynamoDb#getTable(String)}.
 */
@ThreadSafe
public class Table implements PutItemApi, GetItemApi, QueryApi, ScanApi,
                              UpdateItemApi, DeleteItemApi {
    private static final long SLEEP_TIME_MILLIS = 5000;
    private final String tableName;
    private final DynamoDbClient client;
    private final PutItemImpl putItemDelegate;
    private final GetItemImpl getItemDelegate;
    private final UpdateItemImpl updateItemDelegate;
    private final DeleteItemImpl deleteItemDelegate;
    private final QueryImpl queryDelegate;
    private final ScanImpl scanDelegate;
    private volatile TableDescription tableDescription;

    public Table(DynamoDbClient client, String tableName) {
        this(client, tableName, null);
    }

    public Table(DynamoDbClient client, String tableName,
                 TableDescription tableDescription) {
        if (client == null) {
            throw new IllegalArgumentException("client must be specified");
        }
        if (tableName == null || tableName.trim().length() == 0) {
            throw new IllegalArgumentException("table name must not be null or empty");
        }
        this.client = client;
        this.tableName = tableName;
        this.tableDescription = tableDescription;

        this.putItemDelegate = new PutItemImpl(client, this);
        this.getItemDelegate = new GetItemImpl(client, this);
        this.updateItemDelegate = new UpdateItemImpl(client, this);
        this.deleteItemDelegate = new DeleteItemImpl(client, this);

        this.queryDelegate = new QueryImpl(client, this);
        this.scanDelegate = new ScanImpl(client, this);
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the table description; or null if the table description has not
     * yet been described via {@link #describe()}.  No network call.
     */
    public TableDescription getDescription() {
        return tableDescription;
    }

    /**
     * Retrieves the table description from DynamoDB. Involves network calls.
     * Meant to be called as infrequently as possible to avoid throttling
     * exception from the server side.
     *
     * @return a non-null table description
     *
     * @throws ResourceNotFoundException if the table doesn't exist
     */
    public TableDescription describe() {
        DescribeTableResponse result = client.describeTable(
                InternalUtils.applyUserAgent(DescribeTableRequest.builder().tableName(tableName).build()));
        TableDescription description = result.table();
        tableDescription = description;
        return description;
    }

    /**
     * Gets a reference to the specified index. No network call.
     */
    public Index getIndex(String indexName) {
        return new Index(client, indexName, this);
    }

    @Override
    public PutItemOutcome putItem(Item item) {
        return putItemDelegate.putItem(item);
    }

    @Override
    public PutItemOutcome putItem(Item item, Expected... expected) {
        return putItemDelegate.putItem(item, expected);
    }

    @Override
    public PutItemOutcome putItem(Item item, String conditionExpression,
                                  Map<String, String> nameMap, Map<String, Object> valueMap) {
        return putItemDelegate.putItem(item, conditionExpression, nameMap,
                                       valueMap);
    }

    @Override
    public PutItemOutcome putItem(PutItemSpec spec) {
        return putItemDelegate.putItem(spec);
    }

    @Override
    public GetItemOutcome getItemOutcome(KeyAttribute... primaryKeyComponents) {
        return getItemDelegate.getItemOutcome(primaryKeyComponents);
    }

    @Override
    public GetItemOutcome getItemOutcome(PrimaryKey primaryKey) {
        return getItemDelegate.getItemOutcome(primaryKey);
    }

    @Override
    public GetItemOutcome getItemOutcome(PrimaryKey primaryKey,
                                         String projectionExpression, Map<String, String> nameMap) {
        return getItemDelegate.getItemOutcome(primaryKey, projectionExpression,
                                              nameMap);
    }

    @Override
    public GetItemOutcome getItemOutcome(GetItemSpec params) {
        return getItemDelegate.getItemOutcome(params);
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        AttributeUpdate... attributeUpdates) {
        return updateItemDelegate.updateItem(primaryKey, attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        Collection<Expected> expected, AttributeUpdate... attributeUpdates) {
        return updateItemDelegate.updateItem(primaryKey, expected,
                                             attributeUpdates);

    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        String updateExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return updateItemDelegate.updateItem(primaryKey, updateExpression,
                                             nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(PrimaryKey primaryKey,
                                        String updateExpression, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return updateItemDelegate.updateItem(primaryKey, updateExpression,
                                             conditionExpression, nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(UpdateItemSpec updateItemSpec) {
        return updateItemDelegate.updateItem(updateItemSpec);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName, Object hashKeyValue) {
        return queryDelegate.query(hashKeyName, hashKeyValue);
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey) {
        return queryDelegate.query(hashKey);
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition) {
        return queryDelegate.query(hashKey, rangeKeyCondition);
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, String filterExpression,
                                              String projectionExpression, Map<String, String> nameMap,
                                              Map<String, Object> valueMap) {
        return queryDelegate.query(hashKey, rangeKeyCondition,
                                   filterExpression, projectionExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, QueryFilter... queryFilters) {
        return queryDelegate.query(hashKey, rangeKeyCondition, queryFilters);
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, String filterExpression,
                                              Map<String, String> nameMap, Map<String, Object> valueMap) {
        return queryDelegate.query(hashKey, rangeKeyCondition,
                                   filterExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<QueryOutcome> query(QuerySpec spec) {
        return queryDelegate.query(spec);
    }

    @Override
    public ItemCollection<ScanOutcome> scan(ScanFilter... scanFilters) {
        return scanDelegate.scan(scanFilters);
    }

    @Override
    public ItemCollection<ScanOutcome> scan(String filterExpression,
                                            Map<String, String> nameMap, Map<String, Object> valueMap) {
        return scanDelegate.scan(filterExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<ScanOutcome> scan(String filterExpression,
                                            String projectionExpression, Map<String, String> nameMap,
                                            Map<String, Object> valueMap) {
        return scanDelegate.scan(filterExpression, projectionExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<ScanOutcome> scan(ScanSpec params) {
        return scanDelegate.scan(params);
    }

    @Override
    public DeleteItemOutcome deleteItem(KeyAttribute... primaryKeyComponents) {
        return deleteItemDelegate.deleteItem(primaryKeyComponents);
    }

    @Override
    public DeleteItemOutcome deleteItem(PrimaryKey primaryKey) {
        return deleteItemDelegate.deleteItem(primaryKey);
    }

    @Override
    public DeleteItemOutcome deleteItem(PrimaryKey primaryKey,
                                        Expected... expected) {
        return deleteItemDelegate.deleteItem(primaryKey, expected);
    }

    @Override
    public DeleteItemOutcome deleteItem(PrimaryKey primaryKey,
                                        String conditionExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return deleteItemDelegate.deleteItem(primaryKey,
                                             conditionExpression, nameMap, valueMap);
    }

    @Override
    public DeleteItemOutcome deleteItem(DeleteItemSpec spec) {
        return deleteItemDelegate.deleteItem(spec);
    }

    /**
     * Updates the provisioned throughput for this table. Setting the
     * throughput for a table helps you manage performance and is part of the
     * provisioned throughput feature of DynamoDB.
     * <p>
     * The provisioned throughput values can be upgraded or downgraded based
     * on the maximums and minimums listed in the
     * <a href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html"> Limits </a>
     * section in the Amazon DynamoDB Developer Guide.
     * <p>
     * This table must be in the <code>ACTIVE</code> state for this operation
     * to succeed. <i>UpdateTable</i> is an asynchronous operation; while
     * executing the operation, the table is in the <code>UPDATING</code>
     * state. While the table is in the <code>UPDATING</code> state, the
     * table still has the provisioned throughput from before the call. The
     * new provisioned throughput setting is in effect only when the table
     * returns to the <code>ACTIVE</code> state after the <i>UpdateTable</i>
     * operation.
     * <p>
     * You can create, update or delete indexes using <i>UpdateTable</i>.
     * </p>
     *
     * @param spec used to specify all the detailed parameters
     *
     * @return the updated table description returned from DynamoDB.
     */
    public TableDescription updateTable(UpdateTableSpec spec) {
        UpdateTableRequest.Builder reqBuilder = spec.getRequest().toBuilder();
        reqBuilder.tableName(getTableName());
        UpdateTableRequest updated = reqBuilder.build();
        UpdateTableResponse result = client.updateTable(updated);
        TableDescription description = result.tableDescription();
        this.tableDescription = description;

        return description;
    }

    /**
     * Creates a global secondary index (GSI) with only a hash key on this
     * table. Involves network calls. This table must be in the
     * <code>ACTIVE</code> state for this operation to succeed. Creating a
     * global secondary index is an asynchronous operation; while executing the
     * operation, the index is in the <code>CREATING</code> state. Once created,
     * the index will be in <code>ACTIVE</code> state.
     *
     * @param create
     *            used to specify the details of the index creation
     * @param hashKeyDefinition
     *            used to specify the attribute for describing the key schema
     *            for the hash key of the GSI to be created for this table.
     *
     * @return the index being created
     */
    public Index createGsi(
            CreateGlobalSecondaryIndexAction create,
            AttributeDefinition hashKeyDefinition) {
        return doCreateGsi(create, hashKeyDefinition);
    }

    /**
     * Creates a global secondary index (GSI) with both a hash key and a range
     * key on this table. Involves network calls. This table must be in the
     * <code>ACTIVE</code> state for this operation to succeed. Creating a
     * global secondary index is an asynchronous operation; while executing the
     * operation, the index is in the <code>CREATING</code> state. Once created,
     * the index will be in <code>ACTIVE</code> state.
     *
     * @param create
     *            used to specify the details of the index creation
     * @param hashKeyDefinition
     *            used to specify the attribute for describing the key schema
     *            for the hash key of the GSI to be created for this table.
     * @param rangeKeyDefinition
     *            used to specify the attribute for describing the key schema
     *            for the range key of the GSI to be created for this table.
     *
     * @return the index being created
     */
    public Index createGsi(
            CreateGlobalSecondaryIndexAction create,
            AttributeDefinition hashKeyDefinition,
            AttributeDefinition rangeKeyDefinition) {
        return doCreateGsi(create, hashKeyDefinition, rangeKeyDefinition);
    }

    private Index doCreateGsi(
            CreateGlobalSecondaryIndexAction create,
            AttributeDefinition... keyDefinitions) {
        UpdateTableSpec spec = new UpdateTableSpec()
                .withAttributeDefinitions(keyDefinitions)
                .withGlobalSecondaryIndexUpdates(GlobalSecondaryIndexUpdate.builder()
                        .create(create).build());
        updateTable(spec);
        return this.getIndex(create.indexName());
    }

    /**
     * Updates the provisioned throughput for this table. Setting the
     * throughput for a table helps you manage performance and is part of the
     * provisioned throughput feature of DynamoDB.
     * <p>
     * The provisioned throughput values can be upgraded or downgraded based
     * on the maximums and minimums listed in the
     * <a href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html"> Limits </a>
     * section in the Amazon DynamoDB Developer Guide.
     * <p>
     * This table must be in the <code>ACTIVE</code> state for this operation
     * to succeed. <i>UpdateTable</i> is an asynchronous operation; while
     * executing the operation, the table is in the <code>UPDATING</code>
     * state. While the table is in the <code>UPDATING</code> state, the
     * table still has the provisioned throughput from before the call. The
     * new provisioned throughput setting is in effect only when the table
     * returns to the <code>ACTIVE</code> state after the <i>UpdateTable</i>
     * operation.
     * <p>
     * You can create, update or delete indexes using <i>UpdateTable</i>.
     * </p>
     *
     * @param provisionedThroughput target provisioned throughput
     *
     * @return the updated table description returned from DynamoDB.
     */
    public TableDescription updateTable(
            ProvisionedThroughput provisionedThroughput) {
        return updateTable(new UpdateTableSpec()
                                   .withProvisionedThroughput(provisionedThroughput));
    }

    /**
     * A convenient blocking call that can be used, typically during table
     * creation, to wait for the table to become active. This method uses
     * {@link software.amazon.awssdk.services.dynamodb.waiters.AmazonDynamoDBWaiters}
     * to poll the status of the table every 5 seconds.
     *
     * @return the table description when the table has become active
     *
     * @throws IllegalArgumentException if the table is being deleted
     * @throws ResourceNotFoundException if the table doesn't exist
     */
    public TableDescription waitForActive() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * A convenient blocking call that can be used, typically during table
     * deletion, to wait for the table to become deleted. This method uses
     * {@link software.amazon.awssdk.services.dynamodb.waiters.AmazonDynamoDBWaiters}
     * to poll the status of the table every 5 seconds.
     */
    public void waitForDelete() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * A convenient blocking call that can be used to wait on a table until it
     * has either become active or deleted (ie no longer exists) by polling the
     * table every 5 seconds.
     *
     * @return the table description if the table has become active; or null
     *     if the table has been deleted.
     *
     * @deprecated If this method is called immediately after
     *     {@link DynamoDbClient#createTable(CreateTableRequest)} or
     *     {@link DynamoDbClient#deleteTable(DeleteTableRequest)} operation,
     *     the result might be incorrect as all {@link DynamoDbClient}
     *     operations are eventually consistent and might have a few seconds delay before the status is changed.
     */
    @Deprecated
    public TableDescription waitForActiveOrDelete() throws InterruptedException {
        try {
            for (; ; ) {
                TableDescription desc = describe();
                if (desc.tableStatus() == TableStatus.ACTIVE) {
                    return desc;
                } else {
                    Thread.sleep(SLEEP_TIME_MILLIS);
                }
            }
        } catch (ResourceNotFoundException deleted) {
            // Ignored or expected.
        }
        return null;
    }

    /**
     * A convenient blocking call that can be used to wait on a table and all
     * it's indexes until both the table and it's indexes have either become
     * active or deleted (ie no longer exists) by polling the table every 5
     * seconds.
     *
     * @return the table description if the table and all it's indexes have
     *         become active; or null if the table has been deleted.
     *
     * @deprecated If this method is called immediately after
     *     {@link DynamoDbClient#createTable(CreateTableRequest)} or
     *     {@link DynamoDbClient#deleteTable(DeleteTableRequest)} operation,
     *     the result might be incorrect as all {@link DynamoDbClient}
     *     operations are eventually consistent and might have a few seconds delay before the status is changed.
     */
    @Deprecated
    public TableDescription waitForAllActiveOrDelete() throws InterruptedException {
        try {
            retry:
            for (; ; ) {
                TableDescription desc = describe();
                if (desc.tableStatus() == TableStatus.ACTIVE) {
                    List<GlobalSecondaryIndexDescription> descriptions =
                            desc.globalSecondaryIndexes();
                    if (descriptions != null) {
                        for (GlobalSecondaryIndexDescription d : descriptions) {
                            if (d.indexStatus() != IndexStatus.ACTIVE) {
                                // Some index is not active.  Keep waiting.
                                Thread.sleep(SLEEP_TIME_MILLIS);
                                continue retry;
                            }
                        }
                    }
                    return desc;
                }
                Thread.sleep(SLEEP_TIME_MILLIS);
                continue;
            }
        } catch (ResourceNotFoundException deleted) {
            // Ignored or expected.
        }
        return null;
    }

    /**
     * Deletes the table from DynamoDB. Involves network calls.
     */
    public DeleteTableResponse delete() {
        return client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }

    @Override
    public Item getItem(KeyAttribute... primaryKeyComponents) {
        return getItemDelegate.getItem(primaryKeyComponents);
    }

    @Override
    public Item getItem(PrimaryKey primaryKey) {
        return getItemDelegate.getItem(primaryKey);
    }

    @Override
    public Item getItem(PrimaryKey primaryKey, String projectionExpression,
                        Map<String, String> nameMap) {
        return getItemDelegate.getItem(primaryKey, projectionExpression, nameMap);
    }

    @Override
    public Item getItem(GetItemSpec spec) {
        return getItemDelegate.getItem(spec);
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName, Object hashKeyValue) {
        return getItemDelegate.getItemOutcome(hashKeyName, hashKeyValue);
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName, Object hashKeyValue,
                                         String rangeKeyName, Object rangeKeyValue) {
        return getItemDelegate.getItemOutcome(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue);
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue) {
        return getItemDelegate.getItem(hashKeyName, hashKeyValue);
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue,
                        String rangeKeyName, Object rangeKeyValue) {
        return getItemDelegate.getItem(hashKeyName, hashKeyValue, rangeKeyName, rangeKeyValue);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition) {
        return queryDelegate.query(hashKeyName, hashKeyValue, rangeKeyCondition);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition,
                                              QueryFilter... queryFilters) {
        return queryDelegate.query(hashKeyName, hashKeyValue,
                                   rangeKeyCondition, queryFilters);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition,
                                              String filterExpression, Map<String, String> nameMap,
                                              Map<String, Object> valueMap) {
        return queryDelegate.query(hashKeyName, hashKeyValue,
                                   rangeKeyCondition, filterExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<QueryOutcome> query(String hashKeyName,
                                              Object hashKeyValue, RangeKeyCondition rangeKeyCondition,
                                              String filterExpression, String projectionExpression,
                                              Map<String, String> nameMap, Map<String, Object> valueMap) {
        return queryDelegate.query(hashKeyName, hashKeyValue,
                                   rangeKeyCondition, filterExpression, projectionExpression,
                                   nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, AttributeUpdate... attributeUpdates) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        AttributeUpdate... attributeUpdates) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue, attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, Collection<Expected> expected,
                                        AttributeUpdate... attributeUpdates) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             expected, attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        Collection<Expected> expected, AttributeUpdate... attributeUpdates) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue,
                                             expected, attributeUpdates);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName,
                                        Object hashKeyValue, String updateExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             updateExpression, nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                        String rangeKeyName, Object rangeKeyValue,
                                        String updateExpression,
                                        Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue,
                                             updateExpression, nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                        String updateExpression, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             updateExpression, conditionExpression,
                                             nameMap, valueMap);
    }

    @Override
    public UpdateItemOutcome updateItem(String hashKeyName, Object hashKeyValue,
                                        String rangeKeyName, Object rangeKeyValue,
                                        String updateExpression, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return updateItemDelegate.updateItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue,
                                             updateExpression, conditionExpression,
                                             nameMap, valueMap);
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName,
                                         Object hashKeyValue, String projectionExpression,
                                         Map<String, String> nameMap) {
        return getItemDelegate.getItemOutcome(hashKeyName, hashKeyValue,
                                              projectionExpression, nameMap);
    }

    @Override
    public GetItemOutcome getItemOutcome(String hashKeyName,
                                         Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                         String projectionExpression, Map<String, String> nameMap) {
        return getItemDelegate.getItemOutcome(hashKeyName, hashKeyValue,
                                              rangeKeyName, rangeKeyValue, projectionExpression, nameMap);
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue,
                        String projectionExpression, Map<String, String> nameMap) {
        return getItemDelegate.getItem(hashKeyName, hashKeyValue,
                                       projectionExpression, nameMap);
    }

    @Override
    public Item getItem(String hashKeyName, Object hashKeyValue,
                        String rangeKeyName, Object rangeKeyValue,
                        String projectionExpression, Map<String, String> nameMap) {
        return getItemDelegate.getItem(hashKeyName, hashKeyValue,
                                       rangeKeyName, rangeKeyValue, projectionExpression, nameMap);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName, Object hashKeyValue) {
        return deleteItemDelegate.deleteItem(hashKeyName, hashKeyValue);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue) {
        return deleteItemDelegate.deleteItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, Expected... expected) {
        return deleteItemDelegate.deleteItem(hashKeyName, hashKeyValue,
                                             expected);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        Expected... expected) {
        return deleteItemDelegate.deleteItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue, expected);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String conditionExpression,
                                        Map<String, String> nameMap, Map<String, Object> valueMap) {
        return deleteItemDelegate.deleteItem(hashKeyName, hashKeyValue,
                                             conditionExpression, nameMap, valueMap);
    }

    @Override
    public DeleteItemOutcome deleteItem(String hashKeyName,
                                        Object hashKeyValue, String rangeKeyName, Object rangeKeyValue,
                                        String conditionExpression, Map<String, String> nameMap,
                                        Map<String, Object> valueMap) {
        return deleteItemDelegate.deleteItem(hashKeyName, hashKeyValue,
                                             rangeKeyName, rangeKeyValue,
                                             conditionExpression, nameMap, valueMap);
    }

    @Override
    public String toString() {
        return "{" + tableName + ": " + tableDescription + "}";
    }
}
