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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

/**
 * Async smoke coverage for list-index projection expressions against DynamoDB Local (#5603).
 */
public class AsyncListProjectionExpressionTest extends LocalDynamoDbAsyncTestBase {

    private final String tableName = getConcreteTableName("async-list-projection-table");
    private DynamoDbEnhancedAsyncClient enhancedClient;
    private DynamoDbAsyncTable<EnhancedDocument> table;

    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                                    .dynamoDbClient(getDynamoDbAsyncClient())
                                                    .build();
        TableSchema<EnhancedDocument> tableSchema =
            TableSchema.documentSchemaBuilder()
                       .attributeConverterProviders(defaultProvider())
                       .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
                       .build();
        table = enhancedClient.table(tableName, tableSchema);
        table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();

        Map<String, String> homePhone = new HashMap<>();
        homePhone.put("type", "home");
        homePhone.put("number", "111");

        Map<String, String> cellPhone = new HashMap<>();
        cellPhone.put("type", "cell");
        cellPhone.put("number", "222");

        EnhancedDocument person = EnhancedDocument.builder()
                                                  .attributeConverterProviders(defaultProvider())
                                                  .putString("id", "2")
                                                  .putString("firstName", "FirstName2")
                                                  .putString("lastName", "LastName2")
                                                  .putString("extraSibling", "omit-me")
                                                  .putList("phoneNumbers",
                                                           Arrays.asList(homePhone, cellPhone),
                                                           EnhancedType.mapOf(String.class, String.class))
                                                  .build();
        table.putItem(person).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder().tableName(tableName).build()).join();
    }

    @Test
    public void scan_listIndexProjection_returnsCompactedPartialList() {
        ScanEnhancedRequest enhancedRequest =
            ScanEnhancedRequest.builder()
                               .attributesToProject("id", "firstName", "lastName")
                               .addNestedAttributeToProject(NestedAttributeName.create("phoneNumbers[1]", "type"))
                               .build();

        SdkPublisher<Page<EnhancedDocument>> publisher = table.scan(enhancedRequest);
        List<Page<EnhancedDocument>> pages = drainPublisher(publisher, 1);
        assertThat(pages.get(0).items(), hasSize(1));

        EnhancedDocument result = pages.get(0).items().get(0);
        assertThat(result.getString("id"), is("2"));
        assertThat(result.getString("firstName"), is("FirstName2"));
        assertThat(result.getString("lastName"), is("LastName2"));
        assertThat(result.getString("extraSibling"), is(nullValue()));

        List<AttributeValue> phoneNumbers = result.getListOfUnknownType("phoneNumbers");
        assertThat(phoneNumbers, hasSize(1));
        Map<String, AttributeValue> selectedPhone = phoneNumbers.get(0).m();
        assertThat(selectedPhone.get("type").s(), is("cell"));
        assertThat(selectedPhone.containsKey("number"), is(false));
    }
}
