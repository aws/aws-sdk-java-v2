/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.DynamoDbMappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.BatchGetItem.ResultsPage;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.paginators.BatchGetItemIterable;

@RunWith(MockitoJUnitRunner.class)
public class BatchGetItemTest {
    private static final String TABLE_NAME = "table-name";
    private static final String TABLE_NAME_2 = "table-name-2";

    private static final List<FakeItem> FAKE_ITEMS =
        IntStream.range(0, 6).mapToObj($ -> createUniqueFakeItem()).collect(toList());
    private static final List<Map<String, AttributeValue>> FAKE_ITEM_MAPS = FAKE_ITEMS.stream().map(item ->
        FakeItem.getTableSchema().itemToMap(item, FakeItem.getTableMetadata().primaryKeys())).collect(toList());
    private static final List<FakeItemWithSort> FAKESORT_ITEMS =
        IntStream.range(0, 6).mapToObj($ -> createUniqueFakeItemWithSort()).collect(toList());
    private static final List<Map<String, AttributeValue>> FAKESORT_ITEM_MAPS = FAKESORT_ITEMS.stream().map(item ->
        FakeItemWithSort.getTableSchema().itemToMap(item, FakeItemWithSort.getTableMetadata().primaryKeys()))
          .collect(toList());
    private static final List<Key> FAKE_ITEM_KEYS =
        FAKE_ITEMS.stream().map(fakeItem -> Key.create(stringValue(fakeItem.getId()))).collect(toList());
    private static final List<Key> FAKESORT_ITEM_KEYS =
        FAKESORT_ITEMS.stream().map(fakeItemWithSort -> Key.create(stringValue(fakeItemWithSort.getId()),
                                                               stringValue(fakeItemWithSort.getSort()))).collect(toList());

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private MapperExtension mockExtension;

    private MappedDatabase mappedDatabase;
    private MappedTable<FakeItem> fakeItemMappedTable;
    private MappedTable<FakeItemWithSort> fakeItemWithSortMappedTable;

    @Before
    public void setupMappedTables() {
        mappedDatabase = DynamoDbMappedDatabase.builder().dynamoDbClient(mockDynamoDbClient).build();
        fakeItemMappedTable = mappedDatabase.table(TABLE_NAME, FakeItem.getTableSchema());
        fakeItemWithSortMappedTable = mappedDatabase.table(TABLE_NAME_2, FakeItemWithSort.getTableSchema());
    }

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        BatchGetItem operation =
            BatchGetItem.create(ReadBatch.create(fakeItemMappedTable, GetItem.create(FAKE_ITEM_KEYS.get(0))));

        BatchGetItemRequest batchGetItemRequest =
            BatchGetItemRequest.builder()
                               .requestItems(singletonMap("test-table",
                                                          KeysAndAttributes.builder()
                                                                           .keys(singletonList(FAKE_ITEM_MAPS.get(0)))
                                                                           .build()))
                               .build();

        BatchGetItemIterable expectedResponse = mock(BatchGetItemIterable.class);
        when(mockDynamoDbClient.batchGetItemPaginator(any(BatchGetItemRequest.class))).thenReturn(expectedResponse);

