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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBetween;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortGreaterThan;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

public class ScanQueryIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {

    private static final String TABLE_NAME = createTestTableName();

    private static DynamoDbClient dynamoDbClient;
    private static DynamoDbEnhancedClient enhancedClient;
    private static DynamoDbTable<Record> mappedTable;

    @BeforeClass
    public static void setup() {
        dynamoDbClient = createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
        mappedTable.createTable();
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
    }

    @AfterClass
    public static void teardown() {
        try {
            dynamoDbClient.deleteTable(r -> r.tableName(TABLE_NAME));
        } finally {
            dynamoDbClient.close();
        }
    }

    private void insertRecords() {
        RECORDS.forEach(record -> mappedTable.putItem(r -> r.item(record)));
    }

    @Test
    public void scan_withoutReturnConsumedCapacity_checksPageCount() {
        insertRecords();

        Iterator<Page<Record>> results = mappedTable.scan(ScanEnhancedRequest.builder().limit(5).build())
                                                    .iterator();
        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page1.items(), is(RECORDS.subList(0, 5)));
        assertThat(page1.consumedCapacity(), is(nullValue()));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(page1.count(), equalTo(5));
        assertThat(page1.scannedCount(), equalTo(5));

        assertThat(page2.items(), is(RECORDS.subList(5, 9)));
        assertThat(page2.lastEvaluatedKey(), is(nullValue()));
        assertThat(page2.count(), equalTo(4));
        assertThat(page2.scannedCount(), equalTo(4));
    }

    @Test
    public void scan_withReturnConsumedCapacityAndDifferentReadConsistency_checksConsumedCapacity() {
        insertRecords();

        Iterator<Page<Record>> eventualConsistencyResult =
            mappedTable.scan(ScanEnhancedRequest.builder().returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).build())
                       .iterator();

        Page<Record> page = eventualConsistencyResult.next();
        assertThat(eventualConsistencyResult.hasNext(), is(false));
        ConsumedCapacity eventualConsumedCapacity = page.consumedCapacity();
        assertThat(eventualConsumedCapacity, is(notNullValue()));

        Iterator<Page<Record>> strongConsistencyResult =
            mappedTable.scan(ScanEnhancedRequest.builder().returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).build())
                       .iterator();

        page = strongConsistencyResult.next();
        assertThat(strongConsistencyResult.hasNext(), is(false));
        ConsumedCapacity strongConsumedCapacity = page.consumedCapacity();
        assertThat(strongConsumedCapacity, is(notNullValue()));

        assertThat(strongConsumedCapacity.capacityUnits(), is(greaterThanOrEqualTo(eventualConsumedCapacity.capacityUnits())));
    }

    @Test
    public void query_withoutReturnConsumedCapacity_checksPageCount() {
        insertRecords();

        Iterator<Page<Record>> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(sortBetween(k-> k.partitionValue("id-value").sortValue(2),
                                                                                k-> k.partitionValue("id-value").sortValue(6)))
                                                  .limit(3)
                                                  .build())
                       .iterator();

        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page1.items(), is(RECORDS.subList(2, 5)));
        assertThat(page1.consumedCapacity(), is(nullValue()));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(page1.count(), equalTo(3));
        assertThat(page1.scannedCount(), equalTo(3));

        assertThat(page2.items(), is(RECORDS.subList(5, 7)));
        assertThat(page2.lastEvaluatedKey(), is(nullValue()));
        assertThat(page2.count(), equalTo(2));
        assertThat(page2.scannedCount(), equalTo(2));
    }

    @Test
    public void query_withReturnConsumedCapacityAndDifferentReadConsistency_checksConsumedCapacity() {
        insertRecords();

        Iterator<Page<Record>> eventualConsistencyResult =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(sortGreaterThan(k -> k.partitionValue("id-value").sortValue(3)))
                                                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                  .build())
                       .iterator();

        Page<Record> page = eventualConsistencyResult.next();
        assertThat(eventualConsistencyResult.hasNext(), is(false));
        ConsumedCapacity eventualConsumedCapacity = page.consumedCapacity();
        assertThat(eventualConsumedCapacity, is(notNullValue()));

        Iterator<Page<Record>> strongConsistencyResult =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(sortGreaterThan(k -> k.partitionValue("id-value").sortValue(3)))
                                                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                  .build())
                       .iterator();

        page = strongConsistencyResult.next();
        assertThat(strongConsistencyResult.hasNext(), is(false));
        ConsumedCapacity strongConsumedCapacity = page.consumedCapacity();
        assertThat(strongConsumedCapacity, is(notNullValue()));

        assertThat(strongConsumedCapacity.capacityUnits(), is(greaterThanOrEqualTo(eventualConsumedCapacity.capacityUnits())));
    }

    @Test
    public void query_withStringProjectionExpression_checksProjectedAttributes() {
        insertRecords();

        String projectionExpression = "id, sort";
        Iterator<Page<Record>> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(sortBetween(k -> k.partitionValue("id-value").sortValue(2),
                                                                                k -> k.partitionValue("id-value").sortValue(6)))
                                                  .returnStringProjectionExpression(projectionExpression)
                                                  .limit(3)
                                                  .build())
                       .iterator();

        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page1.items().get(0).getId(), is(notNullValue()));
        assertThat(page1.items().get(0).getSort(), is(notNullValue()));

        assertThat(page2.items().get(0).getId(), is(notNullValue()));
        assertThat(page2.items().get(0).getSort(), is(notNullValue()));
    }

    @Test
    public void scan_withStringProjectionExpression_checksProjectedAttributes() {
        insertRecords();

        String projectionExpression = "id, sort";
        Iterator<Page<Record>> results =
            mappedTable.scan(ScanEnhancedRequest.builder()
                                                .returnStringProjectionExpression(projectionExpression)
                                                .limit(5)
                                                .build())
                       .iterator();

        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page1.items().get(0).getId(), is(notNullValue()));
        assertThat(page1.items().get(0).getSort(), is(notNullValue()));

        assertThat(page2.items().get(0).getId(), is(notNullValue()));
        assertThat(page2.items().get(0).getSort(), is(notNullValue()));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue(RECORDS.get(sort).getId()));
        result.put("sort", numberValue(RECORDS.get(sort).getSort()));
        return Collections.unmodifiableMap(result);
    }
}
