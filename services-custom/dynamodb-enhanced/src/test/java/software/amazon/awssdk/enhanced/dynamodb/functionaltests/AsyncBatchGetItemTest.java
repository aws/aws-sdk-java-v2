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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

public class AsyncBatchGetItemTest extends LocalDynamoDbAsyncTestBase {
    private static class Record1 {
        private Integer id;
        private String stringAttr;

        private Integer getId() {
            return id;
        }

        private String getStringAttr() {
            return stringAttr;
        }

        private Record1 setId(Integer id) {
            this.id = id;
            return this;
        }

        private Record1 setStringAttr(String str) {
            this.stringAttr = str;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record1 record1 = (Record1) o;
            return Objects.equals(id, record1.id) && Objects.equals(stringAttr, record1.stringAttr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, stringAttr);
        }
    }

    private static class Record2 {
        private Integer id;
        private String stringAttr;

        private Integer getId() {
            return id;
        }

        private String getStringAttr() {
            return stringAttr;
        }

        private Record2 setId(Integer id) {
            this.id = id;
            return this;
        }

        private Record2 setStringAttr(String str) {
            this.stringAttr = str;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record2 record2 = (Record2) o;
            return Objects.equals(id, record2.id) && Objects.equals(stringAttr, record2.stringAttr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, stringAttr);
        }
    }

    private static final TableSchema<Record1> TABLE_SCHEMA_1 =
        StaticTableSchema.builder(Record1.class)
                         .newItemSupplier(Record1::new)
                         .addAttribute(Integer.class, a -> a.name("id_1")
                                                            .getter(Record1::getId)
                                                            .setter(Record1::setId)
                                                            .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("str_1")
                                                           .getter(Record1::getStringAttr)
                                                           .setter(Record1::setStringAttr))
                         .build();

    private static final TableSchema<Record2> TABLE_SCHEMA_2 =
        StaticTableSchema.builder(Record2.class)
                         .newItemSupplier(Record2::new)
                         .addAttribute(Integer.class, a -> a.name("id_2")
                                                            .getter(Record2::getId)
                                                            .setter(Record2::setId)
                                                            .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("str_1")
                                                           .getter(Record2::getStringAttr)
                                                           .setter(Record2::setStringAttr))
                         .build();

    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient =
        DefaultDynamoDbEnhancedAsyncClient.builder()
                                          .dynamoDbClient(getDynamoDbAsyncClient())
                                          .build();

    private final String tableName1 = getConcreteTableName("table-name-1");
    private final String tableName2 = getConcreteTableName("table-name-2");
    private final DynamoDbAsyncTable<Record1> mappedTable1 = enhancedAsyncClient.table(tableName1, TABLE_SCHEMA_1);
    private final DynamoDbAsyncTable<Record2> mappedTable2 = enhancedAsyncClient.table(tableName2, TABLE_SCHEMA_2);

    private static final List<Record1> RECORDS_1 =
        IntStream.range(0, 2)
                 .mapToObj(i -> new Record1().setId(i).setStringAttr(getStringAttrValue(80_000)))
                 .collect(Collectors.toList());

    private static final List<Record2> RECORDS_2 =
        IntStream.range(0, 2)
                 .mapToObj(i -> new Record2().setId(i).setStringAttr(getStringAttrValue(40_000)))
                 .collect(Collectors.toList());

