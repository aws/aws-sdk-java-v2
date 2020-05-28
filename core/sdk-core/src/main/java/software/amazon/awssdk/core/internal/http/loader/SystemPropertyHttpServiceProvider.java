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

package software.amazon.awssdk.core.internal.http.loader;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * Attempts to load the default implementation from the system property {@link SdkSystemSetting#SYNC_HTTP_SERVICE_IMPL}. The
 * property value should be the fully qualified class name of the factory to use.
 */
@SdkInternalApi
final class SystemPropertyHttpServiceProvider<T> implements SdkHttpServiceProvider<T> {

    private final SystemSetting implSetting;
    private final Class<T> serviceClass;

    /**
     * @param implSetting  {@link SystemSetting} to access the system property that has the implementation FQCN.
     * @param serviceClass Service type being loaded.
     */
    private SystemPropertyHttpServiceProvider(SystemSetting implSetting, Class<T> serviceClass) {
        this.implSetting = implSetting;
        this.serviceClass = serviceClass;
    }

    @Override
    public Optional<T> loadService() {
        return implSetting
                .getStringValue()
                .map(this::createServiceFromProperty);
    }

    private T createServiceFromProperty(String httpImplFqcn) {
        try {
            return serviceClass.cast(Class.forName(httpImplFqcn).newInstance());
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message(String.format("Unable to load the HTTP factory implementation from the "
                                             + "%s system property. Ensure the class '%s' is present on the classpath" +
                                             "and has a no-arg constructor",
                                             SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), httpImplFqcn))
                                    .cause(e)
                                    .build();
        }
    }

    /**
     * @return SystemPropertyHttpServiceProvider instance using the sync HTTP system property.
     */
    static SystemPropertyHttpServiceProvider<SdkHttpService> syncProvider() {
        return new SystemPropertyHttpServiceProvider<>(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL, SdkHttpService.class);
    }

    /**
     * @return SystemPropertyHttpServiceProvider instance using the async HTTP system property.
     */
    static SystemPropertyHttpServiceProvider<SdkAsyncHttpService> asyncProvider() {
        return new SystemPropertyHttpServiceProvider<>(SdkSystemSetting.ASYNC_HTTP_SERVICE_IMPL, SdkAsyncHttpService.class);
    }
}
