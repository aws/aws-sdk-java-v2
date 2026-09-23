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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AtomicCounterRecord;

public class AsyncAtomicCounterTest extends LocalDynamoDbAsyncTestBase {
    private static final TableSchema<AtomicCounterRecord> TABLE_SCHEMA = TableSchema.fromClass(AtomicCounterRecord.class);

    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient =
        DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();

    private final DynamoDbAsyncTable<AtomicCounterRecord> mappedTable =
        enhancedAsyncClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(r -> r.tableName(getConcreteTableName("table-name"))).join();
    }

    @Test
    public void repeatedUpdate_shouldIncrementCountersOnEachUpdate() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId("id1");
        record.setAttribute1("value");
        mappedTable.updateItem(record).join();
        mappedTable.updateItem(record).join();
        mappedTable.updateItem(record).join();

        AtomicCounterRecord persisted = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1"))).join();
        // AtomicCounterRecord annotations: defaultCounter (delta=1, start=0), customCounter (delta=5, start=10),
        // decreasingCounter (delta=-1, start=-20). First updateItem creates the item using start values;
        // each subsequent update adds delta. After 3 updateItem calls: 0+1+1=2, 10+5+5=20, -20-1-1=-22.
        assertThat(persisted.getDefaultCounter()).isEqualTo(2L);
        assertThat(persisted.getCustomCounter()).isEqualTo(20L);
        assertThat(persisted.getDecreasingCounter()).isEqualTo(-22L);
    }
}
