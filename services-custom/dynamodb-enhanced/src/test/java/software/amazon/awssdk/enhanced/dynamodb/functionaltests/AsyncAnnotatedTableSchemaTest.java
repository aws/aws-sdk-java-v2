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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AbstractBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.AbstractImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.SimpleBean;
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

@RunWith(Parameterized.class)
public class AsyncAnnotatedTableSchemaTest extends LocalDynamoDbAsyncTestBase {

    private static final String TABLE_NAME = "table-name";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {
                "@DynamoDbBean",
                SimpleBean.class,
                TableSchema.fromClass(SimpleBean.class),
                (Function<TestItemFactory, Object>) TestItemFactory::bean,
                (Function<TestItemFactory, Object>) TestItemFactory::beanPartial,
                (Function<TestItemFactory, Object>) TestItemFactory::beanItem1,
                (Function<TestItemFactory, Object>) TestItemFactory::beanItem2,
                (Function<TestItemFactory, Object>) TestItemFactory::beanUpdated,
                (Function<TestItemFactory, Object>) TestItemFactory::beanUpdatedNullString,
                AbstractBean.class
            },
            {
                "@DynamoDbImmutable",
                SimpleImmutable.class,
                TableSchema.fromClass(SimpleImmutable.class),
                (Function<TestItemFactory, Object>) TestItemFactory::immutable,
                (Function<TestItemFactory, Object>) TestItemFactory::immutablePartial,
                (Function<TestItemFactory, Object>) TestItemFactory::immutableItem1,
                (Function<TestItemFactory, Object>) TestItemFactory::immutableItem2,
                (Function<TestItemFactory, Object>) TestItemFactory::immutableUpdated,
                (Function<TestItemFactory, Object>) TestItemFactory::immutableUpdatedNullString,
                AbstractImmutable.class
            }
        });
    }

    @Parameterized.Parameter(0)
    public String schemaType;

    @Parameterized.Parameter(1)
    public Class<Object> itemClass;

    @Parameterized.Parameter(2)
    public TableSchema<Object> tableSchema;

    @Parameterized.Parameter(3)
    public Function<TestItemFactory, Object> fullItem;

    @Parameterized.Parameter(4)
    public Function<TestItemFactory, Object> partialItem;

    @Parameterized.Parameter(5)
    public Function<TestItemFactory, Object> firstItem;

    @Parameterized.Parameter(6)
    public Function<TestItemFactory, Object> secondItem;

    @Parameterized.Parameter(7)
    public Function<TestItemFactory, Object> updatedItem;

    @Parameterized.Parameter(8)
    public Function<TestItemFactory, Object> updatedItemWithNullString;

    @Parameterized.Parameter(9)
    public Class<?> abstractItemClass;

    private final DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                                                                          .dynamoDbClient(getDynamoDbAsyncClient())
                                                                                          .build();

    private DynamoDbAsyncTable<Object> mappedTable;
    private TestItemFactory factory;

    @Before
    public void createTable() {
        mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), tableSchema);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();

        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(getConcreteTableName(TABLE_NAME))).join();

        factory = new TestItemFactory();
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
        DescribeTableEnhancedResponse describeTableEnhancedResponse = mappedTable.describeTable().join();
        Assertions.assertThat(describeTableEnhancedResponse.table()).isNotNull();
        Assertions.assertThat(describeTableEnhancedResponse.table().tableName())
                  .isEqualTo(getConcreteTableName(TABLE_NAME));
    }

    @Test
    public void createTableWithDefaults_thenDeleteTable_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbAsyncTable<Object> anotherMappedTable = enhancedClient.table(getConcreteTableName(tableName), tableSchema);
        anotherMappedTable.createTable().join();

        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(getConcreteTableName(tableName))).join();

        DescribeTableEnhancedResponse describeTableEnhancedResponse = anotherMappedTable.describeTable().join();
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

        assertThatThrownBy(() -> anotherMappedTable.describeTable().join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithProvisionedThroughput_succeeds() {
        String tableName = TABLE_NAME + "-1";

        DynamoDbAsyncTable<Object> anotherMappedTable = enhancedClient.table(getConcreteTableName(tableName), tableSchema);
        anotherMappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();

        getDynamoDbAsyncClient().waiter().waitUntilTableExists(b -> b.tableName(getConcreteTableName(tableName))).join();

        DescribeTableEnhancedResponse describeTableEnhancedResponse = anotherMappedTable.describeTable().join();
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

        assertThatThrownBy(() -> anotherMappedTable.describeTable().join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Cannot do operations on a non-existent table");
    }

    @Test
    public void createTableWithDefaults_throwsIllegalArgumentException() {
        TableSchema<?> badSchema = TableSchema.fromClass(abstractItemClass);
        DynamoDbAsyncTable<?> other = enhancedClient.table(getConcreteTableName(TABLE_NAME), badSchema);

        assertThatThrownBy(() -> other.createTable().join())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void createTableWithProvisionedThroughput_throwsIllegalArgumentException() {
        TableSchema<?> badSchema = TableSchema.fromClass(abstractItemClass);
        DynamoDbAsyncTable<?> other = enhancedClient.table(getConcreteTableName(TABLE_NAME), badSchema);

        assertThatThrownBy(() -> other.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Attempt to execute an operation that requires a primary index without defining any primary"
                        + " key attributes in the table metadata.");
    }

    @Test
    public void getItem_itemNotFound_returnsNullValue() {
        Object item = fullItem.apply(factory);

        Object result = mappedTable.getItem(item).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void getItemWithResponse_itemNotFound_returnsNullValue() {
        GetItemEnhancedResponse<Object> getItemEnhancedResponse =
            mappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(getItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(getItemEnhancedResponse.consumedCapacity(), is(nullValue()));
    }

    @Test
    public void putItem_thenGetItem_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

        Object result = mappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItemPartial_thenGetItem_succeeds() {
        Object item = partialItem.apply(factory);
        mappedTable.putItem(item).join();

        Object result = mappedTable.getItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void putItemTwice_thenGetItem_succeeds() {
        Object item1 = firstItem.apply(factory);
        mappedTable.putItem(item1).join();

        Object item2 = secondItem.apply(factory);
        mappedTable.putItem(item2).join();

        SdkPublisher<Object> publisher = mappedTable.scan().items();
        drainPublisher(publisher, 1);

        Object result = mappedTable.getItem(item2).join();
        assertThat(result, is(item2));
    }

    @Test
    public void putItemWithResponse_thenGetItemWithResponse_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item)).join();
        GetItemEnhancedResponse<Object> getItemEnhancedResponse =
            mappedTable.getItemWithResponse(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(getItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItem_withCondition_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

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
                                                  .build()).join();

        Object result = mappedTable.getItem(updated).join();
        assertThat(result, is(updated));
    }

    @Test
    public void putItem_withCondition_throwsConditionalCheckFailedException() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

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

        assertThatThrownBy(() -> mappedTable.putItem(putItemEnhancedRequest).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void updateItem_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

        Object updated = updatedItem.apply(factory);

        Object result = mappedTable.updateItem(updated).join();
        assertThat(result, is(updated));

        SdkPublisher<Object> publisher = mappedTable.scan().items();
        drainPublisher(publisher, 1);
    }

    @Test
    public void updateItem_createsNewCompleteItem_succeeds() {
        Object item = fullItem.apply(factory);

        Object result = mappedTable.updateItem(item).join();
        assertThat(result, is(item));
    }

    @Test
    public void updateItem_createsNewPartialItemThenUpdateItem_succeeds() {
        Object item = partialItem.apply(factory);

        Object result = mappedTable.updateItem(item).join();
        assertThat(result, is(item));

        Object full = fullItem.apply(factory);

        result = mappedTable.updateItem(full).join();
        assertThat(result, is(full));
    }

    @Test
    public void putItem_thenUpdateItemWithNulls_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.updateItem(item).join();

        Object updatedNullString = updatedItemWithNullString.apply(factory);

        Object result = mappedTable.updateItem(updatedNullString).join();
        assertThat(result, is(updatedNullString));
    }

    @Test
    public void putItem_thenUpdateItemWithIgnoreNulls_succeeds() {
        Object item1 = fullItem.apply(factory);
        mappedTable.putItem(item1).join();

        Object partial = partialItem.apply(factory);

        UpdateItemEnhancedRequest<Object> updateItemEnhancedRequest = UpdateItemEnhancedRequest.builder(itemClass)
                                                                                               .item(partial)
                                                                                               .ignoreNulls(true)
                                                                                               .build();

        Object result = mappedTable.updateItem(updateItemEnhancedRequest).join();
        assertThat(result, is(item1));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

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

        mappedTable.updateItem(updateItemEnhancedRequest).join();

        Object result = mappedTable.getItem(updated).join();
        assertThat(result, is(updated));
    }

    @Test
    public void putItem_thenUpdateItemWithCondition_throwsConditionalCheckFailedException() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

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

        assertThatThrownBy(() -> mappedTable.updateItem(updateItemEnhancedRequest).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndDefaultReturnValue_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item)).join();

        Object updated = updatedItem.apply(factory);

        UpdateItemEnhancedResponse<Object> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(updated)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(updated));
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueAllOld_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item)).join();

        Object updated = updatedItem.apply(factory);

        UpdateItemEnhancedResponse<Object> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(updated).returnValues(ReturnValue.ALL_OLD)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void putItemWithResponse_thenUpdateItemWithResponseAndReturnValueNone_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item)).join();

        Object updated = updatedItem.apply(factory);

        UpdateItemEnhancedResponse<Object> updateItemEnhancedResponse =
            mappedTable.updateItemWithResponse(r -> r.item(updated).returnValues(ReturnValue.NONE)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(updateItemEnhancedResponse.attributes(), is(nullValue()));
    }

    @Test
    public void deleteItem_itemNotFound_returnsNullValue() {
        Object item = fullItem.apply(factory);

        Object result = mappedTable.deleteItem(item).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

        Object beforeDeleteResult = mappedTable.deleteItem(item).join();
        Object afterDeleteResult = mappedTable.getItem(item).join();

        assertThat(beforeDeleteResult, is(item));
        assertThat(afterDeleteResult, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_succeeds() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

        Object result = mappedTable.getItem(item).join();
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

        mappedTable.deleteItem(deleteItemEnhancedRequest).join();

        result = mappedTable.getItem(item).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteItem_withCondition_throwsConditionalCheckFailedException() {
        Object item = fullItem.apply(factory);
        mappedTable.putItem(item).join();

        Object result = mappedTable.getItem(item).join();
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

        assertThatThrownBy(() -> mappedTable.deleteItem(deleteItemEnhancedRequest).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseInstanceOf(ConditionalCheckFailedException.class)
            .hasMessageContaining("The conditional request failed");
    }

    @Test
    public void deleteItemWithResponse_succeeds() {
        Object item = fullItem.apply(factory);

        PutItemEnhancedResponse<Object> putItemEnhancedResponse =
            mappedTable.putItemWithResponse(r -> r.item(item)).join();

        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<Object> deleteItemEnhancedResponse =
            mappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        assertThat(putItemEnhancedResponse.attributes(), is(nullValue()));
        assertThat(deleteItemEnhancedResponse.attributes(), is(item));
    }

    @Test
    public void deleteItemWithResponse_itemNotFound_returnsNullValue() {
        Object item = fullItem.apply(factory);
        Key key = mappedTable.keyFrom(item);

        DeleteItemEnhancedResponse<Object> deleteItemEnhancedResponse =
            mappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        assertThat(deleteItemEnhancedResponse.attributes(), is(nullValue()));
    }

    private static final class TestItemFactory {
        private final String id = "id-value";
        private final String sort = "sort-value";

        Object bean() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            item.setStringAttribute("stringAttribute-value");
            return item;
        }

        Object beanPartial() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            return item;
        }

        Object beanItem1() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            item.setStringAttribute("stringAttribute-value-item1");
            return item;
        }

        Object beanItem2() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            item.setStringAttribute("stringAttribute-value-item2");
            return item;
        }

        Object beanUpdated() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            item.setStringAttribute("stringAttribute-value-updated");
            return item;
        }

        Object beanUpdatedNullString() {
            SimpleBean item = new SimpleBean();
            item.setId(id);
            item.setSort(sort);
            item.setStringAttribute(null);
            return item;
        }

        Object immutable() {
            return SimpleImmutable.builder()
                                 .id(id)
                                 .sort(sort)
                                 .stringAttribute("stringAttribute-value")
                                 .build();
        }

        Object immutablePartial() {
            return SimpleImmutable.builder()
                                 .id(id)
                                 .sort(sort)
                                 .build();
        }

        Object immutableItem1() {
            return SimpleImmutable.builder()
                                 .id(id)
                                 .sort(sort)
                                 .stringAttribute("stringAttribute-value-item1")
                                 .build();
        }

        Object immutableItem2() {
            return SimpleImmutable.builder()
                                 .id(id)
                                 .sort(sort)
                                 .stringAttribute("stringAttribute-value-item2")
                                 .build();
        }

        Object immutableUpdated() {
            return SimpleImmutable.builder()
                                 .id(id)
                                 .sort(sort)
                                 .stringAttribute("stringAttribute-value-updated")
                                 .build();
        }

        Object immutableUpdatedNullString() {
            return SimpleImmutable.builder()
                                 .id(id)
                                 .sort(sort)
                                 .stringAttribute(null)
                                 .build();
        }
    }
}

