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

import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@RunWith(MockitoJUnitRunner.class)
public class WriteBatchTest {

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
        WriteBatch builtObject = WriteBatch.builder(FakeItem.class).build();

        assertThat(builtObject.tableName(), is(nullValue()));
        assertThat(builtObject.writeRequests(), is(nullValue()));
    }

    @Test
    public void builder_maximal_consumer_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        WriteBatch builtObject = WriteBatch.builder(FakeItem.class)
                                           .mappedTableResource(fakeItemMappedTable)
                                           .addPutItem(r -> r.item(fakeItem))
                                           .addDeleteItem(r -> r.key(k -> k.partitionValue(fakeItem.getId())))
                                           .build();

        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem,
                                                                                      FakeItem.getTableMetadata().primaryKeys());

        assertThat(builtObject.tableName(), is(TABLE_NAME));
        assertThat(builtObject.writeRequests(), containsInAnyOrder(putRequest(fakeItemMap), deleteRequest(fakeItemMap)));
    }

    @Test
    public void builder_maximal_builder_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        PutItemEnhancedRequest<FakeItem> putItem = PutItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build();
        DeleteItemEnhancedRequest deleteItem = DeleteItemEnhancedRequest.builder()
                                                                        .key(k -> k.partitionValue(fakeItem.getId()))
                                                                        .build();

        WriteBatch builtObject = WriteBatch.builder(FakeItem.class)
                                           .mappedTableResource(fakeItemMappedTable)
                                           .addPutItem(putItem)
                                           .addDeleteItem(deleteItem)
                                           .build();

        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem,
                                                                                      FakeItem.getTableMetadata().primaryKeys());

        assertThat(builtObject.tableName(), is(TABLE_NAME));
        assertThat(builtObject.writeRequests(), containsInAnyOrder(putRequest(fakeItemMap), deleteRequest(fakeItemMap)));
    }

    @Test
    public void builder_missing_mapped_table_resource_error_message() {
        FakeItem fakeItem = createUniqueFakeItem();
        PutItemEnhancedRequest<FakeItem> putItemRequest = PutItemEnhancedRequest.builder(FakeItem.class).item(fakeItem).build();

        Key partitionKey = Key.builder().partitionValue(fakeItem.getId()).build();
        DeleteItemEnhancedRequest deleteItemRequest = DeleteItemEnhancedRequest.builder()
                                                                               .key(partitionKey)
                                                                               .build();

        WriteBatch.Builder<FakeItem> builder = WriteBatch.builder(FakeItem.class);

        String errorMesageAfterBuild = "A mappedTableResource (table) is required when generating the write requests for "
                                       + "WriteBatch";

        assertThrowsMappedTableResourceNullException(() -> builder.addPutItem(putItemRequest).build(),
                                                     errorMesageAfterBuild);
        assertThrowsMappedTableResourceNullException(() -> builder.addPutItem(fakeItem).build(),
                                                     errorMesageAfterBuild);
        assertThrowsMappedTableResourceNullException(() -> builder.addPutItem(r -> r.item(fakeItem)).build(),
                                                     errorMesageAfterBuild);
        assertThrowsMappedTableResourceNullException(() -> builder.addDeleteItem(r -> r.key(partitionKey)).build(),
                                                     errorMesageAfterBuild);
        assertThrowsMappedTableResourceNullException(() -> builder.addDeleteItem(deleteItemRequest).build(),
                                                     errorMesageAfterBuild);

        String errorMessageDeleteKeyFromItem = "A mappedTableResource is required to derive a key from the given keyItem";
        assertThrowsMappedTableResourceNullException(() -> builder.addDeleteItem(fakeItem).build(),
                                                     errorMessageDeleteKeyFromItem);
    }

    @Test
    public void test_write_batch_equalsAndHashcode() {

        EqualsVerifier.forClass(WriteBatch.class)
                      .withNonnullFields("tableName", "writeRequests")
                      .withPrefabValues(WriteRequest.class,
                                        WriteRequest.builder().putRequest(PutRequest.builder().build()).build(),
                                        WriteRequest.builder().deleteRequest(DeleteRequest.builder().build()).build())
                      .verify();
    }


    private static void assertThrowsMappedTableResourceNullException(ThrowableAssert.ThrowingCallable runnable, String message) {
        assertThatThrownBy(runnable).isInstanceOf(NullPointerException.class).hasMessage(message);
    }

    private static WriteRequest putRequest(Map<String, AttributeValue> itemMap) {
        return WriteRequest.builder().putRequest(PutRequest.builder().item(itemMap).build()).build();
    }

    private static WriteRequest deleteRequest(Map<String, AttributeValue> itemMap) {
        return WriteRequest.builder().deleteRequest(DeleteRequest.builder().key(itemMap).build()).build();
    }

}
