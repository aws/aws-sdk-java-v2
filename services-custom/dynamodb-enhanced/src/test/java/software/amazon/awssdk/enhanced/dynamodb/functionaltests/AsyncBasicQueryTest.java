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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttributeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedTestRecord;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

public class AsyncBasicQueryTest extends LocalDynamoDbAsyncTestBase {
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
                     NestedTestRecord nestedTestRecord = new NestedTestRecord();
                     nestedTestRecord.setOuterAttribOne("id-value-" + i);
                     nestedTestRecord.setSort(i);
                     InnerAttributeRecord innerAttributeRecord = new InnerAttributeRecord();
                     innerAttributeRecord.setAttribOne("attribOne-" + i);
                     innerAttributeRecord.setAttribTwo(i);
                     nestedTestRecord.setInnerAttributeRecord(innerAttributeRecord);
                     nestedTestRecord.setDotVariable("v" + i);
                     return nestedTestRecord;
                 })
                 .collect(Collectors.toList());

    private DynamoDbEnhancedAsyncClient enhancedAsyncClient = DefaultDynamoDbEnhancedAsyncClient.builder()
                                                                                                .dynamoDbClient(getDynamoDbAsyncClient())
                                                                                                .build();

    private DynamoDbAsyncTable<Record> mappedTable = enhancedAsyncClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private DynamoDbAsyncTable<NestedTestRecord> mappedNestedTable =
        enhancedAsyncClient.table(getConcreteTableName("nested-table-name"), TableSchema.fromClass(NestedTestRecord.class));

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)).join());
        NESTED_TEST_RECORDS.forEach(record -> mappedNestedTable.putItem(r -> r.item(record)).join());
    }

    private void insertNestedRecords() {
        NESTED_TEST_RECORDS.forEach(record -> mappedNestedTable.putItem(r -> r.item(record)).join());
    }
    
    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
        mappedNestedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName("table-name"))
                                                               .build())
                                .join();
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName("nested-table-name"))
                                                               .build())
                                .join();
    }

    @Test
    public void queryAllRecordsDefaultSettings_usingShortcutForm() {
        insertRecords();

        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(keyEqualTo(k -> k.partitionValue("id-value")));
        
        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        
        assertThat(page.items(), is(RECORDS));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
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

        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .filterExpression(expression)
                                                  .build());

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecords_withAttributeProjection_shouldExcludeUnprojectedAttributes() {
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                    .attributesToProject("value"));

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.items().size(), is(RECORDS.size()));
        Record firstRecord = page.items().get(0);
        assertThat(firstRecord.id, is(nullValue()));
        assertThat(firstRecord.sort, is(nullValue()));
        assertThat(firstRecord.value, is(0));
    }

    @Test
    public void queryAllRecordsWithFilter_viaItems() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#value >= :min_value AND #value <= :max_value")
                                          .expressionValues(expressionValues)
                                          .expressionNames(Collections.singletonMap("#value", "value"))
                                          .build();

        SdkPublisher<Record> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .filterExpression(expression)
                                                  .build()).items();

        List<Record> results = drainPublisher(publisher, 3);

        assertThat(results,
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
    }

    @Test
    public void queryBetween() {
        insertRecords();
        Key fromKey = Key.builder().partitionValue("id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("id-value").sortValue(5).build();
        SdkPublisher<Page<Record>> publisher = mappedTable.query(r -> r.queryConditional(QueryConditional.sortBetween(fromKey, toKey)));

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryBetween_viaItems() {
        insertRecords();
        Key fromKey = Key.builder().partitionValue("id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("id-value").sortValue(5).build();
        SdkPublisher<Record> publisher = mappedTable.query(r -> r.queryConditional(QueryConditional.sortBetween(fromKey, toKey))).items();

        List<Record> results = drainPublisher(publisher, 3);

        assertThat(results,
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
    }

    @Test
    public void queryLimit() {
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .limit(5)
                                                  .build());

        List<Page<Record>> results = drainPublisher(publisher, 3);
        Page<Record> page1 = results.get(0);
        Page<Record> page2 = results.get(1);
        Page<Record> page3 = results.get(2);

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
    public void queryLimit_viaItems() {
        insertRecords();
        SdkPublisher<Record> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .limit(5)
                                                  .build())
                       .items();

        List<Record> results = drainPublisher(publisher, 10);
        assertThat(results, is(RECORDS));
    }

    @Test
    public void queryEmpty() {
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue("id-value"))));

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty_viaItems() {
        SdkPublisher<Record> publisher =
            mappedTable.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))).items();

        List<Record> results = drainPublisher(publisher, 0);
        assertThat(results, is(empty()));
    }

    @Test
    public void queryExclusiveStartKey() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .exclusiveStartKey(exclusiveStartKey)
                                                  .build());

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.items(), is(RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecords_withSelectCount_shouldReturnCountNotItems() {
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                    .select(Select.COUNT));

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.count(), is(RECORDS.size()));
        assertThat(page.scannedCount(), is(RECORDS.size()));
        assertThat(page.items(), is(empty()));
    }

    @Test
    public void queryNestedRecord_withMixedNestedAndTopLevelProjections_shouldProjectMultipleAttributes() {
        insertNestedRecords();
        SdkPublisher<Page<NestedTestRecord>> publisher =
            mappedNestedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                                          .addNestedAttributesToProject(Arrays.asList(
                                              NestedAttributeName.builder().elements("innerAttributeRecord", "attribOne").build()))
                                          .addNestedAttributesToProject(NestedAttributeName.create("innerAttributeRecord", "attribTwo"))
                                          .addAttributeToProject("sort"));

        List<Page<NestedTestRecord>> results = drainPublisher(publisher, 1);
        Page<NestedTestRecord> page = results.get(0);
        assertThat(page.items().size(), is(1));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(1));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-1"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(1));
    }

    @Test
    public void queryRecords_withEmptyProjectionList_shouldReturnAllAttributes() {
        insertNestedRecords();
        SdkPublisher<Page<NestedTestRecord>> publisher =
            mappedNestedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                                          .attributesToProject(new ArrayList<>()));
        List<Page<NestedTestRecord>> results = drainPublisher(publisher, 1);
        NestedTestRecord firstRecord = results.get(0).items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-7"));
        assertThat(firstRecord.getSort(), is(7));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(7));
        assertThat(firstRecord.getDotVariable(), is("v7"));
    }

    @Test
    public void queryRecords_withEmptyProjectionString_shouldThrowAssertionError() {
        insertNestedRecords();
        assertThatExceptionOfType(AssertionError.class).isThrownBy(
            () -> drainPublisher(
                mappedNestedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value-3")))
                                            .attributesToProject("")),
                1));
    }

    @Test
    public void queryRecords_withEmptyNestedProjectionName_shouldThrowAssertionError() {
        insertNestedRecords();
        assertThatExceptionOfType(AssertionError.class).isThrownBy(
            () -> drainPublisher(
                mappedNestedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value-3")))
                                            .addNestedAttributeToProject(NestedAttributeName.create(""))),
                1));
    }
}
