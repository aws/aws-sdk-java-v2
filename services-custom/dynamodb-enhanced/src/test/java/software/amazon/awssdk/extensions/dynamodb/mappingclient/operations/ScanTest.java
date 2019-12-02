/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort.createUniqueFakeItemWithSort;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;

@RunWith(MockitoJUnitRunner.class)
public class ScanTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        OperationContext.of(TABLE_NAME, TableMetadata.primaryIndexName());
    private static final OperationContext GSI_1_CONTEXT =
        OperationContext.of(TABLE_NAME, "gsi_1");

    private final Scan<FakeItem> scanOperation = Scan.create();

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private MapperExtension mockMapperExtension;

    @Test
    public void getServiceCall_makesTheRightCallAndReturnsResponse() {
        ScanRequest scanRequest = ScanRequest.builder().build();
        ScanIterable mockScanIterable = mock(ScanIterable.class);
        when(mockDynamoDbClient.scanPaginator(any(ScanRequest.class))).thenReturn(mockScanIterable);

        ScanIterable response = scanOperation.serviceCall(mockDynamoDbClient).apply(scanRequest);

        assertThat(response, is(mockScanIterable));
        verify(mockDynamoDbClient).scanPaginator(scanRequest);
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
        Scan<FakeItemWithIndices> scanToTest = Scan.create();
        ScanRequest scanRequest = scanToTest.generateRequest(FakeItemWithIndices.getTableSchema(), GSI_1_CONTEXT, null);
        assertThat(scanRequest.indexName(), is("gsi_1"));
    }


    @Test
    public void generateRequest_limit() {
        Scan<FakeItem> operationToTest = Scan.builder().limit(10).build();
        ScanRequest request = operationToTest.generateRequest(FakeItem.getTableSchema(),
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
        Scan<FakeItem> operationToTest =
            Scan.builder().filterExpression(filterExpression).build();

        ScanRequest request = operationToTest.generateRequest(FakeItem.getTableSchema(),
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
        Scan<FakeItem> operationToTest =
            Scan.builder().filterExpression(filterExpression).build();

        ScanRequest request = operationToTest.generateRequest(FakeItem.getTableSchema(),
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
        Scan<FakeItem> operationToTest = Scan.builder().consistentRead(true).build();
        ScanRequest request = operationToTest.generateRequest(FakeItem.getTableSchema(),
                                                              PRIMARY_CONTEXT,
                                                              null);

        ScanRequest expectedRequest = ScanRequest.builder()
                                                 .tableName(TABLE_NAME)
                                                 .consistentRead(true)
                                                 .build();
        assertThat(request, is(expectedRequest));
    }

    @Test
    public void generateRequest_hashKeyOnly_exclusiveStartKey() {
        FakeItem exclusiveStartKey = createUniqueFakeItem();
        Map<String, AttributeValue> keyMap = FakeItem.getTableSchema().itemToMap(exclusiveStartKey, singletonList("id"));
        Scan<FakeItem> scanToTest = Scan.builder().exclusiveStartKey(keyMap).build();

        ScanRequest scanRequest = scanToTest.generateRequest(FakeItem.getTableSchema(),
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

        Scan<FakeItemWithSort> scanToTest = Scan.builder().exclusiveStartKey(keyMap).build();

        ScanRequest scanRequest = scanToTest.generateRequest(FakeItemWithSort.getTableSchema(),
                                                             PRIMARY_CONTEXT,
                                                             null);

        assertThat(scanRequest.exclusiveStartKey(),
                   hasEntry("id", AttributeValue.builder().s(exclusiveStartKey.getId()).build()));
        assertThat(scanRequest.exclusiveStartKey(),
                   hasEntry("sort", AttributeValue.builder().s(exclusiveStartKey.getSort()).build()));
    }

    @Test
    public void transformResults_firstPageMultipleItems_iteratesAndReturnsCorrectItems() {
        List<FakeItem> scanResultItems = generateFakeItemList();
        List<Map<String, AttributeValue>> scanResultMaps =
            scanResultItems.stream().map(ScanTest::getAttributeValueMap).collect(toList());

        ScanIterable scanIterable = generateFakeScanResults(singletonList(scanResultMaps));

        Iterable<Page<FakeItem>> scanResultPages = scanOperation.transformResponse(scanIterable,
                                                                                   FakeItem.getTableSchema(),
                                                                                   PRIMARY_CONTEXT,
                                                                                   null);
        Iterator<Page<FakeItem>> scanResultPageIterator = scanResultPages.iterator();

        assertThat(scanResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page = scanResultPageIterator.next();
        assertThat(scanResultPageIterator.hasNext(), is(false));
        assertThat(page.items(), is(scanResultItems));
    }

    @Test
    public void transformResults_firstPageMultipleItems_setsLastEvaluatedKey() {
        List<FakeItem> scanResultItems = generateFakeItemList();
        FakeItem lastEvaluatedKey = createUniqueFakeItem();
        List<Map<String, AttributeValue>> scanResultMaps =
            scanResultItems.stream().map(ScanTest::getAttributeValueMap).collect(toList());

        ScanIterable scanIterable = generateFakeScanResults(singletonList(scanResultMaps),
                                                               getAttributeValueMap(lastEvaluatedKey));

        Iterable<Page<FakeItem>> scanResultPages = scanOperation.transformResponse(scanIterable,
                                                                                   FakeItem.getTableSchema(),
                                                                                   PRIMARY_CONTEXT,
                                                                                   null);
        Iterator<Page<FakeItem>> scanResultPageIterator = scanResultPages.iterator();

        assertThat(scanResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page = scanResultPageIterator.next();
        assertThat(scanResultPageIterator.hasNext(), is(false));
        assertThat(page.items(), is(scanResultItems));
        assertThat(page.lastEvaluatedKey(), is(getAttributeValueMap(lastEvaluatedKey)));
    }

    @Test
    public void scanItem_twoPagesMultipleItems_iteratesAndReturnsCorrectItems() {
        List<FakeItem> scanResultItems1 = generateFakeItemList();
        List<FakeItem> scanResultItems2 = generateFakeItemList();

        List<Map<String, AttributeValue>> scanResultMaps1 =
            scanResultItems1.stream().map(ScanTest::getAttributeValueMap).collect(toList());
        List<Map<String, AttributeValue>> scanResultMaps2 =
            scanResultItems2.stream().map(ScanTest::getAttributeValueMap).collect(toList());

        ScanIterable scanIterable = generateFakeScanResults(asList(scanResultMaps1, scanResultMaps2));

        Iterable<Page<FakeItem>> scanResultPages = scanOperation.transformResponse(scanIterable,
                                                                                   FakeItem.getTableSchema(),
                                                                                   PRIMARY_CONTEXT,
                                                                                   null);
        Iterator<Page<FakeItem>> scanResultPageIterator = scanResultPages.iterator();

        assertThat(scanResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page1 = scanResultPageIterator.next();
        assertThat(scanResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page2 = scanResultPageIterator.next();
        assertThat(scanResultPageIterator.hasNext(), is(false));
        assertThat(page1.items(), is(scanResultItems1));
        assertThat(page2.items(), is(scanResultItems2));
    }

    @Test
    public void scanItem_withExtension_correctlyTransformsItems() {
        List<FakeItem> scanResultItems1 = generateFakeItemList();
        List<FakeItem> scanResultItems2 = generateFakeItemList();
        List<FakeItem> modifiedResultItems1 = generateFakeItemList();
        List<FakeItem> modifiedResultItems2 = generateFakeItemList();

        List<Map<String, AttributeValue>> scanResultMaps1 =
            scanResultItems1.stream().map(ScanTest::getAttributeValueMap).collect(toList());
        List<Map<String, AttributeValue>> scanResultMaps2 =
            scanResultItems2.stream().map(ScanTest::getAttributeValueMap).collect(toList());

        ReadModification[] readModifications =
            Stream.concat(modifiedResultItems1.stream(), modifiedResultItems2.stream())
                  .map(ScanTest::getAttributeValueMap)
                  .map(attributeMap -> ReadModification.builder().transformedItem(attributeMap).build())
                  .collect(Collectors.toList())
                  .toArray(new ReadModification[]{});
        when(mockMapperExtension.afterRead(anyMap(), any(), any()))
            .thenReturn(readModifications[0], Arrays.copyOfRange(readModifications, 1, readModifications.length));

        ScanIterable scanIterable = generateFakeScanResults(asList(scanResultMaps1, scanResultMaps2));

        Iterable<Page<FakeItem>> scanResultPages = scanOperation.transformResponse(scanIterable,
                                                                                   FakeItem.getTableSchema(),
                                                                                   PRIMARY_CONTEXT,
                                                                                   mockMapperExtension);
        Iterator<Page<FakeItem>> scanResultPageIterator = scanResultPages.iterator();

        assertThat(scanResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page1 = scanResultPageIterator.next();
        assertThat(scanResultPageIterator.hasNext(), is(true));
        Page<FakeItem> page2 = scanResultPageIterator.next();
        assertThat(scanResultPageIterator.hasNext(), is(false));
        assertThat(page1.items(), is(modifiedResultItems1));
        assertThat(page2.items(), is(modifiedResultItems2));

        InOrder inOrder = Mockito.inOrder(mockMapperExtension);
        Stream.concat(scanResultMaps1.stream(), scanResultMaps2.stream())
              .forEach(attributeMap ->
                    inOrder.verify(mockMapperExtension).afterRead(attributeMap,
                                                                  PRIMARY_CONTEXT,
                                                                  FakeItem.getTableMetadata()));
    }

    private static ScanIterable generateFakeScanResults(List<List<Map<String, AttributeValue>>> scanItemMapsPages) {
        List<ScanResponse> scanResponses =
            scanItemMapsPages.stream().map(page -> ScanResponse.builder().items(page).build()).collect(toList());

        ScanIterable mockScanIterable = mock(ScanIterable.class);
        when(mockScanIterable.iterator()).thenReturn(scanResponses.iterator());
        return mockScanIterable;
    }

    private static ScanIterable generateFakeScanResults(List<List<Map<String, AttributeValue>>> scanItemMapsPages,
                                                          Map<String, AttributeValue> lastEvaluatedKey) {
        List<ScanResponse> scanResponses =
            scanItemMapsPages.stream()
                              .map(page -> ScanResponse.builder()
                                                        .items(page)
                                                        .lastEvaluatedKey(lastEvaluatedKey)
                                                        .build())
                              .collect(toList());

        ScanIterable mockScanIterable = mock(ScanIterable.class);
        when(mockScanIterable.iterator()).thenReturn(scanResponses.iterator());
        return mockScanIterable;
    }

    private static List<FakeItem> generateFakeItemList() {
        return IntStream.range(0, 3).mapToObj(ignored -> FakeItem.createUniqueFakeItem()).collect(toList());
    }

    private static Map<String, AttributeValue> getAttributeValueMap(FakeItem fakeItem) {
        return singletonMap("id", AttributeValue.builder().s(fakeItem.getId()).build());
    }
}