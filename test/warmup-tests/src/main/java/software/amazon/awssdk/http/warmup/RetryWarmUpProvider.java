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

import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Test-only provider whose first {@code warmUpClient(SYNC)} call throws; later calls succeed.
 */
public final class RetryWarmUpProvider implements SdkWarmUpProvider {

    private static final AtomicInteger SYNC_ATTEMPTS = new AtomicInteger();
    private static final AtomicInteger SYNC_SUCCESSES = new AtomicInteger();

    public static int syncAttemptCount() {
        return SYNC_ATTEMPTS.get();
    }

    public static int syncSuccessCount() {
        return SYNC_SUCCESSES.get();
    }

    @Override
    public void warmUp() {
    }

    @Override
    public String syncClientClassName() {
        return "software.amazon.awssdk.http.warmup.RetrySyncClient";
    }

    @Override
    public String asyncClientClassName() {
        return null;
    }

    @Override
    public void warmUpClient(ClientType clientType) {
        if (clientType != ClientType.SYNC) {
            return;
        }
        if (SYNC_ATTEMPTS.incrementAndGet() == 1) {
            throw new RuntimeException("Simulated transient warm-up failure (test)");
        }
        SYNC_SUCCESSES.incrementAndGet();
    }
}
