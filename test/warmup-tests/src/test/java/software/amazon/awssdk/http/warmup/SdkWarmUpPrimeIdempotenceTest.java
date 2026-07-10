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

package software.amazon.awssdk.http.warmup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.crac.SdkWarmUp;

/**
 * Verifies {@link SdkWarmUp#prime(Class...)} warms a client at most once per JVM. Uses a dedicated
 * {@link IdempotenceSyncClient} that no other test primes, so the "first warms, repeat no-ops" transition is observable
 * regardless of execution order or JVM sharing.
 */
class SdkWarmUpPrimeIdempotenceTest {

    private String savedRegionProperty;

    @BeforeEach
    void setUp() {
        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "warmup-idempotence-test");
    }

    @AfterEach
    void tearDown() {
        if (savedRegionProperty != null) {
            System.setProperty("aws.region", savedRegionProperty);
        } else {
            System.clearProperty("aws.region");
        }
    }

    @Test
    void prime_sameClientTwice_warmsProviderExactlyOnce() {
        SdkWarmUp.prime(IdempotenceSyncClient.class);
        assertThat(IdempotenceWarmUpProvider.syncWarmCount())
            .as("first prime warms the sync transport once")
            .isEqualTo(1);

        // Second call for the same client must be a no-op: it is already recorded as primed.
        SdkWarmUp.prime(IdempotenceSyncClient.class);
        assertThat(IdempotenceWarmUpProvider.syncWarmCount())
            .as("second prime of the same client does not warm again")
            .isEqualTo(1);
    }
}
