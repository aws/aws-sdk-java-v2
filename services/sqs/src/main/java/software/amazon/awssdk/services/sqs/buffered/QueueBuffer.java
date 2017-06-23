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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.handlers.AsyncHandler;
import software.amazon.awssdk.services.sqs.SQSAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * A buffer to operate on an SQS queue. The buffer batches outbound ( {@code SendMessage},
 * {@code DeleteMessage}, {@code ChangeMessageVisibility}) requests to the queue and pre-fetches
 * messages to receive. In practice, the buffer does almost no work itself, and delegates it to
 * SendQueueBufer and ReceiveQueueBuffer classes.
 * <p>
 * Any errors encountered are passed through to the callers, either as the appropriate Result
 * objects or as exceptions.
 * <p>
 * When the buffer is not used, all internal processing associated with the buffer stops when any
 * outstanding request to SQS completes. In that idle state, the buffer uses neither connections nor
 * threads.
 * <p>
 * Instances of {@code QueueBuffer} are thread-safe.
 */

class QueueBuffer {

    /**
     * This executor that will be shared among all queue buffers. Since a single JVM can access
     * hundreds of queues, it won't do to have hundreds of executors spinning up hundreds of threads
     * for each queue. The DaemonThreadFactory creates daemon threads, which means they won't block
     * the JVM from exiting if only they are still around.
     */
    static ExecutorService executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
    QueueBufferConfig config;
    private final SendQueueBuffer sendBuffer;
    private final ReceiveQueueBuffer receiveBuffer;
    private final SQSAsyncClient realSqs;


    QueueBuffer(QueueBufferConfig paramConfig, String url, SQSAsyncClient sqs) {
        realSqs = sqs;
        config = paramConfig;
        sendBuffer = new SendQueueBuffer(sqs, executor, paramConfig, url);
        receiveBuffer = new ReceiveQueueBuffer(sqs, executor, paramConfig, url);
    }

    /**
     * asynchronously enqueues a message to SQS.
     *
     * @return a Future object that will be notified when the operation is completed; never null
     */
    public Future<SendMessageResponse> sendMessage(SendMessageRequest request,
                                                 AsyncHandler<SendMessageRequest, SendMessageResponse> handler) {
        QueueBufferCallback<SendMessageRequest, SendMessageResponse> callback = null;
        if (handler != null) {
            callback = new QueueBufferCallback<SendMessageRequest, SendMessageResponse>(handler, request);
        }
        QueueBufferFuture<SendMessageRequest, SendMessageResponse> future = sendBuffer.sendMessage(request, callback);
        future.setBuffer(this);
        return future;
    }

    /**
     * Sends a message to SQS and returns the SQS reply.
     *
     * @return never null
     */
    public SendMessageResponse sendMessageSync(SendMessageRequest request) {
        Future<SendMessageResponse> future = sendMessage(request, null);
        return waitForFuture(future);
    }

    /**
     * Asynchronously deletes a message from SQS.
     *
     * @return a Future object that will be notified when the operation is completed; never null
     */

    public Future<DeleteMessageResponse> deleteMessage(DeleteMessageRequest request,
                                                     AsyncHandler<DeleteMessageRequest, DeleteMessageResponse> handler) {
        QueueBufferCallback<DeleteMessageRequest, DeleteMessageResponse> callback = null;
        if (handler != null) {
            callback = new QueueBufferCallback<DeleteMessageRequest, DeleteMessageResponse>(handler, request);
        }

        QueueBufferFuture<DeleteMessageRequest, DeleteMessageResponse> future = sendBuffer.deleteMessage(request, callback);
        future.setBuffer(this);
        return future;
    }

    /**
     * Deletes a message from SQS. Does not return until a confirmation from SQS has been received
     *
     * @return never null
     */
    public DeleteMessageResponse deleteMessageSync(DeleteMessageRequest request) {
        Future<DeleteMessageResponse> future = deleteMessage(request, null);
        return waitForFuture(future);
    }

    /**
     * asynchronously adjust a message's visibility timeout to SQS.
     *
     * @return a Future object that will be notified when the operation is completed; never null
     */

    public Future<ChangeMessageVisibilityResponse>
            changeMessageVisibility(ChangeMessageVisibilityRequest request,
                                    AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse> handler) {
        QueueBufferCallback<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse> callback = null;
        if (handler != null) {
            callback = new QueueBufferCallback<>(handler, request);
        }

        QueueBufferFuture<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResponse> future =
                sendBuffer.changeMessageVisibility(request, callback);
        future.setBuffer(this);
        return future;
    }

    /**
     * Changes visibility of a message in SQS. Does not return until a confirmation from SQS has
     * been received.
     */
    public ChangeMessageVisibilityResponse changeMessageVisibilitySync(ChangeMessageVisibilityRequest request) {
        Future<ChangeMessageVisibilityResponse> future = sendBuffer.changeMessageVisibility(request, null);
        return waitForFuture(future);
    }

