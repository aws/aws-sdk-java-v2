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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.QueryOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.ScanOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

/**
 * Functional coverage for list-index projection expressions against DynamoDB Local (#5603).
 */
public class ListProjectionExpressionTest extends LocalDynamoDbSyncTestBase {

    private final String tableName = getConcreteTableName("list-projection-table");
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<EnhancedDocument> table;
    private TableSchema<EnhancedDocument> tableSchema;

    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(getDynamoDbClient())
                                               .build();
        tableSchema = TableSchema.documentSchemaBuilder()
                                 .attributeConverterProviders(defaultProvider())
                                 .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
                                 .build();
        table = enhancedClient.table(tableName, tableSchema);
        table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        seedPersonDocument();
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }

    @Test
    public void scan_filterAndListIndexProjection_returnsCompactedPartialList() {
        Expression filterExpression = Expression.builder()
                                                .expression("phoneNumbers[1].#type = :type")
                                                .putExpressionName("#type", "type")
                                                .putExpressionValue(":type", stringValue("cell"))
                                                .build();

        ScanEnhancedRequest enhancedRequest = ScanEnhancedRequest.builder()
                                                                 .filterExpression(filterExpression)
                                                                 .attributesToProject("id", "firstName", "lastName")
                                                                 .addNestedAttributeToProject(
                                                                     NestedAttributeName.create("phoneNumbers[1]", "type"))
                                                                 .build();

        ScanRequest lowLevelRequest = ScanOperation.<EnhancedDocument>create(enhancedRequest)
                                                   .generateRequest(tableSchema,
                                                                    DefaultOperationContext.create(
                                                                        tableName, TableMetadata.primaryIndexName()),
                                                                    null);

        assertThat(lowLevelRequest.filterExpression(), is("phoneNumbers[1].#type = :type"));
        assertThat(lowLevelRequest.projectionExpression(),
                   is("#AMZN_MAPPED_id,#AMZN_MAPPED_firstName,#AMZN_MAPPED_lastName,"
                      + "#AMZN_MAPPED_phoneNumbers[1].#AMZN_MAPPED_type"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#type", "type"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_id", "id"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_firstName", "firstName"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_lastName", "lastName"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_phoneNumbers", "phoneNumbers"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_type", "type"));

        Iterator<Page<EnhancedDocument>> results = table.scan(enhancedRequest).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), hasSize(1));

        assertProjectedPerson(page.items().get(0));
    }

    @Test
    public void query_listIndexProjection_returnsCompactedPartialList() {
        QueryEnhancedRequest enhancedRequest = QueryEnhancedRequest.builder()
                                                                   .queryConditional(keyEqualTo(k -> k.partitionValue("2")))
                                                                   .attributesToProject("id", "firstName", "lastName")
                                                                   .addNestedAttributeToProject(
                                                                       NestedAttributeName.create("phoneNumbers[1]", "type"))
                                                                   .build();

        QueryRequest lowLevelRequest = QueryOperation.<EnhancedDocument>create(enhancedRequest)
                                                     .generateRequest(tableSchema,
                                                                      DefaultOperationContext.create(
                                                                          tableName, TableMetadata.primaryIndexName()),
                                                                      null);

        assertThat(lowLevelRequest.projectionExpression(),
                   is("#AMZN_MAPPED_id,#AMZN_MAPPED_firstName,#AMZN_MAPPED_lastName,"
                      + "#AMZN_MAPPED_phoneNumbers[1].#AMZN_MAPPED_type"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_id", "id"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_firstName", "firstName"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_lastName", "lastName"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_phoneNumbers", "phoneNumbers"));
        assertThat(lowLevelRequest.expressionAttributeNames(), hasEntry("#AMZN_MAPPED_type", "type"));

        Iterator<Page<EnhancedDocument>> results = table.query(enhancedRequest).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), hasSize(1));

        assertProjectedPerson(page.items().get(0));
    }

    private void seedPersonDocument() {
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
        table.putItem(person);
    }

    private void assertProjectedPerson(EnhancedDocument result) {
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
