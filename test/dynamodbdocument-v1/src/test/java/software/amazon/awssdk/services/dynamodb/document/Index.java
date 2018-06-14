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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.document.api.QueryApi;
import software.amazon.awssdk.services.dynamodb.document.api.ScanApi;
import software.amazon.awssdk.services.dynamodb.document.internal.IndexQueryImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.IndexScanImpl;
import software.amazon.awssdk.services.dynamodb.document.internal.ScanImpl;
import software.amazon.awssdk.services.dynamodb.document.spec.QuerySpec;
import software.amazon.awssdk.services.dynamodb.document.spec.ScanSpec;
import software.amazon.awssdk.services.dynamodb.document.spec.UpdateTableSpec;
import software.amazon.awssdk.services.dynamodb.model.DeleteGlobalSecondaryIndexAction;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexUpdate;
import software.amazon.awssdk.services.dynamodb.model.IndexStatus;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalSecondaryIndexAction;

/**
 * Represents a secondary index on a DynamoDB table. This covers
 * both GSI (Global Secondary Index) and LSI (Local Secondary Index). Instance
 * of this class can be obtained via {@link Table#getIndex(String)}.
 */
@ThreadSafe
public class Index implements QueryApi, ScanApi {
    private static final long SLEEP_TIME_MILLIS = 5000;
    private final Table table;
    private final String indexName;
    private final QueryApi queryDelegate;
    private final ScanImpl scanDelegate;

    Index(DynamoDbClient client, String indexName, Table table) {
        if (client == null) {
            throw new IllegalArgumentException("client must be specified");
        }
        if (indexName == null || indexName.trim().length() == 0) {
            throw new IllegalArgumentException("index name must not be null or empty");
        }
        if (table == null) {
            throw new IllegalArgumentException("table must be specified");
        }
        this.table = table;
        this.indexName = indexName;
        this.queryDelegate = new IndexQueryImpl(client, this);
        this.scanDelegate = new IndexScanImpl(client, this);
    }

    /**
     * Returns the owning table.
     */
    public final Table getTable() {
        return table;
    }

    /**
     * @return the name of this index
     */
    public final String getIndexName() {
        return indexName;
    }

