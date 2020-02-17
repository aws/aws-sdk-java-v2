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

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.numberValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.integerNumberAttribute;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.stringAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.ConditionCheck;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

public class TransactWriteItemsTest extends LocalDynamoDbSyncTestBase {
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
        StaticTableSchema.builder(Record1.class)
                         .newItemSupplier(Record1::new)
                         .attributes(
                             integerNumberAttribute("id_1", Record1::getId, Record1::setId).as(primaryPartitionKey()),
                             stringAttribute("attribute", Record1::getAttribute, Record1::setAttribute))
                         .build();

    private static final TableSchema<Record2> TABLE_SCHEMA_2 =
        StaticTableSchema.builder(Record2.class)
                         .newItemSupplier(Record2::new)
                         .attributes(
                             integerNumberAttribute("id_2", Record2::getId, Record2::setId).as(primaryPartitionKey()),
                             stringAttribute("attribute", Record2::getAttribute, Record2::setAttribute))
                         .build();

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record1> mappedTable1 = enhancedClient.table(getConcreteTableName("table-name-1"), TABLE_SCHEMA_1);
    private DynamoDbTable<Record2> mappedTable2 = enhancedClient.table(getConcreteTableName("table-name-2"), TABLE_SCHEMA_2);

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

    @Test
    public void singlePut() {
        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addPutItem(mappedTable1, Record1.class, r -> r.item(RECORDS_1.get(0)))
                                             .build());

