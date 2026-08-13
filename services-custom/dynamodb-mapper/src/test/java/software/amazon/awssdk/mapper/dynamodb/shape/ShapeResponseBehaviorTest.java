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
package software.amazon.awssdk.mapper.dynamodb.shape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBQueryExpression;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBScanExpression;
import software.amazon.awssdk.mapper.dynamodb.QueryResultPage;
import software.amazon.awssdk.mapper.dynamodb.ScanResultPage;
import software.amazon.awssdk.mapper.dynamodb.shape.ShapeItems.StringItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 * Mock behavioral tests for the mapper's response handling control flow. Where ShapeRequestTest pins the marshalled request
 * and ShapeResponseTest pins attribute map to POJO unmarshalling
 */
public class ShapeResponseBehaviorTest {

    private static final String HASH_KEY = "1234";

    private DynamoDbClient ddb;
    private DynamoDBMapper mapper;

    @Before
    public void setup() {
        ddb = mock(DynamoDbClient.class);
        mapper = new DynamoDBMapper(ddb);
    }

    @Test
    public void load_returnsNull_whenItemEmpty() {
        when(ddb.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
        assertNull(mapper.load(key()));
    }

    @Test
    public void save_reissuesAsPutItem_whenUpdateReturnsNoAttributes() {
        when(ddb.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());
        when(ddb.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        mapper.save(key());

        ArgumentCaptor<PutItemRequest> putCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(ddb).putItem(putCaptor.capture());
        assertEquals(HASH_KEY, putCaptor.getValue().item().get("id").s());
    }

    @Test
    public void count_sumsAcrossPages_andStopsOnEmptyLastKey() {
        Map<String, AttributeValue> lastKey = Collections.singletonMap("id", AttributeValue.builder().s(HASH_KEY).build());
        when(ddb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().count(3).lastEvaluatedKey(lastKey).build())
                .thenReturn(ScanResponse.builder().count(2).build());

        assertEquals(5, mapper.count(StringItem.class, new DynamoDBScanExpression()));
        verify(ddb, times(2)).scan(any(ScanRequest.class));

        ArgumentCaptor<ScanRequest> scanCaptor = ArgumentCaptor.forClass(ScanRequest.class);
        verify(ddb, times(2)).scan(scanCaptor.capture());
        assertEquals(lastKey, scanCaptor.getAllValues().get(1).exclusiveStartKey());
    }

    @Test
    public void query_lazyPagination_fetchesNextPageUsingLastKey() {
        Map<String, AttributeValue> lastKey = Collections.singletonMap("id", AttributeValue.builder().s(HASH_KEY).build());
        when(ddb.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(item("a")).lastEvaluatedKey(lastKey).build())
                .thenReturn(QueryResponse.builder().items(item("b")).build());

        Iterator<StringItem> it = mapper.query(StringItem.class,
                new DynamoDBQueryExpression<StringItem>().withHashKeyValues(key())).iterator();

        int seen = 0;
        while (it.hasNext()) {
            it.next();
            seen++;
        }
        assertEquals(2, seen);
        verify(ddb, times(2)).query(any(QueryRequest.class));

        ArgumentCaptor<QueryRequest> queryCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(ddb, times(2)).query(queryCaptor.capture());
        assertEquals(lastKey, queryCaptor.getAllValues().get(1).exclusiveStartKey());
    }

    @Test
    public void queryPage_lastEvaluatedKeyIsNull_whenServiceReturnsEmptyKey() {
        when(ddb.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(item("a")).build());

        QueryResultPage<StringItem> page = mapper.queryPage(StringItem.class,
                new DynamoDBQueryExpression<StringItem>().withHashKeyValues(key()));

        assertNull(page.getLastEvaluatedKey());
    }

    @Test
    public void scanPage_lastEvaluatedKeyIsNull_whenServiceReturnsEmptyKey() {
        when(ddb.scan(any(ScanRequest.class)))
                .thenReturn(ScanResponse.builder().items(item("a")).build());

        ScanResultPage<StringItem> page = mapper.scanPage(StringItem.class, new DynamoDBScanExpression());

        assertNull(page.getLastEvaluatedKey());
    }

    private static StringItem key() {
        StringItem item = new StringItem();
        item.setId(HASH_KEY);
        return item;
    }

    private static Map<String, AttributeValue> item(String id) {
        Map<String, AttributeValue> m = new HashMap<>();
        m.put("id", AttributeValue.builder().s(id).build());
        return m;
    }
}
