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
import static org.hamcrest.Matchers.*;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttributeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedTestRecord;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class BasicScanTest extends LocalDynamoDbSyncTestBase {
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
    public void scanAllRecordsDefaultSettings() {
        insertRecords();

        mappedTable.scan(ScanEnhancedRequest.builder().build())
                   .forEach(p -> p.items().forEach(item -> System.out.println(item)));
        Iterator<Page<Record>> results = mappedTable.scan(ScanEnhancedRequest.builder().build()).iterator();

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
                mappedTable.scan(b -> b.attributesToProject("sort")).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().size(), is(RECORDS.size()));

        Record firstRecord = page.items().get(0);
        assertThat(firstRecord.id, is(nullValue()));
        assertThat(firstRecord.sort, is(0));
    }

    @Test
    public void scanAllRecordsDefaultSettings_viaItems() {
        insertRecords();

        SdkIterable<Record> items = mappedTable.scan(ScanEnhancedRequest.builder().limit(2).build()).items();
        assertThat(items.stream().collect(Collectors.toList()), is(RECORDS));
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

        Iterator<Page<Record>> results =
            mappedTable.scan(ScanEnhancedRequest.builder().filterExpression(expression).build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithFilterAndProjection() {
        insertRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<Record>> results =
                mappedTable.scan(
                        ScanEnhancedRequest.builder()
                                .attributesToProject("sort")
                                .filterExpression(expression)
                                .build()
                ).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), hasSize(3));

        Record record = page.items().get(0);

        assertThat(record.id, is(nullValue()));
        assertThat(record.sort, is(3));
    }

    @Test
    public void scanLimit() {
        insertRecords();
        Iterator<Page<Record>> results = mappedTable.scan(r -> r.limit(5)).iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page3 = results.next();
        assertThat(results.hasNext(), is(false));

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
        SdkIterable<Record> results = mappedTable.scan(r -> r.limit(5)).items();
        assertThat(results.stream().collect(Collectors.toList()), is(RECORDS));
    }

    @Test
    public void scanEmpty() {
        Iterator<Page<Record>> results = mappedTable.scan().iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanEmpty_viaItems() {
        Iterator<Record> results = mappedTable.scan().items().iterator();
        assertThat(results.hasNext(), is(false));
    }

    @Test
    public void scanExclusiveStartKey() {
        insertRecords();
        Iterator<Page<Record>> results =
            mappedTable.scan(r -> r.exclusiveStartKey(getKeyMap(7))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanExclusiveStartKey_viaItems() {
        insertRecords();
        SdkIterable<Record> results =
            mappedTable.scan(r -> r.exclusiveStartKey(getKeyMap(7))).items();
        assertThat(results.stream().collect(Collectors.toList()), is(RECORDS.subList(8, 10)));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue("id-value"));
        result.put("sort", numberValue(sort));
        return Collections.unmodifiableMap(result);
    }
    @Test
    public void scanAllRecordsWithFilterAndNestedProjectionSingleAttribute() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(
                                        NestedAttributeName.create(Arrays.asList("innerAttributeRecord","attribOne")))
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getInnerAttributeRecord().getAttribOne()
                        .compareTo(item2.getInnerAttributeRecord().getAttribOne()));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-3"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(nullValue()));

        //Attribute repeated with new and old attributeToProject
        results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.create("sort"))
                                .addAttributeToProject("sort")
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getSort()
                        .compareTo(item2.getSort()));
        firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(3));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));

        results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributeToProject(
                                        NestedAttributeName.create(Arrays.asList("innerAttributeRecord","attribOne")))
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getInnerAttributeRecord().getAttribOne()
                        .compareTo(item2.getInnerAttributeRecord().getAttribOne()));
        firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-3"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithFilterAndNestedProjectionMultipleAttribute() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        final ScanEnhancedRequest build = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .addAttributeToProject("outerAttribOne")
                .addNestedAttributesToProject(Arrays.asList(NestedAttributeName.builder().elements("innerAttributeRecord")
                        .addElement("attribOne").build()))
                .addNestedAttributeToProject(NestedAttributeName.builder()
                        .elements(Arrays.asList("innerAttributeRecord", "attribTwo")).build())
                .build();
        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.scan(
                        build
                ).iterator();

        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getInnerAttributeRecord().getAttribOne()
                        .compareTo(item2.getInnerAttributeRecord().getAttribOne()));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-3"));
        assertThat(firstRecord.getSort(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-3"));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(3));

    }

    @Test
    public void scanAllRecordsWithNonExistigKeyName() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();


        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.builder().addElement("nonExistent").build())
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord, is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithDotInAttributeKeyName() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName
                                        .create("test.com")).build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getDotVariable()
                        .compareTo(item2.getDotVariable()));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(nullValue()));
        assertThat(firstRecord.getDotVariable(), is("v3"));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithSameNamesRepeated() {
        //Attribute repeated with new and old attributeToProject
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<NestedTestRecord> >results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.builder().elements("sort").build())
                                .addAttributeToProject("sort")
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getSort()
                        .compareTo(item2.getSort()));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is(nullValue()));
        assertThat(firstRecord.getSort(), is(3));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));
        assertThat(firstRecord.getInnerAttributeRecord(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithEmptyList() {
        //Attribute repeated with new and old attributeToProject
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<NestedTestRecord> >results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(new ArrayList<>())
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getSort()
                        .compareTo(item2.getSort()));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-3"));
        assertThat(firstRecord.getSort(), is(3));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(3));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-3"));
    }

    @Test
    public void scanAllRecordsWithNullAttributesToProject() {
        //Attribute repeated with new and old attributeToProject
        insertNestedRecords();
        List<String> backwardCompatibilityNull = null;
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<NestedTestRecord> >results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .attributesToProject("test.com")
                                .attributesToProject(backwardCompatibilityNull)
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<NestedTestRecord> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getSort()
                        .compareTo(item2.getSort()));
        NestedTestRecord firstRecord = page.items().get(0);
        assertThat(firstRecord.getOuterAttribOne(), is("id-value-3"));
        assertThat(firstRecord.getSort(), is(3));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribTwo(), is(3));
        assertThat(firstRecord.getInnerAttributeRecord().getAttribOne(), is("attribOne-3"));
    }

    @Test
    public void scanAllRecordsWithNestedProjectionNameEmptyNameMap() {
        insertNestedRecords();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        final Iterator<Page<NestedTestRecord>> results =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.builder().elements("").build()).build()
                ).iterator();

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> { final boolean b = results.hasNext();
        Page<NestedTestRecord> next = results.next(); }).withMessageContaining("ExpressionAttributeNames contains invalid value");

        final Iterator<Page<NestedTestRecord>> resultsAttributeToProject =
                mappedNestedTable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addAttributeToProject("").build()
                ).iterator();

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
            final boolean b = resultsAttributeToProject.hasNext();
            Page<NestedTestRecord> next = resultsAttributeToProject.next();
        });
    }
}
