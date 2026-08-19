/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper.FailedBatch;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.BatchWriteRetryStrategy;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

public class BatchWriteRetryStrategyTest {

    private static final int MAX_RETRY = 10;
    private static final String TABLE_NAME = "tableName";
    private static final String HASH_ATTR = "hash";

    private static Map<String, List<WriteRequest>> unprocessedItems;

    private DynamoDbClient ddbMock;
    private DynamoDBMapper mapper;

    static {
        WriteRequest writeReq = WriteRequest.builder()
                .putRequest(PutRequest.builder()
                        .item(Collections.singletonMap(
                                HASH_ATTR,
                                AttributeValue.builder().s("foo").build()))
                        .build())
                .build();

        unprocessedItems = Collections.singletonMap(TABLE_NAME,
                Arrays.asList(writeReq));
    }

    @Before
    public void setup() {
        ddbMock = mock(DynamoDbClient.class);
        mapper = new DynamoDBMapper(
                ddbMock,
                getConfigWithCustomBatchWriteRetryStrategy(
                        new BatchWriteRetryStrategyWithNoDelay(MAX_RETRY)));
    }

    @Test
    public void testBatchWriteItemCallSuccess_NoRetry() {

        stubBatchWriteItemSuccess();

        List<FailedBatch> failedBatches = mapper.batchSave(new Item("foo"));

        // BatchWriteItem is expected to be called only once
        verify(ddbMock, times(1)).batchWriteItem(isA(BatchWriteItemRequest.class));

        assertEquals(0, failedBatches.size());
    }

    @Test
    public void testUnprocessedItemReturned_BatchWriteItemCallNotExceedMaxRetry() {

        stubBatchWriteItemReturnUnprocessedItems();

        List<FailedBatch> failedBatches = mapper.batchSave(new Item("foo"));

        // BatchWriteItem is expected to be called exactly (MAX_RETRY + 1) times
        verify(ddbMock, times(MAX_RETRY + 1)).batchWriteItem(isA(BatchWriteItemRequest.class));

        assertEquals(1, failedBatches.size());
        FailedBatch failedBatch = failedBatches.get(0);

        assertEquals(
                "Failed batch should contain the same UnprocessedItems returned in the BatchWriteItem response.",
                unprocessedItems,
                failedBatch.getUnprocessedItems());
        assertNull(
                "No exception should be set if the batch failed after max retry",
                failedBatch.getException());
    }

    @Test
    public void testExceptionThrown_NoRetry() {

        RuntimeException exception = new RuntimeException("BOOM");
        stubBatchWriteItemThrowException(exception);

        // put a random item
        Item item = new Item(UUID.randomUUID().toString());
        List<FailedBatch> failedBatches = mapper.batchSave(item);

        verify(ddbMock, times(1)).batchWriteItem(isA(BatchWriteItemRequest.class));

        assertEquals(1, failedBatches.size());
        FailedBatch failedBatch = failedBatches.get(0);

        assertEquals(
                "Failed batch should contain all the input items for batchWrite",
                Collections.singletonMap(TABLE_NAME, Arrays.asList(item.toPutSaveRequest())),
                failedBatch.getUnprocessedItems());
        assertSame(
                "The exception should be the same as one thrown by BatchWriteItem",
                exception,
                failedBatch.getException());
    }

    private void stubBatchWriteItemSuccess() {
        when(ddbMock.batchWriteItem(isA(BatchWriteItemRequest.class)))
                .thenReturn(BatchWriteItemResponse.builder()
                        .unprocessedItems(Collections.<String, List<WriteRequest>>emptyMap())
                        .build());
    }

    private void stubBatchWriteItemReturnUnprocessedItems() {
        when(ddbMock.batchWriteItem(isA(BatchWriteItemRequest.class)))
                .thenReturn(BatchWriteItemResponse.builder()
                        .unprocessedItems(unprocessedItems)
                        .build());
    }

    private void stubBatchWriteItemThrowException(RuntimeException e) {
        when(ddbMock.batchWriteItem(isA(BatchWriteItemRequest.class)))
                .thenThrow(e);
    }

    private DynamoDBMapperConfig getConfigWithCustomBatchWriteRetryStrategy(
            BatchWriteRetryStrategy batchWriteRetryStrategy) {
        return new DynamoDBMapperConfig.Builder()
                .withBatchWriteRetryStrategy(batchWriteRetryStrategy)
                .build();
    }

    private static class BatchWriteRetryStrategyWithNoDelay implements
            BatchWriteRetryStrategy {

        private final int maxRetry;

        public BatchWriteRetryStrategyWithNoDelay(int maxRety) {
            this.maxRetry = maxRety;
        }

        @Override
        public int getMaxRetryOnUnprocessedItems(
                Map<String, List<WriteRequest>> batchWriteItemInput) {
            return maxRetry;
        }

        @Override
        public long getDelayBeforeRetryUnprocessedItems(
                Map<String, List<WriteRequest>> unprocessedItems,
                int retriesAttempted) {
            return 0;
        }

    }

    @DynamoDBTable(tableName = TABLE_NAME)
    public static class Item {

        private String hash;

        public Item(String hash) {
            this.hash = hash;
        }

        @DynamoDBHashKey
        @DynamoDBAttribute(attributeName = HASH_ATTR)
        public String getHash() {
            return hash;
        }
        public void setHash(String hash) {
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
