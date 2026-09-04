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

package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.BatchLoadRetryStrategy;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper.BatchGetItemException;

public class BatchLoadRetryStrategyTest {

    private static final String TABLE_NAME = "tableName";
    private static final String TABLE_NAME2 = "tableName2";
    private static final String TABLE_NAME3 = "tableName3";
    private static final String HASH_ATTR = "hash";

    private static List<Object> itemsToGet;

    private DynamoDbClient ddbMock;
    private DynamoDBMapper mapper;

    static {

        itemsToGet = new ArrayList<Object>();
        itemsToGet.add(new Item3("Bruce Wayne"));
        itemsToGet.add(new Item2("Is"));
        itemsToGet.add(new Item("Batman"));
    }

    @Before
    public void setup() {
        ddbMock = mock(DynamoDbClient.class);
    }

    @Test
    public void testBatchReadCallFailure_NoRetry() {
        when(ddbMock.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(buildGetItemResultWithUnprocessedKeys(1));
        mapper = new DynamoDBMapper(ddbMock, getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.NoRetryBatchLoadRetryStrategy()));

        assertBatchLoadFails();
        verify(ddbMock, times(1)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    public void testBatchReadCallFailure_Retry() {
        when(ddbMock.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(buildGetItemResultWithUnprocessedKeys(1));
        mapper = new DynamoDBMapper(ddbMock, getConfigWithCustomBatchLoadRetryStrategy(new BatchLoadRetryStrategyWithNoDelay(3)));

        assertBatchLoadFails();
        verify(ddbMock, times(4)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    public void testBatchReadCallSuccess_Retry() {
        when(ddbMock.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(buildDefaultGetItemResult()
                        .toBuilder()
                        .unprocessedKeys(new HashMap<String, KeysAndAttributes>(1))
                        .build());
        mapper = new DynamoDBMapper(ddbMock, getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy()));

        mapper.batchLoad(itemsToGet);
        verify(ddbMock, times(1)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    public void testBatchReadCallFailure_Retry_RetryOnCompleteFailure() {
        when(ddbMock.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(buildGetItemResultWithUnprocessedKeys(3));
        mapper = new DynamoDBMapper(ddbMock, getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy()));

        assertBatchLoadFails();
        verify(ddbMock, times(6)).batchGetItem(any(BatchGetItemRequest.class));
    }

    @Test
    public void testBatchReadCallFailure_NoRetry_RetryOnCompleteFailure() {
        when(ddbMock.batchGetItem(any(BatchGetItemRequest.class)))
                .thenReturn(buildGetItemResultWithUnprocessedKeys(3));
        mapper = new DynamoDBMapper(ddbMock, getConfigWithCustomBatchLoadRetryStrategy(new DynamoDBMapperConfig.NoRetryBatchLoadRetryStrategy()));

        assertBatchLoadFails();
        verify(ddbMock, times(1)).batchGetItem(any(BatchGetItemRequest.class));
    }

    private void assertBatchLoadFails() {
        try {
            mapper.batchLoad(itemsToGet);
            fail("Expected BatchGetItemException");
        } catch (BatchGetItemException expected) {
            // expected
        }
    }

    @Test
    public void testNoDelayOnPartialFailure_DefaultRetry() {
        BatchLoadRetryStrategy defaultRetryStrategy = new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy();
        BatchGetItemRequest itemRequest = BatchGetItemRequest.builder()
                .requestItems(buildUnprocessedKeysMap(3))
                .build();
        BatchGetItemResponse itemResult = BatchGetItemResponse.builder()
                .unprocessedKeys(buildUnprocessedKeysMap(2))
                .build();
        BatchLoadContext context = new BatchLoadContext(itemRequest);
        context.setBatchGetItemResult(itemResult);
        context.setRetriesAttempted(2);
        assertEquals(0, defaultRetryStrategy.getDelayBeforeNextRetry(context));
    }

    @Test
    public void testDelayOnPartialFailure_DefaultRetry() {
        BatchLoadRetryStrategy defaultRetryStrategy = new DynamoDBMapperConfig.DefaultBatchLoadRetryStrategy();
        BatchGetItemRequest itemRequest = BatchGetItemRequest.builder()
                .requestItems(buildUnprocessedKeysMap(3))
                .build();
        BatchGetItemResponse itemResult = BatchGetItemResponse.builder()
                .unprocessedKeys(buildUnprocessedKeysMap(3))
                .build();
        BatchLoadContext context = new BatchLoadContext(itemRequest);
        context.setBatchGetItemResult(itemResult);
        context.setRetriesAttempted(2);
        assertTrue(defaultRetryStrategy.getDelayBeforeNextRetry(context) > 0);
    }

    @Test
    public void testBatchLoad_splitsAtHundredKeyBoundary() {
        when(ddbMock.batchGetItem(any(BatchGetItemRequest.class))).thenReturn(buildDefaultGetItemResult());
        mapper = new DynamoDBMapper(ddbMock);

        List<Item> manyItems = new ArrayList<Item>();
        for (int i = 0; i < 101; i++) {
            manyItems.add(new Item("hash" + i));
        }

        mapper.batchLoad(manyItems);

        ArgumentCaptor<BatchGetItemRequest> captor = ArgumentCaptor.forClass(BatchGetItemRequest.class);
        verify(ddbMock, times(2)).batchGetItem(captor.capture());

        List<BatchGetItemRequest> requests = captor.getAllValues();
        KeysAndAttributes firstChunk = requests.get(0).requestItems().get(TABLE_NAME);
        KeysAndAttributes secondChunk = requests.get(1).requestItems().get(TABLE_NAME);

        assertEquals("first chunk fills the 100-key boundary", 100, firstChunk.keys().size());
        assertEquals("second chunk carries the remaining key", 1, secondChunk.keys().size());
    }

    private DynamoDBMapperConfig getConfigWithCustomBatchLoadRetryStrategy(final BatchLoadRetryStrategy batchReadRetryStrategy) {
        return new DynamoDBMapperConfig.Builder().withBatchLoadRetryStrategy(batchReadRetryStrategy).build();
    }

    private Map<String, KeysAndAttributes> buildUnprocessedKeysMap(final int size) {
        final Map<String, KeysAndAttributes> unproccessedKeys = new HashMap<String, KeysAndAttributes>(size);
        for (int i = 0; i < size; i++) {
            unproccessedKeys.put("test" + i, KeysAndAttributes.builder().build());
        }

        return unproccessedKeys;
    }

    private BatchGetItemResponse buildDefaultGetItemResult() {

        final Map<String, List<Map<String, AttributeValue>>> map = new HashMap<String, List<Map<String, AttributeValue>>>();
        return BatchGetItemResponse.builder().responses(map).build();

    }

    private BatchGetItemResponse buildGetItemResultWithUnprocessedKeys(final int size) {
        return buildDefaultGetItemResult()
                .toBuilder()
                .unprocessedKeys(buildUnprocessedKeysMap(size))
                .build();
    }

    static class BatchLoadRetryStrategyWithNoDelay implements BatchLoadRetryStrategy {

        private final int maxRetry;

        /**
         * @param maxRetry
         */
        public BatchLoadRetryStrategyWithNoDelay(final int maxRetry) {
            this.maxRetry = maxRetry;
        }

        /* (non-Javadoc)
         * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.BatchLoadRetryStrategy#getMaxRetryOnUnprocessedKeys(java.util.Map, java.util.Map)
         */
        @Override
        public boolean shouldRetry(final BatchLoadContext batchLoadContext) {
            return batchLoadContext.getRetriesAttempted() < maxRetry;
        }

        /* (non-Javadoc)
         * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.BatchLoadRetryStrategy#getDelayBeforeNextRetry(java.util.Map, int)
         */
        @Override
        public long getDelayBeforeNextRetry(final BatchLoadContext batchLoadContext) {
            return 0;
        }



    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public static class Item {

        private String hash;

        public Item(final String hash) {
            this.hash = hash;
        }

        @DynamoDBAttribute(attributeName = HASH_ATTR)
        @DynamoDBHashKey
        public String getHash() {
            return hash;
        }

        public void setHash(final String hash) {
            this.hash = hash;
        }

        public WriteRequest toPutSaveRequest() {
            return WriteRequest.builder()
                    .putRequest(PutRequest.builder()
                            .item(Collections.singletonMap(HASH_ATTR, AttributeValue.builder().s(hash).build()))
                            .build())
                    .build();
        }
    }

    @DynamoDBTable(tableName = TABLE_NAME2)
    public static class Item2 {

        private String hash;

        public Item2(final String hash) {
            this.hash = hash;
        }

        @DynamoDBAttribute(attributeName = HASH_ATTR)
        @DynamoDBHashKey
        public String getHash() {
            return hash;
        }

        public void setHash(final String hash) {
            this.hash = hash;
        }

        public WriteRequest toPutSaveRequest() {
            return WriteRequest.builder()
                    .putRequest(PutRequest.builder()
                            .item(Collections.singletonMap(HASH_ATTR, AttributeValue.builder().s(hash).build()))
                            .build())
                    .build();
        }
    }

    @DynamoDBTable(tableName = TABLE_NAME3)
    public static class Item3 {

        private String hash;

        public Item3(final String hash) {
            this.hash = hash;
        }

        @DynamoDBAttribute(attributeName = HASH_ATTR)
        @DynamoDBHashKey
        public String getHash() {
            return hash;
        }

        public void setHash(final String hash) {
            this.hash = hash;
        }

        public WriteRequest toPutSaveRequest() {
            return WriteRequest.builder()
                    .putRequest(PutRequest.builder()
                            .item(Collections.singletonMap(HASH_ATTR, AttributeValue.builder().s(hash).build()))
                            .build())
                    .build();
        }
    }
}
