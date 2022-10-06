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

package software.amazon.awssdk.utils.cache.lru;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LruCacheTest {

    private static final int MAX_SIMPLE_TEST_ENTRIES = 10;
    private static final int MAX_SIMPLE_CACHE_SIZE = 3;
    private static final List<Key> simpleTestKeys = IntStream.range(0, MAX_SIMPLE_TEST_ENTRIES)
                                                             .mapToObj(Key::new)
                                                             .collect(Collectors.toList());
    private static final List<Value> simpleTestValues = IntStream.range(0, MAX_SIMPLE_TEST_ENTRIES)
                                                                 .mapToObj(Integer::toString)
                                                                 .map(Value::new)
                                                                 .collect(Collectors.toList());
    @Spy
    private Function<Key, Value> simpleValueSupplier = new SimpleValueSupplier(simpleTestValues);

    /**
     * An implementation of {@link Function} that allows us to (more or less) manually schedule threads so that we can make sure
     * the cache is only calling the underlying supplier when we expect it to.
     */
    private static class WaitingSupplier implements Function<Key, Value>, Closeable {
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

        private WaitingSupplier() {
        }

        @Override
        public Value apply(Key key) {
            startedGetPermits.release(1);

            try {
                permits.acquire(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }

            finishedGetPermits.release(1);
            return simpleTestValues.get(key.key());
        }

        /**
         * Wait for a certain number of "gets" to have started. This will time out and fail the test after a certain amount of
         * time if the "gets" never actually start.
         */
        public void waitForGetsToHaveStarted(int numExpectedGets) {
            assertTrue(invokeSafely(() -> startedGetPermits.tryAcquire(numExpectedGets, 10, TimeUnit.SECONDS)));
        }

        /**
         * Wait for a certain number of "gets" to have finished. This will time out and fail the test after a certain amount of
         * time if the "gets" never finish.
         */
        public void waitForGetsToHaveFinished(int numExpectedGets) {
            assertTrue(invokeSafely(() -> finishedGetPermits.tryAcquire(numExpectedGets, 10, TimeUnit.SECONDS)));
        }

        /**
         * Release all threads blocked in this supplier.
         */
        @Override
        public void close() {
            permits.release(50);
        }
    }


    private ExecutorService executorService;
    private List<Future<?>> allExecutions;
    private Supplier<LruCache<Key, Value>> simpleCache = () -> LruCache.builder(simpleValueSupplier)
                                                                       .maxSize(MAX_SIMPLE_CACHE_SIZE)
                                                                       .build();



    @BeforeEach
    public void setup() {
        executorService = Executors.newFixedThreadPool(50);
        allExecutions = new ArrayList<>();
    }


    @AfterEach
    public void shutdown() {
        executorService.shutdown();
    }

    @Test
    void when_cacheHasMiss_ValueIsCalculatedAndCached() {
        LruCache<Key, Value> cache = simpleCache.get();
        populateAndVerify(cache, 1);
    }

    @Test
    void when_cacheHasHit_ValueIsRetrievedFromCache() {
        LruCache<Key, Value> cache = simpleCache.get();

        populateAndVerify(cache, MAX_SIMPLE_CACHE_SIZE);
        Value result = cache.get(simpleTestKeys.get(2));
        assertThat(cache.size()).isEqualTo(MAX_SIMPLE_CACHE_SIZE);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(simpleTestValues.get(2));

        cache.get(simpleTestKeys.get(2));

        int onlyOneInvocation = 1;
        verify(simpleValueSupplier, times(onlyOneInvocation)).apply(simpleTestKeys.get(2));
    }

    @Test
    void when_cacheFillsUp_ValuesAreEvictedFromCache() {
        LruCache<Key, Value> cache = simpleCache.get();

        populateAndVerify(cache, MAX_SIMPLE_CACHE_SIZE);

        //evict 1
        Value result = cache.get(simpleTestKeys.get(4));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 4);

        //move 3 up
        result = cache.get(simpleTestKeys.get(3));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 3);

        //evict 2
        result = cache.get(simpleTestKeys.get(5));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 5);

        //move 4 up
        result = cache.get(simpleTestKeys.get(4));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 4);

        //evict 3
        result = cache.get(simpleTestKeys.get(2));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 2);

        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(1));
        verify(simpleValueSupplier, times(2)).apply(simpleTestKeys.get(2));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(3));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(4));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(5));
    }

    @Test
    void when_multipleCallers_oneCallerBlocks_simple() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        try  {
            LruCache<Key, Value> cache = simpleCache.get();

            for (int i = 0; i < 1000; i++) {
                int numEntry = ThreadLocalRandom.current().nextInt(MAX_SIMPLE_TEST_ENTRIES - 1);
                executor.submit(() -> cache.get(simpleTestKeys.get(numEntry)));
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void when_multipleCallers_oneCallerBlock_doesnt_Work() {

        try (WaitingSupplier waitingSupplier = new WaitingSupplier()) {
            LruCache<Key, Value> cachedSupplier = LruCache.builder(waitingSupplier)
                                                          .maxSize(5)
                                                          .build();

            waitingSupplier.permits.release(5);
            waitFor(performAsyncGets(cachedSupplier, 8));

            // Make extra sure only 2 "gets" actually happened.
            waitingSupplier.waitForGetsToHaveFinished(2);
        }
    }

    private void assertCacheState(LruCache<Key, Value> cache, Value result, int size, int index) {
        assertThat(cache.size()).isEqualTo(size);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(simpleTestValues.get(index));
    }

    private void populateAndVerify(LruCache<Key, Value> cache, int numEntries) {
        IntStream.range(0, numEntries).forEach(i -> {
            Value result = cache.get(simpleTestKeys.get(i));
            assertThat(cache.size()).isEqualTo(i + 1);
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(simpleTestValues.get(i));
            verify(simpleValueSupplier).apply(simpleTestKeys.get(i));
        });
    }

    /**
     * Asynchronously perform a "get" on the provided supplier, returning the future that will be completed when the "get"
     * finishes.
     */
    private Future<Value> performAsyncGet(LruCache<Key, Value> supplier, Key key) {
        return executorService.submit(() -> supplier.get(key));
    }

    /**
     * Asynchronously perform multiple "gets" on the provided supplier, returning the collection of futures to be completed when
     * the "get" finishes.
     */
    private List<Future<?>> performAsyncGets(LruCache<Key, Value> supplier, int count) {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            futures.add(performAsyncGet(supplier, simpleTestKeys.get(count % 2)));
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

    private static final class Key {
        private final Integer key;

        private Key(Integer key) {
            this.key = key;
        }

        static Key create(Integer key) {
            return new Key(key);
        }

        Integer key() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || getClass() != o.getClass()) {
                return false;
            }
            Key that = (Key) o;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return key != null? key.hashCode() : 0;
        }
    }

    private static final class Value {
        private final String param1;

        Value(String param1) {
            this.param1 = param1;
        }

        String param1() {
            return param1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || getClass() != o.getClass()) {
                return false;
            }
            Value that = (Value) o;
            return Objects.equals(param1, that.param1);
        }

        @Override
        public int hashCode() {
            return param1 != null ? param1.hashCode() : 0;
        }
    }

    private static class SimpleValueSupplier implements Function<Key, Value> {

        List<Value> values;

        SimpleValueSupplier(List<Value> values) {
            this.values = values;
        }

        @Override
        public Value apply(Key key) {
            return values.get(key.key());
        }
    }
}
