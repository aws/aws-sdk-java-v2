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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.secondarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.Attributes.attribute;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class AsyncBasicCrudTest extends LocalDynamoDbAsyncTestBase {
    private static class Record {
        private String id;
        private String sort;
        private String attribute;
        private String attribute2;
        private String attribute3;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private String getSort() {
            return sort;
        }

        private Record setSort(String sort) {
            this.sort = sort;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private Record setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        private String getAttribute2() {
            return attribute2;
        }

        private Record setAttribute2(String attribute2) {
            this.attribute2 = attribute2;
            return this;
        }

        private String getAttribute3() {
            return attribute3;
        }

        private Record setAttribute3(String attribute3) {
            this.attribute3 = attribute3;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort) &&
                   Objects.equals(attribute, record.attribute) &&
                   Objects.equals(attribute2, record.attribute2) &&
                   Objects.equals(attribute3, record.attribute3);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, attribute, attribute2, attribute3);
        }
    }
    
    private static class ShortRecord {
        private String id;
        private String sort;
        private String attribute;

        private String getId() {
            return id;
        }

        private ShortRecord setId(String id) {
            this.id = id;
            return this;
        }

        private String getSort() {
            return sort;
        }

        private ShortRecord setSort(String sort) {
            this.sort = sort;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private ShortRecord setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShortRecord that = (ShortRecord) o;
            return Objects.equals(id, that.id) &&
                   Objects.equals(sort, that.sort) &&
                   Objects.equals(attribute, that.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, attribute);
        }
    }

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                   .newItemSupplier(Record::new)
                   .attributes(
                       attribute("id", TypeToken.of(String.class), Record::getId, Record::setId).as(primaryPartitionKey()),
                       attribute("sort", TypeToken.of(String.class), Record::getSort, Record::setSort).as(primarySortKey()),
                       // This is a DynamoDb reserved word, forces testing of AttributeNames
                       attribute("attribute", TypeToken.of(String.class), Record::getAttribute, Record::setAttribute),
                       // Using tricky characters to force scrubbing of attributeName tokens
                       attribute("*attribute2*", TypeToken.of(String.class), Record::getAttribute2, Record::setAttribute2)
                           .as(secondaryPartitionKey("gsi_1")),
                       attribute("attribute3", TypeToken.of(String.class), Record::getAttribute3, Record::setAttribute3)
                           .as(secondarySortKey("gsi_1")))
                   .build();

    private static final TableSchema<ShortRecord> SHORT_TABLE_SCHEMA =
        StaticTableSchema.builder(ShortRecord.class)
                   .newItemSupplier(ShortRecord::new)
                   .attributes(
                       attribute("id", TypeToken.of(String.class), ShortRecord::getId, ShortRecord::setId).as(primaryPartitionKey()),
                       attribute("sort", TypeToken.of(String.class), ShortRecord::getSort, ShortRecord::setSort).as(primarySortKey()),
                       attribute("attribute", TypeToken.of(String.class), ShortRecord::getAttribute, ShortRecord::setAttribute))
                   .build();

    private DynamoDbEnhancedAsyncClient enhancedAsyncClient =
        DefaultDynamoDbEnhancedAsyncClient.builder()
                                          .dynamoDbClient(getDynamoDbAsyncClient())
                                          .build();

    private DynamoDbAsyncTable<Record> mappedTable = enhancedAsyncClient.table(getConcreteTableName("table-name"),
                                                                               TABLE_SCHEMA);
    private DynamoDbAsyncTable<ShortRecord> mappedShortTable = enhancedAsyncClient.table(getConcreteTableName("table-name"),
                                                                                         SHORT_TABLE_SCHEMA);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())
                                      .globalSecondaryIndices(
                                          GlobalSecondaryIndex.create(
                                              "gsi_1",
                                              Projection.builder().projectionType(ProjectionType.ALL).build(),
                                              getDefaultProvisionedThroughput())))
                   .join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName("table-name"))
                                                               .build())
                                .join();
    }

    @Test
    public void putThenGetItemUsingKey() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(result, is(record));
    }

    @Test
    public void putThenGetItemUsingKeyItem() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(result, is(record));
    }

    @Test
    public void getNonExistentItem() {
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void putTwiceThenGetItem() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four")
                               .setAttribute2("five")
                               .setAttribute3("six");

        mappedTable.putItem(Record.class, r -> r.item(record2)).join();
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(result, is(record2));
    }

    @Test
    public void putThenDeleteItem() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record beforeDeleteResult =
            mappedTable.deleteItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        Record afterDeleteResult =
            mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(beforeDeleteResult, is(record));
        assertThat(afterDeleteResult, is(nullValue()));
    }

    @Test
    public void putWithConditionThatSucceeds() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(record)
                                                  .conditionExpression(conditionExpression)
                                                  .build()).join();

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result, is(record));
    }

    @Test
    public void putWithConditionThatFails() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ConditionalCheckFailedException.class));
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(record)
                                                  .conditionExpression(conditionExpression)
                                                  .build())
                   .join();
    }

    @Test
    public void deleteNonExistentItem() {
        Record result = mappedTable.deleteItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteWithConditionThatSucceeds() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        Key key = mappedTable.keyFrom(record);
        mappedTable.deleteItem(DeleteItemEnhancedRequest.builder()
                                                        .key(key)
                                                        .conditionExpression(conditionExpression)
                                                        .build())
                   .join();

        Record result = mappedTable.getItem(r -> r.key(key)).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteWithConditionThatFails() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ConditionalCheckFailedException.class));
        mappedTable.deleteItem(DeleteItemEnhancedRequest.builder().key(mappedTable.keyFrom(record))
                                                        .conditionExpression(conditionExpression)
                                                        .build()).join();
    }

    @Test
    public void updateOverwriteCompleteRecord() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four")
                               .setAttribute2("five")
                               .setAttribute3("six");
        Record result = mappedTable.updateItem(Record.class, r -> r.item(record2)).join();

        assertThat(result, is(record2));
    }

    @Test
    public void updateCreatePartialRecord() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one");

        Record result = mappedTable.updateItem(Record.class, r -> r.item(record)).join();

        assertThat(result, is(record));
    }

    @Test
    public void updateCreateKeyOnlyRecord() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value");

        Record result = mappedTable.updateItem(Record.class, r -> r.item(record)).join();
        assertThat(result, is(record));
    }

    @Test
    public void updateOverwriteModelledNulls() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four");
        Record result = mappedTable.updateItem(Record.class, r -> r.item(record2)).join();

        assertThat(result, is(record2));
    }

    @Test
    public void updateCanIgnoreNullsAndDoPartialUpdate() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four");
        Record result = mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                                        .item(record2)
                                                                        .ignoreNulls(true)
                                                                        .build())
                                   .join();

        Record expectedResult = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four")
                               .setAttribute2("two")
                               .setAttribute3("three");
        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateShortRecordDoesPartialUpdate() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        ShortRecord record2 = new ShortRecord()
                                         .setId("id-value")
                                         .setSort("sort-value")
                                         .setAttribute("four");
        ShortRecord shortResult = mappedShortTable.updateItem(ShortRecord.class, r -> r.item(record2)).join();
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue(record.getId())
                                                             .sortValue(record.getSort()))).join();

        Record expectedResult = new Record()
                                      .setId("id-value")
                                      .setSort("sort-value")
                                      .setAttribute("four")
                                      .setAttribute2("two")
                                      .setAttribute3("three");
        assertThat(result, is(expectedResult));
        assertThat(shortResult, is(record2));
    }

    @Test
    public void updateKeyOnlyExistingRecordDoesNothing() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        Record updateRecord = new Record().setId("id-value").setSort("sort-value");

        Record result = mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                                        .item(updateRecord)
                                                                        .ignoreNulls(true)
                                                                        .build())
                                   .join();

        assertThat(result, is(record));
    }

    @Test
    public void updateWithConditionThatSucceeds() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(record)
                                                        .conditionExpression(conditionExpression)
                                                        .build())
                   .join();

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result, is(record));
    }

    @Test
    public void updateWithConditionThatFails() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(Record.class, r -> r.item(record)).join();
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ConditionalCheckFailedException.class));
        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(record)
                                                        .conditionExpression(conditionExpression)
                                                        .build())
                   .join();
    }

    @Test
    public void getAShortRecordWithNewModelledFields() {
        ShortRecord shortRecord = new ShortRecord()
                                         .setId("id-value")
                                         .setSort("sort-value")
                                         .setAttribute("one");
        mappedShortTable.putItem(ShortRecord.class, r -> r.item(shortRecord)).join();
        Record expectedRecord = new Record()
                                      .setId("id-value")
                                      .setSort("sort-value")
                                      .setAttribute("one");

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result, is(expectedRecord));
    }
}
