package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.utils.CollectionUtils;

public class UpdateExpressionTest extends LocalDynamoDbSyncTestBase {

    private static final List<String> REQUEST_ATTRIBUTES = new ArrayList<>(Arrays.asList("attr1", "attr2"));

    private static final Set<String> SET_ATTRIBUTE_INIT_VALUE = Stream.of("YELLOW", "BLUE", "RED", "GREEN")
                                                                      .collect(Collectors.toSet());
    private static final Set<String> SET_ATTRIBUTE_DELETE = Stream.of("YELLOW", "RED").collect(Collectors.toSet());

    private static final String NUMBER_ATTRIBUTE_REF = "extensionNumberAttribute";
    private static final long NUMBER_ATTRIBUTE_VALUE = 5L;
    private static final String NUMBER_ATTRIBUTE_VALUE_REF = ":increment_value_ref";
    private static final String SET_ATTRIBUTE_REF = "extensionSetAttribute";

    private static final String TABLE_NAME = "table-name";
    private static final TableSchema<RecordForUpdateExpressions> TABLE_SCHEMA = TableSchema.fromClass(RecordForUpdateExpressions.class);
    private DynamoDbTable<RecordForUpdateExpressions> mappedTable;

    private void initClientWithExtensions(DynamoDbEnhancedClientExtension... extensions) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .extensions(extensions)
                                                                      .build();

        mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), TABLE_SCHEMA);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        getDynamoDbClient().waiter().waitUntilTableExists(r -> r.tableName(getConcreteTableName(TABLE_NAME)));
    }

    @After
    public void deleteTable() {
        if (mappedTable != null) {
            getDynamoDbClient().deleteTable(r -> r.tableName(getConcreteTableName(TABLE_NAME)));
        }
    }

    @Test
    public void attribute_notInPojo_notFilteredInExtension_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();

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
        RecordForUpdateExpressions record = createFullRecord();

        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    @Test
    public void attribute_notInPojo_filteredInExtension_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void attribute_notInPojo_filteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
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
        RecordForUpdateExpressions record = createFullRecord();
        record.setExtensionNumberAttribute(100L);

        verifyDDBError(record, true);
    }

    @Test
    public void attribute_inPojo_notFilteredInExtension_defaultSetsNull_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
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
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void attribute_inPojo_filteredInExtension_defaultSetsNull_updatesNormally() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record));

        verifySetAttribute(record);
    }

    @Test
    public void chainedExtensions_noDuplicates_ignoresNulls_updatesNormally() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions putRecord = createFullRecord();
        putRecord.setExtensionNumberAttribute(11L);
        mappedTable.putItem(putRecord);

        RecordForUpdateExpressions updateRecord = createFullRecord();
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
        verifyDDBError(createFullRecord(), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_sameValue_differentValueRef_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(NUMBER_ATTRIBUTE_VALUE, ":ref"));
        verifyDDBError(createFullRecord(), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_differentValue_differentValueRef_ddbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(13L, ":ref"));
        verifyDDBError(createFullRecord(), false);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_differentValue_sameValueRef_operationMergeError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, NUMBER_ATTRIBUTE_VALUE_REF));
        RecordForUpdateExpressions record = createFullRecord();

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(NUMBER_ATTRIBUTE_VALUE_REF);
    }

    @Test
    public void chainedExtensions_duplicateAttributes_invalidValueRef_operationMergeError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, "illegal"));
        RecordForUpdateExpressions record = createFullRecord();

        assertThatThrownBy(() ->mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("ExpressionAttributeValues contains invalid key")
            .hasMessageContaining("illegal");
    }

    /**
     * Tests that explicit UpdateExpression provided on the request prevents REMOVE actions for the referenced attributes.
     * Normally, null item attributes generate REMOVE actions when ignoreNulls=false. When an UpdateExpression is provided on the
     * request, REMOVE actions are suppressed for attributes referenced in that UpdateExpression to avoid conflicts.
     */
    @Test
    public void updateExpressionInRequest_withoutIgnoreNulls_shouldUpdateSuccessfully() {
        initClientWithExtensions();
        RecordForUpdateExpressions initialRecord = createFullRecord();
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .updateExpression(expressionWithSetListElement(1, "attr3")));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2).containsExactly("attr1", "attr3");
    }

    /**
     * Tests that explicit UpdateExpression provided on the request works with ignoreNulls=true. When ignoreNulls=true, null item
     * attributes are ignored and no REMOVE actions are generated. When an UpdateExpression is provided on the request, it
     * operates independently of the ignoreNulls setting and updates the specified attributes.
     */
    @Test
    public void updateExpressionInRequest_withIgnoreNulls_shouldUpdateSuccessfully() {
        initClientWithExtensions();
        RecordForUpdateExpressions initialRecord = createFullRecord();
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .ignoreNulls(true)
                                     .updateExpression(expressionWithSetListElement(1, "attr3")));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2).containsExactly("attr1", "attr3");
    }

    /**
     * Tests DynamoDbException is thrown when same attribute is referenced both in the POJO item and in an explicit
     * UpdateExpression provided on the request
     */
    @Test
    public void updateExpressionInRequest_whenAttributeAlsoInPojo_shouldThrowConflictError() {
        initClientWithExtensions();
        RecordForUpdateExpressions initialRecord = createFullRecord();
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions updateRecord = createKeyOnlyRecord();
        updateRecord.setRequestAttributeList(Collections.singletonList("attr1"));
        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(updateRecord)
                                                              .updateExpression(expressionWithSetListElement(1, "attr3"))))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths overlap");
    }

    /**
     * Tests DynamoDbException is thrown when same attribute is referenced both in an extension's UpdateExpression and in an
     * explicit UpdateExpression provided on the request.
     */
    @Test
    public void updateExpressionInRequest_whenAttributeAlsoInExtension_shouldThrowDynamoDbError() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions recordForUpdateExpressions = createKeyOnlyRecord();

        // Create an UpdateExpression that conflicts with the extension's UpdateExpression
        // Extension modifies extensionNumberAttribute, so we create a request expression that also modifies it
        UpdateExpression conflictingExpression = UpdateExpression.builder()
                                                                 .addAction(SetAction.builder()
                                                                                     .path("extensionNumberAttribute")
                                                                                     .value(":conflictValue")
                                                                                     .putExpressionValue(":conflictValue",
                                                                                                         AttributeValue.builder().n("99").build())
                                                                                     .build())
                                                                 .build();

        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(recordForUpdateExpressions)
                                                              .updateExpression(conflictingExpression)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths")
            .hasMessageContaining(NUMBER_ATTRIBUTE_REF);
    }

    /**
     * Tests backward compatibility: POJO-only updates should work unchanged. UpdateExpression functionality is opt-in - without
     * providing an UpdateExpression on the request, behavior is identical ad before.
     */
    @Test
    public void backwardCompatibility_pojoOnlyUpdates() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createSimpleRecord();

        // This should work exactly as before - just POJO updates, no extensions or request expressions
        mappedTable.putItem(record);
        record.setExtensionNumberAttribute(100L);
        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(100L);
    }

    /**
     * Tests backward compatibility: Extension-only updates should work unchanged. UpdateExpression functionality is opt-in -
     * without providing an UpdateExpression on the request, behavior is identical as before
     */
    @Test
    public void backwardCompatibility_extensionOnlyUpdates() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createSimpleRecord();

        // This should work exactly as before - extension updates attribute not in POJO
        mappedTable.putItem(record);
        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    /**
     * Tests scan() operation Verifies that scan operations work correctly after update expressions are applied.
     */
    @Test
    public void scanOperation_afterUpdateExpression() {
        initClientWithExtensions();
        RecordForUpdateExpressions record1 = createFullRecord();
        record1.setId("scan1");
        RecordForUpdateExpressions record2 = createFullRecord();
        record2.setId("scan2");

        mappedTable.putItem(record1);
        mappedTable.putItem(record2);

        // Update one record with expression using key-only record to avoid path conflicts
        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        keyRecord.setId("scan1");
        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .updateExpression(expressionWithSetListElement(0, "updated")));

        // Scan and verify both records
        List<RecordForUpdateExpressions> scannedItems = mappedTable.scan().items().stream().collect(Collectors.toList());
        assertThat(scannedItems).hasSize(2);

        RecordForUpdateExpressions updatedRecord = scannedItems.stream()
                                                               .filter(r -> "scan1".equals(r.getId()))
                                                               .findFirst()
                                                               .get();
        assertThat(updatedRecord.getRequestAttributeList().get(0)).isEqualTo("updated");
    }

    /**
     * Tests deleteItem() operation Verifies that items can be deleted after being updated with expressions.
     */
    @Test
    public void deleteItem_afterUpdateExpression() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        // Update with expression using key-only record to avoid path conflicts
        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .updateExpression(expressionWithSetListElement(0, "beforeDelete")));

        // Verify update worked
        RecordForUpdateExpressions updatedRecord = mappedTable.getItem(record);
        assertThat(updatedRecord.getRequestAttributeList().get(0)).isEqualTo("beforeDelete");

        // Delete the item
        mappedTable.deleteItem(record);

        // Verify deletion
        RecordForUpdateExpressions deletedRecord = mappedTable.getItem(record);
        assertThat(deletedRecord).isNull();
    }

    /**
     * Tests batchGetItem() operation Verifies that batch get operations work correctly after update expressions.
     */
    @Test
    public void batchGetItem_afterUpdateExpression() {
        initClientWithExtensions();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .build();

        RecordForUpdateExpressions record1 = createFullRecord();
        record1.setId("batch1");
        RecordForUpdateExpressions record2 = createFullRecord();
        record2.setId("batch2");

        mappedTable.putItem(record1);
        mappedTable.putItem(record2);

        // Update both with expressions using key-only records to avoid path conflicts
        RecordForUpdateExpressions keyRecord1 = createKeyOnlyRecord();
        keyRecord1.setId("batch1");
        mappedTable.updateItem(r -> r.item(keyRecord1)
                                     .updateExpression(expressionWithSetListElement(0, "batch1Updated")));
        RecordForUpdateExpressions keyRecord2 = createKeyOnlyRecord();
        keyRecord2.setId("batch2");
        mappedTable.updateItem(r -> r.item(keyRecord2)
                                     .updateExpression(expressionWithSetListElement(0, "batch2Updated")));

        // Batch get both items
        List<RecordForUpdateExpressions> batchResults = enhancedClient.batchGetItem(r -> r.readBatches(
                                                                          ReadBatch.builder(RecordForUpdateExpressions.class)
                                                                                   .mappedTableResource(mappedTable)
                                                                                   .addGetItem(record1)
                                                                                   .addGetItem(record2)
                                                                                   .build()))
                                                                      .resultsForTable(mappedTable)
                                                                      .stream()
                                                                      .collect(Collectors.toList());

        assertThat(batchResults).hasSize(2);
        assertThat(batchResults.stream().map(r -> r.getRequestAttributeList().get(0)))
            .containsExactlyInAnyOrder("batch1Updated", "batch2Updated");
    }

    /**
     * Tests batchWriteItem() operation Verifies that batch write operations work with items that have update expressions
     * applied.
     */
    @Test
    public void batchWriteItem_withUpdateExpressionItems() {
        initClientWithExtensions();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .build();

        RecordForUpdateExpressions record1 = createFullRecord();
        record1.setId("batchWrite1");
        RecordForUpdateExpressions record2 = createFullRecord();
        record2.setId("batchWrite2");

        // First update with expressions using key-only record to avoid path conflicts
        mappedTable.putItem(record1);
        RecordForUpdateExpressions keyRecord1 = createKeyOnlyRecord();
        keyRecord1.setId("batchWrite1");
        mappedTable.updateItem(r -> r.item(keyRecord1)
                                     .updateExpression(expressionWithSetListElement(0, "preWrite")));

        // Batch write new record and delete updated record
        enhancedClient.batchWriteItem(r -> r.writeBatches(
            WriteBatch.builder(RecordForUpdateExpressions.class)
                      .mappedTableResource(mappedTable)
                      .addPutItem(record2)
                      .addDeleteItem(record1)
                      .build()));

        // Verify results
        assertThat(mappedTable.getItem(record1)).isNull();
        RecordForUpdateExpressions newRecord = mappedTable.getItem(record2);
        assertThat(newRecord).isNotNull();
        assertThat(newRecord.getRequestAttributeList()).containsExactly("attr1", "attr2");
    }

    /**
     * Tests transactGetItems() operation Verifies that transactional get operations work after update expressions.
     */
    @Test
    public void transactGetItems_afterUpdateExpression() {
        initClientWithExtensions();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .build();

        RecordForUpdateExpressions record1 = createFullRecord();
        record1.setId("transact1");
        RecordForUpdateExpressions record2 = createFullRecord();
        record2.setId("transact2");

        mappedTable.putItem(record1);
        mappedTable.putItem(record2);

        // Update with expressions using key-only record to avoid path conflicts
        RecordForUpdateExpressions keyRecord1 = createKeyOnlyRecord();
        keyRecord1.setId("transact1");
        mappedTable.updateItem(r -> r.item(keyRecord1)
                                     .updateExpression(expressionWithSetListElement(0, "transactUpdated")));

        // Transactional get
        List<RecordForUpdateExpressions> transactResults = enhancedClient.transactGetItems(
                                                                             TransactGetItemsEnhancedRequest.builder()
                                                                                                            .addGetItem(mappedTable, record1)
                                                                                                            .addGetItem(mappedTable, record2)
                                                                                                            .build())
                                                                         .stream()
                                                                         .map(doc -> doc.getItem(mappedTable))
                                                                         .collect(Collectors.toList());

        assertThat(transactResults).hasSize(2);
        RecordForUpdateExpressions updatedRecord = transactResults.stream()
                                                                  .filter(r -> "transact1".equals(r.getId()))
                                                                  .findFirst()
                                                                  .get();
        assertThat(updatedRecord.getRequestAttributeList().get(0)).isEqualTo("transactUpdated");
    }

    /**
     * Tests transactWriteItems() operation Verifies that transactional write operations work correctly.
     */
    @Test
    public void transactWriteItems_withUpdateExpression() {
        initClientWithExtensions();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .build();

        RecordForUpdateExpressions record1 = createFullRecord();
        record1.setId("transactWrite1");
        RecordForUpdateExpressions record2 = createFullRecord();
        record2.setId("transactWrite2");

        mappedTable.putItem(record1);

        // Transactional write operations - delete existing item and put new item
        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(mappedTable, record1)
                                             .addPutItem(mappedTable, record2)
                                             .build());

        // Verify both operations succeeded
        RecordForUpdateExpressions deletedRecord = mappedTable.getItem(record1);
        assertThat(deletedRecord).isNull();

        RecordForUpdateExpressions persistedRecord2 = mappedTable.getItem(record2);
        assertThat(persistedRecord2).isNotNull();
        assertThat(persistedRecord2.getRequestAttributeList()).containsExactly("attr1", "attr2");
    }

    /**
     * Tests StaticTableSchema with UpdateExpression extensions
     */
    @Test
    public void staticTableSchema_withUpdateExpressions() {
        TableSchema<RecordForUpdateExpressions> staticSchema = TableSchema.builder(RecordForUpdateExpressions.class)
                                                                          .newItemSupplier(RecordForUpdateExpressions::new)
                                                                          .addAttribute(String.class, a -> a.name("id")
                                                                                                            .getter(RecordForUpdateExpressions::getId)
                                                                                                            .setter(RecordForUpdateExpressions::setId)
                                                                                                            .tags(primaryPartitionKey()))
                                                                          .addAttribute(String.class, a -> a.name(
                                                                              "stringAttribute")
                                                                                                            .getter(RecordForUpdateExpressions::getStringAttribute)
                                                                                                            .setter(RecordForUpdateExpressions::setStringAttribute))
                                                                          .addAttribute(Long.class, a -> a.name(
                                                                              "extensionNumberAttribute")
                                                                                                          .getter(RecordForUpdateExpressions::getExtensionNumberAttribute)
                                                                                                          .setter(RecordForUpdateExpressions::setExtensionNumberAttribute))
                                                                          .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .extensions(new ItemPreservingUpdateExtension())
                                                                      .build();

        String staticTableName = getConcreteTableName("static-table");
        DynamoDbTable<RecordForUpdateExpressions> staticTable = enhancedClient.table(staticTableName, staticSchema);

        try {
            staticTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));

            RecordForUpdateExpressions record = new RecordForUpdateExpressions();
            record.setId("static-test");
            record.setStringAttribute("init");

            staticTable.updateItem(r -> r.item(record));

            RecordForUpdateExpressions persistedRecord = staticTable.getItem(record);
            assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
            assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
        } finally {
            getDynamoDbClient().deleteTable(r -> r.tableName(staticTableName));
        }
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

    /**
     * Creates record with only the partition key (id)
     */
    private RecordForUpdateExpressions createKeyOnlyRecord() {
        RecordForUpdateExpressions record = new RecordForUpdateExpressions();
        record.setId("1");
        return record;
    }

    /**
     * Creates record with POJO attributes (id + stringAttribute)
     */
    private RecordForUpdateExpressions createSimpleRecord() {
        RecordForUpdateExpressions record = new RecordForUpdateExpressions();
        record.setId("1");
        record.setStringAttribute("init");
        return record;
    }

    /**
     * Creates record with POJO + extension + request attributes (requestAttributeList for request UpdateExpressions,
     * extensionSetAttribute for extension UpdateExpressions)
     */
    private RecordForUpdateExpressions createFullRecord() {
        RecordForUpdateExpressions record = createSimpleRecord();
        record.setRequestAttributeList(new ArrayList<>(REQUEST_ATTRIBUTES));
        record.setExtensionSetAttribute(SET_ATTRIBUTE_INIT_VALUE);
        return record;
    }

    private void putInitialItemAndVerify(RecordForUpdateExpressions record) {
        mappedTable.putItem(r -> r.item(record));
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2).isEqualTo(REQUEST_ATTRIBUTES);
    }

    private UpdateExpression expressionWithSetListElement(int index, String value) {
        String listAttributeName = "requestAttributeList";
        String uniqueValueRef = ":val_" + value.replaceAll("[^a-zA-Z0-9]", "_");
        AttributeValue listElementValue = AttributeValue.builder().s(value).build();
        SetAction setListElement = SetAction.builder()
                                            .path(keyRef(listAttributeName) + "[" + index + "]")
                                            .value(uniqueValueRef)
                                            .putExpressionValue(uniqueValueRef, listElementValue)
                                            .putExpressionName(keyRef(listAttributeName), listAttributeName)
                                            .build();
        return UpdateExpression.builder().addAction(setListElement).build();
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
