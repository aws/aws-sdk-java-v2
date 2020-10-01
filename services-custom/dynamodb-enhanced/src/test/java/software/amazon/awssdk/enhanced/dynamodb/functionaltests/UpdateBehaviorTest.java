package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithUpdateBehaviors;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateBehaviorTest extends LocalDynamoDbSyncTestBase {
    private static final Instant INSTANT_1 = Instant.parse("2020-05-03T10:00:00Z");
    private static final Instant INSTANT_2 = Instant.parse("2020-05-03T10:05:00Z");

    private static final TableSchema<RecordWithUpdateBehaviors> TABLE_SCHEMA =
            TableSchema.fromClass(RecordWithUpdateBehaviors.class);

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(getDynamoDbClient())
            .build();


    private final DynamoDbTable<RecordWithUpdateBehaviors> mappedTable =
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
    public void updateBehaviors_firstUpdate() {
        RecordWithUpdateBehaviors record = new RecordWithUpdateBehaviors();
        record.setId("id123");
        record.setCreatedOn(INSTANT_1);
        record.setLastUpdatedOn(INSTANT_2);
        mappedTable.updateItem(record);

        RecordWithUpdateBehaviors persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getCreatedOn()).isEqualTo(INSTANT_1);
        assertThat(persistedRecord.getLastUpdatedOn()).isEqualTo(INSTANT_2);
    }

    @Test
    public void updateBehaviors_secondUpdate() {
        RecordWithUpdateBehaviors record = new RecordWithUpdateBehaviors();
        record.setId("id123");
        record.setCreatedOn(INSTANT_1);
        record.setLastUpdatedOn(INSTANT_2);
        mappedTable.updateItem(record);

        record.setVersion(1L);
        record.setCreatedOn(INSTANT_2);
        record.setLastUpdatedOn(INSTANT_2);
        mappedTable.updateItem(record);

        RecordWithUpdateBehaviors persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getCreatedOn()).isEqualTo(INSTANT_1);
        assertThat(persistedRecord.getLastUpdatedOn()).isEqualTo(INSTANT_2);
    }

    @Test
    public void updateBehaviors_removal() {
        RecordWithUpdateBehaviors record = new RecordWithUpdateBehaviors();
        record.setId("id123");
        record.setCreatedOn(INSTANT_1);
        record.setLastUpdatedOn(INSTANT_2);
        mappedTable.updateItem(record);

        record.setVersion(1L);
        record.setCreatedOn(null);
        record.setLastUpdatedOn(null);
        mappedTable.updateItem(record);

        RecordWithUpdateBehaviors persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getCreatedOn()).isNull();
        assertThat(persistedRecord.getLastUpdatedOn()).isNull();
    }

    @Test
    public void updateBehaviors_transactWriteItems_secondUpdate() {
        RecordWithUpdateBehaviors record = new RecordWithUpdateBehaviors();
        record.setId("id123");
        record.setCreatedOn(INSTANT_1);
        record.setLastUpdatedOn(INSTANT_2);
        mappedTable.updateItem(record);

        record.setVersion(1L);
        record.setCreatedOn(INSTANT_2);
        record.setLastUpdatedOn(INSTANT_2);
        enhancedClient.transactWriteItems(r -> r.addUpdateItem(mappedTable, record));

        RecordWithUpdateBehaviors persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getCreatedOn()).isEqualTo(INSTANT_1);
        assertThat(persistedRecord.getLastUpdatedOn()).isEqualTo(INSTANT_2);
    }

}
