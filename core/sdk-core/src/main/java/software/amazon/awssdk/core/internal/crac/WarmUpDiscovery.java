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

package software.amazon.awssdk.core.internal.crac;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * Shared best-effort {@link java.util.ServiceLoader} iteration for the CRaC warm-up paths.
 */
@SdkInternalApi
public final class WarmUpDiscovery {

    private static final Logger log = Logger.loggerFor(WarmUpDiscovery.class);

    private WarmUpDiscovery() {
    }

    /**
     * Applies {@code action} to every discovered element, skipping (and logging at warn) any element that fails to load or
     * whose action throws, so the rest still run. Logs at debug when nothing is discovered.
     */
    public static <T> void forEachDiscovered(Iterator<T> iterator, Consumer<T> action) {
        boolean discoveredAny = false;
        while (iterator.hasNext()) {
            T element;
            try {
                element = iterator.next();
            } catch (ServiceConfigurationError e) {
                // next() has already advanced past the bad element, so it is safe to continue to the next one.
                log.warn(() -> "Skipping a warm-up task that could not be loaded.", e);
                continue;
            }

            discoveredAny = true;
            T discovered = element;
            try {
                action.accept(discovered);
            } catch (RuntimeException | LinkageError e) {
                // LinkageError because a discovered element can fail to link (missing deps/native lib, failed static init),
                // which is an Error, not an Exception. Skip it to keep warm-up best-effort; fatal Errors still propagate.
                log.warn(() -> "Warm-up failed for " + discovered.getClass().getName() + " and was skipped.", e);
            }
        }

        if (!discoveredAny) {
            log.debug(() -> "No warm-up tasks were discovered on the classpath.");
        }
    }
}
