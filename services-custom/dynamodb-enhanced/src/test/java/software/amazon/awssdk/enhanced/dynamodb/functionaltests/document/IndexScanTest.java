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

import java.util.Collections;
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
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class IndexScanTest extends LocalDynamoDbSyncTestBase {

    private DynamoDbClient lowLevelClient;

    private  DynamoDbTable<EnhancedDocument> docMappedtable ;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private DynamoDbEnhancedClient enhancedClient;
    private final String tableName = getConcreteTableName("table-name");
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
    public void scanAllRecordsDefaultSettings() {
        insertDocuments();

        Iterator<Page<EnhancedDocument>> results = keysOnlyMappedIndex.scan(ScanEnhancedRequest.builder().build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.stream().map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithFilter() {
        insertDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("sort >= :min_value AND sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .build();

        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.scan(ScanEnhancedRequest.builder().filterExpression(expression).build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.stream().filter(r -> r.getNumber("sort").intValue() >= 3
                                                               && r.getNumber("sort").intValue() <= 5).map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanLimit() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results = keysOnlyMappedIndex.scan(r -> r.limit(5)).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page3 = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page1.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.subList(0, 5).stream().map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(page2.items().stream().map(i -> i.toMap()).collect(Collectors.toList()), is(KEYS_ONLY_DOCUMENTS.subList(5, 10).stream().map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page2.lastEvaluatedKey(), is(getKeyMap(9)));
        assertThat(page3.items(), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanEmpty() {
        Iterator<Page<EnhancedDocument>> results = keysOnlyMappedIndex.scan().iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanExclusiveStartKey() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            keysOnlyMappedIndex.scan(r -> r.exclusiveStartKey(getKeyMap(7))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().stream().map(i -> i.toMap()).collect(Collectors.toList()),
                   is(KEYS_ONLY_DOCUMENTS.subList(8, 10).stream().map(i -> i.toMap()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue(KEYS_ONLY_DOCUMENTS.get(sort).getString("id")));
        result.put("sort", numberValue(KEYS_ONLY_DOCUMENTS.get(sort).getNumber("sort")));
        result.put("gsi_id", stringValue(KEYS_ONLY_DOCUMENTS.get(sort).getString("gsi_id")));
        result.put("gsi_sort", numberValue(KEYS_ONLY_DOCUMENTS.get(sort).getNumber("gsi_sort")));
        return Collections.unmodifiableMap(result);
    }
}
