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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Tracks which service clients {@code SdkWarmUp.warmUp(Class...)} has already warmed, so each is warmed at most once per
 * JVM. State is per-instance, so it is unit-testable with fresh instances; production uses a single long-lived instance.
 */
@ThreadSafe
@SdkInternalApi
public final class WarmedClientRegistry {

    private final Set<String> warmed = ConcurrentHashMap.newKeySet();

    /**
     * Returns the not-yet-warmed subset of {@code clientClassNames}, in encounter order, ignoring nulls. Warm the
     * returned names, then pass them to {@link #markWarmed(Collection)}.
     */
    public Set<String> selectUnwarmed(Collection<String> clientClassNames) {
        Set<String> unwarmed = new LinkedHashSet<>();
        for (String name : clientClassNames) {
            if (name != null && !warmed.contains(name)) {
                unwarmed.add(name);
            }
        }
        return unwarmed;
    }

    /**
     * Records the given names as warmed. Call only after warming completes, so a failed run is retried by a later call.
     */
    public void markWarmed(Collection<String> clientClassNames) {
        warmed.addAll(clientClassNames);
    }
}
