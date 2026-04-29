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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collection;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
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

@RunWith(Parameterized.class)
public class AnnotatedTableSchemaTest extends LocalDynamoDbSyncTestBase {

    private static final String TABLE_NAME = "table-name";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return AnnotatedTableSchemaTestSupport.parameters();
    }

    @Parameterized.Parameter(0)
    public String schemaType;

    @Parameterized.Parameter(1)
    public Class<Object> itemClass;

    @Parameterized.Parameter(2)
    public TableSchema<Object> tableSchema;

    @Parameterized.Parameter(3)
    public Function<AnnotatedTableSchemaTestSupport.TestItemFactory, Object> fullItem;

    @Parameterized.Parameter(4)
    public Function<AnnotatedTableSchemaTestSupport.TestItemFactory, Object> partialItem;

    @Parameterized.Parameter(5)
    public Function<AnnotatedTableSchemaTestSupport.TestItemFactory, Object> firstItem;

    @Parameterized.Parameter(6)
    public Function<AnnotatedTableSchemaTestSupport.TestItemFactory, Object> secondItem;

    @Parameterized.Parameter(7)
    public Function<AnnotatedTableSchemaTestSupport.TestItemFactory, Object> updatedItem;

    @Parameterized.Parameter(8)
    public Function<AnnotatedTableSchemaTestSupport.TestItemFactory, Object> updatedItemWithNullString;

    @Parameterized.Parameter(9)
    public Class<?> abstractItemClass;

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private DynamoDbTable<Object> mappedTable;
    private AnnotatedTableSchemaTestSupport.TestItemFactory factory;

    @Before
    public void createTable() {
        mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), tableSchema);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        factory = new AnnotatedTableSchemaTestSupport.TestItemFactory();
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
        Assertions.assertThat(describeTableEnhancedResponse.table().tableName())
                  .isEqualTo(getConcreteTableName(TABLE_NAME));
    }

    @Test
    public void createTableWithDefaults_thenDeleteTable_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbTable<Object> anotherMappedTable = enhancedClient.table(getConcreteTableName(tableName), tableSchema);
        anotherMappedTable.createTable();

        TableDescription tableDescription = anotherMappedTable.describeTable().table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        assertThat(actualTableName).isEqualTo(getConcreteTableName(tableName));
        assertThat(actualReadCapacityUnits).isEqualTo(0L);
        assertThat(actualWriteCapacityUnits).isEqualTo(0L);

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName(tableName))
                                                          .build());

        assertThatThrownBy(anotherMappedTable::describeTable)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithProvisionedThroughput_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbTable<Object> anotherMappedTable = enhancedClient.table(getConcreteTableName(tableName), tableSchema);
        anotherMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));

        TableDescription tableDescription = anotherMappedTable.describeTable().table();

        String actualTableName = tableDescription.tableName();
        Long actualReadCapacityUnits = tableDescription.provisionedThroughput().readCapacityUnits();
        Long actualWriteCapacityUnits = tableDescription.provisionedThroughput().writeCapacityUnits();

        assertThat(actualTableName).isEqualTo(getConcreteTableName(tableName));
        assertThat(actualReadCapacityUnits).isEqualTo(getDefaultProvisionedThroughput().readCapacityUnits());
        assertThat(actualWriteCapacityUnits).isEqualTo(getDefaultProvisionedThroughput().writeCapacityUnits());

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName(tableName))
                                                          .build());

        assertThatThrownBy(anotherMappedTable::describeTable)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithDefaults_throwsIllegalArgumentException() {
        TableSchema<?> badSchema = TableSchema.fromClass(abstractItemClass);
        DynamoDbTable<?> other = enhancedClient.table(getConcreteTableName(TABLE_NAME), badSchema);

        assertThatThrownBy(other::createTable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void createTableWithProvisionedThroughput_throwsIllegalArgumentException() {
        TableSchema<?> badSchema = TableSchema.fromClass(abstractItemClass);
        DynamoDbTable<?> other = enhancedClient.table(getConcreteTableName(TABLE_NAME), badSchema);

        assertThatThrownBy(() -> other.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void getItem_itemNotFound_returnsNullValue() {
        Object item = fullItem.apply(factory);

        Object result = mappedTable.getItem(item);
        assertThat(result).isNull();
    }

    @Test
    public void getItemWithResponse_itemNotFound_returnsNullValue() {
        GetItemEnhancedResponse<Object> getItemEnhancedResponse =
            mappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));

        assertThat(getItemEnhancedResponse.attributes()).isNull();
        assertThat(getItemEnhancedResponse.consumedCapacity()).isNull();
    }

    @Test
    public void putItem_thenGetItem_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object result = mappedTable.getItem(item);
        assertThat(result).isEqualTo(item);
    }

    @Test
    public void putItemPartial_thenGetItem_succeeds() {
        Object item = partialItem.apply(factory);
        mappedTable.putItem(item);

        Object result = mappedTable.getItem(item);
        assertThat(result).isEqualTo(item);
    }

    @Test
    public void putItemTwice_thenGetItem_succeeds() {
        Object item1 = firstItem.apply(factory);
        mappedTable.putItem(item1);

        Object item2 = secondItem.apply(factory);
        mappedTable.putItem(item2);

        long itemCount = mappedTable.scan().items().stream().count();
        assertThat(itemCount).isEqualTo(1L);

        Object result = mappedTable.getItem(item2);
        assertThat(result).isEqualTo(item2);
    }

    @Test
    public void putItemWithResponse_thenGetItemWithResponse_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item));
        GetItemEnhancedResponse<Object> getItemEnhancedResponse =
            mappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));

        assertThat(putItemEnhancedResponse.attributes()).isNull();
        assertThat(getItemEnhancedResponse.attributes()).isEqualTo(item);
    }

    @Test
    public void putItem_withCondition_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object updated = updatedItem.apply(factory);

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        mappedTable.putItem(PutItemEnhancedRequest.builder(itemClass)
                                                  .item(updated)
                                                  .conditionExpression(conditionExpression)
                                                  .build());

        Object result = mappedTable.getItem(updated);
        assertThat(result).isEqualTo(updated);
    }

    @Test
    public void putItem_withCondition_throwsConditionalCheckFailedException() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object updated = updatedItem.apply(factory);

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        PutItemEnhancedRequest<Object> putItemEnhancedRequest = PutItemEnhancedRequest.builder(itemClass)
                                                                                      .item(updated)
                                                                                      .conditionExpression(conditionExpression)
                                                                                      .build();

        assertThatThrownBy(() -> mappedTable.putItem(putItemEnhancedRequest))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void updateItem_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object updated = updatedItem.apply(factory);

        Object result = mappedTable.updateItem(updated);
        assertThat(result).isEqualTo(updated);

        long itemCount = mappedTable.scan().stream().count();
        assertThat(itemCount).isEqualTo(1L);
    }

    @Test
    public void updateItem_createsNewCompleteItem_succeeds() {
        Object item = fullItem.apply(factory);

        Object result = mappedTable.updateItem(item);
        assertThat(result).isEqualTo(item);
    }

    @Test
    public void updateItem_createsNewPartialItemThenUpdateItem_succeeds() {
        Object item = partialItem.apply(factory);

        Object result = mappedTable.updateItem(item);
        assertThat(result).isEqualTo(item);

        Object full = fullItem.apply(factory);

        result = mappedTable.updateItem(full);
        assertThat(result).isEqualTo(full);
    }

    @Test
    public void putItem_thenUpdateItemWithNulls_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.updateItem(item);

        Object updatedNullString = updatedItemWithNullString.apply(factory);

        Object result = mappedTable.updateItem(updatedNullString);
        assertThat(result).isEqualTo(updatedNullString);
    }

    @Test
    public void putItem_thenUpdateItemWithIgnoreNulls_succeeds() {
        Object item1 = fullItem.apply(factory);
        mappedTable.putItem(item1);

        Object item2 = partialItem.apply(factory);

        UpdateItemEnhancedRequest<Object> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(itemClass)
                                                                                               .item(item2)
                                                                                               .ignoreNulls(true)
                                                                                               .build();

        Object result = mappedTable.updateItem(updateItemEnhancedRequest);
        assertThat(result).isEqualTo(item1);
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_succeeds() {
        Object item = fullItem.apply(factory);

        mappedTable.putItem(item);

        Object updated = updatedItem.apply(factory);

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("stringAttribute-value"))
                                                   .build();

        UpdateItemEnhancedRequest<Object> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(itemClass)
                                                                                               .item(updated)
                                                                                               .conditionExpression(conditionExpression)
                                                                                               .build();

        mappedTable.updateItem(updateItemEnhancedRequest);

        Object result = mappedTable.getItem(updated);
        assertThat(result).isEqualTo(updated);
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_throwsConditionalCheckFailedException() {
        Object item = fullItem.apply(factory);

        mappedTable.putItem(item);

        Object updated = updatedItem.apply(factory);

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "someAttribute")
                                                   .putExpressionName("#key1", "stringAttribute")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        UpdateItemEnhancedRequest<Object> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(itemClass)
                                                                                               .item(updated)
                                                                                               .conditionExpression(conditionExpression)
                                                                                               .build();

        assertThatThrownBy(() -> mappedTable.updateItem(updateItemEnhancedRequest))
            .isInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndDefaultReturnValue_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse = mappedTable.putItemWithResponse(r -> r.item(item));

        Object updated = updatedItem.apply(factory);

        UpdateItemEnhancedResponse<Object> updateItemEnhancedResponse = mappedTable.updateItemWithResponse(r -> r.item(updated));

        assertThat(putItemEnhancedResponse.attributes()).isNull();
        assertThat(updateItemEnhancedResponse.attributes()).isEqualTo(updated);
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueAllOld_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item));

        Object updated = updatedItem.apply(factory);

        UpdateItemEnhancedResponse<Object> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(updated).returnValues(ReturnValue.ALL_OLD));

        assertThat(putItemEnhancedResponse.attributes()).isNull();
        assertThat(updateItemEnhancedResponse.attributes()).isEqualTo(item);
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueNone_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item));

        Object updated = updatedItem.apply(factory);

        UpdateItemEnhancedResponse<Object> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(updated).returnValues(ReturnValue.NONE));

        assertThat(putItemEnhancedResponse.attributes()).isNull();
        assertThat(updateItemEnhancedResponse.attributes()).isNull();
    }

    @Test
    public void deleteItem_itemNotFound_returnsNullValue() {
        Object item = fullItem.apply(factory);

        Object result = mappedTable.deleteItem(item);
        assertThat(result).isNull();
    }

    @Test
    public void deleteItem_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object beforeDeleteResult = mappedTable.deleteItem(item);
        Object afterDeleteResult = mappedTable.getItem(item);

        assertThat(beforeDeleteResult).isEqualTo(item);
        assertThat(afterDeleteResult).isNull();
    }

    @Test
    public void deleteItem_withCondition_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object result = mappedTable.getItem(item);
        assertThat(result).isEqualTo(item);

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
        assertThat(result).isNull();
    }

    @Test
    public void deleteItem_withCondition_throwsConditionalCheckFailedException() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item);

        Object result = mappedTable.getItem(item);
        assertThat(result).isEqualTo(item);

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
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item));

        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<Object> deleteItemEnhancedResponse =
            mappedTable.deleteItemWithResponse(r -> r.key(key));

        assertThat(putItemEnhancedResponse.attributes()).isNull();
        assertThat(deleteItemEnhancedResponse.attributes()).isEqualTo(item);
    }

    @Test
    public void deleteItemWithResponse_itemNotFound_returnsNullValue() {
        Object item = fullItem.apply(factory);
        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<Object> deleteItemEnhancedResponse =
            mappedTable.deleteItemWithResponse(r -> r.key(key));

        assertThat(deleteItemEnhancedResponse.attributes()).isNull();
    }
}