    @Before
    public void createTable() {
        mappedTable1.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
        mappedTable2.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(tableName1)
                                                               .build()).join();
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(tableName2)
                                                               .build()).join();
    }

    private void insertRecords() {
        RECORDS_1.forEach(record -> mappedTable1.putItem(r -> r.item(record)).join());
        RECORDS_2.forEach(record -> mappedTable2.putItem(r -> r.item(record)).join());
    }

    @Test
    public void getRecordsFromMultipleTables() {
        insertRecords();

        SdkPublisher<BatchGetResultPage> publisher = batchGetResultPageSdkPublisherForBothTables();

        List<BatchGetResultPage> results = drainPublisher(publisher, 1);
        assertThat(results.size(), is(1));
        BatchGetResultPage page = results.get(0);
        assertThat(page.consumedCapacity(), empty());

        List<Record1> record1List = page.resultsForTable(mappedTable1);
        assertThat(record1List.size(), is(2));
        assertThat(record1List, containsInAnyOrder(RECORDS_1.get(0), RECORDS_1.get(1)));

        List<Record2> record2List = page.resultsForTable(mappedTable2);
        assertThat(record2List.size(), is(2));
        assertThat(record2List, containsInAnyOrder(RECORDS_2.get(0), RECORDS_2.get(1)));
    }

    @Test
    public void getRecordsFromMultipleTables_viaFlattenedItems() {
        insertRecords();

        BatchGetResultPagePublisher publisher = batchGetResultPageSdkPublisherForBothTables();

        List<Record1> table1Results = drainPublisher(publisher.resultsForTable(mappedTable1), 2);
        assertThat(table1Results.size(), is(2));
        assertThat(table1Results, containsInAnyOrder(RECORDS_1.toArray()));

        List<Record2> table2Results = drainPublisher(publisher.resultsForTable(mappedTable2), 2);
        assertThat(table1Results.size(), is(2));
        assertThat(table2Results, containsInAnyOrder(RECORDS_2.toArray()));
    }

    @Test
    public void notFoundRecordReturnsNull() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest = requestWithNotFoundRecord();

        SdkPublisher<BatchGetResultPage> publisher = enhancedAsyncClient.batchGetItem(batchGetItemEnhancedRequest);

        List<BatchGetResultPage> results = drainPublisher(publisher, 1);
        assertThat(results.size(), is(1));

        BatchGetResultPage page = results.get(0);
        List<Record1> record1List = page.resultsForTable(mappedTable1);
        assertThat(record1List.size(), is(1));
        assertThat(record1List.get(0).getId(), is(0));

        List<Record2> record2List = page.resultsForTable(mappedTable2);
        assertThat(record2List.size(), is(2));
        assertThat(record2List, containsInAnyOrder(RECORDS_2.get(0), RECORDS_2.get(1)));
    }

    @Test
    public void notFoundRecordReturnsNull_viaFlattenedItems() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest = requestWithNotFoundRecord();

        BatchGetResultPagePublisher publisher = enhancedAsyncClient.batchGetItem(batchGetItemEnhancedRequest);

        List<Record1> resultsForTable1 = drainPublisher(publisher.resultsForTable(mappedTable1), 1);
        assertThat(resultsForTable1.size(), is(1));
        assertThat(resultsForTable1.get(0).getId(), is(0));

        List<Record2> record2List = drainPublisher(publisher.resultsForTable(mappedTable2), 2);
        assertThat(record2List.size(), is(2));
        assertThat(record2List, containsInAnyOrder(RECORDS_2.toArray()));
    }

    @Test
    public void getRecordsFromMultipleTables_withReturnConsumedCapacity() {
        insertRecords();

        SdkPublisher<BatchGetResultPage> publisher = batchGetResultPageSdkPublisherForBothTables(ReturnConsumedCapacity.TOTAL);

        List<BatchGetResultPage> results = drainPublisher(publisher, 1);
        assertThat(results.size(), is(1));
        BatchGetResultPage page = results.get(0);
        assertThat(page.consumedCapacity(), not(nullValue()));
        assertThat(page.consumedCapacity(), hasSize(2));

        assertThat(page.consumedCapacity(), containsInAnyOrder(
            ConsumedCapacity.builder().tableName(tableName1).capacityUnits(20.0).build(),
            ConsumedCapacity.builder().tableName(tableName2).capacityUnits(10.0).build()
        ));

        List<Record1> record1List = page.resultsForTable(mappedTable1);
        assertThat(record1List.size(), is(2));
        assertThat(record1List, containsInAnyOrder(RECORDS_1.get(0), RECORDS_1.get(1)));

        List<Record2> record2List = page.resultsForTable(mappedTable2);
        assertThat(record2List.size(), is(2));
        assertThat(record2List, containsInAnyOrder(RECORDS_2.get(0), RECORDS_2.get(1)));
    }

    @Test
    public void notFoundRecords_withReturnConsumedCapacity() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest =
            requestWithNotFoundRecord().toBuilder()
                                       .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                       .build();

        SdkPublisher<BatchGetResultPage> publisher = enhancedAsyncClient.batchGetItem(batchGetItemEnhancedRequest);

        List<BatchGetResultPage> results = drainPublisher(publisher, 1);
        assertThat(results.size(), is(1));
        BatchGetResultPage page = results.get(0);
        assertThat(page.consumedCapacity(), containsInAnyOrder(
            ConsumedCapacity.builder().tableName(tableName1).capacityUnits(10.0).build(),
            ConsumedCapacity.builder().tableName(tableName2).capacityUnits(10.0).build()
        ));

        List<Record1> record1List = page.resultsForTable(mappedTable1);
        assertThat(record1List.size(), is(1));
        assertThat(record1List.get(0).getId(), is(0));

        List<Record2> record2List = page.resultsForTable(mappedTable2);
        assertThat(record2List.size(), is(2));
        assertThat(record2List, containsInAnyOrder(RECORDS_2.get(0), RECORDS_2.get(1)));

    }

    private BatchGetItemEnhancedRequest requestWithNotFoundRecord() {
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

    private BatchGetResultPagePublisher batchGetResultPageSdkPublisherForBothTables() {
        return batchGetResultPageSdkPublisherForBothTables(null);
    }

    private BatchGetResultPagePublisher batchGetResultPageSdkPublisherForBothTables(ReturnConsumedCapacity retConsumedCapacity) {
        return enhancedAsyncClient.batchGetItem(
                r -> r.returnConsumedCapacity(retConsumedCapacity)
                      .readBatches(
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
                                     .build()
                      )
        );
    }

    private static String getStringAttrValue(int nChars) {
        char[] bytes = new char[nChars];
        Arrays.fill(bytes, 'a');
        return new String(bytes);
    }
}
