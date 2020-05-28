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

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemComposedClass;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@RunWith(MockitoJUnitRunner.class)
public class GetItemOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());
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
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());

        getItemOperation.generateRequest(FakeItem.getTableSchema(), GSI_1_CONTEXT, null);
    }

    @Test
    public void generateRequest_consistentRead() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder()
                                                          .key(k -> k.partitionValue(keyItem.getId()))
                                                          .consistentRead(true).build());

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
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());

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
        GetItemOperation<FakeItemWithSort> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder()
                                                          .key(k -> k.partitionValue(keyItem.getId())
                                                                     .sortValue(keyItem.getSort()))
                                                          .build());

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
        GetItemOperation<FakeItemComposedClass> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue("whatever")).build());

        getItemOperation.generateRequest(FakeItemComposedClass.getTableSchema(), PRIMARY_CONTEXT, null);
    }

    @Test
    public void transformResponse_noItem() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());
        GetItemResponse response = GetItemResponse.builder().build();

        FakeItem result = getItemOperation.transformResponse(response, FakeItem.getTableSchema(), PRIMARY_CONTEXT,
                                                             null);

        assertThat(result, is(nullValue()));
    }

    @Test
    public void transformResponse_correctlyTransformsIntoAnItem() {
        FakeItem keyItem = createUniqueFakeItem();
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(keyItem.getId())).build());
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
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(baseFakeItem.getId())).build());

        GetItemRequest request = getItemOperation.generateRequest(FakeItem.getTableSchema(),
                                                                  PRIMARY_CONTEXT,
                                                                  mockDynamoDbEnhancedClientExtension);

        assertThat(request.key(), is(keyMap));
        verify(mockDynamoDbEnhancedClientExtension, never()).beforeWrite(any(DynamoDbExtensionContext.BeforeWrite.class));
    }

    @Test
    public void transformResponse_withExtension_appliesItemModification() {
        FakeItem baseFakeItem = createUniqueFakeItem();
        FakeItem fakeItem = createUniqueFakeItem();
        Map<String, AttributeValue> baseFakeItemMap = FakeItem.getTableSchema().itemToMap(baseFakeItem, false);
        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, false);
        GetItemOperation<FakeItem> getItemOperation =
            GetItemOperation.create(GetItemEnhancedRequest.builder().key(k -> k.partitionValue(baseFakeItem.getId())).build());
        GetItemResponse response = GetItemResponse.builder()
                                                  .item(baseFakeItemMap)
                                                  .build();
        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(ReadModification.builder().transformedItem(fakeItemMap).build());

        FakeItem resultItem = getItemOperation.transformResponse(response, FakeItem.getTableSchema(),
                                                                 PRIMARY_CONTEXT, mockDynamoDbEnhancedClientExtension);

        assertThat(resultItem, is(fakeItem));
        verify(mockDynamoDbEnhancedClientExtension).afterRead(DefaultDynamoDbExtensionContext.builder()
                                                              .tableMetadata(FakeItem.getTableMetadata())
                                                              .operationContext(PRIMARY_CONTEXT)
                                                              .items(baseFakeItemMap).build());
    }
}
