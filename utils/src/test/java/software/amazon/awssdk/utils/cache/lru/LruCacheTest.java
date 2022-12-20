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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LruCacheTest {

    private static final int MAX_SIMPLE_TEST_ENTRIES = 10;
    private static final int MAX_SIMPLE_CACHE_SIZE = 3;
    private static final List<Integer> simpleTestKeys = IntStream.range(0, MAX_SIMPLE_TEST_ENTRIES)
                                                                 .boxed()
                                                                 .collect(Collectors.toList());
    private static final List<String> simpleTestValues = IntStream.range(0, MAX_SIMPLE_TEST_ENTRIES)
                                                                  .mapToObj(Integer::toString)
                                                                  .map(String::new)
                                                                  .collect(Collectors.toList());
    @Spy
    private Function<Integer, String> simpleValueSupplier = new SimpleValueSupplier(simpleTestValues);
    private final Function<Integer, String> identitySupplier = key -> Integer.toString(key);

    private final Supplier<LruCache<Integer, String>> simpleCache = () -> LruCache.builder(simpleValueSupplier)
                                                                                  .maxSize(MAX_SIMPLE_CACHE_SIZE)
                                                                                  .build();
    @Test
    void when_cacheHasMiss_ValueIsCalculatedAndCached() {
        LruCache<Integer, String> cache = simpleCache.get();
        primeAndVerifySimpleCache(cache, 1);
    }

    @Test
    void when_cacheHasHit_ValueIsRetrievedFromCache() {
        LruCache<Integer, String> cache = simpleCache.get();

        primeAndVerifySimpleCache(cache, MAX_SIMPLE_CACHE_SIZE);

        //get 2, the last added value. Should get it from cache
        String result = cache.get(simpleTestKeys.get(2));
        assertThat(cache.size()).isEqualTo(MAX_SIMPLE_CACHE_SIZE);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(simpleTestValues.get(2));

        //item 2 was only retrieved once from the supplier, but twice from cache
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(2));
    }

    @Test
    void when_cacheFillsUp_ValuesAreEvictedFromCache() {
        LruCache<Integer, String> cache = simpleCache.get();

        //fill cache [2, 1, 0]
        primeAndVerifySimpleCache(cache, MAX_SIMPLE_CACHE_SIZE);

        //new item requested, evict 0 -> [3, 2, 1]
        String result = cache.get(simpleTestKeys.get(3));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 3);

        //move 2 up -> [2, 3, 1]
        cache.get(simpleTestKeys.get(2));

        //evict 1 -> [4, 2, 3]
        cache.get(simpleTestKeys.get(4));

        //move 2 up -> [2, 4, 3]
        cache.get(simpleTestKeys.get(2));

        //get 1 back -> [1, 2, 4]
        result = cache.get(simpleTestKeys.get(1));
        assertCacheState(cache, result, MAX_SIMPLE_CACHE_SIZE, 1);

        //each item in the test is only retrieved once except for 1
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(0));
        verify(simpleValueSupplier, times(2)).apply(simpleTestKeys.get(1));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(2));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(3));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(4));
    }

    @Test
    void when_mostRecentValueIsHit_ValuesAreReorderedCorrectly() {
        LruCache<Integer, String> cache = simpleCache.get();

        //fill cache [2, 1, 0]
        primeAndVerifySimpleCache(cache, MAX_SIMPLE_CACHE_SIZE);

        //get current mru (most recently used). Cache should stay the same: [2, 1, 0]
        cache.get(simpleTestKeys.get(2));

        //evict items 1,2 -> [3, 4, 2]
        cache.get(simpleTestKeys.get(3));
        cache.get(simpleTestKeys.get(4));

        //get 2, it should come from cache -> [2, 3, 4]
        cache.get(simpleTestKeys.get(2));

        //each value in the test is only retrieved once
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(0));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(1));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(2));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(3));
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(4));
    }

    @Test
    void when_leastRecentValueIsHit_ValuesAreReorderedCorrectly() {
        LruCache<Integer, String> cache = simpleCache.get();

        //fill cache [2, 1, 0]
        primeAndVerifySimpleCache(cache, MAX_SIMPLE_CACHE_SIZE);

        //get current lru (least recently used) and move up
        cache.get(simpleTestKeys.get(0));

        //evict items 1, 2 -> [3, 4, 0]
        cache.get(simpleTestKeys.get(3));
        cache.get(simpleTestKeys.get(4));

        //get 0, should return cached value -> [0, 3, 4]
        cache.get(simpleTestKeys.get(0));

        //get 1, should get from supplier
        cache.get(simpleTestKeys.get(1));

        //get 2, should get from supplier
        cache.get(simpleTestKeys.get(2));

        //1, 2 fell out of cache and were retrieved
        verify(simpleValueSupplier, times(1)).apply(simpleTestKeys.get(0));
        verify(simpleValueSupplier, times(2)).apply(simpleTestKeys.get(1));
        verify(simpleValueSupplier, times(2)).apply(simpleTestKeys.get(2));
    }

    @Test
    void when_cacheHasMiss_AndNoValueIsFound_ReturnsNull() {
        LruCache<Integer, String> cache = simpleCache.get();
        primeAndVerifySimpleCache(cache, 1);

        Integer keyMissingValue = 200;
        String value = cache.get(keyMissingValue);
        assertThat(value).isNull();
        cache.get(keyMissingValue);
        verify(simpleValueSupplier, times(1)).apply(keyMissingValue);
    }

    @ParameterizedTest
    @MethodSource("concurrencyTestValues")
    void when_multipleThreadsAreCallingCache_WorksAsExpected(Integer numThreads,
                                                             Integer numGetsPerThread,
                                                             boolean sleep,
                                                             Integer cacheSize) throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            Function<Integer, String> sleepySupplier = num -> {
                if (sleep) {
                    invokeSafely(() -> Thread.sleep(ThreadLocalRandom.current().nextInt(0, 5)));
                }
                return identitySupplier.apply(num);
            };
            LruCache<Integer, String> cache = LruCache.builder(sleepySupplier)
                                                      .maxSize(cacheSize)
                                                      .build();
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                results.add(executor.submit(() -> {
                    for (int j = 0; j < numGetsPerThread; j++) {
                        int key = ThreadLocalRandom.current().nextInt(cacheSize * 2);
                        String value = cache.get(key);
                        assertThat(value).isEqualTo(Integer.toString(key));
                    }
                }));
            }
            for (Future<?> result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static Stream<Arguments> concurrencyTestValues() {
        // numThreads, numGetsPerThreads, sleepDurationMillis, cacheSize
        return Stream.of(Arguments.of(1000, 5000, false, 5),
                         Arguments.of(1000, 5000, false, 50),
                         Arguments.of(100, 1000, true, 5)
                         );
    }

    private void assertCacheState(LruCache<Integer, String> cache, String result, int size, int index) {
        assertThat(cache.size()).isEqualTo(size);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(simpleTestValues.get(index));
    }

    private void primeAndVerifySimpleCache(LruCache<Integer, String> cache, int numEntries) {
        IntStream.range(0, numEntries).forEach(i -> {
            String result = cache.get(simpleTestKeys.get(i));
            assertThat(cache.size()).isEqualTo(i + 1);
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(simpleTestValues.get(i));
            verify(simpleValueSupplier).apply(simpleTestKeys.get(i));
        });
    }

    private static class SimpleValueSupplier implements Function<Integer, String> {

        List<String> values;

        SimpleValueSupplier(List<String> values) {
            this.values = values;
        }

        @Override
        public String apply(Integer key) {
            String value = null;
            try {
                value = values.get(key);
            } catch (Exception ignored) {

            }
            return value;
        }
    }
}
