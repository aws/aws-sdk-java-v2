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

package software.amazon.awssdk.utils.internal;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * Utilities that assist with Java language reflection.
 */
@SdkInternalApi
public final class ReflectionUtils {
    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS = new ImmutableMap.Builder<Class<?>, Class<?>>()
        .put(boolean.class, Boolean.class)
        .put(byte.class, Byte.class)
        .put(char.class, Character.class)
        .put(double.class, Double.class)
        .put(float.class, Float.class)
        .put(int.class, Integer.class)
        .put(long.class, Long.class)
        .put(short.class, Short.class)
        .put(void.class, Void.class)
        .build();

    private ReflectionUtils() {
    }

    /**
     * Returns the wrapped class type associated with a primitive if one is known.
     * @param clazz The class to get the wrapped class for.
     * @return If the input class is a primitive class, an associated non-primitive wrapped class type will be returned,
     *         otherwise the same class will be returned indicating that no wrapping class is known.
     */
    public static Class<?> getWrappedClass(Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        }

        return PRIMITIVES_TO_WRAPPERS.getOrDefault(clazz, clazz);
    }
}