        SdkIterable<BatchGetItemResponse> response =
            operation.serviceCall(mockDynamoDbClient).apply(batchGetItemRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).batchGetItemPaginator(batchGetItemRequest);
    }

    @Test
    public void generateRequest_multipleBatches_multipleTableSchemas() {
        BatchGetItem operation =
            BatchGetItem.create(ReadBatch.create(fakeItemMappedTable,
                                         GetItem.create(FAKE_ITEM_KEYS.get(0)),
                                         GetItem.create(FAKE_ITEM_KEYS.get(1)),
                                         GetItem.create(FAKE_ITEM_KEYS.get(2))),
                            ReadBatch.create(fakeItemWithSortMappedTable,
                                         GetItem.create(FAKESORT_ITEM_KEYS.get(0)),
                                         GetItem.create(FAKESORT_ITEM_KEYS.get(1)),
                                         GetItem.create(FAKESORT_ITEM_KEYS.get(2))));

        BatchGetItemRequest batchGetItemRequest = operation.generateRequest(mockExtension);

        KeysAndAttributes keysAndAttributes1 = batchGetItemRequest.requestItems().get(TABLE_NAME);
        KeysAndAttributes keysAndAttributes2 = batchGetItemRequest.requestItems().get(TABLE_NAME_2);
        assertThat(keysAndAttributes1.keys(), containsInAnyOrder(FAKE_ITEM_MAPS.subList(0, 3).toArray()));
        assertThat(keysAndAttributes2.keys(), containsInAnyOrder(FAKESORT_ITEM_MAPS.subList(0, 3).toArray()));
        assertThat(keysAndAttributes1.consistentRead(), is(nullValue()));
        assertThat(keysAndAttributes2.consistentRead(), is(nullValue()));
        verifyNoMoreInteractions(mockExtension);
    }

    @Test
    public void generateRequest_multipleBatches_multipleTableSchemas_nonConflictingConsistentRead() {
        BatchGetItem operation =
            BatchGetItem.create(
                ReadBatch.create(fakeItemMappedTable,
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(0)).consistentRead(true).build(),
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(1)).consistentRead(true).build(),
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(2)).consistentRead(true).build()),
                ReadBatch.create(fakeItemWithSortMappedTable,
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(0)).consistentRead(false).build(),
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(1)).consistentRead(false).build(),
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(2)).consistentRead(false).build()));

        BatchGetItemRequest batchGetItemRequest = operation.generateRequest(mockExtension);

        KeysAndAttributes keysAndAttributes1 = batchGetItemRequest.requestItems().get(TABLE_NAME);
        KeysAndAttributes keysAndAttributes2 = batchGetItemRequest.requestItems().get(TABLE_NAME_2);
        assertThat(keysAndAttributes1.keys(), containsInAnyOrder(FAKE_ITEM_MAPS.subList(0, 3).toArray()));
        assertThat(keysAndAttributes2.keys(), containsInAnyOrder(FAKESORT_ITEM_MAPS.subList(0, 3).toArray()));
        assertThat(keysAndAttributes1.consistentRead(), is(true));
        assertThat(keysAndAttributes2.consistentRead(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_multipleBatches_multipleTableSchemas_ConflictingConsistentReadStartingWithNull() {
        BatchGetItem operation =
            BatchGetItem.create(
                ReadBatch.create(fakeItemMappedTable,
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(0)).consistentRead(true).build(),
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(1)).consistentRead(true).build(),
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(2)).consistentRead(true).build()),
                ReadBatch.create(fakeItemWithSortMappedTable,
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(0)).build(),
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(1)).consistentRead(true).build(),
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(2)).consistentRead(true).build()));

        operation.generateRequest(mockExtension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_multipleBatches_multipleTableSchemas_ConflictingConsistentReadStartingWithFalse() {
        BatchGetItem operation =
            BatchGetItem.create(
                ReadBatch.create(fakeItemMappedTable,
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(0)).consistentRead(true).build(),
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(1)).consistentRead(true).build(),
                             GetItem.builder().key(FAKE_ITEM_KEYS.get(2)).consistentRead(true).build()),
                ReadBatch.create(fakeItemWithSortMappedTable,
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(0)).consistentRead(false).build(),
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(1)).consistentRead(true).build(),
                             GetItem.builder().key(FAKESORT_ITEM_KEYS.get(2)).consistentRead(true).build()));

        operation.generateRequest(mockExtension);
    }

    @Test
    public void transformResponse_multipleTables_multipleItems_noExtension() {
        Map<String, List<Map<String, AttributeValue>>> page = new HashMap<>();
        page.put(TABLE_NAME, Arrays.asList(FAKE_ITEM_MAPS.get(0), FAKE_ITEM_MAPS.get(1)));
        page.put(TABLE_NAME_2, singletonList(FAKESORT_ITEM_MAPS.get(0)));

        BatchGetItemResponse fakeResults = generateFakeResults(page);
        BatchGetItem operation = BatchGetItem.create();

        ResultsPage resultsPage = operation.transformResponse(fakeResults, null);

        List<FakeItem> fakeItemResultsPage = resultsPage.getResultsForTable(fakeItemMappedTable);
        List<FakeItemWithSort> fakeItemWithSortResultsPage =
            resultsPage.getResultsForTable(fakeItemWithSortMappedTable);

        assertThat(fakeItemResultsPage, containsInAnyOrder(FAKE_ITEMS.get(0), FAKE_ITEMS.get(1)));
        assertThat(fakeItemWithSortResultsPage, containsInAnyOrder(FAKESORT_ITEMS.get(0)));
    }

    @Test
    public void transformResponse_multipleTables_multipleItems_extensionWithTransformation() {
        Map<String, List<Map<String, AttributeValue>>> page = new HashMap<>();
        page.put(TABLE_NAME, Arrays.asList(FAKE_ITEM_MAPS.get(0), FAKE_ITEM_MAPS.get(1)));
        page.put(TABLE_NAME_2, singletonList(FAKESORT_ITEM_MAPS.get(0)));
        BatchGetItemResponse fakeResults = generateFakeResults(page);
        BatchGetItem operation = BatchGetItem.create();

        // Use the mock extension to transform every item based on table name
        IntStream.range(0, 3).forEach(i -> {
            doReturn(ReadModification.builder().transformedItem(FAKE_ITEM_MAPS.get(i + 3)).build())
                .when(mockExtension)
                .afterRead(eq(FAKE_ITEM_MAPS.get(i)),
                           argThat(operationContext -> operationContext.tableName().equals(TABLE_NAME)),
                           any(TableMetadata.class));
            doReturn(ReadModification.builder().transformedItem(FAKESORT_ITEM_MAPS.get(i + 3)).build())
                .when(mockExtension)
                .afterRead(eq(FAKESORT_ITEM_MAPS.get(i)),
                           argThat(operationContext ->
                                       operationContext.tableName().equals(TABLE_NAME_2)),
                           any(TableMetadata.class));
        });

        ResultsPage resultsPage = operation.transformResponse(fakeResults, mockExtension);

        List<FakeItem> fakeItemResultsPage = resultsPage.getResultsForTable(fakeItemMappedTable);
        List<FakeItemWithSort> fakeItemWithSortResultsPage =
            resultsPage.getResultsForTable(fakeItemWithSortMappedTable);


        assertThat(fakeItemResultsPage, containsInAnyOrder(FAKE_ITEMS.get(3), FAKE_ITEMS.get(4)));
        assertThat(fakeItemWithSortResultsPage, containsInAnyOrder(FAKESORT_ITEMS.get(3)));
    }

    @Test
    public void transformResponse_queryingEmptyResults() {
        BatchGetItemResponse fakeResults = generateFakeResults(emptyMap());
        BatchGetItem operation = BatchGetItem.create();

        ResultsPage resultsPage = operation.transformResponse(fakeResults, null);

        assertThat(resultsPage.getResultsForTable(fakeItemMappedTable), is(emptyList()));
    }

    private static BatchGetItemResponse generateFakeResults(
        Map<String, List<Map<String, AttributeValue>>> itemMapsPage) {

        return BatchGetItemResponse.builder()
                                   .responses(itemMapsPage)
                                   .build();
    }
}
