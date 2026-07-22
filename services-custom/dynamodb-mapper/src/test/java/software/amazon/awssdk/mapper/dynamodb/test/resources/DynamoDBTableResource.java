/*
 * Copyright 2013-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.mapper.dynamodb.test.resources;

import java.util.List;

import com.amazonaws.AmazonServiceException;
import software.amazon.awssdk.mapper.dynamodb.test.util.DynamoDBTestBase;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndexDescription;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import software.amazon.awssdk.mapper.dynamodb.test.resources.TestResource;
import software.amazon.awssdk.mapper.dynamodb.test.util.UnorderedCollectionComparator;
import software.amazon.awssdk.mapper.dynamodb.test.util.UnorderedCollectionComparator.CrossTypeComparator;

public abstract class DynamoDBTableResource implements TestResource {

    protected abstract AmazonDynamoDB getClient();

    protected abstract CreateTableRequest getCreateTableRequest();

    /**
     * Implementation of TestResource interfaces
     */

    @Override
    public void create(boolean waitTillFinished) {
        System.out.println("Creating " + this + "...");
        getClient().createTable(getCreateTableRequest());

        if (waitTillFinished) {
            System.out.println("Waiting for " + this + " to become active...");
            try {
                TableUtils.waitUntilActive(getClient(), getCreateTableRequest().getTableName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void delete(boolean waitTillFinished) {
        System.out.println("Deleting " + this + "...");
        getClient().deleteTable(getCreateTableRequest().getTableName());

        if (waitTillFinished) {
            System.out.println("Waiting for " + this + " to become deleted...");
            DynamoDBTestBase.waitForTableToBecomeDeleted(getClient(), getCreateTableRequest().getTableName());
        }
    }

    @Override
    public ResourceStatus getResourceStatus() {
        CreateTableRequest createRequest = getCreateTableRequest();
        TableDescription table = null;
        try {
            table = getClient().describeTable(
                    createRequest.getTableName()).getTable();
        } catch (AmazonServiceException ase) {
            if ( ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException")) {
                return ResourceStatus.NOT_EXIST;
            }
        }

        String tableStatus = table.getTableStatus();

        if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
            // returns AVAILABLE only if table KeySchema + LSIs + GSIs all match.
            if (UnorderedCollectionComparator.equalUnorderedCollections(createRequest.getKeySchema(), table.getKeySchema())
                 && equalUnorderedGsiLists(createRequest.getGlobalSecondaryIndexes(), table.getGlobalSecondaryIndexes())
                 && equalUnorderedLsiLists(createRequest.getLocalSecondaryIndexes(), table.getLocalSecondaryIndexes())
               ) {
                return ResourceStatus.AVAILABLE;
            } else {
                return ResourceStatus.EXIST_INCOMPATIBLE_RESOURCE;
            }
        } else if (tableStatus.equals(TableStatus.CREATING.toString())
                || tableStatus.equals(TableStatus.UPDATING.toString())
                || tableStatus.equals(TableStatus.DELETING.toString())) {
            return ResourceStatus.TRANSIENT;
        } else {
            return ResourceStatus.NOT_EXIST;
        }
    }

    /**
     * Returns true if the two lists of GlobalSecondaryIndex and
     * GlobalSecondaryIndexDescription share the same set of:
     *  1) indexName
     *  2) projection
     *  3) keySchema (compared as unordered lists)
     */
    static boolean equalUnorderedGsiLists(List<GlobalSecondaryIndex> listA, List<GlobalSecondaryIndexDescription> listB) {
        return UnorderedCollectionComparator.equalUnorderedCollections(
                listA, listB,
                new CrossTypeComparator<GlobalSecondaryIndex, GlobalSecondaryIndexDescription>() {
                    @Override
                    public boolean equals(GlobalSecondaryIndex a, GlobalSecondaryIndexDescription b) {
                        return a.getIndexName().equals(b.getIndexName())
                                && equalProjections(a.getProjection(), b.getProjection())
                                && UnorderedCollectionComparator.equalUnorderedCollections(a.getKeySchema(), b.getKeySchema());
                    }
                });
    }

    /**
     * Returns true if the two lists of LocalSecondaryIndex and
     * LocalSecondaryIndexDescription share the same set of:
     *  1) indexName
     *  2) projection
     *  3) keySchema (compared as unordered lists)
     */
    static boolean equalUnorderedLsiLists(List<LocalSecondaryIndex> listA, List<LocalSecondaryIndexDescription> listB) {
        return UnorderedCollectionComparator.equalUnorderedCollections(
                listA, listB,
                new CrossTypeComparator<LocalSecondaryIndex, LocalSecondaryIndexDescription>() {
                    @Override
                    public boolean equals(LocalSecondaryIndex a, LocalSecondaryIndexDescription b) {
                        // Project parameter might not be specified in the
                        // CreateTableRequest. But it should be treated as equal
                        // to the default projection type - KEYS_ONLY.
                        return a.getIndexName().equals(b.getIndexName())
                                && equalProjections(a.getProjection(), b.getProjection())
                                && UnorderedCollectionComparator.equalUnorderedCollections(a.getKeySchema(), b.getKeySchema());
                    }
                });
    }

    /**
     * Compares the Projection parameter included in the CreateTableRequest,
     * with the one returned from DescribeTableResult.
     */
    static boolean equalProjections(Projection fromCreateTableRequest, Projection fromDescribeTableResult) {
        if (fromCreateTableRequest == null || fromDescribeTableResult == null) {
            throw new IllegalStateException("The projection parameter should never be null.");
        }

        return fromCreateTableRequest.getProjectionType().equals(
                    fromDescribeTableResult.getProjectionType())
                && UnorderedCollectionComparator.equalUnorderedCollections(
                        fromCreateTableRequest.getNonKeyAttributes(),
                        fromDescribeTableResult.getNonKeyAttributes());
    }


    /**
     * Object interfaces
     */
    @Override
    public String toString() {
        return "DynamoDB Table [" + getCreateTableRequest().getTableName() + "]";
    }

    @Override
    public int hashCode() {
        return getCreateTableRequest().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if ( !(other instanceof DynamoDBTableResource) ) {
            return false;
        }
        return getCreateTableRequest().equals(
                ((DynamoDBTableResource)other).getCreateTableRequest());
    }
}
