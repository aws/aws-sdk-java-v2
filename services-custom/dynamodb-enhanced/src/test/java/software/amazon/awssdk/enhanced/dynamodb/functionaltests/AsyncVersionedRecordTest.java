package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.versionAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class AsyncVersionedRecordTest extends LocalDynamoDbAsyncTestBase {
    private static class Record {
        private String id;
        private String attribute;
        private Integer version;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
    }

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id").getter(Record::getId).setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("attribute").getter(Record::getAttribute)
                                                           .setter(Record::setAttribute))
                         .addAttribute(Integer.class, a -> a.name("version").getter(Record::getVersion)
                                                            .setter(Record::setVersion).tags(versionAttribute()))
                         .build();

    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient =
        DynamoDbEnhancedAsyncClient.builder()
                                   .dynamoDbClient(getDynamoDbAsyncClient())
                                   .extensions(VersionedRecordExtension.builder().build())
                                   .build();

    private final DynamoDbAsyncTable<Record> mappedTable = enhancedAsyncClient.table(getConcreteTableName("table-name"),
                                                                                      TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(r -> r.tableName(getConcreteTableName("table-name"))).join();
    }

    @Test
    public void transactWrite_updateVersionedRecord_shouldIncrementVersion() {
        Record record = new Record();
        record.setId("id1");
        record.setAttribute("v1");
        mappedTable.updateItem(record).join();
        Record persistedAfterFirstUpdate = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1"))).join();
        assertThat(persistedAfterFirstUpdate.getVersion()).isEqualTo(1);

        Record update = new Record();
        update.setId("id1");
        update.setAttribute("v2");
        update.setVersion(1);
        enhancedAsyncClient.transactWriteItems(r -> r.addUpdateItem(mappedTable, update)).join();

        Record persisted = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1"))).join();
        assertThat(persisted.getVersion()).isEqualTo(persistedAfterFirstUpdate.getVersion() + 1);
        assertThat(persisted.getAttribute()).isEqualTo("v2");
    }
}
