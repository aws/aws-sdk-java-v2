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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class BatchGetItemTest extends LocalDynamoDbSyncTestBase {
    private static class Record1 {
        private Integer id;

        private Integer getId() {
            return id;
        }

        private Record1 setId(Integer id) {
            this.id = id;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record1 record1 = (Record1) o;
            return Objects.equals(id, record1.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static class Record2 {
        private Integer id;

        private Integer getId() {
            return id;
        }

        private Record2 setId(Integer id) {
            this.id = id;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record2 record2 = (Record2) o;
            return Objects.equals(id, record2.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static final TableSchema<Record1> TABLE_SCHEMA_1 =
        StaticTableSchema.builder(Record1.class)
                         .newItemSupplier(Record1::new)
                         .addAttribute(Integer.class, a -> a.name("id_1")
                                                            .getter(Record1::getId)
                                                            .setter(Record1::setId)
                                                            .tags(primaryPartitionKey()))
                         .build();

    private static final TableSchema<Record2> TABLE_SCHEMA_2 =
        StaticTableSchema.builder(Record2.class)
                         .newItemSupplier(Record2::new)
                         .addAttribute(Integer.class, a -> a.name("id_2")
                                                            .getter(Record2::getId)
                                                            .setter(Record2::setId)
                                                            .tags(primaryPartitionKey()))
                         .build();

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record1> mappedTable1 = enhancedClient.table(getConcreteTableName("table-name-1"), TABLE_SCHEMA_1);
    private DynamoDbTable<Record2> mappedTable2 = enhancedClient.table(getConcreteTableName("table-name-2"), TABLE_SCHEMA_2);

    private static final List<Record1> RECORDS_1 =
        IntStream.range(0, 2)
                 .mapToObj(i -> new Record1().setId(i))
                 .collect(Collectors.toList());

    private static final List<Record2> RECORDS_2 =
        IntStream.range(0, 2)
                 .mapToObj(i -> new Record2().setId(i))
                 .collect(Collectors.toList());

    @Before
    public void createTable() {
        mappedTable1.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        mappedTable2.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name-1"))
                                                          .build());
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name-2"))
                                                          .build());
    }

    private void insertRecords() {
        RECORDS_1.forEach(record -> mappedTable1.putItem(r -> r.item(record)));
        RECORDS_2.forEach(record -> mappedTable2.putItem(r -> r.item(record)));
    }

    @Test
    public void getRecordsFromMultipleTables() {
        insertRecords();
        SdkIterable<BatchGetResultPage> results = getBatchGetResultPagesForBothTables();
        assertThat(results.stream().count(), is(1L));

        results.iterator().forEachRemaining((page) -> {
            List<Record1> table1Results = page.resultsForTable(mappedTable1);
            assertThat(table1Results.size(), is(2));
            assertThat(table1Results.get(0).id, is(0));
            assertThat(table1Results.get(1).id, is(1));
            assertThat(page.resultsForTable(mappedTable2).size(), is(2));
        });
    }

    @Test
    public void getRecordsFromMultipleTables_viaFlattenedItems() {
        insertRecords();

        BatchGetResultPageIterable results = getBatchGetResultPagesForBothTables();

        SdkIterable<Record1> recordsList1 = results.resultsForTable(mappedTable1);
        assertThat(recordsList1, containsInAnyOrder(RECORDS_1.toArray()));

        SdkIterable<Record2> recordsList2 = results.resultsForTable(mappedTable2);
        assertThat(recordsList2, containsInAnyOrder(RECORDS_2.toArray()));
    }

    @Test
    public void notFoundRecordIgnored() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest = batchGetItemEnhancedRequestWithNotFoundRecord();

        SdkIterable<BatchGetResultPage> results = enhancedClient.batchGetItem(batchGetItemEnhancedRequest);

        assertThat(results.stream().count(), is(1L));

        results.iterator().forEachRemaining((page) -> {
            List<Record1> mappedTable1Results = page.resultsForTable(mappedTable1);
            assertThat(mappedTable1Results.size(), is(1));
            assertThat(mappedTable1Results.get(0).id, is(0));
            assertThat(page.resultsForTable(mappedTable2).size(), is(2));
        });
    }

    @Test
    public void notFoundRecordIgnored_viaFlattenedItems() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest = batchGetItemEnhancedRequestWithNotFoundRecord();

        BatchGetResultPageIterable pageIterable = enhancedClient.batchGetItem(batchGetItemEnhancedRequest);

        assertThat(pageIterable.stream().count(), is(1L));

        List<Record1> recordsList1 = pageIterable.resultsForTable(mappedTable1).stream().collect(Collectors.toList());
        assertThat(recordsList1, is(RECORDS_1.subList(0, 1)));

        SdkIterable<Record2> recordsList2 = pageIterable.resultsForTable(mappedTable2);
        assertThat(recordsList2, containsInAnyOrder(RECORDS_2.toArray()));
    }

    private BatchGetItemEnhancedRequest batchGetItemEnhancedRequestWithNotFoundRecord() {
        return BatchGetItemEnhancedRequest.builder()
                                          .readBatches(
                                              ReadBatch.builder(Record1.class)
                                                       .mappedTableResource(mappedTable1)
                                                       .addGetItem(r -> r.key(k -> k.partitionValue(0)))
                                                       .build(),
                                              ReadBatch.builder(Record2.class)
                                                       .mappedTableResource(mappedTable2)
                                                       .addGetItem(r -> r.key(k -> k.partitionValue(0)))
                                                       .build(),
                                              ReadBatch.builder(Record2.class)
                                                       .mappedTableResource(mappedTable2)
                                                       .addGetItem(r -> r.key(k -> k.partitionValue(1)))
                                                       .build(),
                                              ReadBatch.builder(Record1.class)
                                                       .mappedTableResource(mappedTable1)
                                                       .addGetItem(r -> r.key(k -> k.partitionValue(5)))
                                                       .build())
                                          .build();
    }

    private BatchGetResultPageIterable getBatchGetResultPagesForBothTables() {
        return enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record1.class)
                     .mappedTableResource(mappedTable1)
                     .addGetItem(i -> i.key(k -> k.partitionValue(0)))
                     .build(),
            ReadBatch.builder(Record2.class)
                     .mappedTableResource(mappedTable2)
                     .addGetItem(i -> i.key(k -> k.partitionValue(0)))
                     .build(),
            ReadBatch.builder(Record2.class)
                     .mappedTableResource(mappedTable2)
                     .addGetItem(i -> i.key(k -> k.partitionValue(1)))
                     .build(),
            ReadBatch.builder(Record1.class)
                     .mappedTableResource(mappedTable1)
                     .addGetItem(i -> i.key(k -> k.partitionValue(1)))
                     .build()));
    }
}

