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

package software.amazon.awssdk.services.sqs.batchmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchBuffer;
import software.amazon.awssdk.services.sqs.internal.batchmanager.BatchingExecutionContext;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static software.amazon.awssdk.services.sqs.internal.batchmanager.ResponseBatchConfiguration.MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES;

class RequestBatchBufferTest {

    private RequestBatchBuffer<String, String> batchBuffer;
    private ScheduledFuture<?> scheduledFlush;

    private static int maxBufferSize = 1000;

    @BeforeEach
    void setUp() {
        scheduledFlush = mock(ScheduledFuture.class);
    }

    @Test
    void whenPutRequestThenBufferContainsRequest() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        assertEquals(1, batchBuffer.responses().size());
    }

    @Test
    void whenFlushableRequestsThenReturnRequestsUpToMaxBatchItems() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 1, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        Map<String, BatchingExecutionContext<String, String>> flushedRequests = batchBuffer.flushableRequests();
        assertEquals(1, flushedRequests.size());
        assertTrue(flushedRequests.containsKey("0"));
    }

    @Test
    void whenFlushableScheduledRequestsThenReturnAllRequests() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        Map<String, BatchingExecutionContext<String, String>> flushedRequests = batchBuffer.flushableScheduledRequests(1);
        assertEquals(1, flushedRequests.size());
        assertTrue(flushedRequests.containsKey("0"));
    }

    @Test
    void whenMaxBufferSizeReachedThenThrowException() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 3, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, 10);
        for (int i = 0; i < 10; i++) {
            batchBuffer.put("request" + i, new CompletableFuture<>());
        }
        assertThrows(IllegalStateException.class, () -> batchBuffer.put("request11", new CompletableFuture<>()));
    }

    @Test
    void whenPutScheduledFlushThenFlushIsSet() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        ScheduledFuture<?> newScheduledFlush = mock(ScheduledFuture.class);
        batchBuffer.putScheduledFlush(newScheduledFlush);
        assertNotNull(newScheduledFlush);
    }

    @Test
    void whenCancelScheduledFlushThenFlushIsCancelled() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        batchBuffer.cancelScheduledFlush();
        verify(scheduledFlush).cancel(false);
    }

    @Test
    void whenGetResponsesThenReturnAllResponses() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response1 = new CompletableFuture<>();
        CompletableFuture<String> response2 = new CompletableFuture<>();
        batchBuffer.put("request1", response1);
        batchBuffer.put("request2", response2);
        Collection<CompletableFuture<String>> responses = batchBuffer.responses();
        assertEquals(2, responses.size());
        assertTrue(responses.contains(response1));
        assertTrue(responses.contains(response2));
    }

    @Test
    void whenClearBufferThenBufferIsEmpty() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        batchBuffer.clear();
        assertTrue(batchBuffer.responses().isEmpty());
    }

    @Test
    void whenExtractFlushedEntriesThenReturnCorrectEntries() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        for (int i = 0; i < 5; i++) {
            batchBuffer.put("request" + i, new CompletableFuture<>());
        }
        Map<String, BatchingExecutionContext<String, String>> flushedEntries = batchBuffer.flushableRequests();
        assertEquals(5, flushedEntries.size());
    }

    @Test
    void whenHasNextBatchEntryThenReturnTrue() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 1, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        batchBuffer.put("request1", new CompletableFuture<>());
        assertTrue(batchBuffer.flushableRequests().containsKey("0"));
    }


    @Test
    void whenNextBatchEntryThenReturnNextEntryId() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 1, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        batchBuffer.put("request1", new CompletableFuture<>());
        assertEquals("0", batchBuffer.flushableRequests().keySet().iterator().next());
    }

    @Test
    void whenRequestPassedWithLessBytesinArgs_thenCheckForSizeOnly_andDonotFlush() {
        RequestBatchBuffer<SendMessageRequest, SendMessageResponse> batchBuffer
            = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        for (int i = 0; i < 5; i++) {
            batchBuffer.put(SendMessageRequest.builder().build(),
                            new CompletableFuture<>());
        }
        Map<String, BatchingExecutionContext<SendMessageRequest, SendMessageResponse>> flushedEntries =
            batchBuffer.flushableRequestsOnByteLimitBeforeAdd(SendMessageRequest.builder().messageBody("Hi").build());
        assertEquals(0, flushedEntries.size());
    }



    @Test
    void testFlushWhenPayloadExceedsMaxSize() {
        RequestBatchBuffer<SendMessageRequest, SendMessageResponse> batchBuffer
            = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);

        String largeMessageBody = createLargeString('a',245_760);
        batchBuffer.put(SendMessageRequest.builder().messageBody(largeMessageBody).build(),
                        new CompletableFuture<>());
        Map<String, BatchingExecutionContext<SendMessageRequest, SendMessageResponse>> flushedEntries =
            batchBuffer.flushableRequestsOnByteLimitBeforeAdd(SendMessageRequest.builder().messageBody("NewMessage").build());
        assertEquals(1, flushedEntries.size());
    }

    @Test
    void testFlushWhenCumulativePayloadExceedsMaxSize() {
        RequestBatchBuffer<SendMessageRequest, SendMessageResponse> batchBuffer
            = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);

        String largeMessageBody = createLargeString('a',130_000);
        batchBuffer.put(SendMessageRequest.builder().messageBody(largeMessageBody).build(),
                        new CompletableFuture<>());
        batchBuffer.put(SendMessageRequest.builder().messageBody(largeMessageBody).build(),
                        new CompletableFuture<>());
        Map<String, BatchingExecutionContext<SendMessageRequest, SendMessageResponse>> flushedEntries =
            batchBuffer.flushableRequestsOnByteLimitBeforeAdd(SendMessageRequest.builder().messageBody("NewMessage").build());

        //Flushes both the messages since thier sum is greater than 256Kb
        assertEquals(2, flushedEntries.size());
    }


    private String createLargeString(char ch, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }



}