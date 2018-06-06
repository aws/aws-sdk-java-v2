/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.pagination.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class PaginationSubscription<ResponseT> implements Subscription {

    protected AtomicLong outstandingRequests = new AtomicLong(0);
    protected final Subscriber subscriber;
    protected final AsyncPageFetcher<ResponseT> nextPageFetcher;
    protected volatile ResponseT currentPage;

    // boolean indicating whether subscription is terminated
    private AtomicBoolean isTerminated = new AtomicBoolean(false);

    // boolean indicating whether task to handle requests is running
    private AtomicBoolean isTaskRunning = new AtomicBoolean(false);

    public PaginationSubscription(Subscriber subscriber, AsyncPageFetcher<ResponseT> nextPageFetcher) {
        this.subscriber = subscriber;
        this.nextPageFetcher = nextPageFetcher;
    }

    @Override
    public void request(long n) {
        if (isTerminated()) {
            return;
        }

        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("Non-positive request signals are illegal"));
        }

        AtomicBoolean startTask = new AtomicBoolean(false);
        synchronized (this) {
            outstandingRequests.addAndGet(n);
            startTask.set(startTask());
        }

        if (startTask.get()) {
            handleRequests();
        }
    }

    /**
     * Recursive method to deal with requests until there are no outstandingRequests or
     * no more pages.
     */
    protected abstract void handleRequests();

    @Override
    public void cancel() {
        cleanup();
    }

    protected boolean hasNextPage() {
        return currentPage == null || nextPageFetcher.hasNextPage(currentPage);
    }

    protected void completeSubscription() {
        if (!isTerminated()) {
            subscriber.onComplete();
            cleanup();
        }
    }

    private void terminate() {
        isTerminated.compareAndSet(false, true);
    }

    protected boolean isTerminated() {
        return isTerminated.get();
    }

    protected void stopTask() {
        isTaskRunning.set(false);
    }

    private synchronized boolean startTask() {
        return !isTerminated() && isTaskRunning.compareAndSet(false, true);
    }

    protected synchronized void cleanup() {
        terminate();
        stopTask();
    }
}
