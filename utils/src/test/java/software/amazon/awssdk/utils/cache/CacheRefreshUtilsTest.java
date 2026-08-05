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
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetimeExactly0_returns5MinuteWindow() {
        // 0 minutes remaining (already expired)
        Duration window = CacheRefreshUtils.computePrefetchWindow(NOW, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    /**
     * AWS credential services do not issue sessions shorter than 15 minutes, so the specification's smallest window can
     * exceed the lifetime only when the credential source is not an AWS service (or the host clock is skewed). Callers that
     * have to tolerate that use {@link CacheRefreshUtils#computePrefetchWindowForArbitraryLifetime}.
     */
    @Test
    public void remainingLifetimeShorterThanSmallestWindow_stillReturns5MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(3));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetimeExactly10Minutes_returns5MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(10));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetimeExactly20Minutes_returns5MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(20));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void remainingLifetimeJustOver20Minutes_returns15MinuteWindow() {
        // 21 minutes — above the 20 minute boundary, enters the medium tier
        Instant expiration = NOW.plus(Duration.ofMinutes(21));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetimeSecondsOver20Minutes_returns15MinuteWindow() {
        // Boundaries are not truncated to whole minutes.
        Instant expiration = NOW.plus(Duration.ofMinutes(20)).plusSeconds(1);
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetimeMillisOver20Minutes_returns15MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(20)).plusMillis(1);
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetime45Minutes_returns15MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(45));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetime89Minutes_returns15MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(89));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    public void remainingLifetimeExactly90Minutes_returns60MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(90));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    public void remainingLifetime6Hours_returns60MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofHours(6));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    public void remainingLifetime12Hours_returns60MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofHours(12));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    public void remainingLifetimeNegative_returns5MinuteWindow() {
        // Expiration is in the past
        Instant expiration = NOW.minus(Duration.ofMinutes(5));
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void explicitPrefetchTime_returnsExplicitValue() {
        Instant expiration = NOW.plus(Duration.ofHours(6));
        Duration explicitPrefetch = Duration.ofMinutes(30);
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, explicitPrefetch, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    public void explicitPrefetchTime_ignoresRemainingLifetime() {
        // Even with short remaining lifetime, explicit value is used
        Instant expiration = NOW.plus(Duration.ofMinutes(10));
        Duration explicitPrefetch = Duration.ofMinutes(60);
        Duration window = CacheRefreshUtils.computePrefetchWindow(expiration, explicitPrefetch, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(60));
    }

    // computePrefetchWindowForArbitraryLifetime: adds the halved tier for credential sources that may vend credentials
    // shorter-lived than the smallest specification window.

    @Test
    public void arbitraryLifetime_remainingLifetime3Minutes_returnsHalfOfLifetime() {
        Instant expiration = NOW.plus(Duration.ofMinutes(3));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    public void arbitraryLifetime_remainingLifetime5Minutes_returnsHalfOfLifetime() {
        Instant expiration = NOW.plus(Duration.ofMinutes(5));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofSeconds(150));
    }

    @Test
    public void arbitraryLifetime_remainingLifetimeExactly10Minutes_returnsHalfOfLifetime() {
        // The halved tier joins the 5 minute tier continuously here.
        Instant expiration = NOW.plus(Duration.ofMinutes(10));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void arbitraryLifetime_remainingLifetimeJustOver10Minutes_returns5MinuteWindow() {
        Instant expiration = NOW.plus(Duration.ofMinutes(10)).plusSeconds(1);
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void arbitraryLifetime_remainingLifetime2Minutes_returnsMandatoryWindowFloor() {
        // Half of 2 minutes is exactly the 1 minute floor.
        Instant expiration = NOW.plus(Duration.ofMinutes(2));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    public void arbitraryLifetime_remainingLifetimeUnder2Minutes_isFlooredAtMandatoryWindow() {
        // Half of 90 seconds is 45 seconds, which is narrower than the 1 minute mandatory refresh window.
        Instant expiration = NOW.plus(Duration.ofSeconds(90));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    public void arbitraryLifetime_remainingLifetimeNegative_returns5MinuteWindow() {
        Instant expiration = NOW.minus(Duration.ofMinutes(5));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void arbitraryLifetime_explicitPrefetchTime_returnsExplicitValue() {
        Instant expiration = NOW.plus(Duration.ofMinutes(3));
        Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, Duration.ofMinutes(2), NOW);
        assertThat(window).isEqualTo(Duration.ofMinutes(2));
    }

    /**
     * The halved tier is the only difference between the two methods. Above 10 minutes they must agree, so that the
     * specification's windows apply to every credential long-lived enough to have one.
     */
    @Test
    public void arbitraryLifetime_above10Minutes_matchesSpecificationWindows() {
        for (long lifetimeSeconds = 601; lifetimeSeconds <= 12 * 60 * 60; lifetimeSeconds++) {
            Instant expiration = NOW.plus(Duration.ofSeconds(lifetimeSeconds));
            assertThat(CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(expiration, null, NOW))
                .as("lifetime %s seconds", lifetimeSeconds)
                .isEqualTo(CacheRefreshUtils.computePrefetchWindow(expiration, null, NOW));
        }
    }

    /**
     * The advisory refresh window must land strictly inside the credential's lifetime, otherwise the credential is inside its
     * advisory window the moment it is issued and every subsequent request contacts the credential source. The 1 minute floor
     * means this can only be guaranteed for credentials that outlive the mandatory refresh window.
     */
    @Test
    public void arbitraryLifetime_anyLifetimeLongerThanMandatoryWindow_windowIsShorterThanLifetime() {
        for (long lifetimeSeconds = 61; lifetimeSeconds <= 12 * 60 * 60; lifetimeSeconds++) {
            Duration lifetime = Duration.ofSeconds(lifetimeSeconds);
            Duration window = CacheRefreshUtils.computePrefetchWindowForArbitraryLifetime(NOW.plus(lifetime), null, NOW);
            assertThat(window).as("lifetime %s", lifetime).isLessThan(lifetime);
        }
    }
}
