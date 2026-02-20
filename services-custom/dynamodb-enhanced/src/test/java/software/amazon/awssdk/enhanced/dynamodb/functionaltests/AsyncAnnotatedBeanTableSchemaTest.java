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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.concurrent.CompletionException;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AbstractBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.SimpleBean;
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

public class AsyncAnnotatedBeanTableSchemaTest extends LocalDynamoDbAsyncTestBase {

    private static final String TABLE_NAME = "table-name";

    private static final TableSchema<SimpleBean> TABLE_SCHEMA = TableSchema.fromClass(SimpleBean.class);

    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient = DynamoDbEnhancedAsyncClient.builder()
                                                                                               .dynamoDbClient(getDynamoDbAsyncClient())
                                                                                               .build();

    private final DynamoDbAsyncTable<SimpleBean> asyncMappedTable = enhancedAsyncClient.table(getConcreteTableName(TABLE_NAME),
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
    public void describeTable() {
        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        Assertions.assertThat(describeTableEnhancedResponse.table()).isNotNull();
        Assertions.assertThat(describeTableEnhancedResponse.table().tableName()).isEqualTo(getConcreteTableName(TABLE_NAME));
    }

    @Test
    public void createTableWithDefaults_thenDeleteTable_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbAsyncTable<SimpleBean> asyncMappedTable = enhancedAsyncClient.table(getConcreteTableName(tableName),
                                                                                    TABLE_SCHEMA);
        asyncMappedTable.createTable().join();

        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(getConcreteTableName(tableName))).join();

        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        TableDescription tableDescription = describeTableEnhancedResponse.table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        assertThat(actualTableName, is(getConcreteTableName(tableName)));
        assertThat(actualReadCapacityUnits, is(0L));
        assertThat(actualWriteCapacityUnits, is(0L));

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

