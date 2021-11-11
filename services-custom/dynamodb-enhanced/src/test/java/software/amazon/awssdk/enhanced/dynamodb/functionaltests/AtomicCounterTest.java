package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AtomicCounterRecord;

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
    public void defaultCounterIncrementsCorrectly() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);

        mappedTable.updateItem(record);
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(1L);
    }

    @Test
    public void settingCounterValueHasNoEffect() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setDefaultCounter(10L);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDefaultCounter()).isEqualTo(0L);
    }

    @Test
    public void counterWithCustomValues() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(10L);

        mappedTable.updateItem(record);
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getCustomCounter()).isEqualTo(15L);
    }

    @Test
    public void counterCanDecreaseValue() {
        AtomicCounterRecord record = new AtomicCounterRecord();
        record.setId(RECORD_ID);
        record.setAttribute1(STRING_VALUE);
        mappedTable.updateItem(record);

        AtomicCounterRecord persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getAttribute1()).isEqualTo(STRING_VALUE);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-20L);

        mappedTable.updateItem(record);
        persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getDecreasingCounter()).isEqualTo(-21L);
    }

    @Test
    public void counterInitializedWithPutCanUpdate() {
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
}
