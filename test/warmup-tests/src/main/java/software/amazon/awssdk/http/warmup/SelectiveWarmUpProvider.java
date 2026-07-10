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
 * Test-only {@link SdkWarmUpProvider} for {@link SdkWarmUpPrimeSelectiveTest}. Matches {@link SelectiveSyncClient} and
 * {@link SelectiveAsyncClient} and counts warms per transport; performs no I/O.
 */
public final class SelectiveWarmUpProvider implements SdkWarmUpProvider {

    private static final AtomicInteger SYNC_WARMS = new AtomicInteger();
    private static final AtomicInteger ASYNC_WARMS = new AtomicInteger();

    public static int syncWarmCount() {
        return SYNC_WARMS.get();
    }

    public static int asyncWarmCount() {
        return ASYNC_WARMS.get();
    }

    @Override
    public void warmUp() {
    }

    @Override
    public String syncClientClassName() {
        return "software.amazon.awssdk.http.warmup.SelectiveSyncClient";
    }

    @Override
    public String asyncClientClassName() {
        return "software.amazon.awssdk.http.warmup.SelectiveAsyncClient";
    }

    @Override
    public void warmUpClient(ClientType clientType) {
        if (clientType == ClientType.SYNC) {
            SYNC_WARMS.incrementAndGet();
        } else if (clientType == ClientType.ASYNC) {
            ASYNC_WARMS.incrementAndGet();
        }
    }
}
