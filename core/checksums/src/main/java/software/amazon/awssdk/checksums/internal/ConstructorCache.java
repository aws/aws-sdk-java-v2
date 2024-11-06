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

package software.amazon.awssdk.checksums.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.Logger;

/**
 * A cache that stores classes and their constructors by class name and class loader.
 * <p>
 * This cache uses weak references to both class loaders and classes, allowing them to be garbage collected
 * when no longer needed. It provides methods to retrieve the zero-argument constructor for a class,
 * based on the current thread's context class loader or the system class loader.
 * <p>
 * If a class or its zero-argument constructor cannot be found, an empty result is returned.
 */
@SdkInternalApi
public final class ConstructorCache {
    private static final Logger log = Logger.loggerFor(ConstructorCache.class);

    /**
     * Cache storing classes by class name and class loader.
     * Uses weak references to allow garbage collection when not needed.
     */
    private final Map<String, Map<ClassLoader, Optional<WeakReference<Class<?>>>>> classesByClassName =
        new ConcurrentHashMap<>();

    /**
     * Retrieve the class for the given class name from the context or system class loader.
     * Returns an empty result if the class is not found.
     */
    private Optional<Class<?>> getClass(String className) {
        Map<ClassLoader, Optional<WeakReference<Class<?>>>> classesByClassLoader =
            classesByClassName.computeIfAbsent(className, k -> Collections.synchronizedMap(new WeakHashMap<>()));

        ClassLoader classLoader = ClassLoaderHelper.contextClassLoader();
        Optional<WeakReference<Class<?>>> classRef = classesByClassLoader.computeIfAbsent(classLoader, k -> {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                return Optional.of(new WeakReference<>(clazz));
            } catch (ClassNotFoundException e) {
                return Optional.empty();
            }
        });
        return classRef.map(WeakReference::get);
    }

    /**
     * Retrieve the zero-argument constructor for the given class name.
     * Returns an empty result if no such constructor is found.
     */
    public Optional<Constructor<?>> getConstructor(String className) {
        return getClass(className).flatMap(clazz -> {
            try {
                return Optional.of(clazz.getConstructor());
            } catch (NoSuchMethodException e) {
                log.debug(() -> "Classloader contains " + className + ", but without a zero-arg constructor.", e);
                return Optional.empty();
            }
        });
    }
}