    @Override
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition) {
        return queryDelegate.query(hashKey, rangeKeyCondition);
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
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey,
                                              RangeKeyCondition rangeKeyCondition, String projectionExpression,
                                              String filterExpression, Map<String, String> nameMap,
                                              Map<String, Object> valueMap) {
        return queryDelegate.query(hashKey, rangeKeyCondition,
                                   projectionExpression, filterExpression, nameMap, valueMap);
    }

    @Override
    public ItemCollection<QueryOutcome> query(QuerySpec spec) {
        return queryDelegate.query(spec);
    }

    @Override
    public ItemCollection<QueryOutcome> query(
            String hashKeyName, Object hashKeyValue) {
        return queryDelegate.query(hashKeyName, hashKeyValue);
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
    public ItemCollection<QueryOutcome> query(KeyAttribute hashKey) {
        return queryDelegate.query(hashKey);
    }

    /**
     * Updates the provisioned throughput for this global secondary index (GSI).
     * Setting the throughput for an index helps you manage performance and is
     * part of the provisioned throughput feature of DynamoDB.
     * <p>
     * The provisioned throughput values can be upgraded or downgraded based on
     * the maximums and minimums listed in the <a href=
     * "http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html"
     * > Limits </a> section in the Amazon DynamoDB Developer Guide.
     * <p>
     * This index must be a global secondary index and in the
     * <code>ACTIVE</code> state for this operation to succeed. Updating a GSI
     * is an asynchronous operation; while executing the operation, the index is
     * in the <code>UPDATING</code> state. While the index is in the
     * <code>UPDATING</code> state, the index still has the provisioned
     * throughput from before the call. The new provisioned throughput setting
     * is in effect only when the index returns to the <code>ACTIVE</code> state
     * after the update is complete.
     *
     * @param provisionedThroughput
     *            target provisioned throughput
     *
     * @return the updated table description returned from DynamoDB.
     */
    public TableDescription updateGsi(
            ProvisionedThroughput provisionedThroughput) {
        return table.updateTable(new UpdateTableSpec()
                .withGlobalSecondaryIndexUpdates(GlobalSecondaryIndexUpdate.builder()
                        .update(UpdateGlobalSecondaryIndexAction.builder()
                                .indexName(indexName)
                                .provisionedThroughput(provisionedThroughput).build())
                        .build()));
    }

    /**
     * Deletes this global secondary index (GSI) from the DynamoDB table.
     * Involves network calls.
     * <p>
     * This index must be a global secondary index and in the
     * <code>ACTIVE</code> state for this operation to succeed. Deleting a GSI
     * is an asynchronous operation; while executing the operation, the index is
     * in the <code>DELETING</code> state.
     *
     * @return the updated table description returned from DynamoDB.
     */
    public TableDescription deleteGsi() {
        return table.updateTable(new UpdateTableSpec()
                .withGlobalSecondaryIndexUpdates(
                        GlobalSecondaryIndexUpdate.builder()
                                .delete(DeleteGlobalSecondaryIndexAction.builder()
                                        .indexName(indexName).build())
                                .build()));
    }

    /**
     * A convenient blocking call that can be used, typically during index
     * creation, to wait for the index to become active by polling the table
     * every 5 seconds.
     * <p>
     * Currently online index creation is only supported for Global Secondary
     * Index (GSI). Calling this method on a Local Secondary Index (LSI) would
     * result in <code>IllegalArgumentException</code>.
     *
     * @return the table description when the index has become active
     *
     * @throws IllegalArgumentException if the table is being deleted, or if
     *     the GSI is not being created or updated, or if the GSI doesn't exist
     * @throws ResourceNotFoundException if the table doesn't exist
     */
    public TableDescription waitForActive() throws InterruptedException {
        final Table table = getTable();
        final String tableName = table.getTableName();
        final String indexName = getIndexName();
        retry:
        for (; ; ) {
            TableDescription desc = table.waitForActive();
            final List<GlobalSecondaryIndexDescription> list = desc.globalSecondaryIndexes();
            if (list != null) {
                for (GlobalSecondaryIndexDescription d : list) {
                    if (d.indexName().equals(indexName)) {
                        switch (d.indexStatus()) {
                            case ACTIVE:
                                return desc;
                            case CREATING:
                            case UPDATING:
                                Thread.sleep(SLEEP_TIME_MILLIS);
                                continue retry;
                            default:
                                throw new IllegalArgumentException(
                                        "Global Secondary Index "
                                        + indexName
                                        + " is not being created or updated (with status="
                                        + d.indexStatusAsString() + ")");
                        }
                    }
                }
            }
            throw new IllegalArgumentException("Global Secondary Index "
                                               + indexName + " does not exist in Table " + tableName + ")");
        }
    }

    /**
     * A convenient blocking call that can be used, typically during index
     * deletion on an active table, to wait for the index to become deleted by
     * polling the table every 5 seconds.
     * <p>
     * Currently online index deletion is only supported for Global Secondary
     * Index (GSI). The behavior of calling this method on a Local Secondary
     * Index (LSI) would result in returning the latest table description.
     *
     * @return the table description if this GSI has been deleted; or null if
     *         the underlying table has been deleted.
     *
     * @throws IllegalArgumentException if the table is being deleted, or if the
     *     GSI is not being deleted.
     * @throws ResourceNotFoundException if the table doesn't exist
     */
    public TableDescription waitForDelete() throws InterruptedException {
        final String indexName = getIndexName();
        retry:
        for (; ; ) {
            final TableDescription desc = getTable().waitForActive();
            List<GlobalSecondaryIndexDescription> list = desc.globalSecondaryIndexes();
            if (list != null) {
                for (GlobalSecondaryIndexDescription d : list) {
                    if (d.indexName().equals(indexName)) {
                        if (d.indexStatus() == IndexStatus.DELETING) {
                            Thread.sleep(SLEEP_TIME_MILLIS);
                            continue retry;
                        }
                        throw new IllegalArgumentException(
                                "Global Secondary Index " + indexName
                                + " is not being deleted (with status=" + d.indexStatusAsString() + ")");
                    }
                }
            }
            return desc;
        }
    }

    /**
     * A convenient blocking call that can be used to wait on an index until it
     * has either become active or deleted (ie no longer exists) by polling the
     * table every 5 seconds.
     * <p>
     * Currently online index creation/deletion is only supported for Global
     * Secondary Index (GSI). The behavior of calling this method on a Local
     * Secondary Index (LSI) would result in returning the latest table
     * description.
     *
     * @return the table description when the index has become either active
     *     or deleted
     *
     * @throws IllegalArgumentException if the table is being deleted
     * @throws ResourceNotFoundException if the table doesn't exist
     */
    public TableDescription waitForActiveOrDelete() throws InterruptedException {
        final Table table = getTable();
        final String indexName = getIndexName();
        retry:
        for (; ; ) {
            TableDescription desc = table.waitForActive();
            List<GlobalSecondaryIndexDescription> list = desc.globalSecondaryIndexes();
            if (list != null) {
                for (GlobalSecondaryIndexDescription d : desc.globalSecondaryIndexes()) {
                    if (d.indexName().equals(indexName)) {
                        if (d.indexStatus() == IndexStatus.ACTIVE) {
                            return desc;
                        }
                        Thread.sleep(SLEEP_TIME_MILLIS);
                        continue retry;
                    }
                }
            }
            return desc;
        }
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
}
