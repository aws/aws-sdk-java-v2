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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.Logger;

/**
 * A cache that stores classes and exposes a method to retrieve its zero-argument constructor.
 * <p>
 * This cache stores weak references to loaded classes, allowing them to be garbage collected at any point.
 * <p>
 * Classes are loaded by first attempting to load it via the thread context {@code ClassLoader} (or system {@code ClassLoader} if
 * the calling thread does not have one). If that fails, it will attempt using the {@code ClassLoader} that loaded
 * {@link ClassLoaderHelper}.
 * <p>
 * If a class or its zero-argument constructor cannot be found, an empty result is returned.
 *
 * @see ClassLoaderHelper#loadClass(String, boolean, Class[])
 */
@SdkInternalApi
public final class ConstructorCache {
    private static final Logger log = Logger.loggerFor(ConstructorCache.class);

    /**
     * Cache storing classes by class name and class loader.
     * Uses weak references to allow garbage collection when not needed.
     */
    private final Map<String, Optional<WeakReference<Class<?>>>> classesByClassName =
        new ConcurrentHashMap<>();

    /**
     * Retrieve the class for the given class name from the context or system class loader.
     * Returns an empty result if the class is not found.
     */
    private Optional<Class<?>> getClass(String className) {
        Optional<WeakReference<Class<?>>> classRef = classesByClassName.computeIfAbsent(className, k -> {
            try {
                Class<?> clazz = ClassLoaderHelper.loadClass(k, false);
                return Optional.of(new WeakReference<>(clazz));
            } catch (ClassNotFoundException e) {
                return Optional.empty();
            }
        });

        // Were we able to find this class?
        if (classRef.isPresent()) {
            Class<?> clazz = classRef.get().get();
            // Class hasn't been GC'd
            if (clazz != null) {
                return Optional.of(clazz);
            }
            // if the WeakReference to the class has been garbage collected, it has been unloaded.
            // Remove it from the cache and try a fresh load
            classesByClassName.remove(className);
            return getClass(className);
        }

        return Optional.empty();
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