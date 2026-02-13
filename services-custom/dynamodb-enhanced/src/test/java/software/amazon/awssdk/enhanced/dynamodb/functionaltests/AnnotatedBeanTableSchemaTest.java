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

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
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

public class AnnotatedBeanTableSchemaTest extends LocalDynamoDbSyncTestBase {

    private static final String TABLE_NAME = "table-name";

    private static final TableSchema<SimpleBean> TABLE_SCHEMA = TableSchema.fromClass(SimpleBean.class);

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private final DynamoDbTable<SimpleBean> mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        try {
            getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                              .tableName(getConcreteTableName(TABLE_NAME))
                                                              .build());
        } catch (ResourceNotFoundException ignored) {
            // Table doesn't exist, nothing to delete
        }
    }

    @Test
    public void describeTable_succeeds() {
        DescribeTableEnhancedResponse describeTableEnhancedResponse = mappedTable.describeTable();
        Assertions.assertThat(describeTableEnhancedResponse.table()).isNotNull();
        Assertions.assertThat(describeTableEnhancedResponse.table().tableName()).isEqualTo(getConcreteTableName(TABLE_NAME));
    }

    @Test
    public void createTableWithDefaults_thenDeleteTable_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbTable<SimpleBean> mappedTable = enhancedClient.table(getConcreteTableName(tableName), TABLE_SCHEMA);
        mappedTable.createTable();

        TableDescription tableDescription = mappedTable.describeTable().table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        assertThat(actualTableName, is(getConcreteTableName(tableName)));
        assertThat(actualReadCapacityUnits, is(0L));
        assertThat(actualWriteCapacityUnits, is(0L));

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName(tableName))
                                                          .build());

        assertThatThrownBy(mappedTable::describeTable)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithProvisionedThroughput_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbTable<SimpleBean> mappedTable = enhancedClient.table(getConcreteTableName(tableName), TABLE_SCHEMA);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));

        TableDescription tableDescription = mappedTable.describeTable().table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        assertThat(actualTableName, is(getConcreteTableName(tableName)));
        assertThat(actualReadCapacityUnits, is(getDefaultProvisionedThroughput().readCapacityUnits()));
        assertThat(actualWriteCapacityUnits, is(getDefaultProvisionedThroughput().writeCapacityUnits()));

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName(tableName))
                                                          .build());

        assertThatThrownBy(mappedTable::describeTable)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithDefaults_throwsIllegalArgumentException() {
        TableSchema<AbstractBean> tableSchema = TableSchema.fromClass(AbstractBean.class);
        DynamoDbTable<AbstractBean> mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), tableSchema);

        assertThatThrownBy(mappedTable::createTable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void createTableWithProvisionedThroughput_throwsIllegalArgumentException() {
        TableSchema<AbstractBean> tableSchema = TableSchema.fromClass(AbstractBean.class);
        DynamoDbTable<AbstractBean> mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), tableSchema);

        assertThatThrownBy(() -> mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())))
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

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void getItemWithResponse_itemNotFound_returnsNullValue() {
        GetItemEnhancedResponse<SimpleBean> getItemEnhancedResponse =
            mappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));

        assertThat(getItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(getItemEnhancedResponse.consumedCapacity(), is(nullValue()));
    }

    @Test
    public void putItem_thenGetItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result , is(item));
    }

    @Test
    public void putItemPartial_thenGetItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        mappedTable.putItem(item);

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result , is(item));
    }

    @Test
    public void putItemTwice_thenGetItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value-item1");
        mappedTable.putItem(item);

        item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value-item2");
        mappedTable.putItem(item);

        long itemCount = mappedTable.scan().items().stream().count();
        assertThat(itemCount, is(1L));

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result, is(item));
    }

    @Test
    public void putItemWithResponse_thenGetItemWithResponse_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item));
        GetItemEnhancedResponse<SimpleBean> getItemEnhancedResponse =
            mappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(getItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItem_withCondition_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

        item.setStringAttribute("stringAttribute-value-updated");

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        mappedTable.putItem(PutItemEnhancedRequest.builder(SimpleBean.class)
                                                  .item(item)
                                                  .conditionExpression(conditionExpression)
                                                  .build());

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result, is(item));
    }

    @Test
    public void putItem_withCondition_throwsConditionalCheckFailedException() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

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

        assertThatThrownBy(() -> mappedTable.putItem(putItemEnhancedRequest))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void updateItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

        item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value-updated");

        SimpleBean result = mappedTable.updateItem(item);
        assertThat(result, is(item));

        long itemCount = mappedTable.scan().stream().count();
        assertThat(itemCount, is(1L));
    }

    @Test
    public void updateItem_createsNewCompleteItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        SimpleBean result = mappedTable.updateItem(item);
        assertThat(result, is(item));
    }

    @Test
    public void updateItem_createsNewPartialItemThenUpdateItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");

        SimpleBean result = mappedTable.updateItem(item);
        assertThat(result, is(item));

        item.setStringAttribute("stringAttribute-value");

        result = mappedTable.updateItem(item);
        assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithNulls_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.updateItem(item);

        item.setStringAttribute(null);

        SimpleBean result = mappedTable.updateItem(item);
        assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithIgnoreNulls_succeeds() {
        SimpleBean item1 = new SimpleBean();
        item1.setId("id-value");
        item1.setSort("sort-value");
        item1.setStringAttribute("stringAttribute-value");

        mappedTable.putItem(item1);

        SimpleBean item2 = new SimpleBean();
        item2.setId("id-value");
        item2.setSort("sort-value");

        UpdateItemEnhancedRequest<SimpleBean> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(SimpleBean.class)
                                 .item(item2)
                                 .ignoreNulls(true)
                                 .build();

        SimpleBean result = mappedTable.updateItem(updateItemEnhancedRequest);
        assertThat(result, is(item1));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        mappedTable.putItem(item);
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

        mappedTable.updateItem(updateItemEnhancedRequest);

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result, is(item));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_throwsConditionalCheckFailedException() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        mappedTable.putItem(item);

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

        assertThatThrownBy(() -> mappedTable.updateItem(updateItemEnhancedRequest))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndDefaultReturnValue_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse = mappedTable.putItemWithResponse(r -> r.item(item));

        item.setStringAttribute("stringAttribute-value-updated");

        UpdateItemEnhancedResponse<SimpleBean> updateItemEnhancedResponse = mappedTable.updateItemWithResponse(r -> r.item(item));

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
            mappedTable.putItemWithResponse(r -> r.item(item));

        SimpleBean item2 = new SimpleBean();
        item2.setId("id-value");
        item2.setSort("sort-value");
        item2.setStringAttribute("stringAttribute-value-updated");

        UpdateItemEnhancedResponse<SimpleBean> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(item).returnValues(ReturnValue.ALL_OLD));

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
            mappedTable.putItemWithResponse(r -> r.item(item));

        SimpleBean item2 = new SimpleBean();
        item2.setId("id-value");
        item2.setSort("sort-value");
        item2.setStringAttribute("stringAttribute-value-updated");

        UpdateItemEnhancedResponse<SimpleBean> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(item2).returnValues(ReturnValue.NONE));

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(nullValue()));
    }

    @Test
    public void deleteItem_itemNotFound_returnsNullValue() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        SimpleBean result = mappedTable.deleteItem(item);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

        SimpleBean beforeDeleteResult = mappedTable.deleteItem(item);
        SimpleBean afterDeleteResult = mappedTable.getItem(item);

        assertThat(beforeDeleteResult, is(item));
        assertThat(afterDeleteResult, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result, is(item));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        Key key = mappedTable.keyFrom(item);
        DeleteItemEnhancedRequest deleteItemEnhancedRequest = DeleteItemEnhancedRequest.builder()
                                                                                       .key(key)
                                                                                       .conditionExpression(conditionExpression)
                                                                                       .build();

        mappedTable.deleteItem(deleteItemEnhancedRequest);

        result = mappedTable.getItem(item);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_throwsConditionalCheckFailedException() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        mappedTable.putItem(item);

        SimpleBean result = mappedTable.getItem(item);
        assertThat(result, is(item));

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedRequest deleteItemEnhancedRequest = DeleteItemEnhancedRequest.builder()
                                                                                       .key(key)
                                                                                       .conditionExpression(conditionExpression)
                                                                                       .build();

        assertThatThrownBy(() -> mappedTable.deleteItem(deleteItemEnhancedRequest))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void deleteItemWithResponse_succeeds() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");

        PutItemEnhancedResponse<SimpleBean> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item));



        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<SimpleBean> deleteItemEnhancedResponse =
            mappedTable.deleteItemWithResponse(r -> r.key(key));

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(deleteItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void deleteItemWithResponse_itemNotFound_returnsNullValue() {
        SimpleBean item = new SimpleBean();
        item.setId("id-value");
        item.setSort("sort-value");
        item.setStringAttribute("stringAttribute-value");
        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<SimpleBean> deleteItemEnhancedResponse =
            mappedTable.deleteItemWithResponse(r -> r.key(key));

        assertThat(deleteItemEnhancedResponse.attributes(), is(nullValue()));
    }
}
