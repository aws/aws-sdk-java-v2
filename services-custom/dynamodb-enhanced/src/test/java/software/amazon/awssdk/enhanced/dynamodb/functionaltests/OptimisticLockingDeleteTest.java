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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.model.VersionedRecord;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class OptimisticLockingDeleteTest extends LocalDynamoDbSyncTestBase {


    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<VersionedRecord> mappedTable = enhancedClient.table(
        getConcreteTableName("versioned-table"), TableSchema.fromClass(VersionedRecord.class));

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(
            DeleteTableRequest.builder()
                              .tableName(getConcreteTableName("versioned-table"))
                              .build());
    }

    @Test
    public void deleteItem_withKeyItem_appliesOptimisticLocking() {
        // Put initial item
        VersionedRecord record = new VersionedRecord();
        record.setId("test-id");
        record.setStringAttribute("initial-attr-value");
        mappedTable.putItem(record);

        // Get the item to obtain current version
        VersionedRecord retrievedRecord = mappedTable.getItem(r -> r.key(k -> k.partitionValue("test-id")));
        assertThat(retrievedRecord.getVersion(), is(1)); // VersionedRecordExtension starts at 1

        // Update the item to change version
        retrievedRecord.setStringAttribute("updated-attr-value");
        mappedTable.updateItem(retrievedRecord);

        // Try to delete using old version - should fail
        VersionedRecord oldVersionRecord = new VersionedRecord();
        oldVersionRecord.setId("test-id");
        oldVersionRecord.setVersion(1); // Old version

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.deleteItem(oldVersionRecord, true);
    }

    @Test
    public void deleteItem_withKeyItem_succeedsWithCorrectVersion() {
        // Put initial item
        VersionedRecord record = new VersionedRecord();
        record.setId("test-id");
        record.setStringAttribute("initial-attr-value");
        mappedTable.putItem(record);

        // Get the item to obtain current version
        VersionedRecord retrievedRecord = mappedTable.getItem(r -> r.key(k -> k.partitionValue("test-id")));

        // Delete using correct version - should succeed
        VersionedRecord deletedRecord = mappedTable.deleteItem(retrievedRecord);
        assertThat(deletedRecord.getId(), is("test-id"));

        // Verify item is deleted
        VersionedRecord afterDelete = mappedTable.getItem(r -> r.key(k -> k.partitionValue("test-id")));
        assertThat(afterDelete, is(nullValue()));
    }

    @Test
    public void deleteItem_withKey_doesNotApplyOptimisticLocking() {
        // Put initial item
        VersionedRecord record = new VersionedRecord();
        record.setId("test-id");
        record.setStringAttribute("initial-attr-value");
        mappedTable.putItem(record);

        // Update the item to change version
        VersionedRecord retrievedRecord = mappedTable.getItem(r -> r.key(k -> k.partitionValue("test-id")));
        retrievedRecord.setStringAttribute("updated-attr-value");
        mappedTable.updateItem(retrievedRecord);

        // Delete using key only - should succeed regardless of version
        VersionedRecord deletedRecord = mappedTable.deleteItem(r -> r.key(k -> k.partitionValue("test-id")));
        assertThat(deletedRecord.getId(), is("test-id"));

        // Verify item is deleted
        VersionedRecord afterDelete = mappedTable.getItem(r -> r.key(k -> k.partitionValue("test-id")));
        assertThat(afterDelete, is(nullValue()));
    }
}