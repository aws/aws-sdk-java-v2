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

package software.amazon.awssdk.benchmark.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.benchmark.dynamodb.fixture.BenchmarkItem;
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockClientFactory;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockResponseFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * Verifies Phase 1 shared fixture and sync mock infrastructure.
 */
public class DynamoDbBenchmarkInfrastructureTest {

    @Test
    public void fixtureRepresentationsAreEquivalent() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();

        Map<String, AttributeValue> low = fixture.attributeMap();
        EnhancedDocument document = fixture.document();
        BenchmarkItem typed = fixture.item();

        assertEquals(low, document.toMap());
        assertEquals(typed, fixture.tableSchema().mapToItem(low));
        assertEquals(low, fixture.tableSchema().itemToMap(typed, true));
        assertEquals(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE,
                     low.get(DynamoDbBenchmarkFixture.PARTITION_KEY_NAME).s());
        assertTrue(low.containsKey("stringAttr"));
        assertTrue(low.containsKey("stringList"));
        assertTrue(low.containsKey("stringMap"));
        assertTrue(low.containsKey("nested"));
    }

    @Test
    public void mockResponseBodiesAreNonEmptyProtocolJson() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        Map<String, AttributeValue> item = fixture.attributeMap();

        String getBody = DynamoDbMockResponseFactory.getItemResponseBody(item);
        String putBody = DynamoDbMockResponseFactory.putItemResponseBody();
        String queryBody = DynamoDbMockResponseFactory.queryResponseBody(item);

        assertTrue(getBody.contains("\"Item\""));
        assertTrue(getBody.contains(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE));
        assertNotNull(putBody);
        assertTrue(queryBody.contains("\"Items\""));
        assertTrue(queryBody.contains("\"Count\""));
    }

    @Test
    public void syncMockGetPutQuerySmokeTest() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        Map<String, AttributeValue> item = fixture.attributeMap();

        try (DynamoDbClient getClient = DynamoDbMockClientFactory.syncGetItemClient(item)) {
            GetItemResponse getResponse = getClient.getItem(r -> r.tableName(DynamoDbBenchmarkConstant.TABLE_NAME)
                                                                  .key(Collections.singletonMap(
                                                                      DynamoDbBenchmarkFixture.PARTITION_KEY_NAME,
                                                                      fixture.partitionKeyAttribute())));
            assertEquals(item, getResponse.item());
            assertFalse(getResponse.item().isEmpty());
        }

        try (DynamoDbClient putClient = DynamoDbMockClientFactory.syncPutItemClient()) {
            PutItemResponse putResponse = putClient.putItem(r -> r.tableName(DynamoDbBenchmarkConstant.TABLE_NAME)
                                                                  .item(item));
            assertNotNull(putResponse);
        }

        try (DynamoDbClient queryClient = DynamoDbMockClientFactory.syncQueryClient(item)) {
            QueryResponse queryResponse = queryClient.query(r -> r.tableName(DynamoDbBenchmarkConstant.TABLE_NAME)
                                                                  .keyConditionExpression("pk = :pk")
                                                                  .expressionAttributeValues(
                                                                      Collections.singletonMap(
                                                                          ":pk",
                                                                          fixture.partitionKeyAttribute())));
            assertEquals(1, queryResponse.count().intValue());
            assertEquals(1, queryResponse.items().size());
            assertEquals(item, queryResponse.items().get(0));
        }
    }
}
