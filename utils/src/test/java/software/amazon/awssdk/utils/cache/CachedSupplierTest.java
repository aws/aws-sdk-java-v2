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

package software.amazon.awssdk.utils.cache;

import static org.junit.Assert.fail;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.Closeable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Validate the functionality of {@link CachedSupplier}.
 */
public class CachedSupplierTest {
    /**
     * An executor for performing "get" on the cached supplier asynchronously. This, along with the {@link WaitingSupplier} allows
     * near-manual scheduling of threads so that we can test that the cache is only calling the underlying supplier when we want
     * it to.
     */
    private ExecutorService executorService;

    /**
     * All executions added to the {@link #executorService} since the beginning of an individual test method.
     */
    private List<Future<?>> allExecutions;

    /**
     * Create an executor service for async testing.
     */
    @Before
    public void setup() {
        executorService = Executors.newFixedThreadPool(50);
        allExecutions = new ArrayList<>();
    }

    /**
     * Shut down the executor service when we're done.
     */
    @After
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * An implementation of {@link Supplier} that allows us to (more or less) manually schedule threads so that we can make sure
     * the CachedSupplier is only calling the underlying supplier when we expect it to.
     */
    private static class WaitingSupplier implements Supplier<RefreshResult<String>>, Closeable {
        /**
         * A semaphore that is counted up each time a "get" is started. This is useful during testing for waiting for a certain
         * number of "gets" to start.
         */
        private final Semaphore startedGetPermits = new Semaphore(0);

        /**
         * A semaphore that is counted down each time a "get" is started. This is useful during testing for blocking the threads
         * performing the "get" until it is time for them to complete.
         */
        private final Semaphore permits = new Semaphore(0);

        /**
         * A semaphore that is counted up each time a "get" is finished. This is useful during testing for waiting for a certain
         * number of "gets" to finish.
         */
        private final Semaphore finishedGetPermits = new Semaphore(0);

        private final Instant staleTime;
        private final Instant prefetchTime;

        private WaitingSupplier(Instant staleTime, Instant prefetchTime) {
            this.staleTime = staleTime;
            this.prefetchTime = prefetchTime;
        }

        @Override
        public RefreshResult<String> get() {
            startedGetPermits.release(1);

            try {
                permits.acquire(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }

            finishedGetPermits.release(1);
            return RefreshResult.builder("value")
                                .staleTime(staleTime)
                                .prefetchTime(prefetchTime)
                                .build();
        }

        /**
         * Wait for a certain number of "gets" to have started. This will time out and fail the test after a certain amount of
         * time if the "gets" never actually start.
         */
        public void waitForGetsToHaveStarted(int numExpectedGets) {
            Assert.assertTrue(invokeSafely(() -> startedGetPermits.tryAcquire(numExpectedGets, 10, TimeUnit.SECONDS)));
        }

        /**
         * Wait for a certain number of "gets" to have finished. This will time out and fail the test after a certain amount of
         * time if the "gets" never finish.
         */
        public void waitForGetsToHaveFinished(int numExpectedGets) {
            Assert.assertTrue(invokeSafely(() -> finishedGetPermits.tryAcquire(numExpectedGets, 10, TimeUnit.SECONDS)));
        }

        /**
         * Release all threads blocked in this supplier.
         */
        @Override
        public void close() {
            permits.release(50);
        }
    }

    @Test
    public void allCallsBeforeInitializationBlock() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), future())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier).build();

            // Perform two "gets".
            performAsyncGets(cachedSupplier, 2);

            // Make sure both "gets" are started.
            waitingSupplier.waitForGetsToHaveStarted(2);
        }
    }

    @Test
    public void staleValueBlocksAllCalls() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(past(), future())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier).build();

            // Perform one successful "get".
            waitingSupplier.permits.release(1);
            waitFor(performAsyncGet(cachedSupplier));

            // Perform two "get"s that will attempt to refresh the value, and wait for them to get stuck.
            performAsyncGets(cachedSupplier, 2);
            waitingSupplier.waitForGetsToHaveStarted(3);

            // Release any "gets" that blocked and wait for them to finish.
            waitingSupplier.permits.release(50);
            waitForAsyncGetsToFinish();

            // Make extra sure all 3 "gets" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(3);
        }
    }

    @Test
    public void basicCachingWorks() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), future())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier).build();

            // Perform 5 "gets".
            waitingSupplier.permits.release(5);
            waitFor(performAsyncGets(cachedSupplier, 5));

            // Make extra sure only 1 "get" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(1);
        }
    }

    @Test
    public void oneCallerBlocksPrefetchStrategyWorks() throws InterruptedException {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), past())) {
            CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                  .prefetchStrategy(new OneCallerBlocks())
                                                                  .build();

            // Perform one successful "get" to prime the cache.
            waitingSupplier.permits.release(1);
            waitFor(performAsyncGet(cachedSupplier));

            // Perform one "get" that will attempt to refresh the value, and wait for that one to get stuck.
            performAsyncGet(cachedSupplier);
            waitingSupplier.waitForGetsToHaveStarted(2);

            // Perform a successful "get" because one is already blocked to refresh.
            waitFor(performAsyncGet(cachedSupplier));

            // Release any "gets" that blocked and wait for them to finish.
            waitingSupplier.permits.release(50);
            waitForAsyncGetsToFinish();

            // Make extra sure only 2 "gets" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(2);
        }
    }

    @Test
    public void nonBlockingPrefetchStrategyWorks() {
        try (WaitingSupplier waitingSupplier = new WaitingSupplier(future(), past());
             CachedSupplier<String> cachedSupplier = CachedSupplier.builder(waitingSupplier)
                                                                   .prefetchStrategy(new NonBlocking("test-%s"))
                                                                   .build()) {
            // Perform one successful "get" to prime the cache.
            waitingSupplier.permits.release(1);
            waitFor(performAsyncGet(cachedSupplier));

            // Perform one successful "get" to kick off the async refresh.
            waitFor(performAsyncGet(cachedSupplier));

            // Wait for the async "get" in the background to start (if it hasn't already).
            waitingSupplier.waitForGetsToHaveStarted(2);

            // Make sure only one "get" has actually happened (the async get is currently waiting to be released).
            waitingSupplier.waitForGetsToHaveFinished(1);
        }
    }

    /**
     * Asynchronously perform a "get" on the provided supplier, returning the future that will be completed when the "get"
     * finishes.
     */
    private Future<?> performAsyncGet(CachedSupplier<?> supplier) {
        return executorService.submit(supplier::get);
    }

    /**
     * Asynchronously perform multiple "gets" on the provided supplier, returning the collection of futures to be completed when
     * the "get" finishes.
     */
    private List<Future<?>> performAsyncGets(CachedSupplier<?> supplier, int count) {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            futures.add(performAsyncGet(supplier));
        }
        allExecutions.addAll(futures);
        return futures;
    }

    /**
     * Wait for the provided future to complete, failing the test if it does not.
     */
    private void waitFor(Future<?> future) {
        invokeSafely(() -> future.get(10, TimeUnit.SECONDS));
    }

    /**
     * Wait for all futures in the provided collection fo complete, failing the test if they do not all complete.
     */
    private void waitFor(Collection<Future<?>> futures) {
        futures.forEach(this::waitFor);
    }

    /**
     * Wait for all async gets ever created by this class to complete, failing the test if they do not all complete.
     */
    private void waitForAsyncGetsToFinish() {
        waitFor(allExecutions);
    }

    private Instant past() {
        return Instant.now().minusSeconds(1);
    }

    private Instant future() {
        return Instant.MAX;
    }
}
