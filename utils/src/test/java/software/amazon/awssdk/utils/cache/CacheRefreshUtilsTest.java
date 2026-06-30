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

package software.amazon.awssdk.utils.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CacheRefreshUtils}.
 */
public class CacheRefreshUtilsTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    public void remainingLifetimeUnder20Minutes_returns5MinuteWindow() {
        // 19 minutes remaining
        Instant expiration = NOW.plus(Duration.ofMinutes(19));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetimeExactly0_returns5MinuteWindow() {
        // 0 minutes remaining (already expired)
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(NOW, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetime5Minutes_returns5MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(5));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetimeExactly20Minutes_returns15MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(20));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetime45Minutes_returns15MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(45));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetime89Minutes_returns15MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(89));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetimeExactly90Minutes_returns60MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(90));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    public void remainingLifetime6Hours_returns60MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofHours(6));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    public void remainingLifetime12Hours_returns60MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofHours(12));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    public void remainingLifetimeNegative_returns5MinuteWindow() {
        // Expiration is in the past
        Instant expiration = NOW.minus(Duration.ofMinutes(5));
        Duration window = CacheRefreshUtils.computeDynamicPrefetchWindow(expiration, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }
}
