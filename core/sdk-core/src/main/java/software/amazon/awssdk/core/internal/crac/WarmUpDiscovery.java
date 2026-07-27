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
 * Shared best-effort helpers for the CRaC warm-up paths: {@link java.util.ServiceLoader} iteration and running a
 * single warm-up task without letting its failure stop the others.
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
            runSafely(discovered.getClass().getName(), () -> action.accept(discovered));
        }

        if (!discoveredAny) {
            log.debug(() -> "No warm-up tasks were discovered on the classpath.");
        }
    }

    /**
     * Runs one warm-up {@code task}. Returns {@code true} if it completed. If it fails, logs at warn with
     * {@code description} and returns {@code false}, so a sibling task still runs.
     *
     * <p>Also catches {@link LinkageError}: a task can fail to link (missing dependency or native library) and that
     * is an {@link Error}, not an {@link Exception}. Other errors still propagate.
     */
    public static boolean runSafely(String description, Runnable task) {
        try {
            task.run();
            return true;
        } catch (RuntimeException | LinkageError e) {
            log.warn(() -> "Warm-up failed for " + description + " and was skipped.", e);
            return false;
        }
    }
}
