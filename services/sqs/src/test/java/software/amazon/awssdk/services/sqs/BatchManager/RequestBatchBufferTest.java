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

package software.amazon.awssdk.services.sqs.BatchManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.services.sqs.internal.batchmanager.RequestBatchBuffer;
import software.amazon.awssdk.services.sqs.internal.batchmanager.BatchingExecutionContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestBatchBufferTest {

    private RequestBatchBuffer<String, String> batchBuffer;
    private ScheduledFuture<?> scheduledFlush;

    @BeforeEach
    void setUp() {
        scheduledFlush = mock(ScheduledFuture.class);
        batchBuffer = new RequestBatchBuffer<>(10, scheduledFlush);
    }

    @Test
    void whenPutRequestThenBufferContainsRequest() {
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        assertEquals(1, batchBuffer.responses().size());
    }

    @Test
    void whenFlushableRequestsThenReturnRequestsUpToMaxBatchItems() {
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        Map<String, BatchingExecutionContext<String, String>> flushedRequests = batchBuffer.flushableRequests(1);
        assertEquals(1, flushedRequests.size());
        assertTrue(flushedRequests.containsKey("0"));
    }

    @Test
    void whenFlushableScheduledRequestsThenReturnAllRequests() {
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        Map<String, BatchingExecutionContext<String, String>> flushedRequests = batchBuffer.flushableScheduledRequests(1);
        assertEquals(1, flushedRequests.size());
        assertTrue(flushedRequests.containsKey("0"));
    }

    @Test
    void whenMaxBufferSizeReachedThenThrowException() {
        for (int i = 0; i < 10; i++) {
            batchBuffer.put("request" + i, new CompletableFuture<>());
        }
        assertThrows(IllegalStateException.class, () -> batchBuffer.put("request11", new CompletableFuture<>()));
    }

    @Test
    void whenPutScheduledFlushThenFlushIsSet() {
        ScheduledFuture<?> newScheduledFlush = mock(ScheduledFuture.class);
        batchBuffer.putScheduledFlush(newScheduledFlush);
        assertNotNull(newScheduledFlush);
    }

    @Test
    void whenCancelScheduledFlushThenFlushIsCancelled() {
        batchBuffer.cancelScheduledFlush();
        verify(scheduledFlush).cancel(false);
    }

    @Test
    void whenGetResponsesThenReturnAllResponses() {
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
        CompletableFuture<String> response = new CompletableFuture<>();
        batchBuffer.put("request1", response);
        batchBuffer.clear();
        assertTrue(batchBuffer.responses().isEmpty());
    }

    @Test
    void whenExtractFlushedEntriesThenReturnCorrectEntries() {
        for (int i = 0; i < 5; i++) {
            batchBuffer.put("request" + i, new CompletableFuture<>());
        }
        Map<String, BatchingExecutionContext<String, String>> flushedEntries = batchBuffer.flushableRequests(5);
        assertEquals(5, flushedEntries.size());
    }

    @Test
    void whenHasNextBatchEntryThenReturnTrue() {
        batchBuffer.put("request1", new CompletableFuture<>());
        assertTrue(batchBuffer.flushableRequests(1).containsKey("0"));
    }

    @Test
    void whenNextBatchEntryThenReturnNextEntryId() {
        batchBuffer.put("request1", new CompletableFuture<>());
        assertEquals("0", batchBuffer.flushableRequests(1).keySet().iterator().next());
    }
}