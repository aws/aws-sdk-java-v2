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

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.Logger;

/**
 * {@link SdkHttpServiceProvider} implementation that uses {@link ServiceLoader} to find HTTP implementations on the
 * classpath. If more than one implementation is found on the classpath, then the SDK will choose based on priority order.
 */
@SdkInternalApi
final class ClasspathSdkHttpServiceProvider<T> implements SdkHttpServiceProvider<T> {

    static final Map<String, Integer> SYNC_HTTP_SERVICES_PRIORITY =
        ImmutableMap.<String, Integer>builder()
                    .put("software.amazon.awssdk.http.apache5.Apache5SdkHttpService", 1)
                    .put("software.amazon.awssdk.http.apache.ApacheSdkHttpService", 2)
                    .put("software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService", 3)
                    .put("software.amazon.awssdk.http.crt.AwsCrtSdkHttpService", 4)
                    .build();

    static final Map<String, Integer> ASYNC_HTTP_SERVICES_PRIORITY =
        ImmutableMap.<String, Integer>builder()
                    .put("software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService", 1)
                    .put("software.amazon.awssdk.http.crt.AwsCrtSdkHttpService", 2)
                    .build();

    private static final Logger log = Logger.loggerFor(ClasspathSdkHttpServiceProvider.class);

    private final Map<String, Integer> httpServicesPriority;

    private final SdkServiceLoader serviceLoader;
    private final Class<T> serviceClass;

    @SdkTestInternalApi
    ClasspathSdkHttpServiceProvider(SdkServiceLoader serviceLoader,
                                    Class<T> serviceClass,
                                    Map<String, Integer> httpServicesPriority) {
        this.serviceLoader = serviceLoader;
        this.serviceClass = serviceClass;
        this.httpServicesPriority = httpServicesPriority;
    }

    @Override
    public Optional<T> loadService() {
        Queue<T> impls = new PriorityQueue<>(
            Comparator.comparingInt(o -> httpServicesPriority.getOrDefault(o.getClass().getName(),
                                                                           Integer.MAX_VALUE)));
        Iterable<T> iterable = () -> serviceLoader.loadServices(serviceClass);
        iterable.forEach(impl -> impls.add(impl));

        if (impls.isEmpty()) {
            return Optional.empty();
        }

        log.debug(() -> logServices(impls));
        return Optional.of(impls.poll());
    }

    private String logServices(Queue<T> impls) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        int count = 0;
        for (T clazz : impls) {
            String name = clazz.getClass().getName();
            joiner.add(name);
            count++;
        }
        String implText = joiner.toString();
        T impl = impls.peek();
        String message = count == 1 ? "The HTTP implementation loaded is " + impl :
                         String.format(
                             "Multiple HTTP implementations were found on the classpath. The SDK will use %s since it has the "
                             + "highest priority. The multiple implementations found were: %s",
                             impl,
                             implText);

        return message;
    }

    /**
     * @return ClasspathSdkHttpServiceProvider that loads an {@link SdkHttpService} (sync) from the classpath.
     */
    static SdkHttpServiceProvider<SdkHttpService> syncProvider() {
        return new ClasspathSdkHttpServiceProvider<>(SdkServiceLoader.INSTANCE,
                                                     SdkHttpService.class,
                                                     SYNC_HTTP_SERVICES_PRIORITY);
    }

    /**
     * @return ClasspathSdkHttpServiceProvider that loads an {@link SdkAsyncHttpService} (async) from the classpath.
     */
    static SdkHttpServiceProvider<SdkAsyncHttpService> asyncProvider() {
        return new ClasspathSdkHttpServiceProvider<>(SdkServiceLoader.INSTANCE,
                                                     SdkAsyncHttpService.class,
                                                     ASYNC_HTTP_SERVICES_PRIORITY);
    }
}
