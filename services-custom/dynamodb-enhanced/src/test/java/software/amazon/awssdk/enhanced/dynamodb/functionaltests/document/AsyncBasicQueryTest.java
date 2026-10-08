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
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

public class AsyncBasicQueryTest extends LocalDynamoDbAsyncTestBase {
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
    public void queryAllRecordsDefaultSettings_shortcutForm() {
        insertDocuments();
        SdkPublisher<Page<EnhancedDocument>> publisher = docMappedTable.query(keyEqualTo(k -> k.partitionValue("id-value")));
        List<Page<EnhancedDocument>> pages = drainPublisher(publisher, 1);
        assertThat(pages.get(0).items().size(), is(DOCUMENTS.size()));
    }

    @Test
    public void queryAllRecords_withProjection_shouldSelectOnlyProjectedAttributes() {
        insertDocuments();
        SdkPublisher<Page<EnhancedDocument>> publisher =
            docMappedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                       .attributesToProject("value")
                                       .select("SPECIFIC_ATTRIBUTES"));
        List<Page<EnhancedDocument>> pages = drainPublisher(publisher, 1);
        EnhancedDocument first = pages.get(0).items().get(0);
        assertThat(first.getString("id"), is(nullValue()));
        assertThat(first.getNumber("sort"), is(nullValue()));
        assertThat(first.getNumber("value").intValue(), is(0));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withSelectCount() {
        insertDocuments();
        SdkPublisher<Page<EnhancedDocument>> publisher =
            docMappedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                       .select(Select.COUNT));
        Page<EnhancedDocument> page = drainPublisher(publisher, 1).get(0);
        assertThat(page.count(), is(DOCUMENTS.size()));
        assertThat(page.items(), is(empty()));
    }

    @Test
    public void queryRecords_withDottedAttributeName_shouldProjectCorrectly() {
        EnhancedDocument nested = EnhancedDocument.builder()
                                                  .attributeConverterProviders(defaultProvider())
                                                  .putString("id", "id-value")
                                                  .putNumber("sort", 1)
                                                  .putString("test.com", "v1")
                                                  .build();
        docMappedTable.putItem(nested).join();

        SdkPublisher<Page<EnhancedDocument>> publisher =
            docMappedTable.query(b -> b.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                       .addNestedAttributeToProject(NestedAttributeName.create("test.com")));
        EnhancedDocument first = drainPublisher(publisher, 1).get(0).items().get(0);
        assertThat(first.getString("test.com"), is("v1"));
    }

    @Test
    public void queryExclusiveStartKey() {
        insertDocuments();
        java.util.Map<String, AttributeValue> exclusiveStartKey =
            new java.util.HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));

        SdkPublisher<Page<EnhancedDocument>> publisher =
            docMappedTable.query(QueryEnhancedRequest.builder()
                                                     .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                     .exclusiveStartKey(exclusiveStartKey)
                                                     .build());
        List<Page<EnhancedDocument>> pages = drainPublisher(publisher, 1);
        assertThat(pages.get(0).items().isEmpty(), is(false));
    }
}
