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

import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A {@link CachedSupplier.PrefetchStrategy} that will have one caller at a time block to update the value.
 *
 * Multiple calls to {@link #prefetch(Runnable)} will result in only one caller actually performing the update, with the others
 * immediately returning.
 */
@SdkProtectedApi
public class OneCallerBlocks implements CachedSupplier.PrefetchStrategy {
    /**
     * Whether we are currently refreshing the supplier. This is used to make sure only one caller is blocking at a time.
     */
    private final AtomicBoolean currentlyRefreshing = new AtomicBoolean(false);

    @Override
    public void prefetch(Runnable valueUpdater) {
        if (currentlyRefreshing.compareAndSet(false, true)) {
            try {
                valueUpdater.run();
            } finally {
                currentlyRefreshing.set(false);
            }
        }
    }
}
