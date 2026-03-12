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

package software.amazon.awssdk.utils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;

public class AttributeMapTest {

    private static final AttributeMap.Key<String> STRING_KEY = new AttributeMap.Key<String>(String.class) {
    };

    private static final AttributeMap.Key<Integer> INTEGER_KEY = new AttributeMap.Key<Integer>(Integer.class) {
    };

    private static final AttributeMap.Key<AutoCloseable> CLOSEABLE_KEY = new AttributeMap.Key<AutoCloseable>(AutoCloseable.class) {
    };

    private static final AttributeMap.Key<ExecutorService> EXECUTOR_SERVICE_KEY =
            new AttributeMap.Key<ExecutorService>(ExecutorService.class) {
    };

    @Test
    public void copyCreatesNewOptionsObject() {
        AttributeMap orig = AttributeMap.builder()
                                        .put(STRING_KEY, "foo")
                                        .build();
        assertTrue(orig != orig.copy());
        assertThat(orig).isEqualTo(orig.copy());
        assertThat(orig.get(STRING_KEY)).isEqualTo(orig.copy().get(STRING_KEY));
    }

    @Test
    public void mergeTreatsThisObjectWithHigherPrecedence() {
        AttributeMap orig = AttributeMap.builder()
                                        .put(STRING_KEY, "foo")
                                        .build();
        AttributeMap merged = orig.merge(AttributeMap.builder()
                                                     .put(STRING_KEY, "bar")
                                                     .put(INTEGER_KEY, 42)
                                                     .build());
        assertThat(merged.containsKey(STRING_KEY)).isTrue();
        assertThat(merged.get(STRING_KEY)).isEqualTo("foo");
        // Integer key is not in 'this' object so it should be merged in from the lower precedence
        assertThat(merged.get(INTEGER_KEY)).isEqualTo(42);
    }

