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
        assertThat(persisted.getDefaultCounter()).isEqualTo(2L);
        assertThat(persisted.getCustomCounter()).isEqualTo(20L);
        assertThat(persisted.getDecreasingCounter()).isEqualTo(-22L);
    }
}
