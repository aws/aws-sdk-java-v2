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
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.crac.SdkWarmUp;

/**
 * Verifies a failed {@link SdkWarmUp#prime(Class...)} warm-up is retried on the next call.
 */
class SdkWarmUpPrimeRetryTest {

    private String savedRegionProperty;

    @BeforeEach
    void setUp() {
        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "warmup-retry-test");
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
    void prime_afterWarmUpFailure_retriesOnNextCallThenStops() {
        // First call: the provider throws, so the client is not recorded as primed.
        assertThatCode(() -> SdkWarmUp.prime(RetrySyncClient.class)).doesNotThrowAnyException();
        assertThat(RetryWarmUpProvider.syncAttemptCount()).as("first call attempts the warm").isEqualTo(1);
        assertThat(RetryWarmUpProvider.syncSuccessCount()).as("first call fails").isEqualTo(0);

        // Second call: retried; this time it succeeds.
        SdkWarmUp.prime(RetrySyncClient.class);
        assertThat(RetryWarmUpProvider.syncAttemptCount()).as("failure is retried").isEqualTo(2);
        assertThat(RetryWarmUpProvider.syncSuccessCount()).as("retry succeeds").isEqualTo(1);

        // Third call: already primed, no further attempt.
        SdkWarmUp.prime(RetrySyncClient.class);
        assertThat(RetryWarmUpProvider.syncAttemptCount()).as("successful warm is not repeated").isEqualTo(2);
    }
}
