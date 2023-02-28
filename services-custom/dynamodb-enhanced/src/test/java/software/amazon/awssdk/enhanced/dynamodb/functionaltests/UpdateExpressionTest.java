package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.valueRef;

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
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
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
    private static final List<String> REQUEST_ATTRIBUTE_LIST_INIT_VAL = Arrays.asList("a", "c");
    private static final TableSchema<RecordForUpdateExpressions> TABLE_SCHEMA = TableSchema.fromClass(RecordForUpdateExpressions.class);
    private DynamoDbTable<RecordForUpdateExpressions> mappedTable;

    private void initClientWithExtensionList(DynamoDbEnhancedClientExtension... extensions) {
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
        initClientWithExtensionList(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");

        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    /**
     * This test case represents the most likely extension UpdateExpression use case;
     * an attribute is set in the extensions and isn't present in the request POJO item, and there is no change in
     * the request to set ignoreNull to true.
     */
    @Test
    public void attribute_notInPojo_notFilteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");

        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    @Test
    public void attribute_addedToRequestExpression_noIgnoreNull_updatesNormally() {
        initClientWithExtensionList();
        RecordForUpdateExpressions initialRecord = createBasicRecord("1");
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord("1");
        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .updateExpression(expressionWithSetListElement(1, "b")));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2);
        assertThat(requestAttributeList).containsExactly("a", "b");
    }

    @Test
    public void attribute_addedToRequestExpression_ignoreNulls_updatesNormally() {
        initClientWithExtensionList();
        RecordForUpdateExpressions initialRecord = createBasicRecord("1");
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord("1");
        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .ignoreNulls(true)
                                     .updateExpression(expressionWithSetListElement(1, "b")));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2);
        assertThat(requestAttributeList).containsExactly("a", "b");
    }

    @Test
    public void attribute_inPojo_addedToRequestExpression_ddbError() {
        initClientWithExtensionList();
        RecordForUpdateExpressions initialRecord = createBasicRecord("1");
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions updateRecord = createKeyOnlyRecord("1");
        updateRecord.setRequestAttributeList(Arrays.asList("A"));
        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(updateRecord)
                                                              .updateExpression(expressionWithSetListElement(1, "b"))))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
              .hasMessageContaining("requestAttributeList");
    }

    @Test
    public void attribute_inExtension_addedToRequestExpression_ddbError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(record)
                                                              .updateExpression(expressionWithSetExtensionAttribute())))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths")
            .hasMessageContaining(NUMBER_ATTRIBUTE_REF);
    }

    @Test
    public void attribute_notInPojo_filteredInExtension_ignoresNulls_updatesNormally() {
        initClientWithExtensionList(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void attribute_notInPojo_filteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensionList(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
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
        initClientWithExtensionList(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
        record.setExtensionNumberAttribute(100L);

        verifyDDBError(record, true);
    }

    @Test
    public void attribute_inPojo_notFilteredInExtension_defaultSetsNull_ddbError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
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
        initClientWithExtensionList(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void attribute_inPojo_filteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensionList(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createBasicRecord("1");
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record));

        verifySetAttribute(record);
    }

    @Test
    public void chainedExtensions_noDuplicates_ignoresNulls_updatesNormally() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension(), new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions putRecord = createBasicRecord("1");
        putRecord.setExtensionNumberAttribute(11L);
        putRecord.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        mappedTable.putItem(putRecord);

        RecordForUpdateExpressions updateRecord = createBasicRecord("1");
        updateRecord.setStringAttribute("updated");
        mappedTable.updateItem(r -> r.item(updateRecord).ignoreNulls(true));

        Set<String> expectedSetExtensionAttribute = Stream.of("BLUE", "GREEN").collect(Collectors.toSet());
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(mappedTable.keyFrom(putRecord));
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(16L);
        assertThat(persistedRecord.getExtensionSetAttribute()).isEqualTo(expectedSetExtensionAttribute);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_sameValue_sameValueRef_ddbError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension());
        verifyDDBError(createBasicRecord("1"), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_sameValue_differentValueRef_ddbError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(NUMBER_ATTRIBUTE_VALUE, ":ref"));
        verifyDDBError(createBasicRecord("1"), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_differentValue_differentValueRef_ddbError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(13L, ":ref"));
        verifyDDBError(createBasicRecord("1"), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_differentValue_sameValueRef_operationMergeError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, NUMBER_ATTRIBUTE_VALUE_REF));
        RecordForUpdateExpressions record = createBasicRecord("1");

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(NUMBER_ATTRIBUTE_VALUE_REF);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_invalidValueRef_operationMergeError() {
        initClientWithExtensionList(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, "illegal"));
        RecordForUpdateExpressions record = createBasicRecord("1");

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

    private RecordForUpdateExpressions createKeyOnlyRecord(String id) {
        RecordForUpdateExpressions record = new RecordForUpdateExpressions();
        record.setId(id);
        return record;
    }

    private RecordForUpdateExpressions createBasicRecord(String id) {
        RecordForUpdateExpressions record = new RecordForUpdateExpressions();
        record.setId(id);
        record.setStringAttribute("init");
        record.setRequestAttributeList(REQUEST_ATTRIBUTE_LIST_INIT_VAL);
        return record;
    }

    private void putInitialItemAndVerify(RecordForUpdateExpressions record) {
        mappedTable.putItem(r -> r.item(record));
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2);
        assertThat(requestAttributeList).isEqualTo(REQUEST_ATTRIBUTE_LIST_INIT_VAL);
    }

    private UpdateExpression expressionWithSetListElement(int index, String value) {
        String listAttributeName = "requestAttributeList";
        AttributeValue listElementValue = AttributeValue.builder().s(value).build();
        SetAction setListElement = SetAction.builder()
                                            .path(keyRef(listAttributeName) + "[" + index + "]")
                                            .value(valueRef(listAttributeName))
                                            .putExpressionValue(valueRef(listAttributeName), listElementValue)
                                            .putExpressionName(keyRef(listAttributeName), listAttributeName)
                                            .build();
        return UpdateExpression.builder().addAction(setListElement).build();
    }

    private UpdateExpression expressionWithSetExtensionAttribute() {
        String attributeName = "extensionNumberAttribute";
        AttributeValue elementValue = AttributeValue.builder().n("11").build();
        SetAction setAttribute = SetAction.builder()
                                          .path(keyRef(attributeName))
                                          .value(valueRef(attributeName))
                                          .putExpressionValue(valueRef(attributeName), elementValue)
                                          .putExpressionName(keyRef(attributeName), attributeName)
                                          .build();
        return UpdateExpression.builder().addAction(setAttribute).build();
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
