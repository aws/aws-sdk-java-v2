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
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class IndexScanTest extends LocalDynamoDbSyncTestBase {
    private static class Record {
        private String id;
        private Integer sort;
        private Integer value;
        private String gsiId;
        private Integer gsiSort;

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

        private Integer getValue() {
            return value;
        }

        private Record setValue(Integer value) {
            this.value = value;
            return this;
        }

        private String getGsiId() {
            return gsiId;
        }

        private Record setGsiId(String gsiId) {
            this.gsiId = gsiId;
            return this;
        }

        private Integer getGsiSort() {
            return gsiSort;
        }

        private Record setGsiSort(Integer gsiSort) {
            this.gsiSort = gsiSort;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(sort, record.sort) &&
                   Objects.equals(value, record.value) &&
                   Objects.equals(gsiId, record.gsiId) &&
                   Objects.equals(gsiSort, record.gsiSort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, sort, value, gsiId, gsiSort);
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
                         .addAttribute(String.class, a -> a.name("gsi_id")
                                                           .getter(Record::getGsiId)
                                                           .setter(Record::setGsiId)
                                                           .tags(secondaryPartitionKey("gsi_keys_only")))
                         .addAttribute(Integer.class, a -> a.name("gsi_sort")
                                                            .getter(Record::getGsiSort)
                                                            .setter(Record::setGsiSort)
                                                            .tags(secondarySortKey("gsi_keys_only")))
                         .build();

    private static final List<Record> RECORDS =
        IntStream.range(0, 10)
                 .mapToObj(i -> new Record()
                     .setId("id-value")
                     .setSort(i)
                     .setValue(i)
                     .setGsiId("gsi-id-value")
                     .setGsiSort(i))
                 .collect(Collectors.toList());

    private static final List<Record> KEYS_ONLY_RECORDS =
        RECORDS.stream()
               .map(record -> new Record()
                   .setId(record.id)
                   .setSort(record.sort)
                   .setGsiId(record.gsiId)
                   .setGsiSort(record.gsiSort))
               .collect(Collectors.toList());

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private DynamoDbIndex<Record> keysOnlyMappedIndex = mappedTable.index("gsi_keys_only");

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)));
    }

    @Before
    public void createTable() {
        mappedTable.createTable(
                r -> r.provisionedThroughput(getDefaultProvisionedThroughput())
                        .globalSecondaryIndices(
                                EnhancedGlobalSecondaryIndex.builder()
                                        .indexName("gsi_keys_only")
                                        .projection(p -> p.projectionType(ProjectionType.KEYS_ONLY))
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
    public void scanAllRecordsDefaultSettings() {
        insertRecords();

        Iterator<Page<Record>> results = keysOnlyMappedIndex.scan(ScanEnhancedRequest.builder().build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), is(KEYS_ONLY_RECORDS));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
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
            keysOnlyMappedIndex.scan(ScanEnhancedRequest.builder().filterExpression(expression).build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(),
                   is(KEYS_ONLY_RECORDS.stream().filter(r -> r.sort >= 3 && r.sort <= 5).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanLimit() {
        insertRecords();
        Iterator<Page<Record>> results = keysOnlyMappedIndex.scan(r -> r.limit(5)).iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page3 = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page1.items(), is(KEYS_ONLY_RECORDS.subList(0, 5)));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(page2.items(), is(KEYS_ONLY_RECORDS.subList(5, 10)));
        assertThat(page2.lastEvaluatedKey(), is(getKeyMap(9)));
        assertThat(page3.items(), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanEmpty() {
        Iterator<Page<Record>> results = keysOnlyMappedIndex.scan().iterator();
        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanExclusiveStartKey() {
        insertRecords();
        Iterator<Page<Record>> results =
            keysOnlyMappedIndex.scan(r -> r.exclusiveStartKey(getKeyMap(7))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<Record> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(KEYS_ONLY_RECORDS.subList(8, 10)));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue(KEYS_ONLY_RECORDS.get(sort).getId()));
        result.put("sort", numberValue(KEYS_ONLY_RECORDS.get(sort).getSort()));
        result.put("gsi_id", stringValue(KEYS_ONLY_RECORDS.get(sort).getGsiId()));
        result.put("gsi_sort", numberValue(KEYS_ONLY_RECORDS.get(sort).getGsiSort()));
        return Collections.unmodifiableMap(result);
    }
}
