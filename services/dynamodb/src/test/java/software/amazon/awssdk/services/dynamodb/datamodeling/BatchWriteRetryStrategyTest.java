/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import junit.framework.Assert;
import org.easymock.IExpectationSetters;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.BatchWriteRetryStrategy;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper.FailedBatch;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import static org.easymock.EasyMock.*;

public class BatchWriteRetryStrategyTest {

    private static final int MAX_RETRY = 10;
    private static final String TABLE_NAME = "tableName";
    private static final String HASH_ATTR = "hash";

    private static Map<String, List<WriteRequest>> unprocessedItems;

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

    private DynamoDBClient ddbMock;
    private DynamoDbMapper mapper;

    @Before
    public void setup() {
        ddbMock = createMock(DynamoDBClient.class);
        mapper = new DynamoDbMapper(
                ddbMock,
                getConfigWithCustomBatchWriteRetryStrategy(
                        new BatchWriteRetryStrategyWithNoDelay(MAX_RETRY)));
    }

    @Test
    public void testBatchWriteItemCallSuccess_NoRetry() {

        // BatchWriteItem is expected to be called only once
        expectBatchWriteItemSuccess().once();

        replay(ddbMock);
        List<FailedBatch> failedBatches = mapper.batchSave(new Item("foo"));
        verify(ddbMock);

        Assert.assertEquals(0, failedBatches.size());
    }

    @Test
    public void testUnprocessedItemReturned_BatchWriteItemCallNotExceedMaxRetry() {

        // BatchWriteItem is expected to be called exactly (MAX_RETRY + 1) times
        expectBatchWriteItemReturnUnprocessedItems().times(MAX_RETRY + 1);

        replay(ddbMock);
        List<FailedBatch> failedBatches = mapper.batchSave(new Item("foo"));
        verify(ddbMock);

        Assert.assertEquals(1, failedBatches.size());
        FailedBatch failedBatch = failedBatches.get(0);

        Assert.assertEquals(
                "Failed batch should contain the same UnprocessedItems returned in the BatchWriteItem response.",
                unprocessedItems,
                failedBatch.getUnprocessedItems());
        Assert.assertNull(
                "No exception should be set if the batch failed after max retry",
                failedBatch.getException());
    }

    @Test
    public void testExceptionThrown_NoRetry() {

        RuntimeException exception = new RuntimeException("BOOM");
        expectedBatchWriteItemThrowException(exception);

        replay(ddbMock);
        // put a random item
        Item item = new Item(UUID.randomUUID().toString());
        List<FailedBatch> failedBatches = mapper.batchSave(item);
        verify(ddbMock);

        Assert.assertEquals(1, failedBatches.size());
        FailedBatch failedBatch = failedBatches.get(0);

        Assert.assertEquals(
                "Failed batch should contain all the input items for batchWrite",
                Collections.singletonMap(TABLE_NAME, Arrays.asList(item.toPutSaveRequest())),
                failedBatch.getUnprocessedItems());
        Assert.assertSame(
                "The exception should be the same as one thrown by BatchWriteItem",
                exception,
                failedBatch.getException());
    }

    private IExpectationSetters<BatchWriteItemResponse> expectBatchWriteItemSuccess() {
        return expect(ddbMock.batchWriteItem(isA(BatchWriteItemRequest.class)))
                .andReturn(BatchWriteItemResponse.builder()
                        .unprocessedItems(Collections.<String, List<WriteRequest>>emptyMap()).build());
    }

    private IExpectationSetters<BatchWriteItemResponse> expectBatchWriteItemReturnUnprocessedItems() {
        return expect(ddbMock.batchWriteItem(isA(BatchWriteItemRequest.class)))
                .andReturn(BatchWriteItemResponse.builder()
                                .unprocessedItems(unprocessedItems).build());
    }

    private void expectedBatchWriteItemThrowException(Exception e) {
        expect(ddbMock.batchWriteItem(isA(BatchWriteItemRequest.class)))
                .andThrow(e);
    }

    private DynamoDbMapperConfig getConfigWithCustomBatchWriteRetryStrategy(
            BatchWriteRetryStrategy batchWriteRetryStrategy) {
        return new DynamoDbMapperConfig.Builder()
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
        public int maxRetryOnUnprocessedItems(
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

    @DynamoDbTable(tableName = TABLE_NAME)
    public static class Item {

        private String hash;

        public Item(String hash) {
            this.hash = hash;
        }

        @DynamoDbHashKey
        @DynamoDbAttribute(attributeName = HASH_ATTR)
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
