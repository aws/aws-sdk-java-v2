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

import static software.amazon.awssdk.utils.Validate.notEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Consults a chain of {@link SdkHttpServiceProvider} looking for one that can provide a service instance.
 */
final class SdkHttpServiceProviderChain<T> implements SdkHttpServiceProvider<T> {

    private final List<SdkHttpServiceProvider<T>> httpProviders;

    @SafeVarargs
    SdkHttpServiceProviderChain(SdkHttpServiceProvider<T>... httpProviders) {
        this.httpProviders = Arrays.asList(notEmpty(httpProviders, "httpProviders cannot be null or empty"));
    }

    @Override
    public Optional<T> loadService() {
        return httpProviders.stream()
                            .map(SdkHttpServiceProvider::loadService)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .findFirst();
    }

}
