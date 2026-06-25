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

import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * {@link WarmUpInvoker} implementation that uses {@link ServiceLoader} to find {@link SdkWarmUpProvider}
 * implementations on the classpath and invokes {@code warmUp()} on every one of them.
 */
@SdkInternalApi
public final class ClasspathWarmUpInvoker implements WarmUpInvoker {

    private final WarmUpServiceLoader serviceLoader;

    @SdkTestInternalApi
    ClasspathWarmUpInvoker(WarmUpServiceLoader serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    @Override
    public void invokeAll() {
        WarmUpDiscovery.forEachDiscovered(serviceLoader.loadProviders(), SdkWarmUpProvider::warmUp);
    }

    /**
     * @return ClasspathWarmUpInvoker that discovers {@link SdkWarmUpProvider}s from the classpath.
     */
    public static WarmUpInvoker create() {
        return new ClasspathWarmUpInvoker(WarmUpServiceLoader.INSTANCE);
    }
}
