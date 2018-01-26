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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Service Provider interface for HTTP implementations. The core uses {@link java.util.ServiceLoader} to find appropriate
 * HTTP implementations on the classpath. HTTP implementations that wish to be discovered by the default HTTP provider chain
 * should implement this interface and declare that implementation as a service in the
 * META-INF/service/software.amazon.awssdk.http.SdkHttpService resource. See
 * <a href="https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html>Service Loader</a> for more
 * information.
 *
 * <p>
 * This interface is simply a factory for {@link SdkHttpClientFactory}. Implementations must be thread safe.
 * </p>
 */
@ThreadSafe
public interface SdkHttpService {

    /**
     * @return An {@link SdkHttpClientFactory} capable of creating {@link SdkHttpClient} instances. This factory should be thread
     * safe.
     */
    SdkHttpClientFactory createHttpClientFactory();
}
