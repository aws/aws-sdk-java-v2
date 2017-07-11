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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.AmazonWebServiceRequest;

/**
 * QueueBufferFuture class is used to deliver asynchronous results of various QueueBuffer
 * operations. QueueBufferFutures are not cancellable
 */
class QueueBufferFuture<ReqT extends AmazonWebServiceRequest, ResT> implements Future<ResT> {
    /**
     * callback we should call after the future is done. may be null
     */

    private final QueueBufferCallback<ReqT, ResT> callback;
    private ResT result = null;
    private Exception e = null;
    private boolean done = false;
    /**
     * every future should hold a reference to the buffer that issued it. that way, even if all
     * other references to the buffer are lost, it will not be garbage collected while at least one
     * future it issued is still outstanding.
     */
    private QueueBuffer issuingBuffer = null;

    QueueBufferFuture() {
        this(null);
    }

    QueueBufferFuture(QueueBufferCallback<ReqT, ResT> cb) {
        callback = cb;
    }

    /**
     * Report that the task this future represents has succeeded.
     */
    public synchronized void setSuccess(ResT paramResult) {
        if (done) {
            return; // can't mark done twice
        }
        result = paramResult;
        done = true;
        notifyAll();

        // if we have a callback to call, schedule
        // it on a different thread. Who knows what this
        // thread is doing.
        if (callback != null && issuingBuffer != null) {
            QueueBuffer.executor.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    callback.onSuccess(result);
                    return null;
                }
            });

        }
    }

    /**
     * Report that the task this future represents has failed.
     */
    public synchronized void setFailure(Exception paramE) {
        if (done) {
            return; // can't mark done twice
        }
        e = paramE;
        done = true;
        notifyAll();

        // if we have a callback to call, schedule
        // it on a different thread. Who knows what this
        // thread is doing.
        if (callback != null && issuingBuffer != null) {
            QueueBuffer.executor.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    callback.onError(e);
                    return null;
                }
            });

        }

    }

    @Override
    public boolean cancel(boolean arg0) {
        // not cancellable
        return false;
    }

    public void setBuffer(QueueBuffer paramBuffer) {
        issuingBuffer = paramBuffer;
    }

    @Override
    public ResT get() throws InterruptedException, ExecutionException {
        while (true) {
            try {
                return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                // shouldn't really happen, since we're specifying a very-very
                // long wait. but if it does, just loop
                // and wait more.
            }
        }
    }

    @Override
    public synchronized ResT get(long timeout, TimeUnit tu) throws InterruptedException, ExecutionException,
                                                                   TimeoutException {

        long waitStartMs = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        long timeoutMs = TimeUnit.MILLISECONDS.convert(timeout, tu);
        long timeToWaitMs = timeoutMs;

        while (!done) {
            // if timeToWaitMs is zero, we don't call wait() at all, because wait(0) means
            // "wait forever", which is the opposite of what we want.
            if (timeToWaitMs <= 0) {
                throw new TimeoutException("Timed out waiting for results after " + timeout + " " + tu);
            }

            wait(timeToWaitMs);

            // compute how long to wait in the next loop
            long nowMs = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
            timeToWaitMs = timeoutMs - (nowMs - waitStartMs);

        }

        // if we got here, we are done. Throw if there's anything to throw,
        // otherwise return the result
        if (e != null) {
            throw new ExecutionException(e);
        }

        // may be null, e.g. for Void futures
        return result;
    }

    @Override
    public boolean isCancelled() {
        // not cancellable
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }
}
