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
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttributeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedTestRecord;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

public class AsyncBasicScanTest extends LocalDynamoDbAsyncTestBase {
    private static class Record {
        private String id;
        private Integer sort;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private Integer getSort() {
            return sort;
        }

        private Record setSort(Integer sort) {
            this.sort = sort;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort);
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
                         .build();

    private static final List<Record> RECORDS =
        IntStream.range(0, 10)
                 .mapToObj(i -> new Record().setId("id-value").setSort(i))
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

    private DynamoDbEnhancedAsyncClient enhancedAsyncClient =
        DefaultDynamoDbEnhancedAsyncClient.builder()
                                          .dynamoDbClient(getDynamoDbAsyncClient())
                                          .build();

    private DynamoDbAsyncTable<Record> mappedTable = enhancedAsyncClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private DynamoDbAsyncTable<NestedTestRecord> mappedNestedTable =
        enhancedAsyncClient.table(getConcreteTableName("nested-table-name"), TableSchema.fromClass(NestedTestRecord.class));

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)).join());
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
                                                               .build()).join();
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName("nested-table-name"))
                                                               .build()).join();
    }

    @Test
    public void scanAllRecordsDefaultSettings() {
        insertRecords();

        SdkPublisher<Page<Record>> publisher = mappedTable.scan(ScanEnhancedRequest.builder().build());
        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(), is(RECORDS));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsDefaultSettings_viaItems() {
        insertRecords();

        SdkPublisher<Record> publisher = mappedTable.scan(ScanEnhancedRequest.builder().build()).items();
        List<Record> results = drainPublisher(publisher, 10);

        assertThat(results, is(RECORDS));
    }

    @Test
    public void scanAllRecordsWithFilter() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("sort >= :min_value AND sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .build();

        SdkPublisher<Page<Record>> publisher =
            mappedTable.scan(ScanEnhancedRequest.builder().filterExpression(expression).build());

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanAllRecords_withAttributeProjection_shouldExcludeUnprojectedAttributes() {
        insertRecords();
        SdkPublisher<Page<Record>> publisher = mappedTable.scan(b -> b.attributesToProject("sort"));
        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.items().size(), is(RECORDS.size()));
        Record firstRecord = page.items().get(0);
        assertThat(firstRecord.id, is(nullValue()));
        assertThat(firstRecord.sort, is(0));
    }

    @Test
    public void scanRecords_withBothFilterAndProjection_shouldApplyBoth() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();
        SdkPublisher<Page<Record>> publisher = mappedTable.scan(ScanEnhancedRequest.builder()
                                                                                     .attributesToProject("sort")
                                                                                     .filterExpression(expression)
                                                                                     .build());
        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.items(), hasSize(3));
        assertThat(page.items().get(0).id, is(nullValue()));
        assertThat(page.items().get(0).sort, is(3));
    }

    @Test
    public void scanAllRecordsWithFilter_viaItems() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("sort >= :min_value AND sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .build();

        SdkPublisher<Record> publisher =
            mappedTable.scan(ScanEnhancedRequest.builder().filterExpression(expression).build()).items();

        List<Record> results = drainPublisher(publisher, 3);

        assertThat(results,
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
    }

    @Test
    public void scanLimit() {
        insertRecords();

        SdkPublisher<Page<Record>> publisher = mappedTable.scan(r -> r.limit(5));
        publisher.subscribe(page -> page.items().forEach(item -> System.out.println(item)));

        List<Page<Record>> results = drainPublisher(publisher, 3);

        Page<Record> page1 = results.get(0);
        Page<Record> page2 = results.get(1);
        Page<Record> page3 = results.get(2);

        assertThat(page1.items(), is(RECORDS.subList(0, 5)));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(page2.items(), is(RECORDS.subList(5, 10)));
        assertThat(page2.lastEvaluatedKey(), is(getKeyMap(9)));
        assertThat(page3.items(), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanLimit_viaItems() {
        insertRecords();

        SdkPublisher<Record> publisher = mappedTable.scan(r -> r.limit(5)).items();
        List<Record> results = drainPublisher(publisher, 10);
        assertThat(results, is(RECORDS));
    }

    @Test
    public void scanEmpty() {
        SdkPublisher<Page<Record>> publisher = mappedTable.scan();
        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanEmpty_viaItems() {
        SdkPublisher<Record> publisher = mappedTable.scan().items();
        List<Record> results = drainPublisher(publisher, 0);

        assertThat(results, is(empty()));
    }

    @Test
    public void scanExclusiveStartKey() {
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.scan(ScanEnhancedRequest.builder().exclusiveStartKey(getKeyMap(7)).build());

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(), is(RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanExclusiveStartKey_viaItems() {
        insertRecords();
        SdkPublisher<Record> publisher =
            mappedTable.scan(ScanEnhancedRequest.builder().exclusiveStartKey(getKeyMap(7)).build()).items();

        List<Record> results = drainPublisher(publisher, 2);

        assertThat(results, is(RECORDS.subList(8, 10)));
    }

    @Test
    public void scanAllRecords_withSelectCount_shouldReturnCountNotItems() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();
        SdkPublisher<Page<Record>> publisher = mappedTable.scan(ScanEnhancedRequest.builder()
                                                                                     .select(Select.COUNT)
                                                                                     .filterExpression(expression)
                                                                                     .build());
        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.count(), is(3));
        assertThat(page.items().size(), is(0));
    }

    @Test
    public void scanRecords_withBothFilterAndNestedProjection_shouldApplyBoth() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();
        SdkPublisher<Page<NestedTestRecord>> publisher = mappedNestedTable.scan(ScanEnhancedRequest.builder()
                                                                                                     .filterExpression(expression)
                                                                                                     .addNestedAttributesToProject(
                                                                                                         NestedAttributeName.create(Arrays.asList("innerAttributeRecord", "attribOne")))
                                                                                                     .build());
        List<Page<NestedTestRecord>> results = drainPublisher(publisher, 1);
        assertThat(results.get(0).items().size(), is(3));
    }

    @Test
    public void scanRecords_withEmptyNestedAttributeName_shouldThrowAssertionError() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();

        assertThatExceptionOfType(AssertionError.class).isThrownBy(
            () -> drainPublisher(mappedNestedTable.scan(ScanEnhancedRequest.builder()
                                                                            .filterExpression(expression)
                                                                            .addNestedAttributesToProject(NestedAttributeName.builder()
                                                                                                                    .elements("")
                                                                                                                    .build())
                                                                            .build()),
                                1));
    }

    @Test
    public void scanRecords_withEmptyTopLevelAttributeName_shouldThrowAssertionError() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();
        assertThatExceptionOfType(AssertionError.class).isThrownBy(
            () -> drainPublisher(mappedNestedTable.scan(ScanEnhancedRequest.builder()
                                                                            .filterExpression(expression)
                                                                            .addAttributeToProject("")
                                                                            .build()),
                                1));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue("id-value"));
        result.put("sort", numberValue(sort));
        return Collections.unmodifiableMap(result);
    }
}
