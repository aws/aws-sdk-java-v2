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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBetween;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ScanQueryWithFlattenMapIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {

    private static final String TABLE_NAME = createTestTableName();

    private static DynamoDbClient dynamoDbClient;
    private static DynamoDbEnhancedClient enhancedClient;
    private static DynamoDbTable<Record> mappedTable;

    @BeforeClass
    public static void setup() {
        dynamoDbClient = createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, RECORD_WITH_FLATTEN_MAP_TABLE_SCHEMA);
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
        RECORDS_WITH_FLATTEN_MAP.forEach(record -> mappedTable.putItem(r -> r.item(record)));
    }

    @Test
    public void queryWithFlattenMapRecord_correctlyRetrievesProjectedAttributes() {
        insertRecords();

        Iterator<Page<Record>> results =
            mappedTable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(sortBetween(k-> k.partitionValue("id-value").sortValue(2),
                                                                                k-> k.partitionValue("id-value").sortValue(6)))
                                                  .attributesToProject("mapAttribute1", "mapAttribute2")
                                                  .limit(3)
                                                  .build())
                       .iterator();

        Page<Record> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<Record> page2 = results.next();
        assertThat(results.hasNext(), is(false));

        Map<String, String> resultedAttributesMap = new HashMap<>();
        resultedAttributesMap.put("mapAttribute1", "mapValue1");
        resultedAttributesMap.put("mapAttribute2", "mapValue2");

        List<Record> page1Items = page1.items();
        assertThat(page1Items.size(), is(3));
        assertThat(page1Items.get(0).getAttributesMap(), is(resultedAttributesMap));
        assertThat(page1Items.get(1).getAttributesMap(), is(resultedAttributesMap));
        assertThat(page1Items.get(2).getAttributesMap(), is(resultedAttributesMap));
        assertThat(page1.consumedCapacity(), is(nullValue()));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(page1.count(), equalTo(3));
        assertThat(page1.scannedCount(), equalTo(3));

        List<Record> page2Items = page2.items();
        assertThat(page2Items.size(), is(2));
        assertThat(page2Items.get(0).getAttributesMap(), is(resultedAttributesMap));
        assertThat(page2Items.get(1).getAttributesMap(), is(resultedAttributesMap));
        assertThat(page2.lastEvaluatedKey(), is(nullValue()));
        assertThat(page2.count(), equalTo(2));
        assertThat(page2.scannedCount(), equalTo(2));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue(RECORDS.get(sort).getId()));
        result.put("sort", numberValue(RECORDS.get(sort).getSort()));
        return Collections.unmodifiableMap(result);
    }
}
