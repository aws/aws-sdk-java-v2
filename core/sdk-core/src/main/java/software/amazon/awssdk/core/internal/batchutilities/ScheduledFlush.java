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

package software.amazon.awssdk.core.internal.batchutilities;

import java.util.concurrent.ScheduledFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class ScheduledFlush {
    private final ScheduledFuture<?> future;
    private final CancellableFlush cancellableFlush;

    public ScheduledFlush(CancellableFlush cancellableFlush, ScheduledFuture<?> future) {
        this.cancellableFlush = Validate.paramNotNull(cancellableFlush, "cancellableFlush");
        this.future = Validate.paramNotNull(future, "scheduledFuture");
    }

    public void cancel() {
        future.cancel(false);
        cancellableFlush.cancel();
    }

    public boolean hasExecuted() {
        return cancellableFlush.hasExecuted();
    }
}
