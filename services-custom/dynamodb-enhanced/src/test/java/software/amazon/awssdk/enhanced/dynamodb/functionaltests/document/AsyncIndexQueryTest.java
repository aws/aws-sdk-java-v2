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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class AsyncIndexQueryTest extends LocalDynamoDbAsyncTestBase {
    private static final List<EnhancedDocument> DOCUMENTS =
        IntStream.range(0, 10)
                 .mapToObj(i -> EnhancedDocument.builder()
                                                .attributeConverterProviders(defaultProvider())
                                                .putString("id", "id-value")
                                                .putNumber("sort", i)
                                                .putString("gsi_id", "gsi-id-value")
                                                .putNumber("gsi_sort", i)
                                                .build())
                 .collect(Collectors.toList());

    private final String tableName = getConcreteTableName("doc-table-name");
    private DynamoDbEnhancedAsyncClient enhancedClient;
    private DynamoDbAsyncTable<EnhancedDocument> table;
    private DynamoDbAsyncIndex<EnhancedDocument> index;

    @Before
    public void setup() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        table = enhancedClient.table(tableName,
                                     TableSchema.documentSchemaBuilder()
                                                .attributeConverterProviders(defaultProvider())
                                                .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
                                                .addIndexSortKey(TableMetadata.primaryIndexName(), "sort", AttributeValueType.N)
                                                .addIndexPartitionKey("gsi_keys_only", "gsi_id", AttributeValueType.S)
                                                .addIndexSortKey("gsi_keys_only", "gsi_sort", AttributeValueType.N)
                                                .build());
        table.createTable(CreateTableEnhancedRequest.builder()
                                                    .provisionedThroughput(getDefaultProvisionedThroughput())
                                                    .globalSecondaryIndices(
                                                        EnhancedGlobalSecondaryIndex.builder()
                                                                                    .indexName("gsi_keys_only")
                                                                                    .projection(p -> p.projectionType(ProjectionType.KEYS_ONLY))
                                                                                    .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                                    .build())
                                                    .build()).join();
        index = table.index("gsi_keys_only");
    }

    @After
    public void cleanup() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder().tableName(tableName).build()).join();
    }

    private void insertDocuments() {
        DOCUMENTS.forEach(d -> table.putItem(d).join());
    }

    @Test
    public void queryAllRecordsDefaultSettings_usingShortcutForm() {
        insertDocuments();
        Page<EnhancedDocument> page = drainPublisher(index.query(keyEqualTo(k -> k.partitionValue("gsi-id-value"))), 1).get(0);
        assertThat(page.items().size(), is(DOCUMENTS.size()));
    }

    @Test
    public void queryBetween() {
        insertDocuments();
        Key fromKey = Key.builder().partitionValue("gsi-id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("gsi-id-value").sortValue(5).build();
        SdkPublisher<Page<EnhancedDocument>> publisher =
            index.query(r -> r.queryConditional(QueryConditional.sortBetween(fromKey, toKey)));
        Page<EnhancedDocument> page = drainPublisher(publisher, 1).get(0);
        assertThat(page.items().size(), is(3));
    }

    @Test
    public void queryLimit() {
        insertDocuments();
        List<Page<EnhancedDocument>> pages = drainPublisher(index.query(QueryEnhancedRequest.builder()
                                                                                     .queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))
                                                                                     .limit(5)
                                                                                     .build()), 3);
        assertThat(pages.get(0).items().size(), is(5));
        assertThat(pages.get(2).items(), is(empty()));
    }

    @Test
    public void queryExclusiveStartKey() {
        insertDocuments();
        Map<String, AttributeValue> startKey = new HashMap<>();
        startKey.put("id", stringValue("id-value"));
        startKey.put("sort", numberValue(7));
        startKey.put("gsi_id", stringValue("gsi-id-value"));
        startKey.put("gsi_sort", numberValue(7));
        Page<EnhancedDocument> page = drainPublisher(index.query(QueryEnhancedRequest.builder()
                                                                                      .queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))
                                                                                      .exclusiveStartKey(startKey)
                                                                                      .build()), 1).get(0);
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
        assertThat(page.items().size(), is(2));
    }
}
