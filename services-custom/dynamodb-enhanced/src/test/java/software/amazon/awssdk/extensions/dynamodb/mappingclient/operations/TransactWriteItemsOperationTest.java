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

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.WriteTransaction;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

@RunWith(MockitoJUnitRunner.class)
public class TransactWriteItemsOperationTest {
    private static final String TABLE_NAME = "table-name";

    private final FakeItem fakeItem1 = FakeItem.createUniqueFakeItem();
    private final FakeItem fakeItem2 = FakeItem.createUniqueFakeItem();
    private final Map<String, AttributeValue> fakeItemMap1 = FakeItem.getTableSchema().itemToMap(fakeItem1, true);
    private final Map<String, AttributeValue> fakeItemMap2 = FakeItem.getTableSchema().itemToMap(fakeItem2, true);

    @Mock
    private WriteTransaction mockWriteTransaction1;
    @Mock
    private WriteTransaction mockWriteTransaction2;
    @Mock
    private MapperExtension mockMapperExtension;
    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private TransactWriteItem fakeTransactWriteItem1 = TransactWriteItem.builder()
                                                                        .put(Put.builder()
                                                                                .item(fakeItemMap1)
                                                                                .tableName(TABLE_NAME)
                                                                                .build())
                                                                        .build();

    private TransactWriteItem fakeTransactWriteItem2 = TransactWriteItem.builder()
                                                                        .put(Put.builder()
                                                                                .item(fakeItemMap2)
                                                                                .tableName(TABLE_NAME)
                                                                                .build())
                                                                        .build();

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;
    private DynamoDbTable<FakeItemWithSort> fakeItemWithSortMappedTable;

    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
    }

    @Test
    public void generateRequest_singleTransaction() {
        TransactWriteItemsEnhancedRequest transactGetItemsEnhancedRequest =
            TransactWriteItemsEnhancedRequest.builder()
                                             .writeTransactions(
                                                 WriteTransaction.create(fakeItemMappedTable, PutItem.create(fakeItem1)))
                                             .build();

        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(transactGetItemsEnhancedRequest);
        TransactWriteItemsRequest actualRequest = operation.generateRequest(mockMapperExtension);
        TransactWriteItemsRequest expectedRequest = TransactWriteItemsRequest.builder()
                                                                             .transactItems(fakeTransactWriteItem1)
                                                                             .build();

        assertThat(actualRequest, is(expectedRequest));
        verifyZeroInteractions(mockMapperExtension);
    }

    @Test
    public void generateRequest_multipleTransactions() {
        TransactWriteItemsEnhancedRequest transactGetItemsEnhancedRequest =
            TransactWriteItemsEnhancedRequest.builder()
                                             .writeTransactions(
                                                 WriteTransaction.create(fakeItemMappedTable, PutItem.create(fakeItem1)),
                                                 WriteTransaction.create(fakeItemMappedTable, PutItem.create(fakeItem2)))
                                             .build();

        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(transactGetItemsEnhancedRequest);
        TransactWriteItemsRequest actualRequest = operation.generateRequest(mockMapperExtension);
        TransactWriteItemsRequest expectedRequest =
            TransactWriteItemsRequest.builder()
                                     .transactItems(fakeTransactWriteItem1, fakeTransactWriteItem2)
                                     .build();

        assertThat(actualRequest, is(expectedRequest));
        verifyZeroInteractions(mockMapperExtension);
    }

    @Test
    public void generateRequest_noTransactions() {
        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(emptyRequest());

        TransactWriteItemsRequest actualRequest = operation.generateRequest(mockMapperExtension);

        TransactWriteItemsRequest expectedRequest = TransactWriteItemsRequest.builder().build();
        assertThat(actualRequest, is(expectedRequest));
        verifyZeroInteractions(mockMapperExtension);
    }

    @Test
    public void getServiceCall_callsServiceAndReturnsResult() {
        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(emptyRequest());
        TransactWriteItemsRequest request =
            TransactWriteItemsRequest.builder()
                                     .transactItems(singletonList(fakeTransactWriteItem1))
                                     .build();
        TransactWriteItemsResponse expectedResponse = TransactWriteItemsResponse.builder()
                                                                                .build();
        when(mockDynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class))).thenReturn(expectedResponse);

        TransactWriteItemsResponse actualResponse = operation.serviceCall(mockDynamoDbClient).apply(request);

        assertThat(actualResponse, is(sameInstance(expectedResponse)));
        verify(mockDynamoDbClient).transactWriteItems(request);
        verifyZeroInteractions(mockMapperExtension);
    }

    @Test
    public void transformResponse_doesNothing() {
        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(emptyRequest());
        TransactWriteItemsResponse response = TransactWriteItemsResponse.builder().build();

        operation.transformResponse(response, mockMapperExtension);

        verifyZeroInteractions(mockMapperExtension);
    }

    private static TransactWriteItemsEnhancedRequest emptyRequest() {
        return TransactWriteItemsEnhancedRequest.builder().writeTransactions().build();
    }
}
