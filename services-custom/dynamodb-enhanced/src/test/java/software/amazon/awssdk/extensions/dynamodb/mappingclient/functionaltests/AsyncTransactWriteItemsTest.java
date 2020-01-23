/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.numberValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.integerNumber;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.string;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.AsyncMappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.AsyncMappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.DynamoDbAsyncMappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.ConditionCheck;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.CreateTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.DeleteItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.GetItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.TransactWriteItems;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.UpdateItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.WriteTransaction;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class AsyncTransactWriteItemsTest extends LocalDynamoDbAsyncTestBase {
    private static class Record1 {
        private Integer id;
        private String attribute;

        private Integer getId() {
            return id;
        }

        private Record1 setId(Integer id) {
            this.id = id;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private Record1 setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record1 record1 = (Record1) o;
            return Objects.equals(id, record1.id) &&
                   Objects.equals(attribute, record1.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attribute);
        }
    }

    private static class Record2 {
        private Integer id;
        private String attribute;

        private Integer getId() {
            return id;
        }

        private Record2 setId(Integer id) {
            this.id = id;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private Record2 setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record2 record2 = (Record2) o;
            return Objects.equals(id, record2.id) &&
                   Objects.equals(attribute, record2.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attribute);
        }
    }

    private static final TableSchema<Record1> TABLE_SCHEMA_1 =
        TableSchema.builder()
                   .newItemSupplier(Record1::new)
                   .attributes(
                       integerNumber("id_1", Record1::getId, Record1::setId).as(primaryPartitionKey()),
                       string("attribute", Record1::getAttribute, Record1::setAttribute))
                   .build();

    private static final TableSchema<Record2> TABLE_SCHEMA_2 =
        TableSchema.builder()
                   .newItemSupplier(Record2::new)
                   .attributes(
                       integerNumber("id_2", Record2::getId, Record2::setId).as(primaryPartitionKey()),
                       string("attribute", Record2::getAttribute, Record2::setAttribute))
                   .build();

    private AsyncMappedDatabase mappedDatabase = DynamoDbAsyncMappedDatabase.builder()
                                                                            .dynamoDbClient(getDynamoDbAsyncClient())
                                                                            .build();

    private AsyncMappedTable<Record1> mappedTable1 = mappedDatabase.table(getConcreteTableName("table-name-1"),
                                                                          TABLE_SCHEMA_1);
    private AsyncMappedTable<Record2> mappedTable2 = mappedDatabase.table(getConcreteTableName("table-name-2"),
                                                                     TABLE_SCHEMA_2);

    private static final List<Record1> RECORDS_1 =
        IntStream.range(0, 2)
                 .mapToObj(i -> new Record1().setId(i).setAttribute(Integer.toString(i)))
                 .collect(Collectors.toList());

    private static final List<Record2> RECORDS_2 =
        IntStream.range(0, 2)
                 .mapToObj(i -> new Record2().setId(i).setAttribute(Integer.toString(i)))
                 .collect(Collectors.toList());

    @Before
    public void createTable() {
        mappedTable1.execute(CreateTable.of(getDefaultProvisionedThroughput())).join();
        mappedTable2.execute(CreateTable.of(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name-1"))
                                                          .build()).join();
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name-2"))
                                                          .build()).join();
    }

    @Test
    public void singlePut() {
        List<WriteTransaction> writeTransactions =
            singletonList(WriteTransaction.of(mappedTable1, PutItem.of(RECORDS_1.get(0))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        Record1 record = mappedTable1.execute(GetItem.of(Key.of(numberValue(0)))).join();
        assertThat(record, is(RECORDS_1.get(0)));
    }

    @Test
    public void multiplePut() {
        List<WriteTransaction> writeTransactions =
            asList(WriteTransaction.of(mappedTable1, PutItem.of(RECORDS_1.get(0))),
                   WriteTransaction.of(mappedTable2, PutItem.of(RECORDS_2.get(0))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        Record1 record1 = mappedTable1.execute(GetItem.of(Key.of(numberValue(0)))).join();
        Record2 record2 = mappedTable2.execute(GetItem.of(Key.of(numberValue(0)))).join();
        assertThat(record1, is(RECORDS_1.get(0)));
        assertThat(record2, is(RECORDS_2.get(0)));
    }

    @Test
    public void singleUpdate() {
        List<WriteTransaction> writeTransactions =
            singletonList(WriteTransaction.of(mappedTable1, UpdateItem.of(RECORDS_1.get(0))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        Record1 record = mappedTable1.execute(GetItem.of(Key.of(numberValue(0)))).join();
        assertThat(record, is(RECORDS_1.get(0)));
    }

    @Test
    public void multipleUpdate() {
        List<WriteTransaction> writeTransactions =
            asList(WriteTransaction.of(mappedTable1, UpdateItem.of(RECORDS_1.get(0))),
                   WriteTransaction.of(mappedTable2, UpdateItem.of(RECORDS_2.get(0))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        Record1 record1 = mappedTable1.execute(GetItem.of(Key.of(numberValue(0)))).join();
        Record2 record2 = mappedTable2.execute(GetItem.of(Key.of(numberValue(0)))).join();
        assertThat(record1, is(RECORDS_1.get(0)));
        assertThat(record2, is(RECORDS_2.get(0)));
    }

    @Test
    public void singleDelete() {
        mappedTable1.execute(PutItem.of(RECORDS_1.get(0))).join();

        List<WriteTransaction> writeTransactions =
            singletonList(WriteTransaction.of(mappedTable1, DeleteItem.of(Key.of(numberValue(0)))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        Record1 record = mappedTable1.execute(GetItem.of(Key.of(numberValue(0)))).join();
        assertThat(record, is(nullValue()));
    }

    @Test
    public void multipleDelete() {
        mappedTable1.execute(PutItem.of(RECORDS_1.get(0))).join();
        mappedTable2.execute(PutItem.of(RECORDS_2.get(0))).join();

        List<WriteTransaction> writeTransactions =
            asList(WriteTransaction.of(mappedTable1, DeleteItem.of(Key.of(numberValue(0)))),
                   WriteTransaction.of(mappedTable2, DeleteItem.of(Key.of(numberValue(0)))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        Record1 record1 = mappedTable1.execute(GetItem.of(Key.of(numberValue(0)))).join();
        Record2 record2 = mappedTable2.execute(GetItem.of(Key.of(numberValue(0)))).join();
        assertThat(record1, is(nullValue()));
        assertThat(record2, is(nullValue()));
    }

    @Test
    public void singleConditionCheck() {
        mappedTable1.execute(PutItem.of(RECORDS_1.get(0))).join();

        Expression conditionExpression1 = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key1 = Key.of(numberValue(0));
        List<WriteTransaction> writeTransactions =
            singletonList(WriteTransaction.of(mappedTable1, ConditionCheck.of(key1, conditionExpression1)));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();
    }

    @Test
    public void multiConditionCheck() {
        mappedTable1.execute(PutItem.of(RECORDS_1.get(0))).join();
        mappedTable2.execute(PutItem.of(RECORDS_2.get(0))).join();

        Expression conditionExpression1 = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key1 = Key.of(numberValue(0));
        Key key2 = Key.of(numberValue(0));

        List<WriteTransaction> writeTransactions =
            asList(WriteTransaction.of(mappedTable1, ConditionCheck.of(key1, conditionExpression1)),
                   WriteTransaction.of(mappedTable2, ConditionCheck.of(key2, conditionExpression1)));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();
    }

    @Test
    public void mixedCommands() {
        mappedTable1.execute(PutItem.of(RECORDS_1.get(0))).join();
        mappedTable2.execute(PutItem.of(RECORDS_2.get(0))).join();

        Expression conditionExpression1 = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key1 = Key.of(numberValue(0));

        List<WriteTransaction> writeTransactions =
            asList(WriteTransaction.of(mappedTable1, ConditionCheck.of(key1, conditionExpression1)),
                   WriteTransaction.of(mappedTable2, PutItem.of(RECORDS_2.get(1))),
                   WriteTransaction.of(mappedTable1, UpdateItem.of(RECORDS_1.get(1))),
                   WriteTransaction.of(mappedTable2, DeleteItem.of(Key.of(numberValue(0)))));

        mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();

        assertThat(mappedTable1.execute(GetItem.of(Key.of(numberValue(1)))).join(), is(RECORDS_1.get(1)));
        assertThat(mappedTable2.execute(GetItem.of(Key.of(numberValue(0)))).join(), is(nullValue()));
        assertThat(mappedTable2.execute(GetItem.of(Key.of(numberValue(1)))).join(), is(RECORDS_2.get(1)));
    }

    @Test
    public void mixedCommands_conditionCheckFailsTransaction() {
        mappedTable1.execute(PutItem.of(RECORDS_1.get(0))).join();
        mappedTable2.execute(PutItem.of(RECORDS_2.get(0))).join();

        Expression conditionExpression1 = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("1")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key1 = Key.of(numberValue(0));

        List<WriteTransaction> writeTransactions =
            asList(WriteTransaction.of(mappedTable2, PutItem.of(RECORDS_2.get(1))),
                   WriteTransaction.of(mappedTable1, UpdateItem.of(RECORDS_1.get(1))),
                   WriteTransaction.of(mappedTable1, ConditionCheck.of(key1, conditionExpression1)),
                   WriteTransaction.of(mappedTable2, DeleteItem.of(Key.of(numberValue(0)))));

        try {
            mappedDatabase.execute(TransactWriteItems.of(writeTransactions)).join();
            fail("Expected CompletionException to be thrown");
        } catch (CompletionException e) {
            assertThat(e.getCause(), instanceOf(TransactionCanceledException.class));
        }

        assertThat(mappedTable1.execute(GetItem.of(Key.of(numberValue(1)))).join(), is(nullValue()));
        assertThat(mappedTable2.execute(GetItem.of(Key.of(numberValue(0)))).join(), is(RECORDS_2.get(0)));
        assertThat(mappedTable2.execute(GetItem.of(Key.of(numberValue(1)))).join(), is(nullValue()));
    }
}

