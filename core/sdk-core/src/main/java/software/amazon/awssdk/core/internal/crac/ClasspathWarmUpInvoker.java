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
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * {@link WarmUpInvoker} implementation that uses {@link ServiceLoader} to find {@link SdkWarmUpProvider}
 * implementations on the classpath and invokes {@code warmUp()} on every one of them.
 */
@SdkInternalApi
public final class ClasspathWarmUpInvoker implements WarmUpInvoker {

    private static final Logger log = Logger.loggerFor(ClasspathWarmUpInvoker.class);

    private final WarmUpServiceLoader serviceLoader;

    @SdkTestInternalApi
    ClasspathWarmUpInvoker(WarmUpServiceLoader serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    @Override
    public void invokeAll() {
        Iterator<SdkWarmUpProvider> iterator = serviceLoader.loadProviders();
        boolean invokedAny = false;

        while (iterator.hasNext()) {
            SdkWarmUpProvider provider;
            try {
                provider = iterator.next();
            } catch (ServiceConfigurationError e) {
                // next() has already advanced past the bad provider, so it is safe to continue to the next one.
                log.warn(() -> "Skipping an SdkWarmUpProvider that could not be loaded.", e);
                continue;
            }

            invokedAny = true;
            try {
                provider.warmUp();
            } catch (RuntimeException e) {
                log.warn(() -> "An SdkWarmUpProvider failed during warmUp() and was skipped.", e);
            }
        }

        if (!invokedAny) {
            log.debug(() -> "No SdkWarmUpProvider implementations were discovered on the classpath.");
        }
    }

    /**
     * @return ClasspathWarmUpInvoker that discovers {@link SdkWarmUpProvider}s from the classpath.
     */
    public static WarmUpInvoker create() {
        return new ClasspathWarmUpInvoker(WarmUpServiceLoader.INSTANCE);
    }
}
