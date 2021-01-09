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
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ScanPublisher;

@RunWith(MockitoJUnitRunner.class)
public class ScanOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    private final ScanOperation<FakeItem> scanOperation = ScanOperation.create(ScanEnhancedRequest.builder().build());

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        ScanRequest scanRequest = ScanRequest.builder().build();
        ScanIterable mockScanIterable = mock(ScanIterable.class);
        when(mockDynamoDbClient.scanPaginator(any(ScanRequest.class))).thenReturn(mockScanIterable);

        SdkIterable<ScanResponse> response = scanOperation.serviceCall(mockDynamoDbClient).apply(scanRequest);

        assertThat(response, is(mockScanIterable));
        verify(mockDynamoDbClient).scanPaginator(scanRequest);
    }

    @Test
    public void getAsyncServiceCall_makesTheRightCallAndReturnsResponse() {
        ScanRequest scanRequest = ScanRequest.builder().build();
        ScanPublisher mockScanPublisher = mock(ScanPublisher.class);
        when(mockDynamoDbAsyncClient.scanPaginator(any(ScanRequest.class))).thenReturn(mockScanPublisher);

        SdkPublisher<ScanResponse> response = scanOperation.asyncServiceCall(mockDynamoDbAsyncClient)
                                                           .apply(scanRequest);

        assertThat(response, is(mockScanPublisher));
        verify(mockDynamoDbAsyncClient).scanPaginator(scanRequest);
    }

    @Test
    public void generateRequest_defaultScan() {
        ScanRequest request = scanOperation.generateRequest(FakeItem.getTableSchema(),
                                                            PRIMARY_CONTEXT,
                                                            null);

        ScanRequest expectedRequest = ScanRequest.builder()
            .tableName(TABLE_NAME)
            .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_knowsHowToUseAnIndex() {
        ScanOperation<FakeItemWithIndices> operation = ScanOperation.create(ScanEnhancedRequest.builder().build());
        ScanRequest scanRequest = operation.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);
        assertThat(scanRequest.indexName(), is("gsi_1"));
    }


    @Test
    public void generateRequest_limit() {
        ScanOperation<FakeItem> operation = ScanOperation.create(ScanEnhancedRequest.builder().limit(10).build());
        ScanRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                              PRIMARY_CONTEXT,
                                                              null);

        ScanRequest expectedRequest = ScanRequest.builder()
            .tableName(TABLE_NAME)
            .limit(10)
            .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_filterCondition_expressionAndValues() {
        Map<String, AttributeValue> expressionValues = singletonMap(":test-key", stringValue("test-value"));
        Expression filterExpression =
            Expression.builder().expression("test-expression").expressionValues(expressionValues).build();
        ScanOperation<FakeItem> operation =
            ScanOperation.create(ScanEnhancedRequest.builder().filterExpression(filterExpression).build());

        ScanRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                              PRIMARY_CONTEXT,
                                                              null);
        ScanRequest expectedRequest = ScanRequest.builder()
                                                 .tableName(TABLE_NAME)
                                                 .filterExpression("test-expression")
                                                 .expressionAttributeValues(expressionValues)
                                                 .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_filterCondition_expressionOnly() {
        Expression filterExpression = Expression.builder().expression("test-expression").build();
        ScanOperation<FakeItem> operation =
            ScanOperation.create(ScanEnhancedRequest.builder().filterExpression(filterExpression).build());

        ScanRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                              PRIMARY_CONTEXT,
                                                              null);
        ScanRequest expectedRequest = ScanRequest.builder()
                                                 .tableName(TABLE_NAME)
                                                 .filterExpression("test-expression")
                                                 .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_consistentRead() {
        ScanOperation<FakeItem> operation = ScanOperation.create(ScanEnhancedRequest.builder().consistentRead(true).build());
        ScanRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                                                              PRIMARY_CONTEXT,
                                                              null);

        ScanRequest expectedRequest = ScanRequest.builder()
                                                 .tableName(TABLE_NAME)
                                                 .consistentRead(true)
                                                 .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_projectionExpression() {
        ScanOperation<FakeItem> operation = ScanOperation.create(
                ScanEnhancedRequest.builder()
                        .attributesToProject("id")
                        .addAttributeToProject("version")
                        .build()
        );
        ScanRequest request = operation.generateRequest(FakeItem.getTableSchema(),
                PRIMARY_CONTEXT,
                null);

        Map<String, String> expectedExpressionAttributeNames = new HashMap<>();
        expectedExpressionAttributeNames.put("#AMZN_MAPPED_id", "id");
        expectedExpressionAttributeNames.put("#AMZN_MAPPED_version", "version");

        ScanRequest expectedRequest = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .projectionExpression("#AMZN_MAPPED_id,#AMZN_MAPPED_version")
                .expressionAttributeNames(expectedExpressionAttributeNames)
                .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_hashKeyOnly_exclusiveStartKey() {
        FakeItem exclusiveStartKey = createUniqueFakeItem();
        Map<String, AttributeValue> keyMap = FakeItem.getTableSchema().itemToMap(exclusiveStartKey, singletonList("id"));
        ScanOperation<FakeItem> operation =
            ScanOperation.create(ScanEnhancedRequest.builder().exclusiveStartKey(keyMap).build());

        ScanRequest scanRequest = operation.generateRequest(FakeItem.getTableSchema(),
                                                             PRIMARY_CONTEXT,
                                                             null);

        assertThat(scanRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
    }

    @Test
    public void generateRequest_hashAndRangeKey_exclusiveStartKey() {
        FakeItemWithSort exclusiveStartKey = createUniqueFakeItemWithSort();
        Map<String, AttributeValue> keyMap =
            FakeItemWithSort.getTableSchema().itemToMap(exclusiveStartKey,
                                                        FakeItemWithSort.getTableMetadata().primaryKeys());

        ScanOperation<FakeItemWithSort> operation =
            ScanOperation.create(ScanEnhancedRequest.builder().exclusiveStartKey(keyMap).build());

        ScanRequest scanRequest = operation.generateRequest(FakeItemWithSort.getTableSchema(),
                                                             PRIMARY_CONTEXT,
                                                             null);

        assertThat(scanRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
        assertThat(scanRequest.exclusiveStartKey(),
                   hasEntry("sort", AttributeValue.builder().s(exclusiveStartKey.getSort()).build()));
    }

    @Test
    public void transformResults_multipleItems_returnsCorrectItems() {
        List<FakeItem> scanResultItems = generateFakeItemList();
        List<Map<String, AttributeValue>> scanResultMaps =
            scanResultItems.stream().map(ScanOperationTest::getAttributeValueMap).collect(toList());

        ScanResponse scanResponse = generateFakeScanResults(scanResultMaps);

        Page<FakeItem> scanResultPage = scanOperation.transformResponse(scanResponse,
                                                                        FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);
        assertThat(scanResultPage.items(), is(scanResultItems));
    }

    @Test
    public void transformResults_multipleItems_setsLastEvaluatedKey() {
        List<FakeItem> scanResultItems = generateFakeItemList();
        FakeItem lastEvaluatedKey = createUniqueFakeItem();
        List<Map<String, AttributeValue>> scanResultMaps =
            scanResultItems.stream().map(ScanOperationTest::getAttributeValueMap).collect(toList());

        ScanResponse scanResponse = generateFakeScanResults(scanResultMaps, getAttributeValueMap(lastEvaluatedKey));

        Page<FakeItem> scanResultPage = scanOperation.transformResponse(scanResponse,
                                                                        FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        null);

        assertThat(scanResultPage.lastEvaluatedKey(), is(getAttributeValueMap(lastEvaluatedKey)));
    }

    @Test
    public void scanItem_withExtension_correctlyTransformsItems() {
        List<FakeItem> scanResultItems = generateFakeItemList();
        List<FakeItem> modifiedResultItems = generateFakeItemList();

        List<Map<String, AttributeValue>> scanResultMaps =
            scanResultItems.stream().map(ScanOperationTest::getAttributeValueMap).collect(toList());

        ReadModification[] readModifications =
            modifiedResultItems.stream()
                  .map(ScanOperationTest::getAttributeValueMap)
                  .map(attributeMap -> ReadModification.builder().transformedItem(attributeMap).build())
                  .collect(Collectors.toList())
                  .toArray(new ReadModification[]{});
        when(mockDynamoDbEnhancedClientExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(readModifications[0], Arrays.copyOfRange(readModifications, 1, readModifications.length));

        ScanResponse scanResponse = generateFakeScanResults(scanResultMaps);

        Page<FakeItem> scanResultPage = scanOperation.transformResponse(scanResponse,
                                                                        FakeItem.getTableSchema(),
                                                                        PRIMARY_CONTEXT,
                                                                        mockDynamoDbEnhancedClientExtension);

        assertThat(scanResultPage.items(), is(modifiedResultItems));

        InOrder inOrder = Mockito.inOrder(mockDynamoDbEnhancedClientExtension);
        scanResultMaps.forEach(
            attributeMap -> inOrder.verify(mockDynamoDbEnhancedClientExtension).afterRead(
                DefaultDynamoDbExtensionContext.builder()
                                               .tableMetadata(FakeItem.getTableMetadata())
                                               .operationContext(PRIMARY_CONTEXT)
                                               .items(attributeMap).build()));
    }

    private static ScanResponse generateFakeScanResults(List<Map<String, AttributeValue>> scanItemMapsPage) {
        return ScanResponse.builder().items(scanItemMapsPage).build();
    }

    private static ScanResponse generateFakeScanResults(List<Map<String, AttributeValue>> scanItemMapsPage,
                                                        Map<String, AttributeValue> lastEvaluatedKey) {
        return ScanResponse.builder()
                           .items(scanItemMapsPage)
                           .lastEvaluatedKey(lastEvaluatedKey)
                           .build();
    }

    private static List<FakeItem> generateFakeItemList() {
        return IntStream.range(0, 3).mapToObj(ignored -> FakeItem.createUniqueFakeItem()).collect(toList());
    }

    private static Map<String, AttributeValue> getAttributeValueMap(FakeItem fakeItem) {
        return singletonMap("id", AttributeValue.builder().s(fakeItem.getId()).build());
    }
}
