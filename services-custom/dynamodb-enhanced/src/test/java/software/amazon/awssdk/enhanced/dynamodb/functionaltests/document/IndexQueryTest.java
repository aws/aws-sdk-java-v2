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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class IndexQueryTest extends LocalDynamoDbSyncTestBase {

    private DynamoDbClient lowLevelClient;

    private  DynamoDbTable<EnhancedDocument> docMappedtable ;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private DynamoDbEnhancedClient enhancedClient;
    private final String tableName = getConcreteTableName("doc-table-name");
    DynamoDbIndex<EnhancedDocument> keysOnlyMappedIndex ;

    @Before
    public void createTable() {


        lowLevelClient = getDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(lowLevelClient)
                                               .build();
        docMappedtable = enhancedClient.table(tableName,
                                              TableSchema.documentSchemaBuilder()
                                                         .attributeConverterProviders(defaultProvider())
                                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                               "id",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(TableMetadata.primaryIndexName(), "sort",
                                                                          AttributeValueType.N)
                                                  .addIndexPartitionKey("gsi_keys_only", "gsi_id", AttributeValueType.S)
                                                  .addIndexSortKey("gsi_keys_only", "gsi_sort", AttributeValueType.N)
                                                         .attributeConverterProviders(defaultProvider())
                                                         .build());

        docMappedtable.createTable(CreateTableEnhancedRequest.builder()
                                                             .provisionedThroughput(getDefaultProvisionedThroughput())
                                                             .globalSecondaryIndices(
                                                                 EnhancedGlobalSecondaryIndex.builder()
                                                                                             .indexName("gsi_keys_only")
                                                                                             .projection(p -> p.projectionType(ProjectionType.KEYS_ONLY))
                                                                                             .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                                             .build())
                                                             .build());

        keysOnlyMappedIndex = docMappedtable.index("gsi_keys_only");

    }


    private static final List<EnhancedDocument> DOCUMENTS =
        IntStream.range(0, 10)
                 .mapToObj(i -> EnhancedDocument.builder()
                     .attributeConverterProviders(defaultProvider())
                                                .putString("id", "id-value")
                                                .putNumber("sort", i)
                                                .putNumber("value", i)
                                                .putString("gsi_id", "gsi-id-value")
                                                .putNumber("gsi_sort", i)
                                                .build()
                 ).collect(Collectors.toList());

    private static final List<EnhancedDocument> KEYS_ONLY_DOCUMENTS =
        DOCUMENTS.stream()
               .map(record -> EnhancedDocument.builder()
                   .attributeConverterProviders(defaultProvider())
                                              .putString("id", record.getString("id"))
                                              .putNumber("sort", record.getNumber("sort"))
                                              .putString("gsi_id", record.getString("gsi_id"))
                                              .putNumber("gsi_sort", record.getNumber("gsi_sort")).build()
               )
               .collect(Collectors.toList());

    private void insertDocuments() {
        DOCUMENTS.forEach(document -> docMappedtable.putItem(r -> r.item(document)));
    }



    @After
    public void deleteTable() {

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(tableName)
                                                          .build());
    }

    @Test
    public void queryAllRecordsDefaultSettings_usingShortcutForm() {
        insertDocuments();

        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.query(keyEqualTo(k -> k.partitionValue("gsi-id-value"))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.stream().map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryBetween() {
        insertDocuments();
        Key fromKey = Key.builder().partitionValue("gsi-id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("gsi-id-value").sortValue(5).build();
        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.query(r -> r.queryConditional(QueryConditional.sortBetween(fromKey, toKey))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.stream().filter(r -> r.getNumber("sort").intValue() >= 3 && r.getNumber("sort").intValue() <= 5)
                          .map( j -> j.toMap()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryLimit() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.query(QueryEnhancedRequest.builder()
                                                          .queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))
                                                          .limit(5)
                                                          .build())
                               .iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page3 = results.next();
        assertThat(results.hasNext(), is(false));

        Map<String, AttributeValue> expectedLastEvaluatedKey1 = new HashMap<>();
        expectedLastEvaluatedKey1.put("id", stringValue(KEYS_ONLY_DOCUMENTS.get(4).getString("id")));
        expectedLastEvaluatedKey1.put("sort", numberValue(KEYS_ONLY_DOCUMENTS.get(4).getNumber("sort")));
        expectedLastEvaluatedKey1.put("gsi_id", stringValue(KEYS_ONLY_DOCUMENTS.get(4).getString("gsi_id")));
        expectedLastEvaluatedKey1.put("gsi_sort", numberValue(KEYS_ONLY_DOCUMENTS.get(4).getNumber("gsi_sort")));
        Map<String, AttributeValue> expectedLastEvaluatedKey2 = new HashMap<>();
        expectedLastEvaluatedKey2.put("id", stringValue(KEYS_ONLY_DOCUMENTS.get(9).getString("id")));
        expectedLastEvaluatedKey2.put("sort", numberValue(KEYS_ONLY_DOCUMENTS.get(9).getNumber("sort")));
        expectedLastEvaluatedKey2.put("gsi_id", stringValue(KEYS_ONLY_DOCUMENTS.get(9).getString("gsi_id")));
        expectedLastEvaluatedKey2.put("gsi_sort", numberValue(KEYS_ONLY_DOCUMENTS.get(9).getNumber("gsi_sort")));

        assertThat(page1.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.subList(0, 5).stream().map( i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page1.lastEvaluatedKey(), is(expectedLastEvaluatedKey1));
        assertThat(page2.items().stream().map(i -> i.toMap()).collect(Collectors.toList()), is(KEYS_ONLY_DOCUMENTS.subList(5, 10).stream().map( i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page2.lastEvaluatedKey(), is(expectedLastEvaluatedKey2));
        assertThat(page3.items().stream().map(i -> i.toMap()).collect(Collectors.toList()), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty() {
        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryExclusiveStartKey() {
        insertDocuments();
        Map<String, AttributeValue> expectedLastEvaluatedKey = new HashMap<>();
        expectedLastEvaluatedKey.put("id", stringValue(KEYS_ONLY_DOCUMENTS.get(7).getString("id")));
        expectedLastEvaluatedKey.put("sort", numberValue(KEYS_ONLY_DOCUMENTS.get(7).getNumber("sort")));
        expectedLastEvaluatedKey.put("gsi_id", stringValue(KEYS_ONLY_DOCUMENTS.get(7).getString("gsi_id")));
        expectedLastEvaluatedKey.put("gsi_sort", numberValue(KEYS_ONLY_DOCUMENTS.get(7).getNumber("gsi_sort")));
        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.query(QueryEnhancedRequest.builder()
                                                          .queryConditional(keyEqualTo(k -> k.partitionValue("gsi-id-value")))
                                                          .exclusiveStartKey(expectedLastEvaluatedKey).build())
                               .iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.subList(8, 10).stream().map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }
}
