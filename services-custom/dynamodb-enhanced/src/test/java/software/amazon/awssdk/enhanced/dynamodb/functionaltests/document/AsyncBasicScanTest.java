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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

public class AsyncBasicScanTest extends LocalDynamoDbAsyncTestBase {
    private static final List<EnhancedDocument> DOCUMENTS =
        IntStream.range(0, 10)
                 .mapToObj(i -> EnhancedDocument.builder()
                                                .attributeConverterProviders(defaultProvider())
                                                .putString("id", "id-value")
                                                .putNumber("sort", i)
                                                .putNumber("value", i)
                                                .build())
                 .collect(Collectors.toList());

    private final String tableName = getConcreteTableName("doc-table-name");
    private DynamoDbEnhancedAsyncClient enhancedClient;
    private DynamoDbAsyncTable<EnhancedDocument> docMappedTable;

    @Before
    public void setup() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        docMappedTable = enhancedClient.table(tableName,
                                              TableSchema.documentSchemaBuilder()
                                                         .attributeConverterProviders(defaultProvider())
                                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(TableMetadata.primaryIndexName(), "sort",
                                                                          AttributeValueType.N)
                                                         .build());
        docMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void cleanup() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder().tableName(tableName).build()).join();
    }

    private void insertDocuments() {
        DOCUMENTS.forEach(d -> docMappedTable.putItem(d).join());
    }

    @Test
    public void scanAllRecordsDefaultSettings() {
        insertDocuments();
        Page<EnhancedDocument> page = drainPublisher(docMappedTable.scan(), 1).get(0);
        assertThat(page.items().size(), is(DOCUMENTS.size()));
    }

    @Test
    public void scanAllRecordsWithFilterAndProjection() {
        insertDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();
        SdkPublisher<Page<EnhancedDocument>> publisher =
            docMappedTable.scan(ScanEnhancedRequest.builder()
                                                   .attributesToProject("sort")
                                                   .filterExpression(expression)
                                                   .build());
        Page<EnhancedDocument> page = drainPublisher(publisher, 1).get(0);
        assertThat(page.items().size(), is(3));
        assertThat(page.items().get(0).getString("id"), is(nullValue()));
        assertThat(page.items().get(0).getNumber("sort").intValue(), is(3));
    }

    @Test
    public void scanAllRecords_withSelectCount_shouldReturnCount() {
        insertDocuments();
        Page<EnhancedDocument> page = drainPublisher(docMappedTable.scan(b -> b.select(Select.COUNT)), 1).get(0);
        assertThat(page.count(), is(DOCUMENTS.size()));
        assertThat(page.items(), is(empty()));
    }

    @Test
    public void scanExclusiveStartKey() {
        insertDocuments();
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", stringValue("id-value"));
        key.put("sort", numberValue(7));
        Page<EnhancedDocument> page = drainPublisher(docMappedTable.scan(b -> b.exclusiveStartKey(key)), 1).get(0);
        assertThat(page.items().size(), is(2));
    }
}
