package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.WRITE_IF_NOT_EXISTS;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy.LEGACY;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE;

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
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Functional tests for {@code updateItem} with extension-provided and request-level {@link UpdateExpression}s.
 * <p>
 * <b>{@link UpdateExpressionMergeStrategy} (integration-level cases exercised here)</b>
 * <ul>
 *   <li>LEGACY, same scalar, POJO + request → DynamoDB overlap error</li>
 *   <li>PRIORITIZE_HIGHER_SOURCE, same scalar, POJO + request → request value stored</li>
 *   <li>LEGACY, document root vs nested path, POJO + request → overlap error</li>
 *   <li>PRIORITIZE_HIGHER_SOURCE, document root vs nested → nested request path stored</li>
 *   <li>PRIORITIZE_HIGHER_SOURCE, list, POJO + extension + request → non-overlapping indices compose (e.g. ext[0] + req[1])</li>
 *   <li>PRIORITIZE_HIGHER_SOURCE, list of maps, three sources → request and extension sibling paths both apply when
 *   non-overlapping</li>
 *   <li>LEGACY, extension + request same scalar → overlap error</li>
 *   <li>PRIORITIZE_HIGHER_SOURCE, extension + request same scalar → request wins</li>
 *   <li>PRIORITIZE_HIGHER_SOURCE, disjoint top-level names → both mutations apply</li>
 *   <li>Default merge strategy, extension + request same scalar → same overlap error as LEGACY</li>
 * </ul>
 */
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
    private static final TableSchema<RecordForUpdateExpressions> TABLE_SCHEMA =
        TableSchema.fromClass(RecordForUpdateExpressions.class);
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

    // --- Atomic counter (SET with if_not_exists + add) ---

    @Test
    public void updateItem_givenCounterAbsent_whenIfNotExistsIncrementExpression_thenStoresIncrement() {
        initClientWithExtensions();

        RecordForUpdateExpressions initialRecord = createKeyOnlyRecord();
        initialRecord.setId("atomicCounter1");
        mappedTable.putItem(initialRecord);

        long incrementBy = 30L;

        UpdateExpression updateExpression = UpdateExpression
            .builder()
            .addAction(
                SetAction.builder()
                         .path("incrementedAttribute")
                         .value("if_not_exists(incrementedAttribute, :zero) + :increment")
                         .putExpressionValue(":zero", AttributeValue.builder().n("0").build())
                         .putExpressionValue(
                             ":increment",
                             AttributeValue.builder().n(Long.toString(incrementBy)).build())
                         .build())
            .build();

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        keyRecord.setId("atomicCounter1");

        mappedTable.updateItem(r -> r.item(keyRecord).updateExpression(updateExpression));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        assertThat(persistedRecord.getIncrementedAttribute()).isEqualTo(30L);
    }

    @Test
    public void updateItem_givenCounterPresent_whenIfNotExistsIncrementExpression_thenAddsToExistingValue() {
        initClientWithExtensions();

        RecordForUpdateExpressions initialRecord = createKeyOnlyRecord();
        initialRecord.setId("atomicCounter2");
        initialRecord.setIncrementedAttribute(10L);
        mappedTable.putItem(initialRecord);

        long incrementBy = 30L;

        UpdateExpression updateExpression = UpdateExpression
            .builder()
            .addAction(
                SetAction.builder()
                         .path("incrementedAttribute")
                         .value("if_not_exists(incrementedAttribute, :zero) + :increment")
                         .putExpressionValue(":zero", AttributeValue.builder().n("0").build())
                         .putExpressionValue(
                             ":increment",
                             AttributeValue.builder().n(Long.toString(incrementBy)).build())
                         .build())
            .build();

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        keyRecord.setId("atomicCounter2");

        mappedTable.updateItem(r -> r.item(keyRecord)
                                     .updateExpression(updateExpression));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        assertThat(persistedRecord.getIncrementedAttribute()).isEqualTo(40L);
    }

    // --- Extension vs POJO: preserving / filtering extensions ---

    @Test
    public void updateItem_givenPreservingExtension_attributeAbsentFromPojo_whenIgnoreNullsTrue_thenExtensionFieldsUnchanged() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();

        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    /**
     * Extension-only attributes must survive default update behavior.
     * <p>
     * With {@code ignoreNulls=false}, POJO-null fields would normally produce REMOVE actions. This verifies that attributes
     * referenced by extension expressions are excluded from generated REMOVE actions to avoid self-conflicts.
     */
    @Test
    public void updateItem_givenPreservingExtension_attributeAbsentFromPojo_whenIgnoreNullsFalse_thenRemoveSuppressedForExtensionPath() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();

        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    @Test
    public void updateItem_givenFilteringExtension_attributeAbsentFromPojo_whenIgnoreNullsTrue_thenSetMutationApplied() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void updateItem_givenFilteringExtension_attributeAbsentFromPojo_whenIgnoreNullsFalse_thenSetMutationApplied() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        mappedTable.updateItem(r -> r.item(record));

        verifySetAttribute(record);
    }

    /**
     * The extension adds an UpdateExpression with the number attribute, and the request results in an UpdateExpression with the
     * number attribute. This causes DDB to reject the request.
     */
    @Test
    public void updateItem_givenPreservingExtension_attributeInPojo_whenIgnoreNullsTrue_thenDynamoDbRejectsOverlappingPaths() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        record.setExtensionNumberAttribute(100L);

        verifyDDBError(record, true);
    }

    @Test
    public void updateItem_givenPreservingExtension_attributeInPojo_whenIgnoreNullsFalse_thenDynamoDbRejectsOverlappingPaths() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        record.setExtensionNumberAttribute(100L);

        verifyDDBError(record, false);
    }

    /**
     * When the extension filters the transact item representing the request POJO attributes and removes the attribute from the
     * POJO if it's there, only the extension UpdateExpression will reference the attribute and no DDB conflict results.
     */
    @Test
    public void updateItem_givenFilteringExtension_attributeInPojo_whenIgnoreNullsTrue_thenNoConflict() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record).ignoreNulls(true));

        verifySetAttribute(record);
    }

    @Test
    public void updateItem_givenFilteringExtension_attributeInPojo_whenIgnoreNullsFalse_thenNoConflict() {
        initClientWithExtensions(new ItemFilteringUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setStringAttribute("init");
        record.setExtensionSetAttribute(Stream.of("PURPLE").collect(Collectors.toSet()));
        mappedTable.updateItem(r -> r.item(record));

        verifySetAttribute(record);
    }

    // --- Chained extensions (duplicate vs distinct paths) ---

    @Test
    public void updateItem_givenTwoExtensionsDistinctPaths_whenIgnoreNullsTrue_thenBothMutationsApply() {
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
    public void updateItem_givenDuplicatePreservingExtensions_sameValueAndPlaceholder_whenUpdate_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension());
        verifyDDBError(createFullRecord(), false);
    }

    @Test
    public void updateItem_givenDuplicatePreservingExtensions_sameValueDifferentPlaceholder_whenUpdate_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(),
                                 new ItemPreservingUpdateExtension(NUMBER_ATTRIBUTE_VALUE, ":ref"));
        verifyDDBError(createFullRecord(), false);
    }

    @Test
    public void updateItem_givenDuplicatePreservingExtensions_differentValues_whenUpdate_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(13L, ":ref"));
        verifyDDBError(createFullRecord(), false);
    }

    @Test
    public void updateItem_givenDuplicatePreservingExtensions_conflictingValuesSamePlaceholder_whenUpdate_thenIllegalArgumentException() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(),
                                 new ItemPreservingUpdateExtension(10L, NUMBER_ATTRIBUTE_VALUE_REF));
        RecordForUpdateExpressions record = createFullRecord();

        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attempt to coalesce two expressions with conflicting expression values")
            .hasMessageContaining(NUMBER_ATTRIBUTE_VALUE_REF);
    }

    @Test
    public void updateItem_givenDuplicatePreservingExtensions_invalidPlaceholder_whenUpdate_thenDynamoDbException() {
        initClientWithExtensions(new ItemPreservingUpdateExtension(), new ItemPreservingUpdateExtension(10L, "illegal"));
        RecordForUpdateExpressions record = createFullRecord();

        assertThatThrownBy(() -> mappedTable.updateItem(r -> r.item(record)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("ExpressionAttributeValues contains invalid key")
            .hasMessageContaining("illegal");
    }

    /**
     * Tests that explicit UpdateExpression provided on the request prevents REMOVE actions for the referenced attributes.
     * Normally, null item attributes generate REMOVE actions when ignoreNulls=false. When an UpdateExpression is provided on the
     * request, REMOVE actions are suppressed for attributes referenced in that UpdateExpression to avoid conflicts.
     */
    // --- Request-level UpdateExpression on list attribute (REMOVE suppression) ---
    @Test
    public void updateItem_givenRequestExpressionSetsListIndex_whenIgnoreNullsFalse_thenListUpdatedWithoutRemoveConflict() {
        initClientWithExtensions();
        RecordForUpdateExpressions initialRecord = createFullRecord();
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.updateItem(
            r -> r.item(keyRecord)
                  .updateExpression(updateExpressionSetRequestListElement(1, "attr3")));

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
    public void updateItem_givenRequestExpressionSetsListIndex_whenIgnoreNullsTrue_thenListUpdated() {
        initClientWithExtensions();
        RecordForUpdateExpressions initialRecord = createFullRecord();
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.updateItem(
            r -> r.item(keyRecord)
                  .ignoreNulls(true)
                  .updateExpression(updateExpressionSetRequestListElement(1, "attr3")));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2).containsExactly("attr1", "attr3");
    }

    /**
     * A request expression that targets the same path as the POJO update is rejected by DynamoDB as overlapping paths.
     */
    @Test
    public void updateItem_givenRequestExpressionOverlapsPojoPath_whenUpdate_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions();
        RecordForUpdateExpressions initialRecord = createFullRecord();
        putInitialItemAndVerify(initialRecord);

        RecordForUpdateExpressions updateRecord = createKeyOnlyRecord();
        updateRecord.setRequestAttributeList(Collections.singletonList("attr1"));
        assertThatThrownBy(() -> mappedTable.updateItem(
            r -> r.item(updateRecord)
                  .updateExpression(updateExpressionSetRequestListElement(1, "attr3"))))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths overlap");
    }

    // --- Merge strategy: scalar (POJO vs request) ---

    @Test
    public void updateItem_givenLegacyMergeStrategy_whenPojoAndRequestSetSameScalar_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setExtensionNumberAttribute(100L);
        UpdateExpression reqExpression = updateExpressionSetLongAttribute("extensionNumberAttribute", 200L);

        assertThatThrownBy(() -> mappedTable.updateItem(
            r -> r.item(record)
                  .updateExpression(reqExpression)
                  .updateExpressionMergeStrategy(LEGACY)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths");
    }

    @Test
    public void updateItem_givenPrioritizeHigherSourceMerge_whenPojoAndRequestSetSameScalar_thenRequestValuePersists() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setExtensionNumberAttribute(100L);
        mappedTable.updateItem(
            r -> r.item(record)
                  .updateExpression(updateExpressionSetLongAttribute("extensionNumberAttribute", 200L))
                  .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(200L);
    }

    // --- Merge strategy: list (POJO + extension + request) ---

    @Test
    public void updateItem_givenPrioritizeHigherSourceMerge_whenPojoExtensionAndRequestTouchSameList_thenNonOverlappingIndicesCompose() {
        initClientWithExtensions(new ListFirstElementUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setRequestAttributeList(Arrays.asList("pojo1", "pojo2"));
        mappedTable.updateItem(
            r -> r.item(record)
                  .updateExpression(updateExpressionSetRequestListElement(1, "request1"))
                  .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getRequestAttributeList()).containsExactly("extension0", "request1");
    }

    // --- Merge strategy: document path (root vs nested) ---

    @Test
    public void updateItem_givenLegacyMergeStrategy_whenPojoSetsDocumentRootAndRequestSetsNestedField_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setObjectAttribute(nestedObject("pojoName", "pojoCity"));

        assertThatThrownBy(() -> mappedTable.updateItem(
            r -> r.item(record)
                  .updateExpression(
                      UpdateExpression.builder()
                                      .addAction(
                                          SetAction.builder()
                                                   .path("#objectAttribute.#name")
                                                   .value(":str_requestName")
                                                   .putExpressionName("#objectAttribute", "objectAttribute")
                                                   .putExpressionName("#name", "name")
                                                   .putExpressionValue(
                                                       ":str_requestName",
                                                       AttributeValue.builder().s("requestName").build())
                                                   .build())
                                      .build())
                  .updateExpressionMergeStrategy(LEGACY)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths");
    }

    @Test
    public void updateItem_givenPrioritizeHigherSourceMerge_whenPojoSetsDocumentRootAndRequestSetsNestedField_thenNestedPathPersists() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        RecordForUpdateExpressions afterPut = mappedTable.getItem(record);
        assertThat(afterPut.getObjectAttribute().getCity()).isEqualTo("originCity");

        record.setObjectAttribute(nestedObject("pojoName", "pojoCity"));
        mappedTable.updateItem(
            r -> r.item(record)
                  .updateExpression(
                      UpdateExpression.builder()
                                      .addAction(
                                          SetAction.builder()
                                                   .path("#objectAttribute.#name")
                                                   .value(":str_requestName")
                                                   .putExpressionName("#objectAttribute", "objectAttribute")
                                                   .putExpressionName("#name", "name")
                                                   .putExpressionValue(
                                                       ":str_requestName",
                                                       AttributeValue.builder().s("requestName").build())
                                                   .build())
                                      .build())
                  .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getObjectAttribute().getName()).isEqualTo("requestName");
        assertThat(persistedRecord.getObjectAttribute().getCity()).isEqualTo("originCity");
    }

    // --- Merge strategy: list of maps (three sources) ---

    @Test
    public void updateItem_givenPrioritizeHigherSourceMerge_whenPojoExtensionAndRequestTouchObjectList_thenRequestNestedUpdatePersists() {
        initClientWithExtensions(new ObjectListFirstElementNameUpdateExtension());
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        record.setObjectListAttribute(Arrays.asList(
            nestedObject("pojo0", "pojoCity0"),
            nestedObject("pojo1", "pojoCity1")));
        mappedTable.updateItem(
            r -> r.item(record)
                  .updateExpression(
                      UpdateExpression.builder()
                                      .addAction(
                                          SetAction.builder()
                                                   .path("#objectListAttribute[1].#name")
                                                   .value(":str_requestObject1")
                                                   .putExpressionName("#objectListAttribute", "objectListAttribute")
                                                   .putExpressionName("#name", "name")
                                                   .putExpressionValue(
                                                       ":str_requestObject1",
                                                       AttributeValue.builder().s("requestObject1").build())
                                                   .build())
                                      .build())
                  .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getObjectListAttribute().get(0).getName()).isEqualTo("extensionObject0");
        assertThat(persistedRecord.getObjectListAttribute().get(0).getCity()).isEqualTo("originCity0");
        assertThat(persistedRecord.getObjectListAttribute().get(1).getName()).isEqualTo("requestObject1");
        assertThat(persistedRecord.getObjectListAttribute().get(1).getCity()).isEqualTo("originCity1");
    }

    // --- Merge strategy: extension vs request (same scalar) ---

    @Test
    public void updateItem_givenLegacyMergeStrategy_whenExtensionAndRequestSetSameScalar_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();

        UpdateExpression reqExpression =
            updateExpressionSetLongAttribute("extensionNumberAttribute", 99L);

        assertThatThrownBy(() -> mappedTable.updateItem(
            r -> r.item(keyRecord)
                  .updateExpression(reqExpression)
                  .updateExpressionMergeStrategy(LEGACY)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths");
    }

    @Test
    public void updateItem_givenPrioritizeHigherSourceMerge_whenExtensionAndRequestSetSameScalar_thenRequestValuePersists() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.putItem(keyRecord);

        mappedTable.updateItem(
            r -> r.item(keyRecord)
                  .updateExpression(updateExpressionSetLongAttribute("extensionNumberAttribute", 99L))
                  .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(99L);
    }

    @Test
    public void updateItem_givenPrioritizeHigherSourceMerge_whenDisjointTopLevelNames_thenBothMutationsApply() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        RecordForUpdateExpressions updateRecord = createKeyOnlyRecord();
        updateRecord.setStringAttribute("updated");
        mappedTable.updateItem(
            r -> r.item(updateRecord)
                  .ignoreNulls(true)
                  .updateExpression(updateExpressionSetRequestListElement(0, "reqVal"))
                  .updateExpressionMergeStrategy(PRIORITIZE_HIGHER_SOURCE));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(updateRecord);
        // stringAttribute uses WRITE_IF_NOT_EXISTS in the schema, so an existing value is preserved on update.
        assertThat(persistedRecord.getStringAttribute()).isEqualTo("init");
        assertThat(persistedRecord.getRequestAttributeList().get(0)).isEqualTo("reqVal");
    }

    @Test
    public void updateItem_givenDefaultMergeStrategy_whenExtensionAndRequestSetSameScalar_thenDynamoDbRejectsOverlap() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();

        UpdateExpression reqExpression =
            UpdateExpression.builder()
                            .addAction(
                                SetAction.builder()
                                         .path("extensionNumberAttribute")
                                         .value(":conflictValue")
                                         .putExpressionValue(
                                             ":conflictValue",
                                             AttributeValue.builder().n("99").build())
                                         .build())
                            .build();

        assertThatThrownBy(() -> mappedTable.updateItem(
            r -> r.item(keyRecord).updateExpression(reqExpression)))
            .isInstanceOf(DynamoDbException.class)
            .hasMessageContaining("Two document paths")
            .hasMessageContaining(NUMBER_ATTRIBUTE_REF);
    }

    // --- Backward compatibility (no request-level UpdateExpression) ---

    /**
     * POJO-only updates are unchanged. Request-level UpdateExpression is opt-in; without it, behavior matches earlier releases.
     */
    @Test
    public void updateItem_givenNoRequestExpression_whenPojoOnlyUpdate_thenScalarPersists() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createSimpleRecord();

        // Backward-compatible baseline: POJO update flow without request-level expression.
        mappedTable.putItem(record);
        record.setExtensionNumberAttribute(100L);
        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(100L);
    }

    /**
     * Backward compatibility: extension-only updates are unchanged when no request-level UpdateExpression is supplied.
     */
    @Test
    public void updateItem_givenNoRequestExpression_whenExtensionOnlyUpdate_thenExtensionValuePersists() {
        initClientWithExtensions(new ItemPreservingUpdateExtension());
        RecordForUpdateExpressions record = createSimpleRecord();

        // Backward-compatible baseline: extension-only mutation without request-level expression.
        mappedTable.putItem(record);
        mappedTable.updateItem(r -> r.item(record));

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        assertThat(persistedRecord.getExtensionNumberAttribute()).isEqualTo(5L);
    }

    // --- Other APIs after request-level UpdateExpression (scan, batch, transact) ---

    /**
     * Verifies {@code scan} returns items updated earlier with a request-level UpdateExpression.
     */
    @Test
    public void scan_givenRequestExpressionUpdatedList_whenScan_thenReadItemReflectsUpdate() {
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
        mappedTable.updateItem(
            r -> r.item(keyRecord)
                  .updateExpression(updateExpressionSetRequestListElement(0, "updated")));

        // Scan and verify both records
        List<RecordForUpdateExpressions> scannedItems =
            mappedTable.scan().items().stream().collect(Collectors.toList());
        assertThat(scannedItems).hasSize(2);

        RecordForUpdateExpressions updatedRecord = scannedItems.stream()
                                                               .filter(r -> "scan1".equals(r.getId()))
                                                               .findFirst()
                                                               .get();
        assertThat(updatedRecord.getRequestAttributeList().get(0)).isEqualTo("updated");
    }

    /**
     * Verifies {@code deleteItem} succeeds after an update that used a request-level UpdateExpression.
     */
    @Test
    public void deleteItem_givenRequestExpressionUpdatedList_whenDeleteKey_thenItemAbsent() {
        initClientWithExtensions();
        RecordForUpdateExpressions record = createFullRecord();
        mappedTable.putItem(record);

        // Update with expression using key-only record to avoid path conflicts
        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        mappedTable.updateItem(
            r -> r.item(keyRecord)
                  .updateExpression(updateExpressionSetRequestListElement(0, "beforeDelete")));

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
     * Verifies {@code batchGetItem} returns items updated with request-level UpdateExpressions.
     */
    @Test
    public void batchGetItem_givenRequestExpressionUpdatedTwoItems_whenBatchGet_thenBothReflectUpdates() {
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
        mappedTable.updateItem(
            r -> r.item(keyRecord1)
                  .updateExpression(updateExpressionSetRequestListElement(0, "batch1Updated")));
        RecordForUpdateExpressions keyRecord2 = createKeyOnlyRecord();
        keyRecord2.setId("batch2");
        mappedTable.updateItem(
            r -> r.item(keyRecord2)
                  .updateExpression(updateExpressionSetRequestListElement(0, "batch2Updated")));

        // Batch get both items
        List<RecordForUpdateExpressions> batchResults =
            enhancedClient
                .batchGetItem(
                    r -> r.readBatches(
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
     * Verifies {@code batchWriteItem} with put and delete after a request-level UpdateExpression update.
     */
    @Test
    public void batchWriteItem_givenUpdatedItemAndPutDelete_whenBatchWrite_thenPutVisibleAndDeleteSucceeds() {
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
        mappedTable.updateItem(
            r -> r.item(keyRecord1)
                  .updateExpression(updateExpressionSetRequestListElement(0, "preWrite")));

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
     * Verifies {@code transactGetItems} returns an item updated with a request-level UpdateExpression.
     */
    @Test
    public void transactGetItems_givenRequestExpressionUpdatedItem_whenTransactGet_thenUpdatedFieldReturned() {
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
        mappedTable.updateItem(
            r -> r.item(keyRecord1)
                  .updateExpression(updateExpressionSetRequestListElement(0, "transactUpdated")));

        // Transactional get
        List<RecordForUpdateExpressions> transactResults =
            enhancedClient.transactGetItems(
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
     * Verifies {@code transactWriteItems} delete + put in one transaction.
     */
    @Test
    public void transactWriteItems_givenDeleteAndPutInTransaction_whenExecute_thenOldGoneNewPresent() {
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

    // --- Transact update with request-level UpdateExpression ---

    @Test
    public void transactWriteItems_givenTransactUpdateWithRequestExpression_whenExecute_thenListElementUpdated() {
        initClientWithExtensions();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .build();

        RecordForUpdateExpressions record = createFullRecord();
        record.setId("transactUpdateReqExpr");
        mappedTable.putItem(record);

        RecordForUpdateExpressions keyRecord = createKeyOnlyRecord();
        keyRecord.setId("transactUpdateReqExpr");

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addUpdateItem(
                                                 mappedTable,
                                                 TransactUpdateItemEnhancedRequest
                                                     .builder(RecordForUpdateExpressions.class)
                                                     .item(keyRecord)
                                                     .updateExpression(updateExpressionSetRequestListElement(1, "txn"))
                                                     .build())
                                             .build());

        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(keyRecord);
        assertThat(persistedRecord.getRequestAttributeList()).containsExactly("attr1", "txn");
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
        record.setObjectAttribute(nestedObject("originName", "originCity"));
        record.setObjectListAttribute(Arrays.asList(
            nestedObject("originObject0", "originCity0"),
            nestedObject("originObject1", "originCity1")));
        return record;
    }

    private NestedRecordForUpdateExpressions nestedObject(String name, String city) {
        NestedRecordForUpdateExpressions nestedObject = new NestedRecordForUpdateExpressions();
        nestedObject.setName(name);
        nestedObject.setCity(city);
        return nestedObject;
    }

    private void putInitialItemAndVerify(RecordForUpdateExpressions record) {
        mappedTable.putItem(r -> r.item(record));
        RecordForUpdateExpressions persistedRecord = mappedTable.getItem(record);
        List<String> requestAttributeList = persistedRecord.getRequestAttributeList();
        assertThat(requestAttributeList).hasSize(2).isEqualTo(REQUEST_ATTRIBUTES);
    }

    /**
     * SET {@code requestAttributeList[index]} to a string; uses an expression-attribute name for the list top-level attribute.
     */
    private static UpdateExpression updateExpressionSetRequestListElement(int index, String elementValue) {
        String valueRef = ":val_" + elementValue.replaceAll("[^a-zA-Z0-9]", "_");
        String listAttr = "requestAttributeList";
        String listToken = keyRef(listAttr);
        return UpdateExpression.builder()
                               .addAction(SetAction.builder()
                                                   .path(listToken + "[" + index + "]")
                                                   .value(valueRef)
                                                   .putExpressionValue(
                                                       valueRef,
                                                       AttributeValue.builder().s(elementValue).build())
                                                   .putExpressionName(listToken, listAttr)
                                                   .build())
                               .build();
    }

    /**
     * SET a numeric attribute to {@code numericValue} (placeholder {@code :value_<n>}).
     */
    private static UpdateExpression updateExpressionSetLongAttribute(String attributeName, long numericValue) {
        String valueRef = ":value_" + numericValue;
        return UpdateExpression.builder()
                               .addAction(SetAction.builder()
                                                   .path(attributeName)
                                                   .value(valueRef)
                                                   .putExpressionValue(
                                                       valueRef,
                                                       AttributeValue.builder().n(Long.toString(numericValue)).build())
                                                   .build())
                               .build();
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

            if (context.operationName() == OperationName.UPDATE_ITEM) {
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

    private static final class ListFirstElementUpdateExtension implements DynamoDbEnhancedClientExtension {
        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            return WriteModification.builder()
                                    .updateExpression(expressionWithSetFirstElement())
                                    .build();
        }

        private UpdateExpression expressionWithSetFirstElement() {
            return UpdateExpression
                .builder()
                .addAction(SetAction.builder()
                                    .path("requestAttributeList[0]")
                                    .value(":extensionValue")
                                    .putExpressionValue(":extensionValue", AttributeValue.builder().s("extension0").build())
                                    .build())
                .build();
        }
    }

    private static final class ObjectListFirstElementNameUpdateExtension implements DynamoDbEnhancedClientExtension {
        @Override
        public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
            return WriteModification
                .builder()
                .updateExpression(
                    UpdateExpression
                        .builder()
                        .addAction(
                            SetAction.builder()
                                     .path("#objectListAttribute[0].#name")
                                     .value(":extensionObject0")
                                     .putExpressionName("#objectListAttribute", "objectListAttribute")
                                     .putExpressionName("#name", "name")
                                     .putExpressionValue(":extensionObject0",
                                                         AttributeValue.builder().s("extensionObject0").build())
                                     .build())
                        .build())
                .build();
        }
    }

    @DynamoDbBean
    public static final class RecordForUpdateExpressions {
        private String id;
        private String stringAttribute1;
        private List<String> requestAttributeList;
        private Long extensionAttribute1;
        private Set<String> extensionAttribute2;
        private Long incrementedAttribute;
        private NestedRecordForUpdateExpressions objectAttribute;
        private List<NestedRecordForUpdateExpressions> objectListAttribute;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDbUpdateBehavior(WRITE_IF_NOT_EXISTS)
        public String getStringAttribute() {
            return stringAttribute1;
        }

        public void setStringAttribute(String stringAttribute1) {
            this.stringAttribute1 = stringAttribute1;
        }

        public List<String> getRequestAttributeList() {
            return requestAttributeList;
        }

        public void setRequestAttributeList(List<String> requestAttributeList) {
            this.requestAttributeList = requestAttributeList;
        }

        public Long getExtensionNumberAttribute() {
            return extensionAttribute1;
        }

        public void setExtensionNumberAttribute(Long extensionAttribute1) {
            this.extensionAttribute1 = extensionAttribute1;
        }

        public Set<String> getExtensionSetAttribute() {
            return extensionAttribute2;
        }

        public void setExtensionSetAttribute(Set<String> extensionAttribute2) {
            this.extensionAttribute2 = extensionAttribute2;
        }

        public Long getIncrementedAttribute() {
            return incrementedAttribute;
        }

        public RecordForUpdateExpressions setIncrementedAttribute(Long incrementedAttribute) {
            this.incrementedAttribute = incrementedAttribute;
            return this;
        }

        public NestedRecordForUpdateExpressions getObjectAttribute() {
            return objectAttribute;
        }

        public void setObjectAttribute(NestedRecordForUpdateExpressions objectAttribute) {
            this.objectAttribute = objectAttribute;
        }

        public List<NestedRecordForUpdateExpressions> getObjectListAttribute() {
            return objectListAttribute;
        }

        public void setObjectListAttribute(List<NestedRecordForUpdateExpressions> objectListAttribute) {
            this.objectListAttribute = objectListAttribute;
        }
    }

    @DynamoDbBean
    public static final class NestedRecordForUpdateExpressions {
        private String name;
        private String city;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }
}
