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

package software.amazon.awssdk.services.sqs.internal.batchmanager.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;


/**
 * Interface representing a buffer that handles batching of requests and responses.
 *
 * @param <RequestT>  the type of the request
 * @param <ResponseT> the type of the response
 */
@SdkInternalApi
public interface BatchBuffer<RequestT, ResponseT> {
    /**
     * Retrieves a map of flushable requests up to the specified maximum number of batch items.
     *
     * @param maxBatchItems the maximum number of items to be included in the flushed batch
     * @return a map of batch execution contexts for flushable requests
     */
    Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableRequests(int maxBatchItems);

    /**
     * Retrieves a map of flushable scheduled requests up to the specified maximum number of batch items.
     *
     * @param maxBatchItems the maximum number of items to be included in the flushed batch
     * @return a map of batch execution contexts for flushable scheduled requests
     */
    Map<String, BatchingExecutionContext<RequestT, ResponseT>> flushableScheduledRequests(int maxBatchItems);

    /**
     * Adds a request and its corresponding response future to the batch buffer.
     *
     * @param request  the request to be added to the buffer
     * @param response the future response associated with the request
     */
    void put(RequestT request, CompletableFuture<ResponseT> response);

    /**
     * Sets the scheduled flush task for the batch buffer.
     *
     * @param scheduledFlush the scheduled future representing the flush task
     */
    void putScheduledFlush(ScheduledFuture<?> scheduledFlush);

    /**
     * Cancels the scheduled flush task for the batch buffer.
     */
    void cancelScheduledFlush();

    /**
     * Retrieves a collection of response futures from the batch buffer.
     *
     * @return a collection of CompletableFuture objects representing the responses
     */
    Collection<CompletableFuture<ResponseT>> responses();

    /**
     * Clears all entries from the batch buffer.
     */
    void clear();
}