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
 * Verifies that when {@link SdkWarmUp#prime(Class...)} is called with a batch mixing an already-primed client and a new
 * one, only the new client is warmed. Uses dedicated clients that no other test primes.
 */
class SdkWarmUpPrimeSelectiveTest {

    private String savedRegionProperty;

    @BeforeEach
    void setUp() {
        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "warmup-selective-test");
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
    void prime_previouslyPrimedClientInNewBatch_warmsOnlyTheNewClient() {
        // Prime the sync client first.
        SdkWarmUp.prime(SelectiveSyncClient.class);
        assertThat(SelectiveWarmUpProvider.syncWarmCount()).isEqualTo(1);
        assertThat(SelectiveWarmUpProvider.asyncWarmCount()).isEqualTo(0);

        // Now prime a batch containing the already-primed sync client plus a new async client.
        SdkWarmUp.prime(SelectiveSyncClient.class, SelectiveAsyncClient.class);

        assertThat(SelectiveWarmUpProvider.syncWarmCount())
            .as("already-primed sync client is not warmed again")
            .isEqualTo(1);
        assertThat(SelectiveWarmUpProvider.asyncWarmCount())
            .as("the new async client is warmed once")
            .isEqualTo(1);
    }
}
