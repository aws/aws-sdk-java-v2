/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.loader;

import static software.amazon.awssdk.utils.Validate.notNull;

import java.util.Optional;

/**
 * Decorator of {@link SdkHttpServiceProvider} to provide lazy initialized caching.
 */
final class CachingSdkHttpServiceProvider<T> implements SdkHttpServiceProvider<T> {

    private final SdkHttpServiceProvider<T> delegate;

    /**
     * We assume that the service obtained from the provider chain will always be the same (even if it's an empty optional) so
     * we cache it as a field.
     */
    private volatile Optional<T> factory;

    CachingSdkHttpServiceProvider(SdkHttpServiceProvider<T> delegate) {
        this.delegate = notNull(delegate, "Delegate service provider cannot be null");
    }

    @Override
    public Optional<T> loadService() {
        if (factory == null) {
            synchronized (this) {
                if (factory == null) {
                    this.factory = delegate.loadService();
                }
            }
        }
        return factory;
    }
}
