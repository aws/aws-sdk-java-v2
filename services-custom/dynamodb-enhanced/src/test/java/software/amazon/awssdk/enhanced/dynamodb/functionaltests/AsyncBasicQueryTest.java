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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.Attributes.attribute;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.between;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.equalTo;

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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

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
                   .attributes(
                       attribute("id", TypeToken.of(String.class), Record::getId, Record::setId).as(primaryPartitionKey()),
                       attribute("sort", TypeToken.of(Integer.class), Record::getSort, Record::setSort).as(primarySortKey()),
                       attribute("value", TypeToken.of(Integer.class), Record::getValue, Record::setValue))
        .build();

    private static final List<Record> RECORDS =
        IntStream.range(0, 10)
                 .mapToObj(i -> new Record().setId("id-value").setSort(i).setValue(i))
                 .collect(Collectors.toList());

    private DynamoDbEnhancedAsyncClient enhancedAsyncClient = DefaultDynamoDbEnhancedAsyncClient.builder()
                                                                                                .dynamoDbClient(getDynamoDbAsyncClient())
                                                                                                .build();

    private DynamoDbAsyncTable<Record> mappedTable = enhancedAsyncClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(Record.class, r -> r.item(record)).join());
    }
    
    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName("table-name"))
                                                               .build())
                                .join();
    }

    @Test
    public void queryAllRecordsDefaultSettings() {
        insertRecords();

        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(r -> r.queryConditional(equalTo(Key.create(stringValue("id-value")))));
        
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
                                                  .queryConditional(equalTo(Key.create(stringValue("id-value"))))
                                                  .filterExpression(expression)
                                                  .build());

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryBetween() {
        insertRecords();
        Key fromKey = Key.create(stringValue("id-value"), numberValue(3));
        Key toKey = Key.create(stringValue("id-value"), numberValue(5));
        SdkPublisher<Page<Record>> publisher = mappedTable.query(r -> r.queryConditional(between(fromKey, toKey)));

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(),
                   is(RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryLimit() {
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(equalTo(Key.create(stringValue("id-value"))))
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
    public void queryEmpty() {
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(r -> r.queryConditional(equalTo(Key.create(stringValue("id-value")))));

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);

        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryExclusiveStartKey() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));
        insertRecords();
        SdkPublisher<Page<Record>> publisher =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(equalTo(Key.create(stringValue("id-value"))))
                                                  .exclusiveStartKey(exclusiveStartKey)
                                                  .build());

        List<Page<Record>> results = drainPublisher(publisher, 1);
        Page<Record> page = results.get(0);
        assertThat(page.items(), is(RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }
}
