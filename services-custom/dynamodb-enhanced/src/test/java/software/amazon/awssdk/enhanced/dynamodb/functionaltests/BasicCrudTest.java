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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import java.util.Objects;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class BasicCrudTest extends LocalDynamoDbSyncTestBase {
    private static final String ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS = "a*t:t.r-i#bute3";

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
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(Record::getSort)
                                                           .setter(Record::setSort)
                                                           .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("attribute")
                                                           .getter(Record::getAttribute)
                                                           .setter(Record::setAttribute))
                         .addAttribute(String.class, a -> a.name("attribute2*")
                                                           .getter(Record::getAttribute2)
                                                           .setter(Record::setAttribute2)
                                                           .tags(secondaryPartitionKey("gsi_1")))
                         .addAttribute(String.class, a -> a.name(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                           .getter(Record::getAttribute3)
                                                           .setter(Record::setAttribute3)
                                                           .tags(secondarySortKey("gsi_1")))
                         .build();

    private static final TableSchema<ShortRecord> SHORT_TABLE_SCHEMA =
        StaticTableSchema.builder(ShortRecord.class)
                         .newItemSupplier(ShortRecord::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(ShortRecord::getId)
                                                           .setter(ShortRecord::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(ShortRecord::getSort)
                                                           .setter(ShortRecord::setSort)
                                                           .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("attribute")
                                                           .getter(ShortRecord::getAttribute)
                                                           .setter(ShortRecord::setAttribute))
                         .build();

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private DynamoDbTable<ShortRecord> mappedShortTable = enhancedClient.table(getConcreteTableName("table-name"),
                                                                               SHORT_TABLE_SCHEMA);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())
                .globalSecondaryIndices(
                        EnhancedGlobalSecondaryIndex.builder()
                                .indexName("gsi_1")
                                .projection(p -> p.projectionType(ProjectionType.ALL))
                                .provisionedThroughput(getDefaultProvisionedThroughput())
                                .build()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void putThenGetItemUsingKey() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(r -> r.item(record));
        
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));

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

        mappedTable.putItem(r -> r.item(record));

        Record keyItem = new Record();
        keyItem.setId("id-value");
        keyItem.setSort("sort-value");

        Record result = mappedTable.getItem(keyItem);

        assertThat(result, is(record));
    }

    @Test
    public void getNonExistentItem() {
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));
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

        mappedTable.putItem(r -> r.item(record));
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four")
                               .setAttribute2("five")
                               .setAttribute3("six");

        mappedTable.putItem(r -> r.item(record2));
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));

        assertThat(result, is(record2));
    }

    @Test
    public void putThenDeleteItem_usingShortcutForm() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(record);
        Record beforeDeleteResult =
            mappedTable.deleteItem(Key.builder().partitionValue("id-value").sortValue("sort-value").build());
        Record afterDeleteResult =
            mappedTable.getItem(Key.builder().partitionValue("id-value").sortValue("sort-value").build());

        assertThat(beforeDeleteResult, is(record));
        assertThat(afterDeleteResult, is(nullValue()));
    }

    @Test
    public void putThenDeleteItem_usingKeyItemForm() {
        Record record = new Record()
            .setId("id-value")
            .setSort("sort-value")
            .setAttribute("one")
            .setAttribute2("two")
            .setAttribute3("three");

        mappedTable.putItem(record);
        Record beforeDeleteResult =
            mappedTable.deleteItem(record);
        Record afterDeleteResult =
            mappedTable.getItem(Key.builder().partitionValue("id-value").sortValue("sort-value").build());

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

        mappedTable.putItem(r -> r.item(record));
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(record)
                                                  .conditionExpression(conditionExpression).build());

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));
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

        mappedTable.putItem(r -> r.item(record));
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(record)
                                                  .conditionExpression(conditionExpression).build());
    }

    @Test
    public void deleteNonExistentItem() {
        Record result = mappedTable.deleteItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));
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

        mappedTable.putItem(r -> r.item(record));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        Key key = mappedTable.keyFrom(record);
        mappedTable.deleteItem(DeleteItemEnhancedRequest.builder().key(key).conditionExpression(conditionExpression).build());

        Record result = mappedTable.getItem(r -> r.key(key));
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

        mappedTable.putItem(r -> r.item(record));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.deleteItem(DeleteItemEnhancedRequest.builder().key(mappedTable.keyFrom(record))
                                                        .conditionExpression(conditionExpression)
                                                        .build());
    }

    @Test
    public void updateOverwriteCompleteRecord_usingShortcutForm() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one")
                              .setAttribute2("two")
                              .setAttribute3("three");

        mappedTable.putItem(record);
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four")
                               .setAttribute2("five")
                               .setAttribute3("six");
        Record result = mappedTable.updateItem(record2);

        assertThat(result, is(record2));
    }

    @Test
    public void updateCreatePartialRecord() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value")
                              .setAttribute("one");

        Record result = mappedTable.updateItem(r -> r.item(record));

        assertThat(result, is(record));
    }

    @Test
    public void updateCreateKeyOnlyRecord() {
        Record record = new Record()
                              .setId("id-value")
                              .setSort("sort-value");

        Record result = mappedTable.updateItem(r -> r.item(record));
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

        mappedTable.putItem(r -> r.item(record));
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four");
        Record result = mappedTable.updateItem(r -> r.item(record2));

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

        mappedTable.putItem(r -> r.item(record));
        Record record2 = new Record()
                               .setId("id-value")
                               .setSort("sort-value")
                               .setAttribute("four");
        Record result = mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                                        .item(record2)
                                                                        .ignoreNulls(true)
                                                                        .build());

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

        mappedTable.putItem(r -> r.item(record));
        ShortRecord record2 = new ShortRecord()
                                         .setId("id-value")
                                         .setSort("sort-value")
                                         .setAttribute("four");
        ShortRecord shortResult = mappedShortTable.updateItem(r -> r.item(record2));
        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue(record.getId()).sortValue(record.getSort())));

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

        mappedTable.putItem(r -> r.item(record));
        Record updateRecord = new Record().setId("id-value").setSort("sort-value");

        Record result = mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                                        .item(updateRecord)
                                                                        .ignoreNulls(true)
                                                                        .build());

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

        mappedTable.putItem(r -> r.item(record));
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(record)
                                                        .conditionExpression(conditionExpression)
                                                        .build());

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));
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

        mappedTable.putItem(r -> r.item(record));
        record.setAttribute("four");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(record)
                                                        .conditionExpression(conditionExpression)
                                                        .build());
    }

    @Test
    public void getAShortRecordWithNewModelledFields() {
        ShortRecord shortRecord = new ShortRecord()
                                         .setId("id-value")
                                         .setSort("sort-value")
                                         .setAttribute("one");
        mappedShortTable.putItem(r -> r.item(shortRecord));
        Record expectedRecord = new Record()
                                      .setId("id-value")
                                      .setSort("sort-value")
                                      .setAttribute("one");

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));
        assertThat(result, is(expectedRecord));
    }
}