    /**
     * Options are optional.
     */
    @Test
    public void mergeWithOptionNotPresentInBoth_DoesNotThrow() {
        AttributeMap orig = AttributeMap.builder()
                                        .put(STRING_KEY, "foo")
                                        .build();
        AttributeMap merged = orig.merge(AttributeMap.builder()
                                                     .put(STRING_KEY, "bar")
                                                     .build());
        assertThat(merged.get(INTEGER_KEY)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAll_ThrowsRuntimeExceptionWhenTypesMismatched() {
        Map<AttributeMap.Key<?>, Object> attributes = new HashMap<>();
        attributes.put(STRING_KEY, 42);
        AttributeMap.builder()
                    .putAll(attributes)
                    .build();
    }

    @Test
    public void close_closesAll() {
        SdkAutoCloseable closeable = mock(SdkAutoCloseable.class);
        ExecutorService executor = mock(ExecutorService.class);

        AttributeMap.builder()
                    .put(CLOSEABLE_KEY, closeable)
                    .put(EXECUTOR_SERVICE_KEY, executor)
                    .build()
                    .close();

        verify(closeable).close();
        verify(executor).shutdown();
    }

    @Test
    public void lazyAttributes_resolvedWhenRead() {
        AttributeMap map = mapWithLazyString();
        assertThat(map.get(STRING_KEY)).isEqualTo("5");
    }

    @Test
    public void lazyAttributesBuilder_resolvedWhenRead() {
        AttributeMap.Builder map = mapBuilderWithLazyString();
        assertThat(map.get(STRING_KEY)).isEqualTo("5");

        map.put(INTEGER_KEY, 6);
        assertThat(map.get(STRING_KEY)).isEqualTo("6");
    }

    @Test
    public void lazyAttributes_resultCached() {
        AttributeMap.Builder map = mapBuilderWithLazyString();
        String originalGet = map.get(STRING_KEY);
        assertThat(map.get(STRING_KEY)).isSameAs(originalGet);

        map.put(CLOSEABLE_KEY, null);
        assertThat(map.get(STRING_KEY)).isSameAs(originalGet);
    }

    @Test
    public void lazyAttributesBuilder_resolvedWhenBuilt() {
        Runnable lazyRead = mock(Runnable.class);
        AttributeMap.Builder map = mapBuilderWithLazyString(lazyRead);
        verify(lazyRead, never()).run();

        map.build();
        verify(lazyRead, Mockito.times(1)).run();
    }

    @Test
    public void lazyAttributes_notReResolvedAfterToBuilderBuild() {
        Runnable lazyRead = mock(Runnable.class);
        AttributeMap.Builder map = mapBuilderWithLazyString(lazyRead);
        verify(lazyRead, never()).run();

        AttributeMap builtMap = map.build();
        verify(lazyRead, Mockito.times(1)).run();

        builtMap.toBuilder().build().toBuilder().build();
        verify(lazyRead, Mockito.times(1)).run();
    }

    @Test
    public void lazyAttributes_canUpdateTheMap_andBeUpdatedWithPutLazyIfAbsent() {
        AttributeMap.Builder map = AttributeMap.builder();
        map.putLazyIfAbsent(STRING_KEY, c -> {
            // Force a modification to the underlying map. We wouldn't usually do this so explicitly, but
            // this can happen internally within AttributeMap when resolving uncached lazy values,
            // so it needs to be possible.
            map.put(INTEGER_KEY, 0);
            return "string";
        });
        map.putLazyIfAbsent(STRING_KEY, c -> "string"); // Force the value to be resolved within the putLazyIfAbsent
        assertThat(map.get(STRING_KEY)).isEqualTo("string");
    }

    @Test
    public void changesInBuilder_doNotAffectBuiltMap() {
        AttributeMap.Builder builder = mapBuilderWithLazyString();
        AttributeMap map = builder.build();

        builder.put(INTEGER_KEY, 6);
        assertThat(builder.get(INTEGER_KEY)).isEqualTo(6);
        assertThat(builder.get(STRING_KEY)).isEqualTo("6");

        assertThat(map.get(INTEGER_KEY)).isEqualTo(5);
        assertThat(map.get(STRING_KEY)).isEqualTo("5");
    }

    @Test
    public void changesInToBuilder_doNotAffectBuiltMap() {
        AttributeMap map = mapWithLazyString();
        AttributeMap.Builder builder = map.toBuilder();

        builder.put(INTEGER_KEY, 6);
        assertThat(builder.get(INTEGER_KEY)).isEqualTo(6);
        assertThat(builder.get(STRING_KEY)).isEqualTo("6");

        assertThat(map.get(INTEGER_KEY)).isEqualTo(5);
        assertThat(map.get(STRING_KEY)).isEqualTo("5");
    }

    @Test
    public void close_ExecutorDoesNotDeadlockOnClose() throws Exception {
        SdkAutoCloseable closeable = mock(SdkAutoCloseable.class);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        AttributeMap attributeMap = AttributeMap.builder()
                                                .put(CLOSEABLE_KEY, closeable)
                                                .put(EXECUTOR_SERVICE_KEY, executor)
                                                .build();

        // Previously, running AttributeMap#close from a thread managed by the ExecutorService
        // that's stored in that AttributeMap instance would result in a deadlock, where this
        // invocation would time out. This verifies that that scenario no longer happens.
        CompletableFuture.runAsync(attributeMap::close, executor).get(5L, TimeUnit.SECONDS);

        verify(closeable).close();
        assertThat(executor.isShutdown()).isTrue();
    }

    /**
     * This tests that the {@link ExecutorService} which as of Java 21 implements the {@link AutoCloseable}
     * interface, doesn't have its {@link AutoCloseable#close()} method called, but instead the expected
     * {@link ExecutorService#shutdown()} method is.
     *
     * This test scenario can be removed when the SDK upgrades its minimum supported version to Java 21,
     * whereupon this scenario will be handled by {@link AttributeMapTest#close_closesAll}.
     */
    @Test
    public void close_shutsDownExecutorService() throws Exception {
        SdkAutoCloseable closeable = mock(SdkAutoCloseable.class);
        CloseableExecutorService executor = mock(CloseableExecutorService.class);

        AttributeMap.builder()
                    .put(CLOSEABLE_KEY, closeable)
                    .put(EXECUTOR_SERVICE_KEY, executor)
                    .build()
                    .close();

        verify(closeable).close();
        verify(executor, never()).close();
        verify(executor).shutdown();
    }

    /**
     * Simulates the API contract of the ExecutorService as of Java 21, where it extends the
     * {@link AutoCloseable} interface and is susceptible to being closed by {@link AttributeMap#close()}.
     */
    private interface CloseableExecutorService extends ExecutorService, AutoCloseable {}

    private static AttributeMap mapWithLazyString() {
        return mapBuilderWithLazyString().build();
    }

    private static AttributeMap.Builder mapBuilderWithLazyString() {
        return mapBuilderWithLazyString(() -> {});
    }

    private static AttributeMap.Builder mapBuilderWithLazyString(Runnable runOnLazyRead) {
        return AttributeMap.builder()
                           .putLazy(STRING_KEY, c -> {
                               runOnLazyRead.run();
                               return Integer.toString(c.get(INTEGER_KEY));
                           })
                           .put(INTEGER_KEY, 5);
    }
}
