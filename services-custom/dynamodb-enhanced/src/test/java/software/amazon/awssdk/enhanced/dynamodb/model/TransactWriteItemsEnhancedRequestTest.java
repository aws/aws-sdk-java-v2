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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@RunWith(MockitoJUnitRunner.class)
public class TransactWriteItemsEnhancedRequestTest {

    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;


    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).extensions().build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
    }


    @Test
    public void builder_minimal() {
        TransactWriteItemsEnhancedRequest builtObject = TransactWriteItemsEnhancedRequest.builder().build();

        assertThat(builtObject.transactWriteItems(), is(nullValue()));
    }

    @Test
    public void builder_maximal_consumer_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#attribute = :attribute")
                                                   .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                   .expressionNames(singletonMap("#attribute", "attribute"))
                                                   .build();

        TransactWriteItemsEnhancedRequest builtObject =
            TransactWriteItemsEnhancedRequest.builder()
                                             .addPutItem(fakeItemMappedTable, FakeItem.class, r -> r.item(fakeItem))
                                             .addDeleteItem(fakeItemMappedTable, r -> r.key(k -> k.partitionValue(fakeItem.getId())))
                                             .addUpdateItem(fakeItemMappedTable, FakeItem.class, r -> r.item(fakeItem))
                                             .addConditionCheck(fakeItemMappedTable, r -> r.key(k -> k.partitionValue(fakeItem.getId()))
                                                                                           .conditionExpression(conditionExpression))
                                             .build();

        assertThat(builtObject.transactWriteItems().size(), is(4));
        assertThat(builtObject.transactWriteItems().get(0), is(getTransactWriteItems(fakeItem).get(0)));
        assertThat(builtObject.transactWriteItems().get(1), is(getTransactWriteItems(fakeItem).get(1)));

        assertThat(builtObject.transactWriteItems().get(2).update(), is(notNullValue()));
        assertThat(builtObject.transactWriteItems().get(2).update().key().get("id").s(), is(fakeItem.getId()));

        assertThat(builtObject.transactWriteItems().get(3).conditionCheck(), is(notNullValue()));
        assertThat(builtObject.transactWriteItems().get(3).conditionCheck().key().get("id").s(), is(fakeItem.getId()));
    }

    @Test
    public void builder_maximal_builder_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        PutItemEnhancedRequest<FakeItem> putItem = PutItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build();
        DeleteItemEnhancedRequest deleteItem = DeleteItemEnhancedRequest.builder()
                                                                        .key(k -> k.partitionValue(fakeItem.getId()))
                                                                        .build();
        UpdateItemEnhancedRequest<FakeItem> updateItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                        .item(fakeItem).build();
        Expression conditionExpression = Expression.builder()
                                                   .expression("#attribute = :attribute")
                                                   .expressionValues(singletonMap(":attribute", stringValue("0")))
                                                   .expressionNames(singletonMap("#attribute", "attribute"))
                                                   .build();
        ConditionCheck<FakeItem> conditionCheck = ConditionCheck.builder()
                                                      .key(k -> k.partitionValue(fakeItem.getId()))
                                                      .conditionExpression(conditionExpression)
                                                      .build();

        TransactWriteItemsEnhancedRequest builtObject =
            TransactWriteItemsEnhancedRequest.builder()
                                             .addPutItem(fakeItemMappedTable, putItem)
                                             .addDeleteItem(fakeItemMappedTable, deleteItem)
                                             .addUpdateItem(fakeItemMappedTable, updateItem)
                                             .addConditionCheck(fakeItemMappedTable, conditionCheck)
                                             .build();

        assertThat(builtObject.transactWriteItems().size(), is(4));
        assertThat(builtObject.transactWriteItems().get(0), is(getTransactWriteItems(fakeItem).get(0)));
        assertThat(builtObject.transactWriteItems().get(1), is(getTransactWriteItems(fakeItem).get(1)));

        assertThat(builtObject.transactWriteItems().get(2).update(), is(notNullValue()));
        assertThat(builtObject.transactWriteItems().get(2).update().key().get("id").s(), is(fakeItem.getId()));

        assertThat(builtObject.transactWriteItems().get(3).conditionCheck(), is(notNullValue()));
        assertThat(builtObject.transactWriteItems().get(3).conditionCheck().key().get("id").s(), is(fakeItem.getId()));
    }

    private List<TransactWriteItem> getTransactWriteItems(FakeItem fakeItem) {
        final Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);

        TransactWriteItem putWriteItem = TransactWriteItem.builder()
                                                          .put(Put.builder()
                                                                  .item(fakeItemMap)
                                                                  .tableName(TABLE_NAME)
                                                                  .build())
                                                          .build();
        TransactWriteItem deleteWriteItem = TransactWriteItem.builder()
                                                             .delete(Delete.builder()
                                                                           .key(fakeItemMap)
                                                                           .tableName(TABLE_NAME)
                                                                           .build())
                                                             .build();

        return Arrays.asList(putWriteItem, deleteWriteItem);
    }

}
