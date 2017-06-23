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

package software.amazon.awssdk.services.sqs.buffered;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.sqs.SQSAsyncClient;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListDeadLetterSourceQueuesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.PurgeQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sqs.model.RemovePermissionResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesResponse;
import software.amazon.awssdk.util.VersionInfoUtils;

/**
 * AmazonSQSBufferedAsyncClient provides client-side batching of outgoing sendMessage, deleteMessage
 * and changeMessageVisibility calls. <br>
 * After receiving a call, rather than executing it right away, this client waits for a configurable
 * period of time ( default=200ms) for other calls of the same type to come in; if such calls do
 * come in, they are also not executed immediately, but instead are added to the batch. When the
 * batch becomes full or the timeout period expires, the entire batch is executed at once and the
 * results are returned to the callers. This method of operation leads to reduced operating costs
 * (since SQS charges per call and fewer calls are made) and increased overall throughput (since
 * more work is performed per call, and all fixed costs of making a call are amortized over a
 * greater amount of work). The cost of this method is increased latency for individual calls, since
 * calls spend some time waiting on the client side for the potential batch-mates to appear before
 * they are actually executed. <br>
 * This client also performs pre-fetching of messages from SQS. After the first receiveMessage call
 * is made, the client attempts not only to satisfy that call, but also pre-fetch extra messages to
 * store in a temporary buffer. Future receiveMessage calls will be satisfied from the buffer, and
 * only if the buffer is empty will the calling thread have to wait for the messages to be fetched.
 * The size of the buffer and the maximum number of threads used for prefetching are configurable. <br>
 * AmazonSQSBufferedAsyncClient is thread-safe.<br>
 */
public class SqsBufferedAsyncClient implements SQSAsyncClient {

    public static final String USER_AGENT = SqsBufferedAsyncClient.class.getSimpleName() + "/"
                                            + VersionInfoUtils.getVersion();

    private final CachingMap buffers = new CachingMap(16, (float) 0.75, true);
    private final SQSAsyncClient realSqs;
    private final QueueBufferConfig bufferConfigExemplar;

    public SqsBufferedAsyncClient(SQSAsyncClient paramRealSqs) {
        this(paramRealSqs, new QueueBufferConfig());
    }

    // route all future constructors to the most general one, because validation
    // happens here
    public SqsBufferedAsyncClient(SQSAsyncClient paramRealSqs, QueueBufferConfig config) {
        config.validate();
        realSqs = paramRealSqs;
        bufferConfigExemplar = config;
    }

    @Override
    public CompletableFuture<AddPermissionResponse> addPermission(AddPermissionRequest addPermissionRequest) {
        ResultConverter.appendUserAgent(addPermissionRequest, USER_AGENT);
        return realSqs.addPermission(addPermissionRequest);
    }

    @Override
    public CompletableFuture<CreateQueueResponse> createQueue(CreateQueueRequest createQueueRequest) {
        ResultConverter.appendUserAgent(createQueueRequest, USER_AGENT);
        return realSqs.createQueue(createQueueRequest);
    }

