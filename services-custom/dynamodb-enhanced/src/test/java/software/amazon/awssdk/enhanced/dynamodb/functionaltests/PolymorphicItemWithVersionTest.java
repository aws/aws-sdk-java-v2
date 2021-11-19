package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.PolymorphicItemWithVersionSubtype;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.PolymorphicItemWithVersionSubtype.SubtypeWithVersion;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.PolymorphicItemWithVersionSubtype.SubtypeWithoutVersion;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These functional tests are designed to ensure that the correct subtype TableMetadata is passed to extensions on
 * beforeWrite for a polymorphic TableSchema. This is done at the operation level, so it's the operations that are
 * really being tested. Since the versioned record extension only uses the beforeWrite hook, the other hooks are tested
 * with a fake extension that captures the context.
 */
public class PolymorphicItemWithVersionTest extends LocalDynamoDbSyncTestBase {
    private static final String VERSION_ATTRIBUTE_METADATA_KEY = "VersionedRecordExtension:VersionAttribute";

    private static final TableSchema<PolymorphicItemWithVersionSubtype> TABLE_SCHEMA =
            TableSchema.fromClass(PolymorphicItemWithVersionSubtype.class);

    private final FakeExtension fakeExtension = new FakeExtension();

    private final DynamoDbEnhancedClient enhancedClient =
            DynamoDbEnhancedClient.builder()
                                  .dynamoDbClient(getDynamoDbClient())
                                  .extensions(VersionedRecordExtension.builder().build(), fakeExtension)
                                  .build();

    private final DynamoDbTable<PolymorphicItemWithVersionSubtype> mappedTable =
            enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    private final class FakeExtension implements DynamoDbEnhancedClientExtension {
        private DynamoDbExtensionContext.AfterRead afterReadContext;
        private DynamoDbExtensionContext.BeforeWrite beforeWriteContext;

        public void reset() {
            this.afterReadContext = null;
            this.beforeWriteContext = null;
        }

        public DynamoDbExtensionContext.AfterRead getAfterReadContext() {
            return this.afterReadContext;
        }

        public DynamoDbExtensionContext.BeforeWrite getBeforeWriteContext() {
            return this.beforeWriteContext;
        }

        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            this.beforeWriteContext = context;
            return DynamoDbEnhancedClientExtension.super.beforeWrite(context);
        }

        @Override
        public ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {
            this.afterReadContext = context;
            return DynamoDbEnhancedClientExtension.super.afterRead(context);
        }
    }

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void putItem_subtypeWithVersion_updatesVersion() {
        SubtypeWithVersion record = new SubtypeWithVersion();
        record.setId("123");
        record.setType("with_version");
        record.setAttributeTwo("value");

        mappedTable.putItem(record);

        PolymorphicItemWithVersionSubtype result = mappedTable.getItem(Key.builder().partitionValue("123").build());

        assertThat(result).isInstanceOf(SubtypeWithVersion.class);
        assertThat((SubtypeWithVersion)result).satisfies(typedResult -> {
            assertThat(typedResult.getId()).isEqualTo("123");
            assertThat(typedResult.getType()).isEqualTo("with_version");
            assertThat(typedResult.getAttributeTwo()).isEqualTo("value");
            assertThat(typedResult.getVersion()).isEqualTo(1);
        });
    }

    @Test
    public void putItem_beforeWrite_providesCorrectSubtypeTableSchema() {
        SubtypeWithVersion record = new SubtypeWithVersion();
        record.setId("123");
        record.setType("with_version");
        record.setAttributeTwo("value");

        mappedTable.putItem(record);

        assertThat(fakeExtension.getBeforeWriteContext().tableSchema().itemType())
                .isEqualTo(EnhancedType.of(SubtypeWithVersion.class));
    }

    @Test
    public void updateItem_beforeWrite_providesCorrectSubtypeTableSchema() {
        SubtypeWithVersion record = new SubtypeWithVersion();
        record.setId("123");
        record.setType("with_version");
        record.setAttributeTwo("value");

        mappedTable.updateItem(record);

        assertThat(fakeExtension.getBeforeWriteContext().tableSchema().itemType())
            .isEqualTo(EnhancedType.of(SubtypeWithVersion.class));
    }

    @Test
    public void updateItem_subtypeWithVersion_updatesVersion() {
        SubtypeWithVersion record = new SubtypeWithVersion();
        record.setId("123");
        record.setType("with_version");
        record.setAttributeTwo("value");

        mappedTable.updateItem(record);

        PolymorphicItemWithVersionSubtype result = mappedTable.getItem(Key.builder().partitionValue("123").build());

        assertThat(result).isInstanceOf(SubtypeWithVersion.class);
        assertThat((SubtypeWithVersion)result).satisfies(typedResult -> {
            assertThat(typedResult.getId()).isEqualTo("123");
            assertThat(typedResult.getType()).isEqualTo("with_version");
            assertThat(typedResult.getAttributeTwo()).isEqualTo("value");
            assertThat(typedResult.getVersion()).isEqualTo(1);
        });
    }

    @Test
    public void getItem_subtypeWithVersion_afterReadContextHasCorrectMetadata() {
        SubtypeWithVersion record = new SubtypeWithVersion();
        record.setId("123");
        record.setType("with_version");
        record.setAttributeTwo("value");

        mappedTable.putItem(record);
        fakeExtension.reset();

        mappedTable.getItem(Key.builder().partitionValue("123").build());

        assertThat(fakeExtension.getAfterReadContext().tableMetadata().customMetadata())
                .containsEntry(VERSION_ATTRIBUTE_METADATA_KEY, "version");
    }

    /**
     * If an enhanced write request reads data (such as 'returnValues' in PutItem) the afterRead hook is invoked in
     * extensions. This test ensures that for a polymorphic table schema the correct TableMetadata for the subtype that
     * was actually returned (and not the one written) is used.
     */
    @Test
    public void putItem_returnExistingRecord_afterReadContextHasCorrectMetadata() {
        SubtypeWithVersion record = new SubtypeWithVersion();
        record.setId("123");
        record.setType("with_version");
        record.setAttributeTwo("value1");

        mappedTable.putItem(record);
        fakeExtension.reset();

        SubtypeWithoutVersion newRecord = new SubtypeWithoutVersion();
        newRecord.setId("123");
        newRecord.setType("no_version");
        newRecord.setAttributeOne("value2");

        PutItemEnhancedRequest<PolymorphicItemWithVersionSubtype> enhancedRequest =
                PutItemEnhancedRequest.builder(PolymorphicItemWithVersionSubtype.class)
                                      .returnValues("ALL_OLD")
                                      .item(newRecord)
                                      .build();

        mappedTable.putItem(enhancedRequest);
        assertThat(fakeExtension.getAfterReadContext().tableMetadata().customMetadata())
                .containsEntry(VERSION_ATTRIBUTE_METADATA_KEY, "version");
    }
}
