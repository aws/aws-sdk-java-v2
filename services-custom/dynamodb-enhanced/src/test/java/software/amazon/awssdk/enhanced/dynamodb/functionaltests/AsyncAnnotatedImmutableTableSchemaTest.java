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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.concurrent.CompletionException;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AbstractImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.SimpleImmutable;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

public class AsyncAnnotatedImmutableTableSchemaTest extends LocalDynamoDbAsyncTestBase {

    private static final String TABLE_NAME = "table-name";

    private static final TableSchema<SimpleImmutable> TABLE_SCHEMA = TableSchema.fromClass(SimpleImmutable.class);

    private final DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                                                                          .dynamoDbClient(getDynamoDbAsyncClient())
                                                                                          .build();

    private final DynamoDbAsyncTable<SimpleImmutable> asyncMappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME),
                                                                                              TABLE_SCHEMA);

    @Before
    public void createTable() {
        asyncMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();

        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(getConcreteTableName(TABLE_NAME))).join();
    }

    @After
    public void deleteTable() {
        try {
            getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                                   .tableName(getConcreteTableName(TABLE_NAME))
                                                                   .build()).join();

            getDynamoDbAsyncClient().waiter().waitUntilTableNotExists(b -> b.tableName(getConcreteTableName(TABLE_NAME))).join();
        } catch (ResourceNotFoundException ignored) {
            // Table doesn't exist, nothing to delete
        }
    }

    @Test
    public void describeTable_succeeds() {
        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        Assertions.assertThat(describeTableEnhancedResponse.table()).isNotNull();
        Assertions.assertThat(describeTableEnhancedResponse.table().tableName()).isEqualTo(getConcreteTableName(TABLE_NAME));
    }

    @Test
    public void createTableWithDefaults_thenDeleteTable_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbAsyncTable<SimpleImmutable> asyncMappedTable = enhancedClient.table(getConcreteTableName(tableName),
                                                                                    TABLE_SCHEMA);
        asyncMappedTable.createTable().join();

        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        TableDescription tableDescription = describeTableEnhancedResponse.table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        MatcherAssert.assertThat(actualTableName, is(getConcreteTableName(tableName)));
        MatcherAssert.assertThat(actualReadCapacityUnits, is(0L));
        MatcherAssert.assertThat(actualWriteCapacityUnits, is(0L));

        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName(tableName))
                                                               .build()).join();

        getDynamoDbAsyncClient().waiter().waitUntilTableNotExists(b -> b.tableName(getConcreteTableName(tableName))).join();

        assertThatThrownBy(() -> asyncMappedTable.describeTable().join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithProvisionedThroughput_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbAsyncTable<SimpleImmutable> asyncMappedTable = enhancedClient.table(getConcreteTableName(tableName),
                                                                                    TABLE_SCHEMA);
        asyncMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();

        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        TableDescription tableDescription = describeTableEnhancedResponse.table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        MatcherAssert.assertThat(actualTableName, is(getConcreteTableName(tableName)));
        MatcherAssert.assertThat(actualReadCapacityUnits, is(getDefaultProvisionedThroughput().readCapacityUnits()));
        MatcherAssert.assertThat(actualWriteCapacityUnits, is(getDefaultProvisionedThroughput().writeCapacityUnits()));

        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName(tableName))
                                                               .build()).join();

        getDynamoDbAsyncClient().waiter().waitUntilTableNotExists(b -> b.tableName(getConcreteTableName(tableName))).join();

        assertThatThrownBy(() -> asyncMappedTable.describeTable().join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithDefaults_throwsIllegalArgumentException() {
        TableSchema<AbstractImmutable> tableSchema = TableSchema.fromClass(AbstractImmutable.class);
        DynamoDbAsyncTable<AbstractImmutable> asyncMappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME),
                                                                                      tableSchema);

        assertThatThrownBy(() -> asyncMappedTable.createTable().join())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void createTableWithProvisionedThroughput_throwsIllegalArgumentException() {
        TableSchema<AbstractImmutable> tableSchema = TableSchema.fromClass(AbstractImmutable.class);
        DynamoDbAsyncTable<AbstractImmutable> asyncMappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME),
                                                                                      tableSchema);

        assertThatThrownBy(() -> asyncMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void getItem_itemNotFound_returnsNullValue() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(nullValue()));
    }

    @Test
    public void getItemWithResponse_itemNotFound_returnsNullValue() {
        GetItemEnhancedResponse<SimpleImmutable> getItemEnhancedResponse =
            asyncMappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        MatcherAssert.assertThat(getItemEnhancedResponse.attributes(), is(nullValue()));
        MatcherAssert.assertThat(getItemEnhancedResponse.consumedCapacity(), is(nullValue()));
    }

    @Test
    public void putItem_thenGetItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItemPartial_thenGetItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItemTwice_thenGetItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value-item1")
                                              .build();
        asyncMappedTable.putItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value-item2")
                              .build();
        asyncMappedTable.putItem(item).join();

        SdkPublisher<SimpleImmutable> publisher = asyncMappedTable.scan().items();
        drainPublisher(publisher, 1);

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItemWithResponse_thenGetItemWithResponse_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        PutItemEnhancedResponse<SimpleImmutable> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();
        GetItemEnhancedResponse<SimpleImmutable> getItemEnhancedResponse =
            asyncMappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        MatcherAssert.assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        MatcherAssert.assertThat(getItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItem_withCondition_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value-updated")
                              .build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        asyncMappedTable.putItem(PutItemEnhancedRequest.builder(SimpleImmutable.class)
                                                       .item(item)
                                                       .conditionExpression(conditionExpression)
                                                       .build()).join();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItem_withCondition_throwsConditionalCheckFailedException() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value-updated")
                              .build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        PutItemEnhancedRequest<SimpleImmutable> putItemEnhancedRequest = PutItemEnhancedRequest.builder(SimpleImmutable.class)
                                                                                               .item(item)
                                                                                               .conditionExpression(conditionExpression)
                                                                                               .build();

        assertThatThrownBy(() -> asyncMappedTable.putItem(putItemEnhancedRequest).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void updateItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value-updated")
                              .build();

        SimpleImmutable result = asyncMappedTable.updateItem(item).join();
        MatcherAssert.assertThat(result, is(item));

        SdkPublisher<SimpleImmutable> publisher = asyncMappedTable.scan().items();
        drainPublisher(publisher, 1);
    }

    @Test
    public void updateItem_createsNewCompleteItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        SimpleImmutable result = asyncMappedTable.updateItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void updateItem_createsNewPartialItemThenUpdateItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .build();

        SimpleImmutable result = asyncMappedTable.updateItem(item).join();
        MatcherAssert.assertThat(result, is(item));

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value")
                              .build();

        result = asyncMappedTable.updateItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithNulls_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.updateItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute(null)
                              .build();

        SimpleImmutable result = asyncMappedTable.updateItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithIgnoreNulls_succeeds() {
        SimpleImmutable item1 = SimpleImmutable.builder()
                                               .id("id-value")
                                               .sort("sort-value")
                                               .stringAttribute("stringAttribute-value")
                                               .build();

        asyncMappedTable.putItem(item1).join();

        SimpleImmutable item2 = SimpleImmutable.builder()
                                               .id("id-value")
                                               .sort("sort-value")
                                               .build();

        UpdateItemEnhancedRequest<SimpleImmutable> updateItemEnhancedRequest =
            UpdateItemEnhancedRequest.builder(SimpleImmutable.class)
                                     .item(item2)
                                     .ignoreNulls(true)
                                     .build();

        SimpleImmutable result = asyncMappedTable.updateItem(updateItemEnhancedRequest).join();
        MatcherAssert.assertThat(result, is(item1));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        asyncMappedTable.putItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value-updated")
                              .build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        UpdateItemEnhancedRequest<SimpleImmutable> updateItemEnhancedRequest =
            UpdateItemEnhancedRequest.builder(SimpleImmutable.class)
                                     .item(item)
                                     .conditionExpression(conditionExpression)
                                     .build();

        asyncMappedTable.updateItem(updateItemEnhancedRequest).join();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_throwsConditionalCheckFailedException() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        asyncMappedTable.putItem(item).join();

        item = SimpleImmutable.builder()
                              .id("id-value")
                              .sort("sort-value")
                              .stringAttribute("stringAttribute-value-updated")
                              .build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        UpdateItemEnhancedRequest<SimpleImmutable> updateItemEnhancedRequest =
            UpdateItemEnhancedRequest.builder(SimpleImmutable.class)
                                     .item(item)
                                     .conditionExpression(conditionExpression)
                                     .build();

        assertThatThrownBy(() -> asyncMappedTable.updateItem(updateItemEnhancedRequest).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndDefaultReturnValue_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        PutItemEnhancedResponse<SimpleImmutable> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();

        SimpleImmutable item2 = SimpleImmutable.builder()
                                               .id("id-value")
                                               .sort("sort-value")
                                               .stringAttribute("stringAttribute-value-updated")
                                               .build();

        UpdateItemEnhancedResponse<SimpleImmutable> updateItemEnhancedResponse =
            asyncMappedTable.updateItemWithResponse(r -> r.item(item2)).join();

        MatcherAssert.assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        MatcherAssert.assertThat(updateItemEnhancedResponse.attributes(), is(item2));
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueAllOld_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        PutItemEnhancedResponse<SimpleImmutable> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();


        SimpleImmutable item2 = SimpleImmutable.builder()
                                               .id("id-value")
                                               .sort("sort-value")
                                               .stringAttribute("stringAttribute-value-updated")
                                               .build();

        UpdateItemEnhancedResponse<SimpleImmutable> updateItemEnhancedResponse =
            asyncMappedTable.updateItemWithResponse(r -> r.item(item2).returnValues(ReturnValue.ALL_OLD)).join();

        MatcherAssert.assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        MatcherAssert.assertThat(updateItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueNone_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        PutItemEnhancedResponse<SimpleImmutable> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();

        SimpleImmutable item2 = SimpleImmutable.builder()
                                               .id("id-value")
                                               .sort("sort-value")
                                               .stringAttribute("stringAttribute-value-updated")
                                               .build();

        UpdateItemEnhancedResponse<SimpleImmutable> updateItemEnhancedResponse =
            asyncMappedTable.updateItemWithResponse(r -> r.item(item2).returnValues(ReturnValue.NONE)).join();

        MatcherAssert.assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        MatcherAssert.assertThat(updateItemEnhancedResponse.attributes(), is(nullValue()));
    }

    @Test
    public void deleteItem_itemNotFound_returnsNullValue() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        SimpleImmutable result = asyncMappedTable.deleteItem(item).join();
        MatcherAssert.assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        SimpleImmutable beforeDeleteResult = asyncMappedTable.deleteItem(item).join();
        SimpleImmutable afterDeleteResult = asyncMappedTable.getItem(item).join();

        MatcherAssert.assertThat(beforeDeleteResult, is(item));
        MatcherAssert.assertThat(afterDeleteResult, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        Key key = asyncMappedTable.keyFrom(item);
        DeleteItemEnhancedRequest deleteItemEnhancedRequest = DeleteItemEnhancedRequest.builder()
                                                                                       .key(key)
                                                                                       .conditionExpression(conditionExpression)
                                                                                       .build();

        asyncMappedTable.deleteItem(deleteItemEnhancedRequest).join();

        result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_throwsConditionalCheckFailedException() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        asyncMappedTable.putItem(item).join();

        SimpleImmutable result = asyncMappedTable.getItem(item).join();
        MatcherAssert.assertThat(result, is(item));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        Key key = asyncMappedTable.keyFrom(item);
        DeleteItemEnhancedRequest deleteItemEnhancedRequest = DeleteItemEnhancedRequest.builder()
                                                                                       .key(key)
                                                                                       .conditionExpression(conditionExpression)
                                                                                       .build();

        assertThatThrownBy(() -> asyncMappedTable.deleteItem(deleteItemEnhancedRequest).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void deleteItemWithResponse_succeeds() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();

        PutItemEnhancedResponse<SimpleImmutable> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();


        Key key = asyncMappedTable.keyFrom(item);
        DeleteItemEnhancedResponse<SimpleImmutable> deleteItemEnhancedResponse =
            asyncMappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        MatcherAssert.assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        MatcherAssert.assertThat(deleteItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void deleteItemWithResponse_itemNotFound_returnsNullValue() {
        SimpleImmutable item = SimpleImmutable.builder()
                                              .id("id-value")
                                              .sort("sort-value")
                                              .stringAttribute("stringAttribute-value")
                                              .build();
        Key key = asyncMappedTable.keyFrom(item);
        DeleteItemEnhancedResponse<SimpleImmutable> deleteItemEnhancedResponse =
            asyncMappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        MatcherAssert.assertThat(deleteItemEnhancedResponse.attributes(), is(nullValue()));
    }
}
