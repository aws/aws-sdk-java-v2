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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemComposedClass;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;

@RunWith(MockitoJUnitRunner.class)
public class GetItemTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        OperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        OperationContext.create(TABLE_NAME, "gsi_1");

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private MapperExtension mockMapperExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(keyItem.getId())));
        GetItemRequest getItemRequest = GetItemRequest.builder().tableName(TABLE_NAME).build();
        GetItemResponse expectedResponse = GetItemResponse.builder().build();
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(expectedResponse);

        GetItemResponse response = getItemOperation.serviceCall(mockDynamoDbClient).apply(getItemRequest);

        assertThat(response, sameInstance(expectedResponse));
        verify(mockDynamoDbClient).getItem(getItemRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_withIndex_throwsIllegalArgumentException() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(keyItem.getId())));

        getItemOperation.generateRequest(FakeItem.getTableSchema(), GSI_1_CONTEXT, null);
    }

    @Test
    public void generateRequest_consistentRead() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItem<FakeItem> getItemOperation =
            GetItem.builder().key(Key.create(stringValue(keyItem.getId()))).consistentRead(true).build();

        GetItemRequest request = getItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  null);

        Map<String, AttributeValue> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        GetItemRequest expectedRequest = GetItemRequest.builder()
                                                       .tableName(TABLE_NAME)
                                                       .key(expectedKeyMap)
                                                       .consistentRead(true)
                                                       .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_partitionKeyOnly() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(keyItem.getId())));

        GetItemRequest request = getItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  null);

        Map<String, AttributeValue> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        GetItemRequest expectedRequest = GetItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(expectedKeyMap)
            .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_partitionAndSortKey() {
        FakeItemWithSort keyItem = createUniqueFakeItemWithSort();
        GetItem<FakeItemWithSort> getItemOperation = GetItem.create(Key.create(stringValue(keyItem.getId()),
                                                                       stringValue(keyItem.getSort())));

        GetItemRequest request = getItemOperation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  null);

        Map<String, AttributeValue> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        expectedKeyMap.put("sort", AttributeValue.builder().s(keyItem.getSort()).build());
        GetItemRequest expectedRequest = GetItemRequest.builder()
                                                       .tableName(TABLE_NAME)
                                                       .key(expectedKeyMap)
                                                       .build();
        assertThat(request, is(expectedRequest));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateRequest_noPartitionKey_throwsIllegalArgumentException() {
        GetItem<FakeItemComposedClass> getItemOperation = GetItem.create(Key.create(stringValue("whatever")));

        getItemOperation.generateRequest(FakeItemComposedClass.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void transformResponse_noItem() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(keyItem.getId())));
        GetItemResponse response = GetItemResponse.builder().build();

        FakeItem result = getItemOperation.transformResponse(response, FakeItem.getTableSchema(), PRIMARY_CONTEXT,
                                                             null);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void transformResponse_correctlyTransformsIntoAnItem() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(keyItem.getId())));
        Map<String, AttributeValue> responseMap = new HashMap<>();
        responseMap.put("id", AttributeValue.builder().s(keyItem.getId()).build());
        responseMap.put("subclass_attribute", AttributeValue.builder().s("test-value").build());
        GetItemResponse response = GetItemResponse.builder()
                                                  .item(responseMap)
                                                  .build();

        FakeItem result = getItemOperation.transformResponse(response, FakeItem.getTableSchema(), PRIMARY_CONTEXT,
                                                             null);

        assertThat(result.getId(), is(keyItem.getId()));
        assertThat(result.getSubclassAttribute(), is("test-value"));
    }

    @Test
    public void generateRequest_withExtension_doesNotModifyKey() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> keyMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, singletonList("id"));
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(baseFakeItem.getId())));

        GetItemRequest request = getItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  mockMapperExtension);

        assertThat(request.key(), is(keyMap));
        verify(mockMapperExtension, never()).beforeWrite(anyMap(), any(), any());
    }

    @Test
    public void transformResponse_withExtension_appliesItemModification() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeItemMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, false);
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, false);
        GetItem<FakeItem> getItemOperation = GetItem.create(Key.create(stringValue(baseFakeItem.getId())));
        GetItemResponse response = GetItemResponse.builder()
                                                  .item(baseFakeItemMap)
                                                  .build();
        when(mockMapperExtension.afterRead(anyMap(), any(), any()))
            .thenReturn(ReadModification.builder().transformedItem(fakeItemMap).build());

        FakeItem resultItem = getItemOperation.transformResponse(response, FakeItem.getTableSchema(),
                                                                 PRIMARY_CONTEXT, mockMapperExtension);

        assertThat(resultItem, is(fakeItem));
        verify(mockMapperExtension).afterRead(baseFakeItemMap, PRIMARY_CONTEXT, FakeItem.getTableMetadata());
    }
}
