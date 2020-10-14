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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBetween;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttributeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedTestRecord;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class BasicQueryTest extends LocalDynamoDbSyncTestBase {

    private static class Record {
        private String id;
        private Integer sort;
        private Integer value;

        public String getId() {
            return id;
        }

        public Record setId(String id) {
            this.id = id;
            return this;
        }

        public Integer getSort() {
            return sort;
        }

        public Record setSort(Integer sort) {
            this.sort = sort;
            return this;
        }

        public Integer getValue() {
            return value;
        }

        public Record setValue(Integer value) {
            this.value = value;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort) &&
                   Objects.equals(value, record.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, value);
        }
    }

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(Integer.class, a -> a.name("sort")
                                                            .getter(Record::getSort)
                                                            .setter(Record::setSort)
                                                            .tags(primarySortKey()))
                         .addAttribute(Integer.class, a -> a.name("value")
                                                            .getter(Record::getValue)
                                                            .setter(Record::setValue))
                         .build();

    private static final List<Record> RECORDS =
        IntStream.range(0, 10)
                 .mapToObj(i -> new Record().setId("id-value").setSort(i).setValue(i))
                 .collect(Collectors.toList());

    private static final List<NestedTestRecord> NESTED_TEST_RECORDS =
            IntStream.range(0, 10)
                    .mapToObj(i -> {
                        final NestedTestRecord nestedTestRecord = new NestedTestRecord();
                        nestedTestRecord.setOuterAttribOne("id-value-" + i);
                        nestedTestRecord.setSort(i);
                        final InnerAttributeRecord innerAttributeRecord = new InnerAttributeRecord();
                        innerAttributeRecord.setAttribOne("attribOne-"+i);
                        innerAttributeRecord.setAttribTwo(i);
                        nestedTestRecord.setInnerAttributeRecord(innerAttributeRecord);
                        nestedTestRecord.setDotVariable("v"+i);
                        return nestedTestRecord;
                    })
                    .collect(Collectors.toList());

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    private DynamoDbTable<NestedTestRecord> mappedNestedTable = enhancedClient.table(getConcreteTableName("nested-table-name"),
            TableSchema.fromClass(NestedTestRecord.class));

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)));
        NESTED_TEST_RECORDS.forEach(nestedTestRecord -> mappedNestedTable.putItem(r -> r.item(nestedTestRecord)));
    }

    private void insertNestedRecords() {
        NESTED_TEST_RECORDS.forEach(nestedTestRecord -> mappedNestedTable.putItem(r -> r.item(nestedTestRecord)));
    }

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        mappedNestedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));

    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("nested-table-name"))
                                                          .build());
    }

    @Test
    public void queryAllRecordsDefaultSettings_shortcutForm() {
        insertRecords();

        Iterator<Page<Record>> results =
            mappedTable.query(keyEqualTo(k -> k.partitionValue("id-value"))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), is(RECORDS));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withProjection() {
        insertRecords();

        Iterator<Page<Record>> results =
                mappedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                        .attributesToProject("value")
                ).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().size(), is(RECORDS.size()));

        Record firstRecord = page.items().get(0);
        assertThat(firstRecord.id, is(nullValue()));
        assertThat(firstRecord.sort, is(nullValue()));
        assertThat(firstRecord.value, is(0));
    }

    @Test
    public void queryAllRecordsDefaultSettings_shortcutForm_viaItems() {
        insertRecords();

        PageIterable<Record> query = mappedTable.query(keyEqualTo(k -> k.partitionValue("id-value")));
        SdkIterable<Record> results = query.items();

        assertThat(results.stream().collect(Collectors.toList()), is(RECORDS));
    }

    @Test
    public void queryAllRecordsWithFilter() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#value >= :min_value AND #value <= :max_value")
                                          .expressionValues(expressionValues)
                                          .expressionNames(Collections.singletonMap("#value", "value"))
                                          .build();

        Iterator<Page<Record>> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .filterExpression(expression)
                                                  .build())
                       .iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecordsWithFilterAndProjection() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#value >= :min_value AND #value <= :max_value")
                .expressionValues(expressionValues)
                .expressionNames(Collections.singletonMap("#value", "value"))
                .build();

        Iterator<Page<Record>> results =
                mappedTable.query(QueryEnhancedRequest.builder()
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                        .filterExpression(expression)
                        .attributesToProject("value")
                        .build())
                        .iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), hasSize(3));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));

        Record record = page.items().get(0);
        assertThat(record.id, nullValue());
        assertThat(record.sort, nullValue());
        assertThat(record.value, is(3));
    }

    @Test
    public void queryBetween() {
        insertRecords();
        Key fromKey = Key.builder().partitionValue("id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("id-value").sortValue(5).build();
        Iterator<Page<Record>> results = mappedTable.query(r -> r.queryConditional(sortBetween(fromKey, toKey))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryLimit() {
        insertRecords();
        Iterator<Page<Record>> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .limit(5)
                                                  .build())
                       .iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page3 = results.next();
        assertThat(results.hasNext(), is(false));

        Map<String, AttributeValue> expectedLastEvaluatedKey1 = new HashMap<>();
        expectedLastEvaluatedKey1.put("id", stringValue("id-value"));
        expectedLastEvaluatedKey1.put("sort", numberValue(4));
        Map<String, AttributeValue> expectedLastEvaluatedKey2 = new HashMap<>();
        expectedLastEvaluatedKey2.put("id", stringValue("id-value"));
        expectedLastEvaluatedKey2.put("sort", numberValue(9));
        assertThat(page1.items(), is(RECORDS.subList(0, 5)));
        assertThat(page1.lastEvaluatedKey(), is(expectedLastEvaluatedKey1));
        assertThat(page2.items(), is(RECORDS.subList(5, 10)));
        assertThat(page2.lastEvaluatedKey(), is(expectedLastEvaluatedKey2));
        assertThat(page3.items(), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty() {
        Iterator<Page<Record>> results =
            mappedTable.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))).iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty_viaItems() {
        PageIterable<Record> query = mappedTable.query(keyEqualTo(k -> k.partitionValue("id-value")));
        SdkIterable<Record> results = query.items();

        assertThat(results.stream().collect(Collectors.toList()), is(empty()));
    }

    @Test
    public void queryExclusiveStartKey() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));
        insertRecords();
        Iterator<Page<Record>> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .exclusiveStartKey(exclusiveStartKey)
                                                  .build())
                       .iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryExclusiveStartKey_viaItems() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));
        insertRecords();
        SdkIterable<Record> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .exclusiveStartKey(exclusiveStartKey)
                                                  .build())
                       .items();

        assertThat(results.stream().collect(Collectors.toList()), is(RECORDS.subList(8, 10)));
    }

    @Test
    public void queryNestedRecord_SingleAttributeName() {
        insertNestedRecords();
        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                        .addNestedAttributeToProject(NestedAttributeName.builder().addElement("innerAttributeRecord")
                                .addElement("attribOne").build())).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-1"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(nullValue()));
        results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                        .addNestedAttributeToProject(NestedAttributeName.create("sort"))
                .addAttributeToProject("sort")).iterator();
        assertThat(results.hasNext(), is(true));
        page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(1));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));
    }


    @Test
    public void queryNestedRecord_withAttributeNameList() {
        insertNestedRecords();
        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                        .addNestedAttributesToProject(Arrays.asList(
                                NestedAttributeName.builder().elements("innerAttributeRecord", "attribOne").build(),
                                NestedAttributeName.builder().addElement("outerAttribOne").build()))
                .addNestedAttributesToProject(NestedAttributeName.builder()
                        .addElements(Arrays.asList("innerAttributeRecord","attribTwo")).build())).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-1"));
        assertThat(firstRecord.getSort(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-1"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(1));
    }




    @Test
    public void queryNestedRecord_withAttributeNameListAndStringAttributeToProjectAppended() {
        insertNestedRecords();
        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                        .addNestedAttributesToProject(Arrays.asList(
                                NestedAttributeName.builder().elements("innerAttributeRecord","attribOne").build()))
                        .addNestedAttributesToProject(NestedAttributeName.create("innerAttributeRecord","attribTwo"))
                .addAttributeToProject("sort")).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(is(nullValue())));
        assertThat(firstRecord.getSort(), is(1));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-1"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(1));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withNestedProjectionNamesNotInNameMap() {
        insertNestedRecords();

        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                        .addNestedAttributeToProject( NestedAttributeName.builder().addElement("nonExistentSlot").build())).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord, is(nullValue()));
    }
    @Test
    public void queryRecordDefaultSettings_withDotInTheName() {
        insertNestedRecords();
        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                        .addNestedAttributeToProject( NestedAttributeName.create("test.com"))).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(is(nullValue())));
        assertThat(firstRecord.getSort(), is(is(nullValue())));
        assertThat(firstRecord.getInnerAttributeRecord() ,  is(nullValue()));
        assertThat(firstRecord.getDotVariable(), is("v7"));
        Iterator<Page<NestedTestRecord>> resultWithAttributeToProject =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                        .attributesToProject( "test.com").build()).iterator();
        assertThat(resultWithAttributeToProject.hasNext(), is(true));
        Page<NestedTestRecord> pageResult = resultWithAttributeToProject.next();
        assertThat(resultWithAttributeToProject.hasNext(), is(false));
        assertThat(pageResult.items().size(), is(1));
        NestedTestRecord record = pageResult.items().get(0);
        assertThat(record.getOuterAttribOne(), is(is(nullValue())));
        assertThat(record.getSort(), is(is(nullValue())));
        assertThat(firstRecord.getInnerAttributeRecord() ,  is(nullValue()));
        assertThat(record.getDotVariable(), is("v7"));
    }

    @Test
    public void queryRecordDefaultSettings_withEmptyAttributeList() {
        insertNestedRecords();
        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                        .attributesToProject(new ArrayList<>()).build()).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-7"));
        assertThat(firstRecord.getSort(), is(7));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(7));
        assertThat(firstRecord.getDotVariable(), is("v7"));
    }

    @Test
    public void queryRecordDefaultSettings_withNullAttributeList() {
        insertNestedRecords();

        List<String> backwardCompatibilty = null;

        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.query(b -> b
                        .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                        .attributesToProject(backwardCompatibilty).build()).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-7"));
        assertThat(firstRecord.getSort(), is(7));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(7));
        assertThat(firstRecord.getDotVariable(), is("v7"));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withNestedProjectionNameEmptyNameMap() {
        insertNestedRecords();

        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> {
                    Iterator<Page<NestedTestRecord>> results =  mappedNestedTable.query(b -> b.queryConditional(
                            keyEqualTo(k -> k.partitionValue("id-value-3")))
                            .attributesToProject("").build()).iterator();
                    assertThat(results.hasNext(), is(true));
                    Page<NestedTestRecord> page = results.next();
                });

        assertThatExceptionOfType(Exception.class).isThrownBy(
                () -> {
                    Iterator<Page<NestedTestRecord>> results =  mappedNestedTable.query(b -> b.queryConditional(
                            keyEqualTo(k -> k.partitionValue("id-value-3")))
                            .addNestedAttributeToProject(NestedAttributeName.create("")).build()).iterator();
                    assertThat(results.hasNext(), is(true));
                    Page<NestedTestRecord> page = results.next();

                });
    }
}