    @Override
    public CompletableFuture<DeleteQueueResponse> deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        ResultConverter.appendUserAgent(deleteQueueRequest, USER_AGENT);
        return realSqs.deleteQueue(deleteQueueRequest);
    }


    @Override
    public CompletableFuture<GetQueueAttributesResponse> getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest) {
        ResultConverter.appendUserAgent(getQueueAttributesRequest, USER_AGENT);
        return realSqs.getQueueAttributes(getQueueAttributesRequest);
    }

    @Override
    public CompletableFuture<GetQueueUrlResponse> getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        ResultConverter.appendUserAgent(getQueueUrlRequest, USER_AGENT);
        return realSqs.getQueueUrl(getQueueUrlRequest);
    }

    @Override
    public CompletableFuture<ListQueuesResponse> listQueues(ListQueuesRequest listQueuesRequest) {
        ResultConverter.appendUserAgent(listQueuesRequest, USER_AGENT);
        return realSqs.listQueues(listQueuesRequest);
    }

    @Override
    public CompletableFuture<PurgeQueueResponse> purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        ResultConverter.appendUserAgent(purgeQueueRequest, USER_AGENT);
        return realSqs.purgeQueue(purgeQueueRequest);
    }

    @Override
    public CompletableFuture<RemovePermissionResponse> removePermission(RemovePermissionRequest removePermissionRequest) {
        ResultConverter.appendUserAgent(removePermissionRequest, USER_AGENT);
        return realSqs.removePermission(removePermissionRequest);
    }

    @Override
    public CompletableFuture<SetQueueAttributesResponse> setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest)
            throws 
                   AmazonClientException {
        ResultConverter.appendUserAgent(setQueueAttributesRequest, USER_AGENT);
        return realSqs.setQueueAttributes(setQueueAttributesRequest);
    }

    @Override
    public CompletableFuture<ChangeMessageVisibilityBatchResponse> changeMessageVisibilityBatch(
            ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityBatchRequest, USER_AGENT);
        return realSqs.changeMessageVisibilityBatch(changeMessageVisibilityBatchRequest);
    }

    @Override
    public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(
            ChangeMessageVisibilityRequest changeMessageVisibilityRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(changeMessageVisibilityRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(changeMessageVisibilityRequest.queueUrl());
        return CompletableFuture.completedFuture(buffer.changeMessageVisibilitySync(changeMessageVisibilityRequest));
    }

    @Override
    public CompletableFuture<SendMessageBatchResponse> sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(sendMessageBatchRequest, USER_AGENT);
        return realSqs.sendMessageBatch(sendMessageBatchRequest);
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) throws
                                                                                       AmazonClientException {
        QueueBuffer buffer = getQBuffer(sendMessageRequest.queueUrl());
        ResultConverter.appendUserAgent(sendMessageRequest, USER_AGENT);
        return CompletableFuture.completedFuture(buffer.sendMessageSync(sendMessageRequest));
    }

    @Override
    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(receiveMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(receiveMessageRequest.queueUrl());
        return CompletableFuture.completedFuture(buffer.receiveMessageSync(receiveMessageRequest));
    }

    @Override
    public CompletableFuture<DeleteMessageBatchResponse> deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageBatchRequest, USER_AGENT);
        return realSqs.deleteMessageBatch(deleteMessageBatchRequest);
    }

    @Override
    public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest deleteMessageRequest) throws
            AmazonClientException {
        ResultConverter.appendUserAgent(deleteMessageRequest, USER_AGENT);
        QueueBuffer buffer = getQBuffer(deleteMessageRequest.queueUrl());
        return CompletableFuture.completedFuture(buffer.deleteMessageSync(deleteMessageRequest));
    }

    @Override
    public CompletableFuture<ListDeadLetterSourceQueuesResponse> listDeadLetterSourceQueues(
            ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest)
            throws  AmazonClientException {
        ResultConverter.appendUserAgent(listDeadLetterSourceQueuesRequest, USER_AGENT);
        return realSqs.listDeadLetterSourceQueues(listDeadLetterSourceQueuesRequest);
    }

    @Override
    public void close() throws Exception {
        realSqs.close();
    }

    /**
     * Returns (creating it if necessary) a queue buffer for a particular queue Since we are only
     * storing a limited number of queue buffers, it is possible that as a result of calling this
     * method the least recently used queue buffer will be removed from our queue buffer cache
     *
     * @return a queue buffer associated with the provided queue URL. Never null
     */
    private synchronized QueueBuffer getQBuffer(String qUrl) {
        QueueBuffer toReturn = buffers.get(qUrl);
        if (null == toReturn) {
            QueueBufferConfig config = new QueueBufferConfig(bufferConfigExemplar);
            toReturn = new QueueBuffer(config, qUrl, realSqs);
            buffers.put(qUrl, toReturn);
        }
        return toReturn;
    }

    private class CachingMap extends LinkedHashMap<String, QueueBuffer> {
        private static final long serialVersionUID = 1;
        private static final int MAX_ENTRIES = 100;

        CachingMap(int initial, float loadFactor, boolean accessOrder) {
            super(initial, loadFactor, accessOrder);
        }

        protected boolean removeEldestEntry(java.util.Map.Entry<String, QueueBuffer> eldest) {
            return size() > MAX_ENTRIES;
        }

    }
}
