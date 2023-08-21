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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AtomicCounterRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

public class AtomicCounterTest extends LocalDynamoDbSyncTestBase {

    private static final String STRING_VALUE = "string value";
    private static final String RECORD_ID = "id123";

    private static final TableSchema<AtomicCounterRecord> TABLE_SCHEMA = TableSchema.fromClass(AtomicCounterRecord.class);

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private final DynamoDbTable<AtomicCounterRecord> mappedTable =
        enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(getConcreteTableName("table-name")));
    }

    @Test
    public void createViaUpdate_incrementsCorrectly() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(10L);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-20L);

        mappedTable.updateItem(record);
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(1L);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(15L);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-21L);
    }

    @Test
    public void createViaUpdate_multipleUpdates_incrementsCorrectly() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        int numUpdates = 50;
        IntStream.range(0, numUpdates).forEach(i -> mappedTable.updateItem(record));

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(numUpdates);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(10 + numUpdates * 5);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-20 + numUpdates * -1);
    }

    @Test
    public void createViaPut_incrementsCorrectly() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.putItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(10L);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-20L);

        mappedTable.updateItem(record);
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo("string value");
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(1L);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(15L);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-21L);
    }

    @Test
    public void createViaUpdate_settingCounterInPojo_hasNoEffect() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setDefaultCounter(10L);
        record.setAttribute1(STRING_VALUE);

        mappedTable.updateItem(record);
        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(10L);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-20L);
    }

    @Test
    public void updateItem_retrievedFromDb_shouldNotThrowException() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        AtomicCounterRecord retrievedRecord = mappedTable.getItem(record);
        retrievedRecord.setAttribute1("ChangingThisAttribute");

        retrievedRecord = mappedTable.updateItem(retrievedRecord);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getDefaultCounter()).isEqualTo(1L);
        assertThat(retrievedRecord.getCustomCounter()).isEqualTo(15L);
        assertThat(retrievedRecord.getDecreasingCounter()).isEqualTo(-21L);
    }

    @Test
    public void createViaPut_settingCounterInPojoHasNoEffect() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setDefaultCounter(10L);
        record.setAttribute1(STRING_VALUE);
        mappedTable.putItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);
    }

    @Test
    public void transactUpdate_incrementsCorrectly() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);

        enhancedClient.transactWriteItems(r -> r.addPutItem(mappedTable, record));

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);

        enhancedClient.transactWriteItems(r -> r.addUpdateItem(mappedTable, record));
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(1L);
    }

    @Test
    public void batchPut_initializesCorrectly() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);

        enhancedClient.batchWriteItem(r -> r.addWriteBatch(WriteBatch.builder(AtomicCounterRecord.class)
                                                                     .mappedTableResource(mappedTable)
                                                                     .addPutItem(record)
                                                                     .build()));

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);

        enhancedClient.transactWriteItems(r -> r.addUpdateItem(mappedTable, record));
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(1L);
    }
}
