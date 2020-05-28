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

package software.amazon.awssdk.http.nio.netty.internal.http2;

import io.netty.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Tracking the status after sending out the PING frame
 */
@SdkInternalApi
public final class PingTracker {

    private final Supplier<ScheduledFuture<?>> timerFutureSupplier;
    private ScheduledFuture<?> pingTimerFuture;

    PingTracker(Supplier<ScheduledFuture<?>> timerFutureSupplier) {
        this.timerFutureSupplier = timerFutureSupplier;
    }

    public void start() {
        pingTimerFuture = timerFutureSupplier.get();
    }

    public void cancel() {
        if (pingTimerFuture != null) {
            pingTimerFuture.cancel(false);
        }
    }
}
