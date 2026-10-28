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

package software.amazon.awssdk.auth.credentials;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the prefetch (advisory refresh) time that {@link ContainerCredentialsProvider} computes when no prefetch time is
 * configured.
 */
class ContainerCredentialsProviderPrefetchTimeTest {

    /**
     * Without a configured prefetch time, the provider refreshes at a random time between a lower and an upper bound after
     * the fetch. The bounds depend on the remaining lifetime L of the credentials and the advisory refresh window W for that
     * lifetime: hi = L - W and lo = min(max(min(1 h, L - 15 min), 0), 3/4 * hi). If L - W is zero or negative, the refresh is
     * at the fetch.
     */
    @ParameterizedTest(name = "lifetime={0}s")
    @CsvSource({
        // lifetime (s), lo (ms), hi (ms)
        "21600, 3600000, 18000000", // 6 h: 1 h to 5 h
        "7200,  2700000,  3600000", // 2 h: hi = L - W = 60 min, lo = 3/4 * hi = 45 min
        "5400,  1350000,  1800000", // 90 min (W = 60 min): hi = 30 min, lo = 3/4 * hi = 22.5 min
        "5340,  3330000,  4440000", // 89 min (W = 15 min): hi = 74 min, lo = 3/4 * hi = 55.5 min
        "3600,  2025000,  2700000", // 1 h: hi = 45 min, lo = 3/4 * hi = 33.75 min
        "1260,   270000,   360000", // 21 min (W = 15 min): hi = 6 min, lo = 3/4 * hi = 4.5 min
        "1200,   300000,   900000", // 20 min (W = 5 min): hi = 15 min, lo = L - 15 min = 5 min
        "600,         0,   300000", // 10 min: hi = 5 min, lo = 0 because L - 15 min is negative
        "330,         0,    30000", // 5 min 30 s: hi = 30 s, lo = 0
        "300,         0,        0", // 5 min: L - W = 0, so at the fetch
        "180,         0,        0"  // 3 min: L - W < 0, so at the fetch
    })
    void defaultPrefetchTime_lowestAndHighestRandomFraction_returnsRangeBounds(long lifetimeSeconds, long loMillis,
                                                                               long hiMillis) {
        Instant now = Instant.now().truncatedTo(SECONDS);
        Instant expiration = now.plusSeconds(lifetimeSeconds);

        assertThat(ContainerCredentialsProvider.defaultPrefetchTime(now, expiration, 0)).isEqualTo(now.plusMillis(loMillis));
        assertThat(ContainerCredentialsProvider.defaultPrefetchTime(now, expiration, 1)).isEqualTo(now.plusMillis(hiMillis));

        long seed = System.nanoTime();
        Random random = new Random(seed);
        for (int i = 0; i < 10_000; i++) {
            assertThat(ContainerCredentialsProvider.defaultPrefetchTime(now, expiration, random.nextDouble()))
                .as("seed %s", seed)
                .isBetween(now.plusMillis(loMillis), now.plusMillis(hiMillis));
        }
    }

    /**
     * For any lifetime, the default prefetch time is no sooner than the fetch and no later than the start of the advisory
     * refresh window, W before expiration. If the credentials are already inside their advisory refresh window, the prefetch
     * time is the fetch.
     */
    @Test
    void defaultPrefetchTime_anyLifetime_isNoLaterThanAdvisoryWindowAndNoSoonerThanFetch() {
        Instant now = Instant.now().truncatedTo(SECONDS);
        long seed = System.nanoTime();
        Random random = new Random(seed);
        for (int i = 0; i < 100_000; i++) {
            Duration lifetime = Duration.ofMillis(1 + (long) (random.nextDouble() * Duration.ofHours(7).toMillis()));
            Instant expiration = now.plus(lifetime);

            Instant prefetch = ContainerCredentialsProvider.defaultPrefetchTime(now, expiration, random.nextDouble());

            String description = String.format("seed %s, lifetime %s", seed, lifetime);
            Duration window = advisoryRefreshWindow(lifetime);
            assertThat(prefetch).as(description).isAfterOrEqualTo(now);
            if (lifetime.compareTo(window) <= 0) {
                assertThat(prefetch).as(description).isEqualTo(now);
            } else {
                assertThat(prefetch).as(description).isBeforeOrEqualTo(expiration.minus(window));
            }
        }
    }

    /**
     * Credentials without an expiration are refreshed at a random time 60 to 65 minutes after the fetch.
     */
    @Test
    void noExpirationPrefetchTime_anyRandomFraction_isSixtyToSixtyFiveMinutesAfterFetch() {
        Instant now = Instant.now().truncatedTo(SECONDS);

        assertThat(ContainerCredentialsProvider.noExpirationPrefetchTime(now, 0)).isEqualTo(now.plus(60, MINUTES));
        assertThat(ContainerCredentialsProvider.noExpirationPrefetchTime(now, 1)).isEqualTo(now.plus(65, MINUTES));

        long seed = System.nanoTime();
        Random random = new Random(seed);
        for (int i = 0; i < 10_000; i++) {
            assertThat(ContainerCredentialsProvider.noExpirationPrefetchTime(now, random.nextDouble()))
                .as("seed %s", seed)
                .isBetween(now.plus(60, MINUTES), now.plus(65, MINUTES));
        }
    }

    /**
     * The advisory refresh window table of the credential cache specification.
     */
    private static Duration advisoryRefreshWindow(Duration lifetime) {
        if (lifetime.compareTo(Duration.ofMinutes(20)) <= 0) {
            return Duration.ofMinutes(5);
        }
        return lifetime.compareTo(Duration.ofMinutes(90)) < 0 ? Duration.ofMinutes(15) : Duration.ofMinutes(60);
    }
}
