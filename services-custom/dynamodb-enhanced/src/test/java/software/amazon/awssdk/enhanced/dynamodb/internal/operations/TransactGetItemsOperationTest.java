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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.DefaultDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.ItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsResponse;

@RunWith(MockitoJUnitRunner.class)
public class TransactGetItemsOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final String TABLE_NAME_2 = "table-name-2";

    private static final List<FakeItem> FAKE_ITEMS =
        IntStream.range(0, 6).mapToObj($ -> createUniqueFakeItem()).collect(toList());
    private static final List<Map<String, AttributeValue>> FAKE_ITEM_MAPS =
        FAKE_ITEMS.stream()
                  .map(item -> FakeItem.getTableSchema().itemToMap(item, FakeItem.getTableMetadata().primaryKeys()))
                  .collect(toList());
    private static final List<Key> FAKE_ITEM_KEYS =
        FAKE_ITEMS.stream().map(fakeItem -> Key.builder().partitionValue(fakeItem.getId()).build()).collect(toList());

    private static final List<FakeItemWithSort> FAKESORT_ITEMS =
        IntStream.range(0, 6)
                 .mapToObj($ -> createUniqueFakeItemWithSort()).collect(toList());

    private static final List<Map<String, AttributeValue>> FAKESORT_ITEM_MAPS =
        FAKESORT_ITEMS.stream()
                      .map(item -> FakeItemWithSort.getTableSchema()
                                                   .itemToMap(item, FakeItemWithSort.getTableMetadata().primaryKeys()))
                      .collect(toList());
    private static final List<Key> FAKESORT_ITEM_KEYS =
        FAKESORT_ITEMS.stream()
                      .map(fakeItemWithSort -> Key.builder()
                                                  .partitionValue(fakeItemWithSort.getId())
                                                  .sortValue(fakeItemWithSort.getSort())
                                                  .build())
                      .collect(toList());

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockExtension;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;
    private DynamoDbTable<FakeItemWithSort> fakeItemWithSortMappedTable;

    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).extensions().build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
        fakeItemWithSortMappedTable = enhancedClient.table(TABLE_NAME_2, FakeItemWithSort.getTableSchema());
    }

    @Test
    public void generateRequest_getsFromMultipleTables_usingShortcutForm() {
        TransactGetItemsEnhancedRequest transactGetItemsEnhancedRequest =
            TransactGetItemsEnhancedRequest.builder()
                                           .addGetItem(fakeItemMappedTable, FAKE_ITEM_KEYS.get(0))
                                           .addGetItem(fakeItemWithSortMappedTable, FAKESORT_ITEM_KEYS.get(0))
                                           .addGetItem(fakeItemWithSortMappedTable, FAKESORT_ITEM_KEYS.get(1))
                                           .addGetItem(fakeItemMappedTable, FAKE_ITEM_KEYS.get(1))
                                           .build();

        TransactGetItemsOperation operation = TransactGetItemsOperation.create(transactGetItemsEnhancedRequest);

        List<TransactGetItem> transactGetItems = Arrays.asList(
            TransactGetItem.builder().get(Get.builder().tableName(TABLE_NAME).key(FAKE_ITEM_MAPS.get(0)).build()).build(),
            TransactGetItem.builder().get(Get.builder().tableName(TABLE_NAME_2).key(FAKESORT_ITEM_MAPS.get(0)).build()).build(),
            TransactGetItem.builder().get(Get.builder().tableName(TABLE_NAME_2).key(FAKESORT_ITEM_MAPS.get(1)).build()).build(),
            TransactGetItem.builder().get(Get.builder().tableName(TABLE_NAME).key(FAKE_ITEM_MAPS.get(1)).build()).build());

        TransactGetItemsRequest expectedRequest = TransactGetItemsRequest.builder()
                                                                         .transactItems(transactGetItems)
                                                                         .build();

        TransactGetItemsRequest actualRequest = operation.generateRequest(null);

        assertThat(actualRequest, is(expectedRequest));
    }

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse_usingKeyItemForm() {
        TransactGetItemsEnhancedRequest transactGetItemsEnhancedRequest =
            TransactGetItemsEnhancedRequest.builder()
                                           .addGetItem(fakeItemMappedTable, FAKE_ITEMS.get(0))
                                           .build();

        TransactGetItemsOperation operation = TransactGetItemsOperation.create(transactGetItemsEnhancedRequest);

        TransactGetItem transactGetItem =
            TransactGetItem.builder().get(Get.builder().tableName(TABLE_NAME).key(FAKE_ITEM_MAPS.get(0)).build()).build();

        TransactGetItemsRequest transactGetItemsRequest = TransactGetItemsRequest.builder()
                                                                                 .transactItems(singletonList(transactGetItem))
                                                                                 .build();

        TransactGetItemsResponse expectedResponse = TransactGetItemsResponse.builder().build();
        when(mockDynamoDbClient.transactGetItems(any(TransactGetItemsRequest.class))).thenReturn(expectedResponse);

        TransactGetItemsResponse response = operation.serviceCall(mockDynamoDbClient).apply(transactGetItemsRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).transactGetItems(transactGetItemsRequest);
    }

    @Test
    public void transformResponse_noExtension_returnsItemsFromDifferentTables() {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(emptyRequest());

        List<ItemResponse> itemResponses = Arrays.asList(
            ItemResponse.builder().item(FAKE_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(FAKESORT_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(FAKESORT_ITEM_MAPS.get(1)).build(),
            ItemResponse.builder().item(FAKE_ITEM_MAPS.get(1)).build());
        TransactGetItemsResponse response = TransactGetItemsResponse.builder()
                                                                    .responses(itemResponses)
                                                                    .build();

        List<Document> result = operation.transformResponse(response, null);

        assertThat(result, contains(DefaultDocument.create(FAKE_ITEM_MAPS.get(0)),
                                    DefaultDocument.create(FAKESORT_ITEM_MAPS.get(0)),
                                    DefaultDocument.create(FAKESORT_ITEM_MAPS.get(1)),
                                    DefaultDocument.create(FAKE_ITEM_MAPS.get(1))));
    }

    @Test
    public void transformResponse_doesNotInteractWithExtension() {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(emptyRequest());

        List<ItemResponse> itemResponses = Arrays.asList(
            ItemResponse.builder().item(FAKE_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(FAKESORT_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(FAKESORT_ITEM_MAPS.get(1)).build(),
            ItemResponse.builder().item(FAKE_ITEM_MAPS.get(1)).build());
        TransactGetItemsResponse response = TransactGetItemsResponse.builder()
                                                                    .responses(itemResponses)
                                                                    .build();

        operation.transformResponse(response, mockExtension);

        verifyZeroInteractions(mockExtension);
    }

    @Test
    public void transformResponse_noExtension_returnsNullsAsNulls() {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(emptyRequest());

        List<ItemResponse> itemResponses = Arrays.asList(
            ItemResponse.builder().item(FAKE_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(FAKESORT_ITEM_MAPS.get(0)).build(),
            null);
        TransactGetItemsResponse response = TransactGetItemsResponse.builder()
                                                                    .responses(itemResponses)
                                                                    .build();

        List<Document> result = operation.transformResponse(response, null);

        assertThat(result, contains(DefaultDocument.create(FAKE_ITEM_MAPS.get(0)),
                                    DefaultDocument.create(FAKESORT_ITEM_MAPS.get(0)),
                                    null));
    }

    @Test
    public void transformResponse_noExtension_returnsEmptyAsNull() {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(emptyRequest());

        List<ItemResponse> itemResponses = Arrays.asList(
            ItemResponse.builder().item(FAKE_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(FAKESORT_ITEM_MAPS.get(0)).build(),
            ItemResponse.builder().item(emptyMap()).build());
        TransactGetItemsResponse response = TransactGetItemsResponse.builder()
                                                                    .responses(itemResponses)
                                                                    .build();

        List<Document> result = operation.transformResponse(response, null);

        assertThat(result, contains(DefaultDocument.create(FAKE_ITEM_MAPS.get(0)),
                                    DefaultDocument.create(FAKESORT_ITEM_MAPS.get(0)),
                                    DefaultDocument.create(emptyMap())));
    }

    private static TransactGetItemsEnhancedRequest emptyRequest() {
        return TransactGetItemsEnhancedRequest.builder().build();
    }
}
