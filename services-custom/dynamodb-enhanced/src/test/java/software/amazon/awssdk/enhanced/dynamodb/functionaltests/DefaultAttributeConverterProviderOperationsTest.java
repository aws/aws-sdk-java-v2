/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase.drainPublisher;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ITEM_ID;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.assertReconstructedCollections;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.assertWrittenItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.completeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.completeItemWithNullableNull;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.countersAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.eventsAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.eventsWithNullElementAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.labelsAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.oneCounter;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.oneLabel;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.populatedConverterRecord;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.typedCollectionsDocumentWithNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ConverterRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

/**
 * Tests collection conversion during DynamoDB Enhanced Client table operations.
 * <p>
 * The tests exercise scans, queries, individual writes and reads, batch operations, and transactions with synchronous
 * and asynchronous clients. They verify stored collection forms, reconstructed collection order, explicit and omitted
 * null values, and conversion failures before or during an operation.
 */
public class DefaultAttributeConverterProviderOperationsTest extends LocalDynamoDbTestBase {

    @BeforeAll
    public static void startLocalDynamoDb() {
        localDynamoDb().start();
    }

    @AfterAll
    public static void stopLocalDynamoDbForJunit5() {
        localDynamoDb().stop();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Scan returns stored collection attributes in insertion order")
    public void scan_whenTableContainsCollectionItem_reconstructsLinkedCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            List<ConverterRecord> items = operations.scanItems();

            assertThat(items).hasSize(1);
            assertReconstructedCollections(items.get(0));
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Query returns stored collection attributes in insertion order")
    public void query_whenTableContainsCollectionItem_reconstructsLinkedCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            List<ConverterRecord> items = operations.queryByPartition(ITEM_ID);

            assertThat(items).hasSize(1);
            assertReconstructedCollections(items.get(0));
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Update persists collection attributes and returns them reconstructed")
    public void updateItem_whenRecordHasCollections_writesAndReconstructsCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            ConverterRecord result = operations.updateItem(populatedConverterRecord());

            assertWrittenItem(operations.storedItem());
            assertReconstructedCollections(result);
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Put persists the key and collection attribute forms")
    public void putItem_whenRecordHasCollections_writesKeyAndCollectionAttributeForms(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            assertWrittenItem(operations.storedItem());
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Get returns stored collection attributes in insertion order")
    public void putItemThenGetItem_whenTableContainsCompleteItem_reconstructsLinkedCollections(
        DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            ConverterRecord result = operations.getItem(itemKey());

            assertReconstructedCollections(result);
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Delete returns the previous collection attributes and removes the item")
    public void deleteItem_whenTableContainsCompleteItem_returnsOldCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            ConverterRecord result = operations.deleteItem(itemKey());

            assertReconstructedCollections(result);
            assertThat(operations.getItem(itemKey())).isNull();
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Batch get returns stored collection attributes in insertion order")
    public void batchGetItem_whenTableContainsCompleteItem_reconstructsLinkedCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            List<ConverterRecord> items = operations.batchGetItem(ITEM_ID);

            assertThat(items).hasSize(1);
            assertReconstructedCollections(items.get(0));
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Batch write persists the key and collection attribute forms")
    public void batchWriteItem_whenPutContainsCollections_writesKeyAndCollectionAttributeForms(
        DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.batchWriteItem(populatedConverterRecord());

            assertWrittenItem(operations.storedItem());
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Transact get returns stored collection attributes in insertion order")
    public void transactGetItems_whenTableContainsCompleteItem_reconstructsLinkedCollections(
        DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putItem(populatedConverterRecord());

            List<ConverterRecord> items = operations.transactGetItems(ITEM_ID);

            assertThat(items).hasSize(1);
            assertReconstructedCollections(items.get(0));
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Transact write persists the key and collection attribute forms")
    public void transactWriteItems_whenPutContainsCollections_writesKeyAndCollectionAttributeForms(
        DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.transactWriteItems(populatedConverterRecord());

            assertWrittenItem(operations.storedItem());
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Get fails when a stored list attribute is a map")
    public void getItem_whenStoredEventsAreAMap_throwsIllegalStateException(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            Map<String, AttributeValue> item = new LinkedHashMap<>(completeItem());
            item.put("events", AttributeValue.fromM(Collections.emptyMap()));
            operations.putRawItem(item);

            assertThatThrownBy(() -> operations.getItem(itemKey()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute."
                            + "ListAttributeConverter cannot convert an attribute of type M into the requested type "
                            + "interface java.util.List");
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Put omits a null map attribute and still writes the set and list")
    public void putItem_whenCountersAreNull_omitsCountersAndWritesSetAndList(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            ConverterRecord record = populatedConverterRecord();
            record.setCounters(null);

            operations.putItem(record);

            Map<String, AttributeValue> item = operations.storedItem();
            assertThat(item).doesNotContainKey("counters");
            assertThat(item.get("labels")).isEqualTo(labelsAttribute());
            assertThat(item.get("events")).isEqualTo(eventsAttribute());
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Put stores a null list member and get returns that null")
    public void putItemThenGetItem_whenListContainsNullElement_writesNullMemberAndReadsArrayListWithNull(
        DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            ConverterRecord record = populatedConverterRecord();
            ArrayList<String> events = new ArrayList<>();
            events.add("a");
            events.add(null);
            events.add("b");
            record.setEvents(events);

            operations.putItem(record);

            assertThat(operations.storedItem().get("events")).isEqualTo(eventsWithNullElementAttribute());

            ConverterRecord result = operations.getItem(itemKey());

            assertThat(result.getEvents()).isInstanceOf(ArrayList.class).containsExactly("a", null, "b");
            assertThat(result.getCounters()).isInstanceOf(LinkedHashMap.class).isEqualTo(oneCounter());
            assertThat(result.getLabels()).isEqualTo(oneLabel());
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Put fails before the request when a map value is null")
    public void putItem_whenMapValueIsNull_throwsNullPointerExceptionBeforeRequest(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            ConverterRecord record = populatedConverterRecord();
            LinkedHashMap<String, Integer> counters = new LinkedHashMap<>();
            counters.put("missing", null);
            record.setCounters(counters);

            assertThatThrownBy(() -> operations.putItem(record))
                .isInstanceOf(NullPointerException.class)
                .satisfies(ex -> assertThat(ex.getMessage() == null || ex.getMessage().contains("null")).isTrue());
            assertThat(operations.getItem(itemKey())).isNull();
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Put of a document stores explicit null together with typed collections")
    public void putItem_whenDocumentHasPutNull_writesNullAndTypedCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putDocument(typedCollectionsDocumentWithNull());

            Map<String, AttributeValue> item = operations.storedItem();
            assertThat(item.get("nullable")).isEqualTo(AttributeValue.fromNul(true));
            assertThat(item.get("counters")).isEqualTo(countersAttribute());
            assertThat(item.get("labels")).isEqualTo(labelsAttribute());
            assertThat(item.get("events")).isEqualTo(eventsAttribute());
        } finally {
            operations.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Get leaves a null property unset and reconstructs the collections")
    public void getItem_whenNullableIsDynamoDbNull_skipsSetterAndReconstructsCollections(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = openTable(clientType);
        try {
            operations.putRawItem(completeItemWithNullableNull());

            ConverterRecord result = operations.getItem(itemKey());

            assertThat(result.getNullable()).isNull();
            assertReconstructedCollections(result);
        } finally {
            operations.deleteTable();
        }
    }

    private ConverterRecordOperations openTable(DynamoDbEnhancedClientType clientType) {
        ConverterRecordOperations operations = clientType == DynamoDbEnhancedClientType.SYNC
                                               ? new SyncOperations()
                                               : new AsyncOperations();
        operations.createTable();
        return operations;
    }

    private static Key itemKey() {
        return Key.builder().partitionValue(ITEM_ID).build();
    }

    private static <T> T joinFuture(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException completionException) {
            Throwable cause = completionException.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw completionException;
        }
    }

    private interface ConverterRecordOperations {
        void createTable();

        void deleteTable();

        void putItem(ConverterRecord record);

        ConverterRecord getItem(Key key);

        ConverterRecord updateItem(ConverterRecord record);

        ConverterRecord deleteItem(Key key);

        List<ConverterRecord> scanItems();

        List<ConverterRecord> queryByPartition(String partitionValue);

        List<ConverterRecord> batchGetItem(String partitionValue);

        void batchWriteItem(ConverterRecord record);

        List<ConverterRecord> transactGetItems(String partitionValue);

        void transactWriteItems(ConverterRecord record);

        void putDocument(EnhancedDocument document);

        void putRawItem(Map<String, AttributeValue> item);

        Map<String, AttributeValue> storedItem();
    }

    private final class SyncOperations implements ConverterRecordOperations {
        private final DynamoDbClient dynamoDbClient = localDynamoDb().createClient();
        private final DynamoDbEnhancedClient enhancedClient =
            DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        private final String tableName = getConcreteTableName("table-name");
        private final DynamoDbTable<ConverterRecord> table =
            enhancedClient.table(tableName, TableSchema.fromBean(ConverterRecord.class));
        private final DynamoDbTable<EnhancedDocument> documentTable =
            enhancedClient.table(tableName,
                                 TableSchema.documentSchemaBuilder()
                                            .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id",
                                                                  AttributeValueType.S)
                                            .build());

        @Override
        public void createTable() {
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        }

        @Override
        public void deleteTable() {
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
        }

        @Override
        public void putItem(ConverterRecord record) {
            table.putItem(record);
        }

        @Override
        public ConverterRecord getItem(Key key) {
            return table.getItem(key);
        }

        @Override
        public ConverterRecord updateItem(ConverterRecord record) {
            return table.updateItem(record);
        }

        @Override
        public ConverterRecord deleteItem(Key key) {
            return table.deleteItem(key);
        }

        @Override
        public List<ConverterRecord> scanItems() {
            List<ConverterRecord> items = new ArrayList<>();
            for (ConverterRecord item : table.scan().items()) {
                items.add(item);
            }
            return items;
        }

        @Override
        public List<ConverterRecord> queryByPartition(String partitionValue) {
            List<ConverterRecord> items = new ArrayList<>();
            for (ConverterRecord item : table.query(QueryConditional.keyEqualTo(k -> k.partitionValue(partitionValue)))
                                             .items()) {
                items.add(item);
            }
            return items;
        }

        @Override
        public List<ConverterRecord> batchGetItem(String partitionValue) {
            List<ConverterRecord> items = new ArrayList<>();
            for (ConverterRecord item : enhancedClient.batchGetItem(r -> r.readBatches(
                ReadBatch.builder(ConverterRecord.class)
                         .mappedTableResource(table)
                         .addGetItem(i -> i.key(k -> k.partitionValue(partitionValue)))
                         .build())).resultsForTable(table)) {
                items.add(item);
            }
            return items;
        }

        @Override
        public void batchWriteItem(ConverterRecord record) {
            enhancedClient.batchWriteItem(r -> r.writeBatches(
                WriteBatch.builder(ConverterRecord.class)
                          .mappedTableResource(table)
                          .addPutItem(record)
                          .build()));
        }

        @Override
        public List<ConverterRecord> transactGetItems(String partitionValue) {
            return enhancedClient.transactGetItems(r -> r.addGetItem(table, Key.builder().partitionValue(partitionValue).build())).stream().map(document -> document.getItem(table)).collect(Collectors.toList());
        }

        @Override
        public void transactWriteItems(ConverterRecord record) {
            enhancedClient.transactWriteItems(r -> r.addPutItem(table, record));
        }

        @Override
        public void putDocument(EnhancedDocument document) {
            documentTable.putItem(document);
        }

        @Override
        public void putRawItem(Map<String, AttributeValue> item) {
            dynamoDbClient.putItem(r -> r.tableName(tableName).item(item));
        }

        @Override
        public Map<String, AttributeValue> storedItem() {
            return dynamoDbClient.getItem(r -> r.tableName(tableName)
                                          .key(Collections.singletonMap("id", AttributeValue.fromS(ITEM_ID)))
                                          .consistentRead(true))
                           .item();
        }
    }

    private final class AsyncOperations implements ConverterRecordOperations {
        private final DynamoDbAsyncClient dynamoDbAsyncClient = localDynamoDb().createAsyncClient();
        private final DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
        private final String tableName = getConcreteTableName("table-name");
        private final DynamoDbAsyncTable<ConverterRecord> table =
            enhancedClient.table(tableName, TableSchema.fromBean(ConverterRecord.class));
        private final DynamoDbAsyncTable<EnhancedDocument> documentTable =
            enhancedClient.table(tableName,
                                 TableSchema.documentSchemaBuilder()
                                            .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id",
                                                                  AttributeValueType.S)
                                            .build());

        @Override
        public void createTable() {
            joinFuture(table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())));
        }

        @Override
        public void deleteTable() {
            joinFuture(dynamoDbAsyncClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build()));
        }

        @Override
        public void putItem(ConverterRecord record) {
            joinFuture(table.putItem(record));
        }

        @Override
        public ConverterRecord getItem(Key key) {
            return joinFuture(table.getItem(key));
        }

        @Override
        public ConverterRecord updateItem(ConverterRecord record) {
            return joinFuture(table.updateItem(record));
        }

        @Override
        public ConverterRecord deleteItem(Key key) {
            return joinFuture(table.deleteItem(key));
        }

        @Override
        public List<ConverterRecord> scanItems() {
            return drainPublisher(table.scan().items(), 1);
        }

        @Override
        public List<ConverterRecord> queryByPartition(String partitionValue) {
            return drainPublisher(
                table.query(QueryConditional.keyEqualTo(k -> k.partitionValue(partitionValue))).items(), 1);
        }

        @Override
        public List<ConverterRecord> batchGetItem(String partitionValue) {
            return drainPublisher(enhancedClient.batchGetItem(r -> r.readBatches(
                ReadBatch.builder(ConverterRecord.class)
                         .mappedTableResource(table)
                         .addGetItem(i -> i.key(k -> k.partitionValue(partitionValue)))
                         .build())).resultsForTable(table), 1);
        }

        @Override
        public void batchWriteItem(ConverterRecord record) {
            joinFuture(enhancedClient.batchWriteItem(r -> r.writeBatches(
                WriteBatch.builder(ConverterRecord.class)
                          .mappedTableResource(table)
                          .addPutItem(record)
                          .build())));
        }

        @Override
        public List<ConverterRecord> transactGetItems(String partitionValue) {
            return joinFuture(enhancedClient.transactGetItems(
                r -> r.addGetItem(table, Key.builder().partitionValue(partitionValue).build()))).stream().map(document -> document.getItem(table)).collect(Collectors.toList());
        }

        @Override
        public void transactWriteItems(ConverterRecord record) {
            joinFuture(enhancedClient.transactWriteItems(r -> r.addPutItem(table, record)));
        }

        @Override
        public void putDocument(EnhancedDocument document) {
            joinFuture(documentTable.putItem(document));
        }

        @Override
        public void putRawItem(Map<String, AttributeValue> item) {
            joinFuture(dynamoDbAsyncClient.putItem(r -> r.tableName(tableName).item(item)));
        }

        @Override
        public Map<String, AttributeValue> storedItem() {
            return dynamoDbAsyncClient.getItem(r -> r.tableName(tableName)
                                          .key(Collections.singletonMap("id", AttributeValue.fromS(ITEM_ID)))
                                          .consistentRead(true))
                           .join()
                           .item();
        }
    }
}