        DynamoDbAsyncTable<SimpleBean> asyncMappedTable = enhancedAsyncClient.table(getConcreteTableName(tableName),
                                                                                    TABLE_SCHEMA);
        asyncMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();

        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(getConcreteTableName(tableName))).join();

        DescribeTableEnhancedResponse describeTableEnhancedResponse = asyncMappedTable.describeTable().join();
        TableDescription tableDescription = describeTableEnhancedResponse.table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        assertThat(actualTableName, is(getConcreteTableName(tableName)));
        assertThat(actualReadCapacityUnits, is(getDefaultProvisionedThroughput().readCapacityUnits()));
        assertThat(actualWriteCapacityUnits, is(getDefaultProvisionedThroughput().writeCapacityUnits()));

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
        TableSchema<AbstractBean> tableSchema = TableSchema.fromClass(AbstractBean.class);
        DynamoDbAsyncTable<AbstractBean> asyncMappedTable = enhancedAsyncClient.table(getConcreteTableName(TABLE_NAME),
                                                                                      tableSchema);

        assertThatThrownBy(() -> asyncMappedTable.createTable().join())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void createTableWithProvisionedThroughput_throwsIllegalArgumentException() {
        TableSchema<AbstractBean> tableSchema = TableSchema.fromClass(AbstractBean.class);
        DynamoDbAsyncTable<AbstractBean> asyncMappedTable = enhancedAsyncClient.table(getConcreteTableName(TABLE_NAME),
                                                                                      tableSchema);

        assertThatThrownBy(() -> asyncMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void getItem_itemNotFound_returnsNullValue() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void getItemWithResponse_itemNotFound_returnsNullValue() {
        GetItemEnhancedResponse<SimpleBean> getItemEnhancedResponse =
            asyncMappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(getItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(getItemEnhancedResponse.consumedCapacity(), is(nullValue()));
    }

    @Test
    public void putItem_thenGetItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItemPartial_thenGetItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        asyncMappedTable.putItem(item).join();

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItemTwice_thenGetItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value-item1");
        asyncMappedTable.putItem(item).join();

        item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value-item2");
        asyncMappedTable.putItem(item).join();

        SdkPublisher<SimpleBean> publisher = asyncMappedTable.scan().items();
        drainPublisher(publisher, 1);

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItemWithResponse_thenGetItemWithResponse_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();
        GetItemEnhancedResponse<SimpleBean> getItemEnhancedResponse =
            asyncMappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(getItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItem_withCondition_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        item.setStringAttribute("stringAttribute-value-updated");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        asyncMappedTable.putItem(PutItemEnhancedRequest.builder(SimpleBean.class)
                                                       .item(item)
                                                       .conditionExpression(conditionExpression)
                                                       .build()).join();

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItem_withCondition_throwsConditionalCheckFailedException() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        item.setStringAttribute("stringAttribute-value-updated");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        PutItemEnhancedRequest<SimpleBean> putItemEnhancedRequest = PutItemEnhancedRequest.builder(SimpleBean.class)
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
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value-updated");

        SimpleBean result = asyncMappedTable.updateItem(item).join();
        assertThat(result, is(item));

        SdkPublisher<SimpleBean> publisher = asyncMappedTable.scan().items();
        drainPublisher(publisher, 1);
    }

    @Test
    public void updateItem_createsNewCompleteItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        SimpleBean result = asyncMappedTable.updateItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void updateItem_createsNewPartialItemThenUpdateItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");

        SimpleBean result = asyncMappedTable.updateItem(item).join();
        assertThat(result, is(item));

        item.setStringAttribute("stringAttribute-value");

        result = asyncMappedTable.updateItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithNulls_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.updateItem(item).join();

        item.setStringAttribute(null);

        SimpleBean result = asyncMappedTable.updateItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithIgnoreNulls_succeeds() {
        SimpleBean item1 = new SimpleBean();
        item1.setId("id-value");
        item1.setSort("sort-value");
        item1.setStringAttribute("stringAttribute-value");

        asyncMappedTable.putItem(item1).join();

        SimpleBean item2 = new SimpleBean();
        item2.setId("id-value");
        item2.setSort("sort-value");

        UpdateItemEnhancedRequest<SimpleBean> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(SimpleBean.class)
                                                                                                   .item(item2)
                                                                                                   .ignoreNulls(true)
                                                                                                   .build();

        SimpleBean result = asyncMappedTable.updateItem(updateItemEnhancedRequest).join();
        assertThat(result, is(item1));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        asyncMappedTable.putItem(item).join();
        item.setStringAttribute("stringAttribute-value-updated");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        UpdateItemEnhancedRequest<SimpleBean> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(SimpleBean.class)
                                                                                                   .item(item)
                                                                                                   .conditionExpression(conditionExpression)
                                                                                                   .build();

        asyncMappedTable.updateItem(updateItemEnhancedRequest).join();

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_throwsConditionalCheckFailedException() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        asyncMappedTable.putItem(item).join();

        item.setStringAttribute("stringAttribute-value-updated");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        UpdateItemEnhancedRequest<SimpleBean> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(SimpleBean.class)
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
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();

        item.setStringAttribute("stringAttribute-value-updated");

        UpdateItemEnhancedResponse<SimpleBean> updateItemEnhancedResponse =
            asyncMappedTable.updateItemWithResponse(r -> r.item(item)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueAllOld_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();

        SimpleBean item2 = new SimpleBean();
        item2.setId("id-value");
        item2.setSort("sort-value");
        item2.setStringAttribute("stringAttribute-value-updated");

        UpdateItemEnhancedResponse<SimpleBean> updateItemEnhancedResponse =
            asyncMappedTable.updateItemWithResponse(r -> r.item(item).returnValues(ReturnValue.ALL_OLD)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueNone_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();

        SimpleBean item2 = new SimpleBean();
        item2.setId("id-value");
        item2.setSort("sort-value");
        item2.setStringAttribute("stringAttribute-value-updated");

        UpdateItemEnhancedResponse<SimpleBean> updateItemEnhancedResponse =
            asyncMappedTable.updateItemWithResponse(r -> r.item(item2).returnValues(ReturnValue.NONE)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(nullValue()));
    }

    @Test
    public void deleteItem_itemNotFound_returnsNullValue() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        SimpleBean result = asyncMappedTable.deleteItem(item).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        SimpleBean beforeDeleteResult = asyncMappedTable.deleteItem(item).join();
        SimpleBean afterDeleteResult = asyncMappedTable.getItem(item).join();

        assertThat(beforeDeleteResult, is(item));
        assertThat(afterDeleteResult, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));

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
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_throwsConditionalCheckFailedException() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        asyncMappedTable.putItem(item).join();

        SimpleBean result = asyncMappedTable.getItem(item).join();
        assertThat(result, is(item));

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
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            asyncMappedTable.putItemWithResponse(r -> r.item(item)).join();


        Key key = asyncMappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<SimpleBean> deleteItemEnhancedResponse =
            asyncMappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(deleteItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void deleteItemWithResponse_itemNotFound_returnsNullValue() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        Key key = asyncMappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<SimpleBean> deleteItemEnhancedResponse =
            asyncMappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        assertThat(deleteItemEnhancedResponse.attributes(), is(nullValue()));
    }
}
