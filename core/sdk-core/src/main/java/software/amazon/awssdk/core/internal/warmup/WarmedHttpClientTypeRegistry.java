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

package software.amazon.awssdk.core.internal.warmup;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ClientType;

/**
 * Tracks whether each HTTP client type has been warmed, so {@code SdkWarmUp.warmUp(Class...)} warms each at most once
 * per JVM. Only {@link ClientType#SYNC} and {@link ClientType#ASYNC} are tracked.
 */
@ThreadSafe
@SdkInternalApi
public final class WarmedHttpClientTypeRegistry {

    private volatile boolean syncWarmed;
    private volatile boolean asyncWarmed;

    public boolean isWarmed(ClientType clientType) {
        if (clientType == ClientType.SYNC) {
            return syncWarmed;
        }
        if (clientType == ClientType.ASYNC) {
            return asyncWarmed;
        }
        return false;
    }

    /**
     * Mark only after warming completes, so a failed run is retried later.
     */
    public void markWarmed(ClientType clientType) {
        if (clientType == ClientType.SYNC) {
            syncWarmed = true;
        } else if (clientType == ClientType.ASYNC) {
            asyncWarmed = true;
        }
    }
}
