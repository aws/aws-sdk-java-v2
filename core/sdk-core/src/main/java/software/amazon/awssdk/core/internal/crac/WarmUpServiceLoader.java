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
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;

/**
 * Thin layer over {@link ServiceLoader} for {@link SdkWarmUpProvider}.
 */
@SdkInternalApi
class WarmUpServiceLoader {

    public static final WarmUpServiceLoader INSTANCE = new WarmUpServiceLoader();

    Iterator<SdkWarmUpProvider> loadProviders() {
        return ServiceLoader.load(SdkWarmUpProvider.class,
                                  ClassLoaderHelper.classLoader(WarmUpServiceLoader.class)).iterator();
    }
}
