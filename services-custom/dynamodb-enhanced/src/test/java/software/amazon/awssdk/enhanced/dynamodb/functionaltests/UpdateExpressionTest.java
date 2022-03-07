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
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionConverter;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.utils.CollectionUtils;

public class UpdateExpressionTest extends LocalDynamoDbSyncTestBase {

    private static final Set<String> SET_ATTRIBUTE_INIT_VALUE = Stream.of("YELLOW", "BLUE", "RED", "GREEN")
                                                                      .collect(Collectors.toSet());
    private static final Set<String> SET_ATTRIBUTE_DELETE = Stream.of("YELLOW", "RED").collect(Collectors.toSet());

    private static final String NUMBER_ATTRIBUTE_REF = "extensionNumberAttribute";
    private static final long NUMBER_ATTRIBUTE_VALUE = 5L;
    private static final String NUMBER_ATTRIBUTE_VALUE_REF = ":increment_value_ref";
    private static final String SET_ATTRIBUTE_REF = "extensionSetAttribute";

    private static final TableSchema<RecordForUpdateExpressions> TABLE_SCHEMA = TableSchema.fromClass(RecordForUpdateExpressions.class);
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
    public void attribute_notInPojo_notFilteredInExtension_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();

        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    /**
     * This test case represents the most likely extension UpdateExpression use case;
     * an attribute is set in the extensions and isn't present in the request POJO item, and there is no change in
     * the request to set ignoreNull to true.
     * <p>
     * By default, ignorNull is false, so attributes that aren't set on the request are deleted from the DDB table through
     * the updateItemOperation generating REMOVE actions for those attributes. This is prevented by
     * {@link UpdateItemOperation} using {@link UpdateExpressionConverter#findAttributeNames(UpdateExpression)}
     * to not create REMOVE actions attributes it finds referenced in an extension UpdateExpression.
     * Therefore, this use case updates normally.
     */
    @Test
    public void attribute_notInPojo_notFilteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();

        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    @Test
    public void attribute_notInPojo_filteredInExtension_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void attribute_notInPojo_filteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        mappedTable.updateItem(r -> r.item(record));

        verifySetAttribute(record);
    }

    /**
     * The extension adds an UpdateExpression with the number attribute, and the request
     * results in an UpdateExpression with the number attribute. This causes DDB to reject the request.
     */
    @Test
    public void attribute_inPojo_notFilteredInExtension_ignoresNulls_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();
        record.setExtensionNumberAttribute(100L);

        verifyDDBError(record, true);
    }

    @Test
    public void attribute_inPojo_notFilteredInExtension_defaultSetsNull_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();
        record.setExtensionNumberAttribute(100L);

        verifyDDBError(record, false);
    }

    /**
     * When the extension filters the transact item representing the request POJO attributes and removes the attribute
     * from the POJO if it's there, only the extension UpdateExpression will reference the attribute and no DDB
     * conflict results.
     */
    @Test
    public void attribute_inPojo_filteredInExtension_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void attribute_inPojo_filteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record));

        verifySetAttribute(record);
    }

    @Test
    public void chainedExtensions_noDuplicates_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions putRecord = createRecordWithoutExtensionAttributes();
        putRecord.setExtensionNumberAttribute(11L);
        putRecord.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(putRecord);

        RecordForUpdateExpressions updateRecord = createRecordWithoutExtensionAttributes();
        updateRecord.setStringAttribute("updated");
        mappedTable.updateItem(r -> r.item(updateRecord).ignoreNulls(true));

        Set<String> expectedSetExtensionAttribute = Stream.of("BLUE", "GREEN").collect(Collectors.toSet());
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(mappedTable.keyFrom(putRecord));
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(16L);
        assertThat(persistedRecord.getExtensionSetAttribute()).isEqualTo(expectedSetExtensionAttribute);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_sameValue_sameValueRef_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension());
        verifyDDBError(createRecordWithoutExtensionAttributes(), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_sameValue_differentValueRef_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(NUMBER_ATTRIBUTE_VALUE, ":ref"));
        verifyDDBError(createRecordWithoutExtensionAttributes(), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_differentValue_differentValueRef_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(13L, ":ref"));
        verifyDDBError(createRecordWithoutExtensionAttributes(), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_differentValue_sameValueRef_operationMergeError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, NUMBER_ATTRIBUTE_VALUE_REF));
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(NUMBER_ATTRIBUTE_VALUE_REF);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_invalidValueRef_operationMergeError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, "illegal"));
        RecordForUpdateExpressions record = createRecordWithoutExtensionAttributes();

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("ExpressionAttributeValues contains invalid key")
            .hasMessageContaining("illegal");
    }

    private void verifyDDBError(RecordForUpdateExpressions record, boolean ignoreNulls) {
        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(record).ignoreNulls(ignoreNulls)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths")
            .hasMessageContaining(NUMBER_ATTRIBUTE_REF);
    }

    private void verifySetAttribute(RecordForUpdateExpressions record) {
        Set<String> expectedAttribute = Stream.of("BLUE", "GREEN").collect(Collectors.toSet());
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isNull();
        assertThat(persistedRecord.getExtensionSetAttribute()).isEqualTo(expectedAttribute);
    }

    private RecordForUpdateExpressions createRecordWithoutExtensionAttributes() {
        RecordForUpdateExpressions record = new RecordForUpdateExpressions();
        record.setId("1");
        record.setStringAttribute("init");
        return record;
    }

    private static final class ItemPreservingUpdateExtension implements DynamoDbEnhancedClientExtension {
        private long incrementValue;
        private String valueRef;

        private ItemPreservingUpdateExtension() {
            this(NUMBER_ATTRIBUTE_VALUE, NUMBER_ATTRIBUTE_VALUE_REF);
        }

        private ItemPreservingUpdateExtension(long incrementValue, String valueRef) {
            this.incrementValue = incrementValue;
            this.valueRef = valueRef;
        }

        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            UpdateExpression updateExpression =
                UpdateExpression.builder().addAction(addToNumericAttribute(NUMBER_ATTRIBUTE_REF)).build();

            return WriteModification.builder().updateExpression(updateExpression).build();
        }

        private AddAction addToNumericAttribute(String attributeName) {
            AttributeValue actualValue = AttributeValue.builder().n(Long.toString(incrementValue)).build();
            return AddAction.builder()
                            .path(attributeName)
                            .value(valueRef)
                            .putExpressionValue(valueRef, actualValue)
                            .build();
        }
    }

    private static final class ItemFilteringUpdateExtension implements DynamoDbEnhancedClientExtension {

        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            Map<String, AttributeValue> transformedItemMap = context.items();

            if ( context.operationName() == OperationName.UPDATE_ITEM) {
                List<String> attributesToFilter = Arrays.asList(SET_ATTRIBUTE_REF);
                transformedItemMap = CollectionUtils.filterMap(transformedItemMap, e -> !attributesToFilter.contains(e.getKey()));
            }
            UpdateExpression updateExpression =
                UpdateExpression.builder().addAction(deleteFromList(SET_ATTRIBUTE_REF)).build();

            return WriteModification.builder()
                                    .updateExpression(updateExpression)
                                    .transformedItem(transformedItemMap)
                                    .build();
        }

        private DeleteAction deleteFromList(String attributeName) {
            AttributeValue actualValue = AttributeValue.builder().ss(SET_ATTRIBUTE_DELETE).build();
            String valueName = ":toDelete";
            return DeleteAction.builder()
                               .path(attributeName)
                               .value(valueName)
                               .putExpressionValue(valueName, actualValue)
                               .build();
        }
    }
}
