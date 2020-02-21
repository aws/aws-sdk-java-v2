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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.numberValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.integerNumberAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchGetResultPage;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.GetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.PutItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.ReadBatch;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.StaticTableSchema;
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
                         .attributes(integerNumberAttribute("id_1", Record1::getId, Record1::setId).as(primaryPartitionKey()))
                         .build();

    private static final TableSchema<Record2> TABLE_SCHEMA_2 =
            StaticTableSchema.builder(Record2.class)
                             .newItemSupplier(Record2::new)
                             .attributes(integerNumberAttribute("id_2", Record2::getId, Record2::setId).as(primaryPartitionKey()))
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
        mappedTable1.createTable(CreateTableEnhancedRequest.create(getDefaultProvisionedThroughput()));
        mappedTable2.createTable(CreateTableEnhancedRequest.create(getDefaultProvisionedThroughput()));
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
        RECORDS_1.forEach(record -> mappedTable1.putItem(PutItemEnhancedRequest.create(record)));
        RECORDS_2.forEach(record -> mappedTable2.putItem(PutItemEnhancedRequest.create(record)));
    }

    @Test
    public void getRecordsFromMultipleTables() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest =
            BatchGetItemEnhancedRequest.builder()
                                           .readBatches(
                                               ReadBatch.builder(Record1.class)
                                                        .mappedTableResource(mappedTable1)
                                                        .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(0))))
                                                        .build(),
                                               ReadBatch.builder(Record2.class)
                                                        .mappedTableResource(mappedTable2)
                                                        .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(0))))
                                                        .build(),
                                               ReadBatch.builder(Record2.class)
                                                        .mappedTableResource(mappedTable2)
                                                        .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(1))))
                                                        .build(),
                                               ReadBatch.builder(Record1.class)
                                                        .mappedTableResource(mappedTable1)
                                                        .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(1))))
                                                        .build())
                                           .build();

        SdkIterable<BatchGetResultPage> results = enhancedClient.batchGetItem(batchGetItemEnhancedRequest);

        assertThat(results.stream().count(), is(1L));

        results.iterator().forEachRemaining((page) -> {
            List<Record1> table1Results = page.getResultsForTable(mappedTable1);
            assertThat(table1Results.size(), is(2));
            assertThat(table1Results.get(0).id, is(0));
            assertThat(table1Results.get(1).id, is(1));
            assertThat(page.getResultsForTable(mappedTable2).size(), is(2));
        });
    }

    @Test
    public void notFoundRecordIgnored() {
        insertRecords();

        BatchGetItemEnhancedRequest batchGetItemEnhancedRequest =
            BatchGetItemEnhancedRequest.builder()
                                       .readBatches(
                                           ReadBatch.builder(Record1.class)
                                                    .mappedTableResource(mappedTable1)
                                                    .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(0))))
                                                    .build(),
                                           ReadBatch.builder(Record2.class)
                                                    .mappedTableResource(mappedTable2)
                                                    .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(0))))
                                                    .build(),
                                           ReadBatch.builder(Record2.class)
                                                    .mappedTableResource(mappedTable2)
                                                    .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(1))))
                                                    .build(),
                                           ReadBatch.builder(Record1.class)
                                                    .mappedTableResource(mappedTable1)
                                                    .addGetItem(GetItemEnhancedRequest.create(Key.create(numberValue(5))))
                                                    .build())
                                       .build();

        SdkIterable<BatchGetResultPage> results = enhancedClient.batchGetItem(batchGetItemEnhancedRequest);

        assertThat(results.stream().count(), is(1L));

        results.iterator().forEachRemaining((page) -> {
            List<Record1> mappedTable1Results = page.getResultsForTable(mappedTable1);
            assertThat(mappedTable1Results.size(), is(1));
            assertThat(mappedTable1Results.get(0).id, is(0));
            assertThat(page.getResultsForTable(mappedTable2).size(), is(2));
        });
    }
}

