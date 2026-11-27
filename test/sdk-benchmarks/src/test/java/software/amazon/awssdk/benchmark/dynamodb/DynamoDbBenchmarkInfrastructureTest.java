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
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.benchmark.dynamodb.fixture.BenchmarkItem;
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockClientFactory;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockResponseFactory;
import software.amazon.awssdk.benchmark.dynamodb.pipeline.DocumentGetItemBenchmark;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * Verifies shared fixture and sync/async mock infrastructure for Tier C benchmarks.
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
        assertTrue(low.containsKey(DynamoDbBenchmarkFixture.STRING_ATTR));
        assertTrue(low.containsKey(DynamoDbBenchmarkFixture.STRING_LIST_ATTR));
        assertTrue(low.containsKey(DynamoDbBenchmarkFixture.STRING_MAP_ATTR));
        assertTrue(low.containsKey(DynamoDbBenchmarkFixture.NESTED_ATTR));
    }

    @Test
    public void documentPutPathConvertsJavaValuesToAttributeMap() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        // Document was built via put* Java APIs; toMap must still match the LOW AttributeValue map.
        assertEquals(fixture.attributeMap(), fixture.document().toMap());
        assertEquals(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE,
                     fixture.document().getString(DynamoDbBenchmarkFixture.PARTITION_KEY_NAME));
        assertEquals("benchmark-string",
                     fixture.document().getString(DynamoDbBenchmarkFixture.STRING_ATTR));
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

    @Test
    public void documentGetAccessorConversionSmokeTest() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        try (DynamoDbClient client = DynamoDbMockClientFactory.syncGetItemClient(fixture.attributeMap())) {
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(client)
                                                                          .build();
            TableSchema<EnhancedDocument> documentSchema =
                TableSchema.documentSchemaBuilder()
                           .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                 DynamoDbBenchmarkFixture.PARTITION_KEY_NAME,
                                                 AttributeValueType.S)
                           .attributeConverterProviders(defaultProvider())
                           .build();
            DynamoDbTable<EnhancedDocument> table =
                enhancedClient.table(DynamoDbBenchmarkConstant.TABLE_NAME, documentSchema);
            Key key = Key.builder()
                         .partitionValue(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE)
                         .build();

            EnhancedDocument document = table.getItem(key);
            DocumentGetItemBenchmark.consumeRepresentativeAttributes(document, null);
        }
    }

    @Test
    public void asyncMockGetItemSmokeTestRepeatedCallsSucceed() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        try (DynamoDbMockClientFactory.AsyncMockResources resources =
                 DynamoDbMockClientFactory.asyncGetItemClient(fixture.attributeMap())) {
            DynamoDbAsyncClient client = resources.client();
            for (int i = 0; i < 200; i++) {
                GetItemResponse response = client.getItem(r -> r.tableName(DynamoDbBenchmarkConstant.TABLE_NAME)
                                                                .key(Collections.singletonMap(
                                                                    DynamoDbBenchmarkFixture.PARTITION_KEY_NAME,
                                                                    fixture.partitionKeyAttribute())))
                                                 .join();
                assertEquals(fixture.attributeMap(), response.item());
            }
        }
    }
}
