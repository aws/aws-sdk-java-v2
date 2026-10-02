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
    void whenExtractBatchIfReadyThenReturnRequestsUpToMaxBatchItems() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 1, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        Map<String, BatchingExecutionContext<String, String>> extractedEntries = batchBuffer.extractBatchIfReady();
        assertEquals(1, extractedEntries.size());
        assertTrue(extractedEntries.containsKey("0"));
    }

    @Test
    void whenExtractEntriesForScheduledFlushThenReturnAllRequests() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        Map<String, BatchingExecutionContext<String, String>> extractedEntries = batchBuffer.extractEntriesForScheduledFlush(1);
        assertEquals(1, extractedEntries.size());
        assertTrue(extractedEntries.containsKey("0"));
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
    void whenCancelAndReplaceScheduledFlushThenFlushIsSetAndOldFlushIsCanceled() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        ScheduledFuture<?> newScheduledFlush = mock(ScheduledFuture.class);
        batchBuffer.cancelAndReplaceScheduledFlush(newScheduledFlush);
        assertNotNull(newScheduledFlush);
        verify(scheduledFlush).cancel(false);
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
    void whenExtractEntriesThenReturnCorrectEntries() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        for (int i = 0; i < 5; i++) {
            batchBuffer.put("request" + i, new CompletableFuture<>());
        }
        Map<String, BatchingExecutionContext<String, String>> extractedEntries = batchBuffer.extractBatchIfReady();
        assertEquals(5, extractedEntries.size());
    }

    @Test
    void whenHasNextBatchEntryThenReturnTrue() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 1, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        batchBuffer.put("request1", new CompletableFuture<>());
        assertTrue(batchBuffer.extractBatchIfReady().containsKey("0"));
    }


    @Test
    void whenNextBatchEntryThenReturnNextEntryId() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 1, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        batchBuffer.put("request1", new CompletableFuture<>());
        assertEquals("0", batchBuffer.extractBatchIfReady().keySet().iterator().next());
    }

    @Test
    void whenRequestPassedWithLessBytesinArgs_thenCheckForSizeOnly_andDonotFlush() {
        RequestBatchBuffer<SendMessageRequest, SendMessageResponse> batchBuffer
            = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        for (int i = 0; i < 5; i++) {
            batchBuffer.put(SendMessageRequest.builder().build(),
                            new CompletableFuture<>());
        }
        Map<String, BatchingExecutionContext<SendMessageRequest, SendMessageResponse>> extractedEntries =
            batchBuffer.extractBatchIfSizeExceeded(SendMessageRequest.builder().messageBody("Hi").build());
        assertEquals(0, extractedEntries.size());
    }



    @Test
    void testFlushWhenPayloadExceedsMaxSize() {
        RequestBatchBuffer<SendMessageRequest, SendMessageResponse> batchBuffer
            = new RequestBatchBuffer<>(scheduledFlush, 5, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);

        String largeMessageBody = createLargeString('a',245_760);
        batchBuffer.put(SendMessageRequest.builder().messageBody(largeMessageBody).build(),
                        new CompletableFuture<>());
        Map<String, BatchingExecutionContext<SendMessageRequest, SendMessageResponse>> extractedEntries =
            batchBuffer.extractBatchIfSizeExceeded(SendMessageRequest.builder().messageBody("NewMessage").build());
        assertEquals(1, extractedEntries.size());
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
        Map<String, BatchingExecutionContext<SendMessageRequest, SendMessageResponse>> extractedEntries =
            batchBuffer.extractBatchIfSizeExceeded(SendMessageRequest.builder().messageBody("NewMessage").build());

        //Flushes both the messages since thier sum is greater than 256Kb
        assertEquals(2, extractedEntries.size());
    }


    @Test
    void whenSequentialCancelAndReplaceScheduledFlushThenEachPreviousFlushIsCanceled() {
        batchBuffer = new RequestBatchBuffer<>(scheduledFlush, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        
        // Create a sequence of mock scheduled futures
        ScheduledFuture<?> flush1 = mock(ScheduledFuture.class);
        ScheduledFuture<?> flush2 = mock(ScheduledFuture.class);
        ScheduledFuture<?> flush3 = mock(ScheduledFuture.class);
        
        // First replacement - should cancel the initial scheduledFlush
        batchBuffer.cancelAndReplaceScheduledFlush(flush1);
        verify(scheduledFlush, times(1)).cancel(false);
        
        // Second replacement - should cancel flush1
        batchBuffer.cancelAndReplaceScheduledFlush(flush2);
        verify(flush1, times(1)).cancel(false);
        
        // Verify flush2 has not been canceled (it's the current one)
        verify(flush2, never()).cancel(false);
        
        // Verify buffer is still functional
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("test-request", response);
        assertEquals(1, batchBuffer.responses().size());
    }

    @Test
    void whenCancelAndReplaceScheduledFlushWithNullInitialFlushThenNoExceptionThrown() {
        // Create buffer with null initial flush
        batchBuffer = new RequestBatchBuffer<>(null, 10, MAX_SEND_MESSAGE_PAYLOAD_SIZE_BYTES, maxBufferSize);
        
        ScheduledFuture<?> newFlush = mock(ScheduledFuture.class);
        
        // Should not throw exception when initial flush is null
        assertDoesNotThrow(() -> batchBuffer.cancelAndReplaceScheduledFlush(newFlush));
        
        // Verify newFlush is not canceled (it's the current one)
        verify(newFlush, never()).cancel(false);
    }

    private String createLargeString(char ch, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }



}