    /**
     * Submits a request to receive some messages from SQS.
     *
     * @return a Future object that will be notified when the operation is completed; never null;
     */

    public Future<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest rq,
                                                       AsyncHandler<ReceiveMessageRequest, ReceiveMessageResponse> handler) {
        if (canBeRetrievedFromQueueBuffer(rq)) {
            QueueBufferCallback<ReceiveMessageRequest, ReceiveMessageResponse> callback = null;
            if (handler != null) {
                callback = new QueueBufferCallback<ReceiveMessageRequest, ReceiveMessageResponse>(handler, rq);
            }

            QueueBufferFuture<ReceiveMessageRequest, ReceiveMessageResponse> future = receiveBuffer.receiveMessageAsync(
                    rq, callback);
            future.setBuffer(this);
            return future;
        } else if (handler != null) {
            return realSqs.receiveMessage(rq);
        } else {
            return realSqs.receiveMessage(rq);
        }
    }

    /**
     * Retrieves messages from an SQS queue.
     *
     * @return never null
     */
    public ReceiveMessageResponse receiveMessageSync(ReceiveMessageRequest rq) {
        Future<ReceiveMessageResponse> future = receiveMessage(rq, null);
        return waitForFuture(future);
    }

    /**
     * Shuts down the queue buffer. Once this method has been called, the queue buffer is not
     * operational and all subsequent calls to it may fail.
     *
     * Enabling {@link QueueBufferConfig#flushOnShutdown} will wait for the pending tasks in
     * {@link SendQueueBuffer} to finish before shutting down.
     */
    public void shutdown() {
        if (config.isFlushOnShutdown()) {
            flush();
        }
        receiveBuffer.shutdown();
    }

    /**
     * Flushes all outstanding outbound requests in the {@link SendQueueBuffer}.
     */
    void flush() {
        sendBuffer.flush();
    }

    /**
     * We prefetch and load results in the buffer by making basic requests. I.E. we don't request
     * queue or message attributes and we have a default visibility timeout. If the user's request
     * deviates from the basic request we can't fulfill the request directly from the buffer, we
     * have to hit SQS directly (Note that when going to SQS directly messages currently in the
     * buffer may be unavailable due to the visibility timeout).
     *
     * @return True if the request can be fulfilled directly from the buffer, false if we have to go
     *         back to the service to fetch the results
     */
    private boolean canBeRetrievedFromQueueBuffer(ReceiveMessageRequest rq) {
        return !hasRequestedQueueAttributes(rq) && !hasRequestedMessageAttributes(rq) && isBufferingEnabled()
               && (rq.visibilityTimeout() == null);
    }

    /**
     * @return True if request has been configured to return queue attributes. False otherwise
     */
    private boolean hasRequestedQueueAttributes(ReceiveMessageRequest rq) {
        return rq.attributeNames() != null && !rq.attributeNames().isEmpty();
    }

    /**
     * @return True if request has been configured to return message attributes. False otherwise
     */
    private boolean hasRequestedMessageAttributes(ReceiveMessageRequest rq) {
        return rq.messageAttributeNames() != null && !rq.messageAttributeNames().isEmpty();
    }

    /**
     * @return True if the client has been configured to prefetch batches of messages. False
     *         otherwise
     */
    private boolean isBufferingEnabled() {
        return (config.getMaxInflightReceiveBatches() > 0 && config.getMaxDoneReceiveBatches() > 0);
    }

    /**
     * this method carefully waits for futures. If waiting throws, it converts the exceptions to the
     * exceptions that SQS clients expect. This is what we use to turn asynchronous calls into
     * synchronous ones
     */
    private <ResultTypeT> ResultTypeT waitForFuture(Future<ResultTypeT> future) {
        ResultTypeT toReturn = null;
        try {
            toReturn = future.get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            AmazonClientException ce = new AmazonClientException(
                    "Thread interrupted while waiting for execution result");
            ce.initCause(ie);
            throw ce;
        } catch (ExecutionException ee) {
            // if the cause of the execution exception is an SQS exception, extract it
            // and throw the extracted exception to the clients
            // otherwise, wrap ee in an SQS exception and throw that.
            Throwable cause = ee.getCause();

            if (cause instanceof AmazonClientException) {
                throw (AmazonClientException) cause;
            }

            AmazonClientException ce = new AmazonClientException(
                    "Caught an exception while waiting for request to complete...");
            ce.initCause(ee);
            throw ce;
        }

        return toReturn;

    }

    /**
     * We need daemon threads in our executor so that we don't keep the process running if our
     * executor threads are the only ones left in the process.
     */
    private static class DaemonThreadFactory implements ThreadFactory {
        static AtomicInteger threadCount = new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            int threadNumber = threadCount.addAndGet(1);
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("SQSQueueBufferWorkerThread-" + threadNumber);
            return thread;
        }

    }
}