        Record1 record = mappedTable1.getItem(r -> r.key(Key.create(numberValue(0))));
        assertThat(record, is(RECORDS_1.get(0)));
    }

    @Test
    public void multiplePut() {
        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addPutItem(mappedTable1, Record1.class, r -> r.item(RECORDS_1.get(0)))
                                             .addPutItem(mappedTable2, Record2.class, r -> r.item(RECORDS_2.get(0)))
                                             .build());

        Record1 record1 = mappedTable1.getItem(r -> r.key(Key.create(numberValue(0))));
        Record2 record2 = mappedTable2.getItem(r -> r.key(Key.create(numberValue(0))));
        assertThat(record1, is(RECORDS_1.get(0)));
        assertThat(record2, is(RECORDS_2.get(0)));
    }

    @Test
    public void singleUpdate() {
        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addUpdateItem(mappedTable1, Record1.class, r -> r.item(RECORDS_1.get(0)))
                                             .build());

        Record1 record = mappedTable1.getItem(r -> r.key(Key.create(numberValue(0))));
        assertThat(record, is(RECORDS_1.get(0)));
    }

    @Test
    public void multipleUpdate() {
        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addUpdateItem(mappedTable1, Record1.class, r -> r.item(RECORDS_1.get(0)))
                                             .addUpdateItem(mappedTable2, Record2.class, r -> r.item(RECORDS_2.get(0)))
                                             .build());

        Record1 record1 = mappedTable1.getItem(r -> r.key(Key.create(numberValue(0))));
        Record2 record2 = mappedTable2.getItem(r -> r.key(Key.create(numberValue(0))));
        assertThat(record1, is(RECORDS_1.get(0)));
        assertThat(record2, is(RECORDS_2.get(0)));
    }

    @Test
    public void singleDelete() {
        mappedTable1.putItem(Record1.class, r -> r.item(RECORDS_1.get(0)));

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(mappedTable1, r -> r.key(Key.create(numberValue(0))))
                                             .build());

        Record1 record = mappedTable1.getItem(r -> r.key(Key.create(numberValue(0))));
        assertThat(record, is(nullValue()));
    }

    @Test
    public void multipleDelete() {
        mappedTable1.putItem(Record1.class, r -> r.item(RECORDS_1.get(0)));
        mappedTable2.putItem(Record2.class, r -> r.item(RECORDS_2.get(0)));

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addDeleteItem(mappedTable1, r -> r.key(Key.create(numberValue(0))))
                                             .addDeleteItem(mappedTable2, r -> r.key(Key.create(numberValue(0))))
                                             .build());

        Record1 record1 = mappedTable1.getItem(r -> r.key(Key.create(numberValue(0))));
        Record2 record2 = mappedTable2.getItem(r -> r.key(Key.create(numberValue(0))));
        assertThat(record1, is(nullValue()));
        assertThat(record2, is(nullValue()));
    }

    @Test
    public void singleConditionCheck() {
        mappedTable1.putItem(Record1.class, r -> r.item(RECORDS_1.get(0)));

        Expression conditionExpression = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();
        Key key = Key.create(numberValue(0));

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addConditionCheck(mappedTable1, ConditionCheck.builder()
                                                                                            .key(key)
                                                                                            .conditionExpression(conditionExpression)
                                                                                            .build())
                                             .build());
    }

    @Test
    public void multiConditionCheck() {
        mappedTable1.putItem(Record1.class, r -> r.item(RECORDS_1.get(0)));
        mappedTable2.putItem(Record2.class, r -> r.item(RECORDS_2.get(0)));

        Expression conditionExpression = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key1 = Key.create(numberValue(0));
        Key key2 = Key.create(numberValue(0));

        enhancedClient.transactWriteItems(
            TransactWriteItemsEnhancedRequest.builder()
                                             .addConditionCheck(mappedTable1, ConditionCheck.builder()
                                                                                            .key(key1)
                                                                                            .conditionExpression(conditionExpression)
                                                                                            .build())
                                             .addConditionCheck(mappedTable2, ConditionCheck.builder()
                                                                                            .key(key2)
                                                                                            .conditionExpression(conditionExpression)
                                                                                            .build())
                                             .build());
    }

    @Test
    public void mixedCommands() {
        mappedTable1.putItem(Record1.class, r -> r.item(RECORDS_1.get(0)));
        mappedTable2.putItem(Record2.class, r -> r.item(RECORDS_2.get(0)));

        Expression conditionExpression = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key = Key.create(numberValue(0));

        TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest =
            TransactWriteItemsEnhancedRequest.builder()
                                             .addConditionCheck(mappedTable1, ConditionCheck.builder()
                                                                                            .key(key)
                                                                                            .conditionExpression(conditionExpression)
                                                                                            .build())
                                             .addPutItem(mappedTable2, Record2.class, r -> r.item(RECORDS_2.get(1)))
                                             .addUpdateItem(mappedTable1, Record1.class, r -> r.item(RECORDS_1.get(1)))
                                             .addDeleteItem(mappedTable2, r -> r.key(Key.create(numberValue(0))))
                                             .build();

        enhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest);

        assertThat(mappedTable1.getItem(r -> r.key(Key.create(numberValue(1)))), is(RECORDS_1.get(1)));
        assertThat(mappedTable2.getItem(r -> r.key(Key.create(numberValue(0)))), is(nullValue()));
        assertThat(mappedTable2.getItem(r -> r.key(Key.create(numberValue(1)))), is(RECORDS_2.get(1)));
    }

    @Test
    public void mixedCommands_conditionCheckFailsTransaction() {
        mappedTable1.putItem(Record1.class, r -> r.item(RECORDS_1.get(0)));
        mappedTable2.putItem(Record2.class, r -> r.item(RECORDS_2.get(0)));

        Expression conditionExpression = Expression.builder()
                                                    .expression("#attribute = :attribute")
                                                    .expressionValues(singletonMap(":attribute", stringValue("1")))
                                                    .expressionNames(singletonMap("#attribute", "attribute"))
                                                    .build();

        Key key = Key.create(numberValue(0));

        TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest =
            TransactWriteItemsEnhancedRequest.builder()
                                             .addPutItem(mappedTable2, Record2.class, r -> r.item(RECORDS_2.get(1)))
                                             .addUpdateItem(mappedTable1, Record1.class, r -> r.item(RECORDS_1.get(1)))
                                             .addConditionCheck(mappedTable1, ConditionCheck.builder()
                                                                                            .key(key)
                                                                                            .conditionExpression(conditionExpression)
                                                                                            .build())
                                             .addDeleteItem(mappedTable2, r -> r.key(Key.create(numberValue(0))))
                                             .build();

        try {
            enhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest);
            fail("Expected TransactionCanceledException to be thrown");
        } catch(TransactionCanceledException ignored) {
        }

        assertThat(mappedTable1.getItem(r -> r.key(Key.create(numberValue(1)))), is(nullValue()));
        assertThat(mappedTable2.getItem(r -> r.key(Key.create(numberValue(0)))), is(RECORDS_2.get(0)));
        assertThat(mappedTable2.getItem(r -> r.key(Key.create(numberValue(1)))), is(nullValue()));
    }
}

