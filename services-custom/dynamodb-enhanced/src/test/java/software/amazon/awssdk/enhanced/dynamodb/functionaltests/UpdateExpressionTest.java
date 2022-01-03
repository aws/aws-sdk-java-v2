package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordForUpdateExpressions;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationName;
import software.amazon.awssdk.enhanced.dynamodb.update.AddUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteUpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.utils.CollectionUtils;

public class UpdateExpressionTest extends LocalDynamoDbSyncTestBase {

    private static final Long ATTRIBUTE1_INCREMENT = 5L;
    private static final Set<String> ATTRIBUTE2_INIT_VALUE = Stream.of("YELLOW", "BLUE", "RED", "GREEN")
                                                                   .collect(Collectors.toSet());
    private static final Set<String> ATTRIBUTE2_DELETE = Stream.of("YELLOW", "RED").collect(Collectors.toSet());

    private static final String ATTRIBUTE1 = "extensionAttribute1";
    private static final String ATTRIBUTE2 = "extensionAttribute2";


    private static final TableSchema<RecordForUpdateExpressions> TABLE_SCHEMA =
            TableSchema.fromClass(RecordForUpdateExpressions.class);


    private DynamoDbTable<RecordForUpdateExpressions> mappedTable;

    private void initClientWithExtensions(DynamoDbEnhancedClientExtension... extensions) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .extensions(extensions)
                                                                      .build();

        mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(getConcreteTableName("table-name")));
    }

    @Test
    public void attribute1NotInPojo_notFilteredInExtension_RequestIgnoresNull() {
        initClientWithExtensions(new NonFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();

        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute1()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionAttribute1()).isEqualTo(5L);
    }

    @Test
    public void attribute1NotInPojo_notFilteredInExtension_IgnoreNullDefaultFalse_HandledByFilter() {
        initClientWithExtensions(new NonFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();

        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute1()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionAttribute1()).isEqualTo(5L);
    }

    @Test
    public void attribute1InPojo_notFilteredInExtension_RequestIgnoresNull_duplicateError() {
        initClientWithExtensions(new NonFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();
        record.setExtensionAttribute1(100L);

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record).ignoreNulls(true)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths")
            .hasMessageContaining(ATTRIBUTE1);
    }

    @Test
    public void attribute1InPojo_notFilteredInExtension_IgnoreNullDefaultFalse_duplicateError() {
        initClientWithExtensions(new NonFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();
        record.setExtensionAttribute1(100L);

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record).ignoreNulls(true)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths")
            .hasMessageContaining(ATTRIBUTE1);
    }

    @Test
    public void attribute2NotInPojo_filteredInExtension_RequestIgnoresNull() {
        initClientWithExtensions(new FilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();
        record.setExtensionAttribute2(ATTRIBUTE2_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute1("init");
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifyAttribute2(record);
    }

    @Test
    public void attribute2NotInPojo_filteredInExtension_IgnoreNullDefaultFalse() {
        initClientWithExtensions(new FilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();
        record.setExtensionAttribute2(ATTRIBUTE2_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute1("init");
        mappedTable.updateItem(r -> r.item(record));

        verifyAttribute2(record);
    }

    @Test
    public void attribute2InPojo_filteredInExtension_RequestIgnoresNull_duplicateError() {
        initClientWithExtensions(new FilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();
        record.setExtensionAttribute2(ATTRIBUTE2_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute1("init");
        record.setExtensionAttribute2(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifyAttribute2(record);
    }

    @Test
    public void attribute2InPojo_filteredInExtension_IgnoreNullDefaultFalse_duplicateError() {
        initClientWithExtensions(new FilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecord();
        record.setExtensionAttribute2(ATTRIBUTE2_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute1("init");
        record.setExtensionAttribute2(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record));

        verifyAttribute2(record);
    }

    @Test
    public void multipleExtensions_RequestIgnoresNull() {
        initClientWithExtensions(new NonFilteringUpdateExtension(), new FilteringUpdateExtension());
        RecordForUpdateExpressions putRecord = createRecord();
        putRecord.setExtensionAttribute1(11L);
        putRecord.setExtensionAttribute2(ATTRIBUTE2_INIT_VALUE);
        mappedTable.putItem(putRecord);

        RecordForUpdateExpressions record = createRecord();
        record.setStringAttribute1("init");
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        Set<String> expectedExtensionAttribute2 = Stream.of("BLUE", "GREEN").collect(Collectors.toSet());
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute1()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionAttribute1()).isEqualTo(16L);
        assertThat(persistedRecord.getExtensionAttribute2()).isEqualTo(expectedExtensionAttribute2);
    }

    private void verifyAttribute2(RecordForUpdateExpressions record) {
        Set<String> expectedExtensionAttribute2 = Stream.of("BLUE", "GREEN").collect(Collectors.toSet());
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute1()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionAttribute1()).isNull();
        assertThat(persistedRecord.getExtensionAttribute2()).isEqualTo(expectedExtensionAttribute2);
    }

    private RecordForUpdateExpressions createRecord() {
        RecordForUpdateExpressions record = new RecordForUpdateExpressions();
        record.setId("1");
        record.setStringAttribute1("init");
        return record;
    }

    private static final class NonFilteringUpdateExtension implements DynamoDbEnhancedClientExtension {

        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            UpdateExpression updateExpression =
                UpdateExpression.builder()
                                .addAction(addToNumericAttribute(ATTRIBUTE1))
                                .build();

            return WriteModification.builder()
                                    .updateExpression(updateExpression)
                                    .build();
        }

        private AddUpdateAction addToNumericAttribute(String attributeName) {
            AttributeValue actualValue = AttributeValue.builder().n(Long.toString(ATTRIBUTE1_INCREMENT)).build();
            String valueName = ":increment";
            return AddUpdateAction.builder()
                                  .path(attributeName)
                                  .value(valueName)
                                  .putExpressionValue(valueName, actualValue)
                                  .build();
        }

    }

    private static final class FilteringUpdateExtension implements DynamoDbEnhancedClientExtension {

        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            Map<String, AttributeValue> transformedItemMap = context.items();

            if ( context.operationName() == OperationName.UPDATE_ITEM) {
                List<String> attributesToFilter = Arrays.asList(ATTRIBUTE2);
                transformedItemMap = CollectionUtils.filterMap(transformedItemMap, e -> !attributesToFilter.contains(e.getKey()));
            }
            UpdateExpression updateExpression =
                UpdateExpression.builder()
                                .addAction(deleteFromList(ATTRIBUTE2))
                                .build();

            return WriteModification.builder()
                                    .updateExpression(updateExpression)
                                    .transformedItem(transformedItemMap)
                                    .build();
        }

        private DeleteUpdateAction deleteFromList(String attributeName) {
            AttributeValue actualValue = AttributeValue.builder().ss(ATTRIBUTE2_DELETE).build();
            String valueName = ":toDelete";
            return DeleteUpdateAction.builder()
                                     .path(attributeName)
                                     .value(valueName)
                                     .putExpressionValue(valueName, actualValue)
                                     .build();
        }
    }
}
