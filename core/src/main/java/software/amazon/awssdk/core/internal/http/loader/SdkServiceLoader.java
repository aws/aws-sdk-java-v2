/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Iterator;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Thin layer over {@link ServiceLoader} to allow mocking in tests.
 */
@SdkInternalApi
class SdkServiceLoader {

    public static final SdkServiceLoader INSTANCE = new SdkServiceLoader();

    <T> Iterator<T> loadServices(Class<T> clzz) {
        return ServiceLoader.load(clzz).iterator();
    }
}
