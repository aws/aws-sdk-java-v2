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

package software.amazon.awssdk.core.interceptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.utils.Validate;

/**
 * Factory for creating request/response handler chains from the classpath.
 */
@SdkProtectedApi
public final class ClasspathInterceptorChainFactory {

    private static final String GLOBAL_INTERCEPTOR_PATH = "software/amazon/awssdk/global/handlers/execution.interceptors";

    /**
     * Constructs a new request handler chain by analyzing the specified classpath resource.
     *
     * @param resource The resource to load from the classpath containing the list of request handlers to instantiate.
     * @return A list of request handlers based on the handlers referenced in the specified resource.
     */
    public List<ExecutionInterceptor> getInterceptors(String resource) {
        return new ArrayList<>(createExecutionInterceptorsFromClasspath(resource));
    }

    /**
     * Load the global handlers by reading the global execution interceptors resource.
     */
    public List<ExecutionInterceptor> getGlobalInterceptors() {
        return new ArrayList<>(createExecutionInterceptorsFromClasspath(GLOBAL_INTERCEPTOR_PATH));
    }

    private Collection<ExecutionInterceptor> createExecutionInterceptorsFromClasspath(String path) {
        try {
            return createExecutionInterceptorsFromResources(classLoader().getResources(path))
                .collect(Collectors.toMap(p -> p.getClass().getSimpleName(), p -> p, (p1, p2) -> p1)).values();
        } catch (IOException e) {
            throw SdkClientException.builder()
                                    .message("Unable to instantiate execution interceptor chain.")
                                    .cause(e)
                                    .build();
        }
    }

    private Stream<ExecutionInterceptor> createExecutionInterceptorsFromResources(Enumeration<URL> resources) {
        if (resources == null) {
            return Stream.empty();
        }

        return Collections.list(resources).stream().flatMap(this::createExecutionInterceptorFromResource);
    }

    private Stream<ExecutionInterceptor> createExecutionInterceptorFromResource(URL resource) {
        try {
            if (resource == null) {
                return Stream.empty();
            }

            List<ExecutionInterceptor> interceptors = new ArrayList<>();

            try (InputStream stream = resource.openStream();
                 InputStreamReader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                 BufferedReader fileReader = new BufferedReader(streamReader)) {

                String interceptorClassName = fileReader.readLine();
                while (interceptorClassName != null) {
                    ExecutionInterceptor interceptor = createExecutionInterceptor(interceptorClassName);
                    if (interceptor != null) {
                        interceptors.add(interceptor);
                    }
                    interceptorClassName = fileReader.readLine();
                }
            }

            return interceptors.stream();
        } catch (IOException e) {
            throw SdkClientException.builder()
                                    .message("Unable to instantiate execution interceptor chain.")
                                    .cause(e)
                                    .build();
        }
    }

    private ExecutionInterceptor createExecutionInterceptor(String interceptorClassName) {
        if (interceptorClassName == null) {
            return null;
        }

        interceptorClassName = interceptorClassName.trim();
        if (interceptorClassName.equals("")) {
            return null;
        }

        try {
            Class<?> executionInterceptorClass = ClassLoaderHelper.loadClass(interceptorClassName,
                                                                             ExecutionInterceptor.class, getClass());
            Object executionInterceptorObject = executionInterceptorClass.newInstance();

            if (executionInterceptorObject instanceof ExecutionInterceptor) {
                return (ExecutionInterceptor) executionInterceptorObject;
            } else {
                throw SdkClientException.builder()
                                        .message("Unable to instantiate request handler chain for client. Listed"
                                                + " request handler ('" + interceptorClassName + "') does not implement" +
                                                " the " + ExecutionInterceptor.class + " API.")
                                        .build();
            }
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw SdkClientException.builder()
                                    .message("Unable to instantiate executor interceptor for client.")
                                    .cause(e)
                                    .build();
        }
    }

    private ClassLoader classLoader() {
        return Validate.notNull(ClassLoaderHelper.classLoader(getClass()),
                                "Failed to load the classloader of this class or the system.");
    }
}
