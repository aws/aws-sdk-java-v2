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
package software.amazon.awssdk.regions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Test;

public class RegionTest {

    @Test(expected = NullPointerException.class)
    public void of_ThrowsNullPointerException_WhenNullValue() {
        Region.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void of_ThrowsIllegalArgumentException_WhenEmptyString() {
        Region.of("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void of_ThrowsIllegalArgumentException_WhenBlankString() {
        Region.of(" ");
    }

    @Test
    public void of_ReturnsRegion_WhenValidString() {
        Region region = Region.of("us-east-1");
        assertThat(region.id()).isEqualTo("us-east-1");
        assertSame(Region.US_EAST_1, region);
    }

    @Test
    public void sameValueSameClassAreSameInstance() {
        Region first = Region.of("first");
        Region alsoFirst = Region.of("first");

        assertThat(first).isSameAs(alsoFirst);
    }

    @Test
    public void canBeUsedAsKeysInMap() {
        Map<Region, String> someMap = new HashMap<>();
        someMap.put(Region.of("key"), "A Value");

        assertThat(someMap.get(Region.of("key"))).isEqualTo("A Value");
    }

    @Test
    public void idIsUrlEncoded() {
        Region region = Region.of("http://my-host.com/?");
        assertThat(region.id()).isEqualTo("http%3A%2F%2Fmy-host.com%2F%3F");
    }

    @Test
    public void globalRegionIsRecognized() {
        Region globalRegion = Region.of("aws-global");
        assertThat(globalRegion.id()).isEqualTo("aws-global");
        assertSame(Region.AWS_GLOBAL, globalRegion);
    }

    @Test
    public void multipleGlobalRegionsAreSupported() {
        Region awsGlobal = Region.of("aws-global");
        Region s3Global = Region.of("s3-global");

        assertThat(awsGlobal.id()).isEqualTo("aws-global");
        assertThat(s3Global.id()).isEqualTo("s3-global");

        assertSame(Region.of("aws-global"), awsGlobal);
        assertSame(Region.of("s3-global"), s3Global);
    }

    @Test
    public void allPartitionGlobalRegionsAreRecognized() {
        assertThat(Region.of("aws-global").id()).isEqualTo("aws-global");
        assertThat(Region.of("aws-cn-global").id()).isEqualTo("aws-cn-global");
        assertThat(Region.of("aws-us-gov-global").id()).isEqualTo("aws-us-gov-global");
        assertThat(Region.of("aws-iso-global").id()).isEqualTo("aws-iso-global");
        assertThat(Region.of("aws-iso-b-global").id()).isEqualTo("aws-iso-b-global");
        assertThat(Region.of("aws-iso-f-global").id()).isEqualTo("aws-iso-f-global");
    }

    @Test
    public void unreferencedCustomRegionIsCollectable() throws Exception {
        String id = "osrs-weak-" + UUID.randomUUID();
        WeakReference<Region> ref = createUnreferencedRegion(id);

        gcUntil(() -> ref.get() == null);
        assertThat(ref.get()).isNull();

        // Trigger the expunge step so the cleared entry is removed from the cache.
        gcUntil(() -> {
            Region.of("osrs-weak-trigger");
            return !regionCacheValues().containsKey(id);
        });
        assertThat(regionCacheValues()).doesNotContainKey(id);
    }

    @Test
    public void manyDistinctRegionsDoNotAccumulate() throws Exception {
        int count = 50_000;
        String prefix = "osrs-many-" + UUID.randomUUID() + "-";
        for (int i = 0; i < count; i++) {
            Region.of(prefix + i);
        }

        int limit = Region.regions().size() + 1000;
        gcUntil(() -> {
            Region.of("osrs-many-trigger");
            return regionCacheValues().size() < limit;
        });
        assertThat(regionCacheValues().size()).isLessThan(limit);
    }

    @Test
    public void liveCustomRegionKeepsIdentity() throws Exception {
        String id = "osrs-live-" + UUID.randomUUID();
        Region region = Region.of(id);

        for (int i = 0; i < 5; i++) {
            System.gc();
            byte[] garbage = new byte[1 << 20];
            Thread.sleep(50);
        }

        assertThat(Region.of(id)).isSameAs(region);
        assertThat(regionCacheValues()).containsKey(id);
    }

    @Test
    public void concurrentOfCallsReturnSameInstance() throws Exception {
        String id = "osrs-race-" + UUID.randomUUID();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Region>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return Region.of(id);
                }));
            }
            start.countDown();

            List<Region> results = new ArrayList<>();
            for (Future<Region> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            for (Region result : results) {
                assertThat(result).isSameAs(results.get(0));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static WeakReference<Region> createUnreferencedRegion(String id) {
        return new WeakReference<>(Region.of(id));
    }

    private static void gcUntil(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 50 && !condition.getAsBoolean(); i++) {
            System.gc();
            byte[] garbage = new byte[1 << 20];
            Thread.sleep(50);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> regionCacheValues() {
        try {
            Field values = Class.forName("software.amazon.awssdk.regions.Region$RegionCache").getDeclaredField("VALUES");
            values.setAccessible(true);
            return (Map<String, ?>) values.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